package org.sensorhub.process.datasave.processes;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducerModule;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.process.datasave.DatasaveTriggerComponent;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;

import org.sensorhub.process.datasave.helpers.ComparisonType;
import org.sensorhub.utils.Async;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockParallel;
import org.vast.data.DataBlockString;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Class built with inspiration from StreamDataSource class
 */
public class DatasaveProcess extends ExecutableProcessImpl implements ISensorHubProcess {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasave", "Data saving process", null, DatasaveProcess.class);
    ISensorHub hub;
    Quantity timeBeforeTrigger;
    IObsSystemDatabase inputDB;
    DatasaveProcessConfig config;

    public static final String INPUT_MODULE_ID_PARAM = "inputModuleID";
    Text inputModuleIDParam;
    String inputModuleID;
    public static final String INPUT_DATABASE_ID_PARAM = "inputDatabaseID";
    Text inputDatabaseIDParam;
    String inputDatabaseID;
    public static final String TRIGGERS_PARAM = "triggersRecord";
    DataRecord triggersParam;
    HashMap<String, DatasaveTriggerComponent> triggersMap;
    public static final String SAVE_TIME_PARAM = "saveTime";
    Quantity saveTimeParam;
    double saveTime;

    public DatasaveProcess() {
        super(INFO);

        triggersMap = new HashMap<>();

        SWEHelper fac = new SWEHelper();

        inputModuleIDParam = fac.createText()
            .label("Input Module ID")
            .definition(SWEHelper.getPropertyUri("InputModuleID"))
            .build();
        paramData.add(INPUT_MODULE_ID_PARAM, inputModuleIDParam);

        inputDatabaseIDParam = fac.createText()
                .label("Input Database ID")
                .definition(SWEHelper.getPropertyUri("InputDatabaseID"))
                .build();
        paramData.add(INPUT_DATABASE_ID_PARAM, inputDatabaseIDParam);

        triggersParam = fac.createRecord()
                .updatable(true)
                .label("Triggers Record")
                .definition(SWEHelper.getPropertyUri("TriggersRecord"))
                .addField("numTriggers", fac.createCount()
                        .id("numTriggers")
                        .label("Num Triggers")
                        .definition(SWEHelper.getPropertyUri("NumTriggers"))
                        .build())
                .addField("triggers", fac.createArray()
                        .label("Triggers")
                        .definition(SWEHelper.getPropertyUri("Triggers"))
                        .withVariableSize("numTriggers")
                        .withElement("trigger", fac.createRecord()
                                .label("Trigger")
                                .definition(SWEHelper.getPropertyUri("Trigger"))
                                .addField("observedProperty", fac.createText()
                                        .label("Observed Property")
                                        .definition(SWEHelper.getPropertyUri("ObservedProperty"))
                                        .build())
                                .addField("comparisonType", fac.createCategory()
                                        .label("Comparison Type")
                                        .definition(SWEHelper.getPropertyUri("ComparisonType"))
                                        .addAllowedValues(ComparisonType.class)
                                        .build())
                                .addField("threshold", fac.createText()
                                        .label("Threshold")
                                        .definition(SWEHelper.getPropertyUri("Threshold"))
                                        .build())
                                .build())
                        .build())
                .build();
        paramData.add(TRIGGERS_PARAM, triggersParam);

        saveTimeParam = fac.createQuantity()
                .label("Save Time")
                .description("Time in seconds to save before trigger")
                .build();
        paramData.add(SAVE_TIME_PARAM, saveTimeParam);
    }

