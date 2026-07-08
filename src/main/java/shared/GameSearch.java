package shared;

import java.io.Serializable;

public class GameSearch implements Serializable {
    private final String gameName;
    private final byte[] img;
    private final float minBet;
    private final float maxBet;
    private final String riskLevel;
    private final String bettingCategory;
    private final double stars;
    private final float jackpot;

    public GameSearch(String gameName, byte[] img, float minBet, float maxBet, String riskLevel, String bettingCategory, double stars, float jackpot) {
        this.gameName = gameName;
        this.img = img;
        this.minBet = minBet;
        this.maxBet = maxBet;
        this.riskLevel = riskLevel;
        this.bettingCategory = bettingCategory;
        this.stars = stars;
        this.jackpot = jackpot;

    }

    public String getGameName() {
        return gameName;
    }
    public byte[] getImg() {
        return img;
    }
    public float getMinBet() {return minBet;}
    public float getMaxBet() {return maxBet;}
    public String getRiskLevel() {return riskLevel;}
    public String getBettingCategory() {return bettingCategory;}
    public double getStars() {return stars;}
    public float getJackpot() {return jackpot;}
}
