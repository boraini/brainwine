package brainwine.gameserver.zone;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import brainwine.gameserver.util.MathUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.util.WeightedMap;
import brainwine.shared.JsonHelper;

/**
 * Manages plant growth in a zone.
 */
public class GrowthManager {
    
    public static final int MAX_RAIN_CYCLES = 500; // Maximum number of rain cycles that are permitted in a single growth update
    private static final String GROW_LAMP = "lighting/grow-lamp-lit";
    private static final double GROW_LAMP_CENTER_OFFSET = 0.5;
    private static final double GROW_LAMP_RANGE = 5.0;
    private static final Logger logger = LogManager.getLogger();
    private static final Map<Item, Growable> growables = new HashMap<>();
    private static final Map<Biome, Map<Item, WeightedMap<Item>>> sourcesByBiome = new HashMap<>();
    private final Set<Integer> sourceIndices = new HashSet<>();
    private final Map<Item, WeightedMap<Item>> sources;
    private final Zone zone;
    private final Set<MetaBlock>[] growLampsByChunkIndex;
    
    public GrowthManager(Zone zone) {
        this.sources = sourcesByBiome.getOrDefault(zone.getBiome(), Collections.emptyMap());
        this.zone = zone;
        growLampsByChunkIndex = new Set[zone.getNumChunksWidth() * zone.getNumChunksHeight()];
    }
    
    public static void loadGrowthData() {
        growables.clear();
        sourcesByBiome.clear();
        
        try {
            URL url = ResourceFinder.getResourceUrl("growth.json");
            Map<String, Object> data = JsonHelper.readValue(url, new TypeReference<Map<String, Object>>(){});
            growables.putAll(JsonHelper.readValue(data.get("growables"), new TypeReference<Map<Item, Growable>>(){}));
            sourcesByBiome.putAll(JsonHelper.readValue(data.get("sources"), new TypeReference<Map<Biome, Map<Item, WeightedMap<Item>>>>(){}));
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load growth data", e);
        }
    }

    public boolean isReceivingLight(int x, int y) {
        if(zone.getSunlight()[x] >= y) {
            return true;
        } else {
            int c = zone.getChunkIndex(x, y);
            if(isReceivingLightFromGrowLamps(x, y, c)) return true;
            boolean checkLeft = x >= zone.getChunkWidth();
            boolean checkRight = x < zone.getWidth() - zone.getChunkWidth();
            boolean checkTop = y >= zone.getChunkHeight();
            if(checkLeft && isReceivingLightFromGrowLamps(x, y, c - 1)) return true;
            if(checkRight && isReceivingLightFromGrowLamps(x, y, c + 1)) return true;
            if(checkTop && isReceivingLightFromGrowLamps(x, y, c - zone.getNumChunksWidth())) return true;
            if(checkTop && checkLeft && isReceivingLightFromGrowLamps(x, y, c - zone.getNumChunksWidth() - 1)) return true;
            return checkTop && checkRight && isReceivingLightFromGrowLamps(x, y, c - zone.getNumChunksWidth() + 1);
        }
    }

    private boolean isReceivingLightFromGrowLamps(int x, int y, int c) {
        Set<MetaBlock> selection = growLampsByChunkIndex[c];
        if(selection != null) for(MetaBlock growLamp : selection) {
            if(y >= growLamp.getY() && MathUtils.distance(x, y, growLamp.getX() + GROW_LAMP_CENTER_OFFSET, growLamp.getY()) < GROW_LAMP_RANGE) {
                return true;
            }
        }
        return false;
    }

    public boolean fertilize(int x, int y) {
        if(!isReceivingLight(x, y)) return false;
        if(!zone.isPurified()) return false;
        if(!zone.areCoordinatesInBounds(x, y) || !zone.areCoordinatesInBounds(x, y + 1)) return false;

        int replaceY = -1;
        int plantY = -1;
        int sourceY = -1;
        Item replacementItem = Item.AIR;
        Item plantItem = Item.AIR;

        Item belowItem = zone.getBlock(x, y + 1).getFrontItem();
        if(belowItem.hasId("ground/earth-compost")) {
            // Fertilizer placed directly on compost.
            plantY = y;
            sourceY = y + 1;
        } else if(growables.containsKey(belowItem)) {
            // Fertilizer placed onto plant.
            if(!zone.areCoordinatesInBounds(x, y + 2)) return false;
            plantItem = belowItem;
            plantY = y + 1;
            replacementItem = growables.containsKey(plantItem) ? growables.get(plantItem).getReplaceSource() : null;
            replaceY = y + 2;
        } else {
            if(belowItem.isAir()) {
                // Fertilizer placed above compost - same behaviour as immediate placement.
                sourceY = y + 2;
                plantY = y + 1;
            } else {
                // Fertilizer placed onto bulb.
                WeightedMap<Item> source = sources.get(belowItem);
                if(source == null || source.isEmpty()) return false;
                plantItem = source.next();
                replacementItem = growables.containsKey(plantItem) ? growables.get(plantItem).getReplaceSource() : null;
                replaceY = y + 1;
                plantY = y + 1;
            }
        }

        if(sourceY != -1) {
            if(!zone.areCoordinatesInBounds(x, sourceY)) return false;
            WeightedMap<Item> source = sources.get(zone.getBlock(x, sourceY).getFrontItem());
            if(source == null || source.isEmpty()) return false;
            plantItem = source.next();
        }

        zone.updateBlock(x, y, Layer.FRONT, Item.AIR);
        // stack blocks on top of each other
        int currentY = y + 2;
        if(replaceY != -1) {
            currentY = Math.min(currentY, replaceY);
            if(replacementItem != null) zone.updateBlock(x, currentY, Layer.FRONT, replacementItem);
            currentY--;
        }
        if(plantY != -1) {
            currentY = Math.min(currentY, plantY);
            zone.updateBlock(x, currentY, Layer.FRONT, plantItem, growables.containsKey(plantItem) ? growables.get(plantItem).getMaxMod() : 0);
            currentY--;
        }

        return true;
    }
    
