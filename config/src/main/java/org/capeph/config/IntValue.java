package org.capeph.config;

public class IntValue extends PathParameter{
    private int value;

    public IntValue(String path) {
        super(path);
        Loader.registerParameter(this);
    }

    public int get() {
        return value;
    }

    void setValue(int value) {
        this.value = value;
    }
}
