package brainwine.api.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerInfoQuery {
    Map<String, Integer> orderLevel = new HashMap<>();
    Set<String> statistics = new HashSet<>();

    public PlayerInfoQuery setOrderLevel(String orderKey, int value) {
        orderLevel.put(orderKey, value);
        return this;
    }

    public PlayerInfoQuery addStatistic(String key) {
        statistics.add(key);
        return this;
    }

    public Map<String, Integer> getOrderLevel() {
        return orderLevel;
    }

    public Set<String> getStatistics() {
        return statistics;
    }
}
