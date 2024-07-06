package org.sensorhub.process.datasave;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataType;
import org.sensorhub.process.datasave.helpers.ComparisonType;

import java.util.LinkedList;
import java.util.Objects;

public class DatasaveTriggerComponent {

    private String observedProperty;
    private ComparisonType comparisonType;
    private String threshold;
    private DataComponent recordDescription;

    public String getRecordName() {
        return recordName;
    }

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

    public boolean compareThreshold(DataComponent input) {
        DataBlock data = this.triggerData;
        switch(data.getDataType()) {
            // Boolean
            case BOOLEAN:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getBooleanValue() == input.getData().getBooleanValue();
                    case LESS_THAN:
                    case GREATER_THAN:
                        return data.getBooleanValue() != input.getData().getBooleanValue();
                }
                break;
            // Int
            case UINT:
            case INT:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getIntValue() == input.getData().getIntValue();
                    case LESS_THAN:
                        return data.getIntValue() > input.getData().getIntValue();
                    case GREATER_THAN:
                        return data.getIntValue() < input.getData().getIntValue();
                }
                break;
            // Float
            case FLOAT:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getFloatValue() == input.getData().getFloatValue();
                    case LESS_THAN:
                        return data.getFloatValue() > input.getData().getFloatValue();
                    case GREATER_THAN:
                        return data.getFloatValue() < input.getData().getFloatValue();
                }
                break;
            // Double
            case DOUBLE:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getDoubleValue() == input.getData().getDoubleValue();
                    case LESS_THAN:
                        return data.getDoubleValue() > input.getData().getDoubleValue();
                    case GREATER_THAN:
                        return data.getDoubleValue() < input.getData().getDoubleValue();
                }
                break;
            // Byte
            case UBYTE:
            case BYTE:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getByteValue() == input.getData().getByteValue();
                    case LESS_THAN:
                        return data.getByteValue() > input.getData().getByteValue();
                    case GREATER_THAN:
                        return data.getByteValue() < input.getData().getByteValue();
                }
                break;
            // Short
            case USHORT:
            case SHORT:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getShortValue() == input.getData().getShortValue();
                    case LESS_THAN:
                        return data.getShortValue() > input.getData().getShortValue();
                    case GREATER_THAN:
                        return data.getShortValue() < input.getData().getShortValue();
                }
                break;
            // Long
            case ULONG:
            case LONG:
                switch(comparisonType) {
                    case EQUAL:
                        return data.getLongValue() == input.getData().getLongValue();
                    case LESS_THAN:
                        return data.getLongValue() > input.getData().getLongValue();
                    case GREATER_THAN:
                        return data.getLongValue() < input.getData().getLongValue();
                }
                break;
            // String
            case UTF_STRING:
            case ASCII_STRING:
                switch(comparisonType) {
                    case EQUAL:
                        return Objects.equals(data.getStringValue(), input.getData().getStringValue());
                    case LESS_THAN:
                    case GREATER_THAN:
                        return !Objects.equals(data.getStringValue(), input.getData().getStringValue());
                }
                break;
        }
        return false;
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
