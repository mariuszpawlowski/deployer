package com.mario;

import com.mariuszpawlowski.teamcity.TeamCityJavaImpl;
import com.mariuszpawlowski.teamcity.entity.build.response.BuildResponse;
import com.mariuszpawlowski.teamcity.entity.build.response.RunBuildResponse;
import com.mariuszpawlowski.teamcity.entity.project.ProjectsResponse;
import com.mariuszpawlowski.tiktalik.TiktalikJava;
import com.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import com.mariuszpawlowski.tiktalik.entity.Image;
import com.mariuszpawlowski.tiktalik.entity.Instance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

import static com.mario.TiktalikUtils.getBackupsWithName;

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

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        tiktalikJava = new TiktalikJavaImpl(loginTiktalik, passwordTiktalik);

        boolean teamcityInstanceIsRunning = false;

        //get current imageUuid
        String currentImageUuid = getImageUuid();

        // create teamcity instance
//        tiktalikJava.createNewInstance(hostName, currentImageUuid, networkUuid, instanceSize, diskSize);

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
    }

    private void checkIfInstanceExists() throws InterruptedException {
        boolean teamcityInstanceExists = true;
        while (teamcityInstanceExists){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceExists = TeamcityUtils.checkIfTeamCityInstanceExists(instances, hostName);
            if (!teamcityInstanceExists){
                teamcityInstanceExists = false;
                System.out.println("Teamcity instance deleted");
            } else {
                System.out.println("Teamcity instance not deleted.");
            }
            Thread.sleep(10 * 1000L);
        }
    }

    private void checkIfDuringAction(String vpsUuid) throws InterruptedException {
        boolean duringAction = true;
        while (duringAction){
            Instance instance = tiktalikJava.getInstance(vpsUuid);
            if (!instance.getActionsPendingCount().equals("0")){
                System.out.println("Instance during action");
            } else {
                System.out.println("Instance action finished");
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
                System.out.println("Instance is stopped.");
            } else {
                System.out.println("Instance is running.");
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
            System.out.println("Build number: " + buildResponse.getNumber() +", Build state: " + buildResponse.getState() + " , Build status: " + buildResponse.getStatus());
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
                    System.out.println("Teamcity is not running");
                } else {
                    System.out.println("Teamcity is running");
                    teamCityStarted = true;
                }
            }
        }
    }

    private String getImageUuid() {
        List<Image> images = tiktalikJava.getListOfImages();
        List<Image> teamcityBackups = getBackupsWithName(imageName, images);
        return teamcityBackups.get(0).getUuid();
    }

}
