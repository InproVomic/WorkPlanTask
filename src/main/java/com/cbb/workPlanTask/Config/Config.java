package com.cbb.workPlanTask.Config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Data
public class Config {

    @Value("${work-plan-task.cron}")
    private String cron;

    @Value("${work-plan-task.filePath}")
    private String filePath;

    @Value("${work-plan-task.day.execute-start-time}")
    private String dayExecuteStartTime;

    @Value("${work-plan-task.day.execute-end-time}")
    private String dayExecuteEndTime;

    @Value("${work-plan-task.month.execute-start-time}")
    private String monthExecuteStartTime;

    @Value("${work-plan-task.month.execute-end-time}")
    private String monthExecuteEndTime;
}
