package brainwine.gameserver.item;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import brainwine.gameserver.item.consumables.Consumable;
import brainwine.gameserver.item.consumables.ConvertConsumable;
import brainwine.gameserver.item.consumables.HealConsumable;
import brainwine.gameserver.item.consumables.LockWorldConsumable;
import brainwine.gameserver.item.consumables.LootConsumable;
import brainwine.gameserver.item.consumables.NameChangeConsumable;
import brainwine.gameserver.item.consumables.RefillConsumable;
import brainwine.gameserver.item.consumables.SkillConsumable;
import brainwine.gameserver.item.consumables.SkillResetConsumable;
import brainwine.gameserver.item.consumables.StealthConsumable;
import brainwine.gameserver.item.consumables.TeleportConsumable;
import brainwine.gameserver.item.consumables.UnlockWorldConsumable;

/**
 * Action types for items.
 * 
 * All consumables depend on their action type, but not all items with actions are consumables.
 * This creates a bit of an awkward situation in terms of implementation, but we're just gonna have to deal with that.
 */
public enum Action {
    
    CONVERT(new ConvertConsumable()),
    DIG,
    SCRUB,
    HEAL(new HealConsumable()),
    LOCK_WORLD(new LockWorldConsumable()),
    LOOT(new LootConsumable()),
    NAME_CHANGE(new NameChangeConsumable()),
    REFILL(new RefillConsumable()),
    REVIVE,
    SKILL(new SkillConsumable()),
    SKILL_RESET(new SkillResetConsumable()),
    SMASH,
    STEALTH(new StealthConsumable()),
    TELEPORT(new TeleportConsumable()),
    UNLOCK_WORLD(new UnlockWorldConsumable()),
    
    @JsonEnumDefaultValue
    NONE;
    
    private final Consumable consumable;
    
    private Action(Consumable consumable) {
        this.consumable = consumable;
    }
    
    private Action() {
        this(null);
    }
    
    @JsonCreator
    public static Action fromId(String id) {
        String formatted = id.toUpperCase().replace(" ", "_");
        
        for(Action value : values()) {
            if(value.toString().equals(formatted)) {
                return value;
            }
        }
        
        return NONE;
    }
    
    public Consumable getConsumable() {
        return consumable;
    }
}
