package net.vorplex.core.util;

import lombok.Setter;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.vorplex.core.VorplexCore;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@Deprecated(since = "2.0-SNAPSHOT-1.4.2", forRemoval = true)
@Setter
public class UserFetcher implements Callable<User> {
    private UUID uuid;

    @Override
    public User call() throws Exception {
        UserManager userManager = VorplexCore.getInstance().getLuckPermsAPI().getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(uuid);
        return userFuture.join();
    }
}