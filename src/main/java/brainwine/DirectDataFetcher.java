package brainwine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import brainwine.api.DataFetcher;
import brainwine.api.models.PlayerInfo;
import brainwine.api.models.PlayerInfoQuery;
import brainwine.api.models.PlayerInfoSummary;
import brainwine.api.models.ZoneInfo;
import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemGroup;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.ItemUseType;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import brainwine.gameserver.zone.ZoneActivity;
import brainwine.gameserver.zone.ZoneManager;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class DirectDataFetcher implements DataFetcher {
    
    private final PlayerManager playerManager;
    private final ZoneManager zoneManager;

    private static final Set<String> includedStats = Stream.of(
            "discoveries", "kills", "assists", "play_time", "areas_explored",
            "containers_looted", "dungeons_raided", "maws_plugged", "undertakings", "deliverances", "deaths",
            "players_killed", "landmarks_upvoted", "landmark_votes_received", "evokers_inhibited",
            "shillings_spent_in_android_shop", "shillings_received_in_android_shop",
            "shillings_spent_in_scrap_market", "shillings_received_in_scrap_market",
            "android_shop_purchases", "android_shop_sales", "scrap_market_purchases", "scrap_market_sales"
            ).collect(Collectors.toSet());
    
    public DirectDataFetcher(PlayerManager playerManager, ZoneManager zoneManager) {
        this.playerManager = playerManager;
        this.zoneManager = zoneManager;
    }
    
    @Override
    public boolean isPlayerNameTaken(String name) {
        return playerManager.getPlayer(name) != null;
    }

    @Override
    public String registerPlayer(String name) {
        return playerManager.register(name);
    }

    @Override
    public String login(String name, String password) {
        return playerManager.login(name, password);
    }
    
    @Override
    public String fetchPlayerName(String name) {
        Player player = playerManager.getPlayer(name);
        return player == null ? null : player.getName();
    }
    
    @Override
    public String fetchPlayerId(String apiToken) {
        Player player = playerManager.getPlayerByApiToken(apiToken);
        return player == null ? null : player.getDocumentId();
    }

    @Override
    public boolean verifyAuthToken(String name, String token) {
        return playerManager.verifyAuthToken(name, token);
    }

    @Override
    public PlayerInfo getPlayerInfo(String nameOrId) {
        Player player = playerManager.getPlayer(nameOrId);

        if(player == null) {
            player = playerManager.getPlayerById(nameOrId);
        }

        return player == null ? null : createPlayerInfo(player);
    }

    @Override
    public Collection<PlayerInfoSummary> fetchPlayerInfo(PlayerInfoQuery query) {
        return playerManager.getPlayers().stream()
                .filter(Objects::nonNull)
                .filter(player -> query.isIncludeBanned() || player.getCurrentBan() == null)
                .filter(player -> query.getOrderLevel().keySet().stream().allMatch(
                        orderKey -> Objects.equals(player.getOrders().getOrDefault(orderKey, 0), query.getOrderLevel().get(orderKey))
                ))
                .map(player -> DirectDataFetcher.createPlayerInfoSummary(player, query))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static PlayerInfoSummary createPlayerInfoSummary(Player player, PlayerInfoQuery query) {
        PlayerInfoSummary info = new PlayerInfoSummary(
                player.getName(),
                player.getIcon(),
                player.getLevel(),
                player.getLevelFromExperience(player.getExperience()),
                player.isAdmin(),
                player.getStatistics().getDeaths(),
                player.getStatistics().getTotalItemsMined(),
                player.getStatistics().getTotalItemsScavenged(),
                player.getStatistics().getItemsPlaced(),
                player.getStatistics().getTotalItemsCrafted()
        );

        Map<String, Object> selectedStats = info.getStatistics();

        for(String statistic : query.getStatistics()) {
            // Derived statistics
            switch(statistic) {
                case "shillings_spent":
                    selectedStats.put("shillings_spent", player.getStatistics().getShillingsSpentInAndroidShop() + player.getStatistics().getShillingsSpentInScrapMarket());
                    break;
                case "shillings_received":
                    selectedStats.put("shillings_received", player.getStatistics().getShillingsReceivedInAndroidShop() + player.getStatistics().getShillingsReceivedInScrapMarket());
                    break;
            }
        }

        if(query.getStatistics().stream().anyMatch(includedStats::contains)) {
            try {
                Map<String, Object> stats = JsonHelper.readValue(player.getStatistics(), new TypeReference<Map<String, Object>>() {});

                for(String statistic : query.getStatistics()) {
                    if(includedStats.contains(statistic)) {
                        Object value = stats.get(statistic);
                        if(value != null) {
                            selectedStats.put(statistic, value);
                        }
                    }
                }
            } catch(JsonProcessingException ignored) {}
        }

        return info;
    }

    private static PlayerInfo createPlayerInfo(Player player) {
        Map<String, String> appearance = new HashMap<>();
        for(Map.Entry<String, Object> entry : player.getCustomizedAppearance().entrySet()) {
            if(entry.getKey() == null || entry.getValue() == null) continue;
            if(entry.getKey().contains("*")) {
                appearance.put(entry.getKey(), Objects.toString(entry.getValue()));
            } else {
                if(entry.getValue() instanceof Integer) {
                    appearance.put(entry.getKey(), ItemRegistry.getItem((int) entry.getValue()).getId());
                }
            }
        }

        // TODO: include hover and propel accessories.
        Item flyAccessory = player.getInventory().findAccessoryWithUse(ItemUseType.FLY);
        if(!flyAccessory.isAir()) {
            appearance.put("u", flyAccessory.getId());
        }

        // TODO: this serializes the items mined and scavenged for no reason.
        Map<String, Object> stats = new HashMap<>();
        try {
            Map<String, Object> allStats = JsonHelper.readValue(player.getStatistics(), new TypeReference<Map<String, Object>>() {});
            for(String key : includedStats) {
                stats.put(key, allStats.get(key));
            }

            int treesMined = 0;
            int mineralsMined = 0;
            for(Map.Entry<Item, Integer> entry : player.getStatistics().getItemsScavenged().entrySet()) {
                if(entry.getKey().getGroup() == ItemGroup.TREE) treesMined += entry.getValue();
                if(entry.getKey().getGroup() == ItemGroup.MINERAL) mineralsMined += entry.getValue();
            }

            stats.put("trees_mined", treesMined);
            stats.put("minerals_mined", mineralsMined);
        } catch(JsonProcessingException e) {
            stats = null;
        }

        return new PlayerInfo(
                player.getName(),
                player.getIcon(),
                player.getLevel(),
                player.getSkills().values().stream().collect(Collectors.summingInt(x -> x - 1)),
                player.isAdmin(),
                player.getStatistics().getDeaths(),
                player.getStatistics().getTotalItemsMined(),
                player.getStatistics().getTotalItemsScavenged(),
                player.getStatistics().getItemsPlaced(),
                player.getStatistics().getTotalItemsCrafted(),
                player.getApiToken(),
                player.getOrders(),
                appearance,
                stats
        );
    }

    @Override
    public ZoneInfo getZoneInfo(String nameOrId) {
        Zone zone = zoneManager.getZoneByName(nameOrId);
        
        if(zone == null) {
            zone = zoneManager.getZone(nameOrId);
        }
        
        return zone == null ? null : createZoneInfo(zone);
    }
    
    /**
     * TODO this will probably be slow if there is a large number of zones
     */
    @Override
    public Collection<ZoneInfo> fetchZoneInfo() {
        List<ZoneInfo> zoneInfo = new ArrayList<>();
        Collection<Zone> zones = zoneManager.getZones();
        
        for(Zone zone : zones) {
            zoneInfo.add(createZoneInfo(zone));
        }
        
        return zoneInfo;
    }

    @Override
    public Collection<ZoneInfo> fetchRecentZoneInfo(String apiToken) {
        Player player = playerManager.getPlayerByApiToken(apiToken);
        return player == null ? new ArrayList<>() : createZoneInfo(player.getRecentZones());
    }
    
    @Override
    public Collection<ZoneInfo> fetchBookmarkedZoneInfo(String apiToken) {
        Player player = playerManager.getPlayerByApiToken(apiToken);
        return player == null ? new ArrayList<>() : createZoneInfo(player.getBookmarkedZones());
    }
    
    private List<ZoneInfo> createZoneInfo(Collection<String> zoneIds) {
        return zoneIds.stream().map(zoneManager::getZone)
                .filter(Objects::nonNull)
                .map(DirectDataFetcher::createZoneInfo)
                .collect(Collectors.toCollection(ArrayList::new));
    }
    
    public static ZoneInfo createZoneInfo(Zone zone) {
        return new ZoneInfo(zone.getDocumentId(),
                zone.getName(),
                zone.getBiome().getId(),
                zone.getActivity() == null || zone.getActivity() == ZoneActivity.NONE ? null : zone.getActivity().toString().toLowerCase(),
                zone.isPvp(),
                zone.isMarket(),
                zone.isTutorial(),
                false,
                zone.isPrivate(),
                zone.isProtected(),
                zone.getPlayerCount(),
                zone.getWidth(),
                zone.getHeight(),
                zone.getSurface(),
                zone.getExplorationProgress(),
                zone.getCreationDate(),
                zone.getOwner(),
                zone.getMembers(),
                null
        );
    }

    public List<Map<String, Object>> getZoneMetaBlocks(String documentId) {
        Zone zone = zoneManager.getZone(documentId);
        if(zone == null) return null;
        return zone.getGlobalMetaBlocks().stream()
                .filter(b -> b.getItem().hasUse(ItemUseType.ZONE_TELEPORT)
                        || b.getItem().hasUse(ItemUseType.TELEPORT)
                        || b.getItem().getId().contains("sign")
                )
                .map(DirectDataFetcher::createMetaBlockData)
                .collect(Collectors.toList());
    }

    private static final Set<String> includedMetablockKeys = Stream.of( "n", "t1", "t2", "t3", "vc" ).collect(Collectors.toCollection(HashSet::new));
    private static Map<String, Object> createMetaBlockData(MetaBlock m) {
        Map<String, Object> metadata = new HashMap<>();
        for(Map.Entry<String, Object> entry : m.getMetadata().entrySet()) {
            if(includedMetablockKeys.contains(entry.getKey())) {
                metadata.put(entry.getKey(), entry.getValue());
            }
        }

        Map<String, Object> data = MapHelper.map(
                String.class, Object.class,
                "x", m.getX(),
                "y", m.getY(),
                "item", m.getItem().getId(),
                "metadata", metadata
        );

        Player owner = m.getOwner();
        if(owner != null) {
            data.put("owner", MapHelper.map(
                    String.class, Object.class,
                    "name", owner.getName()
            ));
        }

        return data;
    }
}
