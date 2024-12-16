package com.challenge.services;

import com.challenge.exceptions.ExternalApiException;
import com.challenge.exceptions.GeneralApplicationException;
import com.challenge.exceptions.TooManyRequestsException;
import com.challenge.model.DiscogsAlbumResponse;
import com.challenge.model.DiscogsArtistResponse;
import com.challenge.model.DiscogsSearchArtistResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Service
public class DiscogsApiClient {
    private static final Logger logger = LoggerFactory.getLogger(DiscogsApiClient.class);
    private final WebClient webClient;
    private final String databaseSearchPath;

    private final String artistsReleasesPath;
    private final String artistDetailsPath;
    private static final String QUERY_TYPE_ARTIST = "type=artist";
    private static final String QUERY_PAGE = "page=%d";
    private static final String QUERY_PER_PAGE = "per_page=%d";

    private static final String ERROR_PAGINATION_NOT_FOUND = "Pagination information not found in response.";
    private static final String ERROR_TOO_MANY_REQUESTS = "Rate limit exceeded after %d retries for URL: %s";
    private final String token;
    private static final int MAX_RETRIES = 3;

    private static final int THREADS_NUMBER = 5;
    private static final int PAGE_SIZE = 50;

    private final ObjectMapper objectMapper;

    public DiscogsApiClient(WebClient webClient,
                            @Value("${discogs.api.token}") String token,
                            @Value("${discogs.api.database-search-path}") String databaseSearchPath,
                            @Value("${discogs.api.artists-releases-path}") String artistsReleasesPath,
                            @Value("${discogs.api.artist-details-path}") String artistDetailsPath,
                            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.token = token;
        this.objectMapper = objectMapper;
        this.databaseSearchPath = databaseSearchPath;
        this.artistsReleasesPath = artistsReleasesPath;
        this.artistDetailsPath = artistDetailsPath;
    }

    public DiscogsSearchArtistResponse searchArtist(String artistName, int page, int perPage) throws JsonProcessingException {
        if (artistName == null || artistName.isBlank()) {
            throw new IllegalArgumentException("Artist name must not be null or empty");
        }
        if (page <= 0 || perPage <= 0) {
            throw new IllegalArgumentException("Page and perPage must be greater than 0");
        }
        String url = String.format("%s?q=%s&%s&%s&%s&token=%s",
                databaseSearchPath,
                artistName,
                QUERY_TYPE_ARTIST,
                String.format(QUERY_PAGE, page),
                String.format(QUERY_PER_PAGE, perPage),
                token);
        return fetchAndDeserialize(url, DiscogsSearchArtistResponse.class);
    }

    public DiscogsAlbumResponse getArtistDiscography(String artistId) throws JsonProcessingException {
        if (artistId == null || artistId.isBlank()) {
            throw new IllegalArgumentException("Artist ID must not be null or empty");
        }

        String firstPageUrl = String.format("%s?%s&%s&token=%s",
                String.format(artistsReleasesPath, artistId),
                String.format(QUERY_PAGE, 1),
                String.format(QUERY_PER_PAGE, PAGE_SIZE),
                token);

        List<DiscogsAlbumResponse.Release> allReleases = new ArrayList<>();

        String jsonResponse = fetchRawResponseWithRetry(firstPageUrl, MAX_RETRIES);
        DiscogsAlbumResponse firstPageResponse = objectMapper.readValue(jsonResponse, DiscogsAlbumResponse.class);

        if (firstPageResponse == null || firstPageResponse.getReleases() == null) {
            return new DiscogsAlbumResponse();
        }

        allReleases.addAll(firstPageResponse.getReleases());
        logger.info("Fetched releases from first page: {}", firstPageResponse.getReleases().size());

        int totalPages = getTotalPagesFromResponse(jsonResponse);
        logger.info("Total pages: {}", totalPages);

        List<String> urls = IntStream.rangeClosed(2, totalPages)
                .mapToObj(page -> String.format("%s?%s&%s&token=%s",
                        String.format(artistsReleasesPath, artistId),
                        String.format(QUERY_PAGE, page),
                        String.format(QUERY_PER_PAGE, PAGE_SIZE),
                        token))
                .toList();
        logger.info("Generated URLs for additional pages: {}", urls);


        List<DiscogsAlbumResponse.Release> additionalReleases = fetchReleasesInParallel(urls);
        logger.info("Fetched additional releases: {}", additionalReleases.size());

        allReleases.addAll(additionalReleases);

        DiscogsAlbumResponse finalResponse = new DiscogsAlbumResponse();
        finalResponse.setReleases(allReleases);
        logger.info("Total releases in final response: {}", allReleases.size());

        return finalResponse;
    }

