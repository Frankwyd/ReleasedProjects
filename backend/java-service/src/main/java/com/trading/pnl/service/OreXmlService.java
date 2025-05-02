package com.trading.pnl.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OreXmlService {
    private static final Logger logger = LoggerFactory.getLogger(OreXmlService.class);

    public void updateOreXml(String workingDir, String date, String marketDataFile) {
        try {
            // 构建ore.xml文件的完整路径
            Path workingDirPath = Paths.get(workingDir);
            Path inputDirPath = workingDirPath.resolve("input");
            Path oreXmlPath = inputDirPath.resolve("ore.xml");
            File oreXmlFile = oreXmlPath.toFile();

            logger.info("Looking for ore.xml at: {}", oreXmlFile.getAbsolutePath());

            if (!oreXmlFile.exists()) {
                throw new RuntimeException("ore.xml file not found at: " + oreXmlFile.getAbsolutePath());
            }

            // 读取文件内容
            List<String> lines = Files.readAllLines(oreXmlPath);

            // 更新参数值
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                // 更新asofDate
                if (line.contains("name=\"asofDate\"")) {
                    lines.set(i, line.replaceFirst(">.*?</Parameter>", ">" + date + "</Parameter>"));
                    logger.info("Updated asofDate to: {}", date);
                }
                // 更新marketDataFile
                if (line.contains("name=\"marketDataFile\"")) {
                    lines.set(i, line.replaceFirst(">.*?</Parameter>", ">" + marketDataFile + "</Parameter>"));
                    logger.info("Updated marketDataFile to: {}", marketDataFile);
                }
            }

            // 写回文件，保持原有格式
            Files.write(oreXmlPath, lines);

            logger.info("Successfully updated ore.xml file at: {}", oreXmlFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Error updating ore.xml file", e);
            throw new RuntimeException("Failed to update ore.xml file: " + e.getMessage());
        }
    }
}