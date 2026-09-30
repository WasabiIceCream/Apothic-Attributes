package dev.shadowsoffire.apothic_attributes.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.util.Offset;
import dev.shadowsoffire.placebo.util.Offset.AnchorPoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Client side of the port: the Attributes GUI, its client command, effect tooltips in the inventory, potion tooltips,
 * the crit particle and the client config reload.
 * <p>
 * Port note (NeoForge -> Fabric): upstream's NeoForge screen events become Fabric's {@link ScreenEvents} and
 * {@link ScreenMouseEvents} (see {@link AttributesGui}); {@code GatherEffectScreenTooltipsEvent} is
 * {@code EffectsInInventoryMixin}, which calls {@link #effectTooltip}; the client command uses Fabric's client command
 * API.
 */
public class AttributesLibClient implements ClientModInitializer {

    /**
     * The Attributes GUI of the inventory screen currently open, if any.
     */
    @Nullable
    private static AttributesGui activeAttribGui = null;

    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register(AttributesLibClient::addAttribComponent);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> registerCommands(dispatcher));

        // Port replacement for upstream's Curios compat: Trinkets items as modifier sources in the Attributes GUI.
        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            dev.shadowsoffire.apothic_attributes.compat.TrinketsModifierSources.register();
        }

        ParticleProviderRegistry.getInstance().register(ALObjects.Particles.APOTH_CRIT, ApothCritProvider::new);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return ApothicAttributes.loc("al_config");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ALConfig.makeReloader().onResourceManagerReload(resourceManager);
            }
        });

        ItemTooltipCallback.EVENT.register((stack, context, flag, tooltips) -> {
            if (!ALConfig.enablePotionTooltips) return;
            if (stack.getItem() instanceof PotionItem) {
                List<MobEffectInstance> effects = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).customEffects();
                if (effects.size() == 1 && tooltips.size() >= 2) {
                    MobEffect effect = effects.get(0).getEffect().value();
                    String key = effect.getDescriptionId() + ".desc";
                    if (I18n.exists(key)) {
                        tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
                    }
                    else if (flag.isAdvanced() && effect.attributeModifiers.isEmpty()) {
                        tooltips.add(2, Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                    }
                }
            }
        });
    }

    /**
     * Upstream's {@code addAttribComponent} (ScreenEvent.Init.Post) and {@code forwardScroll} (ScreenEvent.MouseScrolled.Pre),
     * plus the mouse input a child widget would get: clicks and drags on the open panel go to it and stop there, so a click
     * on the panel never counts as a click outside the inventory (which drops the carried item).
     */
    private static void addAttribComponent(Minecraft mc, Screen screen, int width, int height) {
        if (!(screen instanceof InventoryScreen scn)) {
            activeAttribGui = null;
            return;
        }
        if (!ALConfig.enableAttributesGui) return;
        AttributesGui atrComp = new AttributesGui(scn);
        List<AbstractWidget> widgets = Screens.getWidgets(scn);
        widgets.add(atrComp.toggleBtn);
        widgets.add(atrComp.hideUnchangedBtn);
        if (AttributesGui.wasOpen || AttributesGui.swappedFromCurios) atrComp.toggleVisibility();
        AttributesGui.swappedFromCurios = false;
        activeAttribGui = atrComp;

        ScreenEvents.afterExtract(scn).register((s, gfx, mouseX, mouseY, partialTicks) -> atrComp.extractRenderState(gfx, mouseX, mouseY, partialTicks));
        ScreenMouseEvents.allowMouseClick(scn).register((s, event) -> {
            if (atrComp.mouseClicked(event, false)) return false;
            return !atrComp.isMouseOver(event.x(), event.y());
        });
        ScreenMouseEvents.allowMouseDrag(scn).register((s, event, dx, dy) -> !atrComp.mouseDragged(event, dx, dy));
        ScreenMouseEvents.allowMouseRelease(scn).register((s, event) -> {
            atrComp.scrolling = false;
            return true;
        });
        ScreenMouseEvents.allowMouseScroll(scn).register((s, mouseX, mouseY, scrollX, scrollY) -> {
            return !(atrComp.isMouseOver(mouseX, mouseY) && atrComp.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
        });
        ScreenEvents.remove(scn).register(s -> {
            if (activeAttribGui == atrComp) activeAttribGui = null;
        });
    }

    /**
     * @return The Attributes GUI of the open inventory screen, or null.
     */
    @Nullable
    public static AttributesGui getActiveAttribGui() {
        return activeAttribGui;
    }

    /**
     * Upstream's {@code effectGuiTooltips} (GatherEffectScreenTooltipsEvent): the inventory's effect tooltip becomes
     * "Name (duration)", plus the effect's id with advanced tooltips, its description and its attribute modifiers.
     */
    public static List<Component> effectTooltip(MobEffectInstance effectInst, Component nameIn, Component durationIn) {
        List<Component> tooltips = new ArrayList<>();
        Holder<MobEffect> effect = effectInst.getEffect();

        MutableComponent name = nameIn.copy();
        name.append(" ").append(Component.translatable("(%s)", durationIn).withStyle(ChatFormatting.WHITE));

        if (ApothicAttributes.getTooltipFlag().isAdvanced()) {
            name.append(" ").append(Component.translatable("[%s]", effect.unwrapKey().get().identifier().toString()).withStyle(ChatFormatting.GRAY));
        }
        tooltips.add(name);

        String key = effect.value().getDescriptionId() + ".desc";
        if (I18n.exists(key)) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY));
        }
        else if (ApothicAttributes.getTooltipFlag().isAdvanced() && effect.value().attributeModifiers.isEmpty()) {
            tooltips.add(Component.translatable(key).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        effect.value().createModifiers(effectInst.getAmplifier(),
            (attr, modif) -> tooltips.add(AttributeDisplay.toComponent(attr.value(), modif, ApothicAttributes.getTooltipFlag())));
        return tooltips;
    }

    private static final SuggestionProvider<FabricClientCommandSource> SUGGEST_ANCHOR_POINT = (ctx, builder) -> SharedSuggestionProvider.suggest(
        Arrays.stream(AnchorPoint.values()).map(AnchorPoint::getSerializedName), builder);

    /**
     * Upstream's client command: {@code /apothic_attributes_client set_btn_pos <anchor> [<x> <y>]} moves the Attributes
     * GUI button and saves the position to the config.
     */
    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommands.literal("apothic_attributes_client")
                .then(ClientCommands.literal("set_btn_pos")
                    .then(ClientCommands.argument("anchor", StringArgumentType.string()).suggests(SUGGEST_ANCHOR_POINT)
                        .executes(c -> {
                            updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), 0, 0);
                            return 0;
                        })
                        .then(ClientCommands.argument("x", IntegerArgumentType.integer(-1000, 1000))
                            .then(ClientCommands.argument("y", IntegerArgumentType.integer(-1000, 1000))
                                .executes(c -> {
                                    updateHudPos(AnchorPoint.parse(c.getArgument("anchor", String.class)), c.getArgument("x", Integer.class), c.getArgument("y", Integer.class));
                                    return 0;
                                }))))));
    }

    private static void updateHudPos(AnchorPoint anchor, int x, int y) {
        Configuration cfg = ALConfig.load();
        ALConfig.attributesGuiButtonOffset = new Offset(anchor, x, y);
        Offset.save("GUI Button Offset", "client", ALConfig.attributesGuiButtonOffset, cfg);
    }

    public static void apothCrit(int entityId) {
        Entity entity = Minecraft.getInstance().level.getEntity(entityId);
        if (entity != null) {
            Minecraft.getInstance().particleEngine.createTrackingEmitter(entity, ALObjects.Particles.APOTH_CRIT);
        }
    }

    public static class ApothCritParticle extends CritParticle {

        public ApothCritParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, TextureAtlasSprite sprite) {
            super(level, x, y, z, xa, ya, za, sprite);
            this.bCol = 1F;
            this.rCol = 0.3F;
            this.gCol = 0.8F;
        }
    }

    public static class ApothCritProvider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprite;

        public ApothCritProvider(SpriteSet sprite) {
            this.sprite = sprite;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xa, double ya, double za, RandomSource random) {
            return new ApothCritParticle(level, x, y, z, xa, ya, za, this.sprite.get(random));
        }
    }
}
