package net.shadowmage.ancientwarfare.core.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.shadowmage.ancientwarfare.core.interfaces.IContainerGuiCallback;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.network.PacketGui;
import net.shadowmage.ancientwarfare.core.util.InventoryTools;

import javax.annotation.Nonnull;

public class ContainerBase extends Container {

    public final EntityPlayer player;
    private IContainerGuiCallback gui;
    public int playerSlots = 0;

    public ContainerBase(EntityPlayer player) {
        this.player = player;
    }

    /*
     * set the gui for this container for callback (refreshGui) purposes
     */
    public final void setGui(IContainerGuiCallback gui) {
        this.gui = gui;
    }

    /*
     * @param tx  the upper-left X coordinate of the 9x3 inventory block
     * @param ty  the upper-left Y coordinate of the 9x3 inventory block
     * @param gap the gap size between upper (9x3) and lower(9x1) inventory blocks, in pixels
     */
    protected int addPlayerSlots(int tx, int ty, int gap) {
        int y;
        int x;
        int slotNum;
        int xPos;
        int yPos;
        for (x = 0; x < 9; ++x)//add player hotbar slots
        {
            xPos = tx + x * 18;
            yPos = ty + gap + 3 * 18;
            this.addSlotToContainer(new Slot(player.inventory, x, xPos, yPos));
        }
        for (y = 0; y < 3; ++y) {
            for (x = 0; x < 9; ++x) {
                slotNum = y * 9 + x + 9;// +9 is to increment past hotbar slots
                xPos = tx + x * 18;
                yPos = ty + y * 18;
                this.addSlotToContainer(new Slot(player.inventory, slotNum, xPos, yPos));
            }
        }
        playerSlots = 36;
        return ty + (4 * 18) + gap;
    }

    protected int addPlayerSlots(int ty) {
        return addPlayerSlots(8, ty, 4);
    }

    protected int addPlayerSlots() {
        return addPlayerSlots(8, 240 - 4 - 8 - 4 * 18, 4);
    }

    /*
     * server side method to send a data-packet to the client-side GUI attached to the client-side version of this container
     */
    protected final void sendDataToGui(NBTTagCompound data) {
        if (!player.world.isRemote) {
            PacketGui pkt = new PacketGui();
            pkt.setTag("gui", data);
            NetworkHandler.sendToPlayer((EntityPlayerMP) player, pkt);
        }
    }

    /*
     * send data from client-container to server container
     */
    protected void sendDataToServer(NBTTagCompound data) {
        if (player.world.isRemote) {
            PacketGui pkt = new PacketGui();
            pkt.setData(data);
            NetworkHandler.sendToServer(pkt);
        }
    }

    /*
     * send data from server container to client container
     */
    protected void sendDataToClient(NBTTagCompound data) {
        if (!player.world.isRemote) {
            PacketGui pkt = new PacketGui();
            pkt.setData(data);
            NetworkHandler.sendToPlayer((EntityPlayerMP) player, pkt);
        }
    }

    /*
     * client/server side method to receive packet data from PacketGui
     */
    public final void onPacketData(NBTTagCompound data) {
        if (data.hasKey("gui")) {
            if (this.gui != null) {
                this.gui.handlePacketData(data.getCompoundTag("gui"));
            }
        } else {
            handlePacketData(data);
        }
    }

    /*
     * subclasses should override this method to send any data from server to the client-side container.
     * This method is called immediately after the container has been constructed and set as the active container.
     * The data is received client-side immediately after the GUI has been constructed, initialized, and opened.
     */
    public void sendInitData() {

    }

    /*
     * sub-classes should override this method to handle any packet data they are expecting to receive.
     * packets destined to the GUI or for slot-click have already been filtered out
     */
    public void handlePacketData(NBTTagCompound tag) {

    }

    @Override
    public boolean canInteractWith(EntityPlayer var1) {
        return true;
    }

    /*
     * Causes the GUI to be re-setup on its next update tick
     */
    public void refreshGui() {
        if (this.gui != null) {
            this.gui.refreshGui();
        }
    }

    /*
     * remove the inventory slots from view on the screen, effectively disabling them
     */

    public void removeSlots() {
        for (Slot s : this.inventorySlots) {
            if (s.yPos >= 0) {
                s.yPos -= 10000;
            }
        }
    }

    /*
     * add any removed from screen slots back into view
     */

    public void addSlots() {
        for (Slot s : this.inventorySlots) {
            if (s.yPos < 0) {
                s.yPos += 10000;
            }
        }
    }

    /*
     * override default CRASH with default DO NOTHING
     */
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotClickedIndex) {
        return ItemStack.EMPTY;
    }

    /*
     * merges provided ItemStack with the first available one in the container/player inventory<br>
     * overriden to clean up the mess of the code that was the vanilla code.
     *
     * @return true if item-stack was fully-consumed/merged
     */
    @Override
    protected boolean mergeItemStack(ItemStack incomingStack, int startIndex, int endBeforeIndex, boolean iterateBackwards) {
        Slot slotFromContainer;
        @Nonnull ItemStack stackFromSlot;
        int currentIndex, start, stop, transferAmount;
        int iterator = iterateBackwards ? -1 : 1;
        start = iterateBackwards ? endBeforeIndex : startIndex;
        stop = iterateBackwards ? startIndex : endBeforeIndex;
        if (incomingStack.isStackable()) {
            for (currentIndex = start; incomingStack.getCount() > 0 && currentIndex != stop; currentIndex += iterator) {
                slotFromContainer = this.getSlot(currentIndex);
                if (!slotFromContainer.isItemValid(incomingStack)) {
                    continue;
                }
                stackFromSlot = slotFromContainer.getStack();
                if (stackFromSlot.isEmpty() || !InventoryTools.doItemStacksMatch(incomingStack, stackFromSlot, !incomingStack.getHasSubtypes(), false)) {
                    continue;
                }
                transferAmount = stackFromSlot.getMaxStackSize() - stackFromSlot.getCount();
                if (transferAmount > incomingStack.getCount()) {
                    transferAmount = incomingStack.getCount();
                }
                if (transferAmount > 0) {
                    incomingStack.shrink(transferAmount);
                    stackFromSlot.grow(transferAmount);
                    slotFromContainer.onSlotChanged();
                }
            }
        }
        if (incomingStack.getCount() > 0) {
            for (currentIndex = start; incomingStack.getCount() > 0 && currentIndex != stop; currentIndex += iterator) {
                slotFromContainer = this.getSlot(currentIndex);
                if (!slotFromContainer.isItemValid(incomingStack)) {
                    continue;
                }
                if (!slotFromContainer.getHasStack()) {
                    slotFromContainer.putStack(incomingStack.copy());
                    slotFromContainer.onSlotChanged();
                    incomingStack.setCount(0);
                    break;
                }
            }
        }
        return incomingStack.getCount() == 0;
    }

    protected EnumHand getHandHoldingItem(EntityPlayer player, Item item) {
        if (player.getHeldItemMainhand().getItem() == item) {
            return EnumHand.MAIN_HAND;
        }
        return EnumHand.OFF_HAND; //assume that gui only ever gets open if the player has the item in either hand
    }
}
