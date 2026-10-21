package org.agmas.noellesroles.breacher;

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
 * 破袭者【破坏点】的可点击标记实体。
 * 原版 ItemEntity 的 canHit 为 false，客户端不会对投掷物发交互包，
 * 因此改用自定义实体承载右键交互（配合客户端渲染器绘制镐子模型与名牌）。
 */
public class BreacherMarkerEntity extends Entity {
    public BreacherMarkerEntity(EntityType<? extends BreacherMarkerEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.setGlowing(true);
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

    /** 允许客户端准星命中并发送交互包（原版实体默认 false）。 */
    @Override
    public boolean canHit() {
        return !this.isRemoved();
    }

    /** 交互逻辑由服务端 UseEntityCallback（BreacherRepairHandler）处理。 */
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
