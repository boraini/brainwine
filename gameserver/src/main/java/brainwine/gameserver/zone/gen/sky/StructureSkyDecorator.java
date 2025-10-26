package brainwine.gameserver.zone.gen.sky;

import brainwine.gameserver.prefab.Prefab;
import brainwine.gameserver.util.WeightedMap;
import brainwine.gameserver.zone.gen.GeneratorContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StructureSkyDecorator extends SkyDecorator {
    @JsonProperty("prefabs")
    protected WeightedMap<Prefab> prefabs = new WeightedMap<>();

    @JsonCreator
    protected StructureSkyDecorator() {}

    @Override
    public void decorate(GeneratorContext ctx, int x, int y) {
        if(!prefabs.isEmpty()) {
            place(prefabs.next(ctx.getRandom()), ctx, x, y);
        }
    }

    public static void place(Prefab prefab, GeneratorContext ctx, int x, int y) {
        if(prefab == null) return;
        int clearance = 2;
        int minX = Math.max(clearance, x - prefab.getWidth() + 1);
        int maxX = Math.min(ctx.getWidth() - prefab.getWidth() - clearance, x);
        int minY = Math.max(clearance, y - prefab.getHeight() + 1);
        int maxY = Math.min(ctx.getHeight() - prefab.getHeight() - clearance, y);
        x = minX + (int)(ctx.nextDouble() * (maxX - minX));
        y = minY + (int)(ctx.nextDouble() * (maxY - minY));
        ctx.placePrefab(prefab, x, y);
    }

    public WeightedMap<Prefab> getPrefabs() {
        return prefabs;
    }
}
