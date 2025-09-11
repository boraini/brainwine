package brainwine.gameserver.anticheat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AnticheatConfig {
    @JsonProperty("exploder_farm")
    private ExploderFarm exploderFarm = new ExploderFarm();

    @JsonProperty("afk_entity_spawn")
    private AfkEntitySpawn afkEntitySpawn = new AfkEntitySpawn();

    public ExploderFarm getExploderFarm() {
        return exploderFarm;
    }

    public AfkEntitySpawn getAfkEntitySpawn() {
        return afkEntitySpawn;
    }
}
