/**
 * HomeController handles HTTP requests to the application's home page.
 *
 * @author Uday Jain
 */
package controllers;

import org.json.JSONArray;
import org.json.JSONObject;
import play.mvc.*;
import utils.ReadabilityUtils;
import utils.SentimentUtils;
import utils.WordStatsUtils;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class HomeController extends Controller {

    // Store results for each query in a stack-like structure
    private Map<String, List<Map<String, String>>> allSearchResultsByQuery = new LinkedHashMap<>();
    ReadabilityUtils readabilityUtils = new ReadabilityUtils();
    /**
     * Displays the index page.
     *
     * @return a Result containing the rendered index view
     */
    public Result index() {
        return ok(views.html.index.render("Welcome to YT Lytics"));
    }

    /**
     * Fetches YouTube search results asynchronously based on the provided query.
     *
     * @param query the search query to use for fetching YouTube results
     * @return a CompletionStage containing the Result with the search results
     */
    public CompletionStage<Result> search(String query) {
        String apiKey = "AIzaSyAmOEbts1TlZNZmp_kmmaDPOUE6wONUf4k";  // Replace with your actual API key
        String youtubeUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=" +
                URLEncoder.encode(query, StandardCharsets.UTF_8) + "&maxResults=10&key=" + apiKey;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(youtubeUrl)).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenCompose(responseBody -> {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray items = jsonResponse.optJSONArray("items");

                    if (items == null) {
                        return CompletableFuture.completedFuture(internalServerError("No items found in response."));
                    }

                    List<Map<String, String>> videoList = new ArrayList<>();
                    List<String> descriptions = new ArrayList<>(); // Collect descriptions for sentiment and readability analysis

                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        JSONObject snippet = item.getJSONObject("snippet");
                        JSONObject idObject = item.getJSONObject("id");

                        Map<String, String> video = new HashMap<>();
                        video.put("title", snippet.getString("title"));
                        video.put("channelTitle", snippet.getString("channelTitle"));
                        video.put("description", snippet.getString("description"));
                        video.put("thumbnailUrl", snippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                        video.put("videoId", idObject.getString("videoId"));
                        video.put("channelId", snippet.getString("channelId"));
                        video.put("grade", String.format("%.2f", ReadabilityUtils.calculateFleschKincaidGradeLevel(snippet.getString("description"))));
                        video.put("score", String.format("%.2f", ReadabilityUtils.calculateFleschReadingEase(snippet.getString("description"))));
                        videoList.add(video);
                        // Add the description to the list for sentiment and readability analysis
                        descriptions.add(snippet.getString("description"));
                    }


                    // Analyze overall sentiment
                    String sentiment = SentimentUtils.analyzeSentiment(descriptions);

                    // Calculate average readability scores
                    double avgFleschKincaid = ReadabilityUtils.calculateAverageFleschKincaid(descriptions);
                    double avgFleschReadingEase = ReadabilityUtils.calculateAverageFleschReadingEase(descriptions);


                    // Insert new search results at the top of the map
                    Map<String, List<Map<String, String>>> updatedSearchResults = new LinkedHashMap<>();
                    updatedSearchResults.put(query, videoList);
                    updatedSearchResults.putAll(allSearchResultsByQuery);
                    allSearchResultsByQuery = updatedSearchResults;

                    // Pass sentiment and readability scores to the results view
                    return CompletableFuture.completedFuture(ok(views.html.results.render(allSearchResultsByQuery, sentiment, avgFleschKincaid, avgFleschReadingEase)));
                });
    }

    /**
     * Clears the search history.
     *
     * @return a Result redirecting to the index page
     */
    public Result clearHistory() {
        allSearchResultsByQuery.clear();
        return redirect(routes.HomeController.index());
    }

    /**
     * Fetches and displays video tags for a specific video.
     *
     * @param videoId     the ID of the video
     * @param videoTitle  the title of the video
     * @param channelTitle the title of the channel
     * @return a Result containing the rendered view with video tags
     */
    public Result viewVideoTags(String videoId, String videoTitle, String channelTitle, String channelId) {
        String apiKey = "AIzaSyAmOEbts1TlZNZmp_kmmaDPOUE6wONUf4k";  // Replace with your actual API key
        String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=" + videoId + "&key=" + apiKey;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());

            JSONArray items = jsonResponse.optJSONArray("items");
            String channelDescription = ""; // Default empty string for description
            List<String> tags = new ArrayList<>();

            if (items != null && items.length() > 0) {
                JSONObject snippet = items.getJSONObject(0).getJSONObject("snippet");
                JSONArray tagsArray = snippet.optJSONArray("tags");
                if (tagsArray != null) {
                    for (int i = 0; i < tagsArray.length(); i++) {
                        tags.add(tagsArray.getString(i));
                    }
                }

                // Fetch the channel description
                String channelInfoUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet&id=" + channelId + "&key=" + apiKey;
                HttpRequest channelRequest = HttpRequest.newBuilder().uri(URI.create(channelInfoUrl)).build();
                HttpResponse<String> channelResponse = client.send(channelRequest, HttpResponse.BodyHandlers.ofString());
                JSONObject channelJson = new JSONObject(channelResponse.body());

                if (channelJson.has("items") && channelJson.getJSONArray("items").length() > 0) {
                    JSONObject channelInfo = channelJson.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                    channelDescription = channelInfo.optString("description", "No description available.");
                }
            }

            // Render the tags page
            return ok(views.html.videoTags.render(videoId, videoTitle, channelTitle, channelId, channelDescription, tags));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Failed to fetch video tags.");
        }
    }

    /**
     * Searches for videos based on a tag.
     *
     * @param tag the tag to search by
     * @return a CompletionStage containing the Result with the search results
     */
    public CompletionStage<Result> searchByTag(String tag) {
        return search(tag);
    }

    /**
     * Displays word statistics for a given keyword.
     *
     * @param keyword the keyword to fetch video descriptions for
     * @return a Result containing the rendered view with sorted word statistics
     */
    public Result getWordStats(String keyword) {
        List<String> videoDescriptions = fetchVideoDescriptions(keyword);
        Map<String, Long> wordStats = WordStatsUtils.calculateWordFrequencies(videoDescriptions);
        Map<String, Long> sortedWordStats = WordStatsUtils.sortByFrequency(wordStats);
        return ok(views.html.wordStats.render(sortedWordStats));
    }

    /**
     * Fetches video descriptions for a given keyword.
     *
     * @param keyword the keyword to fetch video descriptions for
     * @return a list of video descriptions
     */
    private List<String> fetchVideoDescriptions(String keyword) {
        List<String> descriptions = new ArrayList<>();
        String apiKey = "AIzaSyAmOEbts1TlZNZmp_kmmaDPOUE6wONUf4k";  // Replace with your actual API key
        int maxResults = 50;

        try {
            String encodedQuery = URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String youtubeUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=" +
                    encodedQuery + "&maxResults=" + maxResults + "&order=date&key=" + apiKey;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(youtubeUrl)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JSONObject jsonResponse = new JSONObject(response.body());
            if (jsonResponse.has("items")) {
                JSONArray items = jsonResponse.getJSONArray("items");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject snippet = items.getJSONObject(i).getJSONObject("snippet");
                    String description = snippet.optString("description", "");
                    if (!description.isEmpty()) {
                        descriptions.add(description);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return descriptions;
    }

    /**
     * Fetches channel info and videos for a specific channel ID.
     *
     * @param channelId the ID of the channel
     * @return a CompletionStage containing the Result with the channel info and videos
     */
    public CompletionStage<Result> getChannelInfo(String channelId) {
        String apiKey = "AIzaSyAmOEbts1TlZNZmp_kmmaDPOUE6wONUf4k";  // Replace with your actual API key
        String channelInfoUrl = "https://www.googleapis.com/youtube/v3/channels?part=snippet,statistics&id=" + channelId + "&key=" + apiKey;
        String videosUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&channelId=" + channelId + "&maxResults=10&order=date&type=video&key=" + apiKey;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest channelRequest = HttpRequest.newBuilder().uri(URI.create(channelInfoUrl)).build();
        HttpRequest videosRequest = HttpRequest.newBuilder().uri(URI.create(videosUrl)).build();

        return client.sendAsync(channelRequest, HttpResponse.BodyHandlers.ofString())
                .thenCombine(client.sendAsync(videosRequest, HttpResponse.BodyHandlers.ofString()), (channelResponse, videosResponse) -> {
                    JSONObject channelJson = new JSONObject(channelResponse.body());
                    JSONObject videosJson = new JSONObject(videosResponse.body());

                    JSONObject channelInfo = channelJson.getJSONArray("items").getJSONObject(0).getJSONObject("snippet");
                    JSONObject channelStats = channelJson.getJSONArray("items").getJSONObject(0).getJSONObject("statistics");

                    Map<String, String> channelData = new HashMap<>();
                    channelData.put("title", channelInfo.getString("title"));
                    channelData.put("description", channelInfo.getString("description"));
                    channelData.put("thumbnailUrl", channelInfo.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                    channelData.put("subscriberCount", channelStats.optString("subscriberCount", "N/A"));

                    List<Map<String, String>> videoList = new ArrayList<>();
                    JSONArray videoItems = videosJson.getJSONArray("items");

                    for (int i = 0; i < videoItems.length(); i++) {
                        JSONObject snippet = videoItems.getJSONObject(i).getJSONObject("snippet");
                        String videoId = videoItems.getJSONObject(i).getJSONObject("id").getString("videoId");

                        Map<String, String> video = new HashMap<>();
                        video.put("title", snippet.getString("title"));
                        video.put("description", snippet.getString("description"));
                        video.put("thumbnailUrl", snippet.getJSONObject("thumbnails").getJSONObject("default").getString("url"));
                        video.put("videoId", videoId);

                        videoList.add(video);
                    }

                    return ok(views.html.channelInfo.render(channelData, videoList));
                });
    }
}
