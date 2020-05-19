package me.vectornetwork.core.util;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

public class UUIDFetcher implements Callable<String> {

    private String name;
    private HashMap<String, String> uuidCache = new HashMap<>();

    public void fetch(String name) {
        this.name = name;
    }

    @Override
    public String call() throws Exception {
        if (name.equalsIgnoreCase("console"))
            return "CONSOLE";
        if (uuidCache.containsKey(name.toLowerCase())){
            System.out.println("fetching UUID from cache....");// TODO: 19/02/2020 remove once tested
            return uuidCache.get(name.toLowerCase());
        }
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn;
        InputStreamReader in = null;
        URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + this.name);
        urlConn = url.openConnection();
        if (urlConn != null) urlConn.setReadTimeout(5000);
        if (urlConn != null && urlConn.getInputStream() != null) {
            in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
            BufferedReader bufferedReader = new BufferedReader(in);
            if (bufferedReader != null) {
                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
        }
        if (in != null)
            in.close();
        if (sb.toString().isEmpty() || sb.toString().length() < 7) return null;
        Gson g = new Gson();
        Profile profile = g.fromJson(sb.toString(), Profile.class);

        NameFetcher.storeName(profile.getUuid(), profile.getName());
        uuidCache.put(name.toLowerCase(), profile.getUuid());
        return profile.getUuid();
    }

    public static UUID formatUUID(String uuid) {
        StringBuffer sb = new StringBuffer(uuid);
        sb.insert(8, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(13, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(18, "-");
        sb = new StringBuffer(sb.toString());
        sb.insert(23, "-");
        return UUID.fromString(sb.toString());
    }

    public void storeUUID(String uuid, String name){
        uuid = uuid.replace("-", "");
        this.uuidCache.put(name.toLowerCase(), uuid);
    }


    public static void updateStoredUUID(String name, String uuid){
        UUIDFetcher uuidFetcher = new UUIDFetcher();
        for (String storedName : uuidFetcher.uuidCache.keySet()) {
            String StoredUUID = uuidFetcher.uuidCache.get(storedName);
            if (uuid.equals(StoredUUID)){
                if (!name.toLowerCase().equals(storedName)){
                    uuidFetcher.uuidCache.remove(storedName);
                    uuidFetcher.uuidCache.put(name.toLowerCase(), uuid);
                }
                return;
            }
        }
        uuidFetcher.uuidCache.put(name.toLowerCase(), uuid);
    }

    private static class Profile {
        private String name;
        private String id;

        public Profile(String name, String id){
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return id;
        }
    }
}
