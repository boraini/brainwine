package brainwine.gameserver.anticheat;

import brainwine.gameserver.item.InventoryType;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Exoskeleton {
    @JsonProperty
    public InventoryType inventoryType = InventoryType.ACCESSORY;

    public InventoryType getInventoryType() {
        return inventoryType;
    }
}
