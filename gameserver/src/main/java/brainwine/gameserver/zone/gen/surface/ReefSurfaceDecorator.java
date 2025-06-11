package brainwine.gameserver.zone.gen.surface;

import brainwine.gameserver.item.Item;
import brainwine.gameserver.item.ItemRegistry;
import brainwine.gameserver.util.MathUtils;
import brainwine.gameserver.util.WeightedMap;
import brainwine.gameserver.zone.gen.GeneratorContext;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Arrays;
import java.util.List;

public class ReefSurfaceDecorator extends SurfaceDecorator {
    @JsonProperty
    private int stride = 10;
    @JsonProperty("min_blobs")
    private int minBlobs = 5;
    @JsonProperty("max_blobs")
    private int maxBlobs = 10;
    @JsonProperty("blob_min_radius")
    private double blobMinRadius = 1.0;
    @JsonProperty("blob_max_radius")
    private double blobMaxRadius = 3.0;
    @JsonProperty("blob_spacing")
    private double blobSpacing = 2.0;
    @JsonProperty("anemone_frequency")
    private double anemoneFrequency = 0.6;
    @JsonProperty("rock_materials")
    private WeightedMap<RockMaterial> rockMaterials = new WeightedMap<>();
    @JsonProperty("anemones")
    private WeightedMap<String> anemones = new WeightedMap<>();

    private static class RockMaterial {
        public String front;
        public String back;
        public double frequency;
    }

    @Override
    public void decorate(GeneratorContext ctx, SurfaceRegion region) {
        if(region.getEnd() - region.getStart() < stride) return;
        if(rockMaterials.isEmpty()) return;

        int currentX = (int)Math.floor(region.getStart() + MathUtils.lerp(blobSpacing * maxBlobs, stride, ctx.nextDouble()));

        while(currentX < region.getEnd() - minBlobs) {
            int numBlobs = minBlobs + ctx.nextInt(maxBlobs - minBlobs);

            double reefStart = currentX;
            double reefEnd = currentX + MathUtils.lerp(blobSpacing * maxBlobs, stride, ctx.nextDouble());

            currentX = (int)(Math.ceil(reefEnd + blobSpacing * (minBlobs + ctx.nextInt(maxBlobs - minBlobs))));

            double[] blobX = new double[numBlobs];
            double[] blobY = new double[numBlobs];
            double[] blobRadiusInner = new double[numBlobs];
            double[] blobRadiusOuter = new double[numBlobs];

            int bottom = ctx.getSurface(currentX);
            int top = ctx.getSurface(currentX);
            // Generate some blobs.
            for(int i = 0; i < numBlobs; i++) {
                blobX[i] = MathUtils.lerp(reefStart, reefEnd, (double)i / numBlobs) + (2.0 * ctx.nextDouble() - 1.0);
                int blockIndex = MathUtils.clamp((int)Math.round(blobX[i]), region.getStart(), region.getEnd() - 1);
                blobRadiusInner[i] = MathUtils.lerp(blobMinRadius, blobMaxRadius, ctx.nextDouble());
                blobRadiusOuter[i] = 1.4 * blobRadiusInner[i];
                blobY[i] = ctx.getSurface(blockIndex) - 0.7 * blobRadiusOuter[i] + 0.5 * (2.0 * ctx.nextDouble() - 1.0);

                // Calculate top and bottom bounds for the next step.
                top = Math.min(top, (int)Math.ceil(blobY[i] - blobRadiusOuter[i]));
                bottom = Math.max(bottom, (int)Math.ceil(blobY[i] + blobRadiusOuter[i]));
            }

            int left = (int)Math.floor(blobX[0] - blobRadiusOuter[0]);
            int right = (int)Math.ceil(blobX[numBlobs - 1] + blobRadiusInner[numBlobs - 1]);

            // Calculate surface height to place anemones.
            int[] backHeight = new int[right - left + 1];
            int[] frontHeight = new int[right - left + 1];
            Arrays.fill(backHeight, Integer.MAX_VALUE);
            Arrays.fill(frontHeight, Integer.MAX_VALUE);

            // Place the blobs.
            RockMaterial rockMaterial = rockMaterials.next(ctx.getRandom());
            Item backItem = ItemRegistry.getItem(rockMaterial.back);
            Item frontItem = ItemRegistry.getItem(rockMaterial.front);

            for(int i = 0; i < numBlobs; i++) {
                for(int x = left; x <= right; x++) {
                    for(int y = top; y <= bottom; y++) {
                        double dist = Math.hypot(blobX[i] - x, blobY[i] - y);
                        if(dist <= blobRadiusOuter[i]) {
                            ctx.updateBlock(x, y, backItem.getLayer(), backItem);
                            backHeight[x - left] = Math.min(backHeight[x - left], y);
                        }
                        if(dist <= blobRadiusInner[i]) {
                            ctx.updateBlock(x, y, frontItem.getLayer(), frontItem);
                            frontHeight[x - left] = Math.min(frontHeight[x - left], y);
                        }
                    }
                }
            }

            // Place anemone.
            if(!anemones.isEmpty()) for(int x = left; x <= right; x++) {
                if(backHeight[x - left] < frontHeight[x - left] && ctx.nextDouble() < anemoneFrequency) {
                    Item anemone = ItemRegistry.getItem(anemones.next(ctx.getRandom()));
                    ctx.updateBlock(x, backHeight[x - left] - 1, anemone.getLayer(), anemone);
                }

                if(ctx.nextDouble() < anemoneFrequency) {
                    Item anemone = ItemRegistry.getItem(anemones.next(ctx.getRandom()));
                    ctx.updateBlock(x, frontHeight[x - left] - 1, anemone.getLayer(), anemone);
                }
            }
        }
    }

    @JsonSetter("rock_materials")
    public void setRockMaterials(List<RockMaterial> materials) {
        rockMaterials = new WeightedMap<>();
        for(RockMaterial material : materials) {
            rockMaterials.addEntry(material, material.frequency);
        }
    }
}
