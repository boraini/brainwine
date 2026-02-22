package brainwine.api;

import static brainwine.api.util.ContextUtils.error;
import static brainwine.api.util.ContextUtils.handleQueryParam;
import static brainwine.shared.LogMarkers.SERVER_MARKER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import brainwine.api.models.PlayerInfo;
import brainwine.api.models.PlayerInfoQuery;
import brainwine.api.models.PlayerInfoSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import brainwine.api.models.PlayersRequest;
import brainwine.api.models.ServerConnectInfo;
import brainwine.api.models.SessionsRequest;

import brainwine.shared.JsonHelper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJackson;

public class GatewayService {

    private static final int playerSearchPageSize = 50;
    private static final Pattern namePattern = Pattern.compile("^[a-zA-Z0-9_.-]{4,20}$");
    private static final Logger logger = LogManager.getLogger();
    private final Api api;
    private final DataFetcher dataFetcher;
    private final Javalin gateway;
    
    public GatewayService(Api api, int port) {
        this.api = api;
        this.dataFetcher = api.getDataFetcher();
        logger.info(SERVER_MARKER, "Starting GatewayService @ port {} ...", port);
        gateway = Javalin.create(config -> config.jsonMapper(new JavalinJackson(JsonHelper.MAPPER)))
            .exception(Exception.class, this::handleException)
            .get("/clients", this::handleNewsRequest)
            .get("/players", this::handlePlayerSearch)
            .get("/players/{player}", this::handleGetPlayer)
            .post("/players", this::handlePlayerRegistration)
            .post("/sessions", this::handlePlayerLogin)
            .post("/passwords/request", this::handlePasswordForget)
            .post("/passwords/reset", this::handlePasswordReset)
            .post("/purchases", this::handleInAppPurchase)
            .start(port);
    }
    
    /**
     * Exception handler function.
     */
    private void handleException(Exception exception, Context ctx) {
        logger.error(SERVER_MARKER, "Exception caught", exception);
        error(ctx, "%s", exception);
    }
    
    /**
     * Handler function for news requests. (main menu)
     */
    private void handleNewsRequest(Context ctx) {
        Map<String, Object> news = new HashMap<>();
        news.put("posts", api.getNews());
        news.put("beta", api.getBeta());
        ctx.json(news);
    }

    private void handleGetPlayer(Context ctx) {
        String nameOrId = ctx.pathParam("player");
        PlayerInfo info = dataFetcher.getPlayerInfo(nameOrId);
        if(info == null) {
            error(ctx, "Player not found.");
            return;
        }

        handleQueryParam(ctx, "api_token", String.class, token -> {
            if(Objects.equals(info.getApiToken(), token)) {
                info.setTokenValidated();
            }
        });

        ctx.json(info);
    }

