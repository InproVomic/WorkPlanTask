package com.cbb.workPlanTask.Util;


import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;


public class CronUtil {

    private static final Logger log = LoggerFactory.getLogger(CronUtil.class);

    public static String getNextFireTimeString(String cronExpr) throws ParseException {
        CronExpression cron = new CronExpression(cronExpr);
        Date next = cron.getNextValidTimeAfter(new Date());

        if (next == null) return null;

        LocalDateTime nextTime = next.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();


        return nextTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }


}

