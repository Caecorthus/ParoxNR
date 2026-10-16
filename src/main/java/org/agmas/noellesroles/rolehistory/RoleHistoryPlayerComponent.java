package org.agmas.noellesroles.rolehistory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.Noellesroles;
import org.jetbrains.annotations.NotNull;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录玩家最近 3 个已完结局的最终角色,用于开局角色抽取时避开重复。
 */
public class RoleHistoryPlayerComponent implements Component {
    public static final ComponentKey<RoleHistoryPlayerComponent> KEY = ComponentRegistry.getOrCreate(
            Identifier.of(Noellesroles.MOD_ID, "role_history"),
            RoleHistoryPlayerComponent.class
    );

    private final ArrayDeque<Identifier> recentRoles = new ArrayDeque<>();

    public RoleHistoryPlayerComponent(PlayerEntity player) {
    }

    /** 对局胜负确定时调用:记录本局最终角色 */
    public void recordRole(Identifier roleId) {
        RoleHistoryCalculator.pushToWindow(this.recentRoles, roleId);
    }

    /** 最近3局角色 id 快照(最旧在前) */
    public List<Identifier> getRecentRoleIdentifiers() {
        return new ArrayList<>(this.recentRoles);
    }

    /** 调试命令用:清空历史 */
    public void clear() {
        this.recentRoles.clear();
    }

    /** 调试命令用:直接设置近3局角色(超窗自动截断) */
    public void setRecentRoles(List<Identifier> roles) {
        this.recentRoles.clear();
        if (roles != null) {
            for (Identifier roleId : roles) {
                RoleHistoryCalculator.pushToWindow(this.recentRoles, roleId);
            }
        }
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList ids = new NbtList();
        for (Identifier roleId : this.recentRoles) {
            ids.add(NbtString.of(roleId.toString()));
        }
        tag.put("recentRoles", ids);
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.recentRoles.clear();
        if (tag.contains("recentRoles")) {
            NbtList ids = tag.getList("recentRoles", NbtString.STRING_TYPE);
            for (int i = 0; i < ids.size(); i++) {
                Identifier roleId = Identifier.tryParse(ids.getString(i));
                if (roleId != null) {
                    RoleHistoryCalculator.pushToWindow(this.recentRoles, roleId);
                }
            }
        }
    }
}
