package brainwine.gameserver.server;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.pipeline.Connection;
import brainwine.gameserver.util.Cidr;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IpBans {
    private static final Logger logger = LogManager.getLogger();
    private final String FILE_NAME = "banned-ips.json";

    List<Item> bannedIps = new ArrayList<>();
    Map<Cidr, Item> bansByIp = new HashMap<>();

    public void loadIpBans() {
        logger.info("Loading ip bans ...");
        try {
            bannedIps = JsonHelper.readValue(new File(FILE_NAME), new TypeReference<List<Item>>() {});
            for(Item item : bannedIps) {
                bansByIp.put(item.getIpAddress(), item);
            }
        } catch(IOException e) {
            logger.error("Failed to read " + FILE_NAME, e);
        }
    }

    public void saveIpBans() {
        try {
            JsonHelper.writeValue(new File(FILE_NAME), bannedIps);
        } catch(IOException e) {
            logger.error("Failed to write " + FILE_NAME, e);
        }
    }

    public Item getIpBanItem(Cidr playerIp) {
        for(Item bannedItem : bannedIps) {
            if(playerIp.matches(bannedItem.getIpAddress())) {
                return bannedItem;
            }
        }
        return null;
    }

    public boolean isCidrBanned(Cidr bannedIp) {
        return bansByIp.containsKey(bannedIp);
    }

    public void banCidr(Cidr bannedIp, Player scapegoat) {
        Item item = bansByIp.get(bannedIp);
        if(item == null) {
            item = new Item(bannedIp);
            bannedIps.add(item);
            bansByIp.put(bannedIp, item);
        }
        if(scapegoat != null) {
            item.getKnownUuids().add(scapegoat.getDocumentId());
            item.getKnownUsernames().add(scapegoat.getName());
        }
    }

    public void unbanCidr(Cidr bannedIp) {
        Item item = bansByIp.remove(bannedIp);
        bannedIps.remove(item);
    }

    public void ban(Player player) {
        if(!player.isOnline()) return;

        Cidr playerIp = player.getConnection().getIpAddress();
        Item banByIpAddress = bansByIp.get(playerIp);

        if(banByIpAddress == null) {
            banByIpAddress = new Item(playerIp);
            bannedIps.add(banByIpAddress);
            bansByIp.put(playerIp, banByIpAddress);
        }

        banByIpAddress.getKnownUuids().add(player.getDocumentId());
        banByIpAddress.getKnownUsernames().add(player.getName());
    }

    public Set<String> unbanAndCheckForCidrBlock(Player player) {
        return player.isOnline() ? unbanAndCheckForCidrBlock(player, player.getConnection()) : new HashSet<>();
    }

    public Set<String> unbanAndCheckForCidrBlock(Player player, Connection connection) {
        Set<String> ipBlocks = new HashSet<>();
        List<Item> deletedItems = new ArrayList<>();
        Cidr playerCurrentIp = connection.getIpAddress();
        for(Item item : bannedIps) {
            Cidr firstIp = item.getIpAddress();
            if(item.getIpAddress().equals(playerCurrentIp)
                    || item.getKnownUuids().contains(player.getDocumentId()) && firstIp != null
            ) {
                if(item.getKnownUuids().size() > 1) {
                    ipBlocks.add((playerCurrentIp == null ? firstIp : playerCurrentIp).toString() + " (multiple players)");
                    continue;
                }
                item.getKnownUuids().remove(player.getDocumentId());
                item.getKnownUsernames().remove(player.getName());
                if(item.getIpAddress().equals(playerCurrentIp)) {
                    bansByIp.remove(playerCurrentIp);
                }
                if(item.getIpAddress().equals(firstIp)) {
                    bansByIp.remove(playerCurrentIp);
                }
                deletedItems.add(item);
            } else {
                if(item.getIpAddress().matches(playerCurrentIp)) {
                    ipBlocks.add(item.getIpAddress().toString());
                }
            }
        }
        bannedIps.removeAll(deletedItems);

        return ipBlocks;
    }

    public static class Item {
        private final Cidr ipAddress;
        @JsonProperty
        private final Set<String> knownUuids = new HashSet<>();
        @JsonProperty
        private final Set<String> knownUsernames = new HashSet<>();

        @JsonCreator
        public Item(@JsonProperty("ip_address") Cidr ipAddress) {
            this.ipAddress = ipAddress;
        }

        public Set<String> getKnownUuids() {
            return knownUuids;
        }

        public Set<String> getKnownUsernames() {
            return knownUsernames;
        }

        public Cidr getIpAddress() {
            return ipAddress;
        }
    }
}
