package com.cbb.workPlanTask.model;

import lombok.Data;

@Data
public class ScanPlan {
    private String seqid;
    private String planName;
    private String planStartdate;     // 计划开始时间
    private String planEnddate;       // 计划结束时间
    private String planState;         // 计划状态
    private String planPeriodCn;      // 周期（中文描述）
    private String planPeriodCron;    // 周期（cron 表达式）
    private String professionId;      // 专业ID
    private String periodText;        // 周期说明文字
    private String memo;              // 备注
    private String creator;           // 创建人
    private String ownId;
    private String scopeid;
}
