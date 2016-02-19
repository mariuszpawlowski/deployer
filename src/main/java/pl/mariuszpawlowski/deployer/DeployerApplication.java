package pl.mariuszpawlowski.deployer;

import pl.mariuszpawlowski.teamcity.TeamCityJavaImpl;
import pl.mariuszpawlowski.teamcity.entity.build.response.BuildResponse;
import pl.mariuszpawlowski.teamcity.entity.build.response.RunBuildResponse;
import pl.mariuszpawlowski.teamcity.entity.project.ProjectsResponse;
import pl.mariuszpawlowski.tiktalik.TiktalikJava;
import pl.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import pl.mariuszpawlowski.tiktalik.entity.Image;
import pl.mariuszpawlowski.tiktalik.entity.Instance;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.concurrent.*;

@SpringBootApplication
public class DeployerApplication implements CommandLineRunner {

    @Value("${loginTiktalik}")
    String loginTiktalik;

    @Value("${passwordTiktalik}")
    String passwordTiktalik;

    @Value("${loginTeamcity}")
    String loginTeamcity;

    @Value("${passwordTeamcity}")
    String passwordTeamcity;

    @Value("${imageName}")
    String imageName;

    @Value("${networkUuid}")
    String networkUuid;

    @Value("${size}")
    String instanceSize;

    @Value("${disk_size_gb}")
    String diskSize;

    @Value("${hostName}")
    String hostName;

    @Value("${buildId}")
    String buildId;

    private TiktalikJava tiktalikJava;
    private TeamCityJavaImpl teamcityJava;

    private Logger log = Logger.getLogger(DeployerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        log.info("Deployer started.");
        String currentImageUuid;
        tiktalikJava = new TiktalikJavaImpl(loginTiktalik, passwordTiktalik);

        GetImageUuidTask getImageUuidTask = new GetImageUuidTask(tiktalikJava, imageName);

        boolean teamcityInstanceIsRunning = false;

        //get current imageUuid
        //String currentImageUuid = getImageUuid();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(getImageUuidTask);

        try {
            currentImageUuid = future.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("Timed out");
        }

        executor.shutdownNow();



        // create teamcity instance
        //tiktalikJava.createNewInstance(hostName, currentImageUuid, networkUuid, instanceSize, diskSize);

        String ip = "";
        String vpsUuid = "";
        while (!teamcityInstanceIsRunning){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceIsRunning = TeamcityUtils.checkIfTeamCityInstanceIsRunning(instances, hostName);
            if (teamcityInstanceIsRunning){
                ip = TiktalikUtils.getIp(instances, hostName);
                vpsUuid = TiktalikUtils.getVpsUuid(instances, hostName);
            }
            Thread.sleep(10 * 1000L);
        }


        // check if teamcity is running
        String teamCityFullHost = "http://" + ip + ":8080";


        teamcityJava = new TeamCityJavaImpl(loginTeamcity, passwordTeamcity, teamCityFullHost);

        checkIfTeamcityIsRunning();

        // run build configuration
        RunBuildResponse runBuildResponse = teamcityJava.runBuild(buildId);
        String currentBuildId = runBuildResponse.getId();
        checkIfBuildFinished(currentBuildId);

        // stop instance
        tiktalikJava.stopInstance(vpsUuid);
        checkIfInstanceStopped(vpsUuid);

        //create backup
        tiktalikJava.createBackup(vpsUuid);
        checkIfDuringAction(vpsUuid);

        //delete old image
        tiktalikJava.deleteImage(currentImageUuid);

        // delete teamcity instance
        tiktalikJava.deleteInstance(vpsUuid);

        // check if instance deleted
        checkIfInstanceExists();

        log.info("Deployer finished.");
    }

    private void checkIfInstanceExists() throws InterruptedException {
        boolean teamcityInstanceExists = true;
        while (teamcityInstanceExists){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceExists = TeamcityUtils.checkIfTeamCityInstanceExists(instances, hostName);
            if (!teamcityInstanceExists){
                teamcityInstanceExists = false;
                log.info("Teamcity instance deleted.");
            } else {
                log.info("Teamcity instance not deleted.");
            }
            Thread.sleep(10 * 1000L);
        }
    }

    private void checkIfDuringAction(String vpsUuid) throws InterruptedException {
        boolean duringAction = true;
        while (duringAction){
            Instance instance = tiktalikJava.getInstance(vpsUuid);
            if (!instance.getActionsPendingCount().equals("0")){
                log.info("Instance during action.");
            } else {
                log.info("Instance action finished.");
                duringAction = false;
            }
            Thread.sleep(10 * 1000L);
        }
    }

    private void checkIfInstanceStopped(String vpsUuid) throws InterruptedException {
        boolean isStopped = false;
        while (!isStopped){
            Instance instance = tiktalikJava.getInstance(vpsUuid);
            if (!instance.getRunning()){
                isStopped = true;
                log.info("Instance is stopped.");
            } else {
                log.info("Instance is running.");
            }
            Thread.sleep(10 * 1000L);
        }
    }

    private void checkIfBuildFinished(String currentBuildId) throws InterruptedException {
        boolean buildFinished = false;
        // check if build finished
        while (!buildFinished){
            BuildResponse buildResponse = teamcityJava.getBuild(currentBuildId);
            if (buildResponse.getState().equals("finished")){
                buildFinished = true;
            }
            log.info("Build number: " + buildResponse.getNumber() +", Build state: " + buildResponse.getState() + " , Build status: " + buildResponse.getStatus());
            Thread.sleep(10 * 1000L);
        }
    }

    private void checkIfTeamcityIsRunning() throws InterruptedException {
        ProjectsResponse projectsResponse = null;
        boolean teamCityStarted = false;
        while (!teamCityStarted){
            try {
                projectsResponse = teamcityJava.getProjects();
            } catch (Exception e){
                // do nothing, just waiting for TC to start
            } finally {
                Thread.sleep(10 * 1000L);
                if (projectsResponse == null){
                    log.warn("Teamcity is not running.");
                } else {
                    log.info("Teamcity is running.");
                    teamCityStarted = true;
                }
            }
        }
    }


}
