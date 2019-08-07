package gt.creeperface.holograms.compatibility.network.packet.generic;

import cn.nukkit.entity.Attribute;
import cn.nukkit.entity.data.EntityMetadata;
import gt.creeperface.holograms.compatibility.network.packet.PacketHolder;

/**
 * @author CreeperFace
 */
public abstract class AbstractAddPacket implements PacketHolder {

    public abstract void setX(double x);

    public abstract void setY(double y);

    public abstract void setZ(double z);

    public abstract void setEntityId(long id);

    public abstract void setMetadata(EntityMetadata metadata);

    public void setAttributes(Attribute[] attributes) {

    }

    public void setName(String name) {

    }
}
