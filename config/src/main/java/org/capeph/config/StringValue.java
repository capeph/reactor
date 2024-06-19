package org.capeph.config;

public class StringValue extends PathParameter{
    private String value;

    StringValue(String path) {
        super(path);
        Loader.registerParameter(this);
    }

    public String get() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }
}
