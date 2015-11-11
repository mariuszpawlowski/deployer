package com.mario;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class DeployerApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DeployerApplication.class, args);
    }

    @Override
    public void run(String... strings) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject("https://www.tiktalik.com/api/v1/computing/instance?api_key=1mlVn8o9lu0rIF/Ygz50KoMN5kbiEYTrZs1hG7BaG4Frg1WRWD+/MRl8TFYfFGkmszf2//C3VTkrB6DoOLNeQg==", Instance.class);
    }
}
