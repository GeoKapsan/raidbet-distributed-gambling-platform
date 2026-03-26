package shared;

import java.io.Serializable;
import java.util.HashMap;

public class Request implements Serializable {

    public enum Type {
        ADD_GAME, REMOVE_GAME, CHANGE_RISK, SHOW_GAMES, GIVE_NUMBER, // Manager operations
        SEARCH, PLAY,                       // Player operations
        RESPONSE                            // Internal operation
    }

    private final Type type;
    private final HashMap<String, Object> payload = new HashMap<>();

    public Request(Type type) {
        this.type = type;
    }

    public void put(String key, Object value) {
        payload.put(key, value);
    }
    public Object get(String key) {
        return payload.get(key);
    }
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Request{type=" + type + "}";
    }
}
