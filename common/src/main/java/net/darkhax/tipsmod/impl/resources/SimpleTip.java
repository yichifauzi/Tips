package net.darkhax.tipsmod.impl.resources;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.darkhax.bookshelf.api.ClientServices;
import net.darkhax.bookshelf.api.Services;
import net.darkhax.bookshelf.api.function.CachedSupplier;
import net.darkhax.bookshelf.api.serialization.Serializers;
import net.darkhax.tipsmod.api.TipsAPI;
import net.darkhax.tipsmod.api.resources.ITip;
import net.darkhax.tipsmod.api.resources.ITipSerializer;
import net.darkhax.tipsmod.impl.TipsModCommon;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A simple implementation of the tip.
 */
public class SimpleTip implements ITip {

    /**
     * The serializer for this type of tip.
     */
    public static final ITipSerializer<SimpleTip> SERIALIZER = new Serializer();

    /**
     * The namespaced id of the tip.
     */
    private final ResourceLocation id;

    /**
     * The title text to display.
     */
    private final Component title;

    /**
     * The body of the tip.
     */
    private final Component text;

    /**
     * Time to keep the tip displayed for.
     */
    private final Optional<Integer> cycleTime;

    private final ConditionRules<Screen> screens;
    private final ConditionRules<Holder<Biome>> biome;
    private final ConditionRules<Holder<DimensionType>> dimension;
    private final ConditionRules<Set<ResourceLocation>> advancements;

    public SimpleTip(ResourceLocation id, Component title, Component text, Optional<Integer> cycleTime) {
        this(id, title, text, cycleTime, null, null, null, null);
    }

    public SimpleTip(ResourceLocation id, Component title, Component text, Optional<Integer> cycleTime, ConditionRules<Screen> screens, ConditionRules<Holder<Biome>> biomes, ConditionRules<Holder<DimensionType>> dimension, ConditionRules<Set<ResourceLocation>> advancements) {

        this.id = id;
        this.title = title;
        this.text = text;
        this.cycleTime = cycleTime;
        this.screens = screens;
        this.biome = biomes;
        this.dimension = dimension;
        this.advancements = advancements;
    }

    @Override
    public ResourceLocation getId() {

        return this.id;
    }

    @Override
    public Component getTitle() {

        return this.title;
    }

    @Override
    public Component getText() {

        return this.text;
    }

    @Override
    public int getCycleTime() {

        return this.cycleTime.orElse(TipsModCommon.CONFIG.defaultCycleTime);
    }

    @Override
    public boolean canDisplayOnScreen(Screen screen) {

        if ((this.screens != null && !this.screens.isEmpty()) ? this.screens.test(screen) : TipsAPI.canRenderOnScreen(screen)) {

            final LocalPlayer player = Minecraft.getInstance().player;

            if (player == null) {
                return false;
            }

            if (this.biome != null && !this.biome.isEmpty() && !this.biome.test(player.level().getBiome(player.blockPosition()))) {
                return false;
            }

            if (this.dimension != null && !this.dimension.isEmpty() && !this.dimension.test(player.level().dimensionTypeRegistration())) {
                return false;
            }

            if (this.advancements != null && !this.advancements.isEmpty()) {
                final CachedSupplier<Set<ResourceLocation>> completedAdvancements = CachedSupplier.cache(() -> ClientServices.CLIENT.getCompletedAdvancements(player).stream().map(Advancement::getId).collect(Collectors.toSet()));
                if (!this.advancements.test(completedAdvancements.get())) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private static final class Serializer implements ITipSerializer<SimpleTip> {

        @Override
        public SimpleTip fromJSON(ResourceLocation id, JsonObject json) {

            final Component title = Serializers.TEXT.fromJSON(json, "title", TipsModCommon.CONFIG.defaultTitle);
            final Component text = Serializers.TEXT.fromJSON(json, "tip");
            final Optional<Integer> cycleTime = Serializers.INT.fromJSONOptional(json, "cycleTime");

            if (title == null) {

                throw new JsonParseException("Tip " + id.toString() + " does not have a title. This is required!");
            }

            if (text == null) {

                throw new JsonParseException("Tip " + id.toString() + " does not have text. This is required.");
            }

            final JsonObject conditions = json.getAsJsonObject("conditions");

            if (conditions != null) {
                final ConditionRules<Screen> screenRules = ConditionRules.fromElement(ConditionRules::screenRuleBuilder, conditions.get("screens"));
                final ConditionRules<Holder<Biome>> biomeRules = ConditionRules.fromElement(rule -> ConditionRules.registryRuleBuilder(Services.TAGS::biomeTag, rule), conditions.get("biomes"));
                final ConditionRules<Holder<DimensionType>> dimensionRules = ConditionRules.fromElement(rule -> ConditionRules.registryRuleBuilder(Services.TAGS::dimensionTypeTag, rule), conditions.get("dimensions"));
                final ConditionRules<Set<ResourceLocation>> advancements = ConditionRules.fromElement(ConditionRules::resourceLocationsRuleBuilder, conditions.get("advancements"));
                return new SimpleTip(id, title, text, cycleTime, screenRules, biomeRules, dimensionRules, advancements);
            }

            return new SimpleTip(id, title, text, cycleTime);
        }

        @Override
        public JsonObject toJSON(SimpleTip toWrite) {

            final JsonObject json = new JsonObject();
            Serializers.RESOURCE_LOCATION.toJSON(json, "type", TipsAPI.DEFAULT_SERIALIZER);
            Serializers.TEXT.toJSON(json, "title", toWrite.title);
            Serializers.TEXT.toJSON(json, "tip", toWrite.text);
            Serializers.INT.toJSONOptional(json, "cycleTime", toWrite.cycleTime);

            final JsonObject conditions = new JsonObject();

            if (toWrite.screens != null && !toWrite.screens.isEmpty()) {
                conditions.add("screens", toWrite.screens.writeJson());
            }

            if (toWrite.biome != null && !toWrite.biome.isEmpty()) {
                conditions.add("biomes", toWrite.biome.writeJson());
            }

            if (toWrite.dimension != null && !toWrite.dimension.isEmpty()) {
                conditions.add("dimensions", toWrite.dimension.writeJson());
            }

            if (toWrite.advancements != null && !toWrite.advancements.isEmpty()) {
                conditions.add("advancements", toWrite.advancements.writeJson());
            }

            if (!conditions.keySet().isEmpty()) {
                json.add("conditions", conditions);
            }

            return json;
        }
    }
}