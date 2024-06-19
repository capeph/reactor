package org.capeph.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {


    @Test
    public void testDefaultConfig() {
        assertEquals(6, Config.minPoolSize.get());
        assertEquals("/lookup", Config.lookupPath.get());
    }

}