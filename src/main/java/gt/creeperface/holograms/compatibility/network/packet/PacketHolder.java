package gt.creeperface.holograms.compatibility.network.packet;

import cn.nukkit.network.protocol.DataPacket;

/**
 * @author CreeperFace
 */
public interface PacketHolder {

    DataPacket getPacket();

    default void encodePacket(boolean markEncoded) {
        DataPacket pk = getPacket();

        pk.encode();

        if (markEncoded)
            pk.isEncoded = true;
    }
}
