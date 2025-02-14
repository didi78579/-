package world.world;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.*;
import java.util.logging.Logger;

import world.world.Family;

public class BeaconMain extends JavaPlugin implements Listener {
    private BanManager banManager;
    private static final int BEACON_EFFECT_RADIUS = 100;
    private static Set<String> allUsers = new HashSet<>();
    private PlayerReviveManager reviveManager;
    private FamilyGUIHandler familyGuiHandler;
    private ScoreboardManager scoreboardManager;
    private Scoreboard scoreboard;
    private FamilyManager familyManager;
    private Map<String, Location> playerLastTpLocations = new HashMap<>();
    private Set<String> messageSentPlayers = new HashSet<>();
    
    @Override
    public void onEnable() {
        // 필요한 매니저 초기화 (가문 매니저, 부활 매니저 등)
        familyManager = new FamilyManager(this);
        banManager = new BanManager();
        reviveManager = new PlayerReviveManager(this);
        scoreboardManager = getServer().getScoreboardManager();
        scoreboard = scoreboardManager.getMainScoreboard();

        PluginManager pm = getServer().getPluginManager();

        // MasterCompassManager 초기화 및 관련 이벤트 등록
        MasterCompassManager masterCompassManager = new MasterCompassManager(this, familyManager);
        pm.registerEvents(new MasterCompassListener(masterCompassManager), this);
        pm.registerEvents(new MasterCompassDeathListener(masterCompassManager), this);

        // FamilyGUIHandler는 한 번만 생성 및 등록
        Logger logger = this.getLogger();
        familyGuiHandler = new FamilyGUIHandler(this, reviveManager, familyManager, logger);
        pm.registerEvents(familyGuiHandler, this); // GUI 핸들러 등록

        // 기타 이벤트 등록
        pm.registerEvents(new EventListener(), this);
        pm.registerEvents(new PlayerJoinListener(this, familyManager, reviveManager), this);
        pm.registerEvents(new BeaconBlockEventHandler(this, familyGuiHandler, familyManager, reviveManager), this);
        pm.registerEvents(new ChatPermissionManager(), this);

        // 명령어 등록
        this.getCommand("getbeacon").setExecutor(this);
        this.getCommand("tpbeacon").setExecutor(this);
        this.getCommand("start").setExecutor(new StartGameCommand());
        this.getCommand("family").setExecutor(new FamilyCommand(familyManager));

        // 게임 규칙 핸들러 초기화
        new GameRuleHandler(this);

        // 비콘 효과를 5초 주기로 적용
        new BukkitRunnable() {
            @Override
            public void run() {
                applyBeaconEffects();
            }
        }.runTaskTimer(this, 0, 20 * 5); // 5초 주기

        // FamilyGUIHandler 추가 기능 (업데이트 등)
        familyGuiHandler.startGiftBoxUpdater();

        // 비활성화 설정
        disableAchievements();
        disableCoordinates();
    }

