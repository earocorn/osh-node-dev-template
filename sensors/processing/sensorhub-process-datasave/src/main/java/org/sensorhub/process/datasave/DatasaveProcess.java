package org.sensorhub.process.datasave;

import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;
import org.sensorhub.process.datasave.config.TriggerThresholdConfig;
import org.sensorhub.process.datasave.test.AlarmRecorder;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatasaveProcess extends ExecutableProcessImpl implements ISensorHubProcess {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasave", "Data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    Quantity timeBeforeTrigger;
    IObsSystemDatabase inputDB;
    DatasaveProcessConfig config;

    public DatasaveProcess(DatasaveProcessConfig config, ISensorHub hub) {
        super(INFO);
        this.hub = hub;
        this.config = config;

        SWEHelper fac = new SWEHelper();
        assert hub != null;

        paramData.add("timeBeforeTrigger", timeBeforeTrigger = fac.createQuantity()
                .label("Time Before Trigger")
                .description("Time in seconds to save historical data")
                .value(config.timeBeforeTrigger)
                .build());

        try {
            var inputModule = hub.getModuleRegistry().getModuleById(config.inputModuleID);
            if(inputModule instanceof IProcessModule) {
                IProcessModule inputProcessModule = (IProcessModule) inputModule;
                for(Map.Entry<String, DataComponent> entry : inputProcessModule.getOutputDescriptors().entrySet()) {
                    for(TriggerThresholdConfig trigger : config.triggers) {
                        if(entry.getValue().getDefinition().equals(trigger.triggerObservedProperty)) {
                            inputData.add(entry.getValue().getName(), entry.getValue());
                            paramData.add(entry.getValue().getName() + "Threshold", entry.getValue());
                        }
                    }
                }
            }
            if(inputModule instanceof ISensorModule) {
                ISensorModule inputSensorModule = (ISensorModule) inputModule;
                for(Map.Entry<String, ? extends IStreamingDataInterface> entry : inputSensorModule.getOutputs().entrySet()) {
                    DataComponent entryDataRecord = entry.getValue().getRecordDescription();
                    for(TriggerThresholdConfig trigger : config.triggers) {
                        if(entryDataRecord.getDefinition().equals(trigger.triggerObservedProperty)) {
                            inputData.add(entryDataRecord.getName(), entryDataRecord);
                            paramData.add(entryDataRecord.getName() + "Threshold", entryDataRecord);
                        }
                        int recordSize = entryDataRecord.getComponentCount();
                        for(int i = 0; i < recordSize; i++) {
                            DataComponent component = entryDataRecord.getComponent(i);
                            if(component.getDefinition().equals(trigger.triggerObservedProperty)) {
                                inputData.add(component.getName(), component);
                                paramData.add(component.getName() + "Threshold", component);
                            }
                        }
                    }
                }
            }
            if(config.inputDatabase.includeFilter != null) {
                inputDB = config.inputDatabase.getFilteredView(hub);
            } else {
                inputDB = hub.getDatabaseRegistry().getObsDatabaseByModuleID(config.inputDatabase.sourceDatabaseId);
            }
            for(Map.Entry<DataStreamKey, IDataStreamInfo> entry : inputDB.getObservationStore().getDataStreams().entrySet()) {
                outputData.add(entry.getValue().getOutputName(), entry.getValue().getRecordStructure());
            }
        } catch (SensorHubException e) {
            throw new RuntimeException(e);
        }


        // TODO: Initialize inputs from "trigger field"
        // TODO: Initialize parameter from "trigger threshold" and "timeframe"
        // TODO: Initialize output from "observed properties"
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

    public boolean isTriggered() {
        int size = inputData.size();
        for(TriggerThresholdConfig trigger : config.triggers) {

            for(int i = 0; i < size; i++) {
                DataComponent inputComponent = inputData.getComponent(i);
                System.out.println(inputComponent.getData().getUnderlyingObject());
            }
        }
        return true;
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
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
