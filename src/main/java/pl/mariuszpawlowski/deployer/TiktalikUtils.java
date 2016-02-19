package pl.mariuszpawlowski.deployer;

import pl.mariuszpawlowski.tiktalik.entity.Image;
import pl.mariuszpawlowski.tiktalik.entity.Instance;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Mariusz.Pawlowski on 2015-11-25.
 */
public class TiktalikUtils {

    public static String getDomainName(List<Instance> instances, String hostName) {
        String domainName = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst()
                .get().getInterfaces().get(0).getNetwork().getDomainname();
        return domainName;
    }

    public static String getIp(List<Instance> instances, String hostName) {
        String ip = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst()
                .get().getInterfaces().get(0).getIp();
        return ip;
    }

    public static String getVpsUuid(List<Instance> instances, String hostName) {
        String vpsUuid = instances.stream()
                .filter(i -> i.getHostname().equals(hostName))
                .findFirst()
                .get().getUuid();
        return vpsUuid;
    }

    public static List<Image> getBackupsWithName(String imageName, List<Image> images) {
        List<Image> imagesWithName = images.stream()
                .filter(i -> i.getName().equals(imageName))
                .collect(Collectors.toList());
        Collections.sort(imagesWithName);
        return imagesWithName;
    }
}
