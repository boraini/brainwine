package brainwine.gameserver.anticheat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnticheatConfig {
    @JsonProperty("afk_entity_spawn")
    private AfkEntitySpawn afkEntitySpawn = new AfkEntitySpawn();

    @JsonProperty("exoskeleton")
    private Exoskeleton exoskeleton = new Exoskeleton();

    @JsonProperty("exploder_farm")
    private ExploderFarm exploderFarm = new ExploderFarm();

    @JsonProperty("exploration")
    private Exploration exploration = new Exploration();

    public AfkEntitySpawn getAfkEntitySpawn() {
        return afkEntitySpawn;
    }

    public Exoskeleton getExoskeleton() {
        return exoskeleton;
    }

    public ExploderFarm getExploderFarm() {
        return exploderFarm;
    }

    public Exploration getExploration() {
        return exploration;
    }
}
