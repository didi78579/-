package upgrade_final2.upgrade_final2;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class GrindstoneRename implements Listener {

    private final JavaPlugin plugin;

    // GUI 제목
    private static final String GRINDSTONE_GUI_TITLE = ChatColor.AQUA + "작명 숫돌";

    // 플레이어가 이름을 설정할 대기 상태를 저장하는 맵
    private final HashMap<UUID, ItemStack> nameChangingPlayers = new HashMap<>();

    public GrindstoneRename(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onGrindstoneClick(PlayerInteractEvent event) {
        // 우클릭이 숫돌일 때만 반응
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.GRINDSTONE) {
            Player player = event.getPlayer();

            // GUI 생성
            Inventory renameGUI = Bukkit.createInventory(null, 9, GRINDSTONE_GUI_TITLE);

            // 중앙 슬롯(4번)에 숫돌 배치
            renameGUI.setItem(4, createGrindstone());

            // GUI 열기
            player.openInventory(renameGUI);

            // 기본 숫돌 UI 방지
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        Inventory customInventory = event.getView().getTopInventory();

        // GUI가 "작명 숫돌"인지 확인
        if (event.getView().getTitle().equals(GRINDSTONE_GUI_TITLE)) {
            event.setCancelled(true); // 기본 동작 방지

            int slot = event.getRawSlot(); // 클릭한 슬롯
            ItemStack clickedItem = event.getCurrentItem(); // 클릭된 아이템
            ItemStack guiItem = customInventory.getItem(4); // 중앙 슬롯의 아이템

            // 중앙 슬롯 클릭 처리
            if (clickedInventory != null && clickedInventory.equals(customInventory)) {
                if (slot == 4 && guiItem != null) {
                    // 숫돌은 이름 변경 불가
                    if (guiItem.getType() == Material.GRINDSTONE) {
                        player.sendMessage(ChatColor.RED + "숫돌의 이름은 변경할 수 없습니다!");
                        return;
                    }

                    // 좌클릭으로 이름 변경 시작
                    if (event.isLeftClick()) {
                        nameChangingPlayers.put(player.getUniqueId(), guiItem); // 이름 변경 대기 등록
                        customInventory.setItem(4, createGrindstone()); // 중앙 슬롯 초기화
                        player.closeInventory(); // GUI 닫기
                        player.sendMessage(ChatColor.GREEN + "변경할 이름을 채팅으로 입력하세요.");
                    }
                }
            }

            // 플레이어 인벤토리에서 중앙 슬롯으로 아이템 이동
            if (clickedInventory != null && clickedInventory.equals(player.getInventory())) {
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    // 중앙 슬롯에 넣을 숫돌 검사
                    if (guiItem != null && guiItem.getType() != Material.GRINDSTONE) {
                        player.sendMessage(ChatColor.RED + "중앙 슬롯에 이미 아이템이 있습니다.");
                    } else {
                        customInventory.setItem(4, clickedItem); // 숫돌 외 모든 아이템 추가 가능
                        player.getInventory().clear(event.getSlot()); // 인벤토리 슬롯 제거
                        player.sendMessage(ChatColor.GREEN + "아이템이 작명 슬롯으로 이동되었습니다.");
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 이름 변경 대기 상태 확인
        if (nameChangingPlayers.containsKey(playerId)) {
            event.setCancelled(true); // 채팅 취소
            String newName = event.getMessage(); // 입력된 이름
            ItemStack item = nameChangingPlayers.get(playerId); // 대기 상태의 아이템 가져오기

            // 숫돌 이름 변경 방지
            if (item.getType() == Material.GRINDSTONE) {
                player.sendMessage(ChatColor.RED + "숫돌의 이름은 변경할 수 없습니다!");
                return;
            }

            // 아이템 메타 데이터 가져오기
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 기존 이름 가져오기
                String oldName = meta.getDisplayName();
                String colorPrefix = ChatColor.RESET.toString(); // 기본 색상으로 초기화

                if (oldName != null && !oldName.isEmpty()) {
                    // 기존 이름의 색상 코드 추출
                    int colorCodeIndex = oldName.indexOf(ChatColor.COLOR_CHAR); // § 위치 탐색
                    if (colorCodeIndex != -1 && colorCodeIndex + 1 < oldName.length()) {
                        colorPrefix = oldName.substring(colorCodeIndex, colorCodeIndex + 2); // § + 색상 코드
                    }
                }

                // 기존 이름이 금색(ChatColor.GOLD)일 경우 접두사 추가
                if (colorPrefix.equals(ChatColor.GOLD.toString())) {
                    newName = "전설의 " + newName;
                }

                // 새 이름 적용 (기존 색상 유지)
                meta.setDisplayName(colorPrefix + newName); // 색상 + 새 이름
                item.setItemMeta(meta);

                // 완료 메시지
                player.sendMessage(ChatColor.AQUA + "아이템 이름이 '" + colorPrefix + newName + ChatColor.AQUA + "'(으)로 변경되었습니다!");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            }

            // 상태 초기화
            nameChangingPlayers.remove(playerId);
            player.getInventory().addItem(item); // 변경된 아이템 반환
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(GRINDSTONE_GUI_TITLE)) {
            Inventory customInventory = event.getInventory();
            ItemStack item = customInventory.getItem(4);

            if (item != null && item.getType() != Material.GRINDSTONE) {
                // GUI 종료 시 남은 아이템 반환
                Player player = (Player) event.getPlayer();
                player.getInventory().addItem(item);
            }
        }
    }

    /**
     * 작명용 숫돌 생성 메서드
     */
    private ItemStack createGrindstone() {
        ItemStack grindstone = new ItemStack(Material.GRINDSTONE);
        ItemMeta meta = grindstone.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "작명용 숫돌");
            grindstone.setItemMeta(meta);
        }

        return grindstone;
    }
}