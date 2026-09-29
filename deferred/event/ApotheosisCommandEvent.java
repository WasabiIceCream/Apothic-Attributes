package dev.shadowsoffire.apothic_attributes.event;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@link ApotheosisCommandEvent} is fired during the {@link RegisterCommandsEvent} to allow registration of children of the /apoth command.
 */
public class ApotheosisCommandEvent extends Event {

    private final LiteralArgumentBuilder<CommandSourceStack> root;
    private final CommandBuildContext ctx;

    public ApotheosisCommandEvent(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext ctx) {
        this.root = root;
        this.ctx = ctx;
    }

    public LiteralArgumentBuilder<CommandSourceStack> getRoot() {
        return this.root;
    }

    public CommandBuildContext getContext() {
        return ctx;
    }
}
