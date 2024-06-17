package org.capeph.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//TODO: add support for Maps and Lists
public class Config {


    private static int minPoolSize;
    private static int maxPoolSize;
    private static String lookupUrl;


    private static String lookupPath;

    private static final Logger log = LogManager.getFormatterLogger(Config.class);

    static {
        loadConfig();
    }

    private static void setValues(Map<String, Object> configValues) {
        lookupUrl = getString("reactor.lookup.url", configValues, lookupUrl);
        lookupPath = getString("reactor.lookup.path", configValues, lookupPath);
        minPoolSize = getInt("reactor.pool.min", configValues, minPoolSize);
        maxPoolSize = getInt("reactor.pool.max", configValues, maxPoolSize);
    }

    @SuppressWarnings("unchecked")
    private static void loadConfig() {
        // TODO: provide path to config file in property
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        InputStream resource = Config.class.getResourceAsStream("/config.yaml");
        try {
            Map<String, Object> fileValues = mapper.readValue(resource, Map.class);
            setValues(fileValues);
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


    private static int getInt(String key, Map<String, Object> fileValues, int defaultValue) {
        int result;
        String resultStr = System.getProperty(key);
        if (resultStr != null) {
            result = Integer.parseInt(resultStr);
        } else {
            result = switch (getFrom(key, fileValues)) {
                case Integer intObj -> intObj;
                case String strObj -> Integer.parseInt(strObj);
                default -> defaultValue;
            };
        }
        return result;
    }


    private static String getString(String key, Map<String, Object> fileValues, String defaultValue) {
        String result = System.getProperty(key);
        if (result == null) {
            result = String.valueOf(getFrom(key, fileValues));
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    public static int getMinPoolSize() {
        return minPoolSize;
    }

    public static int getMaxPoolSize() {
        return maxPoolSize;
    }

    public static String getLookupUrl() {
        return lookupUrl;
    }


    public static String getLookupPath() {
        return lookupPath;
    }

}
