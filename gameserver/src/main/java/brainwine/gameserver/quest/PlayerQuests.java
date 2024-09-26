package brainwine.gameserver.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.zone.Zone;

public class PlayerQuests {
    private PlayerQuests() {}

    public static void beginQuest(Player player, Quest quest) {
        if (player == null) return;
        
        if (quest == null) {
            player.notify("Quest not found!");
            return;
        }

        List<Integer> progresses = new ArrayList<>(quest.getTasks().size());

        for(int i = 0; i < quest.getTasks().size(); i++) {
            progresses.add(0);
        }
        
        player.getQuestProgresses().put(quest.getId(), new QuestProgress(quest.getId(), progresses));

        player.notify("Quest has started! Use the /quests command to view your progress at any time.");
    }

    public static boolean isTaskComplete(Quest quest, Player player, int i) {
        int progress = player.getQuestProgresses().get(quest.getId()).getTaskProgress(i);

        QuestTask task = quest.getTasks().get(i);

        boolean satisfiesQuantity = task.getQuantity() <= progress;
        boolean satisfiesInventory = task.getCollectInventory() == null || task.getCollectInventory().playerSatisfies(player);

        return satisfiesQuantity && satisfiesInventory;
    }

    public static boolean canFinishQuest(Player player, Quest quest) {
        if (player == null) return false;
        
        if (quest == null) {
            player.notify("Quest not found!");
            return false;
        }

        QuestProgress progress = player.getQuestProgresses().get(quest.getId());

        if (progress == null) {
            player.notify("Your quest progress is not found!");
            return false;
        }

        for(int i = 0; i < quest.getTasks().size(); i++) {
            if (!isTaskComplete(quest, player, i)) {
                return false;
            }
        }

        return true;
    }

    public static void finishQuest(Player player, Quest quest) {
        for (QuestTask task : quest.getTasks()) {
            if (task.getCollectInventory() != null) {
                task.getCollectInventory().removeFromPlayer(player);
            }
        }

        quest.getReward().reward(player);
    }

    public static boolean patternMatch(List<Object> event, Object[] pattern) {
        if (event.size() > pattern.length) return false;
        int iterationCount = Math.min(event.size(), pattern.length);
        for (int i = 0; i < iterationCount; i++) {
            if (pattern[i] == null) continue;
            if (event.get(i) == null) continue;

            if (!Objects.equals(pattern[i], event.get(i))) return false;
        }

        return true;
    }

    public static void handleEvent(Player player, Object... pattern) {
        for (Map.Entry<String, QuestProgress> questProgressEntry : player.getQuestProgresses().entrySet()) {
            String questId = questProgressEntry.getKey();
            QuestProgress questProgress = questProgressEntry.getValue();
            Quest quest = Quests.get(questId);
            int i = 0;
            for (QuestTask task : quest.getTasks()) {
                if (task.getEvents() == null) continue;

                if (!task.doesQualify(player)) {
                    continue;
                }

                for (List<Object> event : task.getEvents()) {
                    if (patternMatch(event, pattern)) {
                        questProgress.getTaskProgresses().set(i, questProgress.getTaskProgress(i) + 1);
                        break;
                    }
                }
                i++;
            }
        }
    }

    public static void handleEnterZone(Player player, Zone zone) {
        handleEvent(player, "entered", "zone_name", zone.getName());
    }

    public static void handleKill(Player player, Entity other) {
        handleEvent(player, "kill", "code", other.getType());

        if(other.isPlayer()) {
            // don't reward players for killing each other
        } else {
            Npc npc = (Npc) other;
            handleEvent(player, "kill", "category", npc.getConfig().getEntityClass());
        }
        
    }

    public static void handleChat(Player player) {
        handleEvent(player, "chat");
    }

    public static void handleReturn(Player player) {
        handleEvent(player, "return");
    }

}
