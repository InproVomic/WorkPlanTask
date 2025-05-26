package com.cbb.workPlanTask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
    public class WorkPlanTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkPlanTaskApplication.class, args);
    }

}
