package org.goodbot;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ChessArticle {

    static class FastHashing {
        int[] ttDepth;
        int[] ttScore;
        long[] ttKeys;
        Move[] ttBestMove;
        int mask;
        long totalHits = 0;
        long successHits = 0;

        FastHashing() {
            this.ttDepth = new int[1 << 23];
            this.ttScore = new int[1 << 23];
            this.ttKeys = new long[1 << 23];
            this.ttBestMove = new Move[1 << 23];
            this.mask = (1 << 23) -1;
        }

        void addScore(long zorbist, int score, int depth, Move bestMove) {
            int hashIndex = (int)(zorbist & mask);
            if (ttKeys[hashIndex] != zorbist || ttDepth[hashIndex] < depth){
                ttKeys[hashIndex] = zorbist;
                ttDepth[hashIndex] = depth;
                ttScore[hashIndex] = score;
                ttBestMove[hashIndex] = bestMove;
            }
        }

        Integer getScore(long zorbist, int depth) {
            int hashIndex = (int)(zorbist & mask);
            this.totalHits = this.totalHits + 1;
            if (ttKeys[hashIndex] == zorbist && ttDepth[hashIndex] >= depth) {
                this.successHits = this.successHits + 1;
                return ttScore[hashIndex];
            }
            return null;
        }

        Move getBestMove(long zorbist) {
            int hashIndex = (int)(zorbist & mask);
            if (ttKeys[hashIndex] == zorbist) {
                return ttBestMove[hashIndex];
            }
            return null;
        }

        Double getHitsRatio() {
            return 1.0 * this.successHits/this.totalHits;
        }

        void clearHits() {
            this.successHits = 0;
            this.totalHits = 0;
        }

    }

    static int count = 0;

    static int[] PIECE_VALUE = {10, 30, 32, 50, 90, 0, -10, -30, -32, -50, -90, 0, 0};

    static long evalTime = 0;

    static int getBoardValue(Board board) {
        long tm = System.nanoTime();
        int score = 0;
        Piece[] pieceList = board.boardToArray();
        for(Piece p: pieceList) {
            score = score + PIECE_VALUE[p.ordinal()];
        }
//        int whiteMobility = (int) board.legalMoves().stream()
//                .filter(move -> board.getPiece(move.getFrom()).getPieceSide() == Side.WHITE).count();
//
//        int blackMobility = (int) board.legalMoves().stream()
//                .filter(move -> board.getPiece(move.getFrom()).getPieceSide() == Side.BLACK).count();
//
//        int mobilityScore =  board.getSideToMove().equals(Side.WHITE) ? 4 * (whiteMobility - blackMobility) : 4 * (blackMobility - whiteMobility);
        evalTime = evalTime + (System.nanoTime() - tm);
        return score;
    }

//    public static class CalculateMove implements Callable<Integer> {
//
//        Board board;
//        Move move;
//        int depth;
//        boolean isMaximiser;
//
//        public CalculateMove(Board board, Move move, int depth, boolean isMaximiser) {
//            this.board = board.clone();
//            this.move = move;
//            this.depth = depth;
//            this.isMaximiser = isMaximiser;
//        }
//
//        @Override
//        public Integer call() {
//            board.doMove(move);
//            count = count + 1;
//            int currentScore = findMoveAndScore(board, depth, alpha.get(), beta.get(), !isMaximiser);
//            hashing.addScore(board.getZobristKey(), currentScore, depth-1);
//            board.undoMove();
//
//            synchronized (alpha) {
//                synchronized (beta) {
////                    System.out.println(beta.get() > currentScore);
//                    if (isMaximiser && alpha.get() < currentScore) {
//                        bestMove.set(move);
//                        alpha.set(currentScore);
//                    } else if (!isMaximiser && beta.get() > currentScore){
//                        bestMove.set(move);
//                        beta.set(currentScore);
//                    }
//                }
//            }
//            return currentScore;
//        }
//
//    }

    static FastHashing hashing = new FastHashing();

//    static final AtomicInteger alpha = new AtomicInteger(-100000);
//    static final AtomicInteger beta = new AtomicInteger(100000);
//    static final AtomicReference<Move> bestMove = new AtomicReference<>();

    static int intermediateCount = 0;

    public static Move findBestMoveRoot(Board board) {
        List<Move> bestMovesList = board.legalMoves();
        List<Integer> countList = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        boolean isMaximiser = Side.WHITE.equals(board.getSideToMove());
        Move bestMove = null;

        for (int depth = 1; depth <= 6; depth = depth + 1) {
            hashing.clearHits();
            long start = System.nanoTime();
            int alpha = -10000;
            int beta = 10000;
            int bestScore = 0;

            count = 0;
            Map<Move, Integer> moveMap = new HashMap<>();
            long startTime = System.currentTimeMillis();

            for (Move move: board.legalMoves()) {
                intermediateCount = 0;
                board.doMove(move);
                count = count + 1;

                int currentScore = findMoveAndScore(board, depth, alpha, beta, !isMaximiser);

                board.undoMove();

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


            System.out.print("Depth: " + depth);
            System.out.print(" "+bestMove);
            System.out.print(" " + (System.currentTimeMillis() - startTime)/1000.0);

            bestMovesList =
            moveMap
                    .entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (!isMaximiser) {
                Collections.reverse(bestMovesList);
            }

//            System.out.println(bestMovesList);
            countList.add(count);

            if (Side.WHITE.equals(board.getSideToMove()) && moveMap.get(bestMovesList.get(0)) == 10000) {
                return bestMove;
            } else if (Side.BLACK.equals(board.getSideToMove()) && moveMap.get(bestMovesList.get(0)) == -10000) {
                return bestMove;
            }
            System.out.print(" Node count : " + count);
            System.out.print(" Score: " + moveMap.get(bestMovesList.get(0)));
            System.out.printf(" Hashing Hit ratio: %,.2f ", hashing.getHitsRatio() * 100);
            System.out.println(" NPS: " + (1_000_000_000L * count / (System.nanoTime() - start)));
//            System.out.println(" Time spend at leaf node evaluation: " + (evalTime*1.0/1e9));
            evalTime = 0;
        }

        System.out.println("Branching factor: " + (1.0*countList.stream().mapToInt(i -> i).sum()/(countList.stream().mapToInt(i -> i).sum()-countList.get(countList.size()-1))));
        return bestMove;
    }

    public static int quiescence(Board board, int alpha, int beta, int depth, boolean isMaximizer, int quiescenceDepth) {
        int evaluationScore = getBoardValue(board);

        if (quiescenceDepth == 0) {
            return isMaximizer ? alpha : beta;
        }

        if (isMaximizer) {
            if (evaluationScore > alpha) {
                alpha = evaluationScore;
            }
            if (evaluationScore >= beta) {
                return beta;
            }
        } else {
            if (evaluationScore < beta) {
                beta = evaluationScore;
            }
            if (evaluationScore <= alpha) {
                return alpha;
            }
        }

        for (Move move: board.legalMoves()) {
            if (!board.getPiece(move.getTo()).equals(Piece.NONE)) {
                count = count + 1;

                board.doMove(move);
                evaluationScore = quiescence(board, alpha, beta, depth, !isMaximizer, quiescenceDepth-1);
                board.undoMove();

                if (isMaximizer) {
                    if (evaluationScore > alpha) {
                        alpha = evaluationScore;
                    }
                    if (evaluationScore >= beta) {
                        return beta;
                    }
                } else {
                    if (evaluationScore < beta) {
                        beta = evaluationScore;
                    }
                    if (evaluationScore <= alpha) {
                        return alpha;
                    }
                }

            }
        }
//        return evaluationScore;
        return isMaximizer ? alpha : beta;
    }

    public static int findMoveAndScore(Board board, int depth, int alpha, int beta, boolean isMaximizer) {
        count = count + 1;
        intermediateCount = intermediateCount + 1;
        if(board.isMated()) {
            if (Side.WHITE.equals(board.getSideToMove())) {
                return -10000;
            } else {
                return 10000;
            }

        }
        if(board.isDraw()) {
            return 0;
        }

        long zkey = board.getZobristKey();
        Integer cachedScore = hashing.getScore(zkey, depth);
        if (cachedScore != null) {
            return cachedScore;
        }

        if(depth == 0) {
            return getBoardValue(board);
//            return quiescence(board, alpha, beta, depth, isMaximizer, 2);
        }

        List<Move> legalMovesList = board.legalMoves();

        if (isMaximizer) {
            int bestScore = Integer.MIN_VALUE;
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

                if(score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                    alpha = Math.max(score, alpha);
                }

                if (alpha >= beta) {
                    break;
                }

            }
            hashing.addScore(board.getZobristKey(), bestScore, depth-1, bestMove);
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
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

                if(score < bestScore) {
                    bestScore = score;
                    bestMove = move;
                    beta = Math.min(score, beta);
                }

                if (alpha >= beta) {
                    break;
                }

            }
            hashing.addScore(board.getZobristKey(), bestScore, depth-1, bestMove);
            return bestScore;
        }


    }

}
