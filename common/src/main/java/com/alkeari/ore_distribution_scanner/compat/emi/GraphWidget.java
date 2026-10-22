package com.alkeari.ore_distribution_scanner.compat.emi;

import dev.emi.emi.api.widget.DrawableWidget;
import com.alkeari.ore_distribution_scanner.client.graph.OreDistributionGraph;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntry;

/**
 * EMI graph widget. It extends EMI's own {@link DrawableWidget} and passes the draw consumer to the
 * constructor rather than subclassing {@code Widget} and overriding {@code render}: on Architectury
 * Forge an overridden vanilla {@code render(GuiGraphics,...)} does not remap to its SRG name and
 * throws AbstractMethodError. Only {@code mouseClicked} (EMI's own non-vanilla method) is overridden,
 * for the clickable biome selector. The readout is drawn in-canvas, so no tooltip is needed.
 */
public class GraphWidget extends DrawableWidget {
    private final OreEntry entry;

    public GraphWidget(OreEntry entry) {
        super(0, 0, OreDistributionGraph.width(), OreDistributionGraph.height(),
                (draw, mouseX, mouseY, delta) -> OreDistributionGraph.render(draw, 0, 0, entry, mouseX, mouseY));
        this.entry = entry;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return button == 0 && OreDistributionGraph.mouseClicked(0, 0, entry, mouseX, mouseY);
    }
}
