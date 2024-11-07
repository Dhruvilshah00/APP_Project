package utils;

import java.util.List;

public class SentimentUtils {

    public static String analyzeSentiment(List<String> descriptions) {
        int happyCount = 0;
        int sadCount = 0;

        for (String description : descriptions) {
            // Basic analysis - Adjust these as needed
            happyCount += countWords(description, List.of("happy", "joy", "awesome", "fantastic", "great", "wonderful", "love", "excited", "amazing", "glad",
                    "delight", "positive", "cheerful", "smile", "grin","yay", "cool", "fabulous", "splendid", "charming", "blissful", "content", "pleased", "satisfied", ":-)", ":)", ":D", "😊", "😃", "😁", "😄", "🥳", "😆",
                    "🤗"));
            sadCount += countWords(description, List.of("sad", "down", "terrible", "unhappy", "disappointed", "upset", "depress", "bad", "negative", "gloomy",
                    "heartbroken", "mourn", "sorrow", "cry", "miserable", "grief", "pain", "hurt", "broken-hearted", "unfortunate", "woeful", "dismal",":-(", ":(", ":'(", "😢",
                    "😭", "😞", "😔", "😟", "😩", "😫", "😖", "😿"));
        }

        int totalWords = happyCount + sadCount;
        if (totalWords == 0) {
            return ":-|"; // Neutral if no relevant words found
        }

        double happyPercentage = (double) happyCount / totalWords;
        double sadPercentage = (double) sadCount / totalWords;

        if (happyPercentage > 0.7) {
            return ":-)";
        } else if (sadPercentage > 0.7) {
            return ":-(";
        } else {
            return ":-|";
        }
    }

//    private static int countWords(String text, List<String> words) {
//        int count = 0;
//        for (String word : words) {
//            count += text.toLowerCase().split(word, -1).length - 1;
//        }
//        return count;
//    }
    private static int countWords(String text, List<String> words) {
        int count = 0;
        String lowerText = text.toLowerCase();

        for (String word : words) {
            String escapedWord = word.replace(")", "\\)").replace("(", "\\("); // Escape parentheses for emoticons
            count += (lowerText.length() - lowerText.replace(escapedWord, "").length()) / escapedWord.length();
        }

        return count;
    }
}

