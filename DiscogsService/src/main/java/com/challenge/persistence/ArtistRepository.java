package com.challenge.persistence;


import com.challenge.dtos.ArtistWithAlbumsDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtistRepository extends JpaRepository<ArtistEntity, Long> {
    Optional<ArtistEntity> findByName(String name);

    Optional<ArtistEntity> findByDiscogsId(String discogsId);

    //this query is for the cases in which I need to retrieve all albums with each artist,
    //otherwise Lazy loading is applied
    @Query("SELECT a FROM ArtistEntity a LEFT JOIN FETCH a.albums WHERE a.discogsId IN :discogsIds")
    List<ArtistEntity> findArtistsWithAlbums(@Param("discogsIds") List<String> discogsIds);
}