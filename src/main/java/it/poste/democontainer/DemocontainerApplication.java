package it.poste.democontainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
public class DemocontainerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemocontainerApplication.class, args);
    }

}
