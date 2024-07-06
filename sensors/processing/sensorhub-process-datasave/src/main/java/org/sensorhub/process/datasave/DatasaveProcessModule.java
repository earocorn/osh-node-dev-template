package org.sensorhub.process.datasave;

import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.*;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.IProcessProvider;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.sensor.ISensorModule;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;
import org.sensorhub.process.datasave.config.TriggerThresholdConfig;
import org.sensorhub.process.datasave.helpers.ProcessHelper;
import org.sensorhub.process.datasave.helpers.ProcessOutputInterface;
import org.sensorhub.process.datasave.processes.DatasaveProcess;
import org.vast.data.DataArrayImpl;
import org.vast.data.DataBlockMixed;
import org.vast.process.ProcessException;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLException;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

public class DatasaveProcessModule extends AbstractProcessModule<DatasaveProcessConfig> {
        protected SMLUtils smlUtils;
        protected AggregateProcessImpl wrapperProcess;
        protected boolean useThreads = true;
        private String processUniqueID;
        public static final String PROCESS_DATASOURCE_NAME = "source0";
        public static final String PROCESS_INSTANCE_NAME = "process0";

    public DatasaveProcessModule()
        {
            wrapperProcess = new AggregateProcessImpl();
            wrapperProcess.setUniqueIdentifier(UUID.randomUUID().toString());
            initAsync = true;
        }


    @Override
    public void setParentHub(ISensorHub hub) {
        super.setParentHub(hub);
        smlUtils = new SMLUtils(SMLUtils.V2_0);
        smlUtils.setProcessFactory(hub.getProcessingManager());
    }


    @Override
    protected void doInit() throws SensorHubException
    {

        processUniqueID = "urn:osh:process:datasave:" + config.serialNumber;
        OshAsserts.checkValidUID(processUniqueID);

        try {
            processDescription = buildProcess();

        if(processDescription.getName() == null) {
            processDescription.setName(this.getName());
        }

        initChain();

        } catch (ProcessException e) {
            throw new RuntimeException(e);
        }
    }

