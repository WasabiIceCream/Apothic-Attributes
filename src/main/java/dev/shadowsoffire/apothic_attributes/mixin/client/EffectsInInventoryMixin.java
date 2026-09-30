package dev.shadowsoffire.apothic_attributes.mixin.client;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import dev.shadowsoffire.apothic_attributes.client.AttributesLibClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * NeoForge's GatherEffectScreenTooltipsEvent: the tooltip of an effect in the inventory (vanilla's is its name and
 * duration) gets Apothic's description and modifier lines ({@link AttributesLibClient#effectTooltip}). Vanilla shows it
 * when the effect's name doesn't fit beside the inventory.
 */
@Mixin(EffectsInInventory.class)
public class EffectsInInventoryMixin {

    /** The effect whose text {@code extractText} is drawing. Rendering is single-threaded. */
    @Unique
    private static MobEffectInstance apoth_currentEffect;

    @WrapOperation(method = "extractEffects", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/EffectsInInventory;extractText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/Font;IIIIII)V"))
    private void apoth_trackEffect(EffectsInInventory self, GuiGraphicsExtractor gfx, Component name, Component duration, Font font, int a, int b, int c, int d, int e, int f,
        Operation<Void> original, @Local MobEffectInstance effect) {
        apoth_currentEffect = effect;
        try {
            original.call(self, gfx, name, duration, font, a, b, c, d, e, f);
        }
        finally {
            apoth_currentEffect = null;
        }
    }

    @WrapOperation(method = "extractText", at = @At(value = "INVOKE", target = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;"))
    private List<Object> apoth_effectTooltip(Object name, Object duration, Operation<List<Object>> original) {
        MobEffectInstance effect = apoth_currentEffect;
        if (effect != null && name instanceof Component n && duration instanceof Component d) {
            return List.copyOf(AttributesLibClient.effectTooltip(effect, n, d));
        }
        return original.call(name, duration);
    }
}
