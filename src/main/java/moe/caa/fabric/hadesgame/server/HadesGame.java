package moe.caa.fabric.hadesgame.server;

import moe.caa.fabric.hadesgame.server.gameevent.*;
import moe.caa.fabric.hadesgame.server.schedule.HadesGameScheduleManager;
import moe.caa.fabric.hadesgame.server.scoreboard.ScoreboardHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HadesGame implements ModInitializer {
    public static final WeightRandomArrayList<StatusEffect> STATUS_EFFECT_WEIGHT_RANDOM_ARRAY_LIST = new WeightRandomArrayList<>();
    public static final WeightRandomArrayList<Item> ITEM_WEIGHT_RANDOM_ARRAY_LIST = new WeightRandomArrayList<>();
    public static final WeightRandomArrayList<Block> BLOCK_WEIGHT_RANDOM_ARRAY_LIST = new WeightRandomArrayList<>();
    public static Optional<MinecraftServer> server = Optional.empty();
    public static List<String> scoreboardTemp = new ArrayList<>();
    private static Location loc;

    // 大厅位置
    public static Location getLobbyLocation() {
        return loc;
    }

    public static void randomLobbyLocation() {
        final double x = -20000000 + Math.random() * 40000000;
        final double z = -20000000 + Math.random() * 40000000;
        loc = new Location(new Vec3d(x, 302, z), server.get().getOverworld(), 0, 0);
        server.get().getOverworld().getWorldBorder().setCenter(x, z);
        server.get().getOverworld().getWorldBorder().setSize(200);

        placeLobbyBlock();
    }

    public static void deleteDir(File file) {
        //判断是否为文件夹
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File value : files) {
                    deleteDir(value);
                }
            }
        }
        file.delete();
    }

    private static void placeLobbyBlock() {
        // 放置大厅方块
        HadesGameScheduleManager.runTask(() -> {
            ServerWorld world = server.get().getOverworld();

            final Location lobbyLocation = getLobbyLocation();

            // 大厅地面
            for (int i = (int) (lobbyLocation.pos.x - 10); i < (lobbyLocation.pos.x + 10); i++) {
                for (int j = (int) (lobbyLocation.pos.z - 10); j < (lobbyLocation.pos.z + 10); j++) {
                    world.setBlockState(new BlockPos(i, 300, j), Blocks.BARRIER.getDefaultState());
                }
            }


            // 大厅墙壁
            for (int i = (int) (lobbyLocation.pos.x - 10); i < lobbyLocation.pos.x + 10; i++) {
                for (int j = 300; j < 302; j++) {
                    world.setBlockState(new BlockPos(10, j, i), Blocks.BARRIER.getDefaultState());
                    world.setBlockState(new BlockPos(-10, j, i), Blocks.BARRIER.getDefaultState());
                    world.setBlockState(new BlockPos(i, j, 10), Blocks.BARRIER.getDefaultState());
                    world.setBlockState(new BlockPos(i, j, -10), Blocks.BARRIER.getDefaultState());
                }
            }
        });
    }

    public static void removeLobbyBlock() {
        ServerWorld world = server.get().getOverworld();

        final Location lobbyLocation = getLobbyLocation();

        // 大厅地面
        for (int i = (int) (lobbyLocation.pos.x - 10); i < (lobbyLocation.pos.x + 10); i++) {
            for (int j = (int) (lobbyLocation.pos.z - 10); j < (lobbyLocation.pos.z + 10); j++) {
                world.setBlockState(new BlockPos(i, 300, j), Blocks.AIR.getDefaultState());
            }
        }


        // 大厅墙壁
        for (int i = (int) (lobbyLocation.pos.x - 10); i < lobbyLocation.pos.x + 10; i++) {
            for (int j = 300; j < 302; j++) {
                world.setBlockState(new BlockPos(10, j, i), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(-10, j, i), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(i, j, 10), Blocks.AIR.getDefaultState());
                world.setBlockState(new BlockPos(i, j, -10), Blocks.AIR.getDefaultState());
            }
        }
    }

    @Override
    public void onInitialize() {

        // 注册命令
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> HadesGameCommand.register(dispatcher));

        // 赋值服务端实例
        ServerLifecycleEvents.SERVER_STARTING.register(server1 -> {
            server = Optional.of(server1);
            HadesGameScheduleManager.runTask(HadesGame::randomLobbyLocation);
        });

        // 初始化Schedule调度器
        HadesGameScheduleManager.INSTANCE.init();

        // 初始化计分板
        ScoreboardHandler.INSTANCE.init();

        // 初始化游戏核心
        GameCore.INSTANCE.init();

        // 设置计分板标题
        ScoreboardHandler.INSTANCE.currentTitle = "\u00a7e\u00a7l阴间游戏";

        // 设置动态模板
        scoreboardTemp.add("\u00a78{0}");
        scoreboardTemp.add("");
        scoreboardTemp.add("\u00a7f下一事件:");
        scoreboardTemp.add("\u00a7a{1}  \u00a77{2}");
        scoreboardTemp.add(" ");
        scoreboardTemp.add("\u00a7a存活: \u00a7c{3}");
        scoreboardTemp.add("  ");
        scoreboardTemp.add("\u00a7a边界: \u00a7c{4}");
        scoreboardTemp.add("   ");
        scoreboardTemp.add("    ");
        scoreboardTemp.add("\u00a7e哼哼啊啊啊啊啊啊啊");


        // 设置计分板内容
        ScoreboardHandler.INSTANCE.currentContent.clear();
        ScoreboardHandler.INSTANCE.currentContent.addAll(scoreboardTemp);

        // 注册事件

        // 交换位置
        GameCore.INSTANCE.eventList.add(new SwapLocationEvent(), 10);

        // 治疗
        GameCore.INSTANCE.eventList.add(new TreatmentEvent(), 2);

        // 交换背包
        GameCore.INSTANCE.eventList.add(new SwapInventoryEvent(), 10);

        // 交换生命
        GameCore.INSTANCE.eventList.add(new SwapHealthEvent(), 10);

        // 发光
        GameCore.INSTANCE.eventList.add(new LuminescenceEvent(), 5);

        // 雷暴
        GameCore.INSTANCE.eventList.add(new ThunderstormEvent(), 10);

        // 随机效果
        GameCore.INSTANCE.eventList.add(new RandomEffectEvent(), 10);

        // 铁砧雨
        GameCore.INSTANCE.eventList.add(new RainAnvilEvent(), 10);

        // 背包破损
        GameCore.INSTANCE.eventList.add(new RandomClearInventorySectionEvent(), 10);

        // 三倍掉落
        GameCore.INSTANCE.eventList.add(new TripleDropEvent(), 5);

        // 随机掉落
        GameCore.INSTANCE.eventList.add(new RandomDropEvent(), 10);

        // 昏乱
        GameCore.INSTANCE.eventList.add(new RandomMoveEvent(), 10);

        // 方块雨
        GameCore.INSTANCE.eventList.add(new RainBlockEvent(), 5);

        // 沧海桑田
        GameCore.INSTANCE.eventList.add(new TickSpeedUp(), 10);

        // 删除老的世界
        File file = new File("world");
        try {
            deleteDir(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
