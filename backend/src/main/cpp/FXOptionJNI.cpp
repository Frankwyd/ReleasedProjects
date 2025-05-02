#include "com_quantlib_FXOption.h"
#include <ql/quantlib.hpp>

using namespace QuantLib;

JNIEXPORT jdouble JNICALL Java_com_quantlib_FXOption_calculateFXOption
  (JNIEnv* env, jobject obj, jint optionType, jdouble spot, jdouble strike,
   jdouble domesticRate, jdouble foreignRate, jdouble volatility, jint daysToMaturity) {
    
    try {
        // 设置计算日期
        Calendar calendar = TARGET();
        Date todaysDate = Date::todaysDate();
        Settings::instance().evaluationDate() = todaysDate;
        
        // 设置期权参数
        Option::Type type = (optionType == 1) ? Option::Call : Option::Put;
        Date maturity = todaysDate + Period(daysToMaturity, Days);
        
        // 构建收益率曲线
        auto domesticRTS = boost::shared_ptr<YieldTermStructure>(
            new FlatForward(todaysDate, domesticRate, Actual365Fixed()));
        auto foreignRTS = boost::shared_ptr<YieldTermStructure>(
            new FlatForward(todaysDate, foreignRate, Actual365Fixed()));
            
        // 构建波动率曲线
        auto volTS = boost::shared_ptr<BlackVolTermStructure>(
            new BlackConstantVol(todaysDate, calendar, volatility, Actual365Fixed()));
            
        // 构建报价对象
        auto spot_quote = boost::shared_ptr<Quote>(new SimpleQuote(spot));
        
        // 构建BSM过程
        auto bsProcess = boost::shared_ptr<BlackScholesMertonProcess>(
            new BlackScholesMertonProcess(
                Handle<Quote>(spot_quote),
                Handle<YieldTermStructure>(foreignRTS),
                Handle<YieldTermStructure>(domesticRTS),
                Handle<BlackVolTermStructure>(volTS)));
                
        // 创建期权对象
        auto exercise = boost::shared_ptr<Exercise>(new EuropeanExercise(maturity));
        auto payoff = boost::shared_ptr<StrikedTypePayoff>(
            new PlainVanillaPayoff(type, strike));
            
        VanillaOption option(payoff, exercise);
        
        // 设置定价引擎
        option.setPricingEngine(boost::shared_ptr<PricingEngine>(
            new AnalyticEuropeanEngine(bsProcess)));
            
        return option.NPV();
    }
    catch (const std::exception& e) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
        return 0.0;
    }
}

JNIEXPORT jobject JNICALL Java_com_quantlib_FXOption_calculateFXOptionGreeks
  (JNIEnv* env, jobject obj, jint optionType, jdouble spot, jdouble strike,
   jdouble domesticRate, jdouble foreignRate, jdouble volatility, jint daysToMaturity) {
    
    try {
        // 设置计算日期
        Calendar calendar = TARGET();
        Date todaysDate = Date::todaysDate();
        Settings::instance().evaluationDate() = todaysDate;
        
        // 设置期权参数
        Option::Type type = (optionType == 1) ? Option::Call : Option::Put;
        Date maturity = todaysDate + Period(daysToMaturity, Days);
        
        // 构建收益率曲线
        auto domesticRTS = boost::shared_ptr<YieldTermStructure>(
            new FlatForward(todaysDate, domesticRate, Actual365Fixed()));
        auto foreignRTS = boost::shared_ptr<YieldTermStructure>(
            new FlatForward(todaysDate, foreignRate, Actual365Fixed()));
            
        // 构建波动率曲线
        auto volTS = boost::shared_ptr<BlackVolTermStructure>(
            new BlackConstantVol(todaysDate, calendar, volatility, Actual365Fixed()));
            
        // 构建报价对象
        auto spot_quote = boost::shared_ptr<Quote>(new SimpleQuote(spot));
        
        // 构建BSM过程
        auto bsProcess = boost::shared_ptr<BlackScholesMertonProcess>(
            new BlackScholesMertonProcess(
                Handle<Quote>(spot_quote),
                Handle<YieldTermStructure>(foreignRTS),
                Handle<YieldTermStructure>(domesticRTS),
                Handle<BlackVolTermStructure>(volTS)));
                
        // 创建期权对象
        auto exercise = boost::shared_ptr<Exercise>(new EuropeanExercise(maturity));
        auto payoff = boost::shared_ptr<StrikedTypePayoff>(
            new PlainVanillaPayoff(type, strike));
            
        VanillaOption option(payoff, exercise);
        
        // 设置定价引擎
        option.setPricingEngine(boost::shared_ptr<PricingEngine>(
            new AnalyticEuropeanEngine(bsProcess)));
        
        // 计算希腊字母
        double delta = option.delta();
        double gamma = option.gamma();
        double vega = option.vega();
        double theta = option.theta();
        double rho = option.rho();
        
        // 创建Greeks对象
        jclass greeksClass = env->FindClass("com/quantlib/FXOption$Greeks");
        jobject greeks = env->NewObject(greeksClass, 
            env->GetMethodID(greeksClass, "<init>", "()V"));
            
        // 设置字段值
        jfieldID fid;
        fid = env->GetFieldID(greeksClass, "delta", "D");
        env->SetDoubleField(greeks, fid, delta);
        
        fid = env->GetFieldID(greeksClass, "gamma", "D");
        env->SetDoubleField(greeks, fid, gamma);
        
        fid = env->GetFieldID(greeksClass, "vega", "D");
        env->SetDoubleField(greeks, fid, vega);
        
        fid = env->GetFieldID(greeksClass, "theta", "D");
        env->SetDoubleField(greeks, fid, theta);
        
        fid = env->GetFieldID(greeksClass, "rho", "D");
        env->SetDoubleField(greeks, fid, rho);
        
        return greeks;
    }
    catch (const std::exception& e) {
        env->ThrowNew(env->FindClass("java/lang/RuntimeException"), e.what());
        return nullptr;
    }
} 