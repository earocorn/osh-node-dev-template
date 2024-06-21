package org.sensorhub.process.datasave;

import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.process.datasave.test.AlarmRecorder;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;

public class DatasaveProcess extends ExecutableProcessImpl implements ISensorHubProcess {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasave", "Data recording process", null, AlarmRecorder.class);
    ISensorHub hub;

    protected DatasaveProcess() {
        super(INFO);

        // TODO: Initialize inputs from "trigger field"
        // TODO: Initialize parameter from "trigger threshold" and "timeframe"
        // TODO: Initialize output from "observed properties"
    }

    @Override
    public void execute() throws ProcessException {

    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
