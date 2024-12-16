package com.challenge.service;

import com.challenge.api.core.Album;
import com.challenge.api.core.Artist;
import com.challenge.api.core.ArtistComparison;
import com.challenge.exceptions.ExternalApiException;
import com.challenge.exceptions.GeneralApplicationException;
import com.challenge.exceptions.ResourceNotFoundException;
import com.challenge.helper.HelperMethods;
import com.challenge.model.DiscogsAlbumResponse;
import com.challenge.model.DiscogsArtistResponse;
import com.challenge.model.DiscogsSearchArtistResponse;
import com.challenge.persistence.AlbumEntity;
import com.challenge.persistence.AlbumRepository;
import com.challenge.persistence.ArtistEntity;
import com.challenge.persistence.ArtistRepository;
import com.challenge.services.DiscogsApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscogsServiceImplTest {

    @Mock
    private DiscogsApiClient discogsApiClientMock;

    @Mock
    private ArtistRepository artistRepositoryMock;

    @Mock
    private AlbumRepository albumRepositoryMock;

    @Mock
    private HelperMethods helperMethodsMock;

    @InjectMocks
    private DiscogsServiceImpl discogsService;

    private static final String ARTIST_NAME = "The Beatles";

    private static final String ALBUM_NAME_1 = "Album 2";

    private static final String ALBUM_NAME_2 = "Album 3";

    private static final String FORMAT = "vinyl";

    private static final String LABEL_1 = "Label 1";

    private static final String TYPE = "release";

    @Test
    void testSearchArtistsSuccess() throws JsonProcessingException {

        int page = 1;
        int perPage = 10;

        DiscogsSearchArtistResponse mockResponse = new DiscogsSearchArtistResponse();
        DiscogsSearchArtistResponse.SearchResult artist1 = new DiscogsSearchArtistResponse.SearchResult();
        artist1.setId("1");
        artist1.setName(ARTIST_NAME);
        DiscogsSearchArtistResponse.SearchResult artist2 = new DiscogsSearchArtistResponse.SearchResult();
        artist2.setId("2");
        artist2.setName(ARTIST_NAME);

        mockResponse.setResults(List.of(artist1, artist2));


        when(discogsApiClientMock.searchArtist(ARTIST_NAME, page, perPage)).thenReturn(mockResponse);


        List<Artist> result = discogsService.searchArtists(ARTIST_NAME, page, perPage);


        assertEquals(2, result.size());
        assertEquals(ARTIST_NAME, result.get(0).getName());
        assertEquals("1", result.get(0).getDiscogsId());
        verify(discogsApiClientMock, times(1)).searchArtist(ARTIST_NAME, page, perPage);
    }

    @Test
    void testGetAndStoreArtistDiscographyNewArtist() throws JsonProcessingException {
        String artistId = "123";
        boolean sorted = true;
        int page = 1;
        int perPage = 10;

        DiscogsArtistResponse.Artist mockArtist = new DiscogsArtistResponse.Artist();
        mockArtist.setDiscogsId("123");
        mockArtist.setName(ARTIST_NAME);

        DiscogsAlbumResponse.Release release1 = new DiscogsAlbumResponse.Release();
        release1.setId(1L);
        release1.setTitle(ALBUM_NAME_1);
        release1.setYear(1965);
        release1.setFormat(FORMAT);
        release1.setLabel(LABEL_1);
        release1.setType(TYPE);

        DiscogsAlbumResponse.Release release2 = new DiscogsAlbumResponse.Release();
        release2.setId(2L);
        release2.setTitle(ALBUM_NAME_2);
        release2.setYear(1970);
        release2.setFormat(FORMAT);
        release2.setType(TYPE);

        DiscogsAlbumResponse mockAlbumResponse = new DiscogsAlbumResponse();
        mockAlbumResponse.setReleases(List.of(release1, release2));

        doNothing().when(helperMethodsMock).validateArtistId(artistId);
        when(artistRepositoryMock.findByDiscogsId(artistId)).thenReturn(Optional.empty());
        when(discogsApiClientMock.getArtistDetails(artistId)).thenReturn(mockArtist);
        when(discogsApiClientMock.getArtistDiscography(artistId)).thenReturn(mockAlbumResponse);

        ArgumentCaptor<ArtistEntity> artistEntityCaptor = ArgumentCaptor.forClass(ArtistEntity.class);

        Page<Album> result = discogsService.getAndStoreArtistDiscography(artistId, sorted, page, perPage);

        assertEquals(2, result.getContent().size());
        assertEquals(ALBUM_NAME_1, result.getContent().get(0).getTitle());

        verify(artistRepositoryMock, times(1)).save(artistEntityCaptor.capture());
        assertEquals(ARTIST_NAME, artistEntityCaptor.getValue().getName());
        assertEquals(2, artistEntityCaptor.getValue().getAlbums().size());
    }

    @Test
    void testCompareArtistsSuccess() {
        List<String> discogsIds = List.of("123", "456");

        ArtistEntity artist1 = new ArtistEntity();
        artist1.setId(1L);
        artist1.setDiscogsId("123");
        artist1.setName("Artist 1");

        AlbumEntity album1 = new AlbumEntity(artist1, ALBUM_NAME_1, 2000, FORMAT, LABEL_1, TYPE, 1L);
        AlbumEntity album2 = new AlbumEntity(artist1, ALBUM_NAME_2, 2005, FORMAT, LABEL_1, TYPE, 2L);
        artist1.setAlbums(List.of(album1, album2));

        ArtistEntity artist2 = new ArtistEntity();
        artist2.setId(2L);
        artist2.setDiscogsId("456");
        artist2.setName("Artist 2");

        AlbumEntity album3 = new AlbumEntity(artist2, ALBUM_NAME_1, 1995, FORMAT, LABEL_1, TYPE, 3L);
        AlbumEntity album4 = new AlbumEntity(artist2, ALBUM_NAME_2, 2000, FORMAT, LABEL_1, TYPE, 4L);
        artist2.setAlbums(List.of(album3, album4));

        when(artistRepositoryMock.findArtistsWithAlbums(discogsIds)).thenReturn(List.of(artist1, artist2));

        List<ArtistComparison> result = discogsService.compareArtists(discogsIds);

        assertEquals(2, result.size());

        ArtistComparison artistComparison1 = result.get(0);
        assertEquals("Artist 1", artistComparison1.getArtistName());
        assertEquals(2, artistComparison1.getNumberOfReleases());
        assertEquals(6, artistComparison1.getActiveYears());

        ArtistComparison artistComparison2 = result.get(1);
        assertEquals("Artist 2", artistComparison2.getArtistName());
        assertEquals(2, artistComparison2.getNumberOfReleases());
        assertEquals(6, artistComparison2.getActiveYears());

        verify(artistRepositoryMock, times(1)).findArtistsWithAlbums(discogsIds);
    }

    @Test
    void testCompareArtistsResourceNotFound() {
        // Arrange
        List<String> discogsIds = List.of("123", "456");
        when(artistRepositoryMock.findArtistsWithAlbums(discogsIds)).thenReturn(List.of());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () ->
            discogsService.compareArtists(discogsIds)
        );

        assertTrue(exception.getMessage().contains("Insufficient artists found"));
        verify(artistRepositoryMock, times(1)).findArtistsWithAlbums(discogsIds);
    }

    @Test
    void testSearchArtistsResponseNull() throws JsonProcessingException {
        String artistName = "Unknown Artist";
        int page = 1;
        int perPage = 10;

        when(discogsApiClientMock.searchArtist(artistName, page, perPage)).thenReturn(null);

        List<Artist> result = discogsService.searchArtists(artistName, page, perPage);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(discogsApiClientMock, times(1)).searchArtist(artistName, page, perPage);
    }

    @Test
    void testGetAndStoreArtistDiscographyInvalidPageParameters() {
        String artistId = "123";
        boolean sorted = true;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            discogsService.getAndStoreArtistDiscography(artistId, sorted, 0, 10)
        );

        assertEquals("Page and perPage must be greater than 0", exception.getMessage());
    }

    @Test
    void testGetAndStoreArtistDiscographyExistingArtistWithAlbums() {
        String artistId = "123";
        boolean sorted = true;
        int page = 1;
        int perPage = 10;

        ArtistEntity existingArtist = new ArtistEntity();
        existingArtist.setId(1L);

        AlbumEntity album1 = new AlbumEntity(existingArtist, ALBUM_NAME_1, 2000, FORMAT, LABEL_1, TYPE, 1L);
        AlbumEntity album2 = new AlbumEntity(existingArtist, ALBUM_NAME_2, 2005, FORMAT, LABEL_1, TYPE, 2L);

        Page<AlbumEntity> albumPage = new PageImpl<>(List.of(album1, album2), PageRequest.of(0, 10, Sort.by("year")), 2);

        when(artistRepositoryMock.findByDiscogsId(artistId)).thenReturn(Optional.of(existingArtist));
        when(albumRepositoryMock.findAlbumsByArtistId(1L, PageRequest.of(0, 10, Sort.by("year")))).thenReturn(albumPage);

        Page<Album> result = discogsService.getAndStoreArtistDiscography(artistId, sorted, page, perPage);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(ALBUM_NAME_1, result.getContent().get(0).getTitle());
        assertEquals(ALBUM_NAME_2, result.getContent().get(1).getTitle());
        verify(artistRepositoryMock, times(1)).findByDiscogsId(artistId);
    }

    @Test
    void testGetAndStoreArtistDiscographyInvalidArtistResponse() throws JsonProcessingException {
        String artistId = "123";
        boolean sorted = true;
        int page = 1;
        int perPage = 10;

        when(artistRepositoryMock.findByDiscogsId(artistId)).thenReturn(Optional.empty());
        when(discogsApiClientMock.getArtistDetails(artistId)).thenReturn(null);

        ExternalApiException exception = assertThrows(ExternalApiException.class, () ->
            discogsService.getAndStoreArtistDiscography(artistId, sorted, page, perPage)
        );

        assertEquals("No valid artist data returned from Discogs API", exception.getMessage());
    }

    @Test
    void testGetAndStoreArtistDiscographyInvalidAlbumResponse() throws JsonProcessingException {
        String artistId = "123";
        boolean sorted = true;
        int page = 1;
        int perPage = 10;

        DiscogsArtistResponse.Artist mockArtist = new DiscogsArtistResponse.Artist();
        mockArtist.setDiscogsId("123");
        mockArtist.setName(ARTIST_NAME);

        when(artistRepositoryMock.findByDiscogsId(artistId)).thenReturn(Optional.empty());
        when(discogsApiClientMock.getArtistDetails(artistId)).thenReturn(mockArtist);
        when(discogsApiClientMock.getArtistDiscography(artistId)).thenReturn(null);

        ExternalApiException exception = assertThrows(ExternalApiException.class, () ->
            discogsService.getAndStoreArtistDiscography(artistId, sorted, page, perPage)
        );

        assertEquals("No valid album data returned from Discogs API", exception.getMessage());
    }

    @Test
    void testCompareArtistsGeneralException() {
        List<String> discogsIds = List.of("123", "456");

        when(artistRepositoryMock.findArtistsWithAlbums(discogsIds)).thenThrow(new RuntimeException("Database error"));

        GeneralApplicationException exception = assertThrows(GeneralApplicationException.class, () ->
            discogsService.compareArtists(discogsIds)
        );

        assertTrue(exception.getMessage().contains("Unexpected error occurred while comparing artists"));
    }

    @Test
    void testCompareArtistsInvalidDiscogsIds() {
        IllegalArgumentException exceptionNull = assertThrows(IllegalArgumentException.class, () ->
            discogsService.compareArtists(null)
        );
        assertEquals("Discogs IDs must not be null or empty", exceptionNull.getMessage());

        IllegalArgumentException exceptionEmpty = assertThrows(IllegalArgumentException.class, () ->
            discogsService.compareArtists(Collections.emptyList())
        );
        assertEquals("Discogs IDs must not be null or empty", exceptionEmpty.getMessage());
    }
}