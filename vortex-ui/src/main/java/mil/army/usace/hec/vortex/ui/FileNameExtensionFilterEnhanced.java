package mil.army.usace.hec.vortex.ui;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileNameExtensionFilterEnhanced extends FileFilter {
    private final String description;
    private final List<String> extensions;

    public FileNameExtensionFilterEnhanced(String description, String... extensions) {
        if (extensions == null || extensions.length == 0)
            throw new IllegalArgumentException("Extensions must be non-null and not empty");

        this.description = description;
        this.extensions = new ArrayList<>();

        Arrays.stream(extensions).forEach(e -> {
            if(e == null)
                throw new IllegalArgumentException("Each extension must be non-null");
            else
                this.extensions.add(e);
        });
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File f) {
        if(f.isDirectory())
            return true;

        return this.extensions.stream().anyMatch(e -> f.getName().endsWith(e));
    }
}