package org.agmas.noellesroles.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.world.World;
import org.agmas.noellesroles.puppeteer.PuppeteerControl;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 傀儡师的站立人形实体，两种用途：
 * PUPPET —— 闲置在场上的傀儡（死者皮肤）；SHELL —— 傀儡师操纵傀儡期间留在原地的本体替身（傀儡师皮肤）。
 * 免疫一切原版伤害、不可推动；被武器命中时由 {@link PuppeteerControl#onEntityHit} 结算。
 */
public class PuppetEntity extends LivingEntity {
    private static final TrackedData<Byte> KIND = DataTracker.registerData(PuppetEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(PuppetEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Optional<UUID>> SKIN = DataTracker.registerData(PuppetEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);

    public enum Kind {
        PUPPET,
        SHELL
    }

    public PuppetEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createPuppetAttributes() {
        return LivingEntity.createLivingAttributes();
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(KIND, (byte) Kind.PUPPET.ordinal());
        builder.add(OWNER, Optional.empty());
        builder.add(SKIN, Optional.empty());
    }

    public Kind getKind() {
        byte ordinal = this.dataTracker.get(KIND);
        return ordinal == Kind.SHELL.ordinal() ? Kind.SHELL : Kind.PUPPET;
    }

    public void setKind(Kind kind) {
        this.dataTracker.set(KIND, (byte) kind.ordinal());
    }

    public boolean isShell() {
        return getKind() == Kind.SHELL;
    }

    /** 傀儡师（傀儡的主人 / 替身对应的本人）。 */
    @Nullable
    public UUID getOwnerUuid() {
        return this.dataTracker.get(OWNER).orElse(null);
    }

    public void setOwnerUuid(@Nullable UUID owner) {
        this.dataTracker.set(OWNER, Optional.ofNullable(owner));
    }

    /** 渲染所用皮肤的玩家：傀儡为死者，替身为傀儡师本人。傀儡的皮肤 UUID 同时也是傀儡的标识。 */
    @Nullable
    public UUID getSkinUuid() {
        return this.dataTracker.get(SKIN).orElse(null);
    }

    public void setSkinUuid(@Nullable UUID skin) {
        this.dataTracker.set(SKIN, Optional.ofNullable(skin));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            PuppeteerControl.tickEntity(serverWorld, this);
        }
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return List.of();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    // 原版伤害一律无效：只有本模组的武器命中逻辑能让傀儡倒地 / 让替身对应的傀儡师死亡
    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    @Override
    public void takeKnockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    // 与玩家之间的实体碰撞一致：站着的傀儡/替身会挡路
    @Override
    public boolean isCollidable() {
        return true;
    }

    @Override
    public boolean shouldRenderName() {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putByte("PuppetKind", (byte) getKind().ordinal());
        UUID owner = getOwnerUuid();
        if (owner != null) {
            nbt.putUuid("PuppetOwner", owner);
        }
        UUID skin = getSkinUuid();
        if (skin != null) {
            nbt.putUuid("PuppetSkin", skin);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setKind(nbt.getByte("PuppetKind") == Kind.SHELL.ordinal() ? Kind.SHELL : Kind.PUPPET);
        setOwnerUuid(nbt.containsUuid("PuppetOwner") ? nbt.getUuid("PuppetOwner") : null);
        setSkinUuid(nbt.containsUuid("PuppetSkin") ? nbt.getUuid("PuppetSkin") : null);
    }
}
