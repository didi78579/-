package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SwordUpgrade implements Listener {

    private final JavaPlugin plugin;

    public SwordUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void applySwordUpgrade(JavaPlugin plugin, Player player, ItemStack item, int level) {
        if (item == null || !isSword(item.getType())) {
            return; // 검이 아니면 처리하지 않음
        }

        // 확률 설정
        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);


        double baseAttackDamage = calculateBaseAttackDamage(item.getType());
        double attackDamage = baseAttackDamage; // 공격력은 그대로 유지
        double attackSpeed = calculateAttackSpeed(level); // 공격 속도만 증가

        // 아이템 메타 정보 업데이트
        setItemAttributes(item, attackDamage, attackSpeed);

        // 로어 설정
        setItemLore(item, level, successChance, failChance, downgradeChance, destroyChance, attackDamage, attackSpeed);

        // 10강 검의 특별한 능력 추가
        if (level == 10) {
            addSpecialAbility(item, plugin);
            addInfiniteDurability(item);
            announceLegendaryWeapon(plugin, player, item);
        }
    }

    private static boolean isSword(Material material) {
        return switch (material) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    private static void setItemAttributes(ItemStack item, double attackDamage, double attackSpeed) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 기존 공격력과 속도 제거
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);

            // 새로운 공격력 설정
            AttributeModifier attackDamageModifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.attackDamage",
                    attackDamage,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND
            );

            // 새로운 공격 속도 설정
            AttributeModifier attackSpeedModifier = new AttributeModifier(
                    UUID.randomUUID(),
                    "generic.attackSpeed",
                    attackSpeed,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlot.HAND
            );

            // 공격력과 속도 애트리뷰트 추가
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, attackDamageModifier);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeedModifier);

            item.setItemMeta(meta);
        }
    }

    private static void setItemLore(ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance, double attackDamage, double attackSpeed) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");

            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력:무적시간 무시");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void addSpecialAbility(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "ignore_invulnerability"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    private static void addInfiniteDurability(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); // '불멸' 플래그 숨김
            item.setItemMeta(meta);
        }
    }

    private static void announceLegendaryWeapon(JavaPlugin plugin, Player player, ItemStack item) {
        String itemName = item.getItemMeta() != null && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().name();
        String legendaryName = "전설의 " + itemName;

        // 모든 플레이어에게 메시지 전송
        String message = ChatColor.GOLD + "전설의 " + itemName + "이 탄생하였습니다!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }

        // 플레이어를 위한 소리 재생
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f); // 웅장한 소리 재생
        }

        // 아이템 이름 변경
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + legendaryName);
            item.setItemMeta(meta);
        }
    }

    private static double calculateBaseAttackDamage(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> 4.0;
            case STONE_SWORD -> 5.0;
            case IRON_SWORD -> 6.0;
            case GOLDEN_SWORD -> 4.0;
            case DIAMOND_SWORD -> 7.0;
            case NETHERITE_SWORD -> 8.0;
            default -> 0.0;
        };
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

    private static double calculateAttackSpeed(int level) {
        return (level * 0.3); // 예시 계산법
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 공격자가 플레이어인지 확인
        if (event.getDamager() instanceof Player damager && event.getEntity() instanceof Player victim) {
            ItemStack itemInHand = damager.getInventory().getItemInMainHand();

            // 공격자의 아이템이 특별한 능력을 가졌는지 확인
            if (hasSpecialAbility(itemInHand)) {
                // 1틱 뒤 무적 시간 초기화
                Bukkit.getScheduler().runTask(plugin, () -> victim.setNoDamageTicks(0));
            }
        }
    }

    private boolean hasSpecialAbility(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "ignore_invulnerability"), PersistentDataType.BYTE);
    }
}
