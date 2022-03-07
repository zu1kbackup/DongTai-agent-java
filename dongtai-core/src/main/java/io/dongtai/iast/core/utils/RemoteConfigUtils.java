package io.dongtai.iast.core.utils;

import io.dongtai.iast.common.entity.performance.PerformanceMetrics;
import io.dongtai.iast.common.enums.MetricsKey;
import io.dongtai.iast.core.utils.json.GsonUtils;
import io.dongtai.log.DongTaiLog;
import io.github.resilience4j.core.metrics.Metrics;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

/**
 * 远端配置工具类
 *
 * @author chenyi
 * @date 2022/3/3
 */
public class RemoteConfigUtils {

    private RemoteConfigUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 全局配置
     */
    private static Boolean enableAutoFallback;
    /**
     * 高频hook限流相关配置
     */
    private static Double hookLimitTokenPerSecond;
    private static Double hookLimitInitBurstSeconds;
    /**
     * 性能熔断阈值相关配置
     */
    private static Integer maxRiskMetricsCount;
    private static List<PerformanceMetrics> performanceLimitRiskThreshold;
    private static List<PerformanceMetrics> performanceLimitMaxThreshold;


    /**
     * 同步远程配置
     *
     * @param remoteConfig 远程配置内容字符串
     */
    public static void syncRemoteConfig(String remoteConfig) {
        JSONObject configJson = new JSONObject(remoteConfig);
        enableAutoFallback = configJson.getBoolean("enableAutoFallback");
        hookLimitTokenPerSecond = configJson.getDouble("hookLimitTokenPerSecond");
        hookLimitInitBurstSeconds = configJson.getDouble("hookLimitInitBurstSeconds");
        maxRiskMetricsCount = configJson.getInt("maxRiskMetricsCount");
        JSONObject perfLimMaxThresholdJson = configJson.getJSONObject("performanceLimitMaxThreshold");
        performanceLimitMaxThreshold = buildPerformanceMetricsFromJson(perfLimMaxThresholdJson.toString());
        JSONObject perfLimRiskThresholdJson = configJson.getJSONObject("performanceLimitRiskThreshold");
        performanceLimitRiskThreshold = buildPerformanceMetricsFromJson(perfLimRiskThresholdJson.toString());
    }

    /**
     * 将json转化为List<PerformanceMetrics>类型
     * @param json
     * @return
     */
    private static List<PerformanceMetrics> buildPerformanceMetricsFromJson(String json){
        List<PerformanceMetrics> performanceMetricsList  = new ArrayList<>();
        JSONObject jsonObject = new JSONObject(json);
        Set<String> keySet = jsonObject.keySet();
        try {
            for (MetricsKey each : MetricsKey.values()) {
                PerformanceMetrics metrics = new PerformanceMetrics();
                if (keySet.contains(each.getKey())) {
                    DongTaiLog.info(each.getKey());
                    String metricsValueJson = jsonObject.get(each.getKey()).toString();
                    metrics.setMetricsKey(each);
                    metrics.setMetricsValue(GsonUtils.toObject(metricsValueJson, each.getValueType()));
                    performanceMetricsList.add(metrics);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return performanceMetricsList;
    }

    // *************************************************************
    // 全局配置
    // *************************************************************

    /**
     * 是否允许自动降级
     */
    public static Boolean enableAutoFallback() {
        if (enableAutoFallback == null) {
            enableAutoFallback = PropertyUtils.getRemoteSyncLocalConfig("global.autoFallback", Boolean.class, true);
        }
        return enableAutoFallback;
    }

    // *************************************************************
    // 高频hook限流相关配置
    // *************************************************************

    /**
     * 高频hook限流-每秒获得令牌数
     */
    public static Double getHookLimitTokenPerSecond(Properties cfg) {
        if (hookLimitTokenPerSecond == null) {
            hookLimitTokenPerSecond = PropertyUtils.getRemoteSyncLocalConfig("hookLimit.tokenPerSecond", Double.class, 5000.0, cfg);
        }
        return hookLimitTokenPerSecond;
    }

    /**
     * 高频hook限流-初始预放置令牌时间
     */
    public static double getHookLimitInitBurstSeconds(Properties cfg) {
        if (hookLimitInitBurstSeconds == null) {
            hookLimitInitBurstSeconds = PropertyUtils.getRemoteSyncLocalConfig("hookLimit.initBurstSeconds", Double.class, 10.0, cfg);
        }
        return hookLimitInitBurstSeconds;
    }

    // *************************************************************
    // 性能熔断阈值相关配置
    // *************************************************************

    /**
     * 获取性能限制风险阈值
     */
    public static List<PerformanceMetrics> getPerformanceLimitRiskThreshold(Properties cfg) {
        if (performanceLimitRiskThreshold == null) {
            performanceLimitRiskThreshold = buildPerformanceMetrics("performanceLimit.riskThreshold", cfg);
        }
        return performanceLimitRiskThreshold;
    }

    /**
     * 获取性能限制最大阈值
     */
    public static List<PerformanceMetrics> getPerformanceLimitMaxThreshold(Properties cfg) {
        if (performanceLimitMaxThreshold == null) {
            performanceLimitMaxThreshold = buildPerformanceMetrics("performanceLimit.maxThreshold", cfg);
        }
        return performanceLimitMaxThreshold;
    }

    /**
     * 获取不允许超过风险阈值的指标数量(0为不限制，达到阈值数时熔断)
     */
    public static Integer getMaxRiskMetricsCount(Properties cfg) {
        if (maxRiskMetricsCount == null) {
            maxRiskMetricsCount = PropertyUtils.getRemoteSyncLocalConfig("performanceLimit.maxRiskMetricsCount", Integer.class, 3, cfg);
        }
        return maxRiskMetricsCount;
    }

    public static void setMaxRiskMetricsCount(Integer maxRiskMetricsCount) {
        RemoteConfigUtils.maxRiskMetricsCount = maxRiskMetricsCount;
    }

    /**
     * 从配置文件中构建性能指标
     *
     * @param configPrefix 配置前缀
     * @param cfg          配置
     * @return {@link List}<{@link PerformanceMetrics}> 性能指标列表
     */
    private static List<PerformanceMetrics> buildPerformanceMetrics(String configPrefix, Properties cfg) {
        List<PerformanceMetrics> performanceMetricsList = new ArrayList<>();
        for (MetricsKey each : MetricsKey.values()) {
            final Object metricsValue = PropertyUtils.getRemoteSyncLocalConfig(String.format("%s.%s", configPrefix, each.getKey()),
                    Object.class, null, cfg);
            final PerformanceMetrics metrics = new PerformanceMetrics();
            metrics.setMetricsKey(each);
            if (metricsValue instanceof String) {
                try {
                    final Object bean = GsonUtils.toObject((String) metricsValue, each.getValueType());
                    if (bean != null) {
                        metrics.setMetricsValue(bean);
                        performanceMetricsList.add(metrics);
                    }
                } catch (Exception e) {
                    DongTaiLog.warn("invalid metrics value config,msg:{}", e.getMessage());
                }
            }
        }
        return performanceMetricsList;
    }
}
