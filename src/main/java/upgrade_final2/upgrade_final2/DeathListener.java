package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class DeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 소실 저주가 있는 아이템을 인벤토리에서 제거
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (hasCurseOfVanishing(item)) {
                player.getInventory().setItem(i, null); // 해당 슬롯의 아이템 삭제
                Bukkit.getLogger().info("소실 저주 아이템 제거됨: " + item.getType());
            }
        }
    }

    // 소실 저주가 있는지 확인
    private boolean hasCurseOfVanishing(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        return item.getEnchantments().containsKey(Enchantment.VANISHING_CURSE);
    }
}