package net.vorplex.core.util;

import net.vorplex.core.util.profile.TestProfileCacheProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameFetcherTest {

    private static ExecutorService threadPool;

    @BeforeAll
    static void setup() {
        threadPool = Executors.newFixedThreadPool(4,
                r -> {
                    Thread thread = new Thread(r);
                    thread.setName("VorplexCore-unitTests-Async");
                    return thread;
                });
    }

    @AfterAll
    static void cleanup() {
        threadPool.shutdownNow();
    }

    @BeforeEach
    void eachSetup() {
        NameFetcher.setCacheProvider(new TestProfileCacheProvider());
    }

    @Test
    void fetchName_ConsoleUUID_ShouldReturnConsoleName() throws Exception {
        String result = NameFetcher.fetchName(UUIDFetcher.getBLANK_UUID(), threadPool);
        assertEquals("console", result);
    }

    @Test
    void fetchNameAsync_ConsoleUUID_ShouldReturnConsoleName() throws Exception {
        String result = NameFetcher.fetchNameAsync(UUIDFetcher.getBLANK_UUID(), threadPool).get();
        assertEquals("console", result);
    }

    @Test
    void fetchNameAsync_InvalidUUID_ShouldPropagateException() {
        UUID invalidUuid = UUIDFetcher.formatUUID("c232ab00941411ecb3c89f6bdeced846");
        assertThrows(CompletionException.class, () -> NameFetcher.fetchNameAsync(invalidUuid, threadPool).join());
    }

    @Test
    void fetchName_ValidUUID_ShouldReturnExpectedName() throws Exception {
        assertEquals("I54m", NameFetcher.fetchName(UUID.fromString("74f04a9b-b7f9-409d-a940-b051f14dd3a5"), threadPool));
    }
}