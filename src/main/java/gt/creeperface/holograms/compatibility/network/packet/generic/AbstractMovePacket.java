package gt.creeperface.holograms.compatibility.network.packet.generic;

import gt.creeperface.holograms.compatibility.network.packet.PacketHolder;

/**
 * @author CreeperFace
 */
public abstract class AbstractMovePacket implements PacketHolder {

    public abstract void setX(double x);

    public abstract void setY(double y);

    public abstract void setZ(double z);

    public abstract void setEntityId(long id);

    public abstract void setOnGround(boolean onGround);
}
