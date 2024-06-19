package org.sensorhub.process.datasave.helpers;

import com.sun.source.util.Trees;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.ISensorHubConfig;
import org.sensorhub.api.config.DisplayInfo;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessConfig;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.impl.processing.ISensorHubProcess;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DatasaveProcessConfig extends ProcessConfig {

    @DisplayInfo(label = "Database Input ID", desc = "ID of system database module to query")
    public String databaseInputID;

    @DisplayInfo(label = "Input Name", desc = "Input to watch for threshold")
    public String inputName;

    @DisplayInfo(label = "Threshold Value", desc = "Value to trigger data save action")
    public String thresholdValue;

    @DisplayInfo.Required
    @DisplayInfo.FieldType(DisplayInfo.FieldType.Type.MODULE_ID)
    @DisplayInfo.ModuleType(IModule.class)
    @DisplayInfo(label = "System UIDs", desc = "Unique IDs of system drivers included in aggregate process")
    public String processes;



}
