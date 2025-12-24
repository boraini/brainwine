package brainwine.gameserver.dailyreward;

import brainwine.gameserver.GameServer;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.loot.Loot;
import brainwine.gameserver.loot.LootManager;
import brainwine.gameserver.player.Player;
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
    private Map<Item, quantity> items = new HashMap<>();
    private int quantity = 1;
    private String[] lootCategories = new String[0];

    public void rewardFreely(Player player) {
        for(Map.Entry<Item, Integer> item : items)
            player.getInventory().addItem(item.getKey(), item.getValue(), true);
        if(lootCategories.length > 0) {
            Loot loot = GameServer.getInstance().getLootManager().getRandomLoot(player, lootCategories);
            if (loot != null) {
                player.awardLoot(loot);
            }
        }
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
