package com.cbb.workPlanTask;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class testController {
    @Autowired
    ScheduledTask scheduledTask;

    @GetMapping("/test")
    public String test() {
        scheduledTask.runHourly();
        return "成功调用！";
    }
}
