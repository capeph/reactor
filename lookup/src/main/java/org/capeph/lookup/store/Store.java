/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.lookup.store;

import org.capeph.lookup.dto.ReactorInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Store {

    private final Map<String, ReactorInfo> reactors = new ConcurrentHashMap<>();
    private final Map<Integer, String> channels = new ConcurrentHashMap<>();

    public void add(ReactorInfo dto) {
        String name = dto.getName().toLowerCase();
        int channel = dto.getStreamid();
        if (reactors.containsKey(name)) {
            throw new IllegalArgumentException("Reactor with name " +  name + " already exists");
        }
        if (channels.containsKey(channel) && !channels.get(channel).equalsIgnoreCase(name)) {
            throw  new IllegalArgumentException("Another reactor with channel " + channel + " already exists");
        }
        reactors.put(name, dto);
        channels.put(channel, name);
    }

    public Set<Integer> getChannels() {
        return channels.keySet();
    }

    public ReactorInfo get(String name) {
        return reactors.get(name);
    }

}
