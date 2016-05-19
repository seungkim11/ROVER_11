package testUtillities;

import common.Communication;
import common.Coord;
import common.MapTile;

/**
 * Created by samskim on 5/15/16.
 */
public class CommunicationDebugger {

    public static void main(String[] args){
        String url = "http://localhost:3000/api/global";
        String corp_secret = "";
        Communication com = new Communication(url, "ROVER_11", corp_secret);



    }
}
