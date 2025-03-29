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

//        score = score + (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.WHITE_KNIGHT)) * 100)
//                    - (Long.bitCount(CENTER_MASK & board.getBitboard(Piece.BLACK_KNIGHT)) * 100);

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

            countList.add(count);

            if (Side.WHITE.equals(board.getSideToMove()) && moveMap.get(bestMovesList.get(0)) == 10000) {
                return bestMove;
            } else if (Side.BLACK.equals(board.getSideToMove()) && moveMap.get(bestMovesList.get(0)) == -10000) {
                return bestMove;
            }
            System.out.print(" Node count : " + count);
            System.out.print(" Score: " + moveMap.get(bestMovesList.get(0)));
            System.out.printf(" Hashing Hit ratio: %,.2f ", hashing.getHitsRatio() * 100);
            System.out.print(" Positions added: " + hashing.getTotalAdds());
            System.out.println(" NPS: " + (1_000_000_000L * count / (System.nanoTime() - start)));
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
