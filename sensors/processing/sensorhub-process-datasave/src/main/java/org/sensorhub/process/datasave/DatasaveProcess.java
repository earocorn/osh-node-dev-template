package org.sensorhub.process.datasave;

import net.opengis.swe.v20.Count;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.Quantity;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.comm.ICommNetwork;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.OSHProcessInfo;
import org.sensorhub.impl.processing.ISensorHubProcess;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;
import org.sensorhub.process.datasave.config.TriggerThresholdConfig;
import org.sensorhub.process.datasave.test.AlarmRecorder;
import org.vast.process.ExecutableProcessImpl;
import org.vast.process.ProcessException;
import org.vast.swe.SWEHelper;

import java.util.Map;

public class DatasaveProcess extends ExecutableProcessImpl implements ISensorHubProcess {

    public static final OSHProcessInfo INFO = new OSHProcessInfo("datasave", "Data recording process", null, AlarmRecorder.class);
    ISensorHub hub;
    Quantity timeBeforeTrigger;

    public DatasaveProcess(DatasaveProcessConfig config) {
        super(INFO);

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
    }

    @Override
    public void setParentHub(ISensorHub hub) {
        this.hub = hub;
    }
}
