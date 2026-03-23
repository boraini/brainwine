package brainwine.gameserver.minigame;

import brainwine.gameserver.entity.EntityConfig;
import brainwine.gameserver.util.WeightedMap;

import java.util.List;

public class GroupDungeonConfig {
    private long startPeriod = 120000;
    private List<WeightedMap<EntityConfig>> enemies;

    int chaosLevel = 1;

    public long getStartPeriod() {
        return startPeriod;
    }

    public List<WeightedMap<EntityConfig>> getEnemies() {
        return enemies;
    }
}
