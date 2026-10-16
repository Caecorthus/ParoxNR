/**
 * <b>已废弃（deprecated）：幻觉系统的内容。</b>
 *
 * <p>用户 2026-10-11 决定标注废弃：不要再修改、不要在此基础上扩展，也不要因为新机制与这里的旧口径
 * 不一致就顺手改动它（先例：{@code HallucinationHelper.hasSleepTaskCompleted} 只认原版
 * {@code Task.SLEEP}，拿着噬梦者的自定义睡觉任务睡完不会清除幻觉 —— 用户明确要求不改）。
 *
 * <p>幻觉主要由迷乱谋杀模式的「战争迷雾」事件触发；该模式本身已在
 * {@code org.agmas.noellesroles.murdermayhem} 标注废弃，并从模式投票与游戏内说明中隐藏。
 *
 * <p><b>例外（本包内但并未废弃）：</b>{@code KillRewardResolver} / {@code KillRewardContext} /
 * {@code KillRewardResult} —— 它们是**所有**击杀奖励的共用结算器（噬梦者的永眠击杀 100 金币口径
 * 就写在里面），仍属活代码，需要正常维护。这也是本包只做「包级 {@code @Deprecated}」、
 * 不给每个类加 {@code @Deprecated} 注解的原因：那些注解会让引用它们的活代码产生废弃警告。
 */
@Deprecated
package org.agmas.noellesroles.hallucination;
