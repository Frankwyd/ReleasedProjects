package com.trading.pnl.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/ore")
public class OreController {
    private static final Logger logger = LoggerFactory.getLogger(OreController.class);
    private static final String DEFAULT_WORKING_DIR = "E:\\ORE\\Engine\\Examples\\Example_15_Theta_Test";

    @GetMapping("/test")
    public String test() {
        logger.info("Test endpoint called");
        return "ORE Controller is working!";
    }

    @GetMapping("/market-files")
    public Map<String, Object> getMarketFiles(@RequestParam String path, @RequestParam(required = false) String date) {
        logger.info("Getting market files from path: {} for date: {}", path, date);
        Map<String, Object> response = new HashMap<>();
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                logger.error("Directory does not exist or is not a directory: {}", path);
                response.put("status", "error");
                response.put("message", "Directory does not exist or is not a directory");
                return response;
            }

            logger.info("Checking for IRFX Market_Live.xls in directory: {}", path);
            File liveFile = new File(directory, "IRFX Market_Live.xls");
            logger.info("IRFX Market_Live.xls exists: {}", liveFile.exists());

            // 获取所有.xls文件
            File[] files = directory.listFiles((dir, name) -> {
                logger.info("Checking file: {}", name);
                // 首先检查是否是Live文件
                if (name.equals("IRFX Market_Live.xls")) {
                    logger.info("Found IRFX Market_Live.xls");
                    return true;
                }

                // 检查文件扩展名和基本名称
                if (!name.toLowerCase().endsWith(".xls") ||
                        (!name.contains("IRFX Market") && !name.contains("IRFX_Market"))) {
                    logger.info("File {} does not match criteria", name);
                    return false;
                }

                // 如果指定了日期，检查文件是否包含该日期
                if (date != null && !date.isEmpty()) {
                    boolean containsDate = name.contains(date);
                    logger.info("File {} date check: {}", name, containsDate);
                    return containsDate;
                }

                logger.info("File {} passed all checks", name);
                return true;
            });

            if (files == null) {
                logger.error("Failed to list files in directory: {}", path);
                response.put("status", "error");
                response.put("message", "Failed to list files");
                return response;
            }

            logger.info("Found {} files before filtering", files.length);

            // 将文件列表转换为文件名列表并进行排序
            List<String> fileNames = Arrays.stream(files)
                    .map(File::getName)
                    .sorted((a, b) -> {
                        // 确保Live文件始终在最前面
                        if (a.equals("IRFX Market_Live.xls"))
                            return -1;
                        if (b.equals("IRFX Market_Live.xls"))
                            return 1;

                        // 提取时间戳进行比较
                        String timeA = extractTime(a);
                        String timeB = extractTime(b);

                        // 如果两个文件都有时间戳，按时间戳倒序排列
                        if (timeA != null && timeB != null) {
                            return timeB.compareTo(timeA);
                        }

                        // 如果只有一个文件有时间戳，有时间戳的排在后面
                        if (timeA != null)
                            return 1;
                        if (timeB != null)
                            return -1;

                        // 如果都没有时间戳，按文件名排序
                        return a.compareTo(b);
                    })
                    .collect(Collectors.toList());

            logger.info("Files after sorting: {}", fileNames);

            // 确保Live文件在列表中
            if (!fileNames.contains("IRFX Market_Live.xls")) {
                if (liveFile.exists()) {
                    logger.info("Adding IRFX Market_Live.xls to the list");
                    fileNames.add(0, "IRFX Market_Live.xls");
                } else {
                    logger.warn("IRFX Market_Live.xls does not exist in directory");
                }
            }

