package com.alkeari.ore_distribution_scanner.compat.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.DrawableConsumer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreDistributionGraph;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import com.alkeari.ore_distribution_scanner.client.graph.AlkeariColors;

public class OreDistributionReiCategory implements DisplayCategory<OreDistributionDisplay> {
    public static final CategoryIdentifier<OreDistributionDisplay> ID =
            CategoryIdentifier.of(OreDistributionScanner.MOD_ID, "ore_distribution");

    @Override
    public CategoryIdentifier<? extends OreDistributionDisplay> getCategoryIdentifier() {
        return ID;
    }

    @Override
    public Component getTitle() {
        // Empty so REI does not draw a plain title above the branded in-canvas header.
        return Component.empty();
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(new ItemStack(Items.IRON_ORE));
    }

    @Override
    public int getDisplayWidth(OreDistributionDisplay display) {
        return OreDistributionGraph.width() + 8;
    }

    @Override
    public int getDisplayHeight() {
        return OreDistributionGraph.height() + 8;
    }

    @Override
    public List<Widget> setupDisplay(OreDistributionDisplay display, Rectangle bounds) {
        int x = bounds.x + 4;
        int y = bounds.y + 4;
        OreEntry entry = display.entry();
        int w = OreDistributionGraph.width();
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        // Draw through REI's own drawable widget (REI's DrawableConsumer, not a vanilla render
        // override), so it remaps correctly on Forge instead of throwing AbstractMethodError.
        widgets.add(Widgets.createDrawableWidget((DrawableConsumer)
                (graphics, mouseX, mouseY, delta) -> OreDistributionGraph.render(graphics, x, y, entry, mouseX, mouseY)));

        // Clickable biome-selector arrows (REI labels carry their own click handler).
        widgets.add(Widgets.createClickableLabel(new Point(x + 13, y + 43), Component.literal("<"),
                label -> OreDistributionGraph.cycleBiome(entry, -1)).color(AlkeariColors.FORGED_GOLD)
                .tooltip(Component.translatable("gui.ore_distribution_scanner.arrow_prev")));
        widgets.add(Widgets.createClickableLabel(new Point(x + w - 13, y + 43), Component.literal(">"),
                label -> OreDistributionGraph.cycleBiome(entry, 1)).color(AlkeariColors.FORGED_GOLD)
                .tooltip(Component.translatable("gui.ore_distribution_scanner.arrow_next")));

        widgets.add(Widgets.createSlot(new Point(x + 7, y + 21))
                .entries(display.getOutputEntries().get(0))
                .disableBackground()
                .markOutput());
        return widgets;
    }
}
