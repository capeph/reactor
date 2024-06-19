package org.capeph.config;

public class IntValue extends PathParameter{
    private int value;

    public IntValue(String path) {
        super(path);
        Loader.registerParameter(this);
    }

    @Override
    void setValue(Object value) {
        this.value = switch (value) {
            case Integer intObj -> intObj;
            case String strObj -> Integer.parseInt(strObj);
            default -> throw new IllegalArgumentException(getPath() + " is not an integer in file config");
        };
    }

    public int get() {
        return value;
    }
}
