# Gui Container Screen

> **v2.0.1 package change:** `GuiContainerScreen` has moved from `net.kroia.modutilities.gui` to `net.kroia.modutilities.gui.client`. Update your imports accordingly.

The Gui Container Screen is a variant of the [Gui Screen](GuiScreen.md).
All features providec by the [Gui Screen](GuiScreen.md) will work in this too.
Visit the [Gui Screen](GuiScreen.md) to check out further informations what the GuiScreen can do.


---
## Content
- [Example implementation](#example-implementation)
- [Open a container screen](#open-a-container-screen)

---
## Example implementation
Instead of extending the `AbstractContainerScreen<MenuType>` the screen extends from `GuiContainerScreen<MenuType>`. 

``` Java
public class TestScreen extends GuiContainerScreen<MyContainerMenu> {
    private final ContainerView<MyContainerMenu> inventoryView;
    public TestScreen(MyContainerMenu pMenu, Inventory pPlayerInventory, Component pTitle)
    {
        super(pMenu, pPlayerInventory, pTitle);

        // Create the visual and interactive gui element for the container.
        inventoryView = new ContainerView<>(pMenu, pPlayerInventory, Component.literal("Inventory"), new GuiTexture(ModUtilitiesMod.MOD_ID,
                "textures/gui/inventory_hpc.png", 256, 256));
        inventoryView.setSize(176, 166);
        addElement(inventoryView);
    }

    @Override
    protected void updateLayout(Gui gui) {

    }
}
```

---
## Open a container screen
Since it works just like the normal Architectury container screen opening, it will not be described here.



