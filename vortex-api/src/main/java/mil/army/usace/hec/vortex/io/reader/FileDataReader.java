package mil.army.usace.hec.vortex.io.reader;

import mil.army.usace.hec.vortex.VortexData;
import mil.army.usace.hec.vortex.VortexTimeRecord;
import mil.army.usace.hec.vortex.io.PropertyChangeNotifier;

import java.util.List;

public interface FileDataReader extends PropertyChangeNotifier {
    List<VortexData> getDtos();
    int getDtoCount();
    VortexData getDto(int idx);
    List<VortexTimeRecord> getTimeRecords();
}
