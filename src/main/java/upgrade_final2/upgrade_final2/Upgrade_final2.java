package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import upgrade_final2.upgrade_final2.UpgradeItem;

public final class Upgrade_final2 extends JavaPlugin {

    private ToolUpgrade toolUpgrade;

    private DragonKillReward dragonKillReward;
    



    private final Map<UUID, UUID> playerTeams = new HashMap<>();

    @Override
    public void onEnable() {

        getServer().getPluginManager().registerEvents(new DeathListener(), this);

        getServer().getPluginManager().registerEvents(new HelmetAbility(this), this);
        new ChestplateAbility(this);

        getServer().getPluginManager().registerEvents(new CrossbowUpgrade(this), this);

        new LeggingsAbility(this);
        Bukkit.getPluginManager().registerEvents(new BootUpgrade(this), this);

        getServer().getPluginManager().registerEvents(new SwordUpgrade(this), this);
        

        BowUpgrade bowUpgrade = new BowUpgrade(this);
        getServer().getPluginManager().registerEvents(bowUpgrade, this);

        TridentUpgrade tridentUpgrade = new TridentUpgrade(this);
        getServer().getPluginManager().registerEvents(tridentUpgrade, this);

        ShieldUpgrade.initialize(this); // ShieldUpgrade 초기화
        ShieldUpgrade shieldUpgrade = new ShieldUpgrade(this);
        getServer().getPluginManager().registerEvents(shieldUpgrade, this);

        Nether nether = new Nether(this);
        getServer().getPluginManager().registerEvents(nether, this);

        getServer().getPluginManager().registerEvents(new GrindstoneRename(this), this);

        dragonKillReward = new DragonKillReward(this);

        // 이벤트 등록
        getServer().getPluginManager().registerEvents(dragonKillReward, this);

        // 명령어 등록
        getCommand("controlend").setExecutor((sender, command, label, args) -> {
            if (!sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
                return false;
            }

            // 올바른 명령어 인수 확인
            if (args.length < 1 || (!args[0].equalsIgnoreCase("on") && !args[0].equalsIgnoreCase("off"))) {
                sender.sendMessage(ChatColor.RED + "사용법: /controlend <on|off>");
                return false;
            }

            // 엔더 월드 제어 처리
            if (args[0].equalsIgnoreCase("on")) {
                dragonKillReward.setEndAccessAllowed(true);
                sender.sendMessage(ChatColor.GREEN + "엔더 월드 출입이 활성화되었습니다.");
            } else if (args[0].equalsIgnoreCase("off")) {
                dragonKillReward.setEndAccessAllowed(false);
                sender.sendMessage(ChatColor.RED + "엔더 월드 출입이 비활성화되었습니다.");
            }

            return true;
        });

        getCommand("resetend").setExecutor((sender, command, label, args) -> {
            if (sender.isOp()) {
                dragonKillReward.resetEndWorld();
                sender.sendMessage("엔더 월드가 초기화되었습니다.");
                return true;
            }
            return false;
        });


        getServer().getPluginManager().registerEvents(new UpgradeHandler(this), this);
        new CustomRecipe(this);
        new CustomEnchanting(this);

        this.toolUpgrade = new ToolUpgrade(this);
        toolUpgrade.initializeSpecialAbilities();

        getLogger().info("Upgrade_final2 plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Upgrade_final2 plugin has been disabled!");
    }

    public Map<UUID, UUID> getPlayerTeams() {
        return playerTeams;
    }


}