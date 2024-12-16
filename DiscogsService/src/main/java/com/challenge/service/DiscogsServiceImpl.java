package com.challenge.service;


import com.challenge.api.core.Album;
import com.challenge.api.core.Artist;
import com.challenge.api.core.ArtistComparison;
import com.challenge.api.core.DiscogsService;
import com.challenge.dtos.AlbumBasicDto;
import com.challenge.dtos.ArtistWithAlbumsDTO;
import com.challenge.exceptions.*;
import com.challenge.helper.HelperMethods;
import com.challenge.model.DiscogsAlbumResponse;
import com.challenge.model.DiscogsArtistResponse;
import com.challenge.model.DiscogsSearchArtistResponse;
import com.challenge.persistence.*;
import com.challenge.services.DiscogsApiClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class DiscogsServiceImpl implements DiscogsService {

    private final DiscogsApiClient discogsApiClient;
    private final ArtistRepository artistRepository;
    private final AlbumRepository albumRepository;

    private final HelperMethods helperMethods;

    public DiscogsServiceImpl(DiscogsApiClient discogsApiClient, ArtistRepository artistRepository,
                              AlbumRepository albumRepository, HelperMethods helperMethods) {

        this.discogsApiClient = discogsApiClient;
        this.artistRepository = artistRepository;
        this.albumRepository = albumRepository;
        this.helperMethods = helperMethods;
    }

    @Override
    public List<Artist> searchArtists(String artistName, int page, int perPage) throws JsonProcessingException {
        DiscogsSearchArtistResponse response = discogsApiClient.searchArtist(artistName, page, perPage);

        if (response == null || response.getResults() == null) {
            return List.of();
        }

        return response.getResults().stream()
                .map(result -> {
                    Artist artist = new Artist();
                    artist.setDiscogsId(result.getId());
                    artist.setName(result.getName());
                    return artist;
                })
                .toList();
    }

    @Override
    public Page<Album> getAndStoreArtistDiscography(String artistId, boolean sorted, int page, int perPage) {
        if (page < 1 || perPage < 1) {
            throw new IllegalArgumentException("Page and perPage must be greater than 0");
        }

        helperMethods.validateArtistId(artistId);

        try {
            Optional<ArtistEntity> existingArtist = findArtistByDiscogsId(artistId);

            Page<AlbumEntity> albumPage;

            if (existingArtist.isPresent()) {
                Long artistDbId = existingArtist.get().getId();
                albumPage = fetchAlbumsFromDatabase(artistDbId, sorted, page, perPage);
            } else {
                DiscogsArtistResponse.Artist artistResponse = discogsApiClient.getArtistDetails(artistId);

                if (artistResponse == null || artistResponse.getName() == null) {
                    throw new ExternalApiException("No valid artist data returned from Discogs API");
                }

                ArtistEntity artistEntity = HelperMethods.buildArtistEntity(artistResponse);

                DiscogsAlbumResponse albumResponse = discogsApiClient.getArtistDiscography(artistId);

                if (albumResponse == null || albumResponse.getReleases() == null) {
                    throw new ExternalApiException("No valid album data returned from Discogs API");
                }

                List<AlbumEntity> albumEntities = HelperMethods.getAlbumEntities(albumResponse, artistEntity);

                artistEntity.setAlbums(albumEntities);

                saveArtistAndAlbums(artistEntity);

                Pageable pageable = sorted
                        ? PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.ASC, "year"))
                        : PageRequest.of(page - 1, perPage);

                albumPage = HelperMethods.buildAlbumPage(sorted, page, perPage, albumEntities, pageable);
            }

            return albumPage.map(albumEntity -> new Album(
                    albumEntity.getTitle(),
                    albumEntity.getYear(),
                    albumEntity.getFormat(),
                    albumEntity.getLabel()
            ));

        } catch (ExternalApiException | NotFoundException | TooManyRequestsException | IllegalArgumentException e ) {
            throw e;
        } catch (Exception e) {
            throw new GeneralApplicationException("Unexpected error occurred while processing artist discography", e);
        }
    }

    @Retryable(
            value = {CannotGetJdbcConnectionException.class, DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    @Override
    public List<ArtistComparison> compareArtists(List<String> discogsIds) {
        if (discogsIds == null || discogsIds.isEmpty()) {
            throw new IllegalArgumentException("Discogs IDs must not be null or empty");
        }

        for (String artistId : discogsIds) {
            helperMethods.validateArtistId(artistId);
        }

        try {
            List<ArtistEntity> artistEntities = artistRepository.findArtistsWithAlbums(discogsIds);

            if (artistEntities.size() < 2) {
                List<String> foundDiscogsIds = artistEntities.stream()
                        .map(ArtistEntity::getDiscogsId)
                        .toList();

                List<String> missingDiscogsIds = discogsIds.stream()
                        .filter(id -> !foundDiscogsIds.contains(id))
                        .toList();

                throw new ResourceNotFoundException(
                        "Insufficient artists found. Missing Discogs IDs: " + missingDiscogsIds
                );
            }

            List<ArtistWithAlbumsDTO> artists = HelperMethods.artistEntityToDto(artistEntities);


            return artists.stream()
                    .map(artist -> {
                        List<AlbumBasicDto> albums = artist.getAlbums();

                        int numberOfReleases = albums.size();

                        int firstYear = HelperMethods.getFirstYear(albums);

                        int lastYear = HelperMethods.getLastYear(albums);

                        int activeYears = (firstYear > 0 && lastYear > 0) ? lastYear - firstYear + 1 : 0;

                        return new ArtistComparison(artist.getArtistName(), numberOfReleases, activeYears);
                    })
                    .toList();

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralApplicationException("Unexpected error occurred while comparing artists", e);
        }
    }
    @Retryable(
            value = {CannotGetJdbcConnectionException.class, DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    private Optional<ArtistEntity> findArtistByDiscogsId(String discogsId) {
        return artistRepository.findByDiscogsId(discogsId);
    }

    @Retryable(
            value = {CannotGetJdbcConnectionException.class, DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    private Page<AlbumEntity> fetchAlbumsFromDatabase(Long artistDbId, boolean sorted, int page, int perPage) {
        Pageable pageable = sorted
                ? PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.ASC, "year"))
                : PageRequest.of(page - 1, perPage);

        return albumRepository.findAlbumsByArtistId(artistDbId, pageable);
    }

    @Retryable(
            value = {CannotGetJdbcConnectionException.class, DataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    private void saveArtistAndAlbums(ArtistEntity artistEntity) {
        artistRepository.save(artistEntity);
    }
}
