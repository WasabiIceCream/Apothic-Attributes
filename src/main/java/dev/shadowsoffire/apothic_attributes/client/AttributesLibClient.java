package dev.shadowsoffire.apothic_attributes.client;

import java.util.List;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CritParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
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
 * Client side of the port. Deferred with the Attributes GUI (see deferred/): the GUI itself, its client command,
 * and the effect-screen tooltips.
 */
public class AttributesLibClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
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
