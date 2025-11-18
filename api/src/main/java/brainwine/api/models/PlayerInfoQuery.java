package brainwine.api.models;

import java.util.HashMap;
import java.util.Map;

public class PlayerInfoQuery {
    Map<String, Integer> orderLevel = new HashMap<>();

    public PlayerInfoQuery setOrderLevel(String orderKey, int value) {
        orderLevel.put(orderKey, value);
        return this;
    }

    public Map<String, Integer> getOrderLevel() {
        return orderLevel;
    }
}
