package gt.creeperface.holograms.grid.source;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.List;
import java.util.Map;

public class YamlGridSource extends MapGridSource {

    private final File file;

    public YamlGridSource(SourceParameters parameters, Map<String, Object> data) {
        super(parameters);
        Preconditions.checkArgument(data.containsKey("path"));
        Preconditions.checkArgument(data.containsKey("columns"));

        this.file = new File((String) data.get("path"));

        if (data.containsKey("columns")) {
            initColumns((List) data.get("columns"));
        }
    }

    @Override
    public String getIdentifier() {
        return "yaml";
    }

    @Override
    public void load(int offset, int limit) {

    }

    @Override
    public CallType getAllowedCallType() {
        return CallType.ANY;
    }
}
