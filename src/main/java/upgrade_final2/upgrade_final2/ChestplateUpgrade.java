package upgrade_final2.upgrade_final2;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
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
import java.util.Random;
import java.util.UUID;

public class ChestplateUpgrade implements Listener {

    private final JavaPlugin plugin;
    private final Random random;

    public ChestplateUpgrade(JavaPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
    }

    public static void applyChestplateUpgrade(JavaPlugin plugin, Player player, ItemStack chestplate, int level) {
        if (chestplate == null || !isChestplate(chestplate.getType())) {
            return; // 갑옷이 아니라면 처리하지 않음
        }

        // 각 갑옷 종류별 방어, 방어 강도, 밀치기 저항 값 설정
        double[] attributes = getAttributesByMaterial(chestplate.getType());
        double armorValue = attributes[0];
        double toughnessValue = attributes[1];
        double knockbackResistance = attributes[2];

        // 확률 설정
        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        // 새로운 체력 증가 계산
        double healthBonus = level * 2; // 강화 레벨당 체력 2 증가

        // 아이템 메타 정보 업데이트
        setItemAttributes(chestplate, armorValue, toughnessValue, knockbackResistance, healthBonus);

        // 로어 설정
        setItemLore(chestplate, level, successChance, failChance, downgradeChance, destroyChance, armorValue, toughnessValue, knockbackResistance, healthBonus);

        // 10강 갑옷의 특별한 능력 추가
        if (level == 10) {
            addSpecialAbility(chestplate, plugin); // 특수 능력 추가
            setUnbreakable(chestplate); // 내구도 무한 설정
            announceLegendaryChestplate(plugin, player, chestplate); // 전설의 갑옷 알림
        }
    }

    private static boolean isChestplate(Material material) {
        return switch (material) {
            case LEATHER_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> true;
            default -> false;
        };
    }

    private static double[] getAttributesByMaterial(Material material) {
        return switch (material) {
            case LEATHER_CHESTPLATE -> new double[]{3, 0, 0}; // 방어: 3, 방어 강도: 0, 밀치기 저항: 0
            case CHAINMAIL_CHESTPLATE -> new double[]{5, 0, 0}; // 방어: 5, 방어 강도: 0, 밀치기 저항: 0
            case IRON_CHESTPLATE -> new double[]{6, 0, 0}; // 방어: 6, 방어 강도: 0, 밀치기 저항: 0
            case GOLDEN_CHESTPLATE -> new double[]{5, 0, 0}; // 방어: 5, 방어 강도: 0, 밀치기 저항: 0
            case DIAMOND_CHESTPLATE -> new double[]{8, 2, 0}; // 방어: 8, 방어 강도: 2, 밀치기 저항: 0
            case NETHERITE_CHESTPLATE -> new double[]{8, 3, 0.1}; // 방어: 8, 방어 강도: 3, 밀치기 저항: 0.1
            default -> new double[]{0, 0, 0}; // 기본값
        };
    }

    private static void setItemAttributes(ItemStack item, double armor, double toughness, double knockbackResistance, double healthBonus) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);

            AttributeModifier armorModifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);
            AttributeModifier toughnessModifier = new AttributeModifier(UUID.randomUUID(), "generic.armorToughness", toughness, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);
            AttributeModifier knockbackModifier = new AttributeModifier(UUID.randomUUID(), "generic.knockbackResistance", knockbackResistance, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);
            AttributeModifier healthModifier = new AttributeModifier(UUID.randomUUID(), "generic.maxHealth", healthBonus, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.CHEST);

            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, toughnessModifier);
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackModifier);
            meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, healthModifier);

            item.setItemMeta(meta);
        }
    }

    private static void setItemLore(ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance, double armorValue, double toughnessValue, double knockbackResistance, double healthBonus) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");

            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력: 7% 확률로 공격 무효화");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void addSpecialAbility(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "damage_negation"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    private static void setUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); // '무적' 플래그를 숨김
            item.setItemMeta(meta);
        }
    }

    private static void announceLegendaryChestplate(JavaPlugin plugin, Player player, ItemStack item) {
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

    // 확률 계산 함수들
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

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 공격자가 Projectile(화살 등)인 경우도 이벤트 처리
        if (event.getEntity() instanceof Player player) {
            ItemStack chestplate = player.getInventory().getChestplate();
            if (chestplate != null && hasSpecialAbility(chestplate)) {
                // 7% 확률로 데미지 무효화
                if (random.nextDouble() < 0.07) {
                    event.setCancelled(true);
                    player.spigot().sendMessage(
                            ChatMessageType.ACTION_BAR,
                            TextComponent.fromLegacyText(ChatColor.AQUA + "공격이 무효화되었습니다."));;
                }
            }
        }
    }

    private boolean hasSpecialAbility(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "damage_negation"), PersistentDataType.BYTE);
    }
}