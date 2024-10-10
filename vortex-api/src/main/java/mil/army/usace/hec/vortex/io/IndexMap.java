package mil.army.usace.hec.vortex.io;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class IndexMap<K> {
    private final Map<K, Integer> internalMap;

    private IndexMap(Map<K, Integer> internalMap) {
        this.internalMap = internalMap;
    }

    static <K> IndexMap<K> of(List<K> sortedList) {
        Map<K, Integer> map = IntStream.range(0, sortedList.size()).boxed()
                .collect(Collectors.toMap(sortedList::get, index -> index));
        return new IndexMap<>(map);
    }

    int indexOf(K object) {
        return internalMap.getOrDefault(object, -1);
    }
}
