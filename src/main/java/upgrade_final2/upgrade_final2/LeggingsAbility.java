package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class LeggingsAbility implements Listener {

    private final JavaPlugin plugin;
    private final HashMap<UUID, Long> sneakStartTimes; // 웅크리기 시작 시간 기록
    private final HashMap<UUID, BukkitTask> chargeTasks; // 충전 작업 저장
    private final HashMap<UUID, Boolean> isSuperJumpCharged; // 슈퍼 점프 충전 상태 저장
    private final HashMap<UUID, Boolean> hasAirDashed; // 공중 대쉬 사용 여부 저장
    private final HashMap<UUID, Boolean> isSuperJumpState; // 슈퍼 점프 상태 저장

    public LeggingsAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        this.sneakStartTimes = new HashMap<>();
        this.chargeTasks = new HashMap<>();
        this.isSuperJumpCharged = new HashMap<>();
        this.hasAirDashed = new HashMap<>();
        this.isSuperJumpState = new HashMap<>();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack leggings = player.getInventory().getLeggings();

        if (leggings != null && isLegendaryLeggings(leggings)) {
            UUID playerUUID = player.getUniqueId();

            if (event.isSneaking()) {
                // 지면에서 웅크리기: 슈퍼 점프 충전 시작
                if (player.isOnGround()) {
                    sneakStartTimes.put(playerUUID, System.currentTimeMillis());
                    isSuperJumpCharged.put(playerUUID, false); // 초기화
                    isSuperJumpState.put(playerUUID, false); // 슈퍼 점프 상태 초기화

                    // 5초 후 메시지 전송 및 충전 완료 상태 저장
                    BukkitTask chargeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.spigot().sendMessage(
                                ChatMessageType.ACTION_BAR,
                                TextComponent.fromLegacyText(ChatColor.GREEN + "슈퍼 점프 가능!"));
                        isSuperJumpCharged.put(playerUUID, true); // 충전 상태 업데이트
                    }, 100L); // 5초
                    chargeTasks.put(playerUUID, chargeTask);

                } else {
                    // 공중에서 웅크리기: 공중 대쉬 실행
                    if (isSuperJumpState.getOrDefault(playerUUID, false) // 슈퍼 점프 상태인지 확인
                            && !hasAirDashed.getOrDefault(playerUUID, false)) { // 대쉬 사용 여부 확인
                        performAirDash(player);
                        hasAirDashed.put(playerUUID, true); // 대쉬 사용 상태 전환
                    }
                }
            } else {
                // 웅크리기 해제: 슈퍼 점프 실행 (지면에서만 가능)
                Long sneakStartTime = sneakStartTimes.get(playerUUID);
                // 웅크리기 해제 시 슈퍼 점프 실행
                if (sneakStartTime != null && player.isOnGround()) {
                    if (isSuperJumpCharged.getOrDefault(playerUUID, false)) {
                        performSuperJump(player);
                        isSuperJumpCharged.put(playerUUID, false); // 충전 해제
                        isSuperJumpState.put(playerUUID, true); // 슈퍼 점프 상태 활성화
                        hasAirDashed.put(playerUUID, false); // 공중 대쉬 초기화
                    }
                    // 충전 작업 취소
                    BukkitTask chargeTask = chargeTasks.get(playerUUID);
                    if (chargeTask != null) {
                        chargeTask.cancel();
                    }
                    sneakStartTimes.remove(playerUUID);
                    chargeTasks.remove(playerUUID);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // 지면에 닿았을 때 슈퍼 점프 상태 초기화
        if (player.isOnGround() && isSuperJumpState.getOrDefault(playerUUID, false)) {
            hasAirDashed.put(playerUUID, false); // 공중 대쉬 상태 초기화
            isSuperJumpState.put(playerUUID, false); // 슈퍼 점프 상태 초기화
            isSuperJumpCharged.put(playerUUID, false); // 충전 상태 초기화(사용 후)
        }
    }

    private boolean isLegendaryLeggings(ItemStack item) {
        // 전설의 레깅스인지 확인하는 로직 (레깅스 이름 등으로 구분)
        if (item.getType() == Material.LEATHER_LEGGINGS || item.getType() == Material.CHAINMAIL_LEGGINGS
                || item.getType() == Material.IRON_LEGGINGS || item.getType() == Material.GOLDEN_LEGGINGS
                || item.getType() == Material.DIAMOND_LEGGINGS || item.getType() == Material.NETHERITE_LEGGINGS) {
            return item.getItemMeta() != null && item.getItemMeta().getDisplayName().contains("전설의");
        }
        return false;
    }


    private void performSuperJump(Player player) {
        Vector velocity = player.getVelocity();
        velocity.setY(velocity.getY() + 2); // Y축 속도 증가 -> 슈퍼 점프 실행
        player.setVelocity(velocity);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ChatColor.AQUA + "슈퍼 점프 발동!"));
    }

    private void performAirDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize(); // 플레이어의 바라보는 방향
        Vector dashVelocity = direction.multiply(15); // 대쉬 가속
        dashVelocity.setY(0.5); // 위로 약간 상승
        player.setVelocity(dashVelocity);
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ChatColor.LIGHT_PURPLE + "공중 대쉬 발동!"));;
    }
}