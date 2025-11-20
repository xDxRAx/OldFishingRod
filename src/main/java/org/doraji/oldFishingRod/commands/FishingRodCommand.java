package org.doraji.oldFishingRod.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.doraji.oldFishingRod.OldFishingRod;
import org.jetbrains.annotations.NotNull;

public class FishingRodCommand implements CommandExecutor {

    private final OldFishingRod plugin;

    public FishingRodCommand(OldFishingRod plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // 1. 권한 확인 (공통)
        if (!sender.hasPermission("oldfishingrod.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }

        // 2. 인자 개수에 따라 분기

        // --- 여기가 수정된 부분입니다 ---
        if (args.length == 0) {
            sender.sendMessage("§a현재 낚싯바늘 속도 배율은 §e" + plugin.hookSpeedMultiplier + "§a입니다.");
            return true;

        } else if (args.length == 1) {
            try {
                double newSpeed = Double.parseDouble(args[0]);

                if (newSpeed <= 0) {
                    sender.sendMessage("§c속도는 0보다 큰 숫자여야 합니다.");
                    return true;
                }

                plugin.hookSpeedMultiplier = newSpeed;
                plugin.getConfig().set("fishhook-speed", newSpeed);
                plugin.saveConfig();

                sender.sendMessage("§a낚싯바늘의 속도가 §f§l" + newSpeed + "§r§a로 설정되었습니다.");

            } catch (NumberFormatException e) {
                sender.sendMessage("§c올바른 숫자를 입력해주세요. (예: 2 또는 1.5)");
                return true;
            }
            return true;

        } else {
            sender.sendMessage("§c사용법: /fishhookspeed <double>");
            return false;
        }
    }
}