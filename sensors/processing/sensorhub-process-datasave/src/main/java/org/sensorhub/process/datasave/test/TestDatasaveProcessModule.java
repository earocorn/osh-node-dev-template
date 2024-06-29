package org.sensorhub.process.datasave.test;

import org.sensorhub.api.processing.IDataProcess;
import org.sensorhub.impl.processing.AbstractProcessWrapperModule;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;
import org.sensorhub.process.datasave.processes.TestDatasaveProcess;

public class TestDatasaveProcessModule extends AbstractProcessWrapperModule<DatasaveProcessConfig> {

    public TestDatasaveProcessModule() {

    }
    @Override
    protected IDataProcess initProcess() {
        return new TestDatasaveProcess();
    }

    @Override
    public void doInit() {
        super.doInit();

        //var params =  process.getParameterDescriptors();
        // TODO: Set params from config?
    }
}
