package com.wsx.betterreap;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BetterReap extends JavaPlugin implements Listener {

    private Map<NamespacedKey, NamespacedKey> cropsMap = new HashMap<>();
    private Set<NamespacedKey> hoesSet = new HashSet<>();
    private String permission = null;
    private boolean useLore = false;
    private String chainLore = null;
    private int defaultChainNum = 0;
    private int maxChainNum = 0;
    private int boneMealChainNum = 0;
    private int algorithm = 0;
    private List<World> worldBlackList = new ArrayList<>();



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
            sender.sendMessage(ChatColor.GREEN+"BetterReap has reloaded.");
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
        worldBlackList.clear();
        // 读取配置文件
        File configFile = new File(getDataFolder(), "config.yml");
        // 文件不存在就用默认文件
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        // 加载配置文件
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        //读取crops的数据
        List<Map<?, ?>> cropsConfigList = config.getMapList("crops");

        for (Map<?, ?> crop : cropsConfigList) {
            NamespacedKey block = NamespacedKey.fromString((String)crop.get("block"));
            NamespacedKey seed = NamespacedKey.fromString((String)crop.get("seed"));
            cropsMap.put(block,seed);
        }

        //读取hoes的数据
        for (String hoe: config.getStringList("hoes")
             ) {
            hoesSet.add(NamespacedKey.fromString(hoe));
        }

        //读取连锁相关数据
        permission = config.getString("permission");
        useLore = config.getBoolean("useLore");
        chainLore = config.getString("chainLore");
        defaultChainNum = config.getInt("defaultChainNum");
        maxChainNum = config.getInt("maxChainNum");
        boneMealChainNum = config.getInt("boneMealChainNum");
        algorithm = config.getInt("algorithm");
        for (String worldName: config.getStringList("worldBlackList")
             ) {
            worldBlackList.add(getServer().getWorld(worldName));
        }

        //输出配置文件内容，测试用
        getServer().getLogger().info(ChatColor.RED+"cropMap:");
        for (Map.Entry<NamespacedKey, NamespacedKey> crop: cropsMap.entrySet()
        ) {
            getLogger().info(crop.toString());
        }
        getServer().getLogger().info(ChatColor.RED+"hoeList:");
        for (NamespacedKey hoe: hoesSet
        ) {
            getLogger().info(hoe.toString());
        }
        getLogger().info(ChatColor.RED+"permission:"+permission);
        getLogger().info(ChatColor.RED+"useLore:"+useLore);
        getLogger().info(ChatColor.RED+"chainLore:"+chainLore);
        getLogger().info(ChatColor.RED+"defaultChainNum:"+defaultChainNum);
        getLogger().info(ChatColor.RED+"maxChainNum:"+maxChainNum);
        getLogger().info(ChatColor.RED+"boneMealChainNum:"+boneMealChainNum);
        getLogger().info(ChatColor.RED+"algorithm:"+algorithm);
        getServer().getLogger().info(ChatColor.RED+"worldBlackList:");
        for (World world: worldBlackList
             ) {
            getLogger().info(ChatColor.RED+world.getName());
        }
    }


    static class findConnectedBlocks{

        private static int chainNum = 0;
        public static Set<Block> get(Block startBlock,int maxChainNum, int algorithm) {
            Set<Block> connectedBlocks = new LinkedHashSet<>();// 有序，插入和查找快
            chainNum = maxChainNum;
            if (startBlock == null || chainNum <= 0) {
                return connectedBlocks;
            }

            Material targetMaterial = startBlock.getType();

            if(algorithm == 0)
                bfs(startBlock, targetMaterial, connectedBlocks);// dfs搜索方块
            else
                dfs(startBlock, targetMaterial, connectedBlocks);// bfs搜索方块

            return connectedBlocks;
        }

        private static void dfs(Block block, Material targetMaterial, Set<Block> connectedBlocks) {
            if (connectedBlocks.size() >= chainNum) {
                return;
            }

            //注意这里要判断重复,set自身去重但是其它操作仍然需要不重复
            if (block.getType() == targetMaterial & !connectedBlocks.contains(block)) {
                connectedBlocks.add(block);

                if (connectedBlocks.size() >= chainNum) {
                    return;
                }

                for (BlockFace face : BlockFace.values()) {
                    Block relativeBlock = block.getRelative(face);
                    dfs(relativeBlock, targetMaterial, connectedBlocks);
                }
            }
        }

        private static void bfs(Block block, Material targetMaterial, Set<Block> connectedBlocks){
            Queue<Block> queue = new LinkedList<>();
            queue.offer(block);
            connectedBlocks.add(block);

            while (!queue.isEmpty() && connectedBlocks.size() < chainNum) {
                Block currentBlock = queue.poll();

                for (BlockFace face : BlockFace.values()) {
                    Block relativeBlock = currentBlock.getRelative(face);

                    //注意这里要判断重复，因为queue不能去重
                    if (relativeBlock.getType() == targetMaterial && !connectedBlocks.contains(relativeBlock)) {
                        connectedBlocks.add(relativeBlock);
                        queue.offer(relativeBlock);

                        if (connectedBlocks.size() >= chainNum) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private int getMaxChainNum(Player player){
        if(useLore){
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            // 注意可能产生的null问题，要先判断有没有，再获取值
            if(itemInHand.hasItemMeta() && itemInHand.getItemMeta().hasLore()){
                // 正则表达式，匹配 "chainNum:数字"
                Pattern pattern = Pattern.compile(chainLore + ":(\\d+)");
                for (String itemLore: itemInHand.getItemMeta().getLore()
                ) {
                    Matcher matcher = pattern.matcher(itemLore);
                    if(matcher.find()){
                        // 如果匹配到正则表达式则获取匹配数字返回
                        return Math.min(maxChainNum,Integer.parseInt(matcher.group(1)));
                    }
                }
            }
        }
        // 不启用useLore或者没找到Lore，直接返回默认连锁值
        return defaultChainNum;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // 在黑名单世界禁止插件功能
        if(worldBlackList.contains(player.getWorld())){
            player.sendMessage(ChatColor.DARK_RED+"this world is not allowed to use BetterReap.");
            return;
        }
        //没有权限禁止功能
        if(!player.hasPermission(permission)){
            player.sendMessage(ChatColor.DARK_RED+"you are not allowed to use BetterReap.");
            return;
        }

        //如果右击
        if(event.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
            //手上为锄
            Block clickedBlock = event.getClickedBlock();
            if(hoesSet.contains(player.getInventory().getItemInMainHand().getType().getKey())){
                //点击的是作物
                if (clickedBlock != null && cropsMap.containsKey(clickedBlock.getType().getKey())) {
                    if (player.isSneaking()) {
                        //潜行使用连锁
                        for (Block block : findConnectedBlocks.get(clickedBlock, getMaxChainNum(player), algorithm)) {
                            blockReap(block, player);
                        }
                    } else {
                        blockReap(clickedBlock, player);
                    }
                }
            } else if (player.getInventory().getItemInMainHand().getType().equals(Material.BONE_MEAL)) {
                //如果手上拿着骨粉且潜行，触发连锁
                if (clickedBlock != null && player.isSneaking() && cropsMap.containsKey(clickedBlock.getType().getKey())) {
                    for (Block block : findConnectedBlocks.get(clickedBlock, boneMealChainNum, algorithm)) {
                        //被点击的方块本身就会应用骨粉效果，跳过
                        if(block != clickedBlock)
                            blockBoneMeal(block, player);
                    }
                }
            }
        }
    }

    private ItemStack blockReapCache = null;
    private void blockReap(Block block,Player player){
        //判断是否为农作物
        if(block.getBlockData() instanceof Ageable blockDataAgeable){
            //成熟
            if(blockDataAgeable.getAge() == blockDataAgeable.getMaximumAge()){
                NamespacedKey cropSeed = cropsMap.get(block.getType().getKey());
                //设置作物为初始态
                blockDataAgeable.setAge(0);
                //缓存数据，之后恢复
                String blockDataBuffer = blockDataAgeable.getAsString();
                //摧毁方块
                player.breakBlock(block);
                if(blockReapCache != null && blockReapCache.getType().getKey().equals(cropSeed)){
                    //有缓存直接用缓存
                    ItemStack item = blockReapCache;
                    item.setAmount(item.getAmount() - 1);
                    block.setBlockData(getServer().createBlockData(blockDataBuffer));
                    if(item.getAmount() != 0)
                        blockReapCache = item;
                    else
                        blockReapCache = null;
                }else{//没缓存查找
                    //没找到的标志
                    boolean flag = true;
                    //遍历查找背包内种子,注意这里物品可能为null导致bug
                    for (ItemStack item: player.getInventory()
                    ) {
                        if (item != null && item.getType().getKey().equals(cropSeed)) {
                            //找到了，成功消耗种子
                            item.setAmount(item.getAmount() - 1);
                            block.setBlockData(getServer().createBlockData(blockDataBuffer));
                            flag = false;
                            if(item.getAmount() != 0)
                                blockReapCache = item;
                            else
                                blockReapCache = null;
                            break;
                        }
                    }
                    //没找到
                    if(flag) player.sendMessage(ChatColor.RED + cropSeed.toString() + " not found.");
                }
            }
        }
        else{
            player.sendMessage(ChatColor.DARK_RED+"This is not a crop! Wrong config.");
        }
    }

    private ItemStack blockBoneMealCache = null;
    private void blockBoneMeal(Block block,Player player){
        //判断是否为农作物
        if(block.getBlockData() instanceof Ageable blockDataAgeable){
            //成熟
            if(blockDataAgeable.getAge() < blockDataAgeable.getMaximumAge()){
                if(blockBoneMealCache != null && blockBoneMealCache.getType() == Material.BONE_MEAL) {
                    //有缓存直接用缓存
                    ItemStack item = blockBoneMealCache;
                    item.setAmount(item.getAmount()-1);
                    block.applyBoneMeal(BlockFace.UP);
                    if(item.getAmount() != 0)
                        blockBoneMealCache = item;
                    else
                        blockBoneMealCache = null;
                }
                else{
                    //没找到的标志
                    boolean flag = true;
                    //遍历查找背包内种子,注意这里物品可能为null导致bug
                    for (ItemStack item: player.getInventory()
                    ) {
                        if(item != null && item.getType() == Material.BONE_MEAL){
                            item.setAmount(item.getAmount()-1);
                            block.applyBoneMeal(BlockFace.UP);
                            flag = false;
                            //保存缓存
                            if(item.getAmount() != 0)
                                blockBoneMealCache = item;
                            else
                                blockBoneMealCache = null;
                            break;
                        }
                    }
                    //没找到
                    if(flag) player.sendMessage(ChatColor.RED+"bone meal not enough.");
                }
            }
        }
        else{
            player.sendMessage(ChatColor.DARK_RED+"This is not a crop! Wrong config.");
        }
    }
}
