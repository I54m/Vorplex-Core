package net.vorplex.core.util;


import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;

public class NameFetcher {
    private static final HashMap<UUID, String> NAMES = new HashMap<>();

    public static String getName(String uuid) {
        return getName(UUIDFetcher.formatUUID(uuid));
    }

    public static String getName(UUID uuid) {
        if (uuid.equals(UUIDFetcher.getBLANK_UUID()))
            return "CONSOLE";
        if (NAMES.containsKey(uuid))
            return NAMES.get(uuid);
        String output = callURL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", ""));
        Gson g = new Gson();
        Profile profile;
        profile = g.fromJson(output.substring(0, output.indexOf(",\n  \"properties\"")) + "}", Profile.class);
        new UUIDFetcher().storeUUID(uuid, profile.getName());
        NAMES.put(uuid, profile.getName());
        return profile.getName();
    }

    protected static String callURL(String URL) {
        StringBuilder sb = new StringBuilder();
        try {
            URLConnection urlConn = new URL(URL).openConnection();
            urlConn.setReadTimeout(6000);
            if (urlConn.getInputStream() != null) {
                InputStreamReader in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);
                if (bufferedReader.ready()) {
                    int cp;
                    while ((cp = bufferedReader.read()) != -1) {
                        sb.append((char) cp);
                    }
                    bufferedReader.close();
                }
                in.close();
            }
        } catch (IOException e) {
            //ignore error
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void updateStoredName(UUID uuid, String name) {
        storeName(uuid, name);
    }

    public static void storeName(UUID uuid, String name) {
        NAMES.put(uuid, name);
    }

    public static String getStoredName(UUID uuid) {
        return NAMES.get(uuid);
    }

    public static boolean hasNameStored(UUID uuid) {
        return NAMES.containsKey(uuid);
    }

    protected static class Profile {
        private final String name, id;

        public Profile(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }
    }
}