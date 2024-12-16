package com.challenge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DiscogsArtistResponse {

    @JsonProperty("results")
    private List<Artist> results;

    public List<Artist> getResults() {
        return results;
    }

    public void setResults(List<Artist> results) {
        this.results = results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Artist {
        @JsonProperty("id")
        private String discogsId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("profile")
        private String profile;

        public String getDiscogsId() {
            return discogsId;
        }

        public void setDiscogsId(String id) {
            this.discogsId = id;
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
    }
}
