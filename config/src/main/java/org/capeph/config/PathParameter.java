package org.capeph.config;

public class PathParameter {

    private final String path;
    private Loader.ConfigSource source;

    public PathParameter(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setSource(Loader.ConfigSource source) {
        this.source = source;
    }

    public Loader.ConfigSource getSource() {
        return source;
    }
}
