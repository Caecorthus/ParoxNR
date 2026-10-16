package org.agmas.noellesroles.task;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 任务槽位枚举扩展的启动安全测试。
 *
 * <p>背景：{@code Task} 枚举是 JVM 的 final 枚举，本模组靠
 * {@code Unsafe.allocateInstance} + 写 {@code Enum.name/ordinal} 造出额外槽位
 * （见 {@link NoellesTaskTypes#extendEnum}）。这段代码只在**运行时**执行，普通编译不会发现问题，
 * 而它一旦失败就是「游戏启动即崩」——2026-10-11 已经因此崩过两次：
 * <ol>
 *   <li>用 {@code Field#setAccessible} 写 {@code java.lang.Enum.name}
 *       → Java 21 模块系统抛 {@code InaccessibleObjectException}；</li>
 *   <li>改用 Unsafe 后仍然崩，因为 {@code Task.class.getDeclaredField("name")} 找不到字段
 *       ——{@code name}/{@code ordinal} 声明在父类 {@code java.lang.Enum} 上。</li>
 * </ol>
 *
 * <p>所以这里把这套机制在**测试期**真跑一遍：只要能通过，启动时就不会再因为这两类原因崩溃。
 * 测试直接调用生产代码的 {@code extendEnum}（传空数组即可，逻辑只用到
 * {@code TYPES} 注册表与 {@code PlayerMoodComponent.Task.class}），不需要真实对局。
 */
class NoellesTaskTypesTest {

    @Test
    void extendEnumCreatesUsableSlotOnJava21() throws Exception {
        // 这个仓库的 JUnit 运行期类路径不一定带 wathe / Minecraft；
        // 那样的话本测试无法验证任何东西，直接跳过（而不是误报失败）。
        assumeTrue(canLoadWatheTaskClass(), "运行期类路径没有 wathe，跳过（请在能加载 wathe 的环境运行）");

        Method extendEnum = NoellesTaskTypes.class.getDeclaredMethod("extendEnum", PlayerMoodComponent.Task[].class);
        extendEnum.setAccessible(true);

        // 传空数组：extendEnum 只用它的 length 作为「原版槽位数」。
        PlayerMoodComponent.Task[] extended =
                (PlayerMoodComponent.Task[]) extendEnum.invoke(null, (Object) new PlayerMoodComponent.Task[0]);
        assertNotNull(extended, "extendEnum 返回了 null");
        assertEquals(NoellesTaskTypes.customCount(), extended.length, "扩展后的槽位数不对");

        PlayerMoodComponent.Task slot = NoellesTaskTypes.DREAM_SLEEP.slot();
        assertNotNull(slot, "噬梦者睡觉任务槽位没有建出来");
        // 注册表默认不带前缀/提示（沿用原版），噬梦者的任务在注册时两个都声明打开。
        assertTrue(NoellesTaskTypes.suppressesPrefix(slot), "噬梦者任务应当声明免前缀");
        assertTrue(NoellesTaskTypes.suppressesTaskPointHint(slot), "噬梦者任务应当声明免任务点提示");
        // 原版槽位不受影响（沿用原版任务行表现）。
        assertTrue(!NoellesTaskTypes.suppressesPrefix(PlayerMoodComponent.Task.SLEEP), "原版睡觉任务不应免前缀");
        assertSame(slot, extended[0], "注册表槽位与 $VALUES 里的槽位不是同一个实例");

        // name/ordinal 必须真的写进去了：ordinal 决定存档与同步包的编码。
        assertEquals("dream_sleep", slot.name(), "Enum.name 没写进去");
        assertEquals(0, slot.ordinal(), "Enum.ordinal 没写进去");
        assertTrue(slot.toString().contains("dream_sleep"), "toString 异常：" + slot);

        // setFunction 必须可用，否则 vanilla 的 readFromNbt / applySyncPacket 会 NPE。
        PlayerMoodComponent.TrainTask task = NoellesTaskTypes.createDummy(slot);
        assertTrue(task instanceof DreamSleepTask, "工厂没有造出 DreamSleepTask：" + task);
        NbtCompound tag = new NbtCompound();
        tag.putInt("timer", 7);
        PlayerMoodComponent.TrainTask fromNbt = NoellesTaskTypes.DREAM_SLEEP.create(tag);
        assertTrue(fromNbt instanceof DreamSleepTask, "NBT 工厂失效：" + fromNbt);
        assertSame(slot, fromNbt.getType(), "反序列化出来的任务类型不对");

        // 非本模组槽位必须被拒绝（防止调用方误用）。
        assertThrows(IllegalArgumentException.class,
                () -> NoellesTaskTypes.createDummy(PlayerMoodComponent.Task.SLEEP));
    }

    private static boolean canLoadWatheTaskClass() {
        try {
            Class.forName("dev.doctor4t.wathe.cca.PlayerMoodComponent$Task");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
