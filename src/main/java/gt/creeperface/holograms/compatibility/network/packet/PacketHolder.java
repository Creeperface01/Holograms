package gt.creeperface.holograms.compatibility.network.packet;

import cn.nukkit.network.protocol.DataPacket;

/**
 * @author CreeperFace
 */
public interface PacketHolder {

    DataPacket getPacket();

    void encodePacket(boolean markEncoded);
}
