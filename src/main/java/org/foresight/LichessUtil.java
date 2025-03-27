package org.foresight;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LichessUtil {

    static final String lichessURL =  "https://lichess.org";
    static final String authToken = "Bearer lip_lqnhrpo7eLhICGnMguok";

    static List<GameEvent> listOfGames = new ArrayList<>();

    public void listenToEvents() {
        String apiUrl = lichessURL + "/api/stream/event";
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/x-ndjson")
                .headers("Authorization", authToken)
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(HttpResponse::body)
                .thenAccept(inputStream -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        String line;

                        while ((line = reader.readLine()) != null) {
                            try {
                                if (!line.isEmpty()) {
                                    System.out.println(line);
                                    JSONObject json = new JSONObject(line);
                                    String gameType = json.get("type").toString();
                                    JSONObject gameDetails;

                                    switch (gameType) {
                                        case "gameStart":
                                            System.out.println("New Game started");
                                            gameDetails = (JSONObject) json.get("game");
                                            listOfGames.add(new GameEvent(gameDetails.get("gameId").toString(), gameDetails.get("color").toString()));
                                            break;
                                        case "gameFinish":
                                            gameDetails = (JSONObject) json.get("game");
                                            List<GameEvent> gameOverList = listOfGames
                                                    .stream()
                                                    .filter(game -> game.getGameId().equals(gameDetails.get("gameId").toString()))
                                                    .collect(Collectors.toList());
                                            gameOverList.get(0).closeGame();
                                            listOfGames.removeIf(game -> game.getGameId().equals(gameDetails.get("gameId")));
                                            break;
                                        case "challenge":
                                            gameDetails = (JSONObject) json.get("challenge");
                                            System.out.println("Got a new challenge");
                                            listOfGames.add(new GameEvent(gameDetails.get("gameId").toString(), gameDetails.get("color").toString()));
//                                        listenToGameEvents(gameDetails.get("gameId").toString(), gameDetails.get("color").toString());
                                            break;
                                    }
                                }

                            } catch (Exception e) {
                                System.out.println("Wrong msg was: " + line);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Random Exception: ");
                    }
                }).join();
    }

    public CompletableFuture<InputStream> listenToGameEvents(String gameId) {
        String apiUrl = lichessURL + "/api/bot/game/stream/" + gameId;
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/x-ndjson")
                .headers("Authorization", authToken)
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                        .thenApply(HttpResponse::body);
    }

    public void makeMove(String gameId, String move) {
        HttpClient client = HttpClient.newHttpClient();
        String apiUrl = lichessURL + String.format("/api/bot/game/%s/move/%s", gameId, move);
        System.out.println("Move made: " + move);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/x-ndjson")
                .header("Authorization", authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.statusCode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}

