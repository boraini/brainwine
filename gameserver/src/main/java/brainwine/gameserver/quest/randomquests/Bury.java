package brainwine.gameserver.quest.randomquests;

public class Bury extends EventAndQuantity {
    public Bury() {
        this.setEvent("bury");
        this.setTitle("Bury Skeletons");
        this.setDescriptionSource("bury_description");
        this.setSingularTaskDescription("Bury a skeleton");
        this.setPluralTaskDescription("Bury {QUANTITY} skeletons");
        this.getStory()
                .setIntro("You will be burying {QUANTITY} skeletons. Sounds good?")
                .setIncomplete("You still haven't buried {QUANTITY} skeletons.");
    }
}
