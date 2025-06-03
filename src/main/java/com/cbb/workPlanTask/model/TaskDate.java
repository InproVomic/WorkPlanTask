package com.cbb.workPlanTask.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDate {
    private String taskName;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
}
