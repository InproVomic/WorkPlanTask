package com.cbb.workPlanTask.mapper;
import com.cbb.workPlanTask.model.ScanPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ScanPlanMapper {
    List<ScanPlan> searchScanPlan(@Param("planName") String planName,
                                  @Param("staffRealName") String staffRealName);
    Integer updateScanPlan(ScanPlan scanPlan);

    List<ScanPlan> searchSameDayScanPlan(@Param("month") int monthValue,
                                         @Param("day") int dayOfMonth);
}
