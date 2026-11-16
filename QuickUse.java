package com.example.quickuse;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class QuickUse {
    private static final String S = "\u00a7";

    public static void use(MinecraftClient mc, QuickUseConfig.Entry e) {
        ClientPlayerEntity p = mc.player;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (p == null || im == null) return;

        Identifier id = Identifier.tryParse(e.item == null ? "" : e.item.trim().toLowerCase());
        Item item = id == null ? null : Registries.ITEM.getOrEmpty(id).orElse(null);
        if (item == null) {
            chat(p, S + "c" + "Неизвестный предмет: " + S + "f" + e.item);
            return;
        }

        String display = (e.name != null && !e.name.isBlank())
                ? e.name
                : new ItemStack(item).getName().getString();

        // 1) предмет в левой руке
        if (p.getOffHandStack().isOf(item)) {
            swing(p, im.interactItem(p, Hand.OFF_HAND), Hand.OFF_HAND);
            chat(p, S + "7[QuickUse] " + S + "fИспользован: " + S + "a" + display);
            return;
        }

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(item)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            chat(p, S + "7[QuickUse] " + S + "cНе найден: " + S + "f" + display);
            return;
        }

        int sel = inv.selectedSlot;

        if (slot < 9) {
            // 2) предмет в хотбаре: переключаемся, кликаем, возвращаемся
            if (slot != sel) {
                inv.selectedSlot = slot;
                p.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
            swing(p, im.interactItem(p, Hand.MAIN_HAND), Hand.MAIN_HAND);
            if (slot != sel) {
                inv.selectedSlot = sel;
                p.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(sel));
            }
        } else {
            // 3) предмет в инвентаре: меняем его местами с предметом в активном слоте,
            //    кликаем ПКМ и меняем обратно (slot 9..35 совпадает с id слота в окне игрока)
            int syncId = p.playerScreenHandler.syncId;
            im.clickSlot(syncId, slot, sel, SlotActionType.SWAP, p);
            swing(p, im.interactItem(p, Hand.MAIN_HAND), Hand.MAIN_HAND);
            im.clickSlot(syncId, slot, sel, SlotActionType.SWAP, p);
        }

        chat(p, S + "7[QuickUse] " + S + "fИспользован: " + S + "a" + display);
    }

    private static void swing(ClientPlayerEntity p, ActionResult r, Hand hand) {
        if (r.isAccepted()) p.swingHand(hand);
    }

    /** Сообщение только в твоём локальном чате (на сервер не отправляется). */
    private static void chat(ClientPlayerEntity p, String msg) {
        p.sendMessage(Text.literal(msg), false);
    }
}
