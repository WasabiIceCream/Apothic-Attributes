package dev.shadowsoffire.apothic_attributes.mob_effect;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class KnowledgeEffect extends MobEffect {

    public KnowledgeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4EE42);
        // Port note: vanilla's per-level modifier is amount * (level + 1), the same as upstream's NeoForge function; the multiplier is read once at registration.
        this.addAttributeModifier(ALObjects.Attributes.EXPERIENCE_GAINED, ApothicAttributes.loc("ancient_knowledge"), ALConfig.knowledgeMultiplier, Operation.ADD_MULTIPLIED_TOTAL);
    }

}
