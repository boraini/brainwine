package brainwine.gameserver.zone;

import brainwine.gameserver.util.RuleRecord;

public class ZoneRules extends RuleRecord {
    @Rule("purgeable")
    private boolean purgeable = true;
    @Rule("auto-clean")
    private boolean autoCleanEnabled = true;
    @Rule(value="auto-clean-duration", minValue=500, maxValue=120000)
    private int autoCleanDuration = 60000;
    @Rule("do-hostile-entity-spawns")
    private boolean hostileEntitySpawnsEnabled = false;
    @Rule(value="do-peaceful-entity-spawns", adminOnly=true)
    private boolean peacefulEntitySpawnsEnabled = false;
    @Rule(value="deleted", adminOnly = true)
    private boolean deleted = false;

    public static ZoneRules getPrivateDefaults() {
        ZoneRules rules = new ZoneRules();

        rules.autoCleanEnabled = false;
        rules.purgeable = false;

        return rules;
    }

    public boolean isAutoCleanEnabled() {
        return autoCleanEnabled;
    }

    public int getAutoCleanDuration() {
        return autoCleanDuration;
    }

    public boolean isHostileEntitySpawnsEnabled() {
        return hostileEntitySpawnsEnabled;
    }

    public boolean isPeacefulEntitySpawnsEnabled() {
        return peacefulEntitySpawnsEnabled;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
