package brainwine.gameserver.dailyreward;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.loot.LootManager;
import brainwine.gameserver.player.NotificationType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.quest.QuestEvents;
import brainwine.gameserver.util.Cidr;
import brainwine.gameserver.zone.Biome;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DailyReward {
    private OffsetDateTime beginsAt;
    private OffsetDateTime endsAt;
    private Map<String, Integer> playerExperience = new HashMap<>();
    private Set<String> receivedPlayers = new HashSet<>();
    private Set<Cidr> receivedIpAddresses = new HashSet<>();

    private int requiredExperience = 50;
    private Map<Item, Integer> items = new HashMap<>();
    private int quantity = 1;
    private String[] lootCategories = new String[0];

    public DailyReward() {}

    public DailyReward(OffsetDateTime beginsAt, OffsetDateTime endsAt, int requiredExperience, Map<Item, Integer> items, String[] lootCategories) {
        this.beginsAt = beginsAt;
        this.endsAt = endsAt;
        this.requiredExperience = requiredExperience;
        this.items = items;
        this.lootCategories = lootCategories;
    }

    public void rewardFreely(Player player) {
        for(Map.Entry<Item, Integer> item : items.entrySet()) {
            player.getInventory().addItem(item.getKey(), item.getValue(), true);
            showRewardDialog(player);
        }
        if(lootCategories.length > 0) {
            Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(player, lootCategories);
            if (loot != null) {
                player.awardLoot(loot);
                
            }
        }
    }

    public void showRewardDialog(Player player) {
        Dialog dialog = new Dialog();
        DialogSection section = new DialogSection();
        dialog.addSection(section);
        
        items.forEach((item, quantity) -> {
            if(player.hasClientVersion("3.13.8")) {
                section.addItem(new DialogListItem().setItem(item.getCode()).setText(String.format("%s x %s", item.getFancyTitle(), quantity)).setSupportRichText(true));
            } else {
                section.addItem(new DialogListItem().setItem(item.getCode()).setText(String.format("%s x %s", item.getTitle(), quantity)));
            }
        });

        boolean v3 = player.isV3();
        dialog.setTitle("You have a Reward!");
        player.showDialog(dialog);
    }

    public boolean reward(Player player) {
        if (playerExperience.getOrDefault(player.getDocumentId(), 0) < requiredExperience) {
            return false;
        }
        if (receivedPlayers.contains(player.getDocumentId())
                || player.isOnline() && receivedIpAddresses.contains(player.getConnection().getIpAddress())) {
            return false;
        }
        rewardFreely(player);
        receivedPlayers.add(player.getDocumentId());
        if (player.isOnline()) {
            receivedIpAddresses.add(player.getConnection().getIpAddress());
        }
        return true;
    }

    public void addPlayerExperience(Player player, int amount) {
        playerExperience.put(player.getDocumentId(), playerExperience.getOrDefault(player.getDocumentId(), 0) + amount);
    }

    public OffsetDateTime getBeginsAt() {
        return beginsAt;
    }

    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public Set<String> getReceivedPlayers() {
        return receivedPlayers;
    }

    public Set<Cidr> getReceivedIpAddresses() {
        return receivedIpAddresses;
    }

    public Map<String, Integer> getPlayerExperience() {
        return playerExperience;
    }

    public Map<Item, Integer> getItems() {
        return items;
    }

    public String[] getLootCategories() {
        return lootCategories;
    }

    @JsonIgnore
    public String getTitle() {
        if(items.size() > 0) {
            Map.Entry<Item, Integer> ent = items.entrySet().iterator().next();
            return ent.getValue() + " x " + ent.getKey().getTitle();
        }
        if(lootCategories.length > 0) {
            return lootCategories[0];
        }
        return "This Reward";
    }
}
