package com.challenge.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlbumRepository extends JpaRepository<AlbumEntity, Long> {
    List<AlbumEntity> findByArtistIdOrderByYearAsc(Long artistId);

    @Query("SELECT al FROM AlbumEntity al WHERE al.artist.id = :artistId")
    Page<AlbumEntity> findAlbumsByArtistId(@Param("artistId") Long artistId, Pageable pageable);
}
