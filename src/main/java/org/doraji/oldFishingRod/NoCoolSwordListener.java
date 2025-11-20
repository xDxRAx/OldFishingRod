package org.doraji.oldFishingRod;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.player.PlayerItemHeldEvent;
import java.util.*;

public class NoCoolSwordListener implements Listener {

    private final OldFishingRod plugin;

    private static final double FAST_ATTACK_SPEED = 8.0;
    // 기본 공격 속도
    private static final double DEFAULT_ATTACK_SPEED = 4.0;
    // 부스트 지속 시간 (0.5초 = 10틱)
    private static final long BOOST_DURATION_TICKS = 10L;

    // 검 종류
    private static final EnumSet<Material> SWORD_TYPES = EnumSet.of(
            Material.WOODEN_SWORD,
            Material.STONE_SWORD,
            Material.IRON_SWORD,
            Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD,
            Material.NETHERITE_SWORD
    );

    // 플레이어별 공격 속도 복원 태스크를 저장하는 맵
    private final Map<UUID, BukkitTask> attackSpeedTasks = new HashMap<>();

    public NoCoolSwordListener(OldFishingRod plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 플레이어가 나가면 태스크 취소 및 맵에서 제거
        cancelAndReset(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 플레이어가 접속하면 기본 속도로 설정
        resetAttackSpeed(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // 리스폰 시 1틱 후 손 상태를 확인하여 초기화
        runTaskLater(() -> checkHandAndReset(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // 월드 변경 시 1틱 후 손 상태를 확인하여 초기화
        runTaskLater(() -> checkHandAndReset(event.getPlayer()));
    }

    /**
     * 플레이어가 핫바 슬롯을 변경할 때 (스크롤, 숫자키)
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (isSword(newItem)) {
            // 새로 든 아이템이 검이면 부스트 시작
            triggerBoost(player);
        } else {
            // 검이 아니면, (이전에 검을 들어서) 활성화된 부스트가 있다면 취소하고 속도 복원
            cancelAndReset(player);
        }
    }

    // --- 아이템을 놓치는 다른 이벤트들 ---

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        // 아이템을 버린 후 1틱 뒤에 손 상태 체크
        runTaskLater(() -> checkHandAndReset(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        // 아이템이 부서진 후 1틱 뒤에 손 상태 체크
        runTaskLater(() -> checkHandAndReset(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        // F키(양손 교체) 후 1틱 뒤에 손 상태 체크
        runTaskLater(() -> checkHandAndReset(event.getPlayer()));
    }


    // --- 핵심 로직 ---

    /**
     * 플레이어에게 0.5초간 공격 속도 부스트를 적용합니다.
     */
    private void triggerBoost(Player player) {
        UUID uuid = player.getUniqueId();

        // 이미 실행 중인 복원 태스크가 있다면 취소 (타이머 리셋)
        if (attackSpeedTasks.containsKey(uuid)) {
            attackSpeedTasks.get(uuid).cancel();
        }

        // 공격 속도를 즉시 높임
        setAttackSpeed(player, FAST_ATTACK_SPEED);

        // 0.5초(10틱) 후에 속도를 복원하는 새 태스크 생성 및 등록
        BukkitTask resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 시간이 다 되면 속도를 복원
                setAttackSpeed(player, DEFAULT_ATTACK_SPEED);
                // 맵에서 태스크 제거
                attackSpeedTasks.remove(uuid);
            }
        }.runTaskLater(this.plugin, BOOST_DURATION_TICKS);

        // 새 태스크를 맵에 저장
        attackSpeedTasks.put(uuid, resetTask);
    }

    /**
     * 플레이어의 손 상태를 확인하고, 검이 아니면 부스트를 취소하고 속도를 복원합니다.
     */
    private void checkHandAndReset(Player player) {
        if (player == null || !player.isOnline()) return;

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!isSword(mainHandItem)) {
            cancelAndReset(player);
        }
    }

    /**
     * 플레이어의 부스트 태스크를 즉시 취소하고 공격 속도를 기본값으로 복원합니다.
     */
    private void cancelAndReset(Player player) {
        UUID uuid = player.getUniqueId();
        if (attackSpeedTasks.containsKey(uuid)) {
            attackSpeedTasks.get(uuid).cancel();
            attackSpeedTasks.remove(uuid);
        }
        resetAttackSpeed(player);
    }

    /**
     * 플레이어의 공격 속도를 설정합니다.
     */
    private void setAttackSpeed(Player player, double speed) {
        AttributeInstance attribute = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attribute != null) {
            attribute.setBaseValue(speed);
        }
    }

    /**
     * 플레이어의 공격 속도를 기본값(4.0)으로 즉시 복원합니다.
     */
    private void resetAttackSpeed(Player player) {
        setAttackSpeed(player, DEFAULT_ATTACK_SPEED);
    }

    /**
     * 아이템이 검인지 확인합니다.
     */
    private boolean isSword(ItemStack item) {
        return item != null && SWORD_TYPES.contains(item.getType());
    }

    /**
     * Bukkit 스케줄러를 이용해 작업을 지연 실행하는 헬퍼 메소드
     */
    private void runTaskLater(Runnable runnable) {
        new BukkitRunnable() {
            @Override
            public void run() {
                runnable.run();
            }
        }.runTaskLater(this.plugin, 1L); // (Plugin) this -> this.plugin
    }
}
