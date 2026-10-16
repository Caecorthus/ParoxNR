package org.agmas.noellesroles.client.mixin.taskpoint;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.agmas.noellesroles.task.NoellesTaskTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 心情 HUD 任务行的文本调整。
 *
 * <ol>
 *   <li><b>按任务声明去掉前缀</b>：wathe 的任务行文案是「前缀 + 任务名」，前缀按心情类型取
 *       {@code task.fake}（「你可以假装」）或 {@code task.feel}（「你觉得该」）。
 *       自带完整句子的任务（例如噬梦者的「你被噬梦者盯上了：去睡觉！」）挂上前缀会变成
 *       「你可以假装你被噬梦者盯上了：去睡觉！」，所以这类任务在
 *       {@code NoellesTaskTypes.register(..., suppressPrefix=true, ...)} 里声明免前缀 ——
 *       <b>是逐任务声明的，不是"自定义任务一律免前缀"</b>，以后新增任务默认仍带前缀。</li>
 *   <li><b>补充任务点提示</b>：睡/呼吸新鲜空气任务在文案末尾追加操作提示；
 *       自带完整语义的任务用 {@code suppressTaskPointHint=true} 声明免提示。</li>
 * </ol>
 *
 * <p>wathe 的 {@code task.*} 键由 wathe 本体提供，本模组覆写其语言文件不生效，
 * 因此在渲染前直接拼接/替换。提示键只写进简中语言文件，用 {@code I18n.hasTranslation} 门控 —— 其他语言不显示。
 * {@code TaskRenderer} 是 {@code MoodRenderer} 的私有嵌套类，只能用 targets 字符串定位。
 */
@Mixin(targets = "dev.doctor4t.wathe.client.gui.MoodRenderer$TaskRenderer")
public abstract class TaskPointTaskTextMixin {

    /**
     * wathe 取「前缀」时用的语言键（按心情类型二选一）；任务名那次调用传的是任务名本身。
     * 用 {@code TranslatableTextContent#getKey()} 区分两次调用 —— {@code Text#getString()}
     * 返回的是**译文**（只有缺失键时才回退为键名），不能用来判断。
     */
    private static final String FEEL_PREFIX_KEY = "task.feel";
    private static final String FAKE_PREFIX_KEY = "task.fake";

    @Shadow public Text text;

    @ModifyExpressionValue(
            method = "tick(Ldev/doctor4t/wathe/cca/PlayerMoodComponent$TrainTask;FZ)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/text/Text;translatable(Ljava/lang/String;)Lnet/minecraft/text/MutableText;"
            )
    )
    private MutableText noellesroles$dropPrefixForCustomTask(MutableText original,
                                                             PlayerMoodComponent.TrainTask present,
                                                             float delta,
                                                             boolean isFakeMood) {
        // 免前缀是**按任务类型在注册时声明**的（NoellesTaskTypes.register 的 suppressPrefix），
        // 不是「所有自定义任务一律免前缀」——以后新增的任务默认沿用原版前缀行为。
        if (present == null || !NoellesTaskTypes.suppressesPrefix(present.getType())) {
            return original;
        }
        // 原语句是 Text.translatable(前缀).append(Text.translatable(任务名))，两次调用都命中本注入点。
        // 前缀那次的 content key 是 task.feel/task.fake，任务名那次是 task.<name>，以此区分。
        if (original.getContent() instanceof TranslatableTextContent translatable
                && (FEEL_PREFIX_KEY.equals(translatable.getKey()) || FAKE_PREFIX_KEY.equals(translatable.getKey()))) {
            return Text.literal("");
        }
        return original;
    }

    @Inject(method = "tick(Ldev/doctor4t/wathe/cca/PlayerMoodComponent$TrainTask;FZ)Z", at = @At("TAIL"))
    private void noellesroles$appendTaskPointHint(PlayerMoodComponent.TrainTask present, float delta,
                                                  boolean isFakeMood, CallbackInfoReturnable<Boolean> cir) {
        if (present == null) return;
        // 免提示同样是按任务类型声明（suppressTaskPointHint）；未声明的自定义任务照常走下面的提示映射。
        if (NoellesTaskTypes.suppressesTaskPointHint(present.getType())) return;
        String hintKey = switch (present.getName()) {
            case "sleep" -> "tip.noellesroles.taskpoint.sleep_hint";
            case "outside" -> "tip.noellesroles.taskpoint.outside_hint";
            default -> null;
        };
        if (hintKey == null || !I18n.hasTranslation(hintKey)) return;
        this.text = this.text.copy().append(Text.translatable(hintKey));
    }
}
