# MCPlugin_BetterReap
手拿锄时右键农作物方块，如果成熟则使用锄破坏方块（不消耗耐久）来收获，并消耗背包中对应的种子重新种植（如果没有则提示缺少种子，但仍会破坏方块）。
默认支持土豆胡萝卜小麦和原版锄头，配置文件可高度自定义，可以自行添加农作物和对应的种子（可包括模组，实际上种子可以不是对应的种子甚至可以不是种子，只是会消耗1个，不影响重新种植的作物种类），以及锄头类型（也可以使用别的物品）
比起其它收割插件直接让农作物回到初始态，该插件使用玩家破坏方块再重新放置方块的逻辑，可以适合一些mod的需求，并且支持模组物品。
