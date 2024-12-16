package com.challenge.api.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Discogs", description = "REST API for Discogs service.")
@RequestMapping("/discogs")
public interface DiscogsService {

    @Operation(
            summary = "Search for an artist",
            description = "Searches the Discogs database for the given artist name, with optional pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Artists found"),
            @ApiResponse(responseCode = "404", description = "No results found"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @GetMapping(value = "/search/{artistName}", produces = "application/json")
    List<Artist> searchArtists(
            @PathVariable @NotBlank(message = "Artist name must not be blank") String artistName,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "PerPage must be at least 1") int perPage
    ) throws JsonProcessingException;

    @Operation(
            summary = "Get and store artist discography with pagination",
            description = "Fetches the discography (list of releases) for a specific artist from Discogs, stores it in the database, and returns the list of albums with pagination."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Discography retrieved and stored successfully"),
            @ApiResponse(responseCode = "404", description = "Artist not found"),
            @ApiResponse(responseCode = "400", description = "Invalid artist ID provided")
    })
    @PostMapping(value = "/artists/{artistId}/discography", produces = "application/json")
    Page<Album> getAndStoreArtistDiscography(
            @PathVariable @NotBlank(message = "Artist ID must not be blank") String artistId,
            @RequestParam(value = "sorted", defaultValue = "false") boolean sorted,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "Page must be at least 1") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "PerPage must be at least 1") int perPage
    );

    @Operation(
            summary = "Compare artists based on their discography",
            description = "Compares two or more artists based on the number of releases and active years (from the first to the most recent release)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparison performed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid artist IDs provided"),
            @ApiResponse(responseCode = "404", description = "One or more artists not found")
    })
    @GetMapping(value = "/artists/compare", produces = "application/json")
    List<ArtistComparison> compareArtists(
            @RequestParam @NotEmpty(message = "Artist IDs list must not be empty") List<@NotBlank(message = "Each Artist ID must not be blank") String> artistIds
    );
}
