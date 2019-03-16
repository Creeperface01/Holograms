package gt.creeperface.holograms.compatibility.network.packet;

import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.MoveEntityPacket;
import gt.creeperface.holograms.compatibility.network.packet.generic.AbstractMovePacket;

/**
 * @author CreeperFace
 */
public class NukkitGTMovePacket extends AbstractMovePacket {

    private final MoveEntityPacket packet = new MoveEntityPacket();

    @Override
    public DataPacket getPacket() {
        return packet;
    }

    @Override
    public void encodePacket(boolean markEncoded) {
        packet.encode();

        if (markEncoded) {
            packet.isEncoded = true;
        }
    }

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
