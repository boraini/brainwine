package brainwine.gameserver.zone.gen.tasks;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.item.Layer;
import brainwine.gameserver.util.WeightedMap;
import brainwine.gameserver.zone.gen.GeneratorConfig;
import brainwine.gameserver.zone.gen.GeneratorContext;
import brainwine.gameserver.zone.gen.models.SedimentType;
import brainwine.gameserver.zone.gen.models.TerrainType;

public class OceanGeneratorTask implements GeneratorTask {
    private boolean enabled;
    private final WeightedMap<SedimentType> sedimentVariants;

    public OceanGeneratorTask(GeneratorConfig config) {
        this.enabled = config.getTerrainType() == TerrainType.OCEAN;
        this.sedimentVariants = config.getSedimentTypes();
    }
    @Override
    public void generate(GeneratorContext ctx) {
        // Only generate ocean in oceans.
        if(!this.enabled) return;

        // Add sediment to flat regions.
        final int oceanY = getOceanY(ctx);
        final int sedimentDepth = 3;
        final int sedimentCut = 5;
        int lastY = -1000;
        SedimentType currentSediment = null;
        if(!sedimentVariants.isEmpty()) for(int x = 0; x < ctx.getWidth(); x++) {
            int surface = ctx.getSurface(x);
            // Don't generate sediment too close to the surface.
            if(surface > oceanY + sedimentCut) {
                // Sediment doesn't generate on steep parts.
                if (Math.abs(surface - lastY) > sedimentCut) {
                    currentSediment = null;
                } else {
                    if (currentSediment == null) currentSediment = sedimentVariants.next(ctx.getRandom());
                    for (int i = 0; i < sedimentDepth; i++) {
                        ctx.updateBlock(x, surface + i, Layer.BASE, currentSediment.getBaseItem());
                        ctx.updateBlock(x, surface + i, Layer.FRONT, currentSediment.getFrontItem());
                    }
                }
            }
            lastY = ctx.getSurface(x);
        }

        // Fill the rest with water.
        Item water = ItemRegistry.getItem("liquid/water");
        for(int x = 0; x < ctx.getWidth(); x++) {
            int surface = ctx.getSurface(x);

            for(int y = oceanY; y < surface; y++) {
                ctx.updateBlock(x, y, Layer.LIQUID, water, 5);
            }
        }
    }

    public static int getOceanY(GeneratorContext ctx) {
        return (int)(0.4 * ctx.getHeight());
    }
}
