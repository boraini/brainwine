package brainwine.gameserver.quest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.CraftingRequirement;
import brainwine.gameserver.item.LazyItemGetter;
import brainwine.gameserver.player.Player;

public class QuestTaskCollectInventory {
    List<CraftingRequirement> requirements;

    @JsonCreator
    private QuestTaskCollectInventory(Map<String, Integer> inp) {
        requirements = inp.entrySet().stream()
                .map(e -> new CraftingRequirement(new LazyItemGetter(e.getKey()), e.getValue()))
                .collect(Collectors.toList());
    }

    public List<CraftingRequirement> getRequirements() {
        return requirements;
    }

    public boolean playerSatisfies(Player player) {
        if(player == null) {
            return false;
        }

        for(CraftingRequirement req : requirements) {
            if (!player.getInventory().hasItem(req.getItem(), req.getQuantity())) {
                return false;
            }
        }

        return true;

    }

    public void removeFromPlayer(Player player) {
        if(player == null) {
            return;
        }

        for(CraftingRequirement req : requirements) {
            player.getInventory().removeItem(req.getItem(), req.getQuantity());
        }
    }

    public void addDialogListItems(DialogSection section) {
        for (CraftingRequirement req : getRequirements()) {
            section.addItem(new DialogListItem().setItem(req.getItem().getCode()));
        }
    }

}
