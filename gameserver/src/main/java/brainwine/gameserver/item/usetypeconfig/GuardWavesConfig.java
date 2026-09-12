package brainwine.gameserver.item.usetypeconfig;

import brainwine.gameserver.util.MapHelper;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Properties
public class GuardWavesConfig extends ItemUseTypeConfig {
    private Map<Integer, Map<String, Integer>> guards = WaveType.REVENANTS.getGuards();
    private boolean auto = true;
    private boolean explode = true;
    private String change = null;

    @JsonSetter
    public void setGuards(String guardsPreset) {
        guards = WaveType.valueOf(guardsPreset.toUpperCase()).getGuards();
    }

    @JsonSetter
    public void setGuards(Map<Integer, Map<String, Integer>> customGuards) {
        guards = customGuards;
    }

    public Map<Integer, Map<String, Integer>> getGuards() {
        return guards;
    }

    @JsonSetter
    public boolean isAuto() {
        return auto;
    }

    @JsonSetter
    public boolean isExplode() {
        return explode;
    }

    @JsonSetter
    public String getChange() {
        return change;
    }

    public Map<String, Integer> getGuardsForWave(int wave) {
        Optional<Integer> effective = guards.keySet().stream().filter(x -> x <= wave).max(Integer::compare);
        return guards.get(effective.orElse(1));
    }

    public enum WaveType {
        REVENANTS(new Object() {
            Map<Integer, Map<String, Integer>> evaluate() {
                Map<Integer, Map<String, Integer>> result = new HashMap<>();
                result.put(1, MapHelper.map("revenant", 5));
                result.put(2, MapHelper.map("dire-revenant", 3));
                result.put(3, MapHelper.map("revenant-lord", 1));
                result.put(4, MapHelper.map("terrapus/adult", 0));
                return result;
            }
        }.evaluate()),
        BRAINS(new Object() {
            Map<Integer, Map<String, Integer>> evaluate() {
                Map<Integer, Map<String, Integer>> result = new HashMap<>();
                result.put(1, MapHelper.map("brains/small-minion", 5));
                result.put(2, MapHelper.map("brains/medium-minion", 3));
                result.put(3, MapHelper.map("brains/medium-dire-minion", 1));
                result.put(4, MapHelper.map("terrapus/adult", 0));
                return result;
            }
        }.evaluate()),
        ;
        private final Map<Integer, Map<String, Integer>> guards;
        WaveType(Map<Integer, Map<String, Integer>> guards) {
            this.guards = guards;
        }

        Map<Integer, Map<String, Integer>> getGuards() {
            return guards;
        }
    }
}
