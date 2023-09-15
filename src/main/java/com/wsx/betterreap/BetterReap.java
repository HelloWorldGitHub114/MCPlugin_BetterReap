package com.wsx.betterreap;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class BetterReap extends JavaPlugin implements Listener {

    private final Map<String,String> cropsMap = new HashMap<>();
    private final Set<String> hoesSet = new HashSet<>();

    @Override
    public void onEnable() {
        loadConfigValues();
        getServer().getPluginManager().registerEvents(this, this);
        this.getLogger().info(ChatColor.GREEN+"BetterReap loading...");
    }

    @Override
    public void onDisable(){
        this.getLogger().info(ChatColor.RED+"BetterReap disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length==1&&args[0].equals("reload")){
            loadConfigValues();
            sender.sendMessage(ChatColor.GREEN+"BetterReap reload.");
        }else{
            sender.sendMessage(ChatColor.DARK_RED+"wrong command.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>(List.of("reload"));
            result.removeIf(s -> !s.contains(args[0].toLowerCase(Locale.ROOT)));
            return result;
        } else return null;
    }

    private void loadConfigValues() {
        cropsMap.clear();
        hoesSet.clear();
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        //读取crops的数据
        List<Map<?, ?>> cropsConfigList = getConfig().getMapList("crops");

        for (Map<?, ?> crop : cropsConfigList) {
            String block = (String) crop.get("block");
            String seed = (String) crop.get("seed");
            cropsMap.put(block,seed);
        }

        //读取hoes的数据
        hoesSet.addAll(config.getStringList("hoes"));

        getServer().getLogger().info(ChatColor.RED+"cropMap:");
        for (Map.Entry<String,String> crop: cropsMap.entrySet()
        ) {
            this.getLogger().info(crop.toString());
        }
        getServer().getLogger().info(ChatColor.RED+"hoeList:");
        for (String hoe: hoesSet
        ) {
            this.getLogger().info(hoe);
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        //如果右击
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
            //手上为锄
            String ItemName = event.getPlayer().getInventory().getItemInMainHand().getType().getKey().toString();
            if(hoesSet.contains(ItemName)){
                //点击的是作物
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null){
                    BlockData blockData = clickedBlock.getBlockData();
                    String BlockName = clickedBlock.getType().getKey().toString();
                    if(cropsMap.containsKey(BlockName)){
                        //判断是否为农作物
                        if(blockData instanceof Ageable blockDataAgeable){
                            //成熟
                            if(blockDataAgeable.getAge() == blockDataAgeable.getMaximumAge()){
                                //设置作物为初始态
                                blockDataAgeable.setAge(0);
                                //缓存数据，之后恢复
                                String blockDataBuffer = blockDataAgeable.getAsString();
                                //摧毁方块
                                event.getPlayer().breakBlock(clickedBlock);
                                String cropSeed = cropsMap.get(BlockName);
                                //没找到的标志
                                boolean flag = true;
                                //遍历查找背包内种子,注意这里物品可能为null导致bug
                                for (ItemStack item: event.getPlayer().getInventory()
                                     ) {
                                    if (item != null && cropSeed.equals(item.getType().getKey().toString())) {
                                        //找到了，成功消耗种子
                                        item.setAmount(item.getAmount() - 1);
                                        clickedBlock.setBlockData(getServer().createBlockData(blockDataBuffer));
                                        flag = false;
                                        break;
                                    }
                                }
                                //没找到
                                if(flag) event.getPlayer().sendMessage(ChatColor.RED+cropSeed+" not found.");
                            }
                        }
                        else{
                            event.getPlayer().sendMessage(ChatColor.DARK_RED+"This is not a crop! Wrong config.");
                        }
                    }
                }
            }
        }
    }
}
