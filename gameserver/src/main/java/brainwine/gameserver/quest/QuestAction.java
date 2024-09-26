package brainwine.gameserver.quest;

import java.util.List;

public class QuestAction {
    public static enum Type {
        INTERACT,
        BEGIN,
        DONE;
    }

    public static enum Actor {
        PLAYER,
        ANDROID;
    }

    private Actor actor = Actor.PLAYER;

    private String method;
    
    private List<Object> params;

    public Actor getActor() {
        return actor;
    }

    public String getMethod() {
        return method;
    }

    public List<Object> getParams() {
        return params;
    }
    
}
