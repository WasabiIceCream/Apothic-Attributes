package dev.shadowsoffire.apothic_attributes.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Upstream's percent-style tooltips: NeoForge formats an {@code add_value} modifier on a
 * {@link PercentageAttribute} as a percentage ("+5% Crit Chance" instead of "+0.05 Crit Chance").
 * Vanilla already formats {@code add_multiplied_base} that way, so the modifier is shown as one (display only).
 */
@Mixin(ItemAttributeModifiers.Display.Default.class)
public class PercentTooltipMixin {

    @ModifyVariable(method = "apply", at = @At("HEAD"), argsOnly = true)
    private AttributeModifier apoth_percent(AttributeModifier modifier, Consumer<Component> consumer, Player player, Holder<Attribute> attribute) {
        return PercentageAttribute.forDisplay(attribute, modifier);
    }
}
