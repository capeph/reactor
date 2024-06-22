package org.capeph.lookup.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.capeph.lookup.dto.ReactorInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LookupControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    // test store a dto and auto assign channel
    public void testPutGetAutoChannel() throws Exception {
        ReactorInfo  body = ReactorInfo.builder()
                .name("test")
                .endpoint("test.host.name:9000")
                .streamid(0)  // should be automatically bumped to 1;
                .build();
        mvc.perform(
                MockMvcRequestBuilders.post("/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("name").value("test"))
                .andExpect(jsonPath("streamid").value(1));
        mvc.perform(
                MockMvcRequestBuilders.get("/lookup/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("name").value("test"))
                .andExpect(jsonPath("streamid").value(1));
    }

    @Test
    // test to store a dto with fixed channel
    public void testPutGetFixedChannel() throws Exception {
        ReactorInfo body = ReactorInfo.builder()
                .name("two")
                .endpoint("test.host.name:9000")
                .streamid(2)
                .build();
        mvc.perform(
                MockMvcRequestBuilders.post("/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("name").value("two"))
                .andExpect(jsonPath("streamid").value(2));
        mvc.perform(
                MockMvcRequestBuilders.get("/lookup/two"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("name").exists())
                .andExpect(jsonPath("name").value("two"))
                .andExpect(jsonPath("streamid").value(2));
    }


    @Test
    // test store a dto and auto assign channel
    public void testPutWithBadEndpoint() throws Exception {
        ReactorInfo body = ReactorInfo.builder()
                .name("test2")
                .endpoint("superbroken$%#")
                .streamid(0)  // should be automatically bumped to 1;
                .build();
        mvc.perform(MockMvcRequestBuilders.post("/lookup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(body))
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }


    @Test
    // test store a dto and auto assign channel
    public void testPutWithNegativeChannel() throws Exception {
        ReactorInfo body = ReactorInfo.builder()
                .name("test3")
                .endpoint("test.host.name:9000")
                .streamid(-5)  // should fail validation
                .build();
        mvc.perform(MockMvcRequestBuilders.post("/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(body))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }
}