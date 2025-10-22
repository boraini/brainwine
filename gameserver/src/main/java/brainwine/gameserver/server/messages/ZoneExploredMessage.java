package brainwine.gameserver.server.messages;

import brainwine.gameserver.server.Message;
import brainwine.gameserver.server.MessageInfo;

@MessageInfo(id = 53, collection = true)
public class ZoneExploredMessage extends Message {
    
    public int chunkIndex;
    public float explorationPercent;
    
    public ZoneExploredMessage(int chunkIndex, float explorationPercent) {
        this.chunkIndex = chunkIndex;
        this.explorationPercent = explorationPercent;
    }
}
