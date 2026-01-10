package brainwine.gameserver.server.messages;

import brainwine.gameserver.server.Message;
import brainwine.gameserver.server.MessageInfo;

@MessageInfo(id = 56)
public class MissiveInfoMessage extends Message {
    private String type;
    private Object data;
    private MissiveInfoMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public static MissiveInfoMessage unreadCount(int count) {
        return new MissiveInfoMessage("u", count);
    }

    public String getType() {
        return type;
    }

    public Object getData() {
        return data;
    }
}