    private void handlePlayerSearch(Context ctx) {
        final String keyRegex = "^[a-zA-Z0-9_]+$";

        PlayerInfoQuery query = new PlayerInfoQuery();
        for(String paramName : ctx.queryParamMap().keySet()) {
            if(paramName.startsWith("order-")) {
                try {
                    String orderKey = paramName.substring("order-".length());
                    if(!orderKey.isEmpty()) {
                        query.setOrderLevel(orderKey, Integer.parseInt(Objects.requireNonNull(ctx.queryParam(paramName))));
                    }
                } catch(NullPointerException | NumberFormatException e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("paramName", "Must be a number!");
                    ctx.status(422).json(result);
                    return;
                }
            }
        }

        handleQueryParam(ctx, "includeBanned", Boolean.class, value -> {
            query.setIncludeBanned(value);
        });

        handleQueryParam(ctx, "statistics", String.class, csv -> {
            for(String key : csv.split(",")) {
                // Statistics are always alphanumeric, with underscores in between
                if(key.matches(keyRegex)) {
                    query.addStatistic(key);
                }
            }
        });

        // Workaround for if the sorting key will be missing from the queried data
        handleQueryParam(ctx, "sort", String.class, sort -> {
            if(sort.startsWith("statistics.")) {
                String key = sort.substring(sort.indexOf(".") + 1);
                // Statistics are always alphanumeric, with underscores in between
                if(key.matches(keyRegex)) {
                    query.addStatistic(key);
                }
            }
        });

        List<PlayerInfoSummary> players = (List<PlayerInfoSummary>)dataFetcher.fetchPlayerInfo(query);

        handleQueryParam(ctx, "name", String.class, name -> {
            players.removeIf(player -> !player.getName().toLowerCase().contains(name.toLowerCase()));
        });

        handleQueryParam(ctx, "min_level", Integer.class, minLevel -> {
            players.removeIf(player -> player.getLevel() < minLevel);
        });

        handleQueryParam(ctx, "max_level", Integer.class, maxLevel -> {
            players.removeIf(player -> player.getLevel() > maxLevel);
        });

        handleQueryParam(ctx, "admin", Boolean.class, admin -> {
            players.removeIf(player -> player.isAdmin() != admin);
        });

        handleQueryParam(ctx, "sort", String.class, sort -> {
            if(sort.startsWith("statistics.")) {
                String key = sort.substring(sort.indexOf(".") + 1);
                // Statistics are always alphanumeric, with underscores in between
                if(key.matches("^[a-zA-Z0-9_]+$")) {
                    players.sort((a, b) -> {
                        Object valA = a.getStatistics().get(key);
                        Object valB = b.getStatistics().get(key);
                        if(valA instanceof Integer) {
                            if(valB instanceof Integer) {
                                return Integer.compare((Integer)valB, (Integer)valA);
                            }
                            return -1;
                        } else {
                            return valB instanceof Integer ? 1 : a.getName().compareTo(b.getName());
                        }
                    });
                }
                return;
            }

            switch(sort) {
                case "items_mined": // Sort by total items mined
                    players.sort((a, b) -> Integer.compare(b.getItemsMined(), a.getItemsMined()));
                    break;
                case "items_scavenged": // Sort by total items scavenged
                    players.sort((a, b) -> Integer.compare(b.getItemsScavenged(), a.getItemsMined()));
                    break;
                case "items_placed": // Sort by total items placed
                    players.sort((a, b) -> Integer.compare(b.getItemsPlaced(), a.getItemsPlaced()));
                    break;
                case "items_crafted": // Sort by total items crafted
                    players.sort((a, b) -> Integer.compare(b.getItemsCrafted(), a.getItemsCrafted()));
                    break;
                case "level":
                default:
                    players.sort((a, b) -> Integer.compare(b.getLevel(), a.getLevel()));
            }
        });

        // Page
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int fromIndex = (page - 1) * playerSearchPageSize;
        int toIndex = page * playerSearchPageSize;
        ctx.json(players.subList(fromIndex < 0 ? 0 : fromIndex > players.size() ? players.size() : fromIndex, toIndex > players.size() ? players.size() : toIndex));
    }
    
    /**
     * Handler function for registering a new account.
     */
    private void handlePlayerRegistration(Context ctx) {
        PlayersRequest request = ctx.bodyValidator(PlayersRequest.class).get();
        String name = request.getName();
        
        // Check if name is too short, too long or contains illegal characters
        if(!namePattern.matcher(name).matches()) {
            error(ctx, "Please enter a valid username.");
            return;
        }
        
        // Check if a player with this name already exists
        if(dataFetcher.isPlayerNameTaken(name)) {
            error(ctx, "Sorry, this username has already been taken.");
            return;
        }
        
        String token = dataFetcher.registerPlayer(name);
        ctx.json(new ServerConnectInfo(api.getGameServerHost(), name, token));
    }
    
    /**
     * Handler function for logging into an existing account with username & password/auth token.
     * If the user logs in using a password, a new auth token is generated.
     */
    private void handlePlayerLogin(Context ctx) {
        SessionsRequest request = ctx.bodyValidator(SessionsRequest.class).get();
        String name = request.getName();
        String password = request.getPassword();
        String token = request.getToken();
        
        // If a password is present, try to log in and generate an auth token.
        // Null auth token = incorrect username/password combination.
        // Otherwise, if an auth token is present, try to verify that instead.
        if(password != null) {
            token = dataFetcher.login(name, password);
            
            if(token == null) {
                error(ctx, "Username or password is incorrect. Please check your credentials.");
                return;
            }
        } else if(token != null) {
            if(!dataFetcher.verifyAuthToken(name, token)) {
                error(ctx, "The provided session token is invalid or has expired. Please try relogging.");
                return;
            }
        } else {
            error(ctx, "No credentials provided.");
            return;
        }
        
        ctx.json(new ServerConnectInfo(api.getGameServerHost(), dataFetcher.fetchPlayerName(name), token));
    }
    
    /**
     * Handler function for initiating password resets.
     * TODO wip
     */
    private void handlePasswordForget(Context ctx) {
        error(ctx, "Sorry, it is currently not possible to reset your password.");
    }
    
    /**
     * Handler function for processing password resets.
     * TODO wip
     */
    private void handlePasswordReset(Context ctx) {
        error(ctx, "Sorry, it is currently not possible to reset your password.");
    }
    
    /**
     * Handler function for in-app purchases.
     * Permanently doomed to err, as it will never be implemented.
     */
    private void handleInAppPurchase(Context ctx) {
        error(ctx, "Sorry, in-app purchases are not supported.");
    }
    
    /**
     * Stops the gateway service.
     * @see Javalin#stop()
     */
    public void stop() {
        gateway.stop();
    }
}
