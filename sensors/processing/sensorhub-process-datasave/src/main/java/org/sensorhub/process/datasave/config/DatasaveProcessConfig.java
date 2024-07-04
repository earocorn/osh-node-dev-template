package org.sensorhub.process.datasave.config;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.impl.datastore.view.ObsSystemDatabaseViewConfig;

import java.util.ArrayList;
import java.util.List;

public class DatasaveProcessConfig extends ProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo(desc = "Serial number or unique identifier")
    public String serialNumber = "001";

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IDataProducerModule.class)
    @DisplayInfo(label = "Input Module ID", desc = "Data-producer ID to read alarming data")
    public String inputModuleID;


    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IObsSystemDatabaseModule.class)
    @DisplayInfo(label = "Input Database ID",desc="Database to save when process is triggered")
    public String inputDatabaseID;

    @DisplayInfo(label = "Triggers", desc = "Observed properties and threshold values for triggers")
    public List<TriggerThresholdConfig> triggers;

    @DisplayInfo(label = "Time Before Trigger", desc = "Time in seconds before triggered value to save data")
    public double timeBeforeTrigger = 60;

}
