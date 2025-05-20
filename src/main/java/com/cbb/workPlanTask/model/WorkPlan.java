package com.cbb.workPlanTask.model;

import lombok.Data;

@Data
public class WorkPlan {
    private Long planId;
    private String planName;
    private String planTime;
    private String staffRealName;
    private String mobile;
    private String insertTime;
}
