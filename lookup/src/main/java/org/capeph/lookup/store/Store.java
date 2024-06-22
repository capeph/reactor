/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.lookup.store;

import lombok.Getter;
import org.capeph.lookup.dto.ReactorInfo;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Component
public class Store {

    private final Map<String, ReactorInfo> reactors = new ConcurrentHashMap<>();

    @Getter
    private final Set<Integer> channels = new HashSet<>();

    public void add(ReactorInfo dto) {
        String name = dto.getName().toLowerCase();
        int channel = dto.getStreamid();
        if (reactors.containsKey(name)) {
            throw new IllegalArgumentException("Reactor with name " +  name + " already exists");
        }
        if (channels.contains(channel)) {
            throw  new IllegalArgumentException("Another reactor with channel " + channel + " already exists");
        }
        reactors.put(name, dto);
        channels.add(channel);
    }

    public ReactorInfo get(String name) {
        return reactors.get(name);
    }

}
