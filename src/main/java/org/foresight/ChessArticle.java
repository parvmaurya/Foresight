package org.foresight;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ChessArticle {

    private static final Logger logger = LoggerFactory.getLogger(ChessArticle.class);

    static int count = 0;


    static int getBoardValue(Board board) {
        ChessBitboard chessBitboard = new ChessBitboard(board);

        int score = 0;
        score = score + chessBitboard.getStaticScore();
        score = score + chessBitboard.getCenterScore();
        score = score + chessBitboard.generateNetBishopMoves() * 10;
        score = score + chessBitboard.generateNetRookMoves() * 20;
        score = score + chessBitboard.getDoubledPawnsPenalty() * 20;
        score = score + chessBitboard.getIsolatedPawnsPenalty() * 20;
        score = score + chessBitboard.getSpaceScore() * 2;
        return score;
    }

    static FastHashing hashing = new FastHashing();
    static int intermediateNodes = 0;
    static int MAX_TIME = 20000;

    public static Move findBestMoveRoot(Board board) {
        logger.info(" ");
        logger.info(" New Move ");
        long startTime = System.currentTimeMillis();

        List<Move> bestMovesList = board.legalMoves();
        List<Integer> countList = new ArrayList<>();
        boolean isTimeout = false;

        boolean isMaximiser = Side.WHITE.equals(board.getSideToMove());
        Move bestMove = bestMovesList.get(0);
        for (int depth = 1; depth <= 20 && System.currentTimeMillis() - startTime < MAX_TIME; depth = depth + 1) {
            Move tmpBestMove = bestMove;
            hashing.clearHits();
            long start = System.nanoTime();
            int alpha = -100000;
            int beta = 100000;

            count = 0;
            qnodes = 0;
            Map<Move, Integer> moveMap = new HashMap<>();
            long depthStartTime = System.currentTimeMillis();
            for (Move move: bestMovesList) {
                intermediateNodes = 0;
                count = count + 1;
                board.doMove(move);
                int currentScore = 0;
                if (hashing.getScore(board.getZobristKey(), depth) == null) {
                    currentScore = findMoveAndScore(board, depth, alpha, beta, !isMaximiser, startTime);
                } else {
                    currentScore = hashing.getScore(board.getZobristKey(), depth);
                }
                board.undoMove();
                if (System.currentTimeMillis() - startTime > MAX_TIME) { System.out.println("Hogaya Timeout"); isTimeout = true; break;}

                if (isMaximiser && alpha < currentScore) {
                    tmpBestMove = move;
                    alpha = currentScore;
                } else if (!isMaximiser && beta > currentScore){
                    tmpBestMove = move;
                    beta = currentScore;
                }
                moveMap.put(move, currentScore);
            }
            if (isTimeout) break;
            bestMovesList = moveMap
                            .entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList());
            if (isMaximiser) {
                Collections.reverse(bestMovesList);
            }
            hashing.addScore(board.getZobristKey(), (isMaximiser ? alpha : beta), depth-1, tmpBestMove);

            // Set TT
            long zkey = board.getZobristKey();
            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && bestMovesList.contains(ttMove)) {
                bestMovesList.remove(ttMove);
                bestMovesList.add(0, ttMove);
            }

            // Set PV move, i.e best move
            bestMovesList.remove(tmpBestMove);
            bestMovesList.add(0, tmpBestMove);

            countList.add(count);

            // Return if checkmate found
            if (Side.WHITE.equals(board.getSideToMove()) && alpha >= 10000) {
                System.out.println("White checkmate found"); break;}
            else if (Side.BLACK.equals(board.getSideToMove()) && beta <= -10000) {
                System.out.println("Black checkmate found"); break; }

            if (System.currentTimeMillis() - startTime < MAX_TIME) {
                bestMove = tmpBestMove;
            }

            // log relevant metrics
            logger.info("Depth: {} | Node count: {} | Score: {} | Time: {}s | BestMove: {} | Hashing Hit ratio: {}% | Positions added: {} | NPS: {}k | Qnodes%: {}%",
                    depth+1,
                    count,
                    moveMap.get(bestMovesList.get(0)),
                    (System.currentTimeMillis() - depthStartTime) / 1000.0,
                    bestMove,
                    hashing.getHitsRatio() * 100,
                    hashing.getTotalAdds(),
                    1_000_000_000L * count / ((System.nanoTime() - start) * 1000),
                    (count + qnodes) == 0 ? 0 : qnodes * 100 / (count + qnodes)
            );
        }

        logger.info("Branching factor: {}", 1.0 * countList.stream().mapToInt(i -> i).sum() / (countList.stream().mapToInt(i -> i).sum() - countList.get(countList.size() - 1)));
        logger.info("Best move returned: {}", bestMove);
        return bestMove;
    }

    public static int findMoveAndScore(Board board, int depth, int alpha, int beta, boolean isMaximizer, long startTime) {
        count = count + 1;
        intermediateNodes++;
        if (System.currentTimeMillis() - startTime > MAX_TIME) {
            return quiescence(board, alpha, beta, depth, isMaximizer, 3);
        }

        if(board.isMated()) { return Side.WHITE.equals(board.getSideToMove()) ? -10000+depth : 10000-depth;}
        if(board.isDraw()) { return 0; }
        if (board.isRepetition(3)) {return 0;}

        int bestScore = !isMaximizer ? Integer.MAX_VALUE : Integer.MIN_VALUE;

        long zkey = board.getZobristKey();
        Integer cachedScore = hashing.getScore(zkey, depth);
        if (cachedScore != null) {
            return cachedScore;
        }

        if(depth == 0) {
//            return getBoardValue(board);
            int scr = quiescence(board, alpha, beta, depth, isMaximizer, 3);
            hashing.addScore(board.getZobristKey(), scr, 0, null);
            return scr;
        }

        int reductionFactor = 2;
        if (!isMaximizer) {
            if (depth > 3 && !board.isKingAttacked()) {
                board.doNullMove();
                int nullmovescore = findMoveAndScore(board, depth - 2, beta-1, beta, true, startTime);
                // alpha is the best move of maximiser yet, beta is best move for minimiser
                // black skips a move, in that case, white made 2 moves in a row
                board.undoMove();
                if (nullmovescore >= beta) {
                    return nullmovescore;
                }
            }
        }
        else {
            if (depth >= 3 && !board.isKingAttacked()) {
                // minimiser skips a move, means black skips his move, alpha is the best move of white till now, beta is best black move
                // black skips a move, white makes a move, white ka score should be really well
                board.doNullMove();
                int nullmovescore = findMoveAndScore(board, depth - 2, alpha, alpha+1, false, startTime);
                board.undoMove();
                if (alpha >= nullmovescore) {
                    return nullmovescore;
                }
            }
        }

        List<Move> legalMovesList = board.legalMoves();
        int alphaOriginal = alpha;
        int betaOriginal = beta;

        if (isMaximizer) {
            Move bestMove = null;
            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && legalMovesList.contains(ttMove)) {
                legalMovesList.remove(ttMove);
                legalMovesList.add(0, ttMove);
            }

            for(Move move : legalMovesList) {
                board.doMove(move);
                int score = findMoveAndScore(board, depth-1, alpha, beta, false, startTime);
                board.undoMove();
                if (score > bestScore) {
                    bestScore = score;
                }

                if(score > alpha) {
                    bestMove = move;
                    alpha = score;
                }

                if (alpha >= beta) { break;}
            }
            if (bestScore > alphaOriginal && bestScore < betaOriginal) hashing.addScore(board.getZobristKey(), bestScore, depth, bestMove);
            return bestScore;
        } else {
            Move bestMove = null;

            Move ttMove = hashing.getBestMove(zkey);
            if (ttMove != null && legalMovesList.contains(ttMove)) {
                legalMovesList.remove(ttMove);
                legalMovesList.add(0, ttMove);
            }

            for(Move move : legalMovesList) {
                board.doMove(move);
                int score = findMoveAndScore(board, depth-1, alpha, beta, true, startTime);
                board.undoMove();
                if (score < bestScore) {
                    bestScore = score;
                }

                if(score < beta) {
                    bestMove = move;
                    beta = score;
                }

                if (alpha >= beta) { break; }
            }

            if (bestScore > alphaOriginal && bestScore < betaOriginal) hashing.addScore(board.getZobristKey(), bestScore, depth, bestMove);
            return bestScore;
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
            if (!board.getPiece(move.getTo()).equals(Piece.NONE) | board.isKingAttacked() ) {

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
