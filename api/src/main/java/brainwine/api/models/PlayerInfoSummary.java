package brainwine.api.models;

import java.util.HashMap;
import java.util.Map;

public class PlayerInfoSummary {
    private String name = "Unknown Player";
    private int level;
    private int skillLevel;
    private int deaths;
    private int itemsMined;
    private int itemsScavenged;
    private int itemsPlaced;
    private int itemsCrafted;
    private Map<String, Object> statistics;

    public PlayerInfoSummary(String name, int level, int skillLevel, int deaths, int itemsMined, int itemsScavenged, int itemsPlaced, int itemsCrafted) {
        this.name = name;
        this.level = level;
        this.skillLevel = skillLevel;
        this.deaths = deaths;
        this.itemsMined = itemsMined;
        this.itemsScavenged = itemsScavenged;
        this.itemsPlaced = itemsPlaced;
        this.itemsCrafted = itemsCrafted;
        this.statistics = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getItemsMined() {
        return itemsMined;
    }

    public int getItemsScavenged() {
        return itemsScavenged;
    }

    public int getItemsPlaced() {
        return itemsPlaced;
    }

    public int getItemsCrafted() {
        return itemsCrafted;
    }

    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }
}
