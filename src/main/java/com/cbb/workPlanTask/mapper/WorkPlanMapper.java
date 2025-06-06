package com.cbb.workPlanTask.mapper;

import com.cbb.workPlanTask.model.WorkPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface WorkPlanMapper {
    List<WorkPlan> getAllWorkPlans(@Param("findTime") Integer findTime);
    Integer updateInsertTime(@Param("planId") Long planId, @Param("insertTime") Date insertTime);
}