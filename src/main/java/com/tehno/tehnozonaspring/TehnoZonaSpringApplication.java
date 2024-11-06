package com.tehno.tehnozonaspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@EntityScan("com.tehno.tehnozonaspring.model")
public class TehnoZonaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TehnoZonaSpringApplication.class, args);
    }

}
