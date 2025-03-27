package org.foresight;

public class App 
{
    public static void main( String[] args )
    {
        System.out.println("Application Started");
        new LichessUtil().listenToEvents();
    }
}
