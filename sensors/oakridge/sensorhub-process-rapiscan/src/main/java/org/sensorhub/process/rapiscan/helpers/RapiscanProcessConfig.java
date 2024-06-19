package org.sensorhub.process.rapiscan.helpers;

import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.database.IDatabase;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.api.sensor.ISensorModule;

public class RapiscanProcessConfig extends ProcessConfig {

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(ISensorModule.class)
    @DisplayInfo(label = "Rapiscan Driver ID", desc = "Datasource to read occupancy data")
    public String rapiscanDriverID;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IDatabase.class)
    @DisplayInfo(label = "Input Database", desc = "Module ID of system database to query")
    public String databaseModuleID;

}
