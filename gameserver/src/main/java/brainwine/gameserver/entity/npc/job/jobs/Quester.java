package brainwine.gameserver.entity.npc.job.jobs;

import java.util.List;
import java.util.Map;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.job.Job;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.quest.*;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.Pair;

public class Quester extends Job {

    private Pair<String, Integer> getQuestCategoryAndLevel(Npc me) {
        if (me.getName() == null) {
            return null;
        }

        String[] parts = me.getName().split(" ");

        Map<String, String> nameToCategoryTitle = MapHelper.getMap(GameConfiguration.getBaseConfig(), "quests.sources");

        if (parts.length == 1) {
            return new Pair<>(nameToCategoryTitle.get(parts[0]), 1);
        } else {
            return new Pair<>(nameToCategoryTitle.get(parts[0]), MathUtils.clamp(parts[1].length(), 1, 3));
        }

    }

    @Override
    public boolean dialogue(Npc me, Player player) {
        HardcodedQuest hardcodedQuest = Quests.getHardcodedQuests().get(me.getName());
        if (hardcodedQuest != null) {
            return dialogueHardcodedQuest(me, player, hardcodedQuest);
        } else {
            return dialogueOtherQuest(me, player);
        }
    }

    public boolean dialogueHardcodedQuest(Npc me, Player player, HardcodedQuest hardcodedQuest) {
        String questId = hardcodedQuest.getQuestId();

        QuestProgress currentProgress = player.getQuestProgresses().get(questId);

        if (currentProgress == null) {
            dialogueConfirmBeginQuest(me, player, questId);
        } else {
            dialogueCheckProgress(me, player, questId);
        }
        return true;
    }

    public boolean dialogueOtherQuest(Npc me, Player player) {
        Map<String, Object> config = GameConfiguration.getBaseConfig();
        Pair<String, Integer> categoryAndLevel = getQuestCategoryAndLevel(me);

        if (categoryAndLevel != null) {
            String category = categoryAndLevel.getFirst();
            int level = categoryAndLevel.getLast();

            if (category != null) {
                QuestProgress currentProgress = Quests.getQuestProgressInCategory(player, category);

                if (currentProgress == null) {
                    List<Quest> quests = Quests.getRandomQuestsFromCategory(me, category, 5);

                    PlayerQuestDialog.offerQuests(player, quests, System.out::println);
                    return true;
                } else {
                    return dialogueCheckProgress(me, player, currentProgress.getQuestId());
                }
            }
        }

        player.showDialog(DialogHelper.messageDialog("No Quest Offers", MapHelper.getString(config, "dialogs.android.no_quest")));
        return true;
    }

    public boolean dialogueConfirmBeginQuest(Npc me, Player player, String questId) {
        Quest quest = Quests.get(questId);

        if (quest == null) {
            return true;
        }

        PlayerQuestDialog.offerSingleQuest(player, quest, myQuest -> {
            PlayerQuests.getInstance().beginQuest(player, myQuest.getId());
        });

        return true;
    }

    public boolean dialogueCheckProgress(Npc me, Player player, String questId) {
        return true;
    }

}
