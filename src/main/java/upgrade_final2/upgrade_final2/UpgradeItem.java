package upgrade_final2.upgrade_final2;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class UpgradeItem {

    // 만약 plugin 인스턴스를 필요하지 않는다면, 제거할 수 있습니다.
    public static ItemStack getUpgradeStone(int quantity) {
        ItemStack upgradeStone = new ItemStack(Material.AMETHYST_SHARD, quantity);
        ItemMeta meta = upgradeStone.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "강화석");
            upgradeStone.setItemMeta(meta);
        }
        return upgradeStone;
    }

    public static ItemStack getDiamondItem() {
        ItemStack diamond = new ItemStack(Material.DIAMOND);
        ItemMeta meta = diamond.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "강화석 구매(다이아)");
            diamond.setItemMeta(meta);
        }
        return diamond;
    }

    public static ItemStack getExperienceItem() {
        ItemStack xpBottle = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = xpBottle.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "강화석 구매(경험치)");
            xpBottle.setItemMeta(meta);
        }
        return xpBottle;
    }

    public static ItemStack getAnvilItem() {
        ItemStack anvil = new ItemStack(Material.ANVIL);
        ItemMeta meta = anvil.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "강화모루");
            anvil.setItemMeta(meta);
        }
        return anvil;
    }
}