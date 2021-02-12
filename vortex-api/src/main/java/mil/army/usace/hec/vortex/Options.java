package mil.army.usace.hec.vortex;

import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated since 0.10.16, use a Map instead
 */
@Deprecated
public class Options {

    private final Map<String, String> options = new HashMap<>();

    private Options(){}

    public static Options create(){
        return new Options();
    }

    public void add(String key, String value){
        options.put(key, value);
    }

    public Map<String, String> getOptions() {
        return options;
    }
}
