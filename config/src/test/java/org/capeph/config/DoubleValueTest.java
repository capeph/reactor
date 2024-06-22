package org.capeph.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DoubleValueTest {


    @Test
    public void testDoubleValue() {
        DoubleValue dvn = new DoubleValue("double.value.test.double");
        assertEquals(Double.NaN, dvn.get());
        dvn.setValue("12");
        assertEquals(12.0, dvn.get(), 0.001);
        dvn.setValue(27.0f);
        assertEquals(27.0, dvn.get(), 0.001);
        dvn.setValue(31.5d);
        assertEquals(31.5, dvn.get(), 0.001);
        dvn.setValue(17);
        assertEquals(17.0, dvn.get(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> dvn.setValue(null));
        assertThrows(IllegalArgumentException.class, () -> dvn.setValue('a'));
    }

}