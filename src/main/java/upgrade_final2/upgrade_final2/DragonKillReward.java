package upgrade_final2.upgrade_final2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.Random;

public class DragonKillReward implements Listener {

    private final JavaPlugin plugin;
    private boolean canTrigger = true; // 1시간 내 드래곤 사망 트리거 제한
    private boolean portalAlreadyActivated = false; // 엔드 차원문 트리거 제한
    private boolean endAccessAllowed = false; // 엔더 월드 출입 허용 여부
    private static final long ONE_HOUR_TICKS = 72L; // 1시간 = 3600 * 20틱
    private static final long TEN_MINUTES_TICKS = 12000L; // 10분 = 600 * 20틱

    public DragonKillReward(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // 엔더 월드 출입 허용 여부 설정 메서드
    public void setEndAccessAllowed(boolean allowed) {
        this.endAccessAllowed = allowed;
    }

    // 엔더 월드 출입 허용 여부 확인 메서드
    // 드래곤 죽음 이벤트
    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        // 엔더 드래곤이 죽었는지 확인
        if (event.getEntity().getType() == EntityType.ENDER_DRAGON && canTrigger) {
            canTrigger = false; // 1시간 동안 재작동 제한
            portalAlreadyActivated = false; // 포탈 활성화 초기화
            endAccessAllowed = false; // 엔더 월드 출입 금지

            // 드래곤 처치 메시지 알림
            Bukkit.broadcast(Component.text("드래곤이 처치되었습니다! 엔더 월드가 10분 후 닫힙니다.", NamedTextColor.GOLD));

            // 10분 타이머: 엔더 월드 초기화 및 플레이어 이동 처리
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleEndWorldClosure(); // 엔더 월드 닫힘 처리
                }
            }.runTaskLater(plugin, TEN_MINUTES_TICKS);

