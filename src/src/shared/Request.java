package shared;

import java.io.Serializable;

public class Request implements Serializable {

    public enum Type {
        ADD_GAME, REMOVE_GAME, CHANGE_RISK, // Manager operations
        SEARCH, PLAY, ADD_BALANCE,          // Player operations
        RESPONSE                            // Internal operation
    }

    private final Type type;

    public Request(Type type) {
        this.type = type;
    }

    public  Type getType() { return type; }

    @Override
    public String toString() {
        return "Request{type=" + type + "}";
    }
}
