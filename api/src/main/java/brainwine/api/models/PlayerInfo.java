package brainwine.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class PlayerInfo extends PlayerInfoSummary {
    private String apiToken = "";
    private boolean tokenValidated = false;
    private Map<String, Integer> orders;
    private Map<String, String> appearance;
    private Map<String, Object> statistics;
    public PlayerInfo(
                      String name,
                      int level,
                      int skillLevel,
                      boolean admin,
                      int deaths,
                      int itemsMined,
                      int itemsScavenged,
                      int itemsPlaced,
                      int itemsCrafted,
                      String apiToken,
                      Map<String, Integer> orders,
                      Map<String, String> appearance,
                      Map<String, Object> statistics
    ) {
        super(name, level, skillLevel, admin, deaths, itemsMined, itemsScavenged, itemsPlaced, itemsCrafted);
        this.apiToken = apiToken;
        this.orders = orders;
        this.appearance = appearance;
        this.statistics = statistics;
    }

    public void setTokenValidated() {
        tokenValidated = true;
    }

    public boolean isTokenValidated() {
        return tokenValidated;
    }

    @JsonIgnore
    public String getApiToken() {
        return apiToken;
    }

    public Map<String, Integer> getOrders() {
        return orders;
    }

    public Map<String, String> getAppearance() {
        return appearance;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }
}
