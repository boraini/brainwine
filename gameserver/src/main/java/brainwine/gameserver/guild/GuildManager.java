package brainwine.gameserver.guild;

import static brainwine.shared.LogMarkers.SERVER_MARKER;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import brainwine.gameserver.player.Player;
import brainwine.shared.JsonHelper;

/**
 * In-memory registry and disk store for {@link Guild}s. Guilds are persisted as
 * {@code guilds/<id>.json}, mirroring {@link brainwine.gameserver.player.PlayerManager}.
 */
public class GuildManager {

    private static final Logger logger = LogManager.getLogger();
    private final Map<String, Guild> guildsById = new HashMap<>();

    public GuildManager() {
        loadGuilds();
    }

    private void loadGuilds() {
        logger.info(SERVER_MARKER, "Loading guild data ...");
        File dataDir = new File("guilds");
        dataDir.mkdirs();

        for(File file : dataDir.listFiles()) {
            if(!file.isDirectory()) {
                loadGuild(file);
            }
        }

        logger.info(SERVER_MARKER, "Successfully loaded {} guild(s)", guildsById.size());
    }

    private void loadGuild(File file) {
        String id = file.getName().replace(".json", "");

        try {
            Guild guild = JsonHelper.readValue(file, Guild.class);
            guildsById.put(guild.getId() == null ? id : guild.getId(), guild);
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not load configuration for guild id {}", id, e);
        }
    }

    public void saveGuilds() {
        for(Guild guild : guildsById.values()) {
            saveGuild(guild);
        }
    }

    public void saveGuild(Guild guild) {
        File file = new File("guilds", guild.getId() + ".json");

        try {
            JsonHelper.writeValue(file, guild);
        } catch(Exception e) {
            logger.error(SERVER_MARKER, "Could not save guild id {}", guild.getId(), e);
        }
    }

    /**
     * Creates a new guild led by the given player, registers and persists it,
     * and links the player to it.
     */
    public Guild createGuild(Player leader, String zoneId, int x, int y) {
        String id = UUID.randomUUID().toString();
        Guild guild = new Guild(id, leader.getDocumentId(), zoneId, x, y);
        guildsById.put(id, guild);
        leader.setGuildId(id);
        saveGuild(guild);
        return guild;
    }

    /**
     * Validates a candidate name/short-name for the given guild: length bounds
     * and uniqueness across all other guilds. Returns an empty list if valid.
     */
    public List<String> validateNames(Guild guild, String name, String shortName) {
        List<String> errors = new ArrayList<>();

        if(name == null || name.length() < 3 || name.length() > 20) {
            errors.add("Guild name must be between 3 and 20 characters.");
        } else {
            Guild other = getGuildByName(name);

            if(other != null && !other.getId().equals(guild.getId())) {
                errors.add(String.format("Guild name '%s' is already taken.", name));
            }
        }

        if(shortName == null || shortName.length() < 2 || shortName.length() > 8) {
            errors.add("Short name must be between 2 and 8 characters.");
        } else {
            Guild other = getGuildByShortName(shortName);

            if(other != null && !other.getId().equals(guild.getId())) {
                errors.add(String.format("Short name '%s' is already taken.", shortName));
            }
        }

        return errors;
    }

    public Guild getGuild(String id) {
        return id == null ? null : guildsById.get(id);
    }

    public Guild getGuildByName(String name) {
        for(Guild guild : guildsById.values()) {
            if(name.equalsIgnoreCase(guild.getName())) {
                return guild;
            }
        }

        return null;
    }

    public Guild getGuildByShortName(String shortName) {
        for(Guild guild : guildsById.values()) {
            if(shortName.equalsIgnoreCase(guild.getShortName())) {
                return guild;
            }
        }

        return null;
    }

    public Collection<Guild> getGuilds() {
        return guildsById.values();
    }
}
