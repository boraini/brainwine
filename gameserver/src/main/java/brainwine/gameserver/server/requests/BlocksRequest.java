package brainwine.gameserver.server.requests;

import java.util.ArrayList;
import java.util.List;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.Skill;
import brainwine.gameserver.server.PlayerRequest;
import brainwine.gameserver.server.RequestInfo;
import brainwine.gameserver.server.messages.BlockMetaMessage;
import brainwine.gameserver.server.messages.BlocksMessage;
import brainwine.gameserver.server.messages.LightMessage;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.zone.Chunk;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

@RequestInfo(id = 16)
public class BlocksRequest extends PlayerRequest {

    public int[] chunkIndexes;

    private int getMaxActiveChunkCount(Player player) {
        double side = MathUtils.lerp(4.2, 8.2, player.getNormalizedSkill(Skill.PERCEPTION));
        return (int) (side * side);
    }

    @Override
    public void process(Player player) {
        Zone zone = player.getZone();

        if(!player.isGodMode() && player.getActiveChunkCount() > getMaxActiveChunkCount(player)) {
            return;
        }
        
        List<Chunk> chunks = new ArrayList<>();
        List<MetaBlock> metaBlocks = new ArrayList<>();
        int minX = -1;
        int maxX = -1;
        
        for(int index : chunkIndexes) {
            if(!zone.isChunkIndexInBounds(index)) {
                continue;
            }
            
            int x = index % zone.getNumChunksWidth() * zone.getChunkWidth() + zone.getChunkWidth() / 2;
            int y = index / zone.getNumChunksWidth() * zone.getChunkHeight() + zone.getChunkHeight() / 2;
            double distance = Math.hypot(player.getX() - x, player.getY() - y);
            distance = Math.min(distance, Math.hypot(player.getTeleportX() - x, player.getTeleportY() - y));
            
            if(!player.isGodMode() && distance > zone.getChunkWidth() * 5) {
                continue;
            }
            
            Chunk chunk = zone.getChunk(index);
            
            // Kick player if chunk is null (load failure)
            if(chunk == null) {
                player.kick("Chunk load failure.");
                return;
            }
            
            chunks.add(chunk);
            metaBlocks.addAll(zone.getLocalMetaBlocksInChunk(index));

            // If the player is newly seeing the chunk, mark it as fresh
            if(player.addActiveChunk(index)) {
                chunk.setLoadTime(System.currentTimeMillis());
            }
            
            if(chunk.getX() < minX || minX == -1) {
                minX = chunk.getX();
            }
            
            if(chunk.getX() + chunk.getWidth() > maxX || maxX == -1) {
                maxX = chunk.getX() + chunk.getWidth();
            }
        }
        
        player.sendMessage(new BlocksMessage(chunks));
        
        for(MetaBlock metaBlock : metaBlocks) {
            player.sendMessage(new BlockMetaMessage(metaBlock));
        }
        
        if(minX >= 0 && maxX >= 0) {
            player.sendMessage(new LightMessage(minX, zone.getSunlight(minX, maxX - minX)));
        }
    }
}
