package brainwine.gameserver.entity.npc.job.jobs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import brainwine.gameserver.GameConfiguration;
import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogHelper;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.dialog.DialogType;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.job.DialoguerJob;
import brainwine.gameserver.player.Player;
import brainwine.gameserver.quest.*;
import brainwine.gameserver.util.MapHelper;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.Pair;

public class Quester extends DialoguerJob {
    @Override
    public List<DialogSection> getMainDialogSection(Npc me, Player player) {
        return Arrays.asList(new DialogSection().setText(MapHelper.getString(GameConfiguration.getBaseConfig(), "dialogs.android.quest")).setChoice("offers"));
    }

    @Override
    public boolean handleDialogAnswers(Npc me, Player player, Object[] ans) {
        return dialogueOfferQuests(me, player);
    }

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

    public boolean dialogueOfferQuests(Npc me, Player player) {
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
                QuestProgress currentProgress = Quests.getIncompleteQuestProgressInCategory(player, category);

                if (currentProgress == null) {
                    long ongoingQuestCount = player.getQuestProgresses().values().stream().filter(p -> !p.isComplete()).count();

                    if(ongoingQuestCount >= 20) {
                        player.showDialog(DialogHelper.messageDialog("Too Many Quests", "Sorry, but you already have 20 or more ongoing quests. Either finish or cancel some before I can offer you more. You may use the /quests command to cancel incomplete quests.").setType(DialogType.ANDROID));
                    }
                    // Offer a set of quests that the player hasn't had before
                    final int count = 5;
                    Set<String> excludedQuests = new HashSet<>(player.getQuestProgresses().keySet());

                    // If the quester is Newton and there are other uncompleted beginner quests don't offer the "Fancy Another Quest" quest.
                    if("Survive and Thrive".equals(category) && excludedQuests.stream().filter(k -> k.startsWith("survival_") && !k.startsWith("survival_random")).count() < Quests.questMaps.get("Survive and Thrive").size() - 1) {
                        excludedQuests.add("survival_quest");
                    }

                    List<Quest> quests = Quests.getRandomQuestsFromCategory(me, category, excludedQuests, count);

                    if(quests.size() < count) {
                        String categoryPrefix = Quests.titleToPrefix.get(category);
                        List<Quest> randomQuests = RandomQuests.generateRandomPlayerQuests(player, RandomQuestDomain.fromCategoryTitle(category), count - quests.size());
                        for(Quest quest : randomQuests) {
                            String id = Integer.toString((0x1000_000 + (int)Math.floor(Math.random() * 0xEFFF_FFF)), 16);
                            quest.setId(categoryPrefix + "_random_" + id);
                            quest.setGroup(category);

                            if(quest.getReward().getXp() == 0) {
                                quest.setReward(new QuestReward().setXp(Math.max(20, 5 * quest.getReward().getCrowns())));
                            } else {
                                quest.setReward(new QuestReward().setXp(Math.max(20, quest.getReward().getXp())));
                            }

                            if(quest.getTasks() == null) quest.setTasks(new ArrayList<>());
                            quest.getTasks().add(new QuestTask()
                                    .setDescription("Return to the android")
                                    .setEvents(Arrays.asList(Arrays.asList("return")))
                            );
                        }
                        quests.addAll(randomQuests);
                    }

                    PlayerQuestDialog.offerQuests(player, quests, quest -> beginQuest(me, player, quest));
                    return true;
                } else {
                    // Follow up on the previous quest
                    return dialogueCheckProgress(me, player, currentProgress.getQuestId());
                }
            }
        }

        player.showDialog(DialogHelper.messageDialog("No Quest Offers", MapHelper.getString(config, "dialogs.android.no_quest")).setType(DialogType.ANDROID));
        return true;
    }

    public boolean dialogueConfirmBeginQuest(Npc me, Player player, String questId) {
        Quest quest = Quests.get(questId);

        if (quest == null) {
            return true;
        }

        PlayerQuestDialog.offerSingleQuest(player, quest, myQuest -> beginQuest(me, player, myQuest));

        return true;
    }

    public void beginQuest(Npc me, Player player, Quest quest) {
        PlayerQuests.beginQuest(player, quest);

        player.showDialog(PlayerQuestDialog.beginQuestDialogGet(player, quest));
    }

    public boolean dialogueCheckProgress(Npc me, Player player, String questId) {
        Quest quest = Quests.get(player, questId);

        PlayerQuests.handleQuestFinalReturn(player, quest);

        if(PlayerQuests.canFinishQuest(player, quest, true)) {
            player.showDialog(DialogHelper.messageDialog(quest.getStory().getComplete() == null
                    ? "You have successfully completed your quest!"
                    : quest.getStory().getComplete()
                        .replace("$family_name", player.getFamilyName() == null ? "unknown" : player.getFamilyName())
            ).setType(DialogType.ANDROID));

            PlayerQuests.finishQuest(player, quest);

            return true;
        } else {
            Dialog dialog = new Dialog().setType(DialogType.ANDROID).setTitle("Cannot Finish Quest Yet");

            dialog.addSection(new DialogSection().setText(quest.getStory().getIncomplete()));

            QuestProgress progress = player.getQuestProgresses().get(questId);
            String cannotCancelReason = progress.getCannotCancelReason(player);
            if(cannotCancelReason == null) {
                dialog.addSection(new DialogSection().setText("You can give up on it if you want to."));
                if(player.isV3()) {
                    dialog.addSection(new DialogSection().setText("<color=#ff0000>Cancel Quest</color>").setChoice("cancelquest"));
                } else {
                    dialog.addSection(new DialogSection().setText("Cancel Quest").setTextColor("ff0000").setChoice("cancelquest"));
                }
            } else {
                dialog.addSection(new DialogSection().setText("You cannot cancel this quest yet. " + cannotCancelReason));
            }
            
            player.showDialog(dialog, ans -> {
                if(ans.length > 0 && "cancelquest".equals(ans[0])) {
                    PlayerQuests.cancelQuest(player, questId, player.isGodMode());
                }
            });

            return true;
        }
        
    }

}
