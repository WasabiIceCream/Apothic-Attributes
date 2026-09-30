package dev.shadowsoffire.apothic_attributes.client;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Comparator;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.apothic_attributes.api.BooleanAttribute;
import dev.shadowsoffire.apothic_attributes.api.PercentageAttribute;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.TooltipFlag;

/**
 * Fabric stand-in for the display half of NeoForge's {@code IAttributeExtension} ({@code toValueComponent},
 * {@code toComponent}, {@code FORMAT}) and {@code AttributeUtil.ATTRIBUTE_MODIFIER_COMPARATOR}, which the Attributes GUI
 * uses. Same rules: flat values print as numbers, percentage attributes and multiplier operations as percentages,
 * boolean attributes as on/off. Uses vanilla's translation keys (NeoForge's own keys don't exist on Fabric).
 */
public final class AttributeDisplay {

    public static final DecimalFormat FORMAT = Util.make(new DecimalFormat("#.##"), fmt -> fmt.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

    /**
     * Sorts modifiers by operation, then largest amount first, then id.
     */
    public static final Comparator<AttributeModifier> MODIFIER_COMPARATOR = Comparator.comparing(AttributeModifier::operation)
        .thenComparing(Comparator.comparingDouble(AttributeModifier::amount).reversed())
        .thenComparing(AttributeModifier::id);

    private AttributeDisplay() {}

    private static boolean isNullOrAddition(@Nullable Operation op) {
        return op == null || op == Operation.ADD_VALUE;
    }

    /**
     * The value of an attribute (op null) or of a modifier's amount for the given operation.
     */
    public static MutableComponent toValueComponent(Attribute attr, @Nullable Operation op, double value, TooltipFlag flag) {
        if (attr instanceof BooleanAttribute && op == null) {
            return Component.translatable(value > 0 ? "options.on" : "options.off");
        }
        if (isNullOrAddition(op) && !(attr instanceof PercentageAttribute)) {
            return Component.literal(FORMAT.format(value));
        }
        return Component.literal(FORMAT.format(value * 100) + "%");
    }

    /**
     * A modifier line, e.g. "+5% Crit Chance", colored by the attribute's sentiment.
     */
    public static MutableComponent toComponent(Attribute attr, AttributeModifier modif, TooltipFlag flag) {
        double value = modif.amount();
        boolean positive = value > 0;
        Component valueComp = toValueComponent(attr, modif.operation(), Math.abs(value), flag);
        Component name = Component.translatable(attr.getDescriptionId());
        MutableComponent comp;
        if (attr instanceof BooleanAttribute && modif.operation() == Operation.ADD_VALUE) {
            comp = Component.translatable("%s %s", Component.translatable(positive ? "options.on" : "options.off"), name);
        }
        else {
            comp = Component.translatable(positive ? "attribute.modifier.plus.0" : "attribute.modifier.take.0", valueComp, name);
        }
        comp.withStyle(attr.getStyle(positive));
        if (flag.isAdvanced()) {
            comp.append(Component.literal(" [" + modif.id() + "]").withStyle(ChatFormatting.DARK_GRAY));
        }
        return comp;
    }
}
