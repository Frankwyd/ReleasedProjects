package com.quantlib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FXOptionTest {
    private static final double EPSILON = 1e-10;

    @Test
    public void testFXOptionBasicFunctionality() {
        System.out.println("Testing FXOption basic functionality...");

        // 测试参数
        int optionType = FXOption.CALL;
        double spot = 100.0;
        double strike = 105.0;
        double domesticRate = 0.05;
        double foreignRate = 0.03;
        double volatility = 0.15;
        int daysToMaturity = 365;

        try {
            FXOption option = new FXOption();

            // 测试期权定价
            double price = option.calculateFXOption(
                    optionType, spot, strike,
                    domesticRate, foreignRate,
                    volatility, daysToMaturity);
            System.out.println("Call Option Price: " + price);
            assertTrue(price > 0, "Option price should be positive");

            // 测试希腊字母
            FXOption.Greeks greeks = option.calculateFXOptionGreeks(
                    optionType, spot, strike,
                    domesticRate, foreignRate,
                    volatility, daysToMaturity);

            System.out.println("Greeks:");
            System.out.println("Delta: " + greeks.delta);
            System.out.println("Gamma: " + greeks.gamma);
            System.out.println("Vega: " + greeks.vega);
            System.out.println("Theta: " + greeks.theta);
            System.out.println("Rho: " + greeks.rho);

            // 基本验证
            assertTrue(greeks.delta >= 0 && greeks.delta <= 1, "Delta for call should be between 0 and 1");
            assertTrue(greeks.gamma > 0, "Gamma should be positive");
            assertTrue(greeks.vega > 0, "Vega should be positive");

        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testPutCallParity() {
        System.out.println("Testing Put-Call Parity...");

        // 测试参数
        double spot = 100.0;
        double strike = 100.0;
        double domesticRate = 0.05;
        double foreignRate = 0.03;
        double volatility = 0.15;
        int daysToMaturity = 365;

        try {
            FXOption option = new FXOption();

            // 计算看涨期权价格
            double callPrice = option.calculateFXOption(
                    FXOption.CALL, spot, strike,
                    domesticRate, foreignRate,
                    volatility, daysToMaturity);

            // 计算看跌期权价格
            double putPrice = option.calculateFXOption(
                    FXOption.PUT, spot, strike,
                    domesticRate, foreignRate,
                    volatility, daysToMaturity);

            // 验证买权-卖权平价关系
            double tau = daysToMaturity / 365.0;
            double lhs = callPrice - putPrice;
            double rhs = spot * Math.exp(-foreignRate * tau) - strike * Math.exp(-domesticRate * tau);

            System.out.println("Put-Call Parity Test:");
            System.out.println("LHS (C-P): " + lhs);
            System.out.println("RHS (S*e^(-rf*T) - K*e^(-rd*T)): " + rhs);

            assertEquals(lhs, rhs, EPSILON, "Put-Call parity should hold");

        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }
}
