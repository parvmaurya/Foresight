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
