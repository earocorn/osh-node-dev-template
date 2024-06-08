package org.sensorhub.process.rapiscan.test;

import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.impl.utils.rad.RADHelper;
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
                .updatable(true)
                .label("Neutron Entry")
                .description("Data Entry")
                .definition(SWEHelper.getPropertyUri("Entry"))
                .addField("numNeutronRecords", numNeutronEntries = radHelper.createCount()
                        .label("Num Neutron Records")
                        .id("numNeutronRecords")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .value(0)
                        .build())
                .addField("neutronRecords", radHelper.createArray()
                        .label("Data Records")
                        .withVariableSize("numNeutronRecords")
                        .withElement("Neutron Scan", radHelper.createRecord()
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
                                .build())
                        .build())
                .build());

        outputData.add("gammaEntry", gammaEntry = radHelper.createRecord()
                .updatable(true)
                .label("Gamma Entry")
                .description("Data Entry")
                .definition(SWEHelper.getPropertyUri("Entry"))
                .addField("numGammaRecords", numGammaEntries = radHelper.createCount()
                        .label("Num Gamma Records")
                        .id("numGammaRecords")
                        .definition(SWEHelper.getPropertyUri("Quantity"))
                        .value(0)
                        .build())
                .addField("gammaRecords", radHelper.createArray()
                        .label("Data Records")
                        .withVariableSize("numGammaRecords")
                        .withElement("Gamma Scan", radHelper.createRecord()
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
                                .build())
                        .build())
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

    private void publishEntryOutput(List<IObsData> blockList, String dbModuleID, EntryType entryType) {
        var db = getRegistry().getObsDatabaseByModuleID(dbModuleID);
        if(!blockList.isEmpty()) {

            for (IObsData obsData : blockList) {
                db.getObservationStore()
                        .add(obsData);
            }
        }
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
