package brainwine.gameserver.zone;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.item.ModType;
import brainwine.gameserver.item.usetypeconfig.ExtendedSteamableConfig;
import brainwine.gameserver.item.usetypeconfig.SteamSourceConfig;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.util.Vector2i;

/**
 * Distributes steam through collectors to nearby machines via pipes.
 */
public class SteamManager {
    
    public static final int STEAM_UPDATE_INTERVAL = 3000; // Update interval in milliseconds
    public static final int MAX_ITERATIONS = 300; // Maximum number of iterations before giving up
    public static final int MAX_COLLECTOR_DISTANCE = 350; // Collectors that are not within this distance of any players in the zone will be skipped
    public static final byte STATE_EMPTY = 0x0; // Nothing or unrelated
    public static final byte STATE_PIPE = 0x1; // Pipe
    public static final byte STATE_COLLECTOR = 0x2; // Active collector
    private final Set<Integer> collectorIndices = new HashSet<>();
    private final Set<Integer> steamSourceIndices = new HashSet<>();
    private final Set<Integer> steamableIndices = new HashSet<>();
    private final Set<Integer> processedIndices = new HashSet<>();
    private final List<Integer> expiredSteamableIndices = new ArrayList<>();
    private final Queue<SteamIteration> processQueue = new ArrayDeque<>();
    private final Map<Integer, Set<Integer>> extendedSteamableInletIndices = new HashMap<>();
    private final Map<Integer, List<Integer>> extendedSteamableMainIndices = new HashMap<>();
    private final Zone zone;
    private byte[] data;
    private long lastUpdateAt;
    
    public SteamManager(Zone zone) {
        this.zone = zone;
        this.data = new byte[(zone.getWidth() * zone.getHeight()) >> 2];
    }
    
    public void tick(double deltaTime) {
        long now = System.currentTimeMillis();
        
        // Check if it's time to update steam yet
        if(now > lastUpdateAt + STEAM_UPDATE_INTERVAL) {
            updateSteam();
            tickSteamSources();
            lastUpdateAt = now;
        }
    }
    
