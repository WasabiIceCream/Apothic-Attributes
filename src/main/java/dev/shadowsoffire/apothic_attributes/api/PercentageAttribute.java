package dev.shadowsoffire.apothic_attributes.api;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Fabric stand-in for NeoForge's {@code PercentageAttribute}: a ranged attribute whose value reads as a
 * percentage (1.0 = 100%). NeoForge only changes how it's displayed; gameplay is identical. Tooltip
 * formatting is left to the display mixins.
 */
public class PercentageAttribute extends RangedAttribute {

    public PercentageAttribute(String descriptionId, double defaultValue, double min, double max) {
        super(descriptionId, defaultValue, min, max);
    }
}
