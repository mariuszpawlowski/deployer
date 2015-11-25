package com.mario;

import com.mariuszpawlowski.tiktalik.entity.Instance;

import java.util.List;
import java.util.Optional;

/**
 * Created by Mariusz.Pawlowski on 2015-11-25.
 */
public class TeamcityUtils {

    public static boolean checkIfTeamCityInstanceIsRunning(List<Instance> instances, String hostName) {
        boolean teamcityInstanceIsRunning = false;
        Optional<Instance> instance = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst();

        if (instance.isPresent()){
            if (instance.get().getRunning()){
                teamcityInstanceIsRunning = true;
                System.out.println("Teamcity server instance is running.");
            } else {
                System.out.println("Teamcity server instance is not running.");
            }
        } else {
            System.out.println("Teamcity server instance does not exist.");
        }

        return teamcityInstanceIsRunning;
    }

    public static boolean checkIfTeamCityInstanceExists(List<Instance> instances, String hostName) {
        boolean teamcityInstanceExists = false;
        Optional<Instance> instance = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst();

        if (instance.isPresent()){
            System.out.println("Teamcity server instance exists.");
            teamcityInstanceExists = true;
        } else {
            System.out.println("Teamcity server instance does not exist.");
        }

        return teamcityInstanceExists;
    }
}
