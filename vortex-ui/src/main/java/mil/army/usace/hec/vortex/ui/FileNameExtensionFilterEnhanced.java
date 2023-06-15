package mil.army.usace.hec.vortex.ui;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileNameExtensionFilterEnhanced extends FileFilter {
    private final String description;
    private final List<String> extensions;

    public FileNameExtensionFilterEnhanced(String description, String... extensions) {
        if (extensions == null || extensions.length == 0)
            throw new IllegalArgumentException("Extensions must be non-null and not empty");

        this.description = description;
        this.extensions = Stream.of(extensions)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory())
            return true;

        return this.extensions.stream()
                .anyMatch(e -> file.getName().toLowerCase().endsWith(e));
    }
}