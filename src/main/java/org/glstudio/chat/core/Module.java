package org.glstudio.chat.core;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum Module {
    MENTION_SOUNDS("mention-sounds"),
    JOIN_QUIT_MESSAGES("join-quit-messages"),
    ANTI_CAP("anti-cap"),
    CHAT_FORMAT("chat-format"),
    CHAT_FILTER("chat-filter"),
    LINK_BLOCKER("link-blocker"),
    COMMAND_SPY("command-spy"),
    CHAT_COOLDOWN("chat-cooldown"),
    PRIVATE_MESSAGES("private-messages");

    private final String key;

    Module(String key) {
        this.key = key;
    }

    public static Set<String> keys() {
        return Arrays.stream(values()).map(Module::getKey).collect(Collectors.toUnmodifiableSet());
    }
}
