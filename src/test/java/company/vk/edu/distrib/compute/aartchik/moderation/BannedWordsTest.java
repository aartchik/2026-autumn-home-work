package company.vk.edu.distrib.compute.aartchik.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannedWordsTest {
    @Test
    void detectsEncodedAndMixedCaseWordsFromTheResource() throws Exception {
        BannedWords words = new BannedWords();
        assertTrue(words.rejects("https://example.com/%6Fbscene?q=1"));
        assertTrue(words.rejects("https://example.com/?q=CRUELTY"));
        assertTrue(words.rejects("https://example.com/offense"));
        assertFalse(words.rejects("https://example.com/articles?q=clean"));
    }
}
