package upgrade_final2.upgrade_final2;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public class UpgradableItems {

    // 강화 가능한 모든 아이템을 정의합니다.
    private static final Set<Material> upgradableItems = EnumSet.of(
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
            Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.LEATHER_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.CHAINMAIL_HELMET,
            Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.LEATHER_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE,
            Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS, Material.LEATHER_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.CHAINMAIL_LEGGINGS,
            Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS, Material.LEATHER_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.CHAINMAIL_BOOTS,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.FISHING_ROD, Material.CROSSBOW,
            Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE
    );

    public static boolean isUpgradable(Material type) {
        return upgradableItems.contains(type);
    }

    public static boolean isSword(Material type) {
        return switch(type) {
            case DIAMOND_SWORD, NETHERITE_SWORD, WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD -> true;
            default -> false;
        };
    }

    public static boolean isPickaxe(Material type) {
        return switch(type) {
            case DIAMOND_PICKAXE, NETHERITE_PICKAXE, WOODEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, GOLDEN_PICKAXE -> true;
            default -> false;
        };
    }

    public static boolean isShovel(Material type) {
        return switch(type) {
            case DIAMOND_SHOVEL, NETHERITE_SHOVEL, WOODEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, GOLDEN_SHOVEL -> true;
            default -> false;
        };
    }

    public static boolean isAxe(Material type) {
        return switch(type) {
            case DIAMOND_AXE, NETHERITE_AXE, WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE -> true;
            default -> false;
        };
    }

    public static boolean isHoe(Material type) {
        return switch(type) {
            case DIAMOND_HOE, NETHERITE_HOE, WOODEN_HOE, STONE_HOE, IRON_HOE, GOLDEN_HOE -> true;
            default -> false;
        };
    }

    public static boolean isFISHING_ROD(Material type) {
        return type == Material.FISHING_ROD;
    }

    public static boolean isCrossBow(Material type) {
        return type == Material.CROSSBOW;
    }


    public static boolean isHelmet(Material type) {
        return switch(type) {
            case DIAMOND_HELMET, NETHERITE_HELMET, LEATHER_HELMET, IRON_HELMET, GOLDEN_HELMET, CHAINMAIL_HELMET -> true;
            default -> false;
        };
    }

    public static boolean isChestplate(Material type) {
        return switch(type) {
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE, LEATHER_CHESTPLATE, IRON_CHESTPLATE, GOLDEN_CHESTPLATE, CHAINMAIL_CHESTPLATE -> true;
            default -> false;
        };
    }

    public static boolean isLeggings(Material type) {
        return switch(type) {
            case DIAMOND_LEGGINGS, NETHERITE_LEGGINGS, LEATHER_LEGGINGS, IRON_LEGGINGS, GOLDEN_LEGGINGS, CHAINMAIL_LEGGINGS -> true;
            default -> false;
        };
    }

    public static boolean isBoots(Material type) {
        return switch(type) {
            case DIAMOND_BOOTS, NETHERITE_BOOTS, LEATHER_BOOTS, IRON_BOOTS, GOLDEN_BOOTS, CHAINMAIL_BOOTS -> true;
            default -> false;
        };
    }

    public static boolean isBow(Material type) {
        return type == Material.BOW;
    }

    public static boolean isTrident(Material type) {
        return type == Material.TRIDENT;
    }

    public static boolean isShield(Material type) {
        return type == Material.SHIELD;
    }

    public static boolean isTool(Material type) {
        return isPickaxe(type) || isShovel(type) || isAxe(type) || isHoe(type) || isFISHING_ROD(type);
    }

}