            // 1시간 후 다시 드래곤 처치 가능
            Bukkit.getScheduler().runTaskLater(plugin, () -> canTrigger = true, ONE_HOUR_TICKS);
        }
    }

    /**
     * 엔더 월드 닫힘 처리 메서드
     */
    private void handleEndWorldClosure() {
        World endWorld = Bukkit.getWorld("world_the_end");

        if (endWorld != null) {
            // 엔더 월드 내 모든 플레이어를 랜덤한 위치로 이동
            movePlayersToRandomLocation(endWorld);

            // 엔더 월드 초기화
            resetEndWorld();
        }

        endAccessAllowed = false; // 접근 불가 설정
    }

    private void movePlayersToRandomLocation(World endWorld) {
        // 오버월드(기본 월드) 가져오기
        World overworld = Bukkit.getWorld("world");
        if (overworld == null) {
            Bukkit.getLogger().warning("기본 오버월드('world')를 찾을 수 없습니다. 플레이어를 이동시킬 수 없습니다.");
            return;
        }

        for (Player player : endWorld.getPlayers()) {
            // 오버월드에서의 랜덤한 X, Z 좌표 생성 (범위: -5000 ~ 5000)
            double randomX = (Math.random() * 10000) - 5000;
            double randomZ = (Math.random() * 10000) - 5000;

            // X, Z 위치에서 오버월드의 가장 높은 블록 Y 좌표 가져옴
            int highestY = overworld.getHighestBlockYAt((int) randomX, (int) randomZ);

            // 안전한 위치 생성 (가장 높은 블록 위)
            Location randomLocation = new Location(overworld, randomX, highestY + 1, randomZ);

            // 플레이어를 랜덤 위치로 텔레포트
            player.teleport(randomLocation);

            // 플레이어에게 알림 메시지
            player.sendMessage(ChatColor.YELLOW + "오버월드의 랜덤한 위치로 텔레포트되었습니다: " +
                    ChatColor.AQUA + randomX + ", " + (highestY + 1) + ", " + randomZ);
        }
    }

    // 엔더 월드 초기화 메서드
    public void resetEndWorld() {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld != null) {
            // 월드 언로드
            Bukkit.unloadWorld(endWorld, false);

            // 디렉토리 삭제
            File endWorldFolder = endWorld.getWorldFolder();
            deleteDirectory(endWorldFolder);

            // 월드 로드
            WorldCreator worldCreator = new WorldCreator("world_the_end").environment(World.Environment.THE_END);
            World newEndWorld = worldCreator.createWorld();

            if (newEndWorld != null) {
                // 엔더 월드에서 인벤토리 킵 설정
                newEndWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
                Bukkit.broadcastMessage(ChatColor.GREEN + "엔더 월드가 초기화되었습니다.");
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "엔더 월드 초기화 중 문제가 발생했습니다.");
            }
        }
    }

    // 디렉토리 삭제 메서드
    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                deleteDirectory(subFile);
            }
        }
        file.delete();
    }




    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            // 엔더 월드 닫힘 상태일 때
            if (!endAccessAllowed) {
                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(ChatColor.RED + "엔더 월드로의 출입이 금지되어있습니다."));;
                event.setCancelled(true); // 전송 이벤트 취소
                return;
            }

            // 정상 이동 처리
            World endWorld = Bukkit.getWorld("world_the_end");
            if (endWorld != null) {
                double x = (Math.random() * 100) - 50;
                double z = (Math.random() * 100) - 50;
                double y = 250;
                Location randomLocation = new Location(endWorld, x, y, z);
                event.setTo(randomLocation);
                PotionEffect slowFalling = new PotionEffect(PotionEffectType.SLOW_FALLING, 30 * 20, 0);
                player.addPotionEffect(slowFalling);
            }
        }
    }


    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof EnderDragon) {
            EnderDragon enderDragon = (EnderDragon) event.getEntity();
            new BukkitRunnable() {
                @Override
                public void run() {
                    double originalHealth = enderDragon.getMaxHealth();
                    enderDragon.setMaxHealth(originalHealth * 10);
                    enderDragon.setHealth(enderDragon.getMaxHealth());
                }
            }.runTaskLater(plugin, 1L); // Allow for proper initialization delay
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EnderDragon) {
            EnderDragon dragon = (EnderDragon) event.getDamager();
            if (event.getEntity() instanceof Player) {
                event.setDamage(event.getDamage() * 1); // 데미지를 1배로 설정
            }
        }
    }

    // 엔드 포탈 활성화 이벤트
    @EventHandler
    public void onPortalActivation(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() == Material.END_PORTAL) {
            if (!portalAlreadyActivated) {
                portalAlreadyActivated = true;

                World endWorld = Bukkit.getWorld("world_the_end");
                if (endWorld != null) {
                    Location portalLocation = block.getLocation();
                    spawnLootItems(endWorld, portalLocation);

                    // 10분 후 엔더 월드 비활성화 및 초기화
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            endAccessAllowed = false;
                            resetEndWorld();
                        }
                    }.runTaskLater(plugin, TEN_MINUTES_TICKS);
                }
            }
        }
    }


    // 보상 드랍 처리
    private void spawnLootItems(World world, Location center) {
        Random random = new Random();
        int totalQuantity = 1000 + random.nextInt(1001);
        double fixedY = 100.0;

        for (int i = 0; i < totalQuantity; i++) {
            ItemStack itemStack = random.nextBoolean() ? getUpgradeStone(2) : getCustomPaper(2);
            double randomX = center.getX() + (random.nextInt(201) - 100);
            double randomZ = center.getZ() + (random.nextInt(201) - 100);
            Location spawnLocation = new Location(world, randomX, fixedY, randomZ);
            Item droppedItem = world.dropItem(spawnLocation, itemStack);
            if (droppedItem != null) {
                droppedItem.setGlowing(true);
            }
        }
    }

    // 강화석 생성
    private ItemStack getUpgradeStone(int quantity) {
        ItemStack stone = new ItemStack(Material.AMETHYST_SHARD, quantity);
        ItemMeta meta = stone.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("강화석").color(NamedTextColor.LIGHT_PURPLE));
            stone.setItemMeta(meta);
        }
        return stone;
    }

    // 마법 주문서 생성
    private ItemStack getCustomPaper(int quantity) {
        ItemStack paper = new ItemStack(Material.ECHO_SHARD, quantity);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("마석")
                    .color(NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false)); // 이탤릭체 비활성화
            paper.setItemMeta(meta);
        }
        return paper;
    }
}