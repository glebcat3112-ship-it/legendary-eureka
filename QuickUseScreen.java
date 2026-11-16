package com.example.quickuse;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class QuickUseScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int MAX_ROWS = 7;

    private int listening = -1; // индекс записи, для которой ждём нажатие клавиши
    private int scroll = 0;

    public QuickUseScreen() {
        super(Text.literal("QuickUse"));
    }

    @Override
    protected void init() {
        List<QuickUseConfig.Entry> list = QuickUseConfig.entries;
        scroll = MathHelper.clamp(scroll, 0, Math.max(0, list.size() - MAX_ROWS));

        int x0 = width / 2 - 203;
        int y0 = 44;

        for (int r = 0; r < MAX_ROWS; r++) {
            int idx = scroll + r;
            if (idx >= list.size()) break;
            QuickUseConfig.Entry e = list.get(idx);
            int y = y0 + r * ROW_H;

            TextFieldWidget itemField = new TextFieldWidget(textRenderer, x0, y, 130, 20, Text.empty());
            itemField.setMaxLength(100);
            itemField.setText(e.item == null ? "" : e.item);
            itemField.setChangedListener(s -> e.item = s);
            addDrawableChild(itemField);

            addDrawableChild(ButtonWidget.builder(Text.literal("Из руки"), b -> {
                if (client != null && client.player != null) {
                    ItemStack st = client.player.getMainHandStack();
                    if (!st.isEmpty()) {
                        e.item = Registries.ITEM.getId(st.getItem()).toString();
                        rebuild();
                    }
                }
            }).dimensions(x0 + 134, y, 52, 20).build());

            Text keyText = listening == idx
                    ? Text.literal("> нажми <")
                    : Text.literal(keyName(e.key));
            addDrawableChild(ButtonWidget.builder(keyText, b -> {
                listening = idx;
                rebuild();
            }).dimensions(x0 + 190, y, 72, 20).build());

            TextFieldWidget nameField = new TextFieldWidget(textRenderer, x0 + 266, y, 116, 20, Text.empty());
            nameField.setMaxLength(60);
            nameField.setText(e.name == null ? "" : e.name);
            nameField.setSuggestion(e.name == null || e.name.isEmpty() ? "по умолчанию" : "");
            nameField.setChangedListener(s -> {
                e.name = s;
                nameField.setSuggestion(s.isEmpty() ? "по умолчанию" : "");
            });
            addDrawableChild(nameField);

            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                QuickUseConfig.entries.remove(idx);
                listening = -1;
                rebuild();
            }).dimensions(x0 + 386, y, 20, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("+ Добавить"), b -> {
            QuickUseConfig.entries.add(new QuickUseConfig.Entry());
            scroll = Math.max(0, QuickUseConfig.entries.size() - MAX_ROWS);
            rebuild();
        }).dimensions(width / 2 - 103, height - 28, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
                .dimensions(width / 2 + 3, height - 28, 100, 20).build());
    }

    private void rebuild() {
        clearChildren();
        init();
    }

    private static String keyName(int code) {
        if (code < 0) return "Не задана";
        return InputUtil.Type.KEYSYM.createFromCode(code).getLocalizedText().getString();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening >= 0) {
            if (listening < QuickUseConfig.entries.size()) {
                QuickUseConfig.entries.get(listening).key =
                        keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
            }
            listening = -1;
            QuickUseConfig.save();
            rebuild();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, QuickUseConfig.entries.size() - MAX_ROWS);
        int ns = MathHelper.clamp(scroll - (int) Math.signum(verticalAmount), 0, max);
        if (ns != scroll) {
            scroll = ns;
            rebuild();
        }
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // фон + виджеты

        ctx.drawCenteredTextWithShadow(textRenderer, "QuickUse", width / 2, 12, 0xFFFFFF);

        int x0 = width / 2 - 203;
        ctx.drawTextWithShadow(textRenderer, "Предмет (id)", x0, 32, 0xA0A0A0);
        ctx.drawTextWithShadow(textRenderer, "Клавиша", x0 + 192, 32, 0xA0A0A0);
        ctx.drawTextWithShadow(textRenderer, "Своё название", x0 + 268, 32, 0xA0A0A0);

        if (QuickUseConfig.entries.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Список пуст — нажми «+ Добавить»", width / 2, 80, 0x888888);
        }
        ctx.drawCenteredTextWithShadow(textRenderer,
                "Esc при выборе клавиши — сбросить. Колёсико — прокрутка.", width / 2, height - 42, 0x777777);
    }

    @Override
    public void close() {
        QuickUseConfig.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
