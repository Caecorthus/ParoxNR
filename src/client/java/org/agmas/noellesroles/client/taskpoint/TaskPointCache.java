package org.agmas.noellesroles.client.taskpoint;

import dev.doctor4t.wathe.block.DrinkTrayBlock;
import dev.doctor4t.wathe.block.FoodPlatterBlock;
import dev.doctor4t.wathe.block_entity.BeveragePlateBlockEntity;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * 已加载区块扫描快照：餐盘（内容物含可食用物品）、饮品托盘（内容物含 wathe 原版鸡尾酒）、
 * 所有门的 keyName。由 TaskPointHighlightManager 周期性（20 tick）重建；
 * 仅覆盖玩家视距内已加载区块。
 */
public final class TaskPointCache {

    public record DoorEntry(BlockPos lowerPos, String keyName) {
    }

    private final List<BlockPos> foodPlatters;
    private final List<BlockPos> drinkTrays;
    private final List<DoorEntry> doors;

    private TaskPointCache(List<BlockPos> foodPlatters, List<BlockPos> drinkTrays, List<DoorEntry> doors) {
        this.foodPlatters = foodPlatters;
        this.drinkTrays = drinkTrays;
        this.doors = doors;
    }

    public List<BlockPos> getFoodPlatters() {
        return foodPlatters;
    }

    public List<BlockPos> getDrinkTrays() {
        return drinkTrays;
    }

    public List<DoorEntry> getDoors() {
        return doors;
    }

    public static TaskPointCache scan(MinecraftClient client) {
        ClientWorld world = client.world;
        List<BlockPos> foodPlatters = new ArrayList<>();
        List<BlockPos> drinkTrays = new ArrayList<>();
        List<DoorEntry> doors = new ArrayList<>();
        if (world == null) {
            return new TaskPointCache(foodPlatters, drinkTrays, doors);
        }

        int radius = client.options.getViewDistance().getValue();
        BlockPos camera = client.gameRenderer.getCamera().getBlockPos();
        int centerX = camera.getX() >> 4;
        int centerZ = camera.getZ() >> 4;
        // +2 余量覆盖相机跨越区块边界与服务器边缘加载的区块
        int maxRadius = radius + 2;

        for (int cx = centerX - maxRadius; cx <= centerX + maxRadius; cx++) {
            for (int cz = centerZ - maxRadius; cz <= centerZ + maxRadius; cz++) {
                Chunk chunk = world.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(chunk instanceof WorldChunk worldChunk)) continue;
                for (BlockEntity be : worldChunk.getBlockEntities().values()) {
                    BlockPos pos = be.getPos();
                    if (be instanceof BeveragePlateBlockEntity plate) {
                        if (plate.getStoredItems().isEmpty()) continue;
                        var block = world.getBlockState(pos).getBlock();
                        if (block instanceof DrinkTrayBlock) {
                            if (containsWatheDrink(plate)) {
                                drinkTrays.add(pos);
                            }
                        } else if (block instanceof FoodPlatterBlock) {
                            if (containsEdible(plate)) {
                                foodPlatters.add(pos);
                            }
                        }
                    } else if (be instanceof DoorBlockEntity door) {
                        String keyName = door.getKeyName();
                        if (keyName != null && !keyName.isEmpty()) {
                            doors.add(new DoorEntry(pos, keyName));
                        }
                    }
                }
            }
        }
        return new TaskPointCache(foodPlatters, drinkTrays, doors);
    }

    /**
     * 托盘内容物中是否含 wathe 原版鸡尾酒（不含本模组自定义饮品）；混装时任一即可。
     */
    private static boolean containsWatheDrink(BeveragePlateBlockEntity plate) {
        for (ItemStack stack : plate.getStoredItems()) {
            if (stack.isOf(WatheItems.OLD_FASHIONED) || stack.isOf(WatheItems.MOJITO)
                    || stack.isOf(WatheItems.MARTINI) || stack.isOf(WatheItems.COSMOPOLITAN)
                    || stack.isOf(WatheItems.CHAMPAGNE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 餐盘内容物中是否有可食用的物品（带 FOOD 组件）；任一即可。
     */
    private static boolean containsEdible(BeveragePlateBlockEntity plate) {
        for (ItemStack stack : plate.getStoredItems()) {
            if (stack.contains(DataComponentTypes.FOOD)) {
                return true;
            }
        }
        return false;
    }
}
