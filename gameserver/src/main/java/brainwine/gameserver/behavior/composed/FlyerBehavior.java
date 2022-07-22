package brainwine.gameserver.behavior.composed;

import java.util.Map;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.behavior.SelectorBehavior;
import brainwine.gameserver.behavior.parts.FlyBehavior;
import brainwine.gameserver.behavior.parts.FlyTowardBehavior;
import brainwine.gameserver.behavior.parts.IdleBehavior;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.util.MapHelper;

public class FlyerBehavior extends SelectorBehavior {
    
    @JsonCreator
    private FlyerBehavior(@JacksonInject Npc entity, 
            Map<String, Object> config) {
        super(entity, config);
    }
    
    public FlyerBehavior(Npc entity) {
        super(entity);
    }
    
    @Override
    public void addChildren(Map<String, Object> config) {
        if(config.containsKey("idle")) {
            addChild(IdleBehavior.class, MapHelper.getMap(config, "idle"));
        }
        
        addChild(FlyTowardBehavior.class, config);
        addChild(FlyBehavior.class, config);
    }
}
