package com.challenge.api.core;

public class ArtistComparison {
    private String artistName;
    private int numberOfReleases;
    private int activeYears;

    public ArtistComparison(String artistName, int numberOfReleases, int activeYears) {
        this.artistName = artistName;
        this.numberOfReleases = numberOfReleases;
        this.activeYears = activeYears;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public int getNumberOfReleases() {
        return numberOfReleases;
    }

    public void setNumberOfReleases(int numberOfReleases) {
        this.numberOfReleases = numberOfReleases;
    }

    public int getActiveYears() {
        return activeYears;
    }

    public void setActiveYears(int activeYears) {
        this.activeYears = activeYears;
    }
}
