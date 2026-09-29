package dev.shadowsoffire.apothic_attributes;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import dev.shadowsoffire.apothic_attributes.api.CooldownTracker;
import dev.shadowsoffire.apothic_attributes.impl.AttributeEvents;
import dev.shadowsoffire.apothic_attributes.payload.ConfigPayload;
import dev.shadowsoffire.apothic_attributes.payload.CritParticlePayload;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;

/**
 * Fabric port of Apothic Attributes (NeoForge original by Shadows_of_Fire).
 * <p>
 * Port notes: NeoForge's event bus is replaced by Fabric API events and this mod's own mixins (see
 * {@link AttributeEvents}); {@code EntityAttributeModificationEvent} (adding the attributes to every living
 * entity) is {@code LivingEntityAttributesMixin}.
 */
public class ApothicAttributes implements ModInitializer {

    public static final String MODID = "apothic_attributes";
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static final DeferredHelper R = DeferredHelper.create(MODID);
    public static final boolean DEBUG_AUX_DMG = "on".equalsIgnoreCase(System.getenv("APOTH_DEBUG_AUX_DMG"));
    private static final File configDir = FabricLoader.getInstance().getConfigDir().resolve("apotheosis").toFile();

    /**
     * Static record of {@link Player#getAttackStrengthScale(float)} for use in damage events.<br>
     * Recorded when a player attacks (see {@link #recordAtkStrength}) and valid for the entire chain.
     */
    private static float localAtkStrength = 1;

    @Override
    public void onInitialize() {
        ALConfig.load(); // Before registration: the Knowledge effect reads its multiplier when created.
        ALObjects.bootstrap();
        AttributeEvents.register();

        PayloadHelper.registerPayload(new CritParticlePayload.Provider());
        PayloadHelper.registerPayload(new ConfigPayload.Provider());

        MobEffects.BLINDNESS.value().addAttributeModifier(Attributes.FOLLOW_RANGE, loc("blindness"), -0.75, Operation.ADD_MULTIPLIED_TOTAL);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> pruneCooldowns(handler.getPlayer()));
        // Upstream marks every player attribute syncable in common setup; every mod's attributes are registered by server start.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> markPlayerAttributesSyncable());
    }

    /**
     * Marks every attribute players have as syncable, so the client can display them.
     */
    public static void markPlayerAttributesSyncable() {
        AttributeSupplier playerAttribs = DefaultAttributes.getSupplier(EntityType.PLAYER);
        BuiltInRegistries.ATTRIBUTE.listElements().forEach(attr -> {
            if (playerAttribs.hasAttribute(attr)) {
                attr.value().setSyncable(true);
            }
        });
    }

    /**
     * Called at the start of {@link Player#attack(net.minecraft.world.entity.Entity)} (see {@code PlayerMixin}).
     */
    public static void recordAtkStrength(Player player) {
        localAtkStrength = player.getAttackStrengthScale(0.5F);
    }

    public static File getConfigFile(String path) {
        return new File(configDir, path + ".cfg");
    }

    /**
     * Gets the local attack strength of an entity.
     * <p>
     * For players, this is recorded when they attack and is valid for other damage events.
     * <p>
     * For non-players, this value is always 1.
     */
    public static float getLocalAtkStrength(Entity entity) {
        if (entity instanceof Player) {
            return localAtkStrength;
        }
        return 1;
    }

    /**
     * Gets the current tooltip flag.
     *
     * @return If called on the client, the current tooltip flag, otherwise {@link TooltipFlag#NORMAL}
     */
    public static TooltipFlag getTooltipFlag() {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientAccess.getTooltipFlag();
        }
        return TooltipFlag.NORMAL;
    }

    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link ApothicAttributes#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }

    private static class ClientAccess {
        static TooltipFlag getTooltipFlag() {
            return Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
        }
    }

    private static void pruneCooldowns(Player p) {
        CooldownTracker tracker = p.getAttachedOrCreate(ALObjects.Attachments.COOLDOWNS);
        if (tracker.prune(p.level().getGameTime())) {
            p.setAttached(ALObjects.Attachments.COOLDOWNS, tracker);
        }
    }
}
