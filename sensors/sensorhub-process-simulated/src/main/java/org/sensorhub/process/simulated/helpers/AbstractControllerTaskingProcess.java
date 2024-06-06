package org.sensorhub.process.simulated.helpers;

import org.sensorhub.impl.SensorHub;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.process.ProcessInfo;

public abstract class AbstractControllerTaskingProcess extends ExecutableProcessImpl {

    public AbstractControllerTaskingProcess(ProcessInfo processInfo) {
        super(processInfo);

    }

    public abstract void updateOutputs() throws ProcessException;

    @Override
    public void execute() throws ProcessException {

    }
}
