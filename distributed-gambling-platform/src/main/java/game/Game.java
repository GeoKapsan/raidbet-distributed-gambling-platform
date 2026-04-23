package game;

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

    private String bettingCategory;
    private double jackpot;

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

        this.bettingCategory = computeBettingCategory(minBet);
        
    }

    private static double computeJackpot(String riskLevel) {
        switch (riskLevel) {
            case "low":    return 10.0;
            case "medium": return 20.0;
            case "high":   return 40.0;
            default:       return 0.0;
        }
    }

    private static String computeBettingCategory(double minBet) {
        return minBet < 1.0 ? "$" : minBet < 5.0 ? "$$" : "$$$";
    }

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
    public double getJackpot() { return this.jackpot; }

    // --- Setters (for manager operations) ---
    public void setActive(boolean active) { this.active = active; }

    // Recompute jackpot when risk changes
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; this.jackpot = computeJackpot(riskLevel);}

    // recompute category when minBet changes
    public void setMinBet(double minBet) {this.minBet = minBet; this.bettingCategory = computeBettingCategory(minBet);}

    public void setMaxBet(double maxBet) {this.maxBet = maxBet;}
    public void setStars(double stars) { this.stars = stars; }
    public void setNoOfVotes(int noOfVotes) { this.noOfVotes = noOfVotes; }

    public void rate(int stars) {
        setStars((getStars() * getNoOfVotes() + stars) / (noOfVotes + 1));
        setNoOfVotes(noOfVotes + 1);
    }


    public boolean satisfiesFilters(Request request) {
        if (!isActive()) return false;

        String stars = request.containsKey("stars") ? String.valueOf((Integer) request.get("stars")) : null;
        String bettingCategory = request.containsKey("bettingCategory") ? (String) request.get("bettingCategory") : null;
        String riskLevel = request.containsKey("riskLevel") ? (String) request.get("riskLevel") : null;

        if (stars != null && Integer.parseInt(stars) != this.stars) return false;

        if (bettingCategory != null) {
            switch (bettingCategory) {
                case "$":
                    if (minBet < 0.1) return false;
                    break;
                case "$$":
                    if (maxBet < 1) return false;
                    break;
                case "$$$":
                    if (minBet < 5) return false;
                    break;
                default:
                    break;
            }
        }

        if (riskLevel != null && !riskLevel.equals(this.riskLevel)) return false;

        return true;
    }

}