    public DiscogsArtistResponse.Artist getArtistDetails(String artistId) throws JsonProcessingException {
        String url = String.format("%s?token=%s",
                String.format(artistDetailsPath, artistId),
                token);

        return fetchAndDeserialize(url, DiscogsArtistResponse.Artist.class);
    }

    private List<DiscogsAlbumResponse.Release> fetchReleasesInParallel(List<String> urls) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS_NUMBER);
        List<Future<List<DiscogsAlbumResponse.Release>>> futures = new ArrayList<>();

        try {
            for (String url : urls) {
                futures.add(executor.submit(() -> {
                    DiscogsAlbumResponse response = fetchReleasesFromUrlWithRetry(url, 3);
                    return response != null ? response.getReleases() : List.of();
                }));
            }

            List<DiscogsAlbumResponse.Release> releases = new ArrayList<>();
            for (Future<List<DiscogsAlbumResponse.Release>> future : futures) {
                try {
                    releases.addAll(future.get());
                } catch (ExecutionException ex) {
                    if (ex.getCause() instanceof TooManyRequestsException) {
                        logger.error("{}: {}",ERROR_TOO_MANY_REQUESTS, ex.getCause().getMessage());
                    } else {
                        logger.error("Error fetching releases in batch: {}", ex.getCause().getMessage());
                    }
                    throw new ExternalApiException("Batch processing failed for Discogs API", ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new ExternalApiException("Batch processing interrupted", ex);
                }
            }

            return releases;
        } finally {
            executor.shutdown();
        }
    }


    private <T> T fetchAndDeserialize(String url, Class<T> responseType) throws JsonProcessingException {
        String jsonResponse = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        return objectMapper.readValue(jsonResponse, responseType);
    }

    private int getTotalPagesFromResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode paginationNode = rootNode.get("pagination");
            if (paginationNode == null || paginationNode.get("pages") == null) {
                throw new ExternalApiException(ERROR_PAGINATION_NOT_FOUND);
            }
            return paginationNode.get("pages").asInt();
        } catch (JsonProcessingException ex) {
            throw new ExternalApiException("Failed to parse total pages from JSON response.", ex);
        }
    }

    private String fetchRawResponseWithRetry(String url, int retries) {
        return executeWithRetries(
                () -> webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(),
                retries,
                "fetching raw response from URL: " + url
        );
    }

    private DiscogsAlbumResponse fetchReleasesFromUrlWithRetry(String url, int retries) {
        return executeWithRetries(
                () -> {
                    try {
                        return fetchAndDeserialize(url, DiscogsAlbumResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new GeneralApplicationException(e);
                    }
                },
                retries,
                "fetching releases from URL: " + url
        );
    }

    private <T> T executeWithRetries(Supplier<T> action, int retries, String operationDescription) {
        int attempt = 0;
        while (attempt <= retries) {
            try {
                return action.get();
            } catch (WebClientResponseException.TooManyRequests ex) {
                attempt++;
                if (attempt > retries) {
                    throw new TooManyRequestsException("Rate limit exceeded after " + retries + " retries for " + operationDescription, ex);
                }
                int backoff = (int) Math.pow(2, attempt) * 1000;
                logger.warn("429 Too Many Requests. Retrying in {}ms... (Attempt {})", backoff, attempt);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ExternalApiException("Thread interrupted during retry backoff for " + operationDescription, e);
                }
            } catch (Exception ex) {
                throw new ExternalApiException("Unexpected error during " + operationDescription, ex);
            }
        }
        throw new ExternalApiException("Failed after " + retries + " retries for " + operationDescription);
    }
}