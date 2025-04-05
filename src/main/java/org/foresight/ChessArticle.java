package org.foresight;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class ChessArticle {

    static int count = 0;

    static int[] PIECE_VALUE = {10, 30, 32, 50, 90, 0, -10, -30, -32, -50, -90, 0, 0};
    static long CENTER_MASK = (1<<18) | (1<<19) | (1<<20) | (1<<21) | (1<<26) | (1<<27) | (1<<28) | (1<<29) | (1<<34) | (1<<35) | (1<<36) | (1<<37) | (1<<42) | (1<<43) | (1<<44) | (1<<45);
    static long CENTER_CENTER_MASK = (1<<27) | (1<<28) | (1<<35) | (1<<36);

    static int generateWhiteBishopMoves(Board b) {
        long whiteBishops = b.getBitboard(Piece.WHITE_BISHOP);
        long blackBishops = b.getBitboard(Piece.BLACK_BISHOP);

        ArrayList<Integer> whiteBishopsPosition = new ArrayList<>();
        ArrayList<Integer> blackBishopsPosition = new ArrayList<>();
        for(int bishopIndex = 0; bishopIndex<64; bishopIndex = bishopIndex + 1){
            if ((whiteBishops & (1L << bishopIndex)) !=0) whiteBishopsPosition.add(bishopIndex);
            if ((blackBishops & (1L << bishopIndex)) !=0) blackBishopsPosition.add(bishopIndex);
        }

        long occupiedPieces = b.getBitboard();
        int[] bishopPositionOffset = {9, 7, -9, -7};
        int count_moves = 0;

        for (Integer position: whiteBishopsPosition) {
            for (int offset : bishopPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>0; multiplier++) {
                    int pos = position + (offset * multiplier);

                    if ((occupiedPieces & (1L << pos)) != 0) {
                        count_moves++;
                    }
                    }
                }
            }
        for (Integer position: blackBishopsPosition) {
            for (int offset : bishopPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>0; multiplier++) {
                    int pos = position + (offset * multiplier);

                    if ((occupiedPieces & (1L << pos)) != 0) {
                        count_moves--;
                    }
                }
            }
        }
        return count_moves;
        }

    static int getBoardValue(Board board) {
        long whiteBishops = board.getBitboard(Piece.WHITE_BISHOP);
        long whiteKnights = board.getBitboard(Piece.WHITE_KNIGHT);
        long whiteQueen = board.getBitboard(Piece.WHITE_QUEEN);
        long whiteRooks = board.getBitboard(Piece.WHITE_ROOK);
        long whitePawns = board.getBitboard(Piece.WHITE_PAWN);

        long blackBishops = board.getBitboard(Piece.BLACK_BISHOP);
        long blackKnights = board.getBitboard(Piece.BLACK_KNIGHT);
        long blackQueen = board.getBitboard(Piece.BLACK_QUEEN);
        long blackRooks = board.getBitboard(Piece.BLACK_ROOK);
        long blackPawns = board.getBitboard(Piece.BLACK_PAWN);

        int score = 0;
        score = score + Long.bitCount(whiteBishops) * 320;
        score = score + Long.bitCount(whiteKnights) * 300;
        score = score + Long.bitCount(whiteQueen) * 900;
        score = score + Long.bitCount(whiteRooks) * 500;
        score = score + Long.bitCount(whitePawns) * 100;
        score = score - Long.bitCount(blackBishops) * 320;
        score = score - Long.bitCount(blackKnights) * 300;
        score = score - Long.bitCount(blackQueen) * 900;
        score = score - Long.bitCount(blackRooks) * 500;
        score = score - Long.bitCount(blackPawns) * 100;

        score = score + (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.WHITE_KNIGHT)) * 50)
                    - (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.BLACK_KNIGHT)) * 50);

        score = score + (Long.bitCount(CENTER_CENTER_MASK & board.getBitboard(Piece.WHITE_PAWN)) * 10)
                - (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.BLACK_PAWN)) * 10);

        score = score + (Long.bitCount(CENTER_CENTER_MASK & board.getBitboard(Piece.WHITE_KNIGHT)) * 20)
                - (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.BLACK_KNIGHT)) * 20);

        score = score + generateWhiteBishopMoves(board) * 10;
        return score;
    }

    static FastHashing hashing = new FastHashing();
    static int intermediateNodes = 0;
    static int MAX_TIME = 20000;
    static long startTime = 0;

    public static Move findBestMoveRoot(Board board) {
        startTime = System.currentTimeMillis();

        List<Move> bestMovesList = board.legalMoves();
        List<Integer> countList = new ArrayList<>();

        boolean isMaximiser = Side.WHITE.equals(board.getSideToMove());
        Move bestMove = bestMovesList.get(0);
        for (int depth = 1; depth <= 20 && System.currentTimeMillis() - startTime < MAX_TIME; depth = depth + 1) {
            hashing.clearHits();
            long start = System.nanoTime();
            int alpha = -10000;
            int beta = 10000;

            count = 0;
            qnodes = 0;
            Map<Move, Integer> moveMap = new HashMap<>();
            long depthStartTime = System.currentTimeMillis();

            for (Move move: bestMovesList) {
                intermediateNodes = 0;
                count = count + 1;

                board.doMove(move);
                int currentScore = findMoveAndScore(board, depth, alpha, beta, !isMaximiser);
                board.undoMove();

                if (System.currentTimeMillis() - startTime > MAX_TIME) return bestMove;

                if (isMaximiser && alpha < currentScore) {
                    bestMove = move;
                    alpha = currentScore;
                } else if (!isMaximiser && beta > currentScore){
                    bestMove = move;
                    beta = currentScore;
                }
                moveMap.put(move, currentScore);
            }
            hashing.addScore(board.getZobristKey(), (isMaximiser ? alpha : beta), depth-1, bestMove);

            // Set TT
            long zkey = board.getZobristKey();
            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && bestMovesList.contains(ttMove)) {
                bestMovesList.remove(ttMove);
                bestMovesList.add(0, ttMove);
            }

            // Set PV move, i.e best move
            bestMovesList.remove(bestMove);
            bestMovesList.add(0, bestMove);

            countList.add(count);

            // Return if checkmate found
            if (Side.WHITE.equals(board.getSideToMove()) && alpha == 10000) { return bestMove;}
            else if (Side.BLACK.equals(board.getSideToMove()) && beta == -10000) { return bestMove; }

            // log relevant metrics
            System.out.print("Depth: " + depth);
            System.out.print(" " + bestMove);
            System.out.print(" " + (System.currentTimeMillis() - depthStartTime)/1000.0);
            System.out.print(" Node count : " + count);
            System.out.print(" Score: " + moveMap.get(bestMovesList.get(0)));
            System.out.printf(" Hashing Hit ratio: %,.2f ", hashing.getHitsRatio() * 100);
            System.out.print(" Positions added: " + hashing.getTotalAdds());
            System.out.print(" NPS: " + (1_000_000_000L * count / ((System.nanoTime() - start) * 1000)) + "k" );
            System.out.println(" Qnodes% :" + qnodes*100/(count+qnodes) + "%");
        }

        System.out.println("Branching factor: " + (1.0*countList.stream().mapToInt(i -> i).sum()/(countList.stream().mapToInt(i -> i).sum()-countList.get(countList.size()-1))));
        return bestMove;
    }

    public static int findMoveAndScore(Board board, int depth, int alpha, int beta, boolean isMaximizer) {
        count = count + 1;
        intermediateNodes++;
        if (System.currentTimeMillis() - startTime > MAX_TIME) {
            return isMaximizer ? alpha : beta;
        }

        if(board.isMated()) { return Side.WHITE.equals(board.getSideToMove()) ? -10000 : 10000;}
        if(board.isDraw()) { return 0; }

        long zkey = board.getZobristKey();
        Integer cachedScore = hashing.getScore(zkey, depth);
        if (cachedScore != null) {
            return cachedScore;
        }

        if(depth == 0) {
//            return getBoardValue(board);
            int scr = quiescence(board, alpha, beta, depth, isMaximizer, 3);
            return scr;
//            hashing.addScore(board.getZobristKey(), scr, 0, null);
        }

        int reductionFactor = 2;
        if (!isMaximizer) {
            if (depth - reductionFactor - 1 >= 0 & !board.isKingAttacked()) {
                board.doNullMove();
                int nullmovescore = findMoveAndScore(board, depth - reductionFactor - 1, beta-1, beta, true);
                board.undoMove();
                if (nullmovescore >= beta) {
                    return nullmovescore;
                }
            }
        }
        else {
            if (depth - reductionFactor - 1 >= 0 & !board.isKingAttacked()) {
                board.doNullMove();
                int nullmovescore = findMoveAndScore(board, depth - reductionFactor - 1, alpha, alpha+1, false);
                board.undoMove();
                if (alpha >= nullmovescore) {
                    return nullmovescore;
                }
            }
        }

        List<Move> legalMovesList = board.legalMoves();

        if (isMaximizer) {
            Move bestMove = null;
            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && legalMovesList.contains(ttMove)) {
                legalMovesList.remove(ttMove);
                legalMovesList.add(0, ttMove);
            }

            for(Move move : legalMovesList) {
                board.doMove(move);
                int score = findMoveAndScore(board, depth-1, alpha, beta, false);
                board.undoMove();

                if(score > alpha) {
                    bestMove = move;
                    alpha = score;
                }

                if (alpha >= beta) { break;}
            }
            hashing.addScore(board.getZobristKey(), alpha, depth-1, bestMove);
            return alpha;
        } else {
            Move bestMove = null;

            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && legalMovesList.contains(ttMove)) {
                legalMovesList.remove(ttMove);
                legalMovesList.add(0, ttMove);
            }

            for(Move move : legalMovesList) {
                board.doMove(move);
                int score = findMoveAndScore(board, depth-1, alpha, beta, true);
                board.undoMove();

                if(score < beta) {
                    bestMove = move;
                    beta = score;
                }

                if (alpha >= beta) { break; }
            }
            hashing.addScore(board.getZobristKey(), beta, depth-1, bestMove);
            return beta;
        }


    }

    static int qnodes = 0;
    public static int quiescence(Board board, int alpha, int beta, int depth, boolean isMaximizer, int quiescenceDepth) {
        qnodes++;
        int evaluationScore = getBoardValue(board);
        count = count + 1;

//        if (quiescenceDepth == 0) {
//            return isMaximizer ? alpha : beta;
//        }

        if (isMaximizer) {
            if (evaluationScore > alpha) { alpha = evaluationScore; }
            if (evaluationScore >= beta) { return beta; }
        } else {
            if (evaluationScore < beta) { beta = evaluationScore; }
            if (evaluationScore <= alpha) { return alpha; }
        }

        for (Move move: board.legalMoves()) {
            if (!board.getPiece(move.getTo()).equals(Piece.NONE)) {

                board.doMove(move);
                evaluationScore = quiescence(board, alpha, beta, depth, !isMaximizer, quiescenceDepth-1);
                board.undoMove();

                if (isMaximizer) {
                    if (evaluationScore > alpha) { alpha = evaluationScore; }
                } else {
                    if (evaluationScore < beta) { beta = evaluationScore; }
                }
                if (alpha >= beta) { break; }
            }
        }

        return isMaximizer ? alpha : beta;
    }


}
