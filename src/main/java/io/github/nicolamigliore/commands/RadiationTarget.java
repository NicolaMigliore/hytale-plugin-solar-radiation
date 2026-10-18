package io.github.nicolamigliore.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import javax.annotation.Nullable;

final class RadiationTarget {
    private RadiationTarget() {}

    @Nullable
    static Ref<EntityStore> resolve(Ref<EntityStore> self, boolean target,
                                     Store<EntityStore> store, PlayerRef recipient) {
        if (!target) return self;
        var ref = TargetUtil.getTargetEntity(self, 10f, store);
        if (ref != null && ref.isValid() && (store.getComponent(ref, Player.getComponentType()) != null
                || store.getComponent(ref, NPCEntity.getComponentType()) != null)) return ref;
        recipient.sendMessage(Message.raw("Look at a player or mob within 10 blocks."));
        return null;
    }
}
