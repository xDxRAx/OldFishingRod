package org.doraji.oldFishingRod;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.doraji.oldFishingRod.commands.FishingRodCommand;

import java.util.*;

public final class OldFishingRod extends JavaPlugin implements Listener {

    public double hookSpeedMultiplier = 2.0;

    @Override
    public void onEnable() {
        getConfig().addDefault("fishhook-speed", 2.0);
        getConfig().options().copyDefaults(true); // 기본값을 config.yml에 복사
        saveConfig();
        this.hookSpeedMultiplier = getConfig().getDouble("fishhook-speed");
        getServer().getPluginManager().registerEvents(new FishingRodListener(this), this);
        getServer().getPluginManager().registerEvents(new NoCoolSwordListener(this), this);
        Objects.requireNonNull(this.getCommand("fishhookspeed")).setExecutor(new FishingRodCommand(this));
        getLogger().info("OldFishingRod 플러그인이 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        getLogger().info("OldFishingRod 플러그인이 비활성화되었습니다.");
    }
}

