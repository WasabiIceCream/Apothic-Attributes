package dev.shadowsoffire.apothic_attributes.util;

import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.damagesource.DamageSource;

public class AttributesUtil {

    public static boolean isPhysicalDamage(DamageSource src) {
        return src.is(ALObjects.Tags.IS_PHYSICAL) && !src.is(ALObjects.Tags.IS_NON_PHYSICAL);
    }

}
