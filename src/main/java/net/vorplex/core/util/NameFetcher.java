package net.vorplex.core.util;

import com.google.gson.Gson;
import lombok.Setter;
import net.vorplex.core.VorplexCore;
import net.vorplex.core.util.profile.BukkitProfileCacheProvider;
import net.vorplex.core.util.profile.MojangProfile;
import net.vorplex.core.util.profile.ProfileCacheProvider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class NameFetcher {

    @Setter
    private static ProfileCacheProvider cacheProvider = new BukkitProfileCacheProvider();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Gson GSON = new Gson();

    /**
     * Create an HTTP Request to the mojang session server api to fetch the player's name
     *
     * @param uuid the uuid of the player to fetch the uuid for
     * @return the name of the player if fetched
     * @throws Exception if any exceptions were encountered
     */
    private static String lookupName(UUID uuid) throws Exception {
        if (uuid.equals(UUIDFetcher.getBLANK_UUID()))
            return "console";

        String cachedName = cacheProvider.getCachedName(uuid);
        if (cachedName != null)
            return cachedName;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")
                ))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200)
            throw new IllegalStateException("Mojang API responded with a status code of: " + response.statusCode());

        MojangProfile profile = GSON.fromJson(response.body(), MojangProfile.class);

        if (profile == null || profile.name() == null)
            throw new IllegalStateException("profile or profile.name() was null!");

        cacheProvider.storeProfile(profile);

        return profile.name();
    }

    /**
     * Fetch a player's name Asynchronously
     * use CompletableFuture#thenAccept() to run an async function once the name is fetched
     *
     * @param uuid     uuid of player to fetch
     * @param executor the thread executor to use
     * @return the name of the player
     */
    public static CompletableFuture<String> fetchNameAsync(UUID uuid, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return lookupName(uuid);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Fetch a player's name Asynchronously
     * use CompletableFuture#thenAccept() to run an async function once the name is fetched
     *
     * @param uuid uuid of player to fetch
     * @return the name of the player
     */
    public static CompletableFuture<String> fetchNameAsync(UUID uuid) {
        return fetchNameAsync(uuid, VorplexCore.getInstance().getThreadPool());
    }

    /**
     * Fetch a player's name synchronously, with a 10-second timeout
     *
     * @param uuid uuid of player to fetch
     * @return the name of the player
     * @throws Exception any exception that was encountered during name fetching
     */
    public static String fetchName(UUID uuid) throws Exception {
        return fetchNameAsync(uuid, VorplexCore.getInstance().getThreadPool()).get(10, TimeUnit.SECONDS);
    }

    /**
     * Fetch a player's name synchronously, with a 10-second timeout
     *
     * @param uuid     uuid of player to fetch
     * @param executor the thread executor to use
     * @return the name of the player
     * @throws Exception any exception that was encountered during name fetching
     */
    public static String fetchName(UUID uuid, Executor executor) throws Exception {
        return fetchNameAsync(uuid, executor).get(10, TimeUnit.SECONDS);
    }
}