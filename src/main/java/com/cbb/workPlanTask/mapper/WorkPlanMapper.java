package com.cbb.workPlanTask.mapper;

import com.cbb.workPlanTask.model.WorkPlan;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WorkPlanMapper {
    List<WorkPlan> getAllWorkPlans();
}