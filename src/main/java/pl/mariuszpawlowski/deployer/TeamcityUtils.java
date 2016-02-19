package pl.mariuszpawlowski.deployer;

import pl.mariuszpawlowski.tiktalik.entity.Instance;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Created by Mariusz.Pawlowski on 2015-11-25.
 */
public class TeamcityUtils {

    private static Logger log = Logger.getLogger(TeamcityUtils.class);

    public static boolean checkIfTeamCityInstanceIsRunning(List<Instance> instances, String hostName) {
        boolean teamcityInstanceIsRunning = false;
        Optional<Instance> instance = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst();

        if (instance.isPresent()){
            if (instance.get().getRunning()){
                teamcityInstanceIsRunning = true;
                log.info("Teamcity server instance is running.");
            } else {
                log.warn("Teamcity server instance is not running.");
            }
        } else {
            log.error("Teamcity server instance does not exist.");
        }

        return teamcityInstanceIsRunning;
    }

    public static boolean checkIfTeamCityInstanceExists(List<Instance> instances, String hostName) {
        boolean teamcityInstanceExists = false;
        Optional<Instance> instance = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst();

        if (instance.isPresent()){
            log.info("Teamcity server instance exists.");
            teamcityInstanceExists = true;
        } else {
            log.warn("Teamcity server instance does not exist.");
        }

        return teamcityInstanceExists;
    }
}
