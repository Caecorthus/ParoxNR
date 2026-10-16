package org.agmas.noellesroles.client.screen;

import dev.doctor4t.wathe.api.Faction;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.agmas.noellesroles.attendant.AttendantPlayerComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 乘务登记面板新旧两个实现共用的文本与角色列表工具。 */
public final class AttendantUi {
    private AttendantUi() {
    }

    public static Text triStateText(byte value) {
        return switch (value) {
            case AttendantPlayerComponent.YES -> Text.translatable("screen.attendant.value.yes");
            case AttendantPlayerComponent.NO -> Text.translatable("screen.attendant.value.no");
            default -> Text.translatable("screen.attendant.value.unknown");
        };
    }

    public static int triStateColor(byte value) {
        return switch (value) {
            case AttendantPlayerComponent.YES -> 0xFF2F6B34;
            case AttendantPlayerComponent.NO -> 0xFFA3141C;
            default -> 0xFF6B5641;
        };
    }

    public static Text roleText(String roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return Text.translatable("screen.attendant.value.unknown");
        }
        Identifier roleIdentifier = Identifier.tryParse(roleId);
        if (roleIdentifier == null) {
            return Text.translatable("screen.attendant.value.unknown");
        }
        return Text.translatable("announcement.role." + roleIdentifier.getPath());
    }

    public static Text roomText(String roomName) {
        return roomName == null || roomName.isEmpty()
                ? Text.translatable("screen.attendant.value.unknown")
                : Text.literal(roomName);
    }

    public static String displayName(String name) {
        return name == null || name.isEmpty()
                ? Text.translatable("screen.role_target.unknown_player").getString()
                : name;
    }

    public static List<Role> selectableRoles(GameWorldComponent gameWorldComponent) {
        List<Role> roles = new ArrayList<>();
        for (Role role : WatheRoles.ROLES) {
            if (WatheRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role.equals(WatheRoles.VIGILANTE)) continue;
            if (!gameWorldComponent.isRoleEnabled(role)) continue;
            roles.add(role);
        }
        return roles;
    }

    public record FactionGroup(Faction faction, List<Role> roles) {
    }

    /** 可选角色按 乘客→杀手→中立 阵营分组（空阵营不返回）。 */
    public static List<FactionGroup> groupedRoles(GameWorldComponent gameWorldComponent) {
        List<Role> roles = selectableRoles(gameWorldComponent);
        List<FactionGroup> groups = new ArrayList<>();
        for (Faction faction : new Faction[]{Faction.CIVILIAN, Faction.KILLER, Faction.NEUTRAL}) {
            List<Role> group = new ArrayList<>();
            for (Role role : roles) {
                if (role.getFaction() == faction) {
                    group.add(role);
                }
            }
            if (!group.isEmpty()) {
                groups.add(new FactionGroup(faction, group));
            }
        }
        return groups;
    }

    public static Text factionText(Faction faction) {
        return switch (faction) {
            case KILLER -> Text.translatable("roleinfo.category.killer.name");
            case NEUTRAL -> Text.translatable("roleinfo.category.neutral.name");
            default -> Text.translatable("roleinfo.category.passenger.name");
        };
    }

    /** 见面筛选值文本：0=全部 1=见面 2=未见面。 */
    public static Text metFilterText(int filter) {
        return switch (filter) {
            case 1 -> Text.translatable("screen.attendant.filter.met");
            case 2 -> Text.translatable("screen.attendant.filter.unmet");
            default -> Text.translatable("screen.attendant.filter.all");
        };
    }

    /** 存活筛选值文本：0=全部 1=存活 2=死亡。 */
    public static Text aliveFilterText(int filter) {
        return switch (filter) {
            case 1 -> Text.translatable("screen.attendant.filter.alive");
            case 2 -> Text.translatable("screen.attendant.filter.dead");
            default -> Text.translatable("screen.attendant.filter.all");
        };
    }

    /** 登记条目是否通过筛选：未见面=非"是"，存活=非"否"（未知计入未见面/存活）。 */
    public static boolean matchesFilter(AttendantPlayerComponent.Entry entry, int metFilter, int aliveFilter) {
        if (metFilter == 1 && entry.getMet() != AttendantPlayerComponent.YES) return false;
        if (metFilter == 2 && entry.getMet() == AttendantPlayerComponent.YES) return false;
        if (aliveFilter == 1 && entry.getAlive() == AttendantPlayerComponent.NO) return false;
        if (aliveFilter == 2 && entry.getAlive() != AttendantPlayerComponent.NO) return false;
        return true;
    }

    /**
     * 职业重复提醒：所选职业已被标记给其他玩家时返回提示文本（义警与平民允许重复）。
     */
    @Nullable
    public static Text duplicateRoleWarning(AttendantPlayerComponent component, UUID selectedUuid, String roleId) {
        if (roleId == null || roleId.isEmpty()) {
            return null;
        }
        Identifier roleIdentifier = Identifier.tryParse(roleId);
        if (roleIdentifier == null
                || roleIdentifier.equals(WatheRoles.VIGILANTE.identifier())
                || roleIdentifier.equals(WatheRoles.CIVILIAN.identifier())) {
            return null;
        }
        for (AttendantPlayerComponent.Entry entry : component.getEntryList()) {
            if (entry.getUuid().equals(selectedUuid)) {
                continue;
            }
            if (roleId.equals(entry.getRoleId())) {
                return Text.translatable("tip.noellesroles.attendant.role_duplicate",
                        roleText(roleId), displayName(entry.getName()));
            }
        }
        return null;
    }
}
