/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.lookup.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.capeph.lookup.store.Store;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReactorInfo {

    //https://stackoverflow.com/questions/106179/regular-expression-to-match-dns-hostname-or-ip-address
    public static final String ValidIpAddressRegex =
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";

    public static final String ValidHostnameRegex =
            "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

    public static final String ValidHostPortRegex =
            "^[a-zA-Z0-9\\-]*(\\.[a-zA-Z0-9\\-]*)*\\:[0-9]+";


    @Parameter(description = "Name of the reactor")
    private String name;

    @Parameter(description = "Host reactor is running on")
//TODO: better regex to validate endpoint hostname:port
    @Pattern(regexp = ValidHostPortRegex, message = "Must be a valid host name or ip")
    private String endpoint;

    @Parameter(description = "Id of the messaging channel")
    @Min(value = 0, message = "Channel can not be negative")
    private int streamid;

    // if channel is 0 set it to the next available channel instead.
    // this can cause conflicts, so would recommend to either use
    // all preset channels, or all automatic assigned.
    public void updateChannel(Store serviceStore) {
        if (streamid == 0) {
            streamid = serviceStore.getChannels()
                    .stream()
                    .sorted()
                    .reduce(1,
                            (a, e) -> a < e ? a : e + 1);
        }
    }


}
