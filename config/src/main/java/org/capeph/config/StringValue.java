package org.capeph.config;

public class StringValue extends PathParameter{
    private String value;

    StringValue(String path) {
        super(path);
        Loader.registerParameter(this);
    }

    @Override
    void setValue(Object value) {
        this.value = value.toString();  // TODO: add some validation
    }

    public String get() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }
}
