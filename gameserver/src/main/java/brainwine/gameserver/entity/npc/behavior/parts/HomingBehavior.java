package brainwine.gameserver.entity.npc.behavior.parts;

import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.Vector2i;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HomingBehavior extends FlyBehavior {
    @JsonProperty
    private float radius = 30.0f;
    @JsonProperty
    private float chance = 0.2f;

    private boolean triggered;
    private long lastChecked;

    public HomingBehavior(@JacksonInject Npc entity) {
        super(entity);
    }

    @Override
    public boolean canBehave() {
        if(entity.getGuardBlock() == null) return false;
        if(MathUtils.inRange(entity.getX(), entity.getY(), entity.getGuardBlock().getX(), entity.getGuardBlock().getY(), radius)) {
            triggered = false;
        } else {
            if(lastChecked + 1000 < System.currentTimeMillis() && Math.random() < chance) {
                triggered = true;
            }
            lastChecked = System.currentTimeMillis();
        }

        return triggered;
    }

    @Override
    protected float getSpeedMultiplier() {
        return 1.25F;
    }

    @Override
    protected Vector2i getTargetPoint() {
        if(entity.getGuardBlock() == null) return new Vector2i((int)entity.getX(), (int)entity.getY());
        float homeX = entity.getGuardBlock().getX();
        float homeY = entity.getGuardBlock().getY();
        double D = MathUtils.distance(homeX, homeY, entity.getX(), entity.getY());
        double d = MathUtils.lerp(10.0, 30.0, Math.random());
        double dx = d * (homeX - entity.getX()) / D;
        double dy = d * (homeY - entity.getY()) / D;
        return new Vector2i(
                (int)Math.round(entity.getX() + dx),
                (int)Math.round(entity.getY() + dy)
        );
    }
}
