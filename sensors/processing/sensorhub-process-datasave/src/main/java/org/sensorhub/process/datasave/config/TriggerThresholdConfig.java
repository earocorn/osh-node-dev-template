package org.sensorhub.process.datasave.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.process.datasave.helpers.ComparisonType;

public class TriggerThresholdConfig {

    @DisplayInfo.Required
    @DisplayInfo(label = "Trigger's Observed Property", desc = "Observed property URI of ")
    public String triggerObservedProperty;

    @DisplayInfo.Required
    @DisplayInfo(label = "Comparison Type", desc = "Comparison operator to check whether value meets threshold")
    public ComparisonType comparisonType;

    @DisplayInfo.Required
    @DisplayInfo(label = "Trigger Threshold", desc = "Threshold to trigger data saving process")
    public String triggerThreshold;

}
