package brainwine.gameserver.quest;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import brainwine.gameserver.dialog.DialogListItem;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.player.Player;

public class QuestProgress {
    private String questId;
    private List<Integer> taskProgresses;

    public QuestProgress() {}
    public QuestProgress(String questId, List<Integer> taskProgresses) {
        this.questId = questId;
        this.taskProgresses = taskProgresses;
    }

    @JsonIgnore
    public Quest getQuest() {
        return Quests.get(getQuestId());
    }

    public int getTaskProgress(int index) {
        if (index < 0 || index >= getTaskProgresses().size()) {
            return 0;
        }

        return getTaskProgresses().get(index);
    }

    public String getQuestId() {
        return questId;
    }

    public List<Integer> getTaskProgresses() {
        return taskProgresses;
    }

    public String getActionChoice(String action) {
        return String.format("quest.%s.%s", questId, action);
    }

    public List<DialogSection> getDialogSection(Player player, boolean canFinishQuest) {
        List<DialogSection> result = new ArrayList<>();
        DialogSection mainSection = new DialogSection();
        result.add(mainSection);

        Quest quest = getQuest();

        if (quest == null) {
            mainSection.setTitle("QUEST NOT FOUND");
            
            return result;
        }

        mainSection.setTitle(quest.getTitle());

        if (PlayerQuests.canFinishQuest(player, quest)) {
            mainSection.addItem(new DialogListItem().setImage("shop/premium").setText("All tasks done. Visit a quester android to claim your reward!"));
        }

        for (int i = 0; i < quest.getTasks().size(); i++) {
            result.add(quest.getTasks().get(i).getDialogSection(getTaskProgress(i)));
        }

        if (canFinishQuest) {
            result.add(new DialogSection().setText("Finish Quest").setChoice(getActionChoice("finish")));
        }

        DialogSection cancelSection = new DialogSection().setChoice(getActionChoice("cancel"));

        if (player.isV3()) {
            cancelSection.setText("<color=#ff0000>Cancel Quest</color>");
        } else {
            cancelSection.setText("Cancel Quest").setTextColor("#ff0000");
        }

        result.add(cancelSection);

        return result;
    }

}
