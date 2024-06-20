package org.capeph.config;

public class StringValue extends PathParameter{
    private String value;

    StringValue(String path) {
        super(path);
        Loader.setupParameter(this);
    }

    @Override
    void setValue(Object value) {
        this.value = value.toString();  // TODO: add some validation
    }

    public String get() {
        return value;
    }
}
