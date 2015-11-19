package com.mario;

import com.mariuszpawlowski.tiktalik.TiktalikJava;
import com.mariuszpawlowski.tiktalik.TiktalikJavaImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeployerApplication implements CommandLineRunner {

    @Value("${authCodeTiktalik}")
    String authCodeTiktalik;

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        TiktalikJava tiktalikJava = new TiktalikJavaImpl(authCodeTiktalik);
        tiktalikJava.getListOfInstances();

    }

}
