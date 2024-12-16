package com.challenge.dtos;

import java.util.List;

public class ArtistWithAlbumsDTO {
    private String artistName;
    private List<AlbumBasicDto> albums;

    public ArtistWithAlbumsDTO(String artistName, List<AlbumBasicDto> albums) {
        this.artistName = artistName;
        this.albums = albums;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public List<AlbumBasicDto> getAlbums() {
        return albums;
    }

    public void setAlbums(List<AlbumBasicDto> albums) {
        this.albums = albums;
    }
}

