package com.cbb.workPlanTask;

import com.cbb.workPlanTask.Util.FileUtil;
import com.cbb.workPlanTask.model.WorkPlan;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class FileUtilTest {

    @Test
    public void test() throws IOException {
        WorkPlan workPlan1 = new WorkPlan();
        workPlan1.setPlanId(1L);
        workPlan1.setPlanName("test1");
        workPlan1.setPlanTime("2023-01-01");
        workPlan1.setStaffRealName("test");
        workPlan1.setMobile("1234567890");
        workPlan1.setInsertTime("2023-01-01 00:00:00");

        WorkPlan workPlan2 = new WorkPlan();
        workPlan2.setPlanId(2L);
        workPlan2.setPlanName("test2");
        workPlan2.setPlanTime("2023-01-01");
        workPlan2.setStaffRealName("test");
        workPlan2.setMobile("1234567890");
        workPlan2.setInsertTime("2023-01-01 00:00:00");

        FileUtil.appendWorkPlanToFile(workPlan1, "test.json");
        FileUtil.appendWorkPlanToFile(workPlan2, "test.json");
    }

    @Test
    public void test2() throws IOException {
        List<WorkPlan> workPlanList = new ArrayList<>();
        FileUtil.readAllWorkPlansAndClearFile("test.json", workPlanList);
        System.out.println(workPlanList);
    }
}
