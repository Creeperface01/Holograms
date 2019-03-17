package gt.creeperface.holograms.compatibility.network.packet;

import cn.nukkit.network.protocol.MoveEntityPacket;
import gt.creeperface.holograms.compatibility.network.packet.generic.AbstractMovePacket;
import lombok.Getter;

/**
 * @author CreeperFace
 */
public class NukkitGTMovePacket extends AbstractMovePacket {

    @Getter
    private final MoveEntityPacket packet = new MoveEntityPacket();

    @Override
    public void setX(double x) {
        packet.x = x;
    }

    @Override
    public void setY(double y) {
        packet.y = y;
    }

    @Override
    public void setZ(double z) {
        packet.z = z;
    }

    @Override
    public void setEntityId(long id) {
        packet.eid = id;
    }

    @Override
    public void setOnGround(boolean onGround) {
        packet.onGround = onGround;
    }
}
