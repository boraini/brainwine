package brainwine.gameserver.entity.npc.job.jobs;

import brainwine.gameserver.Fake;
import brainwine.gameserver.dialog.DialogSection;
import brainwine.gameserver.entity.npc.Npc;
import brainwine.gameserver.entity.npc.job.DialoguerJob;
import brainwine.gameserver.player.Player;

import java.util.Arrays;
import java.util.List;

public class Joker extends DialoguerJob {

    @Override
    public List<DialogSection> getMainDialogSection(Npc me, Player player) {
        return Arrays.asList(new DialogSection().setText(Fake.get(Fake.Type.JOKE)));
    }

    @Override
    public boolean handleDialogAnswers(Npc me, Player other, Object[] ans) {
        return true;
    }
    
}
