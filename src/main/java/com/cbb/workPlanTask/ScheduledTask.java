package com.cbb.workPlanTask;

import com.cbb.workPlanTask.Config.Config;
import com.cbb.workPlanTask.Util.CronUtil;
import com.cbb.workPlanTask.mapper.ScanPlanMapper;
import com.cbb.workPlanTask.mapper.WorkPlanMapper;
import com.cbb.workPlanTask.model.ScanPlan;
import com.cbb.workPlanTask.model.TaskDate;
import com.cbb.workPlanTask.model.WorkPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ScheduledTask {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTask.class);
    @Autowired
    private WorkPlanMapper workPlanMapper;

    @Autowired
    private ScanPlanMapper scanPlanMapper;

    @Autowired
    private Config config;

    // 每小时整点执行，例如 1:00, 2:00, 3:00 ...
    @Scheduled(cron = "#{@config.cron}")
    public void runHourly() throws IOException, ParseException {
        // 查出所有可读的workPlan
        List<WorkPlan> workPlans = workPlanMapper.getAllWorkPlans(config.getFindTime());// 查出所有可读的workPlan

//        // 扫描推迟的任务,并插入workPlans中
//        String filePath = config.getFilePath();
//        FileUtil.readAllWorkPlansAndClearFile(filePath, workPlans);

        log.info("读取到{}个计划任务", workPlans.size());
        for (WorkPlan workPlan : workPlans) {
            if (workPlan.getPlanName().length() <= 6) {
                log.error("任务名称:'{}'不规范，长度不大于6，请检查", workPlan.getPlanName());
                continue;
            }
            // 判断是否在1小时内
//            boolean result = getDuration(workPlan.getInsertTime());
//            if(!result){
//                // 若不在1小时内，则跳出循环
//                break;
//            }
            // 得到需要执行的任务
            List<ScanPlan> scanPlans = scanPlanMapper
                    .searchScanPlan(workPlan.getPlanName(),
                            workPlan.getStaffRealName());

            if (scanPlans.isEmpty()) {
                log.error("未在任务表中查询到任务");
                continue;
            }
            boolean isBreak = false;

            for (ScanPlan scanPlan : scanPlans) {
                // 根据planId判断是否为当前任务
                if (scanPlan.getPlanName().substring(scanPlan.getPlanName().lastIndexOf("-") + 1)
                        .equals(String.valueOf(workPlan.getPlanId()))) {
                    // 如果是当前任务，则结束当前workPlan
                    isBreak = true;
                    log.info("已避免任务plan_id:{}的任务重复执行", workPlan.getPlanId());
                    break;
                }
            }

            // 若为当前任务，则跳出当前workPlan
            if (isBreak) {
                continue;
            }

            // 判断当前任务是否需要进行推迟
            if (isDelay(workPlan, scanPlans)) {
                String insertTimeStr = workPlan.getInsertTime();
                if (!insertTimeStr.contains(".")) {
                    // 如果没有毫秒，补成 .000
                    insertTimeStr += ".000";
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date insertTimeDate = sdf.parse(insertTimeStr);
                workPlanMapper.updateInsertTime(workPlan.getPlanId(), insertTimeDate);

//                // 将推迟的任务追加到文件中
//                FileUtil.appendWorkPlanToFile(workPlan, filePath);
                continue;
            }

            // 若在1小时内，且包含"检查"/"备份"则跳过本次更新
            if (checkName(workPlan.getPlanName())) {
                log.info("发现包含\"检查\"/\"备份\"跳过本次更新");
                continue;
            }

            // 若在1小时内，且不包含"检查"/"备份"，则执行更新操作
            updateWorkPlan(workPlan, scanPlans);
        }
    }

    // 任务拆分
//    private List<WorkPlan> SplitTask(WorkPlan workPlan) {
//        log.info("发现天任务" + workPlan.getPlanName() + "，需对其进行拆分" + "该任务对应时间为" + workPlan.getPlanTime());
//        List<String> planTimeList = Arrays.asList(workPlan.getPlanTime().split(","));
//        List<WorkPlan> workPlans = new ArrayList<>();
//        planTimeList.forEach(planTime -> {
//            WorkPlan tmpWorkPlan = new WorkPlan();
//            tmpWorkPlan.setPlanName(workPlan.getPlanName());
//            tmpWorkPlan.setStaffRealName(workPlan.getStaffRealName());
//            tmpWorkPlan.setPlanTime(planTime);
//            tmpWorkPlan.setMobile(workPlan.getMobile());
//            tmpWorkPlan.setInsertTime(workPlan.getInsertTime());
//            tmpWorkPlan.setPlanId(workPlan.getPlanId());
//            workPlans.add(tmpWorkPlan);
//            log.info("拆分任务："+tmpWorkPlan.getPlanName()+" 至 "+planTime);
//        });
//        return workPlans;
//    }

    // 判断是否需要推迟任务，若要修改则返回true且修改insertTime
    private boolean isDelay(WorkPlan workPlan, List<ScanPlan> scanPlans) throws ParseException {
        for (ScanPlan scanPlan : scanPlans) {
            // 如果处于已激活状态，则判断其执行日期
            if (scanPlan.getPlanState().equals("1")) {
                // 将Cron表达式转换成下一次执行的日期
                String planTimeStr = CronUtil.getNextFireTimeString(scanPlan.getPlanPeriodCron());
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                String dateTimeStr = workPlan.getInsertTime();
                String workPlanTimeStr = workPlan.getPlanTime();
                if (workPlanTimeStr.contains(",")) {
                    // 如果planTime包含逗号，则只取第一个时间点
                    workPlanTimeStr = workPlanTimeStr.substring(0, workPlanTimeStr.indexOf(','));
                }

                if (dateTimeStr.contains(".")) {
                    dateTimeStr = dateTimeStr.substring(0, dateTimeStr.indexOf('.'));
                }

                // 分别将insertTime和planTimeStr转换成LocalDateTime
                LocalDateTime insertTime = LocalDateTime.parse(dateTimeStr, formatter);
                LocalDate workPlanTime = LocalDate.parse(workPlanTimeStr, formatter2);
                assert planTimeStr != null;
                LocalDateTime planTime = LocalDateTime.parse(planTimeStr, formatter);

                int subTime = planTime.getMonthValue() - LocalDateTime.now().getMonthValue();
                if (planTime.getYear() - LocalDateTime.now().getYear() >= 1) {
                    subTime += 12 * (planTime.getYear() - LocalDateTime.now().getYear());
                }

                // 如果 planTime 比现在时间往后推了2个月以上，说明已经执行过，则不推迟任务
                if (subTime > 2) {
                    return false;
                }

                // 如果 planTime 已经过去，则不推迟任务
                if (planTime.isBefore(LocalDateTime.now())) {
                    return false;
                }

                // 如果是"天"任务，且和workPlan表中的月份相同，则不推迟任务
                if (scanPlan.getPlanPeriodCn().equals("天") && (workPlanTime.getMonthValue() == planTime.getMonthValue() ||
                        (planTime.isBefore(LocalDateTime.now()) && planTime.plusDays(1).getMonthValue() == workPlanTime.getMonthValue()))) {
                    return false;
                }

                // 如果 insertTime 早于 planTime，就将 insertTime 设置为 planTime + 1 小时
                if (insertTime.isBefore(planTime)) {
                    insertTime = planTime.plusHours(config.getFindTime());
                }

                // 修改workPlan的insertTime字段为新的时间
                workPlan.setInsertTime(insertTime.format(formatter));
                log.info("推迟：{} 至 {}再进行更新表操作，任务执行时间: {}", workPlan.getPlanName(), insertTime, workPlan.getPlanTime());
                return true;
            }
        }
        return false;
    }

    // 执行任务
    private void updateWorkPlan(WorkPlan workPlan, List<ScanPlan> scanPlans) {
        // 遍历任务
        for (ScanPlan scanPlan : scanPlans) {
            int index = scanPlan.getPlanName().indexOf('-');
            if (index == -1) {
                log.error("任务名称格式不正确，应为：计划名-任务id");
                continue;
            }

            // 把PLAN_NAME字段-之后的id替换成nechk.tbl_workplan表中的plan_id
            scanPlan.setPlanName(scanPlan.getPlanName().substring(0, index + 1) + workPlan.getPlanId());
            // 把PLAN_STATE修改为1
            scanPlan.setPlanState("1");

            // 设置执行时间
            setExecTime(scanPlan, workPlan);
            // 更新任务
            scanPlanMapper.updateScanPlan(scanPlan);
        }
    }


    private TaskDate getTaskExecuteDate(ScanPlan scanPlan, String planTimeStr, String insertTimeStr) {
        LocalDate planTime = LocalDate.parse(planTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // 查询当天的任务列表
        List<ScanPlan> scanPlans = scanPlanMapper.searchSameDayScanPlan(
                planTime.getMonthValue(),
                planTime.getDayOfMonth());

        // 移除当天不执行的任务
        scanPlans.removeIf(plan -> !isCronMatchDay(plan.getPlanPeriodCron(),
                planTime.getYear(), planTime.getMonthValue(), planTime.getDayOfMonth()));


        // 如果是天任务则要寻找当天0-2点最大执行时间，间隔10分钟
        // 记得要拆分多个planTime的时间！
        String maxExecTime = config.getMonthExecuteStartTime();
        String endTime = config.getMonthExecuteEndTime();

        if (scanPlan.getPlanPeriodCn().equals("天")) {
            maxExecTime = config.getDayExecuteStartTime();
            endTime = config.getDayExecuteEndTime();
        }


        // 得到当天最晚执行的任务时间
        List<String> maxExecList = getMaxExecList(scanPlans, maxExecTime, endTime);

        // 查找上一个任务是不是OA或者核心层又或者是天执行的任务，若是则执行时间间隔10分钟，否则30分钟
        int plusTime = 30;
        if (maxExecList.get(1).contains("OA") || maxExecList.get(1).contains("核心层") ||
                scanPlan.getPlanPeriodCn().equals("天")) {
            plusTime = 10;
        }

        // 得到任务执行的时间
        int hour = getNum(maxExecList.get(0), 3);
        int minute = getNum(maxExecList.get(0), 2) + plusTime;
        int second = getNum(maxExecList.get(0), 1);
        int day = planTime.getDayOfMonth();
        int month = planTime.getMonthValue();
        if (minute >= 60) {
            hour += minute / 60;
            minute %= 60;
        }

        if (hour >= 24) {
            hour -= 24;
            day += 1;
        }

        // 处理同一天执行可能出现的问题
        if (insertTimeStr.equals(planTimeStr)) {
            String timeStr = String.format("%02d:%02d:%02d", hour, minute, second);
            LocalTime giveTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 如果是当天任务，且小于当前时间，则往后推一个小时的整时
            LocalTime now = LocalTime.now();
            if (now.isAfter(giveTime)) {
                hour = now.getHour() + 1;
                minute = 0;
                second = 0;
            }
        }

        if (hour >= 24) {
            hour -= 24;
            day += 1;
        }
        if (day > planTime.getMonth().length(planTime.isLeapYear())) {
            month += 1;
            day = 1;
        }
        if (month > 12) {
            month = 1;
        }
        return new TaskDate(scanPlan.getPlanName(), month, day, hour, minute, second);
    }

    public int findNthSpace(String str, int n) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ' ') {
                count++;
                if (count == n) {
                    return i;  // 返回第 n 个空格的位置（下标）
                }
            }
        }
        return -1; // 没有找到第 n 个空格
    }

    public boolean isCronMatchDay(String planPeriodCron, int year, int month, int day) {
        int scanDay = findNthSpace(planPeriodCron, 3) + 1, dayEnd = findNthSpace(planPeriodCron, 4);
        String[] days = planPeriodCron.substring(scanDay, dayEnd).split(",");
        boolean isDay = false;
        for (String dayStr : days) {
            if (dayStr.equals("*") || dayStr.equals("?") || Integer.parseInt(dayStr) == day) {
                isDay = true;
                break;
            }
        }
        if (!isDay) {
            return false;
        }

        int scanMonth = findNthSpace(planPeriodCron, 4) + 1, monthEnd = findNthSpace(planPeriodCron, 5);
        if(!planPeriodCron.substring(scanMonth, monthEnd).equals("*") &&
                !planPeriodCron.substring(scanMonth, monthEnd).equals("?") &&
                !(Integer.parseInt(planPeriodCron.substring(scanMonth, monthEnd)) == month)){
            return false;
        }

        int scanYear = findNthSpace(planPeriodCron, 5) + 1, yearEnd = findNthSpace(planPeriodCron, 6);
        int theYear = 0;
        String theYearStr;
        if(yearEnd == -1){
            theYearStr = planPeriodCron.substring(scanYear);
        }else {
            theYearStr = planPeriodCron.substring(scanYear, yearEnd);
        }
        if(theYearStr.equals("*") || theYearStr.equals("?")){
            return true;
        }
        theYear = Integer.parseInt(theYearStr);
        return theYear == year;
    }


    private void setExecTime(ScanPlan scanPlan, WorkPlan workPlan ) {
        String insertTimeStr = workPlan.getInsertTime().substring(0, 10);
        String planTimeStr = workPlan.getPlanTime();
        int second=0,minute=0, hour=0, month=0;
        StringBuilder day = null;

        // 包含","说明是天任务
        if(planTimeStr.contains(",")){
            String[] workPlanList = workPlan.getPlanTime().split(",");
            TaskDate taskDateMin = new TaskDate(null,0,0,0,0,0);
            for(String planTime: workPlanList){
                TaskDate taskDate = getTaskExecuteDate(scanPlan, planTime, insertTimeStr);

                if(day == null){
                    day = new StringBuilder(String.valueOf(taskDate.getDay()));
                }else{
                    day.append(",").append(taskDate.getDay());//多日期拼接
                }

                int time1 = taskDate.getHour() * 3600 + taskDate.getMinute() * 60 + taskDate.getSecond();
                int time2 = taskDateMin.getHour() * 3600 + taskDateMin.getMinute() * 60 + taskDateMin.getSecond();
                if(time1 > time2){
                    taskDateMin = taskDate;
                }
            }

            second = taskDateMin.getSecond();
            minute = taskDateMin.getMinute();
            hour = taskDateMin.getHour();
            month = taskDateMin.getMonth();
        }else {
            TaskDate taskDate = getTaskExecuteDate(scanPlan, planTimeStr, insertTimeStr);
            second = taskDate.getSecond();
            minute = taskDate.getMinute();
            hour = taskDate.getHour();
            day = new StringBuilder(String.valueOf(taskDate.getDay()));
            month = taskDate.getMonth();
        }

        // 设置执行时间cron表达式
        String cron = second + " " + minute + " " + hour
                + " " + day + " * ?";

        // 如果是月任务，则设置年为*
        if(scanPlan.getPlanPeriodCn().equals("月")) {
            cron = second + " " + minute + " " + hour
                    + " " + day + " "
                    + month + " ? *";
        }

        // 设置执行时间
        scanPlan.setPlanPeriodCron(cron);
        log.info("设置执行时间cron表达式为: {}", cron);
    }

    private List<String> getMaxExecList(List<ScanPlan> scanPlans,final String pMaxExecTime,final String endTime) {
        String maxExecTime = pMaxExecTime;
        String maxName = "";
        for (ScanPlan scanPlan : scanPlans) {
            if(isBigger(scanPlan.getPlanPeriodCron(), maxExecTime) && isBigger(endTime, scanPlan.getPlanPeriodCron())){
                maxExecTime = scanPlan.getPlanPeriodCron();
                maxName = scanPlan.getPlanName();
            }
        }

        List<String> maxExecList = new ArrayList<>();
        maxExecList.add(maxExecTime);
        maxExecList.add(maxName);
        log.info("找到最大执行时间: {}，其任务名为：{}", maxExecTime,maxName);
        return maxExecList;
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

//    // 判断是否在1小时内
//    private boolean getDuration(String dbTimeStr){
//        if(dbTimeStr.length() < 19){
//            // 补0操作
//            dbTimeStr = dbTimeStr.substring(0, 14) + "0" + dbTimeStr.substring(14);
//        }
//        // 定义格式化器
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//        // 解析为 LocalDateTime
//        LocalDateTime dbTime = LocalDateTime.parse(dbTimeStr, formatter);
//
//        // 当前时间
//        LocalDateTime now = LocalDateTime.now();
//
//        // 计算时间差
//        Duration duration = Duration.between(dbTime, now);
//
//        // 判断是否在1小时内（小于等于3600秒）
//        // 若在1小时内，则返回true,否则返回false
//        return Math.abs(duration.getSeconds()) <= 3600;
//    }
}
