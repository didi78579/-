package upgrade_final2.upgrade_final2;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AbilityWaterBlock {

    private final Player player;
    private final JavaPlugin plugin;

    public AbilityWaterBlock(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
    }

    public void startWater() {
        Location blockLocation = player.getLocation().subtract(0, 0, 0);
        Block block = blockLocation.getBlock();

        // 현재 블록 상태 저장
        Material originalMaterial = block.getType();
        BlockData originalData = block.getBlockData();

        // 물 블록 설정
        block.setType(Material.WATER);

        // 2초 후 블록 원래 상태로 복구
        new BukkitRunnable() {
            @Override
            public void run() {
                block.setType(originalMaterial);
                block.setBlockData(originalData);
            }
        }.runTaskLater(plugin, 2 * 20L); // 2초 후 실행
    }
}