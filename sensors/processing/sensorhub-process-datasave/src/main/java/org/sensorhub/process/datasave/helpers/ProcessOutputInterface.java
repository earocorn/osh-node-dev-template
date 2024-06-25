package org.sensorhub.process.datasave.helpers;

import net.opengis.swe.v20.AbstractSWEIdentifiable;
import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.IEventHandler;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.processing.IProcessModule;
import org.sensorhub.api.processing.ProcessingException;
import org.sensorhub.impl.event.BasicEventHandler;
import org.sensorhub.impl.processing.AbstractProcessModule;
import org.sensorhub.process.datasave.DatasaveProcessModule;
import org.vast.process.DataQueue;
import org.vast.process.ProcessException;
import org.vast.sensorML.AbstractProcessImpl;
import org.vast.sensorML.SMLHelper;
import org.vast.util.Asserts;

// Refactored from SMLOutputInterface
public class ProcessOutputInterface implements IStreamingDataInterface {
    final IProcessModule parentProcess;
    final IEventHandler eventHandler;
    DataComponent outputDef;
    DataEncoding outputEncoding;
    DataBlock lastRecord;
    long lastRecordTime = Long.MIN_VALUE;
    double avgSamplingPeriod = 1.0;
    int avgSampleCount = 0;


    protected DataQueue outputQueue = new DataQueue()
    {
        @Override
        public synchronized void publishData()
        {
            Asserts.checkState(sourceComponent.hasData(), "Source component has no data");
            DataBlock data = sourceComponent.getData();

            long now = System.currentTimeMillis();
            double timeStamp = now / 1000.;

            // refine sampling period
            if (!Double.isNaN(lastRecordTime))
            {
                double dT = timeStamp - lastRecordTime;
                avgSampleCount++;
                if (avgSampleCount == 1)
                    avgSamplingPeriod = dT;
                else
                    avgSamplingPeriod += (dT - avgSamplingPeriod) / avgSampleCount;
            }

            // save last record and send event
            lastRecord = data;
            lastRecordTime = now;
            DataEvent e = new DataEvent(now, ProcessOutputInterface.this, data);
            eventHandler.publish(e);
        }
    };


    public ProcessOutputInterface(AbstractProcessModule parentProcess, AbstractSWEIdentifiable outputDescriptor, AbstractProcessImpl interfacingProcess) throws ProcessingException
    {
        this.parentProcess = parentProcess;
        this.outputDef = SMLHelper.getIOComponent(outputDescriptor);
        this.outputEncoding = SMLHelper.getIOEncoding(outputDescriptor);
        this.eventHandler = new BasicEventHandler();

        try
        {
            DataComponent execOutput = interfacingProcess.getOutputComponent(outputDef.getName());
            interfacingProcess.connect(execOutput, outputQueue);
        }
        catch (ProcessException e)
        {
            throw new ProcessingException("Error while connecting output " + outputDef.getName(), e);
        }
    }


    @Override
    public IDataProducer getParentProducer()
    {
        return parentProcess;
    }


    @Override
    public String getName()
    {
        return outputDef.getName();
    }


    @Override
    public boolean isEnabled()
    {
        return true;
    }


    @Override
    public DataComponent getRecordDescription()
    {
        return outputDef;
    }


    @Override
    public DataEncoding getRecommendedEncoding()
    {
        return outputEncoding;
    }


    @Override
    public DataBlock getLatestRecord()
    {
        return lastRecord;
    }


    @Override
    public long getLatestRecordTime()
    {
        return lastRecordTime;
    }


    @Override
    public double getAverageSamplingPeriod()
    {
        return avgSamplingPeriod;
    }


    @Override
    public void registerListener(IEventListener listener)
    {
        eventHandler.registerListener(listener);
    }


    @Override
    public void unregisterListener(IEventListener listener)
    {
        eventHandler.unregisterListener(listener);
    }

}
