package org.capeph.config;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO: add support for Maps and Lists
public class Loader {

    public enum ConfigSource {FILE, PROPERTY}

    private static final Logger log = LogManager.getLogger(Loader.class);

    static Map<String, PathParameter> parameters = new HashMap<>();
    static Map<String, Object> fileValues;

    static {
        loadFileConfig();
    }

    public static void reloadConfig() {
        loadFileConfig();
        Collection<PathParameter> params = new ArrayList<>(parameters.values());
        parameters.clear();
        params.forEach(Loader::setupParameter);
    }

    static void setupParameter(PathParameter parameter) {
        if (parameters.containsKey(parameter.getPath())) {
            throw new IllegalArgumentException("Another parameter is already defined with the same path");
        }
        setValue(fileValues, parameter);
        parameters.put(parameter.getPath(), parameter);
    }

    private static void setValue(Map<String, Object> fileValues, PathParameter param) {
        String resultStr = System.getProperty(param.getPath());
        if (resultStr != null) {
            param.setValue(resultStr);
            param.setSource(ConfigSource.PROPERTY);
        } else {
            Object fileObject = getFrom(param.getPath(), fileValues);
            if (fileObject != null) {
                param.setValue(fileObject);
                param.setSource(ConfigSource.FILE);
            }
        }
    }

    private static InputStream openConfigFile(String path) throws FileNotFoundException {
        if (path == null) {
            log.info("No external config file specified, using provided file");
            return Loader.class.getResourceAsStream("/config.yaml");
        }
        log.info("Reading config from {}", path);
        File configFile = new File(path);
        return new FileInputStream(configFile);
    }

    @SuppressWarnings("unchecked")
    private static void loadFileConfig() {
        // TODO: provide path to config file in property
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        String path = System.getProperty("reactor.config.file");
        try {
            InputStream resource = openConfigFile(path);
            fileValues = mapper.readValue(resource, Map.class);
        } catch (IOException e) {
            log.error("Failed to read config file");
            throw new UncheckedIOException("File at " + path + " not found:", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getFrom(String key, Map<String, Object> map) {
        Object result = null;
        int idx = -1;
        while (result == null) {
            idx = key.indexOf('.', idx+1);
            String subKey = idx == -1 ? key : key.substring(0, idx);
            result = map.get(subKey);
            if (idx == -1) {
                return result;
            }
            if (result instanceof Map subMap) {
                return getFrom(key.substring(idx+1), subMap);
            }
        }
        throw new IllegalArgumentException("Could not find key");
    }



}
