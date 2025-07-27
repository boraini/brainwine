package brainwine.gameserver.quest.randomquests;

public class Inhibit extends EventAndQuantity {
    public Inhibit() {
        this.setEvent("inhibit");
        this.setTitle("Inhibit Evokers");
        this.setDescriptionSource("inhibit_description");
        this.setSingularTaskDescription("Inhibit an evoker");
        this.setPluralTaskDescription("Inhibit {QUANTITY} evokers");
        this.getStory()
                .setIntro("You will be inhibiting {QUANTITY} evokers. Sounds good?")
                .setIncomplete("You still haven't inhibited {QUANTITY} evokers.");
    }
}
