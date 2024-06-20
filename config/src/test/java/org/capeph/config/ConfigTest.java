package org.capeph.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    private static class Values {
        public static IntValue intVal = new IntValue("test.values.int");
        public static DoubleValue doubleVal = new DoubleValue("test.values.double");
        public static StringValue stringVal = new StringValue("test.values.string");
        public static IntValue doubleKeyInt = new IntValue("test.double.keys.int");
        public static StringValue undefVal = new StringValue("test.values.undefined");
    }

    @BeforeEach
    public void init() {
        System.clearProperty("reactor.config.file");
    }

    @Test
    public void testConfigTypes() {
        // we are reading the
        assertEquals(27, Values.intVal.get());
        assertEquals(64.75, Values.doubleVal.get());
        assertEquals("space string", Values.stringVal.get());
        assertEquals(12, Values.doubleKeyInt.get());
    }

    @Test
    public void testDefaultConfig() {
        // we are reading the
        assertEquals(4, Config.minPoolSize.get());
        assertEquals("/lookup", Config.lookupPath.get());
    }

    @Test
    public void testConflict() {
        assertEquals(27, Values.intVal.get());
        assertThrows(IllegalArgumentException.class, () -> new IntValue("test.values.int"));
    }

    @Test
    public void testUndefinedParam() {
        assertThrows(IllegalArgumentException.class,
                () -> new IntValue("test.values.int.deeper"));
        assertEquals(27, Values.intVal.get());   // make sure regular stuff wasn't broken
    }

    @Test
    public void testBadFile() {
        assertEquals(27, Values.intVal.get());  // old config should still work!
        System.setProperty("reactor.config.file", "/this/does/not/exist");
        assertThrows(UncheckedIOException.class, () -> Loader.reloadConfig());
        assertEquals(27, Values.intVal.get());  // old config should still work!
    }

    @Test
    public void testPropertyOverride() {
        assertEquals(27, Values.intVal.get());  // old config should still work!
        System.setProperty("test.values.int", "42");
        Loader.reloadConfig();
        assertEquals(42, Values.intVal.get());  // check new config
        System.clearProperty("test.values.int");
        Loader.reloadConfig();
        assertEquals(27, Values.intVal.get());  // old config is restored
    }

}