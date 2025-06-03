package com.cbb.workPlanTask;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class ScheduledTaskTest {
    private boolean getDuration(String dbTimeStr){
        if(dbTimeStr.length() < 19){
            // 补0操作
            dbTimeStr = new String(dbTimeStr.substring(0,14) + "0" + dbTimeStr.substring(14));
        }
        // 定义格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析为 LocalDateTime
        LocalDateTime dbTime = LocalDateTime.parse(dbTimeStr, formatter);

        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 计算时间差
        Duration duration = Duration.between(dbTime, now);

        // 判断是否在1小时内（小于等于3600秒）
        if (Math.abs(duration.getSeconds()) <= 3600) {
            return true;
        }
        return false;
    }

    private boolean checkName(String name){
        if (name == null) return true; // 可选：null 视为不包含目标词
        return (name.contains("备份") || name.contains("检查"));
    }

    public static void main(String[] args) {
//        String dbTimeStr = "2025-05-14 16:4:00";
//        boolean result = new ScheduledTaskTest().getDuration(dbTimeStr);
//        if(result){
//            System.out.println("在1小时内");
//        }else{
//            System.out.println("不在1小时内");
//        }
        String name = "三大法师法师";
        boolean result = new ScheduledTaskTest().checkName(name);
        if(result){
            System.out.println("包含目标词");
        }else {
            System.out.println("不包含目标词");
        }
    }

//    @Test
//    void test(){
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        String time = "2025-05-25 18:4:01";
//
//        if(dbTimeStr.length() < 19){
//            // 补0操作
//            dbTimeStr = dbTimeStr.substring(0, 14) + "0" + dbTimeStr.substring(14);
//        }
//        // 分别将insertTime和planTimeStr转换成LocalDateTime
//        LocalDateTime insertTime = LocalDateTime.parse(time, formatter);
//
//    }

}