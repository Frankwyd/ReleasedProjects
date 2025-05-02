package com.quantlib;

public class FXOptionTest {
    public static void main(String[] args) {
        try {
            // 创建 FXOption 实例
            FXOption option = new FXOption();

            // 设置测试参数
            double spot = 7.15; // USD/CNY 即期汇率
            double strike = 7.20; // 执行价格
            double domesticRate = 0.02; // CNY 利率 (2%)
            double foreignRate = 0.05; // USD 利率 (5%)
            double volatility = 0.08; // 波动率 (8%)
            int daysToMaturity = 90; // 到期天数

            System.out.println("FX Option Pricing Test");
            System.out.println("=====================");
            System.out.println("Parameters:");
            System.out.println("Spot Rate: " + spot);
            System.out.println("Strike Price: " + strike);
            System.out.println("CNY Interest Rate: " + (domesticRate * 100) + "%");
            System.out.println("USD Interest Rate: " + (foreignRate * 100) + "%");
            System.out.println("Volatility: " + (volatility * 100) + "%");
            System.out.println("Days to Maturity: " + daysToMaturity);
            System.out.println();

            // 计算看涨期权价格
            double callPrice = option.calculateFXOption(
                    FXOption.CALL,
                    spot,
                    strike,
                    domesticRate,
                    foreignRate,
                    volatility,
                    daysToMaturity);

            // 计算看跌期权价格
            double putPrice = option.calculateFXOption(
                    FXOption.PUT,
                    spot,
                    strike,
                    domesticRate,
                    foreignRate,
                    volatility,
                    daysToMaturity);

            System.out.println("Option Prices:");
            System.out.printf("Call Option: %.6f CNY%n", callPrice);
            System.out.printf("Put Option: %.6f CNY%n", putPrice);
            System.out.println();

            // 计算看涨期权的希腊字母
            System.out.println("Call Option Greeks:");
            FXOption.Greeks callGreeks = option.calculateFXOptionGreeks(
                    FXOption.CALL,
                    spot,
                    strike,
                    domesticRate,
                    foreignRate,
                    volatility,
                    daysToMaturity);

            System.out.printf("Delta: %.6f%n", callGreeks.delta);
            System.out.printf("Gamma: %.6f%n", callGreeks.gamma);
            System.out.printf("Vega: %.6f%n", callGreeks.vega);
            System.out.printf("Theta: %.6f%n", callGreeks.theta);
            System.out.printf("Rho: %.6f%n", callGreeks.rho);
            System.out.println();

            // 计算看跌期权的希腊字母
            System.out.println("Put Option Greeks:");
            FXOption.Greeks putGreeks = option.calculateFXOptionGreeks(
                    FXOption.PUT,
                    spot,
                    strike,
                    domesticRate,
                    foreignRate,
                    volatility,
                    daysToMaturity);

            System.out.printf("Delta: %.6f%n", putGreeks.delta);
            System.out.printf("Gamma: %.6f%n", putGreeks.gamma);
            System.out.printf("Vega: %.6f%n", putGreeks.vega);
            System.out.printf("Theta: %.6f%n", putGreeks.theta);
            System.out.printf("Rho: %.6f%n", putGreeks.rho);

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error loading native library: " + e.getMessage());
            System.err.println("Please ensure the QuantLibJNId.dll is in the java.library.path");
        } catch (Exception e) {
            System.err.println("Error during calculation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}