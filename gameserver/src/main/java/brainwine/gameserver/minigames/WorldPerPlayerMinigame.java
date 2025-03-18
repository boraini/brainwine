package brainwine.gameserver.minigames;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import brainwine.gameserver.zone.ZoneManager;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class WorldPerPlayerMinigame extends WorldMinigame {
    private static class Teleportation {
        @JsonProperty String zoneId = "";
        @JsonProperty boolean unique = false;
    }

    @JsonProperty List<Teleportation> teleportations = new ArrayList<>();

    @Override
    public boolean useBlock(Player player, int x, int y, MetaBlock metaBlock) {
        ZoneManager zoneManager = GameServer.getInstance().getZoneManager();
        if(metaBlock.getItem() != null && metaBlock.getItem().hasUse(ItemUseType.TARGET_TELEPORT)) {
            Zone targetZone = zoneManager.getZoneByName(MapHelper.getString(metaBlock.getMetadata(), "pz"));
            if(targetZone == null) return true;

            for(Teleportation teleportation : teleportations) {
                if(!teleportation.zoneId.equals(targetZone.getDocumentId())) continue;

                if(teleportation.unique) {
                    targetZone = zoneManager.cloneZone(targetZone);
                    if(targetZone == null) return false;
                    zoneManager.renameZone(targetZone, targetZone.getName() + " copied for " + player.getName());
                }

                player.changeZone(targetZone);
                return false;
            }
        }

        return true;
    }
}
