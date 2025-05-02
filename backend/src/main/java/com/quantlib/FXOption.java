package com.quantlib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FXOption {
    static {
        try {
            loadNativeLibrary();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load native library: " + e.getMessage(), e);
        }
    }

    private static void loadNativeLibrary() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String libPath;
        String libName;

        // 确定库文件路径
        if (os.contains("win")) {
            libPath = "/lib/win32/Debug/";
            libName = "QuantLibJNId.dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            libPath = "/lib/darwin/Debug/";
            libName = "libQuantLibJNId.dylib";
        } else {
            libPath = "/lib/linux/Debug/";
            libName = "libQuantLibJNId.so";
        }

        // 从资源中复制库文件到临时目录
        String resourcePath = libPath + libName;
        try (InputStream is = FXOption.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Native library not found in resources: " + resourcePath);
            }

            // 创建临时文件
            Path tempDir = Files.createTempDirectory("quantlib_native");
            File tempLib = new File(tempDir.toFile(), libName);
            tempLib.deleteOnExit();
            tempDir.toFile().deleteOnExit();

            // 复制库文件
            Files.copy(is, tempLib.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 加载库文件
            System.load(tempLib.getAbsolutePath());
        }
    }

    // 期权类型枚举
    public static final int CALL = 1;
    public static final int PUT = 2;

    // 核心定价方法
    public native double calculateFXOption(
            int optionType, // 期权类型 (CALL/PUT)
            double spot, // USD/CNY 即期
            double strike, // 执行价格
            double domesticRate, // CNY 无风险利率
            double foreignRate, // USD 无风险利率
            double volatility, // 波动率
            int daysToMaturity // 到期天数
    );

    // 计算希腊字母
    public static class Greeks {
        public double delta;
        public double gamma;
        public double vega;
        public double theta;
        public double rho;
    }

    public native Greeks calculateFXOptionGreeks(
            int optionType,
            double spot,
            double strike,
            double domesticRate,
            double foreignRate,
            double volatility,
            int daysToMaturity);

    public FXOption() {
        // 默认构造函数
    }
}