package game; //test1

import shared.Request;

import java.io.Serializable;

public class Game implements Serializable {

    private String gameName;
    private String providerName;
    private double stars;
    private int noOfVotes;
    private String logoPath;
    private double minBet;
    private double maxBet;
    private String riskLevel;
    private String hashKey;
    private boolean active = true;

    private final String bettingCategory;
    private final double jackpot;

    public Game(String gameName, String providerName, double stars,
                int noOfVotes, String logoPath, double minBet,
                double maxBet, String riskLevel, String hashKey) {
        this.gameName = gameName;
        this.providerName = providerName;
        this.stars = stars;
        this.noOfVotes = noOfVotes;
        this.logoPath = logoPath;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.riskLevel = riskLevel;
        this.hashKey = hashKey;

        this.bettingCategory = minBet < 1.0 ? "$" : minBet < 5.0 ? "$$" : "$$$";
        switch (riskLevel) {
            case "low":

                this.jackpot = 10.0;
                break;

            case "medium":

                this.jackpot = 20.0;
                break;

            case "high":

                this.jackpot = 40.0;
                break;

            default:
                this.jackpot=0.0;
                break;
        }    }

    // --- Getters ---
    public String getGameName() { return gameName; }
    public String getProviderName() { return providerName; }
    public double getStars() { return stars; }
    public int getNoOfVotes() { return noOfVotes; }
    public String getLogoPath() { return logoPath; }
    public double getMinBet() { return minBet; }
    public double getMaxBet() { return maxBet; }
    public String getRiskLevel() { return riskLevel; }
    public String getHashKey() { return hashKey; }
    public boolean isActive() { return active; }
    public String getBettingCategory() { return bettingCategory; }
    public double getJackpot() { return jackpot; }

    // --- Setters (for manager operations) ---
    public void setActive(boolean active) { this.active = active; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setStars(double stars) { this.stars = stars; }
    public void setNoOfVotes(int noOfVotes) { this.noOfVotes = noOfVotes; }

    public boolean satisfiesFilters(Request request) {
        int stars = (Integer) request.get("stars");
        String bettingCategory = (String) request.get("bettingCategory");
        String riskLevel = (String) request.get("riskLevel");

        if (!isActive()) return false;
        if (stars != this.stars) return false;

        switch (bettingCategory) {
            case "$":
                if (minBet < 0.1) return false;
                break;
            case "$$":
                if (maxBet < 1) return false;
                break;
            case "$$$":
                if (minBet < 5) return false;
        }

        if (riskLevel.equals(this.riskLevel)) return false;

        return true;
    }

}
