package com.cbb.workPlanTask;

import com.cbb.workPlanTask.mapper.ScanPlanMapper;
import com.cbb.workPlanTask.mapper.WorkPlanMapper;
import com.cbb.workPlanTask.model.ScanPlan;
import com.cbb.workPlanTask.model.WorkPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTask.class);
    @Autowired
    private WorkPlanMapper workPlanMapper;

    @Autowired
    private ScanPlanMapper scanPlanMapper;

    // 每小时整点执行，例如 1:00, 2:00, 3:00 ...
    @Scheduled(cron = "0 0 * * * ?")
    public void runHourly() {
        // 查出所有可读的workPlan
        List<WorkPlan> workPlans = workPlanMapper.getAllWorkPlans();// 查出所有可读的workPlan
        log.info("读取到"+workPlans.size()+"个计划任务");
        for (WorkPlan workPlan : workPlans) {
            // 判断是否在1小时内
            boolean result = getDuration(workPlan.getInsertTime());
            if(!result){
                // 若不在1小时内，则跳出循环
                break;
            }

            // 若在1小时内，且包含"检查"/"备份"则跳过本次更新
            if(checkName(workPlan.getPlanName())){
                continue;
            }
            // 若在1小时内，且不包含"检查"/"备份"则执行更新操作
            updateWorkPlan(workPlan);
        }
    }

    // 执行任务
    private void updateWorkPlan(WorkPlan workPlan){
        // 得到需要执行的任务
        List<ScanPlan> scanPlans = scanPlanMapper
                .searchScanPlan(workPlan.getPlanName(),
                workPlan.getStaffRealName());
        if(scanPlans.isEmpty()){
            log.error("未在任务表中查询到任务");
            return;
        }

        // 遍历任务
        for (ScanPlan scanPlan : scanPlans) {
            int index = scanPlan.getPlanName().indexOf('-');
            if(index == -1){
                log.error("任务名称格式不正确，应为：计划名-任务id");
                return;
            }

            // 把PLAN_NAME字段-之后的id替换成nechk.tbl_workplan表中的plan_id
            scanPlan.setPlanName(scanPlan.getPlanName().substring(0,index+1) + workPlan.getPlanId());
            // 把PLAN_STATE修改为1
            scanPlan.setPlanState("1");
            // 设置执行时间
            setExecTime(scanPlan,workPlan.getPlanTime());
            // 更新任务
            scanPlanMapper.updateScanPlan(scanPlan);
        }
    }

    private void setExecTime(ScanPlan scanPlan, String planTimeStr) {
        LocalDate planTime = LocalDate.parse(planTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 查询当天的任务列表
        List<ScanPlan> scanPlans = scanPlanMapper.searchSameDayScanPlan(
                planTime.getMonthValue(),
                planTime.getDayOfMonth());

        // 得到当天最晚执行的任务时间
        String maxExecTime = getMaxExecTime(scanPlans);
        int plusTime = 30;
        if(scanPlan.getPlanName().contains("OA") || scanPlan.getPlanName().contains("核心层")){
            plusTime = 10;
        }

        // 得到任务执行的时间
        int hour = getNum(maxExecTime, 3);
        int minute = getNum(maxExecTime, 2) + plusTime;
        int second = getNum(maxExecTime, 1);
        if(minute >= 60){
            hour += minute / 60;
            minute %= 60;
        }

        // 设置执行时间cron表达式
        String cron = second + " " + minute + " " + hour
                + " " + planTime.getDayOfMonth() + " "
                + planTime.getMonthValue() + " ? ";

        // 设置执行时间
        scanPlan.setPlanPeriodCron(cron);

        // 如果是月任务，则设置年为*
        if(scanPlan.getPlanPeriodCn().equals("月")) {
            cron = cron + "*";
            scanPlan.setPlanPeriodCron(cron);
        }
    }

    private String getMaxExecTime(List<ScanPlan> scanPlans) {
        String maxExecTime = "0 30 1 ? ? ?";
        for (ScanPlan scanPlan : scanPlans) {
            if(isBigger(scanPlan.getPlanPeriodCron(), maxExecTime)){
                maxExecTime = scanPlan.getPlanPeriodCron();
            }
        }
        return maxExecTime;
    }

    private int findNextNumIndex(String str,int n){
        // 如果是第一个直接返回起始位置
        if(n == 1){
            return 0;
        }
        // 统计现在是第几个非空格区间位置
        int now = 1;
        for(int i=0;i<str.length();++i) {
            // 如果是空格，则计数器加1
            if(str.charAt(i)==' '){
                ++now;
            }
            // 如果计数器等于目标位置，则返回当前位置的下一个字符的位置
            if(now == n){
                return i + 1;
            }
        }
        // 如果找不到，则返回-1
        return -1;
    }

    private int getNum(String str, int n){
        // 获取第n个非空格的区间字符
        return Integer.parseInt(str.substring(findNextNumIndex(str,n)
                ,findNextNumIndex(str,n+1) - 1));
    }

    private boolean isBigger(String time1, String time2){
        // 分别获取时分秒
        int hour1 = getNum(time1, 3), hour2 = getNum(time2, 3);
        int min1 = getNum(time1, 2), min2 = getNum(time2, 2);
        int sec1 = getNum(time1, 1), sec2 = getNum(time2, 1);

        if(hour1 > hour2) return true;
        if(hour1 < hour2) return false;
        if(min1 > min2) return true;
        if(min1 < min2) return false;
        return sec1 >= sec2;
    }

    // 判断是否包含备份或者检查，有就返回true，无则返回false
    private boolean checkName(String name){
        if (name == null) return true; // 可选：null 视为不包含目标词
        return (name.contains("备份") || name.contains("检查"));
    }

    // 判断是否在1小时内
    private boolean getDuration(String dbTimeStr){
        if(dbTimeStr.length() < 19){
            // 补0操作
            dbTimeStr = dbTimeStr.substring(0, 14) + "0" + dbTimeStr.substring(14);
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
        // 若在1小时内，则返回true,否则返回false
        return Math.abs(duration.getSeconds()) <= 3600;
    }
}
