package brainwine.gameserver.anticheat;

import brainwine.gameserver.zone.Biome;
import brainwine.gameserver.zone.Zone;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Exploration {
    public enum Region {
        SKY,
        UNDERGROUND,
        ALL,
    }

    private Set<Biome> excludedBiomes = new HashSet<>(Arrays.asList(Biome.SPACE));
    private Region stats = Region.ALL;
    private Region worldExplorationPercent = Region.ALL;

    public Set<Biome> getExcludedBiomes() {
        return excludedBiomes;
    }

    public Region getStats() {
        return stats;
    }

    public Region getWorldExplorationPercent() {
        return worldExplorationPercent;
    }

    public boolean isIncluded(Zone zone) {
        return !excludedBiomes.contains(zone.getBiome());
    }

    public boolean shouldTrackStats(Zone zone, int x, int y) {
        return !isIncluded(zone)
                || stats == Region.ALL
                || zone.isChunkUndergroundXY(x, y) ? stats == Region.UNDERGROUND : stats == Region.SKY;
    }
}
