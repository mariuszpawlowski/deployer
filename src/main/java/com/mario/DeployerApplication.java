package com.mario;

import com.mariuszpawlowski.tiktalik.TiktalikJava;
import com.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import com.mariuszpawlowski.tiktalik.entity.Instance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@SpringBootApplication
public class DeployerApplication implements CommandLineRunner {

    @Value("${login}")
    String login;

    @Value("${password}")
    String password;

    @Value("${imageUuid}")
    String imageUuid;

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        TiktalikJava tiktalikJava = new TiktalikJavaImpl(login, password);
        List<Instance> instances = tiktalikJava.getListOfInstances();

        // create new instance
        String hostName = "teamcity";
        tiktalikJava.createNewInstance(imageUuid, hostName);

        System.out.println("done");

    }

}
