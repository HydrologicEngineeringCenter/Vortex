package mil.army.usace.hec.vortex.geo;

import ucar.nc2.dt.GridCoordSystem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum IndexSearcherFactory {
    INSTANCE;

    private final Map<GridCoordSystem, IndexSearcher> cache = new ConcurrentHashMap<>();

    public IndexSearcher getOrCreate(GridCoordSystem gcs) {
        return cache.computeIfAbsent(gcs, s -> new IndexSearcher(gcs));
    }
}
