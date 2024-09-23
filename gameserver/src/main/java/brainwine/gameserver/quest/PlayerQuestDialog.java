package brainwine.gameserver.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import brainwine.gameserver.dialog.Dialog;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;

public class PlayerQuestDialog {
    private PlayerQuestDialog() {}

    public static Dialog questOffersDialogGet(List<Quest> quests) {
        Dialog result = new Dialog().setTitle("My Quest Offers");

        result.addSection(new DialogSection().setText("Here are my quest offers for you:"));
        for (Quest quest : quests) {
            DialogSection section = new DialogSection().setText(quest.getTitle()).setChoice(quest.getId());

            result.addSection(section);
        }

        result.setActions("Cancel");

        return result;
    }

    public static Quest questOffersDialogGetSelectedOffer(Object[] ans) {
        if (ans.length >= 1 && "cancel".equals(ans[0])) return null;

        else return Quests.get((String) ans[0]);
    }

    public static Dialog confirmBeginQuestDialogGet(Quest quest) {
        Dialog result = new Dialog().setTitle(quest.getTitle());

        result.addSection(new DialogSection().setText(quest.getStory().getIntro()));

        result.setActions("Cancel", quest.getStory().getAccept());

        return result;
    }

    public static Dialog getPlayerQuestsDialog(Player player) {
        Dialog result = new Dialog().setTitle("Your Quests");

        List<DialogSection> all = getPlayerQuestsSection(player, false);

        for (DialogSection section : all) {
            result.addSection(section);
        }

        return result;
    }

    public static List<DialogSection> getPlayerQuestsSection(Player player, boolean canFinishQuest) {
        List<DialogSection> result = new ArrayList<>();
        for (QuestProgress questProgress : player.getQuestProgresses().values()) {
            result.addAll(questProgress.getDialogSection(player, canFinishQuest));
        }

        return result;
    }

    /* DRIVER FUNCTIONS */

    public static void offerQuests(Player player, List<Quest> quests, Consumer<Quest> onSelect) {
        player.showDialog(questOffersDialogGet(quests), ans -> {
            Quest quest = questOffersDialogGetSelectedOffer(ans);

            if (quest != null) {
                offerSingleQuest(player, quest, onSelect);
            }
        });
    }

    public static void offerSingleQuest(Player player, Quest quest, Consumer<Quest> onSelect) {
        player.showDialog(confirmBeginQuestDialogGet(quest), ans -> {
            if (ans.length < 1 || "cancel".equals(ans[0])) return;
            System.out.println(ans[0]);
            onSelect.accept(quest);
        });
    }

}
