package com.acenexus.tata.nexusbot.location.impl;

import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
import com.acenexus.tata.nexusbot.location.LocationService;
import com.acenexus.tata.nexusbot.location.OverpassResponse;
import com.acenexus.tata.nexusbot.location.ToiletLocation;
import com.acenexus.tata.nexusbot.location.mapper.ToiletLocationMapper;
import com.acenexus.tata.nexusbot.location.query.OverpassQueryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OsmLocationServiceImpl implements LocationService {

    private final OsmProperties osmProperties;
    private final RestTemplateBuilder restTemplateBuilder;
    private final OverpassQueryBuilder queryBuilder;
    private final ToiletLocationMapper locationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestTemplate overpassRestTemplate;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory overpassFactory = new SimpleClientHttpRequestFactory();
        overpassFactory.setConnectTimeout(osmProperties.getTimeoutMs());
        overpassFactory.setReadTimeout(osmProperties.getTimeoutMs());

        this.overpassRestTemplate = restTemplateBuilder
                .requestFactory(() -> overpassFactory)
                .defaultHeader("User-Agent", osmProperties.getUserAgent())
                .build();
    }

    @Override
    public CompletableFuture<List<ToiletLocation>> findNearbyToilets(double latitude, double longitude, int radius) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Searching for toilets near ({}, {}) within {}m using OSM", latitude, longitude, radius);

                String overpassQuery = queryBuilder.buildQuery(latitude, longitude, radius);
                log.debug("Overpass query: {}", overpassQuery);

                OverpassResponse response = executeOverpassQuery(overpassQuery);

                if (response == null || !response.isValid()) {
                    log.warn("No toilets found or invalid response from Overpass API");
                    return new ArrayList<>();
                }

                List<ToiletLocation> toilets = locationMapper.mapToToiletLocations(response.getElements(), latitude, longitude);

                toilets.sort(Comparator.comparingDouble(ToiletLocation::getDistance));
                int maxResults = Math.min(toilets.size(), osmProperties.getCarouselMaxItems());

                List<ToiletLocation> result = toilets.subList(0, maxResults);
                log.info("Found {} toilets within {}m", result.size(), radius);

                return result;

            } catch (Exception e) {
                log.error("Error searching nearby toilets using OSM: {}", e.getMessage(), e);
                return new ArrayList<>();
            }
        });
    }

    /**
     * 執行 Overpass 查詢
     */
    private OverpassResponse executeOverpassQuery(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> request = new HttpEntity<>(query, headers);

            ResponseEntity<String> response = overpassRestTemplate.postForEntity(osmProperties.getOverpassBaseUrl(), request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                log.warn("Overpass API returned status: {}", response.getStatusCode());
                return null;
            }

            return objectMapper.readValue(response.getBody(), OverpassResponse.class);

        } catch (RestClientException e) {
            log.error("HTTP error calling Overpass API: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error parsing Overpass response: {}", e.getMessage());
            return null;
        }
    }
}