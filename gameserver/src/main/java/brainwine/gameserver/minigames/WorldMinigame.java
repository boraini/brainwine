package brainwine.gameserver.minigames;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        // Composed
        @JsonSubTypes.Type(name = "world_per_player", value = WorldPerPlayerMinigame.class),
        @JsonSubTypes.Type(name = "tile_matching", value = TileMatchingMinigame.class),
})
public abstract class WorldMinigame {
    protected static final Logger logger = LogManager.getLogger();
    protected Zone zone;

    /**Called when the zone is first created or loaded.*/
    protected void initialize() {}

    public void initialize(Zone zone) {
        this.zone = zone;
        initialize();
    }

    /**Called every time the zone ticks.*/
    public void tick(float deltaTime) {}

    /**Called when the given player uses a new
     *
     * @return true iff the block use should also be handled as usual.
     */
    public boolean useBlock(Player player, int x, int y, MetaBlock metablock) { return true; }

    /**Called when a player leaves the zone.*/
    public void leaveZone(Player player) {}
}
