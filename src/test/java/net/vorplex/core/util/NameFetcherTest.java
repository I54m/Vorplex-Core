package net.vorplex.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameFetcherTest {

    @Test
    void getName() {
        assertEquals("I54m", NameFetcher.getName("74f04a9bb7f9409da940b051f14dd3a5"));
    }

    @Test
    void getStoredName() {
        NameFetcher.storeName(UUIDFetcher.formatUUID("74f04a9bb7f9409da940b051f14dd3a5"), "I54m");
        assertEquals("I54m", NameFetcher.getName("74f04a9bb7f9409da940b051f14dd3a5"));
    }
}