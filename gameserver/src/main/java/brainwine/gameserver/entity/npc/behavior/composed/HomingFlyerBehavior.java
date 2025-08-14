package brainwine.gameserver.entity.npc.behavior.composed;

import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.behavior.SelectorBehavior;
import brainwine.gameserver.entity.npc.behavior.parts.FlyBehavior;
import brainwine.gameserver.entity.npc.behavior.parts.FlyTowardBehavior;
import brainwine.gameserver.entity.npc.behavior.parts.HomingBehavior;
import brainwine.gameserver.entity.npc.behavior.parts.IdleBehavior;
import brainwine.gameserver.util.MapHelper;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

public class HomingFlyerBehavior extends SelectorBehavior {

    @JsonCreator
    private HomingFlyerBehavior(@JacksonInject Npc entity,
                                Map<String, Object> config) {
        super(entity, config);
    }
    
    @Override
    public void addChildren(Map<String, Object> config) {
        if(config.containsKey("idle")) {
            addChild(IdleBehavior.class, MapHelper.getMap(config, "idle"));
        }

        addChild(HomingBehavior.class, config);
        addChild(FlyTowardBehavior.class, config);
        addChild(FlyBehavior.class, config);
    }
}
