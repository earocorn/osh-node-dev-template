package org.sensorhub.process.universalcontroller.test;

import net.opengis.swe.v20.DataBlock;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ControllerDBTest extends ExecutableProcessImpl implements ISensorHubProcess {
    public static final OSHProcessInfo INFO = new OSHProcessInfo("controllerdbtest", "Database testing process", null, ControllerDBTest.class);
    ISensorHub hub;

    public List<DataBlock> getDataFromInterval(Instant start, Instant end) {
        var db = hub.getDatabaseRegistry();
        var obsDb = db.getObsDatabaseByModuleID("test");
        var results = obsDb.getObservationStore().selectResults(new ObsFilter.Builder().
                withPhenomenonTimeDuring(
                        start,
                        end)
                .build());

        return results.collect(Collectors.toList());
    }

    protected ControllerDBTest() {
        super(INFO);
    }

    @Override
    public void execute() throws ProcessException {

    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
