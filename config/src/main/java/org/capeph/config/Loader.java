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

    public enum ConfigSource {FILE, PROPERTY}

    private static final Logger log = LogManager.getLogger(Loader.class);

    static Map<String, PathParameter> parameters = new HashMap<>();
    static Map<String, Object> fileValues;

    static {
        loadFileConfig();
    }

    static void registerParameter(PathParameter parameter) {
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



}
