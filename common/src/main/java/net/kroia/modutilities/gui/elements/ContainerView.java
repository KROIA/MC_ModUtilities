package net.kroia.modutilities.gui.elements;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.kroia.modutilities.gui.Gui;
import net.kroia.modutilities.gui.GuiTexture;
import net.kroia.modutilities.gui.elements.base.GuiElement;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ContainerView<T extends AbstractContainerMenu> extends GuiElement implements MenuAccess<T> {

    private static final float SNAPBACK_SPEED = 100.0F;
    private static final int QUICKDROP_DELAY = 500;
    public static final int SLOT_ITEM_BLIT_OFFSET = 100;
    private static final int HOVER_ITEM_BLIT_OFFSET = 200;
    protected int titleLabelX;
    protected int titleLabelY;
    protected int inventoryLabelX;
    protected int inventoryLabelY;
    protected final T menu;
    protected final Component playerInventoryTitle;
    protected final Component title;

    protected Slot hoveredSlot;

    private Slot clickedSlot;

    private Slot snapbackEnd;

    private Slot quickdropSlot;

    private Slot lastClickSlot;
    private boolean isSplittingStack;
    private ItemStack draggingItem;
    private int snapbackStartX;
    private int snapbackStartY;
    private long snapbackTime;
    private ItemStack snapbackItem;
    private long quickdropTime;
    protected final Set<Slot> quickCraftSlots;
    protected boolean isQuickCrafting;
    private int quickCraftingType;
    private int quickCraftingButton;
    private boolean skipNextRelease;
    private int quickCraftingRemainder;
    private long lastClickTime;
    private int lastClickButton;
    private boolean doubleclick;
    private ItemStack lastQuickMoved;
    protected int slotColor;

    protected Minecraft minecraft = Gui.getMinecraft();

    public final GuiTexture background_texture;

    private Runnable onCloseEvent;

    public ContainerView(T pMenu, Inventory pPlayerInventory, Component pTitle, GuiTexture pBackgroundTexture) {
        super(0, 0, pBackgroundTexture.getWidth(), pBackgroundTexture.getHeight());
        this.background_texture = pBackgroundTexture;
        this.title = pTitle;
        this.draggingItem = ItemStack.EMPTY;
        this.snapbackItem = ItemStack.EMPTY;
        this.quickCraftSlots = Sets.newHashSet();
        this.lastQuickMoved = ItemStack.EMPTY;
        this.slotColor = -2130706433;
        this.menu = pMenu;
        this.playerInventoryTitle = pPlayerInventory.getDisplayName();
        this.skipNextRelease = true;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = pBackgroundTexture.getHeight() - 94;
    }

    private boolean isSameInventory(Slot pSlot, Slot pOtherSlot) {
        return pSlot != null && pOtherSlot != null && pSlot.container == pOtherSlot.container;
    }
    private boolean isActiveAndMatches(KeyMapping key, int keySym, int scanCode) {
        if(key.matches(keySym, scanCode)) {
            return key.isDefault();
        }
        return false;
    }
    private boolean isActiveAndMatches(KeyMapping key, int mouseButton) {
        if(key.matchesMouse(mouseButton)) {
            return key.isDefault();
        }
        return false;
    }

    @Override
    public void init() {
        super.init();
    }
    @Override
    public void renderBackground() {
        drawTexture(background_texture, 0, 0);
    }

    @Override
    public void render() {
        int i = 0;
        int j = 0;
        RenderSystem.disableDepthTest();
        this.hoveredSlot = null;
        int pMouseX = getMouseX();
        int pMouseY = getMouseY();

        int j2;
        int k2;
        for(int k = 0; k < this.menu.slots.size(); ++k) {
            Slot slot = (Slot)this.menu.slots.get(k);
            if (slot.isActive()) {
                this.renderSlot(slot);
            }

            if (this.isHovering(slot, (double)pMouseX, (double)pMouseY) && slot.isActive()) {
                this.hoveredSlot = slot;
                j2 = slot.x;
                k2 = slot.y;
                renderSlotHighlight(j2, k2, 0, this.getSlotColor(k));
            }
        }

        this.renderLabels(pMouseX, pMouseY);
        ItemStack itemstack = this.draggingItem.isEmpty() ? this.menu.getCarried() : this.draggingItem;
        if (!itemstack.isEmpty()) {
            j2 = this.draggingItem.isEmpty() ? 8 : 16;
            String s = null;
            if (!this.draggingItem.isEmpty() && this.isSplittingStack) {
                itemstack = itemstack.copy();
                itemstack.setCount(Mth.ceil((float)itemstack.getCount() / 2.0F));
            } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                itemstack = itemstack.copy();
                itemstack.setCount(this.quickCraftingRemainder);
                if (itemstack.isEmpty()) {
                    s = ChatFormatting.YELLOW + "0";
                }
            }

            this.renderFloatingItem(itemstack, pMouseX - i - 8, pMouseY - j - j2, s);
        }

        if (!this.snapbackItem.isEmpty()) {
            float f = (float)(Util.getMillis() - this.snapbackTime) / 100.0F;
            if (f >= 1.0F) {
                f = 1.0F;
                this.snapbackItem = ItemStack.EMPTY;
            }

            j2 = this.snapbackEnd.x - this.snapbackStartX;
            k2 = this.snapbackEnd.y - this.snapbackStartY;
            int j1 = this.snapbackStartX + (int)((float)j2 * f);
            int k1 = this.snapbackStartY + (int)((float)k2 * f);
            this.renderFloatingItem(this.snapbackItem, j1, k1, (String)null);
        }
        RenderSystem.enableDepthTest();
        renderTooltip(pMouseX, pMouseY);
    }

    @Override
    protected void layoutChanged() {

    }

    public void setOnCloseEvent(Runnable pOnCloseEvent) {
        onCloseEvent = pOnCloseEvent;
    }

    public void renderSlotHighlight(int pX, int pY, int pBlitOffset) {
        renderSlotHighlight(pX, pY, pBlitOffset, -2130706433);
    }

    public void renderSlotHighlight(int p_281453_, int p_281915_, int p_283504_, int color) {
        drawRect(p_281453_, p_281915_, 16, 16, color);
    }

    protected void renderTooltip(int pX, int pY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack itemstack = this.hoveredSlot.getItem();
            drawTooltip(itemstack, pX, pY);
        }

    }

    private void renderFloatingItem(ItemStack pStack, int pX, int pY, String pText) {
        graphicsPosePush();
        graphicsTranslate(0.0F, 0.0F, 232.0F);
        drawItemWithDecoration(pStack, pX, pY);
        graphicsPosePop();
    }

    protected void renderLabels(int pMouseX, int pMouseY) {
        drawText(this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        drawText(this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);
    }

    //protected abstract void renderBg(GuiGraphics var1, float var2, int var3, int var4);

    private void renderSlot(Slot pSlot) {
        int i = pSlot.x;
        int j = pSlot.y;
        ItemStack itemstack = pSlot.getItem();
        boolean flag = false;
        boolean flag1 = pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && !this.isSplittingStack;
        ItemStack itemstack1 = this.menu.getCarried();
        String s = null;
        if (pSlot == this.clickedSlot && !this.draggingItem.isEmpty() && this.isSplittingStack && !itemstack.isEmpty()) {
            itemstack = itemstack.copy();
            itemstack.setCount(itemstack.getCount() / 2);
        } else if (this.isQuickCrafting && this.quickCraftSlots.contains(pSlot) && !itemstack1.isEmpty()) {
            if (this.quickCraftSlots.size() == 1) {
                return;
            }

            if (AbstractContainerMenu.canItemQuickReplace(pSlot, itemstack1, true) && this.menu.canDragTo(pSlot)) {
                itemstack = itemstack1.copy();
                flag = true;
                AbstractContainerMenu.getQuickCraftSlotCount(this.quickCraftSlots, this.quickCraftingType, itemstack, pSlot.getItem().isEmpty() ? 0 : pSlot.getItem().getCount());
                int k = Math.min(itemstack.getMaxStackSize(), pSlot.getMaxStackSize(itemstack));
                if (itemstack.getCount() > k) {
                    String var10000 = ChatFormatting.YELLOW.toString();
                    s = var10000 + k;
                    itemstack.setCount(k);
                }
            } else {
                this.quickCraftSlots.remove(pSlot);
                this.recalculateQuickCraftRemaining();
            }
        }

        graphicsPosePush();
        graphicsTranslate(0.0F, 0.0F, 100.0F);
        if (itemstack.isEmpty() && pSlot.isActive()) {
            Pair<ResourceLocation, ResourceLocation> pair = pSlot.getNoItemIcon();
            if (pair != null) {
                TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)minecraft.getTextureAtlas((ResourceLocation)pair.getFirst()).apply((ResourceLocation)pair.getSecond());
                drawTexture(textureatlassprite, i, j, 16, 16, 0);
                flag1 = true;
            }
        }

        if (!flag1) {
            if (flag) {
                drawRect(i, j, 16, 16, -2130706433);
            }
            drawItemWithDecoration(itemstack, i, j);
        }

        graphicsPosePop();
    }

    private void recalculateQuickCraftRemaining() {
        ItemStack itemstack = this.menu.getCarried();
        if (!itemstack.isEmpty() && this.isQuickCrafting) {
            if (this.quickCraftingType == 2) {
                this.quickCraftingRemainder = itemstack.getMaxStackSize();
            } else {
                this.quickCraftingRemainder = itemstack.getCount();

                ItemStack itemstack1;
                int i;
                for(Iterator var2 = this.quickCraftSlots.iterator(); var2.hasNext(); this.quickCraftingRemainder -= itemstack1.getCount() - i) {
                    Slot slot = (Slot)var2.next();
                    itemstack1 = itemstack.copy();
                    ItemStack itemstack2 = slot.getItem();
                    i = itemstack2.isEmpty() ? 0 : itemstack2.getCount();
                    AbstractContainerMenu.getQuickCraftSlotCount(this.quickCraftSlots, this.quickCraftingType, itemstack1, i);
                    int j = Math.min(itemstack1.getMaxStackSize(), slot.getMaxStackSize(itemstack1));
                    if (itemstack1.getCount() > j) {
                        itemstack1.setCount(j);
                    }
                }
            }
        }
    }

    private Slot findSlot(double pMouseX, double pMouseY) {
        for(int i = 0; i < this.menu.slots.size(); ++i) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot, pMouseX, pMouseY) && slot.isActive()) {
                return slot;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClickedOverElement(int pButton) {
        int pMouseX = getMouseX();
        int pMouseY = getMouseY();
        boolean flag = isActiveAndMatches(minecraft.options.keyPickItem, pButton);
        Slot slot = this.findSlot(pMouseX, pMouseY);
        long i = Util.getMillis();
        this.doubleclick = this.lastClickSlot == slot && i - this.lastClickTime < 250L && this.lastClickButton == pButton;
        this.skipNextRelease = false;
        if (pButton != 0 && pButton != 1 && !flag) {
            this.checkHotbarMouseClicked(pButton);
        } else {
            int j = 0;
            int k = 0;
            boolean flag1 = this.hasClickedOutside(pMouseX, pMouseY, j, k, pButton);
            if (slot != null) {
                flag1 = false;
            }

            int l = -1;
            if (slot != null) {
                l = slot.index;
            }

            if (flag1) {
                l = -999;
            }

            if (minecraft.options.touchscreen().get() && flag1 && this.menu.getCarried().isEmpty()) {
                this.onClose();
                return true;
            }

            if (l != -1) {
                if (minecraft.options.touchscreen().get()) {
                    if (slot != null && slot.hasItem()) {
                        this.clickedSlot = slot;
                        this.draggingItem = ItemStack.EMPTY;
                        this.isSplittingStack = pButton == 1;
                    } else {
                        this.clickedSlot = null;
                    }
                } else if (!this.isQuickCrafting) {
                    if (this.menu.getCarried().isEmpty()) {
                        if (isActiveAndMatches(minecraft.options.keyPickItem, pButton)) {
                            this.slotClicked(slot, l, pButton, ClickType.CLONE);
                        } else {
                            boolean flag2 = l != -999 && (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), 340) || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), 344));
                            ClickType clicktype = ClickType.PICKUP;
                            if (flag2) {
                                this.lastQuickMoved = slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                                clicktype = ClickType.QUICK_MOVE;
                            } else if (l == -999) {
                                clicktype = ClickType.THROW;
                            }

                            this.slotClicked(slot, l, pButton, clicktype);
                        }

                        this.skipNextRelease = true;
                    } else {
                        this.isQuickCrafting = true;
                        this.quickCraftingButton = pButton;
                        this.quickCraftSlots.clear();
                        if (pButton == 0) {
                            this.quickCraftingType = 0;
                        } else if (pButton == 1) {
                            this.quickCraftingType = 1;
                        } else if (isActiveAndMatches(minecraft.options.keyPickItem, pButton)) {
                            this.quickCraftingType = 2;
                        }
                    }
                }
            }
        }

        this.lastClickSlot = slot;
        this.lastClickTime = i;
        this.lastClickButton = pButton;
        return true;

    }

    private void checkHotbarMouseClicked(int pKeyCode) {
        if (this.hoveredSlot != null && this.menu.getCarried().isEmpty()) {
            if (minecraft.options.keySwapOffhand.matchesMouse(pKeyCode)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return;
            }

            for(int i = 0; i < 9; ++i) {
                if (minecraft.options.keyHotbarSlots[i].matchesMouse(pKeyCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                }
            }
        }

    }

    protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
        return pMouseX < (double)pGuiLeft || pMouseY < (double)pGuiTop || pMouseX >= (double)(pGuiLeft + background_texture.getWidth()) || pMouseY >= (double)(pGuiTop + background_texture.getHeight());
    }


    @Override
    public boolean mouseDragged(int pButton, double pDragX, double pDragY) {
        int pMouseX = getMouseX();
        int pMouseY = getMouseY();
        Slot slot = this.findSlot(pMouseX, pMouseY);
        ItemStack itemstack = this.menu.getCarried();
        if (this.clickedSlot != null && (Boolean)minecraft.options.touchscreen().get()) {
            if (pButton == 0 || pButton == 1) {
                if (this.draggingItem.isEmpty()) {
                    if (slot != this.clickedSlot && !this.clickedSlot.getItem().isEmpty()) {
                        this.draggingItem = this.clickedSlot.getItem().copy();
                    }
                } else if (this.draggingItem.getCount() > 1 && slot != null && AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false)) {
                    long i = Util.getMillis();
                    if (this.quickdropSlot == slot) {
                        if (i - this.quickdropTime > 500L) {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.slotClicked(slot, slot.index, 1, ClickType.PICKUP);
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, 0, ClickType.PICKUP);
                            this.quickdropTime = i + 750L;
                            this.draggingItem.shrink(1);
                        }
                    } else {
                        this.quickdropSlot = slot;
                        this.quickdropTime = i;
                    }
                }
            }
        } else if (this.isQuickCrafting && slot != null && !itemstack.isEmpty() && (itemstack.getCount() > this.quickCraftSlots.size() || this.quickCraftingType == 2) && AbstractContainerMenu.canItemQuickReplace(slot, itemstack, true) && slot.mayPlace(itemstack) && this.menu.canDragTo(slot)) {
            this.quickCraftSlots.add(slot);
            this.recalculateQuickCraftRemaining();
        }

        return true;
    }

    @Override
    public void mouseReleased(int pButton) {
        int pMouseX = getMouseX();
        int pMouseY = getMouseY();
        Slot slot = this.findSlot(pMouseX, pMouseY);
        int i = 0;
        int j = 0;
        boolean flag = this.hasClickedOutside(pMouseX, pMouseY, i, j, pButton);
        if (slot != null) {
            flag = false;
        }

        int k = -1;
        if (slot != null) {
            k = slot.index;
        }

        if (flag) {
            k = -999;
        }

        Slot slot1;
        Iterator var14;
        if (this.doubleclick && slot != null && pButton == 0 && this.menu.canTakeItemForPickAll(ItemStack.EMPTY, slot)) {
            if (Screen.hasShiftDown()) {
                if (!this.lastQuickMoved.isEmpty()) {
                    var14 = this.menu.slots.iterator();

                    while(var14.hasNext()) {
                        slot1 = (Slot)var14.next();
                        if (slot1 != null &&
                                slot1.mayPickup(minecraft.player) &&
                                slot1.hasItem() &&
                                isSameInventory(slot1, slot) &&
                                AbstractContainerMenu.canItemQuickReplace(slot1, this.lastQuickMoved, true)) {
                            this.slotClicked(slot1, slot1.index, pButton, ClickType.QUICK_MOVE);
                        }
                    }
                }
            } else {
                this.slotClicked(slot, k, pButton, ClickType.PICKUP_ALL);
            }

            this.doubleclick = false;
            this.lastClickTime = 0L;
        } else {
            if (this.isQuickCrafting && this.quickCraftingButton != pButton) {
                this.isQuickCrafting = false;
                this.quickCraftSlots.clear();
                this.skipNextRelease = true;
                //return true;
                return;
            }

            if (this.skipNextRelease) {
                this.skipNextRelease = false;
                //return true;
                return;
            }

            boolean flag1;
            if (this.clickedSlot != null && minecraft.options.touchscreen().get()) {
                if (pButton == 0 || pButton == 1) {
                    if (this.draggingItem.isEmpty() && slot != this.clickedSlot) {
                        this.draggingItem = this.clickedSlot.getItem();
                    }

                    flag1 = AbstractContainerMenu.canItemQuickReplace(slot, this.draggingItem, false);
                    if (k != -1 && !this.draggingItem.isEmpty() && flag1) {
                        this.slotClicked(this.clickedSlot, this.clickedSlot.index, pButton, ClickType.PICKUP);
                        this.slotClicked(slot, k, 0, ClickType.PICKUP);
                        if (this.menu.getCarried().isEmpty()) {
                            this.snapbackItem = ItemStack.EMPTY;
                        } else {
                            this.slotClicked(this.clickedSlot, this.clickedSlot.index, pButton, ClickType.PICKUP);
                            this.snapbackStartX = Mth.floor(pMouseX - (double)i);
                            this.snapbackStartY = Mth.floor(pMouseY - (double)j);
                            this.snapbackEnd = this.clickedSlot;
                            this.snapbackItem = this.draggingItem;
                            this.snapbackTime = Util.getMillis();
                        }
                    } else if (!this.draggingItem.isEmpty()) {
                        this.snapbackStartX = Mth.floor(pMouseX - (double)i);
                        this.snapbackStartY = Mth.floor(pMouseY - (double)j);
                        this.snapbackEnd = this.clickedSlot;
                        this.snapbackItem = this.draggingItem;
                        this.snapbackTime = Util.getMillis();
                    }

                    this.clearDraggingState();
                }
            } else if (this.isQuickCrafting && !this.quickCraftSlots.isEmpty()) {
                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(0, this.quickCraftingType), ClickType.QUICK_CRAFT);
                var14 = this.quickCraftSlots.iterator();

                while(var14.hasNext()) {
                    slot1 = (Slot)var14.next();
                    this.slotClicked(slot1, slot1.index, AbstractContainerMenu.getQuickcraftMask(1, this.quickCraftingType), ClickType.QUICK_CRAFT);
                }

                this.slotClicked(null, -999, AbstractContainerMenu.getQuickcraftMask(2, this.quickCraftingType), ClickType.QUICK_CRAFT);
            } else if (!this.menu.getCarried().isEmpty()) {
                if (isActiveAndMatches(minecraft.options.keyPickItem,pButton)) {
                    this.slotClicked(slot, k, pButton, ClickType.CLONE);
                } else {
                    flag1 = k != -999 && (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), 340) || InputConstants.isKeyDown(minecraft.getWindow().getWindow(), 344));
                    if (flag1) {
                        this.lastQuickMoved = slot != null && slot.hasItem() ? slot.getItem().copy() : ItemStack.EMPTY;
                    }

                    this.slotClicked(slot, k, pButton, flag1 ? ClickType.QUICK_MOVE : ClickType.PICKUP);
                }
            }
        }

        if (this.menu.getCarried().isEmpty()) {
            this.lastClickTime = 0L;
        }

        this.isQuickCrafting = false;
        //return true;
    }

    public void clearDraggingState() {
        this.draggingItem = ItemStack.EMPTY;
        this.clickedSlot = null;
    }

    private boolean isHovering(Slot pSlot, double pMouseX, double pMouseY) {
        return this.isHovering(pSlot.x, pSlot.y, 16, 16, pMouseX, pMouseY);
    }

    protected boolean isHovering(int pX, int pY, int pWidth, int pHeight, double pMouseX, double pMouseY) {
        int i = 0;
        int j = 0;
        pMouseX -= i;
        pMouseY -= j;
        return pMouseX >= (double)(pX - 1) && pMouseX < (double)(pX + pWidth + 1) && pMouseY >= (double)(pY - 1) && pMouseY < (double)(pY + pHeight + 1);
    }

    protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
        if (pSlot != null) {
            pSlotId = pSlot.index;
        }

        minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, pSlotId, pMouseButton, pType,
                minecraft.player);
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        } else if (isActiveAndMatches(this.minecraft.options.keyInventory, pKeyCode, pScanCode)) {
            if(getGui().getFocusedElement() == null)
                this.onClose();
            return true;
        } else {
            boolean handled = this.checkHotbarKeyPressed(pKeyCode, pScanCode);
            if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
                if (isActiveAndMatches(this.minecraft.options.keyPickItem, pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ClickType.CLONE);
                    handled = true;
                } else if (isActiveAndMatches(this.minecraft.options.keyDrop, pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, Screen.hasControlDown() ? 1 : 0, ClickType.THROW);
                    handled = true;
                }
            } else if (isActiveAndMatches(this.minecraft.options.keyDrop, pKeyCode, pScanCode)) {
                handled = true;
            }

            return handled;
        }
    }

    protected boolean checkHotbarKeyPressed(int pKeyCode, int pScanCode) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null) {
            if (isActiveAndMatches(this.minecraft.options.keySwapOffhand, pKeyCode, pScanCode)) {
                this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 40, ClickType.SWAP);
                return true;
            }

            for(int i = 0; i < 9; ++i) {
                if (isActiveAndMatches(this.minecraft.options.keyHotbarSlots[i],pKeyCode, pScanCode)) {
                    this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, i, ClickType.SWAP);
                    return true;
                }
            }
        }

        return false;
    }

    public void removed() {
        if (this.minecraft.player != null) {
            this.menu.removed(this.minecraft.player);
        }

    }

    public boolean isPauseScreen() {
        return false;
    }

    public final void tick() {
        //super.tick();
        if (this.minecraft.player.isAlive() && !this.minecraft.player.isRemoved()) {
            this.containerTick();
        } else {
            this.minecraft.player.closeContainer();
        }

    }

    protected void containerTick() {
    }

    public T getMenu() {
        return this.menu;
    }

    public @org.jetbrains.annotations.Nullable Slot getSlotUnderMouse() {
        return this.hoveredSlot;
    }

    public int getSlotColor(int index) {
        return this.slotColor;
    }

    public void onClose() {
        if(onCloseEvent != null)
            onCloseEvent.run();
        this.minecraft.player.closeContainer();
    }
}
