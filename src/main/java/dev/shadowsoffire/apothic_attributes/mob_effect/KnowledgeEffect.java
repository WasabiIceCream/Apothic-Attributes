package dev.shadowsoffire.apothic_attributes.mob_effect;

import java.util.function.BiConsumer;

import dev.shadowsoffire.apothic_attributes.ALConfig;
import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public class KnowledgeEffect extends MobEffect {

    private static final Identifier ID = ApothicAttributes.loc("ancient_knowledge");

    public KnowledgeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xF4EE42);
        // Registered for removal and for anything that reads the template; the amount actually applied is computed below.
        this.addAttributeModifier(ALObjects.Attributes.EXPERIENCE_GAINED, ID, ALConfig.knowledgeMultiplier, Operation.ADD_MULTIPLIED_TOTAL);
    }

    /**
     * Port note: upstream's NeoForge modifier function reads {@link ALConfig#knowledgeMultiplier} each time the effect is
     * applied (it is synced from the server). Vanilla templates hold a fixed amount, so the modifier is built here.
     */
    private static AttributeModifier create(int amplifier) {
        return new AttributeModifier(ID, ALConfig.knowledgeMultiplier * (amplifier + 1), Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> output) {
        output.accept(ALObjects.Attributes.EXPERIENCE_GAINED, create(amplifier));
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        AttributeInstance inst = attributes.getInstance(ALObjects.Attributes.EXPERIENCE_GAINED);
        if (inst != null) {
            inst.removeModifier(ID);
            inst.addPermanentModifier(create(amplifier));
        }
    }

}
