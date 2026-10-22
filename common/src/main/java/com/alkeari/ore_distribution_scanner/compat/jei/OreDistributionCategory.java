package com.alkeari.ore_distribution_scanner.compat.jei;

import com.mojang.blaze3d.platform.InputConstants;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import com.alkeari.ore_distribution_scanner.OreDistributionScanner;
import com.alkeari.ore_distribution_scanner.client.graph.OreDistributionGraph;
import com.alkeari.ore_distribution_scanner.client.graph.OreEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class OreDistributionCategory implements IRecipeCategory<OreEntry> {
    public static final RecipeType<OreEntry> RECIPE_TYPE =
            RecipeType.create(OreDistributionScanner.MOD_ID, "ore_distribution", OreEntry.class);

    private final IDrawable icon;
    private final IGuiHelper helper;

    public OreDistributionCategory(IGuiHelper helper) {
        this.helper = helper;
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.IRON_ORE));
    }

    @Override
    public int getWidth() {
        return OreDistributionGraph.width();
    }

    @Override
    public int getHeight() {
        return OreDistributionGraph.height();
    }

    @Override
    public RecipeType<OreEntry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        // Empty so JEI does not draw a plain title above the branded in-canvas header.
        return Component.empty();
    }

    @Override
    public IDrawable getBackground() {
        return helper.createBlankDrawable(OreDistributionGraph.width(), OreDistributionGraph.height());
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OreEntry recipe, mezz.jei.api.recipe.IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 7, 21)
                .addIngredients(VanillaTypes.ITEM_STACK, List.of(recipe.icon()));
    }

    @Override
    public void draw(OreEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        OreDistributionGraph.render(guiGraphics, 0, 0, recipe, mouseX, mouseY);
    }

    @Override
    public List<Component> getTooltipStrings(OreEntry recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        return OreDistributionGraph.tooltip(0, 0, recipe, mouseX, mouseY);
    }

    @Override
    public boolean handleInput(OreEntry recipe, double mouseX, double mouseY, InputConstants.Key input) {
        if (input.getType() == InputConstants.Type.MOUSE && input.getValue() == InputConstants.MOUSE_BUTTON_LEFT) {
            return OreDistributionGraph.mouseClicked(0, 0, recipe, mouseX, mouseY);
        }
        return false;
    }

    public static ResourceLocation uid() {
        return RECIPE_TYPE.getUid();
    }
}
