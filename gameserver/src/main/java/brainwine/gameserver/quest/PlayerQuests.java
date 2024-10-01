package brainwine.gameserver.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import brainwine.gameserver.entity.Entity;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.server.messages.QuestMessage;
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

        QuestProgress progress = new QuestProgress(quest.getId(), progresses);
        
        player.getQuestProgresses().put(quest.getId(), progress);

        player.notify("Quest has started! Use the /quests command to view your progress at any time.");
        sendPlayerQuestMessage(player, progress);
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

    public static void cancelQuest(Player player, String questId) {
        QuestProgress progress = player.getQuestProgresses().get(questId);

        if (progress == null || progress.isComplete()) return;

        player.getQuestProgresses().remove(questId);
        sendPlayerQuestMessage(player, new QuestProgress(questId, null));
    }

    public static void finishQuest(Player player, Quest quest) {
        QuestProgress progress = player.getQuestProgresses().get(quest.getId());

        if (progress.isComplete()) return;

        for (QuestTask task : quest.getTasks()) {
            if (task.getCollectInventory() != null) {
                task.getCollectInventory().removeFromPlayer(player);
            }
        }

        for (int i = 0; i < quest.getTasks().size(); i++) {
            progress.getTaskProgresses().set(i, quest.getTasks().get(i).getQuantity());
        }

        quest.getReward().reward(player);

        progress.markAsComplete();

        sendPlayerQuestMessage(player, progress);
    }

    public static void sendInitialPlayerQuestMessages(Player player) {
        Map<String, QuestProgress> progresses = player.getQuestProgresses();

        if (progresses == null) return;
        
        for (QuestProgress progress : progresses.values()) {
            sendPlayerQuestMessage(player, progress);
        }
    }

    public static void sendPlayerQuestMessage(Player player, QuestProgress progress) {
        Quest quest = Quests.get(progress.getQuestId());

        if (quest == null) return;

        // TODO: detect mobile player properly
        if (player.isV3()) {
            player.sendMessage(new QuestMessage(quest.getPcDetails(), progress.getClientStatus()));
        } else {
            player.sendMessage(new QuestMessage(quest.getMobileDetails(), progress.getClientStatus()));
        }
        
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

    public static void handleQuestFinalReturn(Player player, Quest quest) {
        QuestProgress progress = player.getQuestProgresses().get(quest.getId());

        if (progress == null) return;

        int found = -1;

        for (int i = 0; i < quest.getTasks().size(); i++) {
            QuestTask task = quest.getTasks().get(i);

            if (task.getEvents() != null) {
                for (List<Object> event : task.getEvents()) {
                    for (Object o : event) {
                        if ("return".equals(o)) found = i;
                        if (found != -1) break;
                    }
                    if (found != -1) break;
                }
            }
            if (found != -1) break;
        }

        if (found == -1) return;

        int initialReturnProgress = progress.getTaskProgress(found);
        int wantedProgress = quest.getTasks().get(found).getQuantity();

        while (progress.getTaskProgresses().size() < quest.getTasks().size()) {
            progress.getTaskProgresses().add(0);
        }

        progress.getTaskProgresses().set(found, wantedProgress);

        // if can't finish even with the return task done, set the return task progress to the previous value
        if (!canFinishQuest(player, quest)) {
            progress.getTaskProgresses().set(found, initialReturnProgress);
        }
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

}
