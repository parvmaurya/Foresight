package org.foresight;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;

import java.util.ArrayList;

public class ChessBitboard {

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

//    Rank 7:  56 57 58 59 60 61 62 63
//    Rank 6:  48 49 50 51 52 53 54 55
//    Rank 5:  40 41 42 43 44 45 46 47
//    Rank 4:  32 33 34 35 36 37 38 39
//    Rank 3:  24 25 26 27 28 29 30 31
//    Rank 2:  16 17 18 19 20 21 22 23
//    Rank 1:   8  9 10 11 12 13 14 15
//    Rank 0:   0  1  2  3  4  5  6  7

    ChessBitboard(Board board) {
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

    public void generateWhiteBishopMoves() {
        ArrayList<Long> legalPositions = new ArrayList<>();

        ArrayList<Integer> bishopsPosition = new ArrayList<>();
        for(int bishopIndex = 0; bishopIndex<64; bishopIndex = bishopIndex + 1){
            if ((this.whiteBishops & (1L << bishopIndex)) !=0) bishopsPosition.add(bishopIndex);
        }

        long occupiedPieces = this.getOccupiedPieces();
        long whiteSquares = this.getWhitePieces();
        long blackSquares = this.getBlackPieces();

        int[] bishopPositionOffset = {9, 7, -9, -7};

        for (Integer position: bishopsPosition) {
            for (int offset : bishopPositionOffset) {
                for (int multiplier = 1; (position + offset * multiplier)<64 & (position + offset * multiplier)>0; multiplier++) {
                    int pos = position + (offset * multiplier);

                    if ((occupiedPieces & (1L << pos)) != 0) {
                        legalPositions.add((1L<<position));
                    } else {
                        if ((blackSquares & (1L << pos)) !=0){
                            legalPositions.add((1L<<position));
                        }
                        else {
                            break;
                        }
                    }
                }
            }
        }

    }

}
