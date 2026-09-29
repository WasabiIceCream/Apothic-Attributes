package dev.shadowsoffire.apothic_attributes.api;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Fabric stand-in for NeoForge's {@code PercentageAttribute}: a ranged attribute whose value reads as a
 * percentage (1.0 = 100%). NeoForge only changes how it's displayed; gameplay is identical. Tooltips
 * go through {@link #forDisplay} (see {@code PercentTooltipMixin}).
 */
public class PercentageAttribute extends RangedAttribute {

    public PercentageAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }

    /**
     * @return The modifier to display: an {@code add_value} modifier on a percentage attribute becomes an
     *         {@code add_multiplied_base} one of the same amount, which vanilla shows as a percentage.
     */
    public static AttributeModifier forDisplay(Holder<Attribute> attribute, AttributeModifier modifier) {
        if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE && attribute.value() instanceof PercentageAttribute) {
            return new AttributeModifier(modifier.id(), modifier.amount(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
        return modifier;
    }
}
