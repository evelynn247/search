package com.biyao.search.as.server.common.util;

/**
 * @description: 数据处理器
 * @author: wuzhenwei
 * @date: 2017年6月5日 下午16:54:45
 * @version: V1.0.0
 * TODO: 实例化使用Cache记录统计信息作为中间值，优化性能
 */
public class DataProcessor {
    //精度
    private static double e =1E-5;

    public static double sum(double[] rawData) {
        if (rawData.length == 0) {
            return 0.0;
        }
        double res = 0.0;
        for (int i=0;i<rawData.length;i++) {
            res += rawData[i];
        }
        return res;
    }

    public static double mean(double[] rawData) {
        int size = rawData.length;
        if (size == 0) {
            return 0.0;
        }
        return sum(rawData) / size;
    }

    public static double max(double[] rawData) {
        if (rawData.length == 0) {
            return 0.0;
        }
        double res = rawData[0];
        for (int i=0;i<rawData.length;i++) {
            if (rawData[i] > res) {
                res = rawData[i];
            }
        }
        return res;
    }

    public static double min(double[] rawData) {
        if (rawData.length == 0) {
            return 0.0;
        }
        double res = rawData[0];
        for (int i=0;i<rawData.length;i++) {
            if (rawData[i] < res) {
                res = rawData[i];
            }
        }
        return res;
    }

    protected static double[] logP(double[] rawData, double elementPlus, double base) {
        int size = rawData.length;
        double[] resData = new double[size];
        for (int i=0;i<size;i++) {
            resData[i] = Math.log(rawData[i] + elementPlus) / Math.log(base);
        }
        return resData;
    }

    /* 用于非负数组 */
    public static double[] logMax1(double[] rawData, double elementPlus) {
        double m = max(rawData);
        int size = rawData.length;
        double[] resData = new double[size];
        if (m == min(rawData)) {
            // 同时处理了数组长度为0的情况
            return resData;
        }
        resData = logP(rawData, elementPlus, m + elementPlus);
        return resData;
    }

    public static double[] dscIdx(double[] rawData) {
        int size = rawData.length;
        double[] resData = new double[size];

        for (int i=0;i<size;i++) {
            double t = 1.0;
            for (int j=0;j<size;j++) {
                if (rawData[i] < rawData[j]) {
                    t += 1.0;
                }
            }
            resData[i] = t;
        }

        return resData;
    }

    public static double[] ascIdx(double[] rawData) {
        int size = rawData.length;
        double[] resData = new double[size];

        for (int i=0;i<size;i++) {
            double t = 1.0;
            for (int j=0;j<size;j++) {
                if (rawData[i] > rawData[j]) {
                    t += 1.0;
                }
            }
            resData[i] = t;
        }

        return resData;
    }

    public static double[] ascIdx_LogMax1(double[] rawData) {
        double[] resData = ascIdx(rawData);
        resData = logMax1(resData, 0.0);
        return resData;
    }

    public static double[] cumsumPercent(double[] rawData) {
        int size = rawData.length;
        double[] resData = new double[size];
        double s = sum(rawData);
        if (s < e) {
            return resData;
        }

        for (int i=0;i<size;i++) {
            resData[i] = rawData[i] * 100.0 / s;
        }

        return resData;
    }

    public static double[] div1p(double[] numerator, double[] denominator) {
        int size = numerator.length;
        if (size > denominator.length) {
            // 应该报错？
            size = denominator.length;
        }
        double[] resData = new double[size];

        for (int i=0;i<size;i++) {
            resData[i] = numerator[i] / (denominator[i] + 1.0);
        }

        return resData;
    }

    //    仅用于非负数
    public static double[] rate(double[] numerator, double[] denominator) {
        int size = numerator.length;
        if (size > denominator.length) {
            // 应该报错？
            size = denominator.length;
        }
        double[] resData = new double[size];

        for (int i=0;i<size;i++) {
            resData[i] = denominator[i] > e? numerator[i] / denominator[i] : 0.0;
        }

        return resData;

    }

    protected static double[] variation(double[] subtractor, double[] minuend, int subWeight, int minWeight, double avg) {
        int size = subtractor.length;
//        if (size > minuend.length) size = minuend.length; // 同上注释：应该报错？
        double[] resData = new double[size];
        int weightGap = Math.abs(subWeight - minWeight);
        double denominator = avg * Math.max(weightGap, 1);
        for (int i=0;i<size;i++) {
            resData[i] = (subtractor[i] * subWeight - minuend[i] * minWeight) / denominator;
        }
        return resData;
    }

    public static double[] variationByDays(double[] subtractor, double[] minuend, int subWeight, int minWeight) {
        double avg = mean(minuend) + 1.0;
        return variation(subtractor, minuend, subWeight, minWeight, avg);
    }

    public static double[] minMaxNor(double[] rawData) {
        double high = max(rawData);
        double low = min(rawData);
        int size = rawData.length;
        double[] resData= new double[size];
        if (high <= low + e) {
            return resData;
        }
        double gap = high - low;
        for (int i=0;i<size;i++) {
            resData[i] =(rawData[i] - low) / gap;
        }
        return resData;
    }

    public static double[] minMax_LogMax1(double[] rawData) {
        double[] resData = minMaxNor(rawData);
        for (int i=0;i<resData.length;i++) {
            resData[i] = Math.log(resData[i] + 1.0) / Math.log(2);
        }
        return resData;
    }

    // 将dscIdx数据转成ascIdx数据，避免再进行一次两重循环
    public static double[] dscIdx2logMax1(double[] dscData) {
        double m = dscData.length;
        double[] resData = new double[(int)m];
        if (m < 2) {
            return resData;
        }
        for (int i=0;i<m;i++) {
            resData[i] = Math.log(m + 1 - dscData[i]) / Math.log(m);
        }
        return resData;
    }

//    public static double[] variation2Days

}