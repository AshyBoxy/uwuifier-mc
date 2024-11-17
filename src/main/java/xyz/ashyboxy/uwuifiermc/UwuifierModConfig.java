package xyz.ashyboxy.uwuifiermc;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.JsonHelper;

public class UwuifierModConfig {
    private static UserMode mode = UserMode.BLACKLIST;
    public static List<String> blacklist = new ArrayList<>();
    public static List<String> whitelist = new ArrayList<>();

    public static final Path filePath = FabricLoader.getInstance().getConfigDir().resolve("uwuifier.json");

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Nullable
    public static int load() {
        if (!Files.exists(filePath)) {
            save();
            return 2;
        }
        JsonElement j1;
        try {
            j1 = JsonParser.parseString(Files.readString(filePath));
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        if (!j1.isJsonObject())
            return -2;
        JsonObject j = j1.getAsJsonObject();
        mode = UserMode.fromString(JsonHelper.getString(j, "mode", mode.getString()));
        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        blacklist = gson.fromJson(JsonHelper.getArray(j, "blacklist", null), listType);
        whitelist = gson.fromJson(JsonHelper.getArray(j, "whitelist", null), listType);
        return 1;
    }

    public static void save() {
        JsonObject j = new JsonObject();
        j.addProperty("mode", mode.getString());
        j.add("blacklist", gson.toJsonTree(blacklist));
        j.add("whitelist", gson.toJsonTree(whitelist));
        try {
            Files.writeString(filePath, gson.toJson(j));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int blacklistAdd(String uuid) {
        if (blacklist.indexOf(uuid) > -1)
            return -1;
        blacklist.add(uuid);
        save();
        return 1;
    }

    public static int blacklistRemove(String uuid) {
        if (blacklist.indexOf(uuid) < 0)
            return -1;
        while (blacklist.remove(uuid))
            ;
        save();
        return 1;
    }

    public static int whitelistAdd(String uuid) {
        if (whitelist.indexOf(uuid) > -1)
            return -1;
        whitelist.add(uuid);
        save();
        return 1;
    }

    public static int whitelistRemove(String uuid) {
        if (whitelist.indexOf(uuid) < 0)
            return -1;
        while (whitelist.remove(uuid))
            ;
        save();
        return 1;
    }

    public static UserMode getMode() {
        return mode;
    }

    public static UserMode setMode(UserMode newMode) {
        mode = newMode;
        save();
        return mode;
    }

    private static Map<String, UserMode> userModeStrings;

    public static Map<String, UserMode> getUserModeStrings() {
        return Collections.unmodifiableMap(userModeStrings);
    }

    public static enum UserMode {
        BLACKLIST("blacklist"),
        WHITELIST("whitelist"),
        DISABLED("disabled");

        private String stringRep;

        private UserMode(String stringRep) {
            this.stringRep = stringRep;
            if (userModeStrings == null)
                userModeStrings = new HashMap<>();
            userModeStrings.put(stringRep, this);
        }

        public String getString() {
            return this.stringRep;
        }

        public static UserMode fromString(String mode) {
            return userModeStrings.get(mode);
        }
    }
}
