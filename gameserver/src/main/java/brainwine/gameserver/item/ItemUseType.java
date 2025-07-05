package brainwine.gameserver.item;

import brainwine.gameserver.item.usetypeconfig.*;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

import brainwine.gameserver.item.interactions.BatteryInteraction;
import brainwine.gameserver.item.interactions.BurstInteraction;
import brainwine.gameserver.item.interactions.ChangeInteraction;
import brainwine.gameserver.item.interactions.ComposterInteraction;
import brainwine.gameserver.item.interactions.ContainerInteraction;
import brainwine.gameserver.item.interactions.DialogInteraction;
import brainwine.gameserver.item.interactions.ExpiatorInteraction;
import brainwine.gameserver.item.interactions.FertilizerInteraction;
import brainwine.gameserver.item.interactions.GeckInteraction;
import brainwine.gameserver.item.interactions.ItemInteraction;
import brainwine.gameserver.item.interactions.LandmarkInteraction;
import brainwine.gameserver.item.interactions.MinigameInteraction;
import brainwine.gameserver.item.interactions.NoteInteraction;
import brainwine.gameserver.item.interactions.QuipperInteraction;
import brainwine.gameserver.item.interactions.RecyclerInteraction;
import brainwine.gameserver.item.interactions.SpawnInteraction;
import brainwine.gameserver.item.interactions.SpawnTeleportInteraction;
import brainwine.gameserver.item.interactions.SummoningCircleInteraction;
import brainwine.gameserver.item.interactions.SwitchInteraction;
import brainwine.gameserver.item.interactions.TargetTeleportInteraction;
import brainwine.gameserver.item.interactions.TeleportInteraction;
import brainwine.gameserver.item.interactions.TransmitInteraction;
import brainwine.gameserver.item.interactions.WarmthInteraction;
import brainwine.gameserver.item.interactions.WorldMachineInteraction;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Much like with {@link Action}, block interactions depend on their use type.
 */
public enum ItemUseType {
    
    AFTERBURNER,
    BATTERY(new BatteryInteraction(), BatteryConfig.class),
    BREATH,
    BURST(new BurstInteraction()),
    COMPOSTER(new ComposterInteraction()),
    CONTAINER(new ContainerInteraction()),
    CREATE_DIALOG(new DialogInteraction(true)),
    DESTROY,
    DIALOG(new DialogInteraction(false)),
    DOWSING,
    EXPIATOR(new ExpiatorInteraction()),
    EXTENDED_STEAMABLE(ExtendedSteamableConfig.class),
    GECK(new GeckInteraction()),
    GUARD,
    CHANGE(new ChangeInteraction()),
    FERTILIZER(new FertilizerInteraction()),
    FIELDABLE,
    FLY,
    HAZMAT,
    LANDMARK(new LandmarkInteraction()),
    MEMORY,
    MINIGAME(new MinigameInteraction()),
    MOVE,
    MULTI,
    NOTE(new NoteInteraction()),
    PET,
    PLENTY,
    PROTECTED,
    PUBLIC,
    QUIPPER(new QuipperInteraction()),
    RECYCLER(new RecyclerInteraction()),
    SUMMONING_CIRCLE(new SummoningCircleInteraction()),
    SPAWN(new SpawnInteraction()),
    SPAWN_TELEPORT(new SpawnTeleportInteraction()),
    STEAM_SOURCE(SteamSourceConfig.class),
    SUPPRESS_BOMB,
    SWITCH(new SwitchInteraction()),
    SWITCHED,
    TARGET_TELEPORT(new TargetTeleportInteraction()),
    TELEPORT(new TeleportInteraction()),
    TRIGGER,
    TRANSMIT(new TransmitInteraction()),
    TRANSMITTED,
    WARMTH(new WarmthInteraction()),
    WORLD_MACHINE(new WorldMachineInteraction()),
    ZONE_TELEPORT,
    
    @JsonEnumDefaultValue
    UNKNOWN;
    
    private final ItemInteraction interaction;
    private final Class<? extends ItemUseTypeConfig> configType;
    
    private ItemUseType(ItemInteraction interaction, Class<? extends ItemUseTypeConfig> configType) {
        this.interaction = interaction;
        this.configType = configType;
    }

    private ItemUseType(Class<? extends ItemUseTypeConfig> configType) {
        this(null, configType);
    }

    private ItemUseType(ItemInteraction interaction) {
        this(interaction, ItemUseTypeConfig.class);
    }
    
    private ItemUseType() {
        this(null, ItemUseTypeConfig.class);
    }
    
    @JsonCreator
    public static ItemUseType fromId(String id) {
        String formatted = id.toUpperCase().replace(" ", "_").replace("-", "_");
        
        for(ItemUseType value : values()) {
            if(value.toString().equals(formatted)) {
                return value;
            }
        }
        
        return UNKNOWN;
    }
    
    public ItemInteraction getInteraction() {
        return interaction;
    }

    public ItemUseTypeConfig parseConfig(Object use) throws JsonProcessingException {
        if(configType.equals(ItemUseTypeConfig.class)) {
            // Playing it safe here
            return new ItemUseTypeConfig().setConfig(use);
        } else {
            // Handle the case where the use is set to true
            Properties[] props = configType.getAnnotationsByType(Properties.class);
            if(props.length == 0 || props[0].allowsDefault()) {
                try {
                    // Some config types might want to parse the Boolean
                    ItemUseTypeConfig result = JsonHelper.readValue(use, configType).setConfig(use);
                    return result;
                } catch(JsonProcessingException e) {
                    if(use instanceof Boolean) {
                        return getDefaultConfig();
                    } else {
                        throw e;
                    }
                }
            }

            // Just try to parse and throw any exceptions
            return JsonHelper.readValue(use, configType).setConfig(use);
        }
    }

    public ItemUseTypeConfig getDefaultConfig() {
        try {
            return configType.getConstructor().newInstance();
        } catch(Exception e) {
            throw new RuntimeException("Fatal error in getting the default config for item use type " + this, e);
        }
    }
}
