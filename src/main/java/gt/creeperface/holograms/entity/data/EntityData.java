package gt.creeperface.holograms.entity.data;

import cn.nukkit.entity.data.EntityMetadata;

/**
 * @author CreeperFace
 */
public class EntityData extends EntityMetadata implements Cloneable {

    @Override
    public EntityData clone() {
        try {
            return (EntityData) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