    @Override
    public void notifyParamChange() {
        // check that data sources exist on producer and input db
        // create inputs for triggers and outputs from input db
        inputModuleID = inputModuleIDParam.getData().getStringValue();
        inputDatabaseID = inputDatabaseIDParam.getData().getStringValue();
        // TODO: Upon param change, update the hashmap values
        triggersParam.getComponent("triggers");
        var triggerData = ((DataBlockString) triggersParam.getComponent("triggers").getData()).getUnderlyingObject();
        int index = 0;
        while(index < triggerData.length-1) {
            triggersMap.put(triggerData[index], new DatasaveTriggerComponent(
                    triggerData[index++],
                    ComparisonType.valueOf(triggerData[index++]),
                    triggerData[index++]
            ));
        }
        saveTime = saveTimeParam.getData().getDoubleValue();

        if(inputModuleID != null && inputDatabaseID != null) {
            try {
                // TODO: Initialize inputs from "triggers"
                // TODO: Initialize output from "input database"
                Async.waitForCondition(this::checkTriggers, 500, 10000);
                Async.waitForCondition(this::checkDatabase, 500, 10000);
            } catch (TimeoutException e) {
                if(processInfo == null) {
                    throw new IllegalStateException("Input module " + inputModuleID + " not found", e);
                } else {
                    throw new IllegalStateException("Input module " + inputModuleID + " has no datastreams", e);
                }
            }
        }
    }

    private boolean checkTriggers() {
        // Check triggers array to match observed property with threshold and comparison type. Possibly use hashmap to get values

        var db = hub.getDatabaseRegistry().getFederatedDatabase();
        BigId internalID = null;
        try {
            
            if(hub.getModuleRegistry().getModuleById(inputModuleID) instanceof IDataProducerModule) {
                IDataProducerModule dataProducer = (IDataProducerModule) hub.getModuleRegistry().getModuleById(inputModuleID);
                var inputModule = db.getSystemDescStore().getCurrentVersionEntry(dataProducer.getUniqueIdentifier());
                if(inputModule == null) {
                    return false;
                }
                internalID = inputModule.getKey().getInternalID();
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }
        if(internalID == null) {
            return false;
        }
        int numDsBefore = inputData.size();
        inputData.clear();
        db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(internalID)
                .withCurrentVersion()
                .withObservedProperties(triggersMap.keySet())
                .build())
            .forEach(ds -> {
                triggersMap.get(ds.getRecordStructure().getDefinition()).setRecordDescription(ds.getRecordStructure().copy());
                inputData.add(ds.getOutputName(), ds.getRecordStructure().copy());
            });

        return !inputData.isEmpty() && inputData.size() == numDsBefore;
    }

    private boolean checkDatabase() {
        var db = hub.getDatabaseRegistry().getFederatedDatabase();
        var dbEntry = db.getSystemDescStore().getCurrentVersionEntry(inputDatabaseID);
        if(dbEntry == null) {
            return false;
        }

        int numDsBefore = outputData.size();
        outputData.clear();
        db.getDataStreamStore().select(new DataStreamFilter.Builder()
                .withSystems(dbEntry.getKey().getInternalID())
                .withCurrentVersion()
                .build())
            .forEach(ds -> {
                outputData.add(ds.getOutputName(), ds.getRecordStructure().copy());
            });

        return !outputData.isEmpty() && outputData.size() == numDsBefore;
    }

    public boolean isTriggered() {
        // TODO: Go through triggers and check using param's observed property if the DataBlock from getTriggerData() == inputData.getComponent(triggerComponent.getName())
//        for(DataComponent component :  triggersParam.getComponent("triggers") ) {
//            // TODO: Test this
//            if(dataBlock == inputData.getComponent(triggersMap.get()))
//        }
        for(DatasaveTriggerComponent triggerComponent : triggersMap.values()) {
            if(paramData.getComponent(triggerComponent.getThresholdName()).getData()  == inputData.getComponent(triggerComponent.getName()).getData()) {
                return true;
            }
        }
        return false;
    }

    private List<IObsData> getPastData(String outputName) {
        Instant now = Instant.now();
        Instant before = now.minusSeconds((long) timeBeforeTrigger.getData().getDoubleValue());

        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();
        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(before, now)
                .build();

        return inputDB.getObservationStore().select(filter).collect(Collectors.toList());
    }

    @Override
    public void execute() throws ProcessException {
        // TODO: Publish data to output from input database when trigger threshold is met
        // Test trigger threshold first
        // Then if any are true, publish data
        if(isTriggered()) {
            int size = outputData.size();
            for(int i = 0; i < size; i++) {
                DataComponent item = outputData.getComponent(i);
                String itemName = item.getName();

                for(IObsData data : getPastData(itemName)) {
                    item.setData(data.getResult());
                    try {
                        publishData();
                        publishData(itemName);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
    }
    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
