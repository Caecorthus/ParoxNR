package org.agmas.noellesroles.client.screen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.agmas.noellesroles.Noellesroles;
import org.agmas.noellesroles.packet.SchulteClickC2SPacket;
import org.agmas.noellesroles.packet.SchulteExitC2SPacket;
import org.agmas.noellesroles.packet.SchulteResultS2CPacket;
import org.agmas.noellesroles.packet.SchulteStartS2CPacket;

/**
 * 舒尔特方格修复小游戏：按 1→N² 顺序点击打乱后的数字，限时完成即修复该破坏点的一个等级。
 * 校验以服务端为权威，本界面做乐观展示并上报每次点击。
 */
public class SchulteScreen extends Screen {
    private static final int CELL = 26;
    private static final int GAP = 4;

    private final int pointId;
    private final int level;
    private final int size;
    private final int total;
    private final int[] sequence;
    private int nextIndex;
    private int ticksRemaining;
    private final int timeLimitTicks;
    private boolean awaitingResult;
    private boolean finishedLocally;

    private int gridLeft;
    private int gridTop;

    public SchulteScreen(SchulteStartS2CPacket start) {
        super(Text.translatable("screen.schulte.title"));
        this.pointId = start.pointId();
        this.level = start.level();
        this.size = start.size();
        this.total = size * size;
        this.sequence = start.sequence().clone();
        this.nextIndex = Math.min(start.nextIndex(), total);
        this.timeLimitTicks = start.timeLimitTicks();
        this.ticksRemaining = Math.max(0, start.ticksRemaining());
    }

    @Override
    protected void init() {
        int grid = size * CELL + (size - 1) * GAP;
        this.gridLeft = (this.width - grid) / 2;
        // 顶部信息行下方起排；5x5 时刚好落在退出按钮上方
        this.gridTop = Math.max(this.height / 2 - grid / 2, this.height / 2 - 56);

        for (int i = 0; i < total; i++) {
            int row = i / size;
            int col = i % size;
            int value = sequence[i];
            int x = gridLeft + col * (CELL + GAP);
            int y = gridTop + row * (CELL + GAP);
            ButtonWidget cell = ButtonWidget.builder(Text.literal(String.valueOf(value)), btn -> onCellClick(value))
                    .dimensions(x, y, CELL, CELL)
                    .build();
            cell.active = !isCellDone(value);
            addDrawableChild(cell);
        }

        int gridHeight = size * CELL + (size - 1) * GAP;
        int exitY = Math.min(this.height - 28, gridTop + gridHeight + 4);
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.schulte.exit"), btn -> {
            ClientPlayNetworking.send(new SchulteExitC2SPacket(pointId, level));
            finishedLocally = true;
            close();
        }).dimensions(width / 2 - 60, exitY, 120, 20).build());
    }

    /** 已点完 1..nextIndex，数值不超过 nextIndex 的格子即为已完成。 */
    private boolean isCellDone(int value) {
        return value <= nextIndex;
    }

    private void onCellClick(int value) {
        if (awaitingResult || nextIndex >= total) return;
        ClientPlayNetworking.send(new SchulteClickC2SPacket(pointId, level, value));
        if (value != nextIndex + 1) {
            // 点错：本地立即判定失败并关闭（服务端同样按失败处理并施加冷却）
            awaitingResult = true;
            finishedLocally = true;
            close();
            return;
        }
        nextIndex++;
        if (nextIndex >= total) {
            awaitingResult = true; // 等待服务端确认本等级完成 / 整个破坏点修复
        }
        refreshCells();
    }

    private void refreshCells() {
        for (var child : children()) {
            if (child instanceof ButtonWidget button && button.getMessage().getString().chars().allMatch(Character::isDigit)) {
                int value = Integer.parseInt(button.getMessage().getString());
                button.active = !isCellDone(value) && !awaitingResult;
            }
        }
    }

    @Override
    public void tick() {
        if (awaitingResult || finishedLocally) return;
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    /** 由 S2C 结果包驱动：本等级完成或明确失败时关闭界面。 */
    public void handleResult(SchulteResultS2CPacket result) {
        if (result.pointId() != pointId || result.level() != level) return;
        finishedLocally = true;
        this.client.setScreen(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        RoleScreenHelper.renderRoleMenuBackground(context, this.width, this.height, Noellesroles.BREACHER.color() & 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);

        RoleScreenHelper.drawCenteredTitle(context, this.textRenderer, getTitle(), this.width / 2, gridTop - 60);
        RoleScreenHelper.drawCenteredSubTitle(context, this.textRenderer,
                Text.translatable("screen.schulte.size", size, size, total), this.width / 2, gridTop - 40);

        int seconds = (ticksRemaining + 19) / 20;
        int timeColor = seconds <= 5 ? 0xFFFF5555 : 0xFFFFFFFF;
        String progress = nextIndex + "/" + total;
        context.drawTextWithShadow(this.textRenderer,
                Text.translatable("screen.schulte.progress", progress),
                gridLeft, gridTop - 14, 0xFFE0A0A0);
        String timeText = Text.translatable("screen.schulte.time", seconds).getString();
        context.drawTextWithShadow(this.textRenderer, Text.literal(timeText),
                gridLeft + size * CELL + (size - 1) * GAP - this.textRenderer.getWidth(timeText), gridTop - 14, timeColor);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (!finishedLocally && !awaitingResult) {
            ClientPlayNetworking.send(new SchulteExitC2SPacket(pointId, level));
        }
        super.close();
    }
}
