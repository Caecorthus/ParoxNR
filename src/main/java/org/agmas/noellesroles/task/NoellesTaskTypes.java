package org.agmas.noellesroles.task;

import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 本模组的「心情任务类型注册表」。
 *
 * <h2>为什么需要它</h2>
 * wathe 的任务表是 {@code Map<PlayerMoodComponent.Task, TrainTask>}，
 * 而 {@code Task} 枚举是 {@code private static final Task[] $VALUES} 驱动的普通 Java 枚举，
 * 原版只有 SLEEP / OUTSIDE / EAT / DRINK 四个槽位。想新增一类任务
 * （例如噬梦者的睡觉任务）就必须**追加枚举槽位**，否则：
 * <ul>
 *   <li>新任务只能和某个既有任务共用槽位，同一时刻两者只能存在一个；</li>
 *   <li>HUD（{@code MoodRenderer.renderHud} 会遍历 {@code Task.values()}）不会给它单独一行。</li>
 * </ul>
 * wathe 的 {@code Task} 无法用 MixinExtras 的 {@code @ExtendEnum} 扩展
 * （本机 MixinExtras 0.4.1 / 0.5.0 都不含该注入器），因此这里采用
 * 「改写 {@code $VALUES} 数组」的方式追加真实槽位：
 * {@code PlayerMoodComponent$TaskAccessor} 用 {@code @Mutable @Accessor} 暴露该私有静态字段，
 * {@code TaskEnumExtensionMixin} 在 {@code <clinit>} 的 TAIL 把扩展后的数组写回去。
 * 因为是在枚举类自己的静态初始化里做的，任何 {@code Task.values()} 调用看到的都已经是扩展后的槽位。
 *
 * <h2>为什么反序列化不用改 wathe 代码</h2>
 * {@code PlayerMoodComponent.readFromNbt} 与 {@code applySyncPacket} 都走
 * {@code task.setFunction.apply(tag)}，而 {@code setFunction} 只是 {@code Task} 上的一个
 * {@code private final Function<NbtCompound, TrainTask>} 实例字段。
 * 这里用反射把新槽位的该字段指向我们自己的工厂，vanilla 的两条反序列化路径就自动认我们的任务，
 * 且 {@code ordinal < Task.values().length} 的边界检查在追加后天然放行。
 *
 * <h2>两条铁律（改动前务必读）</h2>
 * <ol>
 *   <li><b>只能追加，绝不能插入或删除槽位</b>：存档（{@code tasks[].type}）与网络同步包
 *       （{@code writeSyncPacket}）传的都是 {@code Task.ordinal()}，一旦重排，旧存档里的任务会被
 *       解释成另一类任务。注册顺序 = 槽位顺序 = ordinal 顺序，注册表按 id 记忆 ordinal。</li>
 *   <li>每个新槽位的 {@code setFunction} 必须安装成功，否则该槽位的 NBT 反序列化会 NPE。</li>
 * </ol>
 *
 * <h2>新增一类任务的步骤</h2>
 * <ol>
 *   <li>写一个 {@code TrainTask} 实现（{@code getName()} 返回任务名，HUD 会拼成 {@code task.feel.<name>}）；</li>
 *   <li>在 {@link #register(String, Function)} 的注册块里加一条（id 全局唯一，建议用模组前缀）；</li>
 *   <li>补语言键 {@code task.dream_sleep} 之类 + 在 {@code TaskPointTaskTextMixin} 里确认不会误加提示。</li>
 * </ol>
 */
public final class NoellesTaskTypes {

    private NoellesTaskTypes() {}

    /** 一个模组自定义任务类型。 */
    public static final class TaskType {
        private final String id;
        private final Function<NbtCompound, PlayerMoodComponent.TrainTask> factory;
        private final boolean suppressPrefix;
        private final boolean suppressTaskPointHint;
        @Nullable private PlayerMoodComponent.Task slot;

        private TaskType(String id,
                         Function<NbtCompound, PlayerMoodComponent.TrainTask> factory,
                         boolean suppressPrefix,
                         boolean suppressTaskPointHint) {
            this.id = id;
            this.factory = factory;
            this.suppressPrefix = suppressPrefix;
            this.suppressTaskPointHint = suppressTaskPointHint;
        }

        public String id() {
            return this.id;
        }

        /** 该类型的枚举槽位；枚举扩展完成后才会被赋值。 */
        public PlayerMoodComponent.Task slot() {
            if (this.slot == null) {
                // 枚举扩展挂在 Task.<clinit> 的 TAIL 注入上；若本会话还没有任何代码碰过
                // PlayerMoodComponent.Task（典型场景：专用服务器重启后零玩家直接 finalize），
                // 枚举类尚未初始化，这里主动触碰一次，触发 <clinit> 并由 extendEnum 回填 slot。
                PlayerMoodComponent.Task.values();
            }
            if (this.slot == null) {
                throw new IllegalStateException("task type '" + this.id + "' has no slot: enum extension did not run");
            }
            return this.slot;
        }

        /** 依据存档数据构造任务实例（vanilla 反序列化路径会调用）。 */
        public PlayerMoodComponent.TrainTask create(NbtCompound tag) {
            return this.factory.apply(tag);
        }

        /**
         * HUD 任务行是否**不加**「你可以假装 / 你觉得该」前缀。
         * <p>
         * 默认 {@code false}：新注册的任务类型默认沿用 wathe 原版行为（带前缀），
         * 只有任务名本身就是一句完整话时（例如噬梦者的「你被噬梦者盯上了：去睡觉！」）才在注册时声明免前缀。
         */
        public boolean suppressesPrefix() {
            return this.suppressPrefix;
        }

        /**
         * 任务行是否**不追加**任务点提示（如「-手持房间钥匙高亮房间门」）。
         * <p>
         * 默认 {@code false}：默认按任务名走 {@code TaskPointTaskTextMixin} 里的提示映射，
         * 只有自带完整语义的任务才在注册时声明免提示。
         */
        public boolean suppressesTaskPointHint() {
            return this.suppressTaskPointHint;
        }
    }

    /** 注册顺序 = 枚举槽位顺序（从 ordinal 4 开始）。 */
    private static final List<TaskType> TYPES = new ArrayList<>();
    private static final Map<String, TaskType> BY_ID = new HashMap<>();

    /**
     * 噬梦者：被入梦标记后获得的睡觉任务（与原版睡觉任务分属不同槽位，可同时存在）。
     * <p>
     * 它的任务名本身就是一句完整的话（「你被噬梦者盯上了：去睡觉！」），所以两个开关都打开：
     * 不加「你可以假装 / 你觉得该」前缀，也不追加任务点提示。
     */
    public static final TaskType DREAM_SLEEP = register("dream_sleep", DreamSleepTask::fromNbt, true, true);

    /**
     * 破袭者破坏点联动任务「去修复破坏点。」：有破坏点存在时注入全体真实/假心情玩家，
     * 全部修复后移除。文案走原版前缀机制（task.feel / task.fake + task.repair_breach），
     * 不参与随机任务刷新，也不计入理智下降的任务数倍率
     * （见 {@code MoodDrainRepairTaskExemptMixin}）。
     */
    public static final TaskType REPAIR_BREACH = register("repair_breach", RepairBreachTask::fromNbt);

    /**
     * 注册一个任务类型，沿用原版任务行表现（带前缀、允许任务点提示）。
     *
     * @param id      类型标识，同时是 {@code task.<id>} 语言键与 HUD 任务名
     * @param factory 由存档数据构造任务实例的工厂（同时用于客户端 dummy 构造）
     */
    public static TaskType register(String id, Function<NbtCompound, PlayerMoodComponent.TrainTask> factory) {
        return register(id, factory, false, false);
    }

    /**
     * 注册一个任务类型。
     *
     * @param suppressPrefix        该任务的文案自带完整句子、不需要「你可以假装 / 你觉得该」前缀时为 true
     * @param suppressTaskPointHint 该任务自带完整语义、不需要追加任务点提示时为 true
     */
    public static TaskType register(String id,
                                    Function<NbtCompound, PlayerMoodComponent.TrainTask> factory,
                                    boolean suppressPrefix,
                                    boolean suppressTaskPointHint) {
        TaskType type = new TaskType(id, factory, suppressPrefix, suppressTaskPointHint);
        TYPES.add(type);
        BY_ID.put(id, type);
        return type;
    }

    /** 某个槽位是否属于本模组追加的类型。 */
    public static boolean isCustom(@Nullable PlayerMoodComponent.Task task) {
        return task != null && indexOf(task) >= 0;
    }

    /** 是否是噬梦者的睡觉任务。按语义判定，不按槽位下标。 */
    public static boolean isDreamSleepTask(@Nullable PlayerMoodComponent.Task task) {
        return task != null && DREAM_SLEEP.slot == task;
    }

    /**
     * 是否是「睡觉类」任务：wathe 原版睡觉任务 + 噬梦者的睡觉任务。
     * <p>
     * 用途是「持有任一睡觉任务」的判定（噬梦者的梦境窥视透视、睡觉恢复等）。
     * 以后新增睡觉类任务时，判定要跟着补，不要退回只比 {@code Task.SLEEP}。
     */
    public static boolean isSleepLike(@Nullable PlayerMoodComponent.TrainTask task) {
        if (task == null) {
            return false;
        }
        if (task instanceof DreamSleepTask) {
            return true;
        }
        PlayerMoodComponent.Task type = task.getType();
        return type == PlayerMoodComponent.Task.SLEEP || isDreamSleepTask(type);
    }

    @Nullable
    public static TaskType typeOf(@Nullable PlayerMoodComponent.Task task) {
        int index = indexOf(task);
        return index < 0 ? null : TYPES.get(index);
    }

    /** 该槽位对应的任务是否声明「不要 HUD 任务行前缀」；非自定义槽位返回 false（沿用原版行为）。 */
    public static boolean suppressesPrefix(@Nullable PlayerMoodComponent.Task task) {
        TaskType type = typeOf(task);
        return type != null && type.suppressesPrefix();
    }

    /** 该槽位对应的任务是否声明「不要任务点提示」；非自定义槽位返回 false（沿用原版行为）。 */
    public static boolean suppressesTaskPointHint(@Nullable PlayerMoodComponent.Task task) {
        TaskType type = typeOf(task);
        return type != null && type.suppressesTaskPointHint();
    }

    /** 本模组槽位在 {@code Task.values()} 里的下标；非自定义槽位返回 -1。 */
    public static int indexOf(@Nullable PlayerMoodComponent.Task task) {
        if (task == null) {
            return -1;
        }
        for (int i = 0; i < TYPES.size(); i++) {
            if (TYPES.get(i).slot == task) {
                return i;
            }
        }
        return -1;
    }

    /** 总共追加了几个槽位。 */
    public static int customCount() {
        return TYPES.size();
    }

    /** wathe 原版的任务槽位数（Task 枚举扩展前的长度）。 */
    public static int vanillaCount() {
        return 4;
    }

    /**
     * 为客户端「假任务」构造一个空白实例。
     * <p>
     * 同步包只传 {@code ordinal}，客户端拿不到真实任务数据，wathe 原版会给每个槽位造一个
     * timer=0 的 dummy（见 {@code PlayerMoodComponent.createDummyTask}）。
     * 本模组追加的槽位同理：dummy 只用于 HUD 渲染任务行，不参与任何判定
     * （客户端 {@code tasks} 仅被 HUD 与任务点高亮读取）。
     *
     * @param slot 必须是本模组追加的槽位，且属于已注册类型
     */
    public static PlayerMoodComponent.TrainTask createDummy(PlayerMoodComponent.Task slot) {
        int index = indexOf(slot);
        if (index < 0) {
            throw new IllegalArgumentException("not a noellesroles task slot: " + slot);
        }
        return TYPES.get(index).factory.apply(new NbtCompound());
    }

    /**
     * 由 {@code PlayerMoodComponent$Task.<clinit>} 的 TAIL 注入调用：
     * 构造真实枚举常量并把它们追加到 {@code $VALUES} 后面，同时安装各自的 {@code setFunction}。
     *
     * @param original 枚举类自己算出来的原版 {@code $VALUES}（长度必须是原版常量数）
     * @return 扩展后的数组，注入代码会把它写回 {@code $VALUES}
     */
    public static PlayerMoodComponent.Task[] extendEnum(PlayerMoodComponent.Task[] original) {
        // 幂等保护：正常情况下 <clinit> 只会跑一次；若被重复调用，槽位已建好就直接放行，
        // 不做任何重建 —— 换掉一个已经流通过（可能已写进存档/NBT 缓存）的枚举常量比不扩展更糟。
        if (!TYPES.isEmpty() && TYPES.get(0).slot != null) {
            return original;
        }

        int vanillaCount = original.length;
        PlayerMoodComponent.Task[] extended = new PlayerMoodComponent.Task[vanillaCount + TYPES.size()];
        System.arraycopy(original, 0, extended, 0, vanillaCount);

        for (int i = 0; i < TYPES.size(); i++) {
            int ordinal = vanillaCount + i;
            TaskType type = TYPES.get(i);

            PlayerMoodComponent.Task slot = newEnumConstant(ordinal, type.id);
            installSetFunction(slot, type.factory);

            type.slot = slot;
            extended[ordinal] = slot;
        }
        return extended;
    }

    /** 懒加载的 {@code sun.misc.Unsafe}：造实例用 {@code allocateInstance}，写字段用 {@code putObject/putInt} + 偏移。 */
    private static final class UnsafeHolder {
        static final Object UNSAFE;
        static final Method ALLOCATE_INSTANCE;
        static final Method OBJECT_FIELD_OFFSET;
        static final Method PUT_OBJECT;
        static final Method PUT_INT;

        static {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                UNSAFE = theUnsafe.get(null);
                ALLOCATE_INSTANCE = unsafeClass.getMethod("allocateInstance", Class.class);
                OBJECT_FIELD_OFFSET = unsafeClass.getMethod("objectFieldOffset", Field.class);
                PUT_OBJECT = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
                PUT_INT = unsafeClass.getMethod("putInt", Object.class, long.class, int.class);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("failed to access sun.misc.Unsafe", e);
            }
        }
    }

    /**
     * 造一个「真实」枚举常量。
     * <p>
     * {@code Task} 是 final 枚举，不能在扩展类里直接 {@code new} 其私有构造器，
     * 因此走 {@code Unsafe.allocateInstance} 拿到实例（跳过构造器），
     * 再补上 {@code Enum.name} 与 {@code Enum.ordinal} 两个父类字段 ——
     * 这正是 JVM 侧 {@code Class#getEnumConstants()} 与 {@code Enum#name()/ordinal()} 依赖的两个字段。
     * {@code setFunction} 由 {@link #installSetFunction} 单独补齐。
     * <p>
     * <b>必须用 Unsafe 而不是反射 {@code Field#set}：</b>Java 21 的模块系统不允许对
     * {@code java.lang.Enum} 的字段 {@code setAccessible}
     * （{@code InaccessibleObjectException: module java.base does not "opens java.lang"}，
     * 2026-10-11 实测：启动即在 {@code Task.<clinit>} 崩掉）。
     * {@code Unsafe.putObject/putInt} 配合 {@code objectFieldOffset} 不需要 open 模块。
     * <p>
     * <b>找字段必须沿父类链：</b>{@code name}/{@code ordinal} 声明在父类 {@code java.lang.Enum} 上，
     * {@code Task.class.getDeclaredField("name")} 会抛 {@code NoSuchFieldException}
     * （2026-10-11 第二次实测崩在这里），所以走 {@link #findField}。
     */
    private static PlayerMoodComponent.Task newEnumConstant(int ordinal, String name) {
        try {
            PlayerMoodComponent.Task slot = (PlayerMoodComponent.Task) UnsafeHolder.ALLOCATE_INSTANCE
                    .invoke(UnsafeHolder.UNSAFE, PlayerMoodComponent.Task.class);

            long nameOffset = (long) UnsafeHolder.OBJECT_FIELD_OFFSET
                    .invoke(UnsafeHolder.UNSAFE, findField("name"));
            UnsafeHolder.PUT_OBJECT.invoke(UnsafeHolder.UNSAFE, slot, nameOffset, name);

            long ordinalOffset = (long) UnsafeHolder.OBJECT_FIELD_OFFSET
                    .invoke(UnsafeHolder.UNSAFE, findField("ordinal"));
            UnsafeHolder.PUT_INT.invoke(UnsafeHolder.UNSAFE, slot, ordinalOffset, ordinal);

            return slot;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to create Task enum constant '" + name + "'", e);
        }
    }

    /**
     * 沿父类链查找 {@code Task} 上的字段（含继承自 {@code java.lang.Enum} 的 {@code name}/{@code ordinal}）。
     * <p>
     * 沿链拿到的是**声明该字段的那个类**的 {@code Field}，这正是
     * {@code Unsafe.objectFieldOffset} 所要求的（它拒绝「继承来的字段」）。
     */
    private static Field findField(String fieldName) throws NoSuchFieldException {
        for (Class<?> c = PlayerMoodComponent.Task.class; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // 继续往父类找
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**
     * 把新槽位的 {@code setFunction} 指向本模组工厂，让 vanilla 的 NBT / 同步反序列化认得它。
     * <p>
     * 这里同样走 Unsafe 而不是反射 {@code Field#set}：该字段是 {@code final} 的，
     * {@code Field#set} 在 final 字段上不保证成功（Java 21 会拒绝部分场景），
     * 而 {@code Unsafe.putObject} + {@code objectFieldOffset} 直接写内存，稳定且与上面的
     * {@code name}/{@code ordinal} 用法一致。
     */
    private static void installSetFunction(PlayerMoodComponent.Task slot,
                                          Function<NbtCompound, PlayerMoodComponent.TrainTask> factory) {
        try {
            long offset = (long) UnsafeHolder.OBJECT_FIELD_OFFSET
                    .invoke(UnsafeHolder.UNSAFE, findField("setFunction"));
            UnsafeHolder.PUT_OBJECT.invoke(UnsafeHolder.UNSAFE, slot, offset, factory);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("failed to install setFunction for task slot", e);
        }
    }
}
