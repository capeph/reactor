package org.capeph.reactor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.lookup.dto.ReactorInfo;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class Registrar {

    private final Logger log = LogManager.getLogger(Registrar.class);

    private final String lookupUrl;

    public Registrar(String lookupUrl) {
        this.lookupUrl = lookupUrl;
    }

    public ReactorInfo register(String name, String endpoint)  {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            ObjectMapper objectMapper = new ObjectMapper();
            // TODO: move DTOs to separate module
            String body = objectMapper.writeValueAsString(new ReactorInfo(name, endpoint, 0));
            HttpRequest update = HttpRequest.newBuilder()
                    .uri(new URI(lookupUrl + "/lookup"))  //TODO:  make path a constant
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(update, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("lookup endpoint returned " + response.statusCode());
            }
            String jsonData = response.body();

            return objectMapper.readValue(jsonData,
                    new TypeReference<ReactorInfo>() {});

        }
        catch (Exception e) {
            log.error("Registration of reactor failed {} ", e.getMessage());
        }
        throw new IllegalStateException("Failed to set up reactor");
    }

    public ReactorInfo lookupReactor(String name) {
        try(HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest query = HttpRequest.newBuilder()
                    .uri(new URI(lookupUrl + "/lookup/" + name))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(query, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("lookup endpoint returned " + response.statusCode());
            }
            String jsonData = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonData,
                    new TypeReference<ReactorInfo>() {});

        } catch (Exception e) {
            throw new IllegalStateException("Lookup failed:", e);
        }
    }


}
