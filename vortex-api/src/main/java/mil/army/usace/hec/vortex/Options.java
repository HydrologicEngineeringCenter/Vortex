package mil.army.usace.hec.vortex;

import java.util.HashMap;
import java.util.Map;

public class Options {

    private Map<String, String> options = new HashMap<>();

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
