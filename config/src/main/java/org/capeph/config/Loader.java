package org.capeph.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO: add support for Maps and Lists
public class Loader {

    private static final Logger log = LogManager.getLogger(Loader.class);

    static Map<String, PathParameter> parameters = new HashMap<>();
    static Map<String, Object> fileValues;

    static {
        loadFileConfig();
    }

    static void registerParameter(PathParameter parameter) {
        setValue(parameter, fileValues);
        parameters.put(parameter.getPath(), parameter);
    }

    private static void setValue(PathParameter parameter, Map<String, Object> configValues) {
        switch (parameter) {
            case StringValue strVal -> setValue(configValues, strVal);
            case IntValue intVal -> setValue(configValues, intVal);
            default -> throw new IllegalArgumentException("Unsupported parameter type for " + parameter.getPath());
        }
    }


    @SuppressWarnings("unchecked")
    private static void loadFileConfig() {
        // TODO: provide path to config file in property
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream resource = Loader.class.getResourceAsStream("/config.yaml");
        try {
            fileValues = mapper.readValue(resource, Map.class);
        } catch (IOException e) {
            log.error("Failed to read config file");
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object getFrom(String key, Map<String, Object> map) {
        Object result = null;
        int idx = 0;
        while (result == null) {
            idx = key.indexOf('.', idx);
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

    public enum ConfigSource {FILE, PROPERTY}

    private static void setValue(Map<String, Object> fileValues, StringValue param) {
        ConfigSource source = ConfigSource.PROPERTY;
        String result = System.getProperty(param.getPath());
        if (result == null) {
            source = ConfigSource.FILE;
            result = String.valueOf(getFrom(param.getPath(), fileValues));
        }
        if (result != null) {
            param.setValue(result);
            param.setSource(source);
        }
    }

    private static void setValue(Map<String, Object> fileValues, IntValue param) {
        String resultStr = System.getProperty(param.getPath());
        if (resultStr != null) {
            param.setValue(Integer.parseInt(resultStr));
            param.setSource(ConfigSource.PROPERTY);
        } else {
            Object fileObject = getFrom(param.getPath(), fileValues);
            if (fileObject != null) {
                param.setValue(switch (getFrom(param.getPath(), fileValues)) {
                    case Integer intObj -> intObj;
                    case String strObj -> Integer.parseInt(strObj);
                    default -> throw new IllegalArgumentException(param.getPath() + " is not an integer in file config");
                });
                param.setSource(ConfigSource.FILE);
            }
        }
    }

}
