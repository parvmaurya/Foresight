package org.foresight;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ChessBitboard {

    Board board;
    long bitboard;
    long whitePawns;
    long whiteRooks;
    long whiteBishops;
    long whiteKnights;
    long whiteQueen;
    long whiteKing;

    long blackPawns;
    long blackRooks;
    long blackBishops;
    long blackKnights;
    long blackQueen;
    long blackKing;
    long[] FILE_MAP = {72340172838076673L, 144680345676153346L, 289360691352306692L, 578721382704613384L,
            1157442765409226768L, 2314885530818453536L, 4629771061636907072L, -9187201950435737472L};
    static int[] PIECE_VALUE = {10, 30, 32, 50, 90, 0, -10, -30, -32, -50, -90, 0, 0};
    static long CENTER_MASK = (1L<<18) | (1L<<19) | (1L<<20) | (1L<<21) | (1L<<26) | (1L<<27) | (1L<<28) | (1L<<29) | (1L<<34) | (1L<<35) | (1L<<36) | (1L<<37) | (1L<<42) | (1L<<43) | (1L<<44) | (1L<<45);
    static long CENTER_CENTER_MASK = (1L<<27) | (1L<<28) | (1L<<35) | (1L<<36);

//    Rank 7:  56 57 58 59 60 61 62 63
//    Rank 6:  48 49 50 51 52 53 54 55
//    Rank 5:  40 41 42 43 44 45 46 47
//    Rank 4:  32 33 34 35 36 37 38 39
//    Rank 3:  24 25 26 27 28 29 30 31
//    Rank 2:  16 17 18 19 20 21 22 23
//    Rank 1:   8  9 10 11 12 13 14 15
//    Rank 0:   0  1  2  3  4  5  6  7

    ChessBitboard(Board board) {
        this.board = board;
        this.bitboard = board.getBitboard();

        this.whiteKing = board.getBitboard(Piece.WHITE_KING);
        this.whiteBishops = board.getBitboard(Piece.WHITE_BISHOP);
        this.whiteKnights = board.getBitboard(Piece.WHITE_KNIGHT);
        this.whiteQueen = board.getBitboard(Piece.WHITE_QUEEN);
        this.whiteRooks = board.getBitboard(Piece.WHITE_ROOK);
        this.whitePawns = board.getBitboard(Piece.WHITE_PAWN);

        this.blackKing = board.getBitboard(Piece.BLACK_KING);
        this.blackBishops = board.getBitboard(Piece.BLACK_BISHOP);
        this.blackKnights = board.getBitboard(Piece.BLACK_KNIGHT);
        this.blackQueen = board.getBitboard(Piece.BLACK_QUEEN);
        this.blackRooks = board.getBitboard(Piece.BLACK_ROOK);
        this.blackPawns = board.getBitboard(Piece.BLACK_PAWN);

    }

    public long getWhitePieces() {
        return this.whiteBishops | this.whiteKing | this.whiteKnights | this.whiteQueen | this.whitePawns | this.whiteRooks;
    }

    public long getBlackPieces() {
        return this.blackBishops | this.blackKing | this.blackKnights | this.blackQueen | this.blackPawns | this.blackRooks;
    }

    public long getOccupiedPieces() {
        return this.getWhitePieces() | this.getBlackPieces();
    }


    void displayBoard() {
        for (int rank = 7; rank >=0; rank--) {
            for (int file = 7; file >=0; file--) {
                long currentPosition = (rank * 8L) + file;
                long currentSquareMask = 1L << currentPosition;
                if((currentSquareMask & this.whitePawns) != 0) System.out.print("P ");
                else if((currentSquareMask & this.whiteRooks) != 0) System.out.print("R ");
                else if((currentSquareMask & this.whiteQueen) != 0) System.out.print("Q ");
                else if((currentSquareMask & this.whiteKnights) != 0) System.out.print("N ");
                else if((currentSquareMask & this.whiteKing) != 0) System.out.print("K ");
                else if((currentSquareMask & this.whiteBishops) != 0) System.out.print("B ");
                else if((currentSquareMask & this.blackPawns) != 0) System.out.print("p ");
                else if((currentSquareMask & this.blackRooks) != 0) System.out.print("r ");
                else if((currentSquareMask & this.blackQueen) != 0) System.out.print("q ");
                else if((currentSquareMask & this.blackKnights) != 0) System.out.print("n ");
                else if((currentSquareMask & this.blackKing) != 0) System.out.print("k ");
                else if((currentSquareMask & this.blackBishops) != 0) System.out.print("b ");
                else System.out.print(". ");
            }
            System.out.println();
        }
    }

    int generateNetBishopMoves() {
        long whiteBishops = this.whiteBishops;
        long blackBishops = this.blackBishops;

        ArrayList<Integer> whiteBishopsPosition = new ArrayList<>();
        ArrayList<Integer> blackBishopsPosition = new ArrayList<>();
        for(int bishopIndex = 0; bishopIndex<64; bishopIndex = bishopIndex + 1){
            if ((whiteBishops & (1L << bishopIndex)) !=0) whiteBishopsPosition.add(bishopIndex);
            if ((blackBishops & (1L << bishopIndex)) !=0) blackBishopsPosition.add(bishopIndex);
        }

        long occupiedPieces = this.bitboard;
        int[] bishopPositionOffset = {9, 7, -9, -7};
        int count_moves = 0;
        int pos = 0;
        for (Integer position: whiteBishopsPosition) {
            for (int offset : bishopPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>=0; multiplier++) {
                    pos = position + (offset * multiplier);
                    if (Math.abs(pos%8 - (pos-offset)%8) != 1) break;
                    if ((occupiedPieces & (1L << pos)) == 0) {
                        count_moves++;
                    } else {
                        break;
                    }
                }
            }
        }
        for (Integer position: blackBishopsPosition) {
            for (int offset : bishopPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>=0; multiplier++) {
                    pos = position + (offset * multiplier);
                    if (Math.abs(pos%8 - (pos-offset)%8) != 1) break;
                    if ((occupiedPieces & (1L << pos)) == 0) {
                        count_moves--;
                    } else {
                        break;
                    }
                }
            }
        }
        return count_moves;
    }

    int generateNetRookMoves() {
        long whiteRooks = this.whiteRooks;
        long blackRooks = this.blackRooks;

        ArrayList<Integer> whiteRooksPosition = new ArrayList<>();
        ArrayList<Integer> blackRooksPosition = new ArrayList<>();
        for(int bishopIndex = 0; bishopIndex<64; bishopIndex = bishopIndex + 1){
            if ((whiteRooks & (1L << bishopIndex)) !=0) whiteRooksPosition.add(bishopIndex);
            if ((blackRooks & (1L << bishopIndex)) !=0) blackRooksPosition.add(bishopIndex);
        }

        long occupiedPieces = this.bitboard;
        int[] rookPositionOffset = {1, -1, 8, -8};
        int count_moves = 0;

        for (Integer position: whiteRooksPosition) {
            for (int offset : rookPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>=0; multiplier++) {
                    int pos = position + (offset * multiplier);
                    if (Math.abs(pos%8 - (pos-offset)%8) != 1) break;

                    if ((occupiedPieces & (1L << pos)) != 0) {
                        count_moves++;
                    }
                }
            }
        }

        for (Integer position: blackRooksPosition) {
            for (int offset : rookPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>=0; multiplier++) {
                    int pos = position + (offset * multiplier);
                    if (Math.abs(pos%8 - (pos-offset)%8) != 1) break;

                    if ((occupiedPieces & (1L << pos)) != 0) {
                        count_moves--;
                    }
                }
            }
        }

        return count_moves;
    }


    int getDoubledPawnsPenalty() {
        long whitePawns = this.whitePawns;
        long blackPawns = this.blackPawns;

        int penalty = 0;
        for (long file: this.FILE_MAP) {
            penalty = penalty - (Long.bitCount(whitePawns & file) -1);
            penalty = penalty + (Long.bitCount(blackPawns & file) -1);
        }
        return penalty;
    }

    int getIsolatedPawnsPenalty() {
        int score = 0;
        for (long file: this.FILE_MAP) {
            boolean isWhitePawnsOnLeftFile = file - 1 < 0 || Long.bitCount(this.whitePawns & file) > 0;
            boolean isWhitePawnsOnRightFile = file + 1 > 7 || Long.bitCount(this.whitePawns & file) > 0;

            if (!isWhitePawnsOnLeftFile & !isWhitePawnsOnRightFile) score = score - 1;

            boolean isBlackPawnsOnLeftFile = file - 1 < 0 || Long.bitCount(this.whitePawns & file) > 0;
            boolean isBlackPawnsOnRightFile = file + 1 > 7 || Long.bitCount(this.whitePawns & file) > 0;

            if (!isBlackPawnsOnLeftFile & !isBlackPawnsOnRightFile) score = score + 1;
        }
        return score;
    }

    int getSpaceScore() {
        AtomicInteger sc = new AtomicInteger();
        board.pseudoLegalMoves().forEach(move -> {
            if (board.getPiece(move.getFrom()).getPieceSide().equals(Side.WHITE)) sc.getAndIncrement();
            if (board.getPiece(move.getFrom()).getPieceSide().equals(Side.BLACK)) sc.getAndDecrement();
        });
        return sc.get();
    }


    int getStaticScore() {
        int score = 0;
        score = score + Long.bitCount(this.whiteBishops) * 320;
        score = score + Long.bitCount(this.whiteKnights) * 300;
        score = score + Long.bitCount(this.whiteQueen) * 900;
        score = score + Long.bitCount(this.whiteRooks) * 500;
        score = score + Long.bitCount(this.whitePawns) * 100;
        score = score - Long.bitCount(this.blackBishops) * 320;
        score = score - Long.bitCount(this.blackKnights) * 300;
        score = score - Long.bitCount(this.blackQueen) * 900;
        score = score - Long.bitCount(this.blackRooks) * 500;
        score = score - Long.bitCount(this.blackPawns) * 100;
        return score;
    }

    int getCenterScore() {
        int score = 0;
        score = score + (Long.bitCount(CENTER_MASK & this.whiteKnights) * 20)
                - (Long.bitCount(CENTER_MASK & this.blackKnights) * 20);

        score = score + (Long.bitCount(CENTER_CENTER_MASK & this.whitePawns) * 10)
                - (Long.bitCount(CENTER_CENTER_MASK & this.blackPawns) * 10);

        score = score + (Long.bitCount(CENTER_CENTER_MASK & this.whiteKnights) * 10)
                - (Long.bitCount(CENTER_CENTER_MASK & this.blackKnights) * 10);
        return score;
    }

}
