// 已废弃并停止使用（2026-10-11）：酒保「防御药剂」自视图 Mixin。
//
// 防御药剂已从游戏中移除，本 Mixin 唯一的作用（让酒保看见自己下的防御药剂 → 绿色 HAPPY_VILLAGER 粒子）
// 随之失去意义；而且它按「盘子的 poisoner 是不是酒保」判定，而安眠药不绑定身份，
// 导致酒保下安眠药时误显示绿粒子，并因 order=1001 + ci.cancel() 抢在
// SleepingPillPlateViewMixin(order=900) 之前，把安眠药盘的蓝粒子整条跳过。
// 删除后安眠药盘交由 SleepingPillPlateViewMixin 处理（蓝粒子 + 压掉原版红粒子）。
//
// 已从 noellesroles.client.mixins.json 注销；本文件可以直接删除（仅因文件沙箱不允许删除而保留）。
package org.agmas.noellesroles.client.mixin.bartender;

final class DefenseVialViewMixin {
    private DefenseVialViewMixin() {}
}
