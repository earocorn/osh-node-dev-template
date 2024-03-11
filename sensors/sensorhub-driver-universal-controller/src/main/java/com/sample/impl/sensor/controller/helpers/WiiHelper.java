package com.sample.impl.sensor.controller.helpers;

import motej.Mote;
import net.opengis.swe.v20.DataRecord;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

public class WiiHelper extends SWEHelper {

    public DataRecord newWiiMoteOutput(String name, Mote mote) {
        SWEBuilders.DataRecordBuilder recordBuilder;
        DataRecord dataRecord;

        recordBuilder = this.createRecord()
                .name(name)
                .addField("sampleTime", this.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"));



        dataRecord = recordBuilder.build();

        return dataRecord;
    }

}