    private void updateSteam() {
        // Do nothing if there are no players in this zone
        if(zone.getPlayerCount() == 0) {
            return;
        }
        
        // Clear data from previous run
        processedIndices.clear();
        expiredSteamableIndices.clear();
        
        // Turn off all steam-powered objects
        for(int index : steamableIndices) {
            int x = index % zone.getWidth();
            int y = index / zone.getWidth();
            
            // Skip if chunk isn't loaded
            if(!zone.isChunkLoaded(x, y)) {
                expiredSteamableIndices.add(index);
                continue;
            }
            
            Block block = zone.getBlock(x, y);
            Item item = block.getFrontItem();
            
            // Skip if front item doesn't use steam
            if(!item.usesSteam()) {
                expiredSteamableIndices.add(index);
                continue;
            }
            
            // Directly set the block mod
            zone.updateBlockMod(x, y, Layer.FRONT, 0);
        }
        
        // Unindex expired steamables
        for(int index : expiredSteamableIndices) {
            steamableIndices.remove(index);
        }
        
        // Enqueue blocks at the spouts of all collectors
        for(int index : collectorIndices) {
            int x = index % zone.getWidth();
            int y = index / zone.getWidth();
            
            // Skip if no player is close to this collector
            if(zone.getPlayersInRange(x, y, MAX_COLLECTOR_DISTANCE).isEmpty()) {
                continue;
            }
            
            // Queue spouts
            processQueue.add(new SteamIteration(x + 1, y - 3, 0, 0)); // Top
            processQueue.add(new SteamIteration(x + 3, y - 1, 1, 0)); // Right
            processQueue.add(new SteamIteration(x + 1, y + 1, 2, 0)); // Bottom
            processQueue.add(new SteamIteration(x - 1, y - 1, 3, 0)); // Left
        }

        // Enqueue blocks at the bottom left side of all steam sources
        for(int index : steamSourceIndices) {
            int x = index % zone.getWidth();
            int y = index / zone.getWidth();

            // Skip if no player is close to this collector
            if(zone.getPlayersInRange(x, y, MAX_COLLECTOR_DISTANCE).isEmpty()) {
                continue;
            }

            Block block = zone.getBlock(x, y);
            Item item = block.getFrontItem();
            SteamSourceConfig steamSource = item.getStructuredUse(ItemUseType.STEAM_SOURCE);
            boolean mirrored = item.isMirrorable() && block.getFrontMod() != 0;

            for(SteamSourceConfig.Outlet outlet : steamSource.getOutlets()) {
                int dx = mirrored ? item.getBlockWidth() - outlet.getPosition().getX() - 1 : outlet.getPosition().getX();
                int dir = mirrored ? (outlet.getDirection() == 3 ? 1 : outlet.getDirection() == 1 ? 3 : outlet.getDirection()): outlet.getDirection();
                processQueue.add(new SteamIteration(x + dx, y + outlet.getPosition().getY(), dir, 0));
            }
        }

        Set<Integer> poweredExtendedSteamableInlets = new HashSet<>();
        // Travel down the pipeline and power on any machines that are reached by it
        while(!processQueue.isEmpty()) {
            SteamIteration iteration = processQueue.poll();
            int depth = iteration.getDepth();
            
            // Skip if depth limit has been reached
            if(depth >= MAX_ITERATIONS) {
                continue;
            }
            
            int x = iteration.getX();
            int y = iteration.getY();
            
            // Skip if coordinates are out of bounds
            if(!zone.areCoordinatesInBounds(x, y)) {
                continue;
            }
            
            int index = zone.getBlockIndex(x, y);
            
            // Skip if block has already been processed
            if(processedIndices.contains(index)) {
                continue;
            }
            
            processedIndices.add(index);
            
            // Skip if block is not a pipe but activate it first if it uses steam
            if(getState(x, y) != STATE_PIPE) {
                if(steamableIndices.contains(index)) {
                    zone.updateBlockMod(x, y, Layer.FRONT, 1); // Directly set the block mod
                }
                
                continue;
            }

            Set<Integer> mainIndices = extendedSteamableInletIndices.get(index);
            if(mainIndices != null && !mainIndices.isEmpty()) {
                poweredExtendedSteamableInlets.add(index);
            }
            
            byte direction = iteration.getDirection();
            int nextDepth = depth + 1;
            
            // Enqueue adjacent blocks for processing
            if(direction != 2) processQueue.add(new SteamIteration(x, y - 1, 0, nextDepth)); // Top
            if(direction != 3) processQueue.add(new SteamIteration(x + 1, y, 1, nextDepth)); // Right
            if(direction != 0) processQueue.add(new SteamIteration(x, y + 1, 2, nextDepth)); // Bottom
            if(direction != 1) processQueue.add(new SteamIteration(x - 1, y, 3, nextDepth)); // Left
        }

        for(int inletIndex : new HashSet<Integer>(extendedSteamableInletIndices.keySet())) {
            boolean powered = poweredExtendedSteamableInlets.contains(inletIndex);
            for(int mainIndex : new HashSet<Integer>(extendedSteamableInletIndices.get(inletIndex))) {
                int x = mainIndex % zone.getWidth();
                int y = mainIndex / zone.getWidth();

                // Items without the extended steam use type had been removed beforehand.
                Block block = zone.getBlock(x, y);
                if(block == null) continue;
                Item item = zone.getBlock(x, y).getFrontItem();
                boolean serverSide = item.getMod() == ModType.ROTATION;
                if(serverSide) {
                    ExtendedSteamableConfig steamable = item.getStructuredUse(ItemUseType.EXTENDED_STEAMABLE);
                    boolean poweredBeforehand = item.hasId(steamable.getOnVariantId());

                    if(poweredBeforehand != powered) {
                        String newItemCode = powered ? steamable.getOnVariantId() : steamable.getOffVariantId();
                        zone.updateBlock(x, y, Layer.FRONT, newItemCode, block.getFrontMod());
                    }
                } else {
                    zone.updateBlockMod(x, y, Layer.FRONT, powered ? 1 : 0);
                }
            }
        }
    }

    public void setSteamSourcePowered(int x, int y, boolean powered, Player owner) {
        Block block = zone.getBlock(x, y);
        Item item = block.getFrontItem();
        SteamSourceConfig steamSource = item.getStructuredUse(ItemUseType.STEAM_SOURCE);
        if(item.isMirrorable()) {
            zone.updateBlock(x, y, Layer.FRONT, ItemRegistry.getItem(powered ? steamSource.getOnVariantId() : steamSource.getOffVariantId()), block.getFrontMod(), owner);
        } else {
            // Update whole block to force it to be re-indexed.
            zone.updateBlock(x, y, Layer.FRONT, block.getFrontItem(), powered ? 1 : 0, owner);
        }
    }

    public boolean isSteamSourcePowered(int x, int y) {
        if(!zone.isChunkLoaded(x, y)) return false;
        Block block = zone.getBlock(x, y);
        Item item = block.getFrontItem();
        if(!item.hasUse(ItemUseType.STEAM_SOURCE)) return false;
        SteamSourceConfig steamSource = item.getStructuredUse(ItemUseType.STEAM_SOURCE);
        if(item.isMirrorable()) {
            return block.getFrontItem().hasId(steamSource.getOnVariantId());
        } else {
            return block.getFrontMod() > 0;
        }
    }

