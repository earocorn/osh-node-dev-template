package org.sensorhub.process.datasave;

import net.opengis.OgcPropertyList;
import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataComponent;
import org.sensorhub.api.ISensorHub;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.module.IModule;
import org.sensorhub.api.module.ModuleEvent;
import org.sensorhub.api.processing.IProcessProvider;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.api.utils.OshAsserts;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.process.datasave.config.DatasaveProcessConfig;
import org.sensorhub.process.datasave.helpers.ProcessHelper;
import org.sensorhub.process.datasave.helpers.ProcessOutputInterface;
import org.vast.process.ProcessException;
import org.vast.sensorML.AggregateProcessImpl;
import org.vast.sensorML.SMLHelper;
import org.vast.sensorML.SMLUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class DatasaveProcessModule extends AbstractProcessModule<DatasaveProcessConfig> {
        protected SMLUtils smlUtils;
        protected AggregateProcessImpl wrapperProcess;
        protected long lastUpdatedProcess = Long.MIN_VALUE;
        protected boolean paused = false;
        protected int errorCount = 0;
        protected boolean useThreads = true;
        private String processUniqueID;

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

        processDescription = buildProcess();

        if(processDescription.getName() == null) {
            processDescription.setName(this.getName());
        }

        initChain();

        // only go further if sensorML file was provided
        // otherwise we'll do it at the next update
//            if (config.sensorML != null)
//            {
//                String smlPath = config.getSensorMLPath();
//
//                // parse SensorML file
//                try (InputStream is = new BufferedInputStream(new FileInputStream(smlPath)))
//                {
//                    processDescription = (AggregateProcessImpl)smlUtils.readProcess(is);
//                    OshAsserts.checkSystemObject(processDescription);
//
//                    // set default name if none set in SensorML file
//                    if (processDescription.getName() == null)
//                        processDescription.setName(this.getName());
//
//                    initChain();
//                }
//                catch (Exception e)
//                {
//                    throw new ProcessingException(String.format("Cannot read SensorML description from '%s'", smlPath), e);
//                }
    }

    public AggregateProcessImpl buildProcess() {
        ProcessHelper processHelper = new ProcessHelper();
        processHelper.getAggregateProcess().setUniqueIdentifier(processUniqueID);

        // TODO: Need to pass
        //  Input Module ID
        //  Input System Database ID
        //  Triggers
        //  Database Observed Properties
        //  Time Before Trigger

        // Process Params:
        // Input Module ID
        // Input System Database ID

        // Constructor Props:
        // Observed properties to create data structure

        DatasaveProcess datasaveProcess = new DatasaveProcess();

        // TODO: Create process description

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
        //useThreads = processDescription.getInputList().isEmpty();

        // make process executable
//            try
//            {
//                //smlUtils.makeProcessExecutable(wrapperProcess, true);
//                wrapperProcess = (AggregateProcessImpl)smlUtils.getExecutableInstance((AggregateProcessImpl)processDescription, useThreads);
//                wrapperProcess.setInstanceName("chain");
//                wrapperProcess.setParentLogger(getLogger());
//                wrapperProcess.init();
//            }
//            catch (SMLException e)
//            {
//                throw new ProcessingException("Cannot prepare process chain for execution", e);
//            }
//            catch (ProcessException e)
//            {
//                throw new ProcessingException(e.getMessage(), e.getCause());
//            }
//
//            // advertise process inputs and outputs
//            refreshIOList(processDescription.getInputList(), inputs);
//            refreshIOList(processDescription.getParameterList(), parameters);
//            refreshIOList(processDescription.getOutputList(), outputs);

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
        errorCount = 0;

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
