package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class HelmetUpgrade {

    public static void applyHelmetUpgrade(JavaPlugin plugin, Player player, ItemStack helmet, int level) {
        if (helmet == null || !isHelmet(helmet.getType())) {
            return; // 투구가 아니면 처리하지 않음
        }

        // 각 투구 종류별 방어, 방어 강도, 밀치기 저항 값 설정
        double[] attributes = getAttributesByMaterial(helmet.getType());
        double armorValue = attributes[0];
        double toughnessValue = attributes[1];
        double knockbackResistance = attributes[2];

        // 확률 설정
        int successChance = calculateSuccessChance(level);
        int failChance = calculateFailChance(level);
        int downgradeChance = calculateDowngradeChance(level);
        int destroyChance = calculateDestroyChance(level);

        // 아이템 메타 정보 업데이트
        setItemAttributes(helmet, armorValue, toughnessValue, knockbackResistance);

        // 강화 레벨에 따른 인첸트 추가
        addEnchantments(helmet, level);

        // 로레 설정
        setItemLore(helmet, level, successChance, failChance, downgradeChance, destroyChance, armorValue, toughnessValue, knockbackResistance);

        // 10강 투구의 특별한 능력 추가
        if (level == 10) {
            addSpecialAbility(helmet, plugin);
            addInfiniteDurability(helmet, plugin);
            announceLegendaryHelmet(plugin, player, helmet);
        }
    }

    private static double[] getAttributesByMaterial(Material material) {
        switch (material) {
            case LEATHER_HELMET:
                return new double[]{1, 0, 0}; // 방어: 1, 방어 강도: 0, 밀치기 저항: 0
            case CHAINMAIL_HELMET:
                return new double[]{2, 0, 0}; // 방어: 2, 방어 강도: 0, 밀치기 저항: 0
            case IRON_HELMET:
                return new double[]{2, 0, 0}; // 방어: 2, 방어 강도: 0, 밀치기 저항: 0
            case GOLDEN_HELMET:
                return new double[]{2, 0, 0}; // 방어: 2, 방어 강도: 0, 밀치기 저항: 0
            case DIAMOND_HELMET:
                return new double[]{3, 2, 0}; // 방어: 3, 방어 강도: 2, 밀치기 저항: 0
            case NETHERITE_HELMET:
                return new double[]{3, 3, 0.1}; // 방어: 3, 방어 강도: 3, 밀치기 저항: 1
            default:
                return new double[]{0, 0, 0}; // 기본값
        }
    }

    private static void addSpecialAbility(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "regeneration"), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
    }

    private static void addInfiniteDurability(ItemStack item, JavaPlugin plugin) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE); // '불멸' 플래그 숨김
            item.setItemMeta(meta);
        }
    }

    private static void announceLegendaryHelmet(JavaPlugin plugin, Player player, ItemStack item) {
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

    private static boolean isHelmet(Material material) {
        return switch (material) {
            case LEATHER_HELMET,
                 CHAINMAIL_HELMET,
                 IRON_HELMET,
                 GOLDEN_HELMET,
                 DIAMOND_HELMET,
                 NETHERITE_HELMET -> true;
            default -> false;
        };
    }

    private static void addEnchantments(ItemStack item, int level) {
        item.removeEnchantment(Enchantment.PROTECTION_PROJECTILE);
        item.removeEnchantment(Enchantment.PROTECTION_FIRE);
        item.removeEnchantment(Enchantment.PROTECTION_EXPLOSIONS);

        switch (level) {
            case 1 -> item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
            case 2 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
            }
            case 3 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 1);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 1);
            }
            case 4 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 1);
            }
            case 5 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 1);
            }
            case 6 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 2);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 2);
            }
            case 7 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 3);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 2);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 2);
            }
            case 8 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 3);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 3);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 2);
            }
            case 9 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 3);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 3);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 3);
            }
            case 10 -> {
                item.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
                item.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
                item.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
            }
        }
    }

    private static void setItemLore(ItemStack item, int level, int successChance, int failChance, int downgradeChance, int destroyChance, double armorValue, double toughnessValue, double knockbackResistance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GREEN + "성공 확률: " + successChance + "%");
            lore.add(ChatColor.YELLOW + "실패 확률: " + failChance + "%");
            lore.add(ChatColor.RED + "하락 확률: " + downgradeChance + "%");
            lore.add(ChatColor.DARK_RED + "파괴 확률: " + destroyChance + "%");

            if (level == 10) {
                lore.add(ChatColor.AQUA + "특수 능력: 재생");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }

    private static void setItemAttributes(ItemStack item, double armor, double toughness, double knockbackResistance) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);

            AttributeModifier armorModifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", armor, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD);
            AttributeModifier toughnessModifier = new AttributeModifier(UUID.randomUUID(), "generic.armorToughness", toughness, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD);
            AttributeModifier knockbackModifier = new AttributeModifier(UUID.randomUUID(), "generic.knockbackResistance", knockbackResistance, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HEAD);

            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, armorModifier);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, toughnessModifier);
            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, knockbackModifier);

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
    }
}