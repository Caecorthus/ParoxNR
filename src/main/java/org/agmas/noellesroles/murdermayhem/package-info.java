/**
 * <b>已废弃（deprecated）：迷乱谋杀模式。</b>
 *
 * <p>用户 2026-10-11 决定标注废弃：该模式不再在模式投票中出现（
 * {@code MapVotingComponentMixin} 的 {@code buildModeChoices} 注入里把 {@code noellesroles:murder_mayhem}
 * 从可选模式移除，连带也不会被「随机模式」抽到），其内容也不再出现在游戏内说明（
 * {@code RoleInfoScreen} / {@code RoleInfoLegacyScreen} 不再构建该分类树节点）。
 *
 * <p>因此本包全部代码只作为历史实现保留，<b>不要再在此基础上扩展、也不要顺手"修一致性"</b>：
 * 不要因为新机制（例如自定义任务类型）与这里的旧口径不一致就去改动它。
 * 若确实需要恢复该模式，先去掉 {@code MapVotingComponentMixin} 里的过滤与两个说明界面的隐藏逻辑。
 *
 * <p>本包与 {@code org.agmas.noellesroles.hallucination}（幻觉）是同一批废弃内容。
 */
@Deprecated
package org.agmas.noellesroles.murdermayhem;
