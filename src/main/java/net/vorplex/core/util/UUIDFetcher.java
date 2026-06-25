package net.vorplex.core.util;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import net.vorplex.core.VorplexCore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;

public class UUIDFetcher implements Callable<UUID> {

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    private static final HashMap<String, UUID> UUIDS = new HashMap<>();

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    public static void updateStoredUUID(String name, UUID uuid) {
    }

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    public void fetch(String name) {
    }

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    public void storeUUID(UUID uuid, String name) {
    }

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    public UUIDFetcher() {
    }

    @Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
    @Override
    public UUID call() throws Exception {
        return null;
    }


    @Getter
    public static final UUID BLANK_UUID = new UUID(0L, 0L);
    @Setter
    public static UUIDCacheProvider cacheProvider = new BukkitUUIDCacheProvider();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Gson GSON = new Gson();

    /**
     * Create an HTTP Request to the mojang api to fetch the player's UUID
     *
     * @param playerName the name of the player to fetch the uuid for
     * @return the uuid of the player is fetched
     * @throws Exception if any exceptions were encountered
     */
    private static UUID lookupUUID(String playerName) throws Exception {
        if (playerName.equalsIgnoreCase("console"))
            return BLANK_UUID;

        UUID cachedUUID = cacheProvider.getCachedUUID(playerName);
        if (cachedUUID != null)
            return cachedUUID;

        if (!playerName.matches("[a-zA-Z0-9_]{1,16}"))
            throw new IllegalArgumentException("Player name does not fit minecraft player name requirements!");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://api.mojang.com/users/profiles/minecraft/" + playerName
                ))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new IllegalStateException("Mojang API responded with a status code of: " + response.statusCode());

        Profile profile = GSON.fromJson(response.body(), Profile.class);

        if (profile == null || profile.id() == null)
            throw new IllegalStateException("profile or profile.id() was null!");

        UUID uuid = formatUUID(profile.id());

        NameFetcher.storeName(uuid, profile.name());

        return uuid;
    }

    private record Profile(String name, String id) {
    }

    /**
     * Format a string into a UUID (with or without dashes)
     *
     * @param uuid the uuid string to format
     * @return the formatted uuid
     */
    public static UUID formatUUID(String uuid) {
        if (uuid.contains("-")) return UUID.fromString(uuid);
        else
            return UUID.fromString(uuid.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
    }

    /**
     * Fetch a player's uuid Asynchronously
     * use CompletableFuture#thenAccept() to run an async function once the uuid is fetched
     *
     * @param playerName name of player to fetch
     * @param executor the thread executor to use
     * @return the UUID of the player
     */
    public static CompletableFuture<UUID> fetchUUIDAsync(String playerName, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return lookupUUID(playerName);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Fetch a player's uuid Asynchronously
     * use CompletableFuture#thenAccept() to run an async function once the uuid is fetched
     *
     * @param playerName name of player to fetch
     * @return the UUID of the player
     */
    public static CompletableFuture<UUID> fetchUUIDAsync(String playerName) {
        return fetchUUIDAsync(playerName, VorplexCore.getInstance().getThreadPool());
    }

    /**
     * Fetch a player's uuid synchronously, with a 10-second timeout
     *
     * @param playerName name of player to fetch
     * @return the UUID of the player
     * @throws Exception any exception that was encountered during UUID fetching
     */
    public static UUID fetchUUID(String playerName) throws Exception {
        return fetchUUIDAsync(playerName).get(10, TimeUnit.SECONDS);
    }

    /**
     * Fetch a player's uuid synchronously, with a 10-second timeout
     *
     * @param playerName name of player to fetch
     * @param executor   the thread executor to use
     * @return the UUID of the player
     * @throws Exception any exception that was encountered during UUID fetching
     */
    public static UUID fetchUUID(String playerName, Executor executor) throws Exception {
        return fetchUUIDAsync(playerName, executor).get(10, TimeUnit.SECONDS);
    }
}
