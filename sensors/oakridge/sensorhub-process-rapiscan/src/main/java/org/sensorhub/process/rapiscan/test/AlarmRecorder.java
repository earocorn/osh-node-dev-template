package org.sensorhub.process.rapiscan.test;

import net.opengis.swe.v20.*;
import net.opengis.swe.v20.Boolean;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.vast.data.AbstractDataBlock;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AlarmRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    IDatabaseRegistry registry;
    Count countInput;
    Boolean alarmInput;
    Text dbInputParam;
    Count numRecords;
    DataRecord entry;

    public AlarmRecorder() {
        super(INFO);

        SWEHelper fac = new SWEHelper();

        inputData.add("count", countInput = fac.createCount()
                .label("Arbitrary Count")
                .value(0)
                .definition(SWEHelper.getPropertyUri("Count"))
                .build());

        inputData.add("alarm", alarmInput = fac.createBoolean()
                .label("Alarm Status")
                .definition(SWEHelper.getPropertyUri("AlarmStatus"))
                .build());

        outputData.add("entry", entry = fac.createRecord()
                .updatable(true)
                .label("Entry")
                .description("Data Entry")
                .definition(SWEHelper.getPropertyUri("Entry"))
                .addField("numRecords", numRecords = fac.createCount()
                        .label("Num Records")
                        .id("numRecords")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .value(0)
                        .build())
                .addField("records", fac.createArray()
                        .label("Data Records")
                        .withVariableSize("numRecords")
                        .withElement("record", fac.createRecord()
                                .label("Data Record")
                                .description("Recorded data")
                                .addField("sampleTime", fac.createQuantity()
                                        .dataType(DataType.DOUBLE)
                                        .value(0)
                                        .label("Sample Time")
                                        .description("Time of data collection"))
                                .addField("count", fac.createCount()
                                        .label("Arbitrary Count")
                                        .value(0)
                                        .definition(SWEHelper.getPropertyUri("Count")))
                                .addField("alarm", fac.createBoolean()
                                        .label("Alarm Status")
                                        .value(false)
                                        .definition(SWEHelper.getPropertyUri("AlarmStatus")))
                                .build())
                        .build())
                .build());

        paramData.add("databaseInput", dbInputParam = fac.createText()
                .label("Database Input")
                .description("Database to query historical results")
                .definition(SWEHelper.getPropertyUri("Database"))
                .value("")
                .build());
    }

    @Override
    public void execute() throws ProcessException {
        // Only populate data entry if alarm is triggered
        if(inputData.getComponent("alarm").getData().getBooleanValue()) {
            System.out.println("Results from past 5 seconds");

            List<IObsData> alarmingData;

            Instant now = Instant.now();
            Instant before = now.minusSeconds(5);

            // Get past 5 seconds of data when alarm is triggered
            alarmingData = getDataFromInterval(before, now, dbInputParam.getData().getStringValue());

            int index = 1;

            numRecords.getData().setIntValue(0, alarmingData.size());

            for(IObsData obsData : alarmingData) {
                AbstractDataBlock[] dataBlock = (AbstractDataBlock[]) obsData.getResult().getUnderlyingObject();
                System.out.println(dataBlock);
                entry.getData().setDoubleValue(index++, dataBlock[0].getDoubleValue());
                entry.getData().setIntValue(index++, dataBlock[1].getIntValue());
                entry.getData().setBooleanValue(index++, dataBlock[2].getBooleanValue());
                getLogger().debug("DATA: {} -> {}", dataBlock, entry);
            }

        }
    }

    public List<IObsData> getDataFromInterval(Instant start, Instant end, String dbModuleID) {
        ObsFilter filter = new ObsFilter.Builder()
                .withPhenomenonTimeDuring(start, end).build();

        var obsDb = getRegistry()
                //"29f2b677-95b1-4499-8e5b-459839ec3eb6"
                .getObsDatabaseByModuleID(dbModuleID)
                .getObservationStore()
                .select(filter);

        return obsDb.collect(Collectors.toList());
    }

    public void publishDataList(List<IObsData> blockList, String dbModuleID) {
        var db = getRegistry().getObsDatabaseByModuleID(dbModuleID);
        if(!blockList.isEmpty()) {

            for (IObsData obsData : blockList) {
                db.getObservationStore()
                        .add(obsData);
            }
        }
    }

    public IDatabaseRegistry getRegistry() {
        if(hub != null) {
            registry = hub.getDatabaseRegistry();
        }
        return registry;
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
