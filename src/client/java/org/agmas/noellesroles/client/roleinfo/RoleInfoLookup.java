package org.agmas.noellesroles.client.roleinfo;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/** Shared client-side lookups for the local player's role and role colours. */
public final class RoleInfoLookup {
    private RoleInfoLookup() {
    }

    public static String currentRoleId() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? null : currentRoleId(client.player);
    }

    public static String currentRoleId(ClientPlayerEntity player) {
        if (player == null) {
            return null;
        }
        GameWorldComponent gwc = GameWorldComponent.KEY.get(player.getWorld());
        for (Role role : WatheRoles.ROLES) {
            if (gwc.isRole(player, role)) {
                return role.identifier().toString();
            }
        }
        return null;
    }

    public static Integer roleColor(String roleId) {
        if (roleId == null || roleId.startsWith("category:")) {
            return null;
        }
        for (Role role : WatheRoles.ROLES) {
            if (role.identifier().toString().equals(roleId)) {
                return role.color();
            }
        }
        return null;
    }
}
