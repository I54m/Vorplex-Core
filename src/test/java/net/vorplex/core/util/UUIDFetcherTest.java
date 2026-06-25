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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UUIDFetcherTest {

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

    @BeforeEach
    void eachSetup() {
        UUIDFetcher.setCacheProvider(new TestProfileCacheProvider());
    }

    @AfterAll
    static void cleanup() {
        threadPool.shutdownNow();
    }


    @Test
    void getBlankUUID_ShouldReturnZeroUuid() {
        UUID expected = new UUID(0L, 0L);

        assertEquals(UUIDFetcher.getBLANK_UUID(), expected);
    }


    @Test
    void formatUUID_WithoutDashes_ShouldReturnFormattedUuid() {
        String raw = "550e8400e29b41d4a716446655440000";
        UUID result = UUIDFetcher.formatUUID(raw);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.toString());
    }


    @Test
    void formatUUID_WithDashes_ShouldReturnSameUuid() {
        UUID result = UUIDFetcher.formatUUID("550e8400-e29b-41d4-a716-446655440000");

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result);
    }


    @Test
    void fetchUUID_InvalidPlayerName_ShouldThrowException() {
        assertThrows(Exception.class, () -> UUIDFetcher.fetchUUID("invalid player", threadPool));
    }


    @Test
    void fetchUUID_ConsolePlayer_ShouldReturnBlankUuid() throws Exception {
        UUID result = UUIDFetcher.fetchUUID("console", threadPool);

        assertEquals(UUIDFetcher.getBLANK_UUID(), result);
    }


    @Test
    void fetchUUIDAsync_ConsolePlayer_ShouldReturnBlankUuid() throws Exception {
        UUID result = UUIDFetcher.fetchUUIDAsync("console", threadPool).get(5, TimeUnit.SECONDS);

        assertEquals(UUIDFetcher.getBLANK_UUID(), result);
    }


    @Test
    void fetchUUIDAsync_FailedLookup_ShouldPropagateException() {
        assertThrows(CompletionException.class, () -> UUIDFetcher.fetchUUIDAsync("bad name!", threadPool).join());
    }

    @Test
    void fetchUUID_ValidPlayer_ShouldReturnExpectedUuid() throws Exception {
        assertEquals(UUID.fromString("74f04a9b-b7f9-409d-a940-b051f14dd3a5"), UUIDFetcher.fetchUUID("I54m", threadPool));
    }
}