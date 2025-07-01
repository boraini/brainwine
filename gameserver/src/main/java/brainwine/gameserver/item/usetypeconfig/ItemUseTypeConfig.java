package brainwine.gameserver.item.usetypeconfig;

@Properties
public class ItemUseTypeConfig {
    protected Object config = null;

    public Object getConfig() {
        return config;
    }

    public ItemUseTypeConfig setConfig(Object config) {
        this.config = config;
        return this;
    }
}
