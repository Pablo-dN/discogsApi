package com.challenge.helper;

import com.challenge.dtos.AlbumBasicDto;
import com.challenge.dtos.ArtistWithAlbumsDTO;
import com.challenge.model.DiscogsAlbumResponse;
import com.challenge.model.DiscogsArtistResponse;
import com.challenge.persistence.AlbumEntity;
import com.challenge.persistence.ArtistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class HelperMethods {

    public static final String RELEASE = "release";
    public static final String MASTER = "master";
    public void validateArtistId(String artistId) {
        if (artistId == null || artistId.isBlank()) {
            throw new IllegalArgumentException("Artist ID must not be blank");
        }
        if (!artistId.matches("\\d+")) {
            throw new IllegalArgumentException("Artist ID must be numeric");
        }
    }

    public static Page<AlbumEntity> buildAlbumPage(boolean sorted, int page, int perPage, List<AlbumEntity> albumEntities, Pageable pageable) {
        Page<AlbumEntity> albumPage;
        albumPage = new PageImpl<>(
                albumEntities.stream()
                        .sorted(sorted
                                        ? Comparator.comparing(
                                        AlbumEntity::getYear,
                                        Comparator.nullsLast(Comparator.naturalOrder())
                                )
                                        : Comparator.comparing(AlbumEntity::getId)
                        )
                        .skip((long) (page - 1) * perPage)
                        .limit(perPage)
                        .toList(),
                pageable,
                albumEntities.size()
        );
        return albumPage;
    }

    public static List<AlbumEntity> getAlbumEntities(DiscogsAlbumResponse albumResponse, ArtistEntity artistEntity) {
        return albumResponse.getReleases().stream()
                .filter(release -> RELEASE.equals(release.getType()) || MASTER.equals(release.getType()))
                .map(release -> new AlbumEntity(
                        artistEntity,
                        release.getTitle(),
                        release.getYear(),
                        release.getFormat(),
                        release.getLabel(),
                        release.getType(),
                        release.getId()
                ))
                .toList();
    }

    public static ArtistEntity buildArtistEntity(DiscogsArtistResponse.Artist artistResponse) {
        return new ArtistEntity(
                artistResponse.getDiscogsId(),
                artistResponse.getName(),
                artistResponse.getProfile()
        );
    }

    public static int getLastYear(List<AlbumBasicDto> albums) {
        return albums.stream()
                .filter(album -> album.getYear() != null)
                .mapToInt(AlbumBasicDto::getYear)
                .max()
                .orElse(0);
    }

    public static int getFirstYear(List<AlbumBasicDto> albums) {
        return albums.stream()
                .filter(album -> album.getYear() != null)
                .mapToInt(AlbumBasicDto::getYear)
                .min()
                .orElse(0);
    }

    public static List<ArtistWithAlbumsDTO> artistEntityToDto(List<ArtistEntity> artistEntities) {
        return artistEntities.stream()
                .map(artist -> new ArtistWithAlbumsDTO(
                        artist.getName(),
                        artist.getAlbums().stream()
                                .map(album -> new AlbumBasicDto(album.getTitle(), album.getYear()))
                                .toList()
                ))
                .toList();
    }
}
