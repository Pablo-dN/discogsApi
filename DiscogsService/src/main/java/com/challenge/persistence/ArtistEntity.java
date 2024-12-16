package com.challenge.persistence;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "artist", indexes = { @Index(name = "artist_unique_idx", columnList = "id", unique = true) })
public class ArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discogs_id", nullable = false, unique = true)
    private String discogsId;

    @Column(nullable = false)
    private String name;


    @Column(columnDefinition = "TEXT")
    private String profile;

    @OneToMany(mappedBy = "artist", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AlbumEntity> albums;

    public ArtistEntity() {
    }

    public ArtistEntity(String discogsId, String name, String profile) {
        this.discogsId = discogsId;
        this.name = name;
        this.profile = profile;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public List<AlbumEntity> getAlbums() {
        return albums;
    }

    public void setAlbums(List<AlbumEntity> albums) {
        this.albums = albums;
    }

    public String getDiscogsId() {
        return discogsId;
    }

    public void setDiscogsId(String discogsId) {
        this.discogsId = discogsId;
    }
}