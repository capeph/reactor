package org.capeph.config;

public class DoubleValue extends PathParameter{
    private double value = Double.NaN;

    public DoubleValue(String path) {
        super(path);
        Loader.setupParameter(this);
    }

    @Override
    void setValue(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Can not set a double to null");
        }
        this.value = switch (value) {
            case Float floatObj -> floatObj;
            case Double doubleObj -> doubleObj;
            case Integer intObj -> (double) intObj;
            case String strObj -> Double.parseDouble(strObj);
            default -> throw new IllegalArgumentException(getPath() + " is not an double in file config");
        };
    }

    public double get() {
        return value;
    }
}
