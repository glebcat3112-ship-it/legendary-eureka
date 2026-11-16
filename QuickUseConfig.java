package com.example.quickuse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class QuickUseConfig {
    public static class Entry {
        /** id предмета, например minecraft:ender_pearl */
        public String item = "minecraft:ender_pearl";
        /** GLFW-код клавиши, -1 = не задана */
        public int key = -1;
        /** своё название для чата; пусто = обычное название предмета */
        public String name = "";
    }

    public static List<Entry> entries = new ArrayList<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("quickuse.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                List<Entry> l = GSON.fromJson(r, new TypeToken<List<Entry>>() {}.getType());
                if (l != null) entries = l;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            entries.add(new Entry());
            save();
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(entries, w);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
