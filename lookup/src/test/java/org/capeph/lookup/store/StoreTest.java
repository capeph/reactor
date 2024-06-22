package org.capeph.lookup.store;

import org.capeph.lookup.dto.ReactorInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoreTest {


    private ReactorInfo getDto(String name, int channel) {
        ReactorInfo dto = new ReactorInfo();
        dto.setStreamid(channel);
        dto.setName(name);
        dto.setEndpoint("localhost:10000");
        return dto;
    }

    public void add(Store store, ReactorInfo dto) {
        dto.updateChannel(store);
        store.add(dto);
    }

    @Test
    public void testAssignChannel1() {
        Store store = new Store();
        add(store, getDto("first", 2));
        add(store, getDto("second", 0));
        assertEquals(2, store.get("first").getStreamid());
        assertEquals(1, store.get("second").getStreamid());
    }

    @Test
    public void testAddTwice() {
        Store store = new Store();
        add(store, getDto("first", 2));
        assertThrows(IllegalArgumentException.class, () -> add(store, getDto("first", 1)));
    }

    @Test
    public void testAddSameChannel() {
        Store store = new Store();
        add(store, getDto("first", 2));
        assertThrows(IllegalArgumentException.class, () -> add(store, getDto("second", 2)));
    }


    @Test
    public void testAssignChannel2() {
        Store store = new Store();
        add(store, getDto("first", 1));
        add(store, getDto("second", 0));
        assertEquals(1, store.get("first").getStreamid());
        assertEquals(2, store.get("second").getStreamid());
    }

    @Test
    public void testAssignChannel3() {
        Store store = new Store();
        add(store, getDto("first", 1));
        add(store, getDto("third", 3));
        add(store, getDto("second", 0));
        assertEquals(1, store.get("first").getStreamid());
        assertEquals(2, store.get("second").getStreamid());
    }

}