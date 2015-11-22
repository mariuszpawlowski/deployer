package com.mario;

import com.mariuszpawlowski.teamcity.TeamCityJavaImpl;
import com.mariuszpawlowski.teamcity.entity.Projects;
import com.mariuszpawlowski.tiktalik.TiktalikJava;
import com.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import com.mariuszpawlowski.tiktalik.entity.Instance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
        // tiktalikJava.createNewInstance(hostName, imageUuid, networkUuid, instanceSize, diskSize);

        String domainName = "";
        while (!teamcityInstanceExists){
            List<Instance> instances = tiktalikJava.getListOfInstances();
            teamcityInstanceExists = checkIfTeamCityInstanceExists(instances);
            if (teamcityInstanceExists){
                domainName = getDomainName(instances, hostName);
            }
          //  Thread.sleep(10 * 1000L);
        }


        // check if teamcity is running
        String teamCityFullHost = "http://" + hostName + ".mario." + domainName + ":8080";
        TeamCityJavaImpl teamcityJava = new TeamCityJavaImpl(loginTeamcity, passwordTeamcity, teamCityFullHost);

        Projects projects = null;
        boolean teamCityStarted = false;
        while (!teamCityStarted){
            try {
                projects = teamcityJava.getProjects();
            } catch (Exception e){
                // do nothing, just waiting for TC to start
            } finally {
               // Thread.sleep(10 * 1000L);
                if (projects == null){
                    System.out.println("Teamcity is not running");
                } else {
                    System.out.println("Teamcity is running");
                    teamCityStarted = true;
                }
            }
        }

    // run build configuration
    teamcityJava.runBuild(buildId);


    }

    private String getDomainName(List<Instance> instances, String hostName) {
        String domainName = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst()
                .get().getInterfaces().get(0).getNetwork().getDomainname();
        return domainName;
    }

    private boolean checkIfTeamCityInstanceExists(List<Instance> instances) {
        boolean teamcityInstanceExists = false;
        Optional<Instance> instance = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst();

        if (instance.get().getRunning()){
            teamcityInstanceExists = true;
            System.out.println("Teamcity server instance is running.");
        } else {
            System.out.println("Teamcity server instance is not running.");
        }

        return teamcityInstanceExists;
    }

}
