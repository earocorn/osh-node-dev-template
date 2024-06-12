package org.sensorhub.process.rapiscan.test;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.IDataStreamInfo;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.api.system.SystemId;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.utils.rad.RADHelper;
import org.vast.data.AbstractDataBlock;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.data.DataBlockParallel;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AlarmRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("alarmrecorder", "Alarm data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    IDatabaseRegistry registry;
    DataRecord occupancyInput;
    Text dbInputParam;
    Count numNeutronEntries;
    Count numGammaEntries;
    DataRecord neutronEntry;
    DataRecord gammaEntry;

    enum EntryType {
        NEUTRON, GAMMA
    }

    public AlarmRecorder() {
        super(INFO);

        RADHelper radHelper = new RADHelper();

        inputData.add("occupancy", occupancyInput = radHelper.createRecord()
                .label("Occupancy")
                .definition(RADHelper.getRadUri("occupancy"))
                .addField("Timestamp", radHelper.createPrecisionTimeStamp())
                .addField("OccupancyCount", radHelper.createOccupancyCount())
                .addField("StartTime", radHelper.createOccupancyStartTime())
                .addField("EndTime", radHelper.createOccupancyEndTime())
                .addField("NeutronBackground", radHelper.createNeutronBackground())
                .addField("GammaAlarm",
                        radHelper.createBoolean()
                                .name("gamma-alarm")
                                .label("Gamma Alarm")
                                .definition(RADHelper.getRadUri("gamma-alarm")))
                .addField("NeutronAlarm",
                        radHelper.createBoolean()
                                .name("neutron-alarm")
                                .label("Neutron Alarm")
                                .definition(RADHelper.getRadUri("neutron-alarm")))
                .build());

        outputData.add("neutronEntry", neutronEntry = radHelper.createRecord()
                        .label("Neutron Scan")
                        .definition(RADHelper.getRadUri("neutron-scan"))
                        .addField("SamplingTime", radHelper.createPrecisionTimeStamp())
                        .addField("Neutron1", radHelper.createNeutronGrossCount())
                        .addField("Neutron2", radHelper.createNeutronGrossCount())
                        .addField("Neutron3", radHelper.createNeutronGrossCount())
                        .addField("Neutron4", radHelper.createNeutronGrossCount())
                        .addField("AlarmState",
                                radHelper.createCategory()
                                        .name("Alarm")
                                        .label("Alarm")
                                        .definition(RADHelper.getRadUri("alarm"))
                                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Neutron High"))
                        .build());

        outputData.add("gammaEntry", gammaEntry = radHelper.createRecord()
                        .label("Gamma Scan")
                        .definition(RADHelper.getRadUri("gamma-scan"))
                        .addField("Sampling Time", radHelper.createPrecisionTimeStamp())
                        .addField("Gamma1", radHelper.createGammaGrossCount())
                        .addField("Gamma2", radHelper.createGammaGrossCount())
                        .addField("Gamma3", radHelper.createGammaGrossCount())
                        .addField("Gamma4", radHelper.createGammaGrossCount())
                        .addField("Alarm State",
                                radHelper.createCategory()
                                        .name("Alarm")
                                        .label("Alarm")
                                        .definition(RADHelper.getRadUri("alarm"))
                                        .addAllowedValues("Alarm", "Background", "Scan", "Fault - Gamma High", "Fault - Gamma Low"))
                        .build());

        paramData.add("databaseInput", dbInputParam = radHelper.createText()
                .label("Database Input")
                .description("Database to query historical results")
                .definition(SWEHelper.getPropertyUri("Database"))
                .value("")
                .build());
    }

    @Override
    public void execute() throws ProcessException {
        // Only populate data entry if alarm is triggered
        if(occupancyInput.getComponent("GammaAlarm").getData().getBooleanValue()) {
            System.out.println("Results from past 10 seconds");

            List<IObsData> alarmingData;

            Instant now = Instant.now();
            Instant before = now.minusSeconds(5);

            // Get past 5 seconds of data when alarm is triggered
            alarmingData = getDataFromInterval(before, now, dbInputParam.getData().getStringValue(), EntryType.GAMMA);

            try {
                publishEntryOutput(alarmingData, dbInputParam.getData().getStringValue(), EntryType.GAMMA);
            } catch (InterruptedException | DataStoreException e) {
                throw new RuntimeException(e);
            }
        }

        if(occupancyInput.getComponent("NeutronAlarm").getData().getBooleanValue()) {

        }
    }

    private List<IObsData> getDataFromInterval(Instant start, Instant end, String dbModuleID, EntryType entryType) {
        String outputName = "";
        switch(entryType) {
            case GAMMA: outputName = "Gamma Scan";
            case NEUTRON: outputName = "Neutron Scan";
        }
        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();

        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end).build();

        var obsDb = getRegistry()
                //"29f2b677-95b1-4499-8e5b-459839ec3eb6"
                .getObsDatabaseByModuleID(dbModuleID)
                .getObservationStore()
                .select(filter);

        return obsDb.collect(Collectors.toList());
    }

    private void publishEntryOutput(List<IObsData> blockList, String dbModuleID, EntryType entryType) throws InterruptedException, DataStoreException {
        var db = getRegistry().getObsDatabaseByModuleID(dbModuleID);
        DataRecord output = null;
        Count numOutputs = null;
        switch (entryType) {
            case GAMMA: {
                output = gammaEntry;
                numOutputs = numGammaEntries;
            }
            case NEUTRON: {
                output = neutronEntry;
                numOutputs = numNeutronEntries;
            }
        }
        // Set number of entries for DataArray
        numOutputs.getData().setIntValue(blockList.size());
        // Update size of entry array
        DataArrayImpl array = (DataArrayImpl) output.getComponent(1);
        array.updateSize();
        System.out.println(output.getComponent(1).getData().getClass()); // == DataBlockParallel
        if(!blockList.isEmpty()) {
            DataBlockParallel entryList = (DataBlockParallel) output.getComponent(1).getData();
            for (int i = 0; i < blockList.size(); i++) {
                entryList.getUnderlyingObject()[0].setDoubleValue(i, blockList.get(i).getResult().getDoubleValue(0));
                entryList.getUnderlyingObject()[1].setIntValue(i, blockList.get(i).getResult().getIntValue(1));
                entryList.getUnderlyingObject()[2].setIntValue(i, blockList.get(i).getResult().getIntValue(2));
                entryList.getUnderlyingObject()[3].setIntValue(i, blockList.get(i).getResult().getIntValue(3));
                entryList.getUnderlyingObject()[4].setIntValue(i, blockList.get(i).getResult().getIntValue(4));
                entryList.getUnderlyingObject()[5].setStringValue(i, blockList.get(i).getResult().getStringValue(5));
                System.out.println("EntryList: " + entryList + " DataBlock: " + blockList.get(i).getResult());
            }
            output.getComponent(1).setData(entryList);
            System.out.println("EntryList: " + entryList + " BlockItemList: " + blockList);
        }

        DataStreamInfo dsInfo = new DataStreamInfo.Builder()
                .withDescription("Alarm entries")
                .withName("Alarm Entries")
                .withRecordDescription(output)
                .build();
        db.getDataStreamStore().add(dsInfo);
        db.getObservationStore().
    }

    private IDatabaseRegistry getRegistry() {
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
