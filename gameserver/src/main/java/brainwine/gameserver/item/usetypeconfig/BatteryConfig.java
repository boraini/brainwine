package brainwine.gameserver.item.usetypeconfig;

@Properties
public class BatteryConfig extends ItemUseTypeConfig {
    double capacity = 50.0;

    public double getCapacity() {
        return capacity;
    }

    @Override
    public String toString() {
        return "BatteryConfig{" + "capacity=" + capacity + '}';
    }
}
