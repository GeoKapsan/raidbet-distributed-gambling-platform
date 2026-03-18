package game; //test

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

        this.bettingCategory = minBet < 1.0 ? "$" : minBet < 5.0 ? "$$" : "$$$"; // to leei FUN sto pdf dk gt
        this.jackpot = maxBet * 100; // den eida pou to leei apla vgazei error
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
    public double getJackpot() { return jackpot; }

    // --- Setters (for manager operations) ---
    public void setActive(boolean active) { this.active = active; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public void setStars(double stars) { this.stars = stars; }
    public void setNoOfVotes(int noOfVotes) { this.noOfVotes = noOfVotes; }

    /*@Override
    public String toString() {
        return String.format("[%s | %s | Stars:%.1f | Bet:%s | Risk:%s | Jackpot:%.1f]",
            gameName, bettingCategory, stars, minBet + "-" + maxBet, riskLevel, jackpot);
    } */

    // i put it as comments because no use for now
}
