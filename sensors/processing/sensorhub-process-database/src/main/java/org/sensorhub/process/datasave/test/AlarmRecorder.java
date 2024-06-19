package org.sensorhub.process.datasave.test;

import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataRecord;
import net.opengis.swe.v20.Text;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.data.IObsData;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.vast.data.DataBlockMixed;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class AlarmRecorder extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasave", "Data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    IDatabaseRegistry registry;
    DataRecord occupancyInput;
    Text dbInputParam;
    DataRecord neutronEntry;
    DataRecord gammaEntry;
    DataComponent video1;

    enum EntryType {
        NEUTRON, GAMMA
    }

    public AlarmRecorder() {
        super(INFO);
//
//        RADHelper radHelper = new RADHelper
//        outputData.add("video1", video1 = vidHelper.newVideoOutputMJPEG("video1", 640, 480).getElementType());
//
//        paramData.add("databaseInput", dbInputParam = radHelper.createText()
//                .label("Database Input")
//                .description("Database to query historical results")
//                .definition(SWEHelper.getPropertyUri("Database"))
//                .value("")
//                .build());
    }

    @Override
    public void execute() throws ProcessException {
        // Only populate data entry if alarm is triggered
        List<IObsData> alarmingData;
        List<IObsData> videoData;
        Instant now = Instant.now();
        Instant before = now.minusSeconds(10);

        if(occupancyInput.getComponent("GammaAlarm").getData().getBooleanValue()) {
            System.out.println("Results from past 10 seconds");

            alarmingData = getDataFromInterval(before, now, dbInputParam.getData().getStringValue(), EntryType.GAMMA);
            videoData = getVideoFromInterval(before.minusSeconds(100), now, dbInputParam.getData().getStringValue());

            try {
                publishVideoOutput(videoData);
                publishEntryOutput(alarmingData, EntryType.GAMMA);
            } catch (InterruptedException | DataStoreException e) {
                throw new RuntimeException(e);
            }
        }

        if(occupancyInput.getComponent("NeutronAlarm").getData().getBooleanValue()) {
            alarmingData = getDataFromInterval(before, now, dbInputParam.getData().getStringValue(), EntryType.NEUTRON);
            videoData = getVideoFromInterval(before.minusSeconds(100), now, dbInputParam.getData().getStringValue());

            try {
                publishVideoOutput(videoData);
                publishEntryOutput(alarmingData, EntryType.NEUTRON);
            } catch (InterruptedException | DataStoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<IObsData> getDataFromInterval(Instant start, Instant end, String dbModuleID, EntryType entryType) {
        String outputName = "";
        switch(entryType) {
            case GAMMA: outputName = "Gamma Scan";
            break;
            case NEUTRON: outputName = "Neutron Scan";
            break;
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

    private void publishEntryOutput(List<IObsData> blockList, EntryType entryType) throws InterruptedException, DataStoreException {
        DataRecord output = null;
        switch (entryType) {
            case GAMMA: {
                output = gammaEntry;
            }
            break;
            case NEUTRON: {
                output = neutronEntry;
            }
            break;
        }
        if(!blockList.isEmpty()) {
            DataBlockMixed entry = (DataBlockMixed) output.getData();
            for (int i = 0; i < blockList.size(); i++) {
                entry.setDoubleValue(0, blockList.get(i).getResult().getDoubleValue(0));
                entry.setIntValue(1, blockList.get(i).getResult().getIntValue(1));
                entry.setIntValue(2, blockList.get(i).getResult().getIntValue(2));
                entry.setIntValue(3, blockList.get(i).getResult().getIntValue(3));
                entry.setIntValue(4, blockList.get(i).getResult().getIntValue(4));
                entry.setStringValue(5, blockList.get(i).getResult().getStringValue(5));

                output.setData(entry);

                publishData();
                publishData(output.getName());

                System.out.println("Entry: " + entry + " DataBlock: " + blockList.get(i).getResult());
            }
        }
    }

    private List<IObsData> getVideoFromInterval(Instant start, Instant end, String dbModuleID) {
        String outputName = "video";
        DataStreamFilter dsFilter = new DataStreamFilter.Builder()
                .withOutputNames(outputName)
                .build();

        ObsFilter filter = new ObsFilter.Builder()
                .withDataStreams(dsFilter)
                .withPhenomenonTimeDuring(start, end).build();

        var obsDb = getRegistry()
                .getObsDatabaseByModuleID(dbModuleID)
                .getObservationStore()
                .select(filter);

        return obsDb.collect(Collectors.toList());
    }

    private void publishVideoOutput(List<IObsData> blockList) {
        if(!blockList.isEmpty()) {
            DataBlockMixed videoOutput = (DataBlockMixed) outputData.getComponent("video1");
            for(int i = 0; i < blockList.size(); i++) {
                System.out.println(blockList.get(i).getResult());
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
