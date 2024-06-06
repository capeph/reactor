/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.lookup.rest;

import jakarta.validation.Valid;
import org.capeph.lookup.dto.ReactorInfo;
import org.capeph.lookup.store.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lookup")
public class LookupController {

    @Autowired
    private Store serviceStore;


    @PostMapping("")
    public ResponseEntity<ReactorInfo> postMapping(@Valid @RequestBody ReactorInfo dto) {
        dto.updateChannel(serviceStore);
        serviceStore.add(dto);
        return ResponseEntity.ok().body(dto);
    }

    @GetMapping("/{name}")
    @ResponseBody
    public ReactorInfo getMapping(@PathVariable String name) {
        ReactorInfo dto = serviceStore.get(name);
        if (dto == null) {
            throw new IllegalArgumentException(name + " not found");
        }
        return dto;
    }

}
