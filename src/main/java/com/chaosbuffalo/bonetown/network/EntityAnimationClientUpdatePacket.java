package com.chaosbuffalo.bonetown.network;


import com.chaosbuffalo.bonetown.BoneTown;
import com.chaosbuffalo.bonetown.entity.IBTAnimatedEntity;
import com.chaosbuffalo.bonetown.entity.animation_state.AnimationComponent;
import com.chaosbuffalo.bonetown.entity.animation_state.messages.AnimationMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class EntityAnimationClientUpdatePacket {
    private int entityId;
    private List<AnimationMessage> messages;

    public EntityAnimationClientUpdatePacket(Entity entity, List<AnimationMessage> messages){
        entityId = entity.getEntityId();
        this.messages = messages;
    }

    public EntityAnimationClientUpdatePacket(PacketBuffer buffer){
        entityId = buffer.readInt();
        int count = buffer.readInt();
        messages = new ArrayList<>();
        for (int i=0; i < count; i++){
            AnimationMessage message = NetworkDeserializers.animationMessageDeserializer.deserialize(buffer);
            if (message == null){
                BoneTown.LOGGER.error("Error decoding EntityAnimationClientUpdatePacket for Entity: {}",
                        entityId);
                break;
            }
            messages.add(message);
        }
    }

    public void toBytes(PacketBuffer buffer){
        buffer.writeInt(entityId);
        buffer.writeInt(messages.size());
        for (AnimationMessage message : messages){
            message.toPacketBuffer(buffer);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            World world = Minecraft.getInstance().world;
            if (world == null) {
                return;
            }
            Entity entity = world.getEntityByID(entityId);
            if (entity instanceof IBTAnimatedEntity){
                AnimationComponent<?> component = ((IBTAnimatedEntity<?>) entity).getAnimationComponent();
                for (AnimationMessage message : messages){
                    component.updateState(message);
                }
            }
        });
        ctx.setPacketHandled(true);
    }


}
