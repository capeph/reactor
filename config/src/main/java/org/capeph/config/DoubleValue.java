package org.capeph.config;

public class DoubleValue extends PathParameter{
    private double value;

    public DoubleValue(String path) {
        super(path);
        Loader.setupParameter(this);
    }

    @Override
    void setValue(Object value) {
        this.value = switch (value) {
            case Float floatObj -> floatObj;
            case Double doubleObj -> doubleObj;
            case Integer intObj -> (double) intObj;
            case String strObj -> Double.parseDouble(strObj);
            default -> throw new IllegalArgumentException(getPath() + " is not an integer in file config");
        };
    }

    public double get() {
        return value;
    }
}
