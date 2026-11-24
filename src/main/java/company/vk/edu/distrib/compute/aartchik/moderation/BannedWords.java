package company.vk.edu.distrib.compute.aartchik.moderation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

final class BannedWords {
    private final List<String> words;

    BannedWords() throws IOException {
        var input = BannedWords.class.getResourceAsStream("/moderation/banned_words.txt");
        if (input == null) {
            throw new IOException("Banned words dictionary is missing");
        }
        try (var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            words = reader.lines().map(String::strip).filter(word -> !word.isEmpty())
                    .map(word -> word.toLowerCase(Locale.ROOT)).toList();
        }
    }

    boolean rejects(String url) {
        String decoded = URLDecoder.decode(url, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return words.stream().anyMatch(decoded::contains);
    }
}
