package upgrade_final2.upgrade_final2;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class BowUpgrade implements Listener {
    private final JavaPlugin plugin;

    public BowUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();

            if (item != null && isBow(item.getType())) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        double level = getLevelFromLore(lore);
                        if (level == 10) {
                            if (!player.hasMetadata("super_charge_task")) {
                                // 슈퍼차지 활성화 작업
                                BukkitRunnable superChargeTask = new BukkitRunnable() {
                                    int ticks = 0;

                                    @Override
                                    public void run() {
                                        // 플레이어가 활시위를 당기고 있는지 확인
                                        if (player.isHandRaised() && player.getInventory().getItemInMainHand().equals(item)) {
                                            ticks++;
                                            if (ticks >= 160) { // 8초 (160틱)
                                                player.setMetadata("super_charged", new FixedMetadataValue(plugin, true));

                                                // 슈퍼차지 활성화 효과
                                                player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 20, 0.5, 1, 0.5);
                                                player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

                                                startSuperChargeTimer(player, 5 * 20); // 슈퍼차지 지속시간 5초
                                                this.cancel(); // 작업 완료 후 취소
                                            }
                                        } else {
                                            // 활을 놓는 등 조건이 깨지면 작업 중단
                                            clearSuperChargeMetadata(player);
                                            this.cancel();
                                        }
                                    }
                                };

                                player.setMetadata("super_charge_task", new FixedMetadataValue(plugin, superChargeTask));
                                superChargeTask.runTaskTimer(plugin, 0, 1); // 1틱마다 실행
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack bow = event.getBow();
            if (isBow(bow.getType()) && bow.hasItemMeta()) {
                ItemMeta meta = bow.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    double level = getLevelFromLore(lore); // 계산된 레벨 확인

                    if (level == 10 && player.hasMetadata("super_charged") && !player.hasMetadata("super_charge_delay")) {
                        Arrow arrow = (Arrow) event.getProjectile();
                        arrow.setMetadata("super_charged_arrow", new FixedMetadataValue(plugin, true));
                        clearSuperChargeMetadata(player);
                        applyFlameParticles(arrow);

                        for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                            if (nearbyPlayer.getWorld().equals(player.getWorld()) &&
                                    nearbyPlayer.getLocation().distance(player.getLocation()) <= 50) {
                                nearbyPlayer.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            ItemStack bow = player.getInventory().getItemInMainHand();

            if (bow != null && isBow(bow.getType()) && bow.hasItemMeta()) {
                ItemMeta meta = bow.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null) {
                        double level = getLevelFromLore(lore);

                        // 적 체력에 비례한 추가 피해 계산
                        if (event.getEntity() instanceof LivingEntity target) {
                            if (arrow.hasMetadata("super_charged_arrow")) {
                                applySuperChargeEffect(player, target);
                            } else {
                                applyAdditionalDamage(player, target, level);
                            }
                        }
                    }
                }
            }
        }
    }

    // 적 체력에 비례한 추가 데미지 계산
    private void applyAdditionalDamage(Player player, LivingEntity target, double level) {
        if (target instanceof Player) {
            double targetCurrentHealth = target.getHealth();
            double maxHealth = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
            double additionalDamage = targetCurrentHealth * Math.min(0.015 * level, 0.15);

            // 새로운 체력을 직접 설정
            double newHealth = Math.max(0, targetCurrentHealth - additionalDamage);
            target.setHealth(newHealth);
        }
    }

    // 슈퍼차지 효과 적용
    private void applySuperChargeEffect(Player player, LivingEntity target) {
        if (target instanceof Player) {
            double superChargeDamage = 20.0; // 체력 10칸 고정 데미지 (10칸 = 20 HP)

            // 새로운 체력을 직접 설정
            double newHealth = Math.max(0, target.getHealth() - superChargeDamage);
            target.setHealth(newHealth);
        }
    }

    private void startSuperChargeTimer(Player player, double durationTicks) {
        BukkitRunnable timerTask = new BukkitRunnable() {
            double ticks = 0;
            boolean fireworkSoundPlayed = false; // 폭죽 소리 재생 여부 플래그

            @Override
            public void run() {
                if (ticks >= durationTicks) {
                    player.removeMetadata("super_charged", plugin);
                    clearSuperChargeTimer(player);
                    player.stopAllSounds(); // 슈퍼차지 시간 이후 자동으로 활시위를 놓게 함
                    cancel();
                } else {
                    if (player.isHandRaised()) {
                        if (!fireworkSoundPlayed) {
                            // 폭죽 소리 추가 (주변 플레이어 포함)
                            for (Player nearbyPlayer : Bukkit.getOnlinePlayers()) {
                                if (nearbyPlayer.getWorld().equals(player.getWorld()) &&
                                        nearbyPlayer.getLocation().distance(player.getLocation()) <= 50) { // 50블록 이내의 플레이어에게만 재생
                                    nearbyPlayer.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                                }
                            }
                            fireworkSoundPlayed = true; // 한 번 재생 후 플래그 업데이트
                        }

                        Location loc = player.getLocation();
                        for (int i = 0; i < 2; i++) { // 파티클 수를 반으로 줄임
                            double offsetX = (Math.random() - 0.5) * 0.6; // 오프셋 범위를 0.3칸까지 좁힘
                            double offsetY = (Math.random() - 0.5) * 0.7 + 0.5; // 오프셋 범위를 0.5에서 1.2까지 설정
                            double offsetZ = (Math.random() - 0.5) * 0.6;
                            player.getWorld().spawnParticle(Particle.FLAME, loc.add(offsetX, offsetY, offsetZ), 1, 0.05, 0.05, 0.05, 0.01);
                        }
                    }
                    ticks++;
                }
            }
        };

        player.setMetadata("super_charge_timer", new FixedMetadataValue(plugin, timerTask));
        player.setMetadata("super_charge_start_time", new FixedMetadataValue(plugin, System.currentTimeMillis()));
        timerTask.runTaskTimer(plugin, 0, 1);
    }

    private void clearSuperChargeTimer(Player player) {
        if (player.hasMetadata("super_charge_timer")) {
            for (MetadataValue value : player.getMetadata("super_charge_timer")) {
                if (value.getOwningPlugin().equals(plugin) && value.value() instanceof BukkitRunnable) {
                    ((BukkitRunnable) value.value()).cancel();
                }
            }
            player.removeMetadata("super_charge_timer", plugin);
        }
    }

    private void clearSuperChargeMetadata(Player player) {
        player.removeMetadata("super_charged", plugin);
        clearSuperChargeTimer(player);
        if (player.hasMetadata("super_charge_task")) {
            for (MetadataValue value : player.getMetadata("super_charge_task")) {
                if (value.getOwningPlugin().equals(plugin) && value.value() instanceof BukkitRunnable) {
                    ((BukkitRunnable) value.value()).cancel();
                }
            }
            player.removeMetadata("super_charge_task", plugin);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearSuperChargeMetadata(event.getPlayer());
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem == null || !isBow(newItem.getType())) {
            clearSuperChargeMetadata(player);
        }
    }

    private void applyFlameParticles(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isDead()) {
                    arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 3, 0.05, 0.05, 0.05, 0.01);
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private double getLevelFromLore(List<String> lore) {
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLUE + "적 체력에 비례한 추가 피해: ")) {
                // "% 기호 제거 및 값 파싱"
                String percentage = ChatColor.stripColor(line)
                        .replace("적 체력에 비례한 추가 피해: ", "")
                        .replace("%", "");
                double healthPercentage = Double.parseDouble(percentage) / 100.0;

                // 레벨 역계산 (healthPercentage = Math.min(0.015 * level, 0.15))
                if (healthPercentage <= 0.15) {
                    return healthPercentage / 0.015;
                }
            }
        }
        return 0; // 기본값
    }

    private static boolean isBow(Material material) {
        return material != null && material == Material.BOW;
    }

    public static void applyBowUpgrade(JavaPlugin plugin, Player player, ItemStack bow, int level) {
        if (bow == null || !isBow(bow.getType())) {
            return;
        }

        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        setItemLore(plugin, bow, level, successChance, failChance, downgradeChance, destroyChance);

        if (level == 10) {
            addSpecialAbility(bow, plugin);
            announceLegendaryBow(plugin, player, bow);
            setUnbreakable(bow, level); // 레벨 정보를 함께 전달
        }
    }

    private static void setItemLore(JavaPlugin plugin, ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "등급 하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");
            lore.add("");
            double healthPercentage = Math.min(0.015 * level, 0.15);
            lore.add(ChatColor.BLUE + "적 체력에 비례한 추가 피해: " + (healthPercentage * 100) + "%");

            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력: 슈퍼차지");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void addSpecialAbility(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(plugin, "special_ability");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    private static void announceLegendaryBow(JavaPlugin plugin, Player player, ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

        String message = ChatColor.GOLD + "전설의 " + itemName + "이 탄생하였습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
        }
    }

    private static void setUnbreakable(ItemStack item, int level) {
        if (level == 10) { // 레벨이 10인 경우에만 내구도를 무한으로 설정
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true); // 내구도 무한 설정
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE); // 툴팁에서 내구도 무한 숨기기
                item.setItemMeta(meta);
            }
        }
    }

    private static int calculateSuccessChance(int level) {
        return switch (level) {
            case 0 -> 90;
            case 1 -> 80;
            case 2 -> 70;
            case 3 -> 60;
            case 4 -> 50;
            case 5, 6, 7, 8, 9 -> 50;
            default -> 0;
        };
    }

    private static int calculateFailChance(int level) {
        return switch (level) {
            case 0 -> 10;
            case 1 -> 20;
            case 2 -> 30;
            case 3 -> 40;
            case 4 -> 50;
            case 5 -> 40;
            case 6 -> 35;
            case 7 -> 25;
            case 8 -> 10;
            case 9 -> 0;
            default -> 0;
        };
    }

    private static int calculateDowngradeChance(int level) {
        return switch (level) {
            case 0, 1, 2, 3, 4, 9 -> 0;
            case 5 -> 8;
            case 6 -> 10;
            case 7 -> 15;
            case 8 -> 20;
            default -> 0;
        };
    }

    private static int calculateDestroyChance(int level) {
        return switch (level) {
            case 0, 1, 2, 3, 4 -> 0;
            case 5 -> 2;
            case 6 -> 5;
            case 7 -> 10;
            case 8 -> 20;
            case 9 -> 50;
            default -> 0;
        };
    }
}