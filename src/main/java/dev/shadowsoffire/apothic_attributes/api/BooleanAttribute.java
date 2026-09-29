package dev.shadowsoffire.apothic_attributes.api;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;

/**
 * Fabric stand-in for NeoForge's {@code BooleanAttribute}: on when the value is above zero. Stored as a
 * ranged attribute from 0 to 1, which is what NeoForge's version does underneath.
 */
public class BooleanAttribute extends RangedAttribute {

    public BooleanAttribute(String descriptionId, boolean defaultValue) {
        super(descriptionId, defaultValue ? 1 : 0, 0, 1);
    }
}
