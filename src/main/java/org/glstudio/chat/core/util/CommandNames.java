package org.glstudio.chat.core.util;

import java.util.List;
import java.util.Locale;

public final class CommandNames {
    private CommandNames() {
    }

    public static String base(String commandLine) {
        if (commandLine == null || commandLine.isEmpty()) {
            return "";
        }

        String token = commandLine.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);

        if (token.startsWith("/")) {
            token = token.substring(1);
        }

        int namespace = token.indexOf(':');
        if (namespace >= 0) {
            token = token.substring(namespace + 1);
        }

        return token;
    }

    public static boolean matches(String commandLine, List<String> names) {
        if (names == null || names.isEmpty()) {
            return false;
        }
        String base = base(commandLine);
        for (String name : names) {
            if (base.equals(base(name))) {
                return true;
            }
        }
        return false;
    }
}
