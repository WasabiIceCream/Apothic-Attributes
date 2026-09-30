package dev.shadowsoffire.apothic_attributes.compat;

import java.util.List;

import dev.shadowsoffire.apothic_attributes.ApothicAttributes;
import dev.shadowsoffire.apothic_attributes.client.AttributesGui;
import dev.shadowsoffire.apothic_attributes.client.AttributesLibClient;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/**
 * Registers the exclusion zones for the Attributes GUI, which prevents the JEI overlay (and the overlays of
 * recipe viewers that consume JEI plugins through a bridge, such as EMI) from rendering below it.
 */
/* Fabric: registered as a jei_mod_plugin entrypoint in fabric.mod.json (JEI ignores @JeiPlugin there). */
@JeiPlugin
public class AttributesJEIPlugin implements IModPlugin {

    @Override
    public Identifier getPluginUid() {
        return ApothicAttributes.loc("attributes_gui");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(InventoryScreen.class, new IGuiContainerHandler<InventoryScreen>(){
            @Override
            public List<Rect2i> getGuiExtraAreas(InventoryScreen screen) {
                // Port note: the GUI isn't a child of the screen here (see AttributesGui), so it's looked up directly.
                AttributesGui gui = AttributesLibClient.getActiveAttribGui();
                return gui != null && gui.getParent() == screen ? gui.getExclusionAreas() : List.of();
            }
        });
    }

}
