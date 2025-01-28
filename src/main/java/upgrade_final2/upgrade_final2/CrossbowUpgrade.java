package upgrade_final2.upgrade_final2;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class CrossbowUpgrade implements Listener {
    private final JavaPlugin plugin;

    public CrossbowUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 크로스보 강화 적용 메서드
     */
    /**
     * 크로스보 강화 적용 메서드
     */
    public static void applyCrossBowUpgrade(JavaPlugin plugin, Player player, ItemStack crossbow, int level) {
        if (crossbow == null || !isCrossbow(crossbow.getType())) {
            return;
        }

        ItemMeta meta = crossbow.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            int successChance = calculateSuccessChance(level);
            int failChance = calculateFailChance(level);
            int downgradeChance = calculateDowngradeChance(level);
            int destroyChance = calculateDestroyChance(level);

            // Lore 구성
            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "등급 하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");
            lore.add("");

            // 고정 추가 데미지 정보
            double additionalDamage = level * 0.5;
            lore.add(ChatColor.BLUE + "고정 추가 데미지: +" + additionalDamage); // 레벨당 0.5씩 증가

            // 레벨 10: 특별 능력
            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력: 레이저 샷");
            }

            meta.setLore(lore);
            crossbow.setItemMeta(meta);

            // 레벨 10: 전설의 아이템 알림 및 내구 무한 처리
            if (level == 10) {
                announceLegendaryBow(plugin, player, crossbow);
                setUnbreakable(crossbow, level);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 화살 공격을 확인
        if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            ItemStack crossbow = player.getInventory().getItemInMainHand();

            // 크로스보 여부 및 강화 검증
            if (isCrossbow(crossbow.getType()) && crossbow.hasItemMeta()) {
                ItemMeta meta = crossbow.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    double level = getLevelFromLore(meta.getLore());

                    // 대상이 플레이어일 경우만 데미지 적용
                    if (level > 0 && event.getEntity() instanceof Player target) {
                        // 추가 데미지 계산
                        double additionalDamage = level * 0.5; // 레벨당 0.5 고정 추가 데미지

                        // 체력 계산 후 재설정 (최소 0으로 제한)
                        double newHealth = target.getHealth() - additionalDamage;
                        if (newHealth < 0) {
                            newHealth = 0;
                        }
                        target.setHealth(newHealth);

                        if (level == 10) {
                            // 레벨 10: 발광 효과 적용
                            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10 * 20, 0)); // 10초 발광
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack crossbow = event.getBow(); // 사용된 무기 (쇠뇌) 가져오기
            if (isCrossbow(crossbow.getType()) && crossbow.hasItemMeta()) {
                ItemMeta meta = crossbow.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    double level = getLevelFromLore(meta.getLore());

                    if (level == 10) {
                        // 화살 속도 3배 증가
                        if (event.getProjectile() instanceof Arrow arrow) {
                            org.bukkit.util.Vector velocity = arrow.getVelocity(); // 현재 속도 가져오기
                            arrow.setVelocity(velocity.multiply(3.0)); // 3배 속도로 발사
                        }
                    }
                }
            }
        }
    }

    private static boolean isCrossbow(Material material) {
        return material != null && material == Material.CROSSBOW;
    }

    private static double getLevelFromLore(List<String> lore) {
        for (String line : lore) {
            if (line.startsWith(ChatColor.BLUE + "고정 추가 데미지: +")) {
                // "고정 추가 데미지: +"를 기준으로 값 파싱
                String damage = ChatColor.stripColor(line)
                        .replace("고정 추가 데미지: +", "");
                double additionalDamage = Double.parseDouble(damage);

                // 레벨 계산 (0.5씩 증가했으므로 역함수 적용)
                return additionalDamage / 0.5;
            }
        }
        return 0;
    }

    private static long getLastSpectralUse(ItemMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey("plugin", "last_spectral_use");
        return container.has(key, PersistentDataType.LONG) ? container.get(key, PersistentDataType.LONG) : 0;
    }

    private static void updateLastSpectralUse(ItemMeta meta, long currentTime) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey("plugin", "last_spectral_use");
        container.set(key, PersistentDataType.LONG, currentTime);
    }

    private static void setUnbreakable(ItemStack item, int level) {
        if (level == 10) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                item.setItemMeta(meta);
            }
        }
    }

    private static void announceLegendaryBow(JavaPlugin plugin, Player player, ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

        String message = ChatColor.GOLD + "축하합니다! 전설의 " + legendaryName + "이 탄생했습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
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
    };
}
