package com.aelion.aero.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Aligns with Aelion Cloud {@code RestGroupScaling}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class GroupScaling {

    private Boolean enabled;
    private int minInstances;
    private int maxInstances;
    private Integer minNonEmpty;
    private Integer targetPlayersPerServer;
    private String scalingMetric;
    private Integer scaleUpThreshold;
    private Integer scaleDownThreshold;
    private Integer evaluationPeriod;
    private int cooldownPeriod;
    private Integer scaleUpIncrement;
    private Integer scaleDownIncrement;
    private List<ScalingSchedule> schedule;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(int minInstances) {
        this.minInstances = minInstances;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public Integer getMinNonEmpty() {
        return minNonEmpty;
    }

    public void setMinNonEmpty(Integer minNonEmpty) {
        this.minNonEmpty = minNonEmpty;
    }

    public Integer getTargetPlayersPerServer() {
        return targetPlayersPerServer;
    }

    public void setTargetPlayersPerServer(Integer targetPlayersPerServer) {
        this.targetPlayersPerServer = targetPlayersPerServer;
    }

    public String getScalingMetric() {
        return scalingMetric;
    }

    public void setScalingMetric(String scalingMetric) {
        this.scalingMetric = scalingMetric;
    }

    public Integer getScaleUpThreshold() {
        return scaleUpThreshold;
    }

    public void setScaleUpThreshold(Integer scaleUpThreshold) {
        this.scaleUpThreshold = scaleUpThreshold;
    }

    public Integer getScaleDownThreshold() {
        return scaleDownThreshold;
    }

    public void setScaleDownThreshold(Integer scaleDownThreshold) {
        this.scaleDownThreshold = scaleDownThreshold;
    }

    public Integer getEvaluationPeriod() {
        return evaluationPeriod;
    }

    public void setEvaluationPeriod(Integer evaluationPeriod) {
        this.evaluationPeriod = evaluationPeriod;
    }

    public int getCooldownPeriod() {
        return cooldownPeriod;
    }

    public void setCooldownPeriod(int cooldownPeriod) {
        this.cooldownPeriod = cooldownPeriod;
    }

    public Integer getScaleUpIncrement() {
        return scaleUpIncrement;
    }

    public void setScaleUpIncrement(Integer scaleUpIncrement) {
        this.scaleUpIncrement = scaleUpIncrement;
    }

    public Integer getScaleDownIncrement() {
        return scaleDownIncrement;
    }

    public void setScaleDownIncrement(Integer scaleDownIncrement) {
        this.scaleDownIncrement = scaleDownIncrement;
    }

    public List<ScalingSchedule> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<ScalingSchedule> schedule) {
        this.schedule = schedule;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class ScalingSchedule {
        private String time;
        private int instances;

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public int getInstances() {
            return instances;
        }

        public void setInstances(int instances) {
            this.instances = instances;
        }
    }
}
