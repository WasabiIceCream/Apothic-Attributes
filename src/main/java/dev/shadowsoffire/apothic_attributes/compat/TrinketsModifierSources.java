package dev.shadowsoffire.apothic_attributes.compat;

import java.util.Comparator;
import java.util.function.BiConsumer;

import dev.shadowsoffire.apothic_attributes.client.ModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSource.ItemModifierSource;
import dev.shadowsoffire.apothic_attributes.client.ModifierSourceType;
import dev.shadowsoffire.apothic_attributes.util.Comparators;
import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import eu.pb4.trinkets.impl.TrinketUtilities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

/**
 * Port replacement for upstream's Curios compat in the Attributes GUI: items in Trinkets slots are listed as the
 * source of the modifiers Trinkets applies for them, drawn as the item's icon.
 * <p>
 * Only loaded when Trinkets is (see {@code AttributesLibClient}). The modifiers come from
 * {@link TrinketUtilities#forEachModifier}, the same call Trinkets 4.x (Trinkets Updated) uses when it applies them
 * (the item's {@code trinkets:attribute_modifiers} component, the item's trinket callback, the modifier event and
 * enchantment attribute effects, each with its per-slot id), skipping the slots Trinkets skips
 * ({@code TrinketSlotAccess#canApplyEffects}: cosmetic slots, broken items). {@code TrinketUtilities} is in Trinkets'
 * impl package: re-check it when Trinkets updates.
 */
public final class TrinketsModifierSources {

    public static final ModifierSourceType<ItemStack> TRINKETS = new ModifierSourceType<>(){

        @Override
        public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> map) {
            TrinketAttachment attachment = TrinketsApi.getAttachment(entity);
            if (attachment == null) return;
            attachment.forEach((access, stack) -> {
                if (stack.isEmpty() || !access.canApplyEffects(stack)) return;
                ItemModifierSource source = new TrinketModifierSource(stack);
                TrinketUtilities.forEachModifier(entity, stack, access, (attr, modif) -> map.accept(modif, source));
            });
        }

        @Override
        public int getPriority() {
            return 50; // Below worn equipment (0), above effects (100).
        }

    };

    public static void register() {
        ModifierSourceType.register(TRINKETS);
    }

    private static class TrinketModifierSource extends ItemModifierSource {

        TrinketModifierSource(ItemStack data) {
            super(TRINKETS, Comparator.comparing(ItemStack::getItem, Comparators.idComparator(BuiltInRegistries.ITEM)), data);
        }
    }
}
