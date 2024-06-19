package org.capeph.config;

public class Config {

    public static IntValue minPoolSize = new IntValue("reactor.pool.min");
    public static IntValue maxPoolSize = new IntValue("reactor.pool.max");
    public static StringValue lookupUrl = new StringValue("reactor.lookup.url");
    public static StringValue lookupPath = new StringValue("reactor.lookup.path");

}