            logger.info("Final file list: {}", fileNames);
            response.put("status", "success");
            response.put("files", fileNames);
        } catch (Exception e) {
            logger.error("Error getting market files: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Error getting market files: " + e.getMessage());
        }
        return response;
    }

    // 从文件名中提取时间戳
    private String extractTime(String fileName) {
        // 匹配格式为 _YYYYMMDD_HHMM 的时间戳
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("_(\\d{8}_\\d{4})");
        java.util.regex.Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @PostMapping("/run")
    public String runOre(@RequestBody Map<String, Object> request) {
        logger.info("Run ORE endpoint called with request: {}", request);
        try {
            // 从请求中获取工作目录
            String workingDir = (String) request.get("workingDir");
            if (workingDir == null || workingDir.trim().isEmpty()) {
                workingDir = DEFAULT_WORKING_DIR;
            }

            // 检查工作目录
            File workingDirFile = new File(workingDir);
            if (!workingDirFile.exists()) {
                logger.error("Working directory does not exist: {}", workingDir);
                return "Error: Working directory does not exist: " + workingDir;
            }
            logger.info("Working directory exists: {}", workingDirFile.getAbsolutePath());

            // 检查ore.xml文件
            File oreXml = new File(workingDirFile, "input\\ore.xml");
            if (!oreXml.exists()) {
                logger.error("ore.xml file does not exist at: {}", oreXml.getAbsolutePath());
                return "Error: ore.xml file does not exist at: " + oreXml.getAbsolutePath();
            }
            logger.info("ore.xml file exists at: {}", oreXml.getAbsolutePath());

            // 使用相对路径执行命令
            String command = "ore input\\ore.xml";
            logger.info("Executing command: {} in directory: {}", command, workingDir);
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command);
            processBuilder.directory(workingDirFile);
            processBuilder.redirectErrorStream(true);

            // 启动进程并获取输出
            Process process = processBuilder.start();
            logger.info("Process started");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;

            // 实时读取输出
            while ((line = reader.readLine()) != null) {
                logger.info("ORE output: {}", line);
                output.append(line).append("\n");
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            logger.info("Process completed with exit code: {}", exitCode);

            // 返回执行结果
            if (exitCode == 0) {
                String result = "Ore execution completed successfully.\nWorking Directory: " + workingDir +
                        "\nCommand: " + command + "\nOutput:\n" + output.toString();
                return result;
            } else {
                String result = "Ore execution failed with exit code: " + exitCode +
                        "\nWorking Directory: " + workingDir +
                        "\nCommand: " + command + "\nOutput:\n" + output.toString();
                return result;
            }
        } catch (Exception e) {
            logger.error("Error running ORE: {}", e.getMessage(), e);
            return "Error running ORE: " + e.getMessage();
        }
    }

    @PostMapping(value = "/clean-directory", produces = "application/json")
    public ResponseEntity<String> cleanDirectory(@RequestBody Map<String, String> request) {
        logger.info("Clean directory endpoint called with request: {}", request);
        String directory = request.get("directory");
        if (directory == null || directory.trim().isEmpty()) {
            logger.error("Directory path is required");
            return ResponseEntity.badRequest().body("Directory path is required");
        }

        try {
            File dir = new File(directory);
            if (!dir.exists() || !dir.isDirectory()) {
                logger.error("Invalid directory path: {}", directory);
                return ResponseEntity.badRequest().body("Invalid directory path: " + directory);
            }

            // 只清理 output 目录
            File outputDir = new File(dir, "output");
            if (outputDir.exists() && outputDir.isDirectory()) {
                logger.info("Cleaning output directory: {}", outputDir.getAbsolutePath());
                File[] outputFiles = outputDir.listFiles();
                if (outputFiles != null) {
                    for (File file : outputFiles) {
                        if (file.isFile()) {
                            if (!file.delete()) {
                                logger.warn("Failed to delete output file: {}", file.getAbsolutePath());
                            } else {
                                logger.info("Successfully deleted output file: {}", file.getAbsolutePath());
                            }
                        }
                    }
                }
                logger.info("Output directory cleaned successfully: {}", outputDir.getAbsolutePath());
            } else {
                logger.info("Output directory does not exist: {}", outputDir.getAbsolutePath());
            }

            return ResponseEntity.ok("Output directory cleaned successfully");
        } catch (Exception e) {
            logger.error("Error cleaning output directory: {}", directory, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cleaning output directory: " + e.getMessage());
        }
    }

    @PostMapping(value = "/save-output", produces = "application/json")
    public ResponseEntity<String> saveOutput(@RequestBody Map<String, String> request) {
        logger.info("Save output endpoint called with request: {}", request);
        String sourceDir = request.get("sourceDir");
        String targetDir = request.get("targetDir");

        if (sourceDir == null || targetDir == null) {
            logger.error("Source and target directories are required");
            return ResponseEntity.badRequest().body("Source and target directories are required");
        }

        try {
            File sourceOutputDir = new File(sourceDir, "output");
            File targetOutputDir = new File(targetDir, "output");

            // 确保源目录存在
            if (!sourceOutputDir.exists() || !sourceOutputDir.isDirectory()) {
                logger.error("Source output directory does not exist: {}", sourceOutputDir.getAbsolutePath());
                return ResponseEntity.badRequest().body("Source output directory does not exist");
            }

            // 创建目标目录（如果不存在）
            if (!targetOutputDir.exists()) {
                if (!targetOutputDir.mkdirs()) {
                    logger.error("Failed to create target directory: {}", targetOutputDir.getAbsolutePath());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to create target directory");
                }
            }

            // 复制所有文件
            File[] files = sourceOutputDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        File targetFile = new File(targetOutputDir, file.getName());
                        try {
                            java.nio.file.Files.copy(
                                    file.toPath(),
                                    targetFile.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            logger.info("Copied file: {} to {}", file.getName(), targetFile.getAbsolutePath());
                        } catch (IOException e) {
                            logger.error("Failed to copy file: {}", file.getName(), e);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Failed to copy file: " + file.getName());
                        }
                    }
                }
            }

            logger.info("Successfully copied all files from {} to {}",
                    sourceOutputDir.getAbsolutePath(), targetOutputDir.getAbsolutePath());
            return ResponseEntity.ok("Output files saved successfully");
        } catch (Exception e) {
            logger.error("Error saving output files: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error saving output files: " + e.getMessage());
        }
    }
}