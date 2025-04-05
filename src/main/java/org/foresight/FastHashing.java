package org.foresight;

import com.github.bhlangonijr.chesslib.move.Move;

class FastHashing {
    int[] ttDepth;
    int[] ttScore;
    long[] ttKeys;
    Move[] ttBestMove;
    int mask;
    long totalHits = 0;
    long successHits = 0;
    long totalAdds = 0;

    FastHashing() {
        this.ttDepth = new int[1 << 24];
        this.ttScore = new int[1 << 24];
        this.ttKeys = new long[1 << 24];
        this.ttBestMove = new Move[1 << 24];
        this.mask = (1 << 24) -1;
    }

    void addScore(long zorbist, int score, int depth, Move bestMove) {
        int hashIndex = (int)(zorbist & mask);
        if (ttKeys[hashIndex] != zorbist || ttDepth[hashIndex] < depth){
            ttKeys[hashIndex] = zorbist;
            ttDepth[hashIndex] = depth;
            ttScore[hashIndex] = score;
            if (bestMove != null) {
                ttBestMove[hashIndex] = bestMove;
            }
            totalAdds++;
        }
    }

    long getTotalAdds() {
        return this.totalAdds;
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
        if (ttKeys[hashIndex] == zorbist & ttBestMove[hashIndex] != null) {
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
        this.totalAdds = 0;
    }

}
