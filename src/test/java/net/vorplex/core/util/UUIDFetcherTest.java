package net.vorplex.core.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UUIDFetcherTest {

    @Test
    void formatUUID() {
        assertEquals(UUID.fromString("74f04a9b-b7f9-409d-a940-b051f14dd3a5"), UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"));
    }

    @Test
    void call() throws Exception {
        UUIDFetcher uuidFetcher = new UUIDFetcher();
        uuidFetcher.fetch("I54m");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<UUID> future = executorService.submit(uuidFetcher);
        assertEquals(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), future.get(5, TimeUnit.SECONDS));
    }

    @Test
    void callStoredUUID() throws Exception {
        UUIDFetcher uuidFetcher = new UUIDFetcher();
        uuidFetcher.storeUUID(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), "I54m");
        uuidFetcher.fetch("I54m");
        assertEquals(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), uuidFetcher.call());
    }
}