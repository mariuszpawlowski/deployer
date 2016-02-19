package pl.mariuszpawlowski.deployer;

import pl.mariuszpawlowski.tiktalik.TiktalikJava;
import pl.mariuszpawlowski.tiktalik.entity.Image;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Mariusz.Pawlowski on 2016-02-19.
 */
public class GetImageUuidTask implements Callable<String> {

    private TiktalikJava tiktalikJava;
    private String imageName;

    public GetImageUuidTask(TiktalikJava tiktalikJava, String imageName) {
        this.tiktalikJava = tiktalikJava;
        this.imageName = imageName;
    }

    @Override
    public String call() throws Exception {
        String currentImageUuid = getImageUuid();
        return currentImageUuid;
    }

    private String getImageUuid() {
        List<Image> images = tiktalikJava.getListOfImages();
        List<Image> teamcityBackups = TiktalikUtils.getBackupsWithName(imageName, images);
        return teamcityBackups.get(0).getUuid();
    }
}
