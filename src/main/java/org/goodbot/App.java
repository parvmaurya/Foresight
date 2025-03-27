package org.goodbot;

import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println("Application Started");
        new LichessUtil().listenToEvents();

//        ChessBitboard chessBitboard = new ChessBitboard(new Board());
//        chessBitboard.displayBoard();

    }
}
