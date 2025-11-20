package org.doraji.oldFishingRod;

import org.bukkit.entity.Entity;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Objects;

public class FishingRodListener implements Listener {

    private final OldFishingRod plugin;

    public FishingRodListener(OldFishingRod plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof FishHook hook)) {
            return;
        }

        if (!(hook.getShooter() instanceof Player player)) {
            return;
        }

        Vector velocity = hook.getVelocity();
        hook.setVelocity(velocity.multiply(plugin.hookSpeedMultiplier));

        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 60;

            @Override
            public void run() {
                if (hook.isDead() || !hook.isValid() || ticks >= maxTicks) {
                    this.cancel();
                    return;
                }

                if (hook.getState() == FishHook.HookState.HOOKED_ENTITY) {
                    Entity hookedEntity = hook.getHookedEntity();

                    if (hookedEntity != null && !hookedEntity.isDead() && hookedEntity instanceof LivingEntity livingHookedEntity) {

                        livingHookedEntity.damage(0.01, player);
                    }

                    this.cancel();
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerReelIn(PlayerFishEvent event) {
        Entity caughtEntity = event.getCaught();

        if (caughtEntity == null) {
            return;
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Objects.requireNonNull(caughtEntity).setVelocity(new Vector(0, 0, 0));
                }
            }.runTaskLater(plugin, 1L); // 1틱(0.05초) 뒤에 실행
        }
    }
}