package org.agmas.noellesroles.recaller;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 回溯者【锚点】实体。
 * 以 {@link org.agmas.noellesroles.breacher.BreacherMarkerEntity} 为模板，关键差异：
 * - 不开 setGlowing(true)：发光=全员透视，违反“锚点仅碎片持有者可见”的定案；
 *   可见性完全由客户端渲染器门控（RecallerAnchorEntityRenderer.shouldRender）+ 描边监听实现。
 * - shouldRender 距离旁路：保证持有者在全图范围都能看到描边/名牌（先例 ThrowingAxeEntity）。
 * canHit 保持 true：隐形实体的准星命中是“放置者蹲下右键回收”交互的前提。
 */
public class RecallerAnchorEntity extends Entity {
    public RecallerAnchorEntity(EntityType<? extends RecallerAnchorEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setCustomNameVisible(true);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        this.setVelocity(Vec3d.ZERO);
    }

    /** 绕过实体渲染距离裁剪，让持有者远距离也能看到锚点（描边/名牌）。 */
    @Override
    public boolean shouldRender(double squaredDistance) {
        return true;
    }

    /** 允许客户端准星命中并发送交互包（回收交互依赖它）。 */
    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    /** 交互逻辑由服务端 UseEntityCallback（RecallerAnchorInteractHandler）处理。 */
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isCollidable() {
        return false;
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
