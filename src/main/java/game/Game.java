package game;

import shared.Request;

import java.io.Serializable;

public class Game implements Serializable {

    private String gameName;
    private String providerName;
    private double stars;
    private int noOfVotes;
    private String logoPath;
    private float minBet;
    private float maxBet;
    private String riskLevel;
    private String hashKey;
    private boolean active = true;

    private String bettingCategory;
    private float jackpot;

    public Game(String gameName, String providerName, double stars,
                int noOfVotes, String logoPath, float minBet,
                float maxBet, String riskLevel, String hashKey) {
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
        this.jackpot = computeJackpot(riskLevel);
    }

    private static float computeJackpot(String riskLevel) {
        switch (riskLevel) {
            case "low":    return 10f;
            case "medium": return 20f;
            case "high":   return 40f;
            default:       return 0f;
        }
    }

    private static String computeBettingCategory(float minBet) {
        return minBet < 1f ? "$" : minBet < 5f ? "$$" : "$$$";
    }

    // --- Getters ---
    public String getGameName() { return gameName; }
    public String getProviderName() { return providerName; }
    public double getStars() { return stars; }
    public int getNoOfVotes() { return noOfVotes; }
    public String getLogoPath() { return logoPath; }
    public float getMinBet() { return minBet; }
    public float getMaxBet() { return maxBet; }
    public String getRiskLevel() { return riskLevel; }
    public String getHashKey() { return hashKey; }
    public boolean isActive() { return active; }
    public String getBettingCategory() { return bettingCategory; }
    public float getJackpot() { return this.jackpot; }

    // --- Setters (for manager operations) ---
    public void setActive(boolean active) { this.active = active; }

    // Recompute jackpot when risk changes
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; this.jackpot = computeJackpot(riskLevel);}

    // recompute category when minBet changes
    public void setMinBet(float minBet) {this.minBet = minBet; this.bettingCategory = computeBettingCategory(minBet);}

    public void setMaxBet(float maxBet) {this.maxBet = maxBet;}
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

        if (bettingCategory != null && !this.bettingCategory.equals(bettingCategory)) return false;

        if (riskLevel != null && !riskLevel.equals(this.riskLevel)) return false;

        return true;
    }

}
