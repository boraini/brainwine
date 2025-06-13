package brainwine.gameserver.item.usetypeconfig;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.util.Vector2i;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;

@Properties(allowsDefault = false)
public class ExtendedSteamableConfig extends ItemUseTypeConfig {
    @JsonProperty("off variant")
    String offVariantId = Item.AIR.getId();

    @JsonProperty("on variant")
    String onVariantId = Item.AIR.getId();

    @JsonProperty("inlets")
    List<Vector2i> inlets = Arrays.asList(new Vector2i(-1, 0));

    public String getOffVariantId() {
        return offVariantId;
    }

    public String getOnVariantId() {
        return onVariantId;
    }

    public List<Vector2i> getInlets() {
        return inlets;
    }

    @Override
    public String toString() {
        return "ExtendedSteamableConfig{" + "offVariantId='" + offVariantId + '\'' + ", onVariantId='" + onVariantId + '\'' + ", inlets=" + inlets + '}';
    }
}
