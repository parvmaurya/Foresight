package org.foresight;

import com.github.bhlangonijr.chesslib.move.Move;

import java.util.List;

public class MoveVariation {

    int depth;
    List<Move> moveList;

    MoveVariation(List<Move> moveList, int depth) {
        this.depth = depth;
        this.moveList = moveList;
    }

}
