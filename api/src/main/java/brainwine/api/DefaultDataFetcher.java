package brainwine.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import brainwine.api.models.PlayerInfo;
import brainwine.api.models.PlayerInfoSummary;
import brainwine.api.models.ZoneInfo;

public class DefaultDataFetcher implements DataFetcher {
    
    private static final UnsupportedOperationException exception = new UnsupportedOperationException("DefaultDataFetcher behavior is undefined.");
    
    @Override
    public boolean isPlayerNameTaken(String name) {
        throw exception;
    }
    
    @Override
    public String registerPlayer(String name) {
        throw exception;
    }
    
    @Override
    public String login(String name, String password) {
        throw exception;
    }
    
    @Override
    public String fetchPlayerName(String name) {
        throw exception;
    }
    
    @Override
    public String fetchPlayerId(String apiToken) {
        throw exception;
    }
    
    @Override
    public boolean verifyAuthToken(String name, String token) {
        throw exception;
    }

    @Override
    public PlayerInfo getPlayerInfo(String nameOrId) {
        throw exception;
    }

    @Override
    public Collection<PlayerInfoSummary> fetchPlayerInfo() {
        throw exception;
    }

    @Override
    public ZoneInfo getZoneInfo(String nameOrId) {
        throw exception;
    }

    @Override
    public List<Map<String, Object>> getZoneMetaBlocks(String documentId) {
        throw exception;
    }

    @Override
    public Collection<ZoneInfo> fetchZoneInfo() {
        throw exception;
    }
    
    @Override
    public Collection<ZoneInfo> fetchRecentZoneInfo(String apiToken) {
        throw exception;
    }
    
    @Override
    public Collection<ZoneInfo> fetchBookmarkedZoneInfo(String apiToken) {
        throw exception;
    }
}
