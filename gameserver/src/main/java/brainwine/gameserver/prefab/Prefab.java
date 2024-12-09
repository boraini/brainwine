package brainwine.gameserver.prefab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.util.Pair;
import brainwine.gameserver.util.Vector2i;
import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.util.WeightedMap;
import brainwine.gameserver.zone.Block;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Prefab {
    
    private String name;
    private boolean dungeon;
    private boolean ruin;
    private boolean loot;
    private boolean decay;
    private boolean mirrorable;
    private int width;
    private int height;
    private Block[] blocks;
    private Map<Item, WeightedMap<Item>> replacements = new HashMap<>();
    private Map<Item, CorrespondingReplacement> correspondingReplacements = new HashMap<>();
    private Map<Integer, Map<String, Object>> metadata = new HashMap<>();
    @JsonIgnore
    private List<Pair<Vector2i, Vector2i>> occupiedAreas = null;
    
    protected Prefab(String name, PrefabConfigFile config, PrefabBlocksFile blockData) {
        this(name, blockData.getWidth(), blockData.getHeight(), blockData.getBlocks(), config.getMetadata());
        dungeon = config.isDungeon();
        ruin = config.isRuin();
        loot = config.hasLoot();
        decay = config.hasDecay();
        mirrorable = config.isMirrorable();
        replacements = config.getReplacements();
        correspondingReplacements = config.getCorrespondingReplacements();
    }
    
    public Prefab(String name, int width, int height, Block[] blocks, Map<Integer, Map<String, Object>> metadata) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.blocks = blocks;
        this.metadata = metadata;
    }
    
    @JsonCreator
    private static Prefab fromName(String name) {
        return GameServer.getInstance().getPrefabManager().getPrefab(name);
    }

    public List<Pair<Vector2i, Vector2i>> getOccupiedAreas() {
        if(occupiedAreas == null) {
            occupiedAreas = new ArrayList<>();

            if(blocks != null) for(int i = 0; i < blocks.length; i++) {
                Item frontItem = blocks[i].getFrontItem();
                if(frontItem != null && frontItem.hasUse(ItemUseType.TELEPORT)) {
                    int x = i % getWidth();
                    int y = i / getWidth();
                    occupiedAreas.add(new Pair<>(
                            new Vector2i(x, y),
                            new Vector2i(x + frontItem.getBlockWidth(), y + frontItem.getBlockHeight())
                    ));
                }
            }
        }

        return occupiedAreas;
    }

    public boolean occupies(int x, int y, boolean mirrored) {
        if(x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) return false;

        int mirroredX = mirrored ? getWidth() - x - 1 : x;

        for(Pair<Vector2i, Vector2i> area : getOccupiedAreas()) {
            if(mirroredX >= area.getFirst().getX() && y >= area.getFirst().getY()
                    && mirroredX < area.getLast().getX() && y < area.getLast().getY()) {
                return true;
            }
        }

        return false;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isDungeon() {
        return dungeon;
    }
    
    public boolean isRuin() {
        return ruin;
    }
    
    public boolean hasLoot() {
        return loot;
    }
    
    public boolean hasDecay() {
        return decay;
    }
    
    public boolean isMirrorable() {
        return mirrorable;
    }
        
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public Block[] getBlocks() {
        return blocks;
    }
    
    public Map<String, Object> getMetadata(int index) {
        return metadata.get(index);
    }
    
    public Map<Integer, Map<String, Object>> getMetadata() {
        return metadata;
    }
    
    public Map<Item, WeightedMap<Item>> getReplacements() {
        return replacements;
    }
    
    public Map<Item, CorrespondingReplacement> getCorrespondingReplacements() {
        return correspondingReplacements;
    }
}
