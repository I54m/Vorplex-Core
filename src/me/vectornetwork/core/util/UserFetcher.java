package me.vectornetwork.core.util;

import me.vectornetwork.core.Main;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public class UserFetcher implements Callable<User> {
    private UUID uuid;

    public void setUuid(UUID uuid){
        this.uuid = uuid;
    }

    @Override
    public User call() throws Exception {
        UserManager userManager = Main.getInstance().luckPermsAPI.getUserManager();
        CompletableFuture<User> userFuture = userManager.loadUser(uuid);
        return userFuture.join();
    }
}