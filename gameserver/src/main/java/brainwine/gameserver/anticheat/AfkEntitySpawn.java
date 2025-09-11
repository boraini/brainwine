package brainwine.gameserver.anticheat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AfkEntitySpawn {
    @JsonProperty
    private boolean enabled = false;

    @JsonProperty
    private long duration = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public long getDuration() {
        return duration;
    }
}
