package com.cbb.workPlanTask.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkPlan {
    private Long planId;
    private String planName;
    private String planTime;
    private String staffRealName;
    private String mobile;
    private String insertTime;
}
