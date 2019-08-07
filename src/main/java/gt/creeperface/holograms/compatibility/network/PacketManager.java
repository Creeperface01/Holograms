package gt.creeperface.holograms.compatibility.network;

import gt.creeperface.holograms.compatibility.network.packet.EntityAddPacket;
import gt.creeperface.holograms.compatibility.network.packet.NukkitXMovePacket;
import gt.creeperface.holograms.compatibility.network.packet.PlayerAddPacket;
import gt.creeperface.holograms.compatibility.network.packet.generic.AbstractAddPacket;
import gt.creeperface.holograms.compatibility.network.packet.generic.AbstractMovePacket;
import lombok.experimental.UtilityClass;

/**
 * @author CreeperFace
 */
@UtilityClass
public final class PacketManager {

    private static final Version version;

    static {
        Version version0;

        try {
            Class.forName("cn.nukkit.network.protocol.MoveEntityAbsolutePacket");
            version0 = Version.NUKKITX;
        } catch (ClassNotFoundException e) {
            version0 = Version.GT;
        }

        version = version0;
    }

    public static void init() {
    }

    public static AbstractMovePacket getMovePacket() {
//        switch (version) {
//            case NUKKITX:
//                return new NukkitXMovePacket();
//            case GT:
//                return new NukkitGTMovePacket();
//        }
//
//        return null;
        return new NukkitXMovePacket();
    }

    public static AbstractAddPacket getAddPacket(int protocol) {
        if (protocol <= 113) {
            return new PlayerAddPacket();
        }

        return new EntityAddPacket();
    }

    private enum Version {
        NUKKITX,
        GT
    }
}
