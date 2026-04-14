package shared;

import java.io.Serializable;
import java.util.HashMap;

public class Request implements Serializable {

    public enum Type {
        PROVIDER_PROFIT, PLAYER_PROFIT,                 // Manager operations
        ADD_GAME, REMOVE_GAME, MODIFY_GAME,
        SEARCH, PLAY, RATE_GAME,                        // Player operations
        REDUCER_CALLBACK,                               // Reducer -> Master operation
        GIVE_NUMBER,
        RESPONSE                                        // Internal operation
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
    public boolean containsKey(String key) {
        return payload.containsKey(key);
    }

    @Override
    public String toString() {
        return "Request{type=" + type + "}";
    }
}
