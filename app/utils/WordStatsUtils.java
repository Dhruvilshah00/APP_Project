/**
 * Utility class for Words Stats
 *
 *
 * @author Uday Jain
 */
package utils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for word frequency statistics.
 *
 * @author Uday Jain
 */
public class WordStatsUtils {

    /**
     * Common stop words to be filtered out during word frequency calculation.
     */
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "is", "in", "with", "a", "of", "to");

    /**
     * Calculates the frequency of each word in the given list of descriptions.
     *
     * @param descriptions the list of text descriptions to analyze
     * @return a map of words and their corresponding frequencies
     * @author Uday Jain
     */
    public static Map<String, Long> calculateWordFrequencies(List<String> descriptions) {
        return descriptions.stream()
                .flatMap(description -> Arrays.stream(description.toLowerCase().split("\\W+"))) // Split by non-word characters
                .filter(word -> !word.isEmpty() && !isCommonStopWord(word)) // Filter out empty and stop words
                .collect(Collectors.groupingBy(word -> word, Collectors.counting())); // Count occurrences of each word
    }

    /**
     * Checks if the given word is a common stop word.
     *
     * @param word the word to check
     * @return true if the word is a stop word; false otherwise
     * @author Uday Jain
     */
    private static boolean isCommonStopWord(String word) {
        return STOP_WORDS.contains(word);
    }

    /**
     * Sorts the word frequency map in descending order of frequency.
     *
     * @param wordStats the map of word frequencies to sort
     * @return a sorted map of words by descending frequency, in a LinkedHashMap to maintain order
     * @author Uday Jain
     */
    public static Map<String, Long> sortByFrequency(Map<String, Long> wordStats) {
        return wordStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // Sort by frequency
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new)); // Collect sorted entries into a LinkedHashMap
    }
}
