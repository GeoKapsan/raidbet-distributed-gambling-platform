package shared;

import java.io.Serializable;

public class GameSearch implements Serializable {
    private final String gameName;
    private final byte[] img;

    public GameSearch(String gameName, byte[] img) {
        this.gameName = gameName;
        this.img = img;
    }
    public String getGameName() {
        return gameName;
    }
    public byte[] getImg() {
        return img;
    }
}
