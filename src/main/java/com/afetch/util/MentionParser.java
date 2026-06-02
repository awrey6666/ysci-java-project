package com.afetch.util;

import com.afetch.domain.entity.User;
import com.afetch.repository.UserRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionParser {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{3,50})");

    private MentionParser() {}

    public static Set<User> parse(String text, UserRepository userRepository) {
        Set<User> mentioned = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String username = matcher.group(1);
            userRepository.findByUsername(username).ifPresent(mentioned::add);
        }
        return mentioned;
    }
}
