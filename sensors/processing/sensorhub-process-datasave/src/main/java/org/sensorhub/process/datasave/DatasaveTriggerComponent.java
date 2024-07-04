package org.sensorhub.process.datasave;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import org.sensorhub.process.datasave.helpers.ComparisonType;

import java.util.LinkedList;

public class DatasaveTriggerComponent {

    private String observedProperty;
    private ComparisonType comparisonType;
    private String threshold;
    private DataComponent recordDescription;
    private String recordName;
    private DataBlock triggerData;

    public DatasaveTriggerComponent(String observedProperty, ComparisonType comparisonType, String threshold) {
        this.observedProperty = observedProperty;
        this.comparisonType = comparisonType;
        this.threshold = threshold;
    }

    public DataType getDataType() {
        return this.recordDescription.getData().getDataType();
    }

    public String getName() {
        return this.recordName;
    }

    public String getThresholdName() {
        return this.recordDescription.getName() + "Threshold";
    }

    public DataComponent getRecordDescription() {
        return this.recordDescription;
    }

    public String getConnectionSource(String componentName) {
        DataComponent current = this.recordDescription;
        LinkedList<String> connectionStringList = new LinkedList<>();
        // TODO: Change this later. It's good for now
        StringBuilder connection = new StringBuilder("components/" + componentName + "/outputs");
        while(current != null) {
            connectionStringList.add(current.getName());
            current = current.getParent();
        }
        for(int connectionStringListIndex = connectionStringList.size()-1; connectionStringListIndex >= 0; connectionStringListIndex--) {
            connection.append("/").append(connectionStringList.get(connectionStringListIndex));
        }
        return connection.toString();
    }

    public void setRecordDescription(DataComponent component) {
        this.recordDescription = component;
        this.recordName = recordDescription.getName();

        String triggerValue = this.threshold;
        DataBlock data = component.createDataBlock();

        switch (data.getDataType()) {
            // Boolean
            case BOOLEAN: data.setBooleanValue(Boolean.parseBoolean(triggerValue));
            break;
            // Int
            case UINT:
            case INT: data.setIntValue(Integer.parseInt(triggerValue));
            break;
            // Float
            case FLOAT: data.setFloatValue(Float.parseFloat(triggerValue));
            break;
            // Double
            case DOUBLE: data.setDoubleValue(Double.parseDouble(triggerValue));
            break;
            // Byte
            case UBYTE:
            case BYTE: data.setByteValue(Byte.parseByte(triggerValue));
            break;
            // Short
            case USHORT:
            case SHORT: data.setShortValue(Short.parseShort(triggerValue));
            break;
            // Long
            case ULONG:
            case LONG: data.setLongValue(Long.parseLong(triggerValue));
            break;
            // String
            case UTF_STRING:
            case ASCII_STRING: data.setStringValue(triggerValue);
            break;
        }
        this.triggerData = data;
    }

    public DataBlock getTriggerData() {
        return this.triggerData;
    }

    public String getThreshold() {
        return threshold;
    }

    public ComparisonType getComparisonType() {
        return comparisonType;
    }

    public String getObservedProperty() {
        return observedProperty;
    }
}
