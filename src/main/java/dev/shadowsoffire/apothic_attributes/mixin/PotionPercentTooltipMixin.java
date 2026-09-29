package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.function.BiConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * The potion tooltip's "When Applied" lines, formatted like {@link PercentTooltipMixin}.
 */
@Mixin(PotionContents.class)
public class PotionPercentTooltipMixin {

    @WrapOperation(method = "addPotionTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;createModifiers(ILjava/util/function/BiConsumer;)V"))
    private static void apoth_percent(MobEffect effect, int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, Operation<Void> original) {
        original.call(effect, amplifier, (BiConsumer<Holder<Attribute>, AttributeModifier>) (attr, mod) -> consumer.accept(attr, PercentageAttribute.forDisplay(attr, mod)));
    }
}
