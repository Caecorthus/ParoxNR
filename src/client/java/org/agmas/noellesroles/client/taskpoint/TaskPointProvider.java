package org.agmas.noellesroles.client.taskpoint;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * 任务点高亮 provider：把一个心情任务（wathe Task）映射为要描边的目标点集合。
 * 未来新增任务或任务点类型时，实现本接口并注册到 {@link TaskPointProviders} 即可。
 */
public interface TaskPointProvider {

    PlayerMoodComponent.Task task();

    /**
     * 返回当前要描边的目标方块位置。为空则不描边。
     */
    List<BlockPos> targets(ClientPlayerEntity player, TaskPointCache cache);
}
