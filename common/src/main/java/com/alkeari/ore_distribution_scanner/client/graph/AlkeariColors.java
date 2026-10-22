package com.alkeari.ore_distribution_scanner.client.graph;

/**
 * The Alkeari Design System palette as packed ARGB ints, the Java reference implementation. Forged
 * Gold is the sole primary accent and must stay visually dominant; supporting accents are used
 * sparingly. Alert Amber marks low-confidence and warnings; Olive-Gold marks freshness and success.
 * The palette is closed: every constant below is one of the eight colors, one of the five hueless
 * neutrals, or a permitted translucent variant of one of them.
 */
public final class AlkeariColors {
    private AlkeariColors() {
    }

    // Opaque brand colors
    public static final int OBSIDIAN_BLACK = 0xFF000000;
    public static final int FORGED_GOLD = 0xFFFFDA00;
    public static final int WARM_GOLD_SHADOW = 0xFFC9A800;
    public static final int SOFT_CHAMPAGNE = 0xFFFFF1B3;
    public static final int STEEL_GRAY = 0xFFA6A6A6;
    public static final int DEEP_GRAPHITE = 0xFF1A1A1A;
    public static final int ALERT_AMBER = 0xFFFFB000;
    public static final int SUCCESS_OLIVE_GOLD = 0xFF8F8B2E;

    // The five hueless neutrals. Structural only, never accents, and the only grays that exist:
    // a near-black invention outside this list is a defect.
    public static final int HAIRLINE = 0xFF333333;
    public static final int RAISED = 0xFF2E2E2E;
    public static final int RAISED_HOVER = 0xFF3D3D3D;
    public static final int DISABLED_TEXT = 0xFF6E6E6E;
    public static final int DE_EMPHASIZED_TEXT = 0xFF8A8A8A;

    // Surface / chrome fills, every one a role above rather than a tint of its own. The graph paints
    // itself on Obsidian Black, so a Deep Graphite pill or tooltip on it is a region, not graphite
    // over graphite; the item slot is an inset well and takes the raised fill.
    public static final int SLOT_FILL = RAISED;
    public static final int SLOT_BORDER = HAIRLINE;
    public static final int SLOT_BEVEL = 0x38A6A6A6; // Steel Gray at 22 percent
    public static final int PILL_FILL = DEEP_GRAPHITE;
    public static final int PILL_BORDER_INACTIVE = HAIRLINE;
    public static final int ARROW_DISABLED = DISABLED_TEXT;
    public static final int TOOLTIP_FILL = DEEP_GRAPHITE;
    public static final int AMBER_CHIP_FILL = DEEP_GRAPHITE;

    // Translucent
    public static final int INNER_HAIRLINE = 0x73C9A800;
    public static final int GRIDLINE = 0x1FA6A6A6;
    public static final int HOVER_GUIDE = 0x80C9A800;
    public static final int CONNECTOR = 0x8CFFF1B3;
    public static final int LOW_CONF_BORDER = 0x8CFFB000;
    public static final int EMPTY_AXIS = 0x59A6A6A6;
    public static final int EMPTY_AXIS_LABEL = 0x80A6A6A6;
    public static final int EMPTY_SCAN_GLYPH = 0x4DFFDA00;

    /** The same color without its alpha, for APIs that take a 24 bit RGB value (chat styles). */
    public static int rgb(int argb) {
        return argb & 0x00FFFFFF;
    }
}
