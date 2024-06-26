/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.messages;

import org.capeph.annotations.ReactorMessage;
import org.capeph.reactor.ReusableMessage;

@ReactorMessage
public class DemoMessage implements ReusableMessage {

    private String stringField;

    private final StringBuffer stringBufferField = new StringBuffer();

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

    public StringBuffer getStringBufferField() {
        return stringBufferField;
    }

    public void setStringBufferField(String value) {
        this.stringBufferField.setLength(0);
        this.stringBufferField.append(value);
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
        stringBufferField.setLength(0);
        intField = 0;
        boolField = false;
        doubleField = 0.0D;
        longField = 0L;
    }
}
