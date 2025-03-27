package org.foresight;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class GameEvent {

    private LichessUtil lichessUtil = new LichessUtil();

    String gameId;
    String side;

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

    void listenToGameEvents() {

            lichessUtil.listenToGameEvents(this.gameId).thenAccept(inputStream -> {
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
                                       lichessUtil.makeMove(gameId, move.toString());
                                       System.out.println("Time taken to find the move: " + move + " is: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds");


//                                       ChessArticle.MovePair mP = ChessArticle.findMoveAndScore(board, 6, -100000, 100000);
//                                       System.out.println("Time taken to find the move: " + mP.getMove() + " is: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds");
//                                       lichessUtil.makeMove(gameId, mP.getMove().toString());
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
                                       lichessUtil.makeMove(gameId, move.toString());
                                       System.out.println("Time taken to find the move: " + move + " is: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds");
//
//                                       ChessArticle.MovePair mP = ChessArticle.findMoveAndScore(board, 6, -100000, 100000);
//                                       System.out.println("Time taken to find the move: " + mP.getMove() + " is: " + (System.currentTimeMillis() - startTime)/1000.0 + " seconds");
//                                       lichessUtil.makeMove(gameId, mP.getMove().toString());
                                   }
                               } catch (Exception e){
                                   System.out.println("Exception: " + e);
                               }
                               break;
                       }

                   }
               }
               catch (Exception e) {
                   e.printStackTrace();
                   System.out.println("Random Exception: " + e.toString());
               }
            });
    }

    void closeGame() {
        System.out.println("Game is over now");
    }



}
