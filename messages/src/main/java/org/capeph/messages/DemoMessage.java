package org.capeph.messages;

import org.capeph.annotations.ReactorMessage;
import org.capeph.reactor.ReusableMessage;

@ReactorMessage
public class DemoMessage implements ReusableMessage {

    private String stringField;

    private int intField;

    private boolean boolField;

    private double doubleField;

    private long longField;

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public int getIntField() {
        return intField;
    }

    public void setIntField(int intField) {
        this.intField = intField;
    }

    public boolean isBoolField() {
        return boolField;
    }

    public void setBoolField(boolean boolField) {
        this.boolField = boolField;
    }

    public double getDoubleField() {
        return doubleField;
    }

    public void setDoubleField(double doubleField) {
        this.doubleField = doubleField;
    }

    public long getLongField() {
        return longField;
    }

    public void setLongField(long longField) {
        this.longField = longField;
    }

    @Override
    public void clear() {
        stringField = "";
        intField = 0;
        boolField = false;
        doubleField = 0.0D;
        longField = 0L;
    }
}
