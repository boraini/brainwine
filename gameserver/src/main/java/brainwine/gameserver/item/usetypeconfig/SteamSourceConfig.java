package brainwine.gameserver.item.usetypeconfig;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.util.Pair;
import brainwine.gameserver.util.Vector2i;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

@Properties(allowsDefault = false)
public class SteamSourceConfig extends ItemUseTypeConfig {
    @JsonProperty("off variant")
    String offVariantId = Item.AIR.getId();

    @JsonProperty("on variant")
    String onVariantId = Item.AIR.getId();

    @JsonProperty("outlets")
    List<Outlet> outlets;

    public SteamSourceConfig() {
        this.outlets = Arrays.asList(new Outlet(new Vector2i(-1, 0), 3));
    }

    @JsonCreator
    public SteamSourceConfig(List<Outlet> outlets) {
        this.outlets = outlets;
    }

    public String getOffVariantId() {
        return offVariantId;
    }

    public String getOnVariantId() {
        return onVariantId;
    }

    public List<Outlet> getOutlets() {
        return outlets;
    }

    public static class Outlet {
        Vector2i position = new Vector2i(0, 0);
        int direction = 0;

        public Outlet() {}

        public Outlet(Vector2i position, int direction) {
            this.position = position;
            this.direction = direction;
        }

        @JsonCreator
        public Outlet(Pair<Vector2i, Integer> def) {
            this(def.getFirst(), def.getLast());
        }

        public Vector2i getPosition() {
            return position;
        }

        public int getDirection() {
            return direction;
        }
    }
}
