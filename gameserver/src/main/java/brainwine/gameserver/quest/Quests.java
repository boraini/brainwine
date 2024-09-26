package brainwine.gameserver.quest;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.resource.ResourceFinder;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.util.PickRandom;
import brainwine.shared.JsonHelper;

public class Quests {
    public static Map<String, Map<String, Quest>> questMaps = new HashMap<>();
    public static Map<String, List<Quest>> questLists = new HashMap<>();
    public static Map<String, String> titleToPrefix = new HashMap<>();
    public static Map<String, HardcodedQuest> hardcodedQuests = new HashMap<>();

    private static final Logger logger = LogManager.getLogger();

    private Quests() {}

    private static void loadHardcodedQuests() {
        try {
            URL url = ResourceFinder.getResourceUrl("hardcoded-quests.json");
            Map<String, Map<String, HardcodedQuest>> obj = JsonHelper.readValue(url,  new TypeReference<Map<String, Map<String, HardcodedQuest>>>(){});
            hardcodedQuests.putAll(MapHelper.getMap(obj, "hardcoded_quests"));
        } catch (Exception e) {
            logger.warn(String.format("Could not load any hardcoded quest entries from hardcoded-quests.json: %s", e.getMessage()));
        }
    }

    private static int loadQuestsForCategory(String categoryPrefix, String categoryTitle) {
        titleToPrefix.put(categoryTitle, categoryPrefix);

        Map<String, Object> config = GameConfiguration.getBaseConfig();
        Map<String, Quest> resultMap = new HashMap<>();
        List<Quest> resultList = new ArrayList<>();

        int counter = 0;
        for (String questId : config.keySet()) {
            try {
            if (questId.startsWith(categoryPrefix)) {
                Quest quest = JsonHelper.readValue(MapHelper.getMap(config, questId), Quest.class);
                quest.setId(questId);
                resultMap.put(questId, quest);
                resultList.add(quest);
                counter++;
            }
            } catch (Exception e) {
                logger.warn(String.format("Could not load quest %s: %s", questId, e.getMessage()));
            }
        }

        questMaps.put(categoryTitle, resultMap);
        questLists.put(categoryTitle, resultList);

        return counter;
    }

    public static void loadQuests() {
        loadHardcodedQuests();

        int counter = 0;

        counter += loadQuestsForCategory("collect", "Arts and Crafts");
        counter += loadQuestsForCategory("combat", "The Art of War");
        counter += loadQuestsForCategory("cooking", "Let Them Eat Cake");
        counter += loadQuestsForCategory("survival", "Survive and Thrive");

        logger.info("Successfully loaded {} quests", counter);
    }

    public static Quest get(String questId) {
        Map<String, Quest> targetMap = null;

        for (String key : titleToPrefix.keySet()) {
            if (questId.startsWith(titleToPrefix.get(key))) targetMap = questMaps.get(key);
        }

        if (targetMap == null) {
            return null;
        }

        Quest quest = targetMap.get(questId);

        quest.setId(questId);

        return quest;
    }

    public static List<Quest> getRandomQuestsFromCategory(Entity me, String categoryTitle, int count) {
        logger.info(categoryTitle);
        List<Quest> targetList = questLists.get(categoryTitle);

        if (targetList == null) {
            return null;
        }

        Random random = new Random(me == null ? 123456789L : me.hashCode());

        return PickRandom.sampleWithoutReplacement(random, targetList, count);
    }

    public static QuestProgress getQuestProgressInCategory(Player player, String categoryTitle) {
        String prefix = titleToPrefix.get(categoryTitle);

        if (prefix == null) {
            return null;
        }

        if (player.getQuestProgresses() == null) {
            return null;
        }

        String id = player.getQuestProgresses().keySet().stream().filter(p -> p.startsWith(prefix)).findFirst().orElse(null);

        return player.getQuestProgresses().get(id);
    }

    /**
     * Populate hardcoded-quests.json with Android names for which a specific quest will
     * be issued to the player instead of showing the quest offers menu.
     */
    public static Map<String, HardcodedQuest> getHardcodedQuests() {
        return hardcodedQuests;
    }

}
