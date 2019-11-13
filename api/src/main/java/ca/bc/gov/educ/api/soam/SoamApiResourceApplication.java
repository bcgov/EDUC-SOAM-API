package ca.bc.gov.educ.api.soam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan("ca.bc.gov.educ.api.soam")
@ComponentScan("ca.bc.gov.educ.api.soam")
@EnableCaching
public class SoamApiResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoamApiResourceApplication.class, args);
    }

}