    @Override
    public void onDisable() {
        if (familyManager != null) {
            familyManager.saveFamilies(); // 가문 데이터 저장
            getLogger().info("Families saved!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (cmd.getName().equalsIgnoreCase("getbeacon")) {
                player.getInventory().addItem(new ItemStack(Material.BEACON, 1));
                return true;
            } else if (cmd.getName().equalsIgnoreCase("tpbeacon")) {
                FamilyManager.Family family = FamilyManager.getFamily(player.getName());
                if (family != null) {
                    Location beaconLocation = family.getBeaconLocation();
                    if (beaconLocation != null) {
                        beaconLocation.setX(beaconLocation.getX() - 1);
                        player.teleport(beaconLocation);
                        beaconLocation.setX(beaconLocation.getX() + 1);

                        playerLastTpLocations.put(player.getName(), beaconLocation);

                    } else {
                        player.sendMessage(ChatColor.RED + "가문의 비콘 위치를 찾지 못했습니다.");
                    }
                } else {
                    player.sendMessage(ChatColor.GOLD + "가문에 속하지 않았습니다.");
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (isBeaconBaseBlock(block) || isPartOfBeaconBase(block)) {
                blockIterator.remove();
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player killer = player.getKiller();

            banManager.banPlayer(player.getUniqueId(), 3600000L);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String username = event.getPlayer().getName();
        addUser(username);

        FamilyManager.Family family = FamilyManager.getFamily(username);
        if (family != null) {
            Player player = event.getPlayer();
            ChatColor teamColor = family.getLeaderColor();
            setTeamColor(player, teamColor);
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // 플레이어의 가문 정보 가져오기
        FamilyManager.Family family = FamilyManager.getFamily(player.getName());

        if (family != null) {
            Location beaconLocation = family.getBeaconLocation();

            if (beaconLocation != null) {
                Location respawnLocation = beaconLocation.clone();
                respawnLocation.setX(respawnLocation.getX() - 1); // 위치 조정

                // 배리어 블록 확인
                Location belowLocation = respawnLocation.clone().add(0, -1, 0); // 아래 블록
                if (belowLocation.getBlock().getType() != Material.BARRIER) {
                    event.setRespawnLocation(respawnLocation); // 배리어가 아닐 때 리스폰 위치 설정
                    playerLastTpLocations.put(player.getName(), beaconLocation);
                }
            }
        }
    }



    private void applyBeaconEffects() {
        getServer().getWorlds().forEach(world -> {
            world.getPlayers().forEach(player -> {
                if (FamilyManager.hasFamily(player.getName())) {
                    Location playerLocation = player.getLocation();
                    FamilyManager.Family playerFamily = FamilyManager.getFamily(player.getName());
                    if (playerFamily != null) {
                        Location beaconLocation = playerFamily.getBeaconLocation();
                        if (beaconLocation != null) {
                            // 월드가 동일한지 확인
                            if (playerLocation.getWorld().equals(beaconLocation.getWorld())) {
                                double distance = playerLocation.distance(beaconLocation);
                                if (distance <= BEACON_EFFECT_RADIUS) {
                                    for (Player otherPlayer : world.getPlayers()) {
                                        if (otherPlayer.equals(player)) {
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, true, false));
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 120, 1, true, false));
                                            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 120, 1, true, false));
                                        } else {
                                            FamilyManager.Family otherPlayerFamily = FamilyManager.getFamily(otherPlayer.getName());
                                            if (otherPlayerFamily != null && !otherPlayerFamily.equals(playerFamily)) {
                                                Location otherPlayerLocation = otherPlayer.getLocation();
                                                // 다른 플레이어도 같은 월드인지 확인
                                                if (beaconLocation.getWorld().equals(otherPlayerLocation.getWorld())
                                                        && otherPlayerLocation.distance(beaconLocation) <= BEACON_EFFECT_RADIUS) {
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 120, 1, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 120, 0, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 120, 0, true, false));
                                                    otherPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 120, 1, true, false));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        });
    }

    public void setTeamColor(Player player, ChatColor teamColor) {
        if (teamColor != null && player != null) {
            Team team = scoreboard.getTeam(player.getName());
            if (team == null) {
                team = scoreboard.registerNewTeam(player.getName());
            }
            team.setColor(teamColor);
            team.addEntry(player.getName());
        }
    }

    private boolean isBeaconBaseBlock(Block block) {
        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation != null) {
                for (int x = -1; x <= 1; x++) {
                    for (int z = -1; z <= 1; z++) {
                        Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                        if (baseBlockLocation.getBlock().equals(block)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 신호기 기반 블록인지 확인
        if (isBeaconBaseBlock(block)) {
            event.setCancelled(true); // 이벤트 취소
            player.sendMessage(ChatColor.RED + "신호기 기반 블록은 파괴할 수 없습니다!");
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        List<Block> pushedBlocks = event.getBlocks();

        for (Block block : pushedBlocks) {
            // 신호기 혹은 신호기 기반 블록인지 확인
            if (isBeaconOrBase(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> retractedBlocks = event.getBlocks();

        for (Block block : retractedBlocks) {
            // 신호기 혹은 신호기 기반 블록인지 확인
            if (isBeaconOrBase(block)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    private boolean isBeaconOrBase(Block block) {
        if (block.getType() == Material.BEACON) {
            return true; // 신호기인지 확인
        }

        // 신호기 기반 블록인지 확인
        for (FamilyManager.Family family : familyManager.getAllFamilies()) {
            if (family != null && family.getBeaconLocation() != null) {
                Location beaconLocation = family.getBeaconLocation();
                if (isPartOfBeaconBase(block, beaconLocation)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isPartOfBeaconBase(Block block, Location beaconLocation) {
        if (beaconLocation == null) {
            return false;
        }

        // 신호기 아래의 기반 블록 확인 (3x3 영역)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location baseBlockLocation = beaconLocation.clone().add(x, -1, z);
                if (baseBlockLocation.getBlock().equals(block)) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean isPartOfBeaconBase(Block block) {
        for (FamilyManager.Family family : FamilyManager.getAllFamilies()) {
            Location beaconLocation = family.getBeaconLocation();
            if (beaconLocation != null) {
                for (int y = -1; y >= -BEACON_EFFECT_RADIUS; y--) {
                    Location baseBlockLocation = beaconLocation.clone().add(0, y, 0);
                    if (baseBlockLocation.getBlock().equals(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addUser(String username) {
        allUsers.add(username);
    }

    private void disableAchievements() {
        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        }
    }

    private void disableCoordinates() {
        for (Player player : getServer().getOnlinePlayers()) {
            player.setPlayerListName(player.getName());
            player.setPlayerListHeaderFooter("", "");
        }
    }
}