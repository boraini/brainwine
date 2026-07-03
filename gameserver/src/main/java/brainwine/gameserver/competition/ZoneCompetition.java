package brainwine.gameserver.competition;

import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;

import java.util.HashMap;
import java.util.Map;

public class ZoneCompetition {
    public static void clearLandmarkVotes(Zone zone) {
        for(MetaBlock metaBlock : zone.getMetaBlocksWithItem("signs/plaque-landmark")) {
            // Store previous votes in "vx" so that people can't mine XP and achievement progress
            Map<String,Object> newVx = new HashMap<>();
            Object vx = metaBlock.getProperty("vx");
            if(vx instanceof Map<?,?>) {
                newVx.putAll((Map<String,Object>)vx);
            }
            Object vc = metaBlock.getProperty("vc");
            if(vc instanceof Map<?,?>) {
                newVx.putAll((Map<String,Object>)vc);
            }
            // Voters
            metaBlock.removeProperty("v");
            // Vote Count
            metaBlock.removeProperty("vc");
            // Saved Vote Count
            metaBlock.setProperty("vx", newVx);
        }
    }

    public static MetaBlock findProtector(Zone zone, CompetitionEntry entry) {
        return zone.getMetaBlock(entry.getX(), entry.getY());
    }

    public static MetaBlock findLandmark(Zone zone, CompetitionEntry entry) {
        MetaBlock compo = findProtector(zone, entry);
        if(compo == null) return null;
        MetaBlock cached = zone.getMetaBlock(entry.getLandmarkX(), entry.getLandmarkY());
        if(cached != null) return cached;
        int field2 = compo.getItem().getField() * compo.getItem().getField();
        MetaBlock notOwned = null;
        for(MetaBlock mb : zone.getMetaBlocksWithUse(ItemUseType.LANDMARK)) {
            int dx = mb.getX() - compo.getX();
            int dy = mb.getY() - compo.getY();
            if(dx * dx + dy * dy <= field2) {
                entry.setLandmarkX(mb.getX());
                entry.setLandmarkY(mb.getY());
                if(mb.hasOwner() && mb.getOwner().getDocumentId().equals(entry.getPlayerDocumentId())) {
                    return mb;
                }
                notOwned = mb;
            }
        }
        return notOwned;
    }
}
