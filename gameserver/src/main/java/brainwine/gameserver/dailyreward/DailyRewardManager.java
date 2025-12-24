package brainwine.gameserver.dailyreward;

import brainwine.shared.JsonHelper;
import brainwine.gameserver.GameServer;
import brainwine.gameserver.dailyreward.DailyReward;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.zone.Zone;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static brainwine.shared.LogMarkers.SERVER_MARKER;


public class DailyRewardManager {
    private static final Logger logger = LogManager.getLogger();

    List<DailyReward> rewards = new ArrayList<>();
    List<DailyReward> activeRewards = new ArrayList<>();

    private static final String FILE_NAME = "daily-rewards.json";
    
    public DailyRewardManager() {
        logger.info("Loading daily rewards...");

        try {
            rewards.addAll(JsonHelper.readValue(new File(FILE_NAME), new TypeReference<List<DailyReward>>() {}));
            for(DailyReward reward : rewards) {
                OffsetDateTime now = OffsetDateTime.now();
                if(now.isBefore(reward.getEndsAt())) {
                    activeRewards.add(reward);
                }
            }
        } catch (Exception e) {
            logger.error(SERVER_MARKER, "Could not load daily rewards.", e);
            return;
        }

        logger.info("Successfully loaded {} rewards.", rewards);
    }

    public void saveDailyRewards() {
        try {
            JsonHelper.writeValue(new File(FILE_NAME), products);
        } catch(Exception e) {
            logger.error("Failed to write {}", FILE_NAME, e);
        }
    }

    public void rewardPlayers(Zone zone) {
        // Maybe not scan for expired daily rewards each timee
        boolean needsFiltering = false;

        final OffsetDateTime now = OffsetDateTime.now();
        Collection<Player> players = zone.getPlayers();
        for(int i = 0; i < activeRewards.length; i++) {
            if(now.isBefore(reward.getBeginsAt())) {
                continue;
            }

            // Reward should still be given if the player has reached the XP goal between ticks
            if(now.isAfter(reward.getEndsAt())) {
                needsFiltering = true;
            }

            for(Player player : players) {
                reward.reward(player);
            }
        }

        if(needsFiltering) {
            activeRewards.removeIf(reward -> now.isAfter(reward.getEndsAt()));
        }
    }

    public void addPlayerExperience(Player player, int amount) {
        OffsetDateTime now = OffsetDateTime.now();
        for(DailyReward reward : activeRewards) {
            if(now.isAfter(reward.getBeginsAt())) {
                reward.addPlayerExperience(player, amount);
            }
        }
    }

    public void addReward(DailyReward reward) {
        rewards.add(reward);
        activeRewards.add(reward);
    }

    public void removeReward(DailyReward reward) {
        rewards.remove(reward);
        activeRewards.remove(reward);
    }

    public List<DailyReward> getRewards() {
        return Collections.unmodifiableList(rewards);
    }

    public List<DailyReward> getActiveRewards() {
        return Collections.unmodifiableList(activeRewards);
    }
    
}
