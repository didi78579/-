package upgrade_final2.upgrade_final2;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Nether implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Long> playerLastJumpTime = new HashMap<>();
    private final Map<UUID, Long> playerAttackTime = new HashMap<>();

    public Nether(JavaPlugin plugin) {
        this.plugin = plugin;
        startPlayerStateChecker(); // 네더 환경 상태 체크 스케줄러 실행
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL) {
            if (player.isOnGround() && !player.getAllowFlight()) {
                setPlayerFlightMode(player);
            }
        }
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!player.getGameMode().equals(GameMode.SURVIVAL)) return;

        ItemStack boots = player.getInventory().getBoots();
        if (!isWearingNetherStarBoots(boots)) {
            player.setAllowFlight(false);
            event.setCancelled(true);
            return;
        }

        // 공격받은 후 30초 이내인지 확인
        long lastAttackTime = playerAttackTime.getOrDefault(playerUUID, 0L);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastAttackTime < 30000) { // 공격 후 30초 내
            long remainingTime = (30000 - (currentTime - lastAttackTime)) / 1000;
            player.sendMessage(ChatColor.RED + "더블 점프를 사용할 수 없습니다. 남은 시간: " + remainingTime + "초");
            event.setCancelled(true);
            return;
        }

        // 대시 수행
        player.setVelocity(player.getLocation().getDirection().multiply(1.3).setY(0.5));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 10, 0.2, 0.2, 0.2, 0.05);
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 1.2f);
        player.setAllowFlight(false); // 대시 후 비활성화
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (to != null) {
            World.Environment fromEnv = from.getWorld().getEnvironment();
            World.Environment toEnv = to.getWorld().getEnvironment();

            if (fromEnv == World.Environment.NORMAL && toEnv == World.Environment.NETHER) {
                // 오버월드 -> 네더
                to.setX(from.getX() * 8);
                to.setZ(from.getZ() * 8);
            } else if (fromEnv == World.Environment.NETHER && toEnv == World.Environment.NORMAL) {
                // 네더 -> 오버월드
                to.setX(from.getX() / 8);
                to.setZ(from.getZ() / 8);
            }

            event.setTo(to);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wither) {
            // 네더의 별 삭제
            event.getDrops().removeIf(item -> item.getType() == Material.NETHER_STAR);

            // 철 부츠 추가
            ItemStack upgradedBoots = new ItemStack(Material.IRON_BOOTS);
            ItemMeta bootsMeta = upgradedBoots.getItemMeta();
            if (bootsMeta != null) {
                bootsMeta.setDisplayName(ChatColor.YELLOW + "위더 부츠");

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.AQUA + "착용시 대시 가능");
                lore.add(ChatColor.LIGHT_PURPLE + "낙하 피해 면역");
                bootsMeta.setLore(lore);

                bootsMeta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
                bootsMeta.addEnchant(Enchantment.THORNS, 2, true);
                bootsMeta.addEnchant(Enchantment.SOUL_SPEED, 1, true);

                upgradedBoots.setItemMeta(bootsMeta);
            }
            event.getDrops().add(upgradedBoots);
        } else if (event.getEntity() instanceof Warden) {
            // 워든 처치 시 "워든 갑옷" 드랍
            World world = event.getEntity().getWorld();
            ItemStack wardenArmor = new ItemStack(Material.DIAMOND_CHESTPLATE); // 다이아몬드 갑옷 생성
            ItemMeta armorMeta = wardenArmor.getItemMeta();
            if (armorMeta != null) {
                armorMeta.setDisplayName(ChatColor.DARK_PURPLE + "워든 갑옷");

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.AQUA + "화염 저항");
                armorMeta.setLore(lore);

                armorMeta.addEnchant(Enchantment.DURABILITY, 3, true); // 내구성 강화

                wardenArmor.setItemMeta(armorMeta);
            }

            // 드롭 위치에 "워든 갑옷" 드롭
            world.dropItemNaturally(event.getEntity().getLocation(), wardenArmor);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            // PVP 공격 발생 시 처리
            Player player = (Player) event.getEntity();
            UUID playerUUID = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            // 공격받은 시간 기록
            playerAttackTime.put(playerUUID, currentTime);
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();

            // 낙하 피해 무효화
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                ItemStack boots = player.getInventory().getBoots();
                if (isWearingNetherStarBoots(boots)) {
                    event.setCancelled(true); // 낙하 피해 무효화
                }
            }

            // 화염 피해 무효화
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE || event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                ItemStack chestplate = player.getInventory().getChestplate();
                if (chestplate != null && chestplate.hasItemMeta() &&
                        (ChatColor.DARK_PURPLE + "워든 갑옷").equals(chestplate.getItemMeta().getDisplayName())) {
                    event.setCancelled(true); // 화염 피해 취소
                    player.setFireTicks(0); // 불 타는 상태 제거
                }
            }
        }
    }

    private void setPlayerFlightMode(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        if (isWearingNetherStarBoots(boots) && player.isOnGround()) {
            player.setAllowFlight(true); // 착륙 시 비행 모드 활성화
        } else {
            player.setAllowFlight(false); // 철 부츠 미착용 시 비활성화
        }
    }

    private boolean isWearingNetherStarBoots(ItemStack boots) {
        return boots != null && boots.hasItemMeta() &&
                (ChatColor.YELLOW + "위더 부츠").equals(boots.getItemMeta().getDisplayName());
    }

    private void startPlayerStateChecker() {
        // 네더 환경 상태 체크 스케줄러
        new BukkitRunnable() {
            @Override
            public void run() {
                checkPlayersState();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.SURVIVAL) {
                        updatePlayerEffects(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20 * 5); // 5초마다 실행
    }

    private void checkPlayersState() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                updatePlayerEffects(player); // 상태 갱신 (웅크리기 상태 포함)
            }
        }
    }

    private void updatePlayerEffects(Player player) {
        // 플레이어가 네더에 있는지 확인
        if (player.getWorld().getEnvironment() == World.Environment.NETHER) {
            ItemStack chestplate = player.getInventory().getChestplate();
            boolean isWearingWardenArmor = chestplate != null && chestplate.hasItemMeta() &&
                    (ChatColor.DARK_PURPLE + "워든 갑옷").equals(chestplate.getItemMeta().getDisplayName());

            // 워든 갑옷 미착용 시 지속적으로 화염 틱과 대미지를 부여
            if (!isWearingWardenArmor) {
                player.setFireTicks(400); // 1초간 화염 지속
            }
            else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false, false));
            }
        }
    }
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // 플레이어가 웅크리고 있는지 확인
        if (event.isSneaking()) { // 웅크리기 시작
            ItemStack chestplate = player.getInventory().getChestplate();

            // 워든 갑옷 착용 확인
            if (isWearingWardenArmor(chestplate)) {
                // 웅크릴 때 화염 피해 면역 처리
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0, false, false, false));
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            ItemStack chestplate = player.getInventory().getChestplate();

            // 워든 갑옷 착용 확인
            if (isWearingWardenArmor(chestplate)) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                        event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                    // 화염 피해를 막고 상태 초기화
                    event.setCancelled(true);
                    player.setFireTicks(0);
                }
            }
        }
    }

    private boolean isWearingWardenArmor(ItemStack chestplate) {
        return chestplate != null && chestplate.hasItemMeta() &&
                (ChatColor.DARK_PURPLE + "워든 갑옷").equals(chestplate.getItemMeta().getDisplayName());
    }
}