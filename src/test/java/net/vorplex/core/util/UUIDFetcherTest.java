package net.vorplex.core.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
        UUIDFetcher.setCacheProvider(name -> null);
    }

    @AfterAll
    static void cleanup() {
        threadPool.shutdownNow();
    }


    @Test
    void testBlankUUID() {
        UUID expected = new UUID(0L, 0L);

        assertEquals(UUIDFetcher.getBLANK_UUID(), expected);
    }


    @Test
    void testFormatUUIDWithoutDashes() {
        String raw = "550e8400e29b41d4a716446655440000";
        UUID result = UUIDFetcher.formatUUID(raw);

        assertEquals("550e8400-e29b-41d4-a716-446655440000", result.toString());
    }


    @Test
    void testFormatUUIDWithDashes() {
        UUID result = UUIDFetcher.formatUUID("550e8400-e29b-41d4-a716-446655440000");

        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), result);
    }


    @Test
    void testInvalidPlayerNameThrowsException() {
        assertThrows(Exception.class, () -> UUIDFetcher.fetchUUID("invalid player", threadPool));
    }


    @Test
    void testConsoleUUID() throws Exception {
        UUID result = UUIDFetcher.fetchUUID("console", threadPool);

        assertEquals(UUIDFetcher.getBLANK_UUID(), result);
    }


    @Test
    void testConsoleUUIDAsync() throws Exception {
        UUID result = UUIDFetcher.fetchUUIDAsync("console", threadPool).get(5, TimeUnit.SECONDS);

        assertEquals(UUIDFetcher.getBLANK_UUID(), result);
    }


    @Test
    void testAsyncFailurePropagates() {
        assertThrows(CompletionException.class, () -> UUIDFetcher.fetchUUIDAsync("bad name!", threadPool).join());
    }
}