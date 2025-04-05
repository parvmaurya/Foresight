package org.foresight;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class GameEvent {

    String gameId;
    String side;

    final String lichessURL = SecretsUtil.readLichessURL();
    final String authToken = SecretsUtil.readLichessSecret();

    GameEvent(String gameId, String side) {
        this.gameId = gameId;
        this.side = side;
        listenToGameEvents();
    }

    String getGameId() {
        return this.gameId;
    }

    Board updateBoardWithMoves(Board board, String moves) {
        if(!moves.isEmpty()) {
            Arrays.asList(moves.split(" "))
                    .forEach(move -> board.doMove(new Move(move, board.getSideToMove())));
        }
        return board;
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
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    void listenToGameEvents() {
            listenToGameEvents(this.gameId).thenAccept(inputStream -> {
               try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                   String line;
                   while ((line = reader.readLine()) != null) {
                       if(line.isEmpty()) continue;

                       System.out.println(line);
                       JSONObject obj = new JSONObject(line);
                       String state = obj.get("type").toString();
                       Board board = new Board();

                       switch (state) {
                           case "gameFull":
                               try{
                                   JSONObject gameState = new JSONObject(obj.get("state").toString());
                                   board = updateBoardWithMoves(board, gameState.get("moves").toString());

                                   if (this.side.equalsIgnoreCase(board.getSideToMove().toString())) {
                                       long startTime = System.currentTimeMillis();
                                       Move move = ChessArticle.findBestMoveRoot(board);
                                       makeMove(gameId, move.toString());
                                       System.out.println("Time taken to find the move: " + move + " is: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds");
                                   }
                               } catch (Exception e){
                                   System.out.println("Exception: " + e);
                               }
                               break;
                           case "gameState":
                               try {
                                   board = updateBoardWithMoves(board, obj.get("moves").toString());
                                   if (this.side.equalsIgnoreCase(board.getSideToMove().toString())) {
                                       long startTime = System.currentTimeMillis();
                                       Move move = ChessArticle.findBestMoveRoot(board);
                                       makeMove(gameId, move.toString());
                                       System.out.println("Time taken to find the move: " + move + " is: " + (System.currentTimeMillis() - startTime)/1000 + " seconds");
                                   }
                               } catch (Exception e){
                                   System.out.println("Exception: " + e);
                               }
                               break;
                       }

                   }
               }
               catch (Exception e) {
                   System.out.println("Random Exception: " + e.getMessage());
               }
            });
    }



    void closeGame() {
        System.out.println("Game is over now");
    }



}
