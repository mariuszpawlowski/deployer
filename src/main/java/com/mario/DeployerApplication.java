package com.mario;

import com.mariuszpawlowski.teamcity.TeamCityJavaImpl;
import com.mariuszpawlowski.teamcity.entity.build.response.BuildResponse;
import com.mariuszpawlowski.teamcity.entity.build.response.RunBuildResponse;
import com.mariuszpawlowski.teamcity.entity.project.ProjectsResponse;
import com.mariuszpawlowski.tiktalik.TiktalikJava;
import com.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import com.mariuszpawlowski.tiktalik.entity.Instance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Optional;

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

    @Value("${imageUuid}")
    String imageUuid;

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

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        tiktalikJava = new TiktalikJavaImpl(loginTiktalik, passwordTiktalik);

        boolean teamcityInstanceExists = false;

        // create teamcity instance
        tiktalikJava.createNewInstance(hostName, imageUuid, networkUuid, instanceSize, diskSize);

        String domainName = "";
        String vpsUuid = "";
        while (!teamcityInstanceExists){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceExists = DeployerUtils.checkIfTeamCityInstanceExists(instances, hostName);
            if (teamcityInstanceExists){
                domainName = DeployerUtils.getDomainName(instances, hostName);
                vpsUuid = DeployerUtils.getVpsUuid(instances, hostName);
            }
            Thread.sleep(10 * 1000L);
        }


        // check if teamcity is running
        String teamCityFullHost = "http://" + hostName + ".mario." + domainName + ":8080";
        TeamCityJavaImpl teamcityJava = new TeamCityJavaImpl(loginTeamcity, passwordTeamcity, teamCityFullHost);

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

        // run build configuration
        RunBuildResponse runBuildResponse = teamcityJava.runBuild(buildId);
        String currentBuildId = runBuildResponse.getId();
        boolean buildFinished = false;

        // check if build finised
        while (!buildFinished){
            BuildResponse buildResponse = teamcityJava.getBuild(currentBuildId);
            if (buildResponse.getState().equals("finished")){
                buildFinished = true;
            }
            System.out.println("Build number: " + buildResponse.getNumber() +", Build state: " + buildResponse.getState() + " , Build status: " + buildResponse.getStatus());
        }

        // delete teamcity instance
        tiktalikJava.deleteInstance(vpsUuid);

        // check if instance deleted
        teamcityInstanceExists = true;
        while (teamcityInstanceExists){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceExists = DeployerUtils.checkIfTeamCityInstanceExists(instances, hostName);
            if (!teamcityInstanceExists){
                teamcityInstanceExists = false;
                System.out.println("Teamcity instance deleted");
            } else {
                System.out.println("Teamcity instance not deleted.");
            }
            Thread.sleep(10 * 1000L);
        }
    }



}
