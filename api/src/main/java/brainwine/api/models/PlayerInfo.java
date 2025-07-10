package brainwine.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public class PlayerInfo extends PlayerInfoSummary {
    private String apiToken = "";
    private boolean tokenValidated = false;
    private Map<String, Object> statistics;
    public PlayerInfo(
                      String name,
                      int level,
                      int skillLevel,
                      int deaths,
                      int itemsMined,
                      int itemsScavenged,
                      int itemsPlaced,
                      int itemsCrafted,
                      String apiToken,
                      Map<String, Object> statistics
    ) {
        super(name, level, skillLevel, deaths, itemsMined, itemsScavenged, itemsPlaced, itemsCrafted);
        this.apiToken = apiToken;
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

    public Map<String, Object> getStatistics() {
        return statistics;
    }
}