    /**
     * Calls {@link #updateGrowables(int, Collection)} where {@code sourceIndices} is the currently indexed growables.
     */
    public void updateGrowables(int rainCycles) {
        updateGrowables(rainCycles, sourceIndices);
    }
    
    /**
     * Updates the specified growables {@code n} times where {@code n} is the number of rain cycles.
     */
    public void updateGrowables(int rainCycles, Collection<Integer> sourceIndices) {        
        // Do nothing if zone isn't purified
        if(!zone.isPurified()) {
            return;
        }
        
        // Do nothing if there's nothing to do... duh!
        if(rainCycles < 1 || sourceIndices.isEmpty()) {
            return;
        }
        
        // Reduce overhead by reducing the number of iterations in exchange for a growth chance boost
        rainCycles = Math.min(MAX_RAIN_CYCLES, rainCycles);
        int growthChanceBoost = Math.min(10, rainCycles);
        rainCycles /= growthChanceBoost;

        // Update growth for each rain cycle
        List<Integer> indices = new ArrayList<>(sourceIndices);
        for(int i = 0; i < rainCycles; i++) {
            for(int index : indices) {
                int x = index % zone.getWidth();
                int y = index / zone.getWidth();
                
                // Unindex if chunk is not loaded
                if(y == 0 || !zone.isChunkLoaded(x, y)) {
                    sourceIndices.remove(index);
                    continue;
                }
                
                // Skip if sunlight can't reach this source
                if(!isReceivingLight(x, y)) {
                    continue;
                }
                
                Block sourceBlock = zone.getBlock(x, y);
                Item sourceItem = sourceBlock.getFrontItem();
                
                // Unindex if block is not a source
                if(!sources.containsKey(sourceItem)) {
                    sourceIndices.remove(index);
                    continue;
                }
                
                Block growableBlock = zone.getBlock(x, y - 1);
                Item growableItem = growableBlock.getFrontItem();
                
                // Place a random growable if block isn't occupied or try to grow it if it is a valid growable
                if(growableItem.isAir()) {
                    growableItem = sources.get(sourceItem).next();
                    
                    // Update block if item exists
                    if(growableItem != null) {
                        zone.updateBlock(x, y - 1, Layer.FRONT, growableItem);
                    }
                } else if(growables.containsKey(growableItem)) {
                    Growable growable = growables.get(growableItem);
                    int mod = growableBlock.getFrontMod();
                    
                    // Try to apply a growth stage if the plant can still grow
                    if(mod < growable.getMaxMod() && Math.random() < growable.getChance() * growthChanceBoost) {
                        zone.updateBlock(x, y - 1, Layer.FRONT, growableItem, ++mod);
                        
                        // Replace source block if max mod has been reached
                        if(growable.getReplaceSource() != null && mod >= growable.getMaxMod()) {
                            zone.updateBlock(x, y, Layer.FRONT, growable.getReplaceSource());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Attempts to index the block and returns {@code true} if the block's front item is a valid growth source.
     */
    public boolean indexBlock(int x, int y, Item item) {
        if(!sources.containsKey(item)) {
            return false;
        }
        
        sourceIndices.add(zone.getBlockIndex(x, y));
        return true;
    }

    public void indexMetaBlock(int index, MetaBlock metaBlock) {
        if(metaBlock.getItem().hasId(GROW_LAMP)) {
            int chunkIndex = zone.getChunkIndex(metaBlock.getX(), metaBlock.getY());
            Set<MetaBlock> selection = growLampsByChunkIndex[chunkIndex];
            if(selection == null) selection = growLampsByChunkIndex[chunkIndex] = new HashSet<>();
            selection.add(metaBlock);
        }
    }

    public void unindexMetaBlock(int index) {
        int x = index % zone.getWidth();
        int y = index / zone.getWidth();
        int chunkIndex = zone.getChunkIndex(x, y);
        Set<MetaBlock> selection = growLampsByChunkIndex[chunkIndex];
        if(selection != null) selection.removeIf(metaBlock -> metaBlock.getX() == x && metaBlock.getY() == y);
    }
}
