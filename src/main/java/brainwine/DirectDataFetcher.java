package brainwine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import brainwine.api.DataFetcher;
import brainwine.api.models.PlayerInfo;
import brainwine.api.models.PlayerInfoSummary;
import brainwine.api.models.ZoneInfo;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.player.PlayerManager;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.zone.MetaBlock;
import brainwine.gameserver.zone.Zone;
import brainwine.gameserver.zone.ZoneManager;
import brainwine.shared.JsonHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class DirectDataFetcher implements DataFetcher {
    
    private final PlayerManager playerManager;
    private final ZoneManager zoneManager;
    
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
    public Collection<PlayerInfoSummary> fetchPlayerInfo() {
        return playerManager.getPlayers().stream()
                .filter(Objects::nonNull)
                .map(DirectDataFetcher::createPlayerInfoSummary)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static PlayerInfoSummary createPlayerInfoSummary(Player player) {
        return new PlayerInfoSummary(
                player.getName(),
                player.getLevel(),
                player.getLevelFromExperience(player.getExperience()),
                player.getStatistics().getDeaths(),
                player.getStatistics().getTotalItemsMined(),
                player.getStatistics().getTotalItemsScavenged(),
                player.getStatistics().getItemsPlaced(),
                player.getStatistics().getTotalItemsCrafted()
        );
    }

    private static PlayerInfo createPlayerInfo(Player player) {
        Map<String, Object> stats;
        try {
            Map<String, Object> all = JsonHelper.readValue(player.getStatistics(), new TypeReference<Map<String, Object>>() {});
            stats = MapHelper.map(
                    String.class, Object.class,
                    "items_mined", all.get("items_mined"),
                    "items_scavenged", all.get("items_scavenged"),
                    "items_crafted", all.get("items_crafted")
            );
        } catch(JsonProcessingException e) {
            stats = new HashMap<>();
        }
        return new PlayerInfo(
                player.getName(),
                player.getLevel(),
                player.getSkills().values().stream().collect(Collectors.summingInt(x -> (Integer)x - 1)),
                player.getStatistics().getDeaths(),
                player.getStatistics().getTotalItemsMined(),
                player.getStatistics().getTotalItemsScavenged(),
                player.getStatistics().getItemsPlaced(),
                player.getStatistics().getTotalItemsCrafted(),
                player.getApiToken(),
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
    
    private static ZoneInfo createZoneInfo(Zone zone) {
        return new ZoneInfo(zone.getName(), 
                zone.getBiome().getId(), 
                null,
                zone.isPvp(),
                false,
                zone.isPrivate(),
                zone.isProtected(),
                zone.getPlayers().size(),
                zone.getWidth(),
                zone.getHeight(),
                zone.getSurface(),
                zone.getExplorationProgress(),
                zone.getCreationDate(),
                zone.getOwner(),
                zone.getMembers(),
                zone.getGlobalMetaBlocks().stream().map(DirectDataFetcher::createMetaBlockData).collect(Collectors.toList()));
    }

    private static Map<String, Object> createMetaBlockData(MetaBlock m) {
        Map<String, Object> data = MapHelper.map(
                String.class, Object.class,
                "x", m.getX(),
                "y", m.getY(),
                "item", m.getItem().getId(),
                "metadata", m.getMetadata()
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
