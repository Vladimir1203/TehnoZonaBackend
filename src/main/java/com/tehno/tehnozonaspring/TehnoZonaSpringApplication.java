package com.tehno.tehnozonaspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class TehnoZonaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(TehnoZonaSpringApplication.class, args);
    }

}
