package mil.army.usace.hec.vortex.io;

public enum ImageFileType {
    TIFF("TIFF"),
    ASC("ASC");

    public final String label;

    private ImageFileType(String label) {
        this.label = label;
    }
}
