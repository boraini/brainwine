package brainwine.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import brainwine.api.models.PlayerInfo;
import brainwine.api.models.PlayerInfoSummary;
import brainwine.api.models.ZoneInfo;

public interface DataFetcher {
    
    public boolean isPlayerNameTaken(String name);
    public String registerPlayer(String name);
    public String login(String name, String password);
    public String fetchPlayerName(String name);
    public String fetchPlayerId(String apiToken);
    public boolean verifyAuthToken(String name, String token);
    public PlayerInfo getPlayerInfo(String nameOrId);
    public Collection<PlayerInfoSummary> fetchPlayerInfo();
    public ZoneInfo getZoneInfo(String nameOrId);
    public List<Map<String, Object>> getZoneMetaBlocks(String documentId);
    public Collection<ZoneInfo> fetchZoneInfo();
    public Collection<ZoneInfo> fetchRecentZoneInfo(String apiToken);
    public Collection<ZoneInfo> fetchBookmarkedZoneInfo(String apiToken);
}
