package gt.creeperface.holograms.compatibility.network.packet;

import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.network.protocol.AddEntityPacket;
import gt.creeperface.holograms.compatibility.network.packet.generic.AbstractAddPacket;
import lombok.Getter;

/**
 * @author CreeperFace
 */
public class EntityAddPacket extends AbstractAddPacket {

    @Getter
    private final AddEntityPacket packet = new AddEntityPacket();

    @Override
    public void setX(double x) {
        packet.x = (float) x;
    }

    @Override
    public void setY(double y) {
        packet.y = (float) y;
    }

    @Override
    public void setZ(double z) {
        packet.z = (float) z;
    }

    @Override
    public void setEntityId(long id) {
        packet.entityRuntimeId = id;
        packet.entityUniqueId = id;
    }

    @Override
    public void setMetadata(EntityMetadata metadata) {
        packet.metadata = metadata;
    }

    @Override
    public void setAttributes(Attribute[] attributes) {
        packet.attributes = attributes;
    }
}
