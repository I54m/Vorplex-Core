package net.vorplex.core.util;

import com.google.gson.Gson;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

public class UUIDFetcher implements Callable<UUID> {

    @Getter
    public static final UUID BLANK_UUID = UUID.fromString("0-0-0-0-0");
    private static final HashMap<String, UUID> UUIDS = new HashMap<>();
    private String name;

    public static UUID formatUUID(String uuid) {
        if (uuid.contains("-")) return UUID.fromString(uuid);
        else
            return UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

    public static void updateStoredUUID(String name, UUID uuid) {
        for (String storedName : UUIDS.keySet()) {
            UUID StoredUUID = UUIDS.get(storedName);
            if (uuid.equals(StoredUUID)) {
                if (!name.toLowerCase().equals(storedName)) {
                    UUIDS.remove(storedName);
                    UUIDS.put(name.toLowerCase(), uuid);
                }
                return;
            }
        }
        UUIDS.put(name.toLowerCase(), uuid);
    }

    public void fetch(String name) {
        this.name = name;
    }

    @Override
    public UUID call() throws Exception {
        if (name.equalsIgnoreCase("console"))
            return BLANK_UUID;
        if (UUIDS.containsKey(name.toLowerCase()))
            return UUIDS.get(name.toLowerCase());
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = new URI("https://api.mojang.com/users/profiles/minecraft/" + this.name).toURL().openConnection();
        urlConn.setReadTimeout(5000);
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
        if (sb.toString().isEmpty() || sb.toString().length() < 7) return null;
        Gson g = new Gson();
        Profile profile;
        profile = g.fromJson(sb.toString(), Profile.class);
        UUID uuid = UUIDFetcher.formatUUID(profile.id);
        NameFetcher.storeName(uuid, profile.name());
        UUIDS.put(name.toLowerCase(), uuid);
        return uuid;
    }

    public void storeUUID(UUID uuid, String name) {
        UUIDS.put(name.toLowerCase(), uuid);
    }


    private record Profile(String name, String id) {

    }
}
