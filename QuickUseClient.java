package com.example.quickuse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public class QuickUseClient implements ClientModInitializer {
    public static KeyBinding openGui;
    private Set<Integer> lastDown = new HashSet<>();

    @Override
    public void onInitializeClient() {
        QuickUseConfig.load();
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.quickuse.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_DELETE, "key.categories.quickuse"));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        while (openGui.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new QuickUseScreen());
        }

        if (mc.player == null || mc.currentScreen != null) {
            lastDown.clear();
            return;
        }

        long window = mc.getWindow().getHandle();
        Set<Integer> now = new HashSet<>();
        for (QuickUseConfig.Entry e : QuickUseConfig.entries) {
            if (e.key < 0) continue;
            boolean down = InputUtil.isKeyPressed(window, e.key);
            if (down) {
                now.add(e.key);
                if (!lastDown.contains(e.key)) QuickUse.use(mc, e);
            }
        }
        lastDown = now;
    }
}
