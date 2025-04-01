package mil.army.usace.hec.vortex.io;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Validation {
    private final boolean isValid;
    private final Set<String> messages = new LinkedHashSet<>();

    private Validation(boolean isValid, List<String> messages) {
        this.isValid = isValid;
        this.messages.addAll(messages);
    }

    public static Validation of(boolean isValid) {
        return new Validation(isValid, Collections.emptyList());
    }

    public static Validation of(boolean isValid, String message) {
        return new Validation(isValid, List.of(message));
    }

    public static Validation of(boolean isValid, List<String> messages) {
        return new Validation(isValid, messages);
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getMessages() {
        return List.copyOf(messages);
    }
}
