package mil.army.usace.hec.vortex.geo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum IndexSearcherFactory {
    INSTANCE;

    private final Map<CoordinateAxes, IndexSearcher> cache = new ConcurrentHashMap<>();

    public IndexSearcher getOrCreate(CoordinateAxes coordinateAxes) {
        return cache.computeIfAbsent(coordinateAxes, s -> new IndexSearcher(coordinateAxes));
    }
}
