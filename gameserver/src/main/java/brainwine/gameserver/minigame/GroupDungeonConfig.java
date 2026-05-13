package brainwine.gameserver.minigame;

import brainwine.gameserver.entity.EntityRegistry;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.util.MapHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupDungeonConfig {
    private static final Logger logger = LogManager.getLogger();

    private long gracePeriod = 120000;
    private int maxPlayerBumps = 4;
    private int maxTotalBumps = 0;
    private double spawnRadius = 10.0;
    private List<DoorState> doors = Arrays.asList(
            new DoorState(
                    new BlockState("mechanical/door-beefy", 1, null),
                    new BlockState("mechanical/door-beefy", 0, null)
            )
    );
    private List<BlockState> speakers = Arrays.asList(
            new BlockState("mechanical/speaker", 0, null)
    );

    private Map<Integer, List<Map<String, Integer>>> enemies = MapHelper.map(1, Arrays.asList(MapHelper.map(
            String.class, Integer.class,
            "brains/small-minion", 5,
            "brains/medium-minion", 2,
            "brains/medium-dire-minion", 1
    )));

    public long getGracePeriod() {
        return gracePeriod;
    }

    public int getMaxPlayerBumps() {
        return maxPlayerBumps;
    }

    public int getMaxTotalBumps() {
        return maxTotalBumps;
    }

    public double getSpawnRadius() {
        return spawnRadius;
    }

    @JsonSetter
    public void setEnemies(Map<Integer, List<Map<String, Integer>>> enemies) {
        if(enemies.isEmpty()) {
            throw new IllegalArgumentException("No enemy tables for the group dungeon are configured.");
        }

        Map<Integer, List<Map<String, Integer>>> newEnemies = new HashMap<>();

        for(Map.Entry<Integer, List<Map<String, Integer>>> waveEntry : enemies.entrySet()) {
            if(waveEntry.getKey() <= 0) continue;
            List<Map<String, Integer>> originalConfigs = enemies.get(waveEntry.getKey());
            if(originalConfigs.isEmpty()) {
                throw new IllegalArgumentException(String.format("No possibilities are specified for the group dungeon wave %d.", waveEntry.getKey()));
            }
            List<Map<String, Integer>> configs = new ArrayList<>();
            for(Map<String, Integer> original : enemies.get(waveEntry.getKey())) {
                Map<String, Integer> filtered = original.entrySet().stream().filter(ent -> EntityRegistry.getEntityConfig(ent.getKey()) != null && ent.getValue() != null && ent.getValue() > 0.0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                if(filtered.isEmpty()) {
                    throw new IllegalArgumentException(String.format("None of the entity IDs in the enemy table for the group dungeon wave %d are valid.", waveEntry.getKey()));
                }
                configs.add(filtered);
            }
            newEnemies.put(waveEntry.getKey(), configs);
        }

        if(!newEnemies.isEmpty()) {
            // Make sure the first wave config is present
            if(!this.enemies.containsKey(1)) {
                int minKey = newEnemies.keySet().stream().min(Integer::compareTo).orElse(1);
                newEnemies.put(1, newEnemies.get(minKey));
            }
            this.enemies = newEnemies;
        } else {
            logger.warn("None of the group dungeon waves are positive - not configuring wave enemies.");
        }
    }

    public Map<Integer, List<Map<String, Integer>>> getEnemies() {
        return enemies;
    }

    public List<DoorState> getDoors() {
        return doors;
    }

    @JsonIgnore
    public List<Item> getAllDoorItems() {
        return doors.stream()
                .flatMap(door -> Stream.of(door.getOpen().getItem(), door.getClosed().getItem()))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<Item> getAllSpeakerItems() {
        return speakers.stream().map(BlockState::getItem).collect(Collectors.toList());
    }

    public static class BlockState {
        public Item item;
        public int mod;
        public Map<String, Object> metadata;

        public BlockState(@JsonSetter String itemId, @JsonSetter int mod, @JsonSetter Map<String, Object> metadata) {
            this(ItemRegistry.getItem(itemId), mod, metadata);
        }

        public BlockState(Item item, int mod, Map<String, Object> metadata) {
            if(item == null) {
                throw new IllegalArgumentException("Item null not found.");
            }

            if(item.isAir()) {
                throw new IllegalArgumentException("Item " + item.getId() + " not found.");
            }

            this.item = item;
            this.mod = mod;
            this.metadata = metadata;
        }

        public Item getItem() {
            return item;
        }

        public int getMod() {
            return mod;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof BlockState)) return false;
            BlockState that = (BlockState)o;
            return mod == that.mod && Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item, mod);
        }
    }

    public static class DoorState {
        private BlockState open;
        private BlockState closed;

        public DoorState(@JsonSetter BlockState open, @JsonSetter BlockState closed) {
            this.open = open;
            this.closed = closed;
        }

        public BlockState getOpen() {
            return open;
        }

        public BlockState getClosed() {
            return closed;
        }
    }
}
