package net.vorplex.core.util.profile;

import net.vorplex.core.util.UUIDFetcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ProfileCacheProviderTest {

    private ProfileCacheProvider cacheProvider;

    @BeforeEach
    void eachSetUp() {
        ProfileCacheProvider.CACHED_PROFILES.clear();

        cacheProvider = new TestProfileCacheProvider();
    }

    @Test
    void storeProfile_CachedProfile_ShouldStoreProfile() {
        UUID uuid = UUID.randomUUID();
        CachedProfile profile = new CachedProfile("TestPlayer", uuid);

        cacheProvider.storeProfile(profile);

        CachedProfile cached = cacheProvider.getFromCache(uuid);

        assertNotNull(cached);
        assertEquals("TestPlayer", cached.name());
        assertEquals(uuid, cached.uuid());
    }

    @Test
    void storeProfile_UuidAndName_ShouldStoreProfile() {
        UUID uuid = UUID.randomUUID();

        cacheProvider.storeProfile(uuid, "TestPlayer");

        CachedProfile cached = cacheProvider.getFromCache(uuid);

        assertNotNull(cached);
        assertEquals("TestPlayer", cached.name());
        assertEquals(uuid, cached.uuid());
    }

    @Test
    void getFromCache_ByUuid_ShouldReturnProfile() {
        UUID uuid = UUID.randomUUID();
        CachedProfile profile = new CachedProfile("TestPlayer", uuid);

        cacheProvider.storeProfile(profile);

        CachedProfile result = cacheProvider.getFromCache(uuid);

        assertEquals(profile, result);
    }

    @Test
    void getFromCache_ByUuid_WhenMissing_ShouldReturnNull() {
        CachedProfile result = cacheProvider.getFromCache(UUID.randomUUID());

        assertNull(result);
    }

    @Test
    void getFromCache_ByName_ShouldReturnProfile() {
        UUID uuid = UUID.randomUUID();
        CachedProfile profile = new CachedProfile("TestPlayer", uuid);

        cacheProvider.storeProfile(profile);

        CachedProfile result = cacheProvider.getFromCache("TestPlayer");

        assertEquals(profile, result);
    }

    @Test
    void getFromCache_ByName_WhenMissing_ShouldReturnNull() {
        CachedProfile result = cacheProvider.getFromCache("UnknownPlayer");

        assertNull(result);
    }

    @Test
    void storeProfile_ShouldOverwriteExistingUuid() {
        UUID uuid = UUID.randomUUID();

        cacheProvider.storeProfile(uuid, "OldName");
        cacheProvider.storeProfile(uuid, "NewName");

        CachedProfile result = cacheProvider.getFromCache(uuid);

        assertNotNull(result);
        assertEquals("NewName", result.name());
    }

    @Test
    void storeProfile_MojangProfile_ShouldStoreConvertedProfile() {
        MojangProfile profile = new MojangProfile("TestPlayer", "12345678123456781234567812345678");

        cacheProvider.storeProfile(profile);

        UUID expectedUuid = UUIDFetcher.formatUUID(profile.id());

        CachedProfile result = cacheProvider.getFromCache(expectedUuid);

        assertNotNull(result);
        assertEquals("TestPlayer", result.name());
        assertEquals(expectedUuid, result.uuid());
    }
}
