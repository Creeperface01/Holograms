package gt.creeperface.holograms.placeholder;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@ToString
public abstract class MatchedPlaceholder implements Cloneable {
    public final String raw;
    public final String name;
    public final int start;
    public final int end;

    public String value;

    public int offset = 0;

    @Override
    public MatchedPlaceholder clone() {
        try {
            return (MatchedPlaceholder) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