    public AggregateProcessImpl buildProcess() throws SensorHubException, ProcessException {
        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        DatasaveProcess datasaveProcess = new DatasaveProcess();

        getParentHub().getProcessingManager().getAllProcessingPackages().add(new ProcessDescriptors());

        String inputUID = null;

        var module = getParentHub().getModuleRegistry().getModuleById(config.inputModuleID);
        if (module instanceof ISensorModule) {
            ISensorModule rapiscanDriver = (ISensorModule) module;
            inputUID = rapiscanDriver.getUniqueIdentifier();
        }

        assert inputUID != null;
        processHelper.addDataSource(PROCESS_DATASOURCE_NAME, inputUID);
        processHelper.addProcess(PROCESS_INSTANCE_NAME, datasaveProcess);

        datasaveProcess.getParameterList().getComponent(DatasaveProcess.INPUT_MODULE_ID_PARAM).getData().setStringValue(config.inputModuleID);
        datasaveProcess.getParameterList().getComponent(DatasaveProcess.INPUT_DATABASE_ID_PARAM).getData().setStringValue(config.inputDatabaseID);

        // Array
        DataRecord triggersRecord = (DataRecord) datasaveProcess.getParameterList().getComponent(DatasaveProcess.TRIGGERS_PARAM);
        DataBlock triggersData = triggersRecord.createDataBlock();

        // Update size component
        triggersData.setIntValue(0, config.triggers.size());

//        // Update size of array
//        var arrayImpl = (DataArrayImpl) triggersArray;
//        arrayImpl.updateSize();

        StringBuilder arrayValues = new StringBuilder("");
        for(TriggerThresholdConfig trigger : config.triggers) {
            if(arrayValues.length() != 0) {
                arrayValues.append(",");
            }
            arrayValues.append(trigger.triggerObservedProperty).append(",");
            arrayValues.append(trigger.comparisonType.name()).append(",");
            arrayValues.append(trigger.triggerThreshold);
        }

        triggersData.setStringValue(1, arrayValues.toString());
        triggersRecord.setData(triggersData);

        datasaveProcess.getParameterList().getComponent(DatasaveProcess.SAVE_TIME_PARAM).getData().setDoubleValue(config.timeBeforeTrigger);

        datasaveProcess.setParentHub(getParentHub());
        datasaveProcess.notifyParamChange();
        processHelper.addOutputList(datasaveProcess.getOutputList());

        for(DatasaveTriggerComponent triggerComponent : datasaveProcess.getTriggersMap().values()) {
            String destinationInput = "components/" + PROCESS_INSTANCE_NAME + "/inputs/" + triggerComponent.getRecordName();
            processHelper.addConnection(triggerComponent.getConnectionSource(PROCESS_DATASOURCE_NAME), destinationInput);
        }

        for(AbstractSWEIdentifiable output : datasaveProcess.getOutputList()) {
            DataComponent component = (DataComponent) output;
            processHelper.addConnection("components/" + PROCESS_INSTANCE_NAME + "/outputs/" + component.getName(), "outputs/" + component.getName());
        }
        // TODO: Add connections from input module -> process input
        //  and from process output to aggregate process output

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            smlUtils.writeProcess(os, processHelper.getAggregateProcess(), true);
            InputStream is = new ByteArrayInputStream(os.toByteArray());
            return (AggregateProcessImpl) smlUtils.readProcess(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected void initChain() throws SensorHubException
    {
        useThreads = processDescription.getInputList().isEmpty();

        try
            {
                //smlUtils.makeProcessExecutable(wrapperProcess, true);
                wrapperProcess = (AggregateProcessImpl)smlUtils.getExecutableInstance((AggregateProcessImpl)processDescription, useThreads);
                wrapperProcess.setInstanceName("chain");
                wrapperProcess.setParentLogger(getLogger());
                wrapperProcess.init();
            }
            catch (SMLException e)
            {
                throw new ProcessingException("Cannot prepare process chain for execution", e);
            }
            catch (ProcessException e)
            {
                throw new ProcessingException(e.getMessage(), e.getCause());
            }

            // advertise process inputs and outputs
            refreshIOList(processDescription.getOutputList(), outputs);

        setState(ModuleEvent.ModuleState.INITIALIZED);
    }


    public AggregateProcessImpl getProcessChain()
    {
        return wrapperProcess;
    }


        protected void refreshIOList(OgcPropertyList<AbstractSWEIdentifiable> ioList, Map<String, DataComponent> ioMap) throws ProcessingException
        {
            ioMap.clear();
            if (ioMap == outputs) {
                outputInterfaces.clear();
            }

            int numSignals = ioList.size();
            for (int i=0; i<numSignals; i++)
            {
                String ioName = ioList.getProperty(i).getName();
                AbstractSWEIdentifiable ioDesc = ioList.get(i);

                DataComponent ioComponent = SMLHelper.getIOComponent(ioDesc);
                ioMap.put(ioName, ioComponent);

                if (ioMap == outputs) {
                    outputInterfaces.put(ioName, new ProcessOutputInterface(this, ioDesc, wrapperProcess));
                }
            }
        }


    @Override
    protected void doStart() throws SensorHubException
    {
        if (wrapperProcess == null)
            throw new ProcessingException("No valid processing chain provided");

        // start processing thread
        if (useThreads)
        {
            try
            {
                wrapperProcess.start(e-> {
                    reportError("Error while executing process chain", e);
                });
            }
            catch (ProcessException e)
            {
                throw new ProcessingException("Cannot start process chain thread", e);
            }
        }
    }


    @Override
    protected void doStop()
    {
        if (wrapperProcess != null && wrapperProcess.isExecutable())
            wrapperProcess.stop();
    }
}
