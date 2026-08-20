package org.glstudio.chat.core.chat;

public record ChatResult(boolean cancelled, String message) {
    private static final ChatResult CANCELLED = new ChatResult(true, null);

    public static ChatResult allow(String message) {
        return new ChatResult(false, message);
    }

    public static ChatResult cancel() {
        return CANCELLED;
    }
}
