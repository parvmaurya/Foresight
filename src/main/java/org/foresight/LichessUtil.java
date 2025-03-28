package org.foresight;

import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class LichessUtil {

    static final String lichessURL = SecretsUtil.readLichessURL();
    static final String authToken = SecretsUtil.readLichessSecret();

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

}

