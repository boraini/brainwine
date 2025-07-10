package brainwine.gameserver.player;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Skill {
    
    AGILITY,
    BARTER,
    BUILDING,
    COMBAT,
    ENGINEERING,
    HORTICULTURE,
    LUCK,
    MINING,
    PERCEPTION,
    STAMINA,
    SURVIVAL;
    
    public static Skill[] getAdvancedSkills() {
        return new Skill[] {COMBAT, ENGINEERING, HORTICULTURE, LUCK, BARTER, SURVIVAL};
    }
    
    public static Skill fromId(String id) {
        for(Skill value : values()) {
            if(value.getId().equalsIgnoreCase(id)) {
                return value;
            }
        }
        
        return null;
    }
    
    @JsonValue
    public String getId() {
        return toString().toLowerCase();
    }
}
