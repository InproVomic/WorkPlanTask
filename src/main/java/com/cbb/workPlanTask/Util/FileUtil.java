package com.cbb.workPlanTask.Util;

import com.cbb.workPlanTask.model.WorkPlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;

@Component
public class FileUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 将单个 WorkPlan 追加保存到文件（每行一个 JSON）
    public static void appendWorkPlanToFile(WorkPlan workPlan, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath, true)) { // true: 追加模式
            String json = objectMapper.writeValueAsString(workPlan);
            writer.write(json + System.lineSeparator());
        }
    }

    // 读取所有 WorkPlan 到列表中，并清空文件
    public static void readAllWorkPlansAndClearFile(String filePath, List<WorkPlan> targetList) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    WorkPlan plan = objectMapper.readValue(line, WorkPlan.class);
                    targetList.add(plan);
                }
            }
        }

        // 清空文件内容
        try (FileWriter clearer = new FileWriter(file, false)) {
            clearer.write(""); // 覆盖为 ""，即清空
        }
    }
}
