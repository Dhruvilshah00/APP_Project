package utils;

import java.util.List;
import java.util.stream.Collectors;

public class ReadabilityUtils {
    /**
     * Calculates the Flesch-Kincaid Grade Level.
     * @param text The text to analyze.
     * @return The Flesch-Kincaid Grade Level score.
     */
    public static double calculateFleschKincaidGradeLevel(String text) {

        int wordCount = countWords(text);
        int sentenceCount = countSentences(text);
        int syllableCount = countSyllables(text);

        if (wordCount == 0 || sentenceCount == 0) {
            return 0.0; // Avoid division by zero
        }

        return 0.39 * ((double) wordCount / sentenceCount) + 11.8 * ((double) syllableCount / wordCount) - 15.59;
    }

    /**
     * Calculates the Flesch Reading Ease Score.
     * @param text The text to analyze.
     * @return The Flesch Reading Ease score.
     */
    public static double calculateFleschReadingEase(String text) {
        int wordCount = countWords(text);
        int sentenceCount = countSentences(text);
        int syllableCount = countSyllables(text);

        if (wordCount == 0 || sentenceCount == 0) {
            return 0.0; // Avoid division by zero
        }

        return 206.835 - 1.015 * ((double) wordCount / sentenceCount) - 84.6 * ((double) syllableCount / wordCount);
    }

    // Helper methods
    private static int countWords(String text) {
        return text.trim().split("\\s+").length;
    }

    private static int countSentences(String text) {
        return text.split("[.!?]").length;
    }

    private static int countSyllables(String word) {
        String vowels = "aeiouy";
        word = word.toLowerCase().replaceAll("[^a-z]", "");
        int syllableCount = 0;
        boolean lastWasVowel = false;

        for (char c : word.toCharArray()) {
            boolean isVowel = vowels.indexOf(c) != -1;
            if (isVowel && !lastWasVowel) {
                syllableCount++;
            }
            lastWasVowel = isVowel;
        }

        if (word.endsWith("e") && syllableCount > 1) {
            syllableCount--;
        }
        return syllableCount > 0 ? syllableCount : 1;
    }

    public static double calculateAverageFleschKincaid(List<String> texts) {
        return texts.stream()
                .mapToDouble(ReadabilityUtils::calculateFleschKincaidGradeLevel)
                .average()
                .orElse(0.0);
    }

    public static double calculateAverageFleschReadingEase(List<String> texts) {
        return texts.stream()
                .mapToDouble(ReadabilityUtils::calculateFleschReadingEase)
                .average()
                .orElse(0.0);
    }
}
