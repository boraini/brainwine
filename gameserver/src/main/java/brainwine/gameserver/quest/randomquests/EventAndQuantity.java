package brainwine.gameserver.quest.randomquests;

import brainwine.gameserver.player.Player;
import brainwine.gameserver.quest.Quest;
import brainwine.gameserver.quest.QuestReward;
import brainwine.gameserver.quest.QuestStory;
import brainwine.gameserver.quest.QuestTask;
import brainwine.gameserver.quest.RandomQuest;
import brainwine.gameserver.quest.RandomQuestReward;
import brainwine.gameserver.quest.RandomQuests;
import brainwine.gameserver.util.randomobject.ConcretionFailureException;
import brainwine.gameserver.util.randomobject.RandomInteger;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EventAndQuantity extends RandomQuest {
    @JsonProperty("quantity")
    private RandomInteger quantity;

    private String title = "Do Something N Times";
    private String event = "inhibit";
    private String singularTaskDescription = "Do this {QUANTITY} times.";
    private String pluralTaskDescription = "Do this {QUANTITY} times.";
    private String descriptionSource = null;
    private String description = null;
    private QuestStory story = new QuestStory()
            .setIntro("You will need to inhibit do something {QUANTITY} times. Sounds good?")
            .setAccept("Alright")
            .setBegin("OK then. Good luck!")
            .setIncomplete("You still haven't killed all the necessary entities.")
            .setComplete("Good job! You are getting a reward your hard work.\nHope to see you again!");

    protected EventAndQuantity setEvent(String event) {
        this.event = event;
        return this;
    }

    protected EventAndQuantity setSingularTaskDescription(String singularTaskDescription) {
        this.singularTaskDescription = singularTaskDescription;
        return this;
    }

    protected EventAndQuantity setPluralTaskDescription(String pluralTaskDescription) {
        this.pluralTaskDescription = pluralTaskDescription;
        return this;
    }

    protected EventAndQuantity setTitle(String title) {
        this.title = title;
        return this;
    }

    protected EventAndQuantity setDescriptionSource(String descriptionSource) {
        this.descriptionSource = descriptionSource;
        return this;
    }

    protected EventAndQuantity setDescription(String description) {
        this.description = description;
        return this;
    }

    protected QuestStory getStory() {
        return this.story;
    }

    private String format(String string, int quantity) {
        return string.replaceAll("\\{QUANTITY}", Integer.toString(quantity));
    }

    @Override
    public Quest nextQuest(Random random, Player player) {
        int quantity;
        QuestReward reward;
        try {
            quantity = this.quantity.next(random);
            reward = RandomQuestReward.nextOrDefault(random, getReward());
        } catch (ConcretionFailureException e) {
            throw new IllegalStateException("Concretion failure!");
        }

        Quest quest = new Quest();
        quest.setDescription(description != null ? description : RandomQuests.getString(random, descriptionSource));

        quest.setTasks(Arrays.asList(
                new QuestTask()
                        .setDescription(format(quantity == 1 ? singularTaskDescription : pluralTaskDescription, quantity))
                        .setQuantity(quantity)
                        .setEvents(List.of(List.of(event)))
        ));

        quest.setTitle(title);
        quest.setReward(reward);

        quest.setStory(new QuestStory()
                .setIntro(format(story.getIntro(), quantity))
                .setAccept(format(story.getAccept(), quantity))
                .setBegin(format(story.getBegin(), quantity))
                .setIncomplete(format(story.getIncomplete(), quantity))
                .setComplete(format(story.getComplete(), quantity))
        );

        return quest;
    }
}