    private void tickSteamSources() {
        // Do nothing if there are no players in this zone
        if(zone.getPlayerCount() == 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        for(MetaBlock metaBlock : zone.getMetaBlocksWithUse(ItemUseType.STEAM_SOURCE)) {
            if(isSteamSourcePowered(metaBlock.getX(), metaBlock.getY())) {
                long f = MapHelper.getLong(metaBlock.getMetadata(), "f", 0);
                if(f > 0 && f < currentTime) {
                    setSteamSourcePowered(metaBlock.getX(), metaBlock.getY(), false, metaBlock.getOwner());
                }
            }
        }
    }

    public void unindexBlock(int x, int y) {
        int index = zone.getBlockIndex(x, y);

        List<Integer> inlets = extendedSteamableMainIndices.remove(index);
        if(inlets != null) for(int inlet : inlets) {
            Set<Integer> steamableIndices = extendedSteamableInletIndices.get(inlet);
            if(steamableIndices != null) steamableIndices.remove(index);
        }
    }
    
    public void indexBlock(int x, int y, Item item) {
        int index = zone.getBlockIndex(x, y);

        unindexBlock(x, y);

        // Does it use steam?
        if(item.usesSteam()) {
            steamableIndices.add(index);
            setState(index, STATE_EMPTY);
            return;
        }

        // Is it a pipe?
        if(item.hasId("mechanical/pipe") || item.hasId("mechanical/pipeiron") || item.hasId("mechanical/pipecopper")) {
            setState(index, STATE_PIPE);
            return;
        }

        // Is it a collector and is it on top of a steam vent?
        if(item.hasId("mechanical/collector") && isCollectorActive(x, y)) {
            collectorIndices.add(index);
            setState(index, STATE_COLLECTOR);
            return;
        } else {
            collectorIndices.remove(index);
        }

        // Is it a powered steam source
        if(isSteamSourcePowered(x, y)) {
            steamSourceIndices.add(index);
            setState(index, STATE_COLLECTOR);
            return;
        } else {
            steamSourceIndices.remove(index);
        }

        if(item.hasUse(ItemUseType.EXTENDED_STEAMABLE)) {
            ExtendedSteamableConfig steamable = item.getStructuredUse(ItemUseType.EXTENDED_STEAMABLE);

            Block block = zone.getBlock(x, y);
            boolean flipped = item.isMirrorable() && block.getFrontMod() != 0;
            List<Integer> inletIndices = new ArrayList<>(steamable.getInlets().size());
            for(Vector2i position : steamable.getInlets()) {
                int worldX = x + (flipped ? (item.getBlockWidth() - position.getX() - 1) : position.getX());
                int worldY = y + position.getY();

                if(zone.areCoordinatesInBounds(worldX, worldY)) {
                    int inletIndex = zone.getBlockIndex(worldX, worldY);

                    extendedSteamableInletIndices.computeIfAbsent(inletIndex, HashSet::new).add(index);
                    inletIndices.add(inletIndex);
                }
            }
            extendedSteamableMainIndices.put(index, inletIndices);
        }

        setState(index, STATE_EMPTY);
    }
    
    private boolean isCollectorActive(int x, int y) {
        return zone.isChunkLoaded(x + 1, y - 1) && zone.getBlock(x + 1, y - 1).getBaseItem().hasId("base/vent");
    }
    
    protected void setData(byte[] data) {
        // Do nothing if data is null
        if(data == null) {
            return;
        }
        
        int size = zone.getWidth() * zone.getHeight();
        
        // Do nothing if data size is incorrect
        if(data.length << 2 != size) {
            return;
        }
        
        this.data = data;
        
        // Index active collectors
        for(int i = 0; i < size; i++) {
            if(getState(i) == STATE_COLLECTOR) {
                collectorIndices.add(i);
            }
        }
    }
    
    private void setState(int index, byte state) {
        int byteOffset = index >> 2;
        int bitOffset = (index % 4) << 1;
        data[byteOffset] &= ~(0x3 << bitOffset); // Clear bits
        data[byteOffset] |= (state & 0x3) << bitOffset; // Set bits
    }
    
    private int getState(int x, int y) {
        return getState(zone.getBlockIndex(x, y));
    }
    
    private int getState(int index) {
        return (data[index >> 2] >> ((index % 4) << 1)) & 0x3;
    }
    
    protected byte[] getData() {
        return data;
    }
}
