package me.vectornetwork.core.util;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;

public class NameFetcher {
    private static HashMap<String, String> names = new HashMap<>();

    public static String getName(String uuid) {
        if (uuid.equalsIgnoreCase("console"))
            return "CONSOLE";
        uuid = uuid.replace("-", "");
        if (names.containsKey(uuid)) {
            return names.get(uuid);
        }
        String output = callURL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < 200) {
            if (output.length() == 0)
                return names.getOrDefault(uuid, null);
            if ((String.valueOf(output.charAt(i)).equalsIgnoreCase("n"))
                    && (String.valueOf(output.charAt(i + 1)).equalsIgnoreCase("a"))
                    && (String.valueOf(output.charAt(i + 2)).equalsIgnoreCase("m"))
                    && (String.valueOf(output.charAt(i + 3)).equalsIgnoreCase("e"))) {
                int k = i + 7;
                while (k < 100) {
                    if (!String.valueOf(output.charAt(k)).equalsIgnoreCase("\"")) {
                        result.append(String.valueOf(output.charAt(k)));
                    } else {
                        break;
                    }
                    k++;
                }
                break;
            }
            i++;
        }
        new UUIDFetcher().storeUUID(uuid, result.toString());
        names.put(uuid, result.toString());
        return result.toString();
    }

    private static String callURL(String URL) {
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn;
        InputStreamReader in = null;
        try {
            URL url = new URL(URL);
            urlConn = url.openConnection();
            if (urlConn != null) urlConn.setReadTimeout(6000);
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
        } catch (IOException e) {
            //ignore error
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static void storeName(String uuid, String name) {
        uuid = uuid.replace("-", "");
        names.put(uuid, name);
    }

    public static String getStoredName(String uuid){
        return names.get(uuid);
    }

    public static boolean hasNameStored(String uuid){
        return names.containsKey(uuid);
    }
}