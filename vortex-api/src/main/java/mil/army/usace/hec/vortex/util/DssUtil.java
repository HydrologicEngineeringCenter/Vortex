package mil.army.usace.hec.vortex.util;

import hec.heclib.dss.DSSPathname;

import java.util.*;

public class DssUtil {

    private DssUtil(){}

    public static Map<String, Set<String>> getPathnameParts(List<String> pathnames){
        Set<String> aParts = new HashSet<>();
        Set<String> bParts = new HashSet<>();
        Set<String> cParts = new HashSet<>();
        Set<String> fParts = new HashSet<>();
        pathnames.stream().map(DSSPathname::new).forEach(pathname ->{
            aParts.add(pathname.getAPart());
            bParts.add(pathname.getBPart());
            cParts.add(pathname.getCPart());
            fParts.add(pathname.getFPart());
        });
        Map<String, Set<String>> parts = new HashMap<>();
        parts.put("aParts", aParts);
        parts.put("bParts", bParts);
        parts.put("cParts", cParts);
        parts.put("fParts", fParts);
        return parts;
    }
}
