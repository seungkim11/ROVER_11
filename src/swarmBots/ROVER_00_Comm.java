package swarmBots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import common.Communication;
import common.Coord;
import common.MapTile;
import common.ScanMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import enums.RoverDriveType;
import enums.Science;
import enums.Terrain;
import rover_logic.SearchLogic;
import supportTools.CommunicationHelper;

/**
 * The seed that this program is built on is a chat program example found here:
 * http://cs.lmu.edu/~ray/notes/javanetexamples/ Many thanks to the authors for publishing their
 * code examples
 */

public class ROVER_00_Comm {

    BufferedReader in;
    PrintWriter out;
    String rovername;
    ScanMap scanMap;
    int sleepTime;
    String SERVER_ADDRESS = "localhost";
    static final int PORT_ADDRESS = 9537;
    public static Map<Coord, MapTile> globalMap;
    List<Coord> destinations;

    public ROVER_00_Comm() {
        // constructor
        System.out.println("ROVER_00 rover object constructed");
        rovername = "ROVER_00";
        SERVER_ADDRESS = "localhost";
        // this should be a safe but slow timer value
        sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        destinations = new ArrayList<>();
    }

    public ROVER_00_Comm(String serverAddress) {
        // constructor
        System.out.println("ROVER_00 rover object constructed");
        rovername = "ROVER_00";
        SERVER_ADDRESS = serverAddress;
        sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        globalMap = new HashMap<>();
        destinations = new ArrayList<>();
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException, InterruptedException {

        // Make connection to SwarmServer and initialize streams
        Socket socket = null;
        try {
            socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);


            // Process all messages from server, wait until server requests Rover ID
            // name - Return Rover Name to complete connection
            while (true) {
                String line = in.readLine();
                if (line.startsWith("SUBMITNAME")) {
                    out.println(rovername); // This sets the name of this instance
                    // of a swarmBot for identifying the
                    // thread to the server
                    break;
                }
            }


            // ********* Rover logic setup *********

            String line = "";
            Coord rovergroupStartPosition = null;
            Coord targetLocation = null;

            /**
             *  Get initial values that won't change
             */
            // **** get equipment listing ****
            ArrayList<String> equipment = new ArrayList<String>();
            equipment = getEquipment();
            System.out.println(rovername + " equipment list results " + equipment + "\n");


            // **** Request START_LOC Location from SwarmServer ****
            out.println("START_LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("START_LOC")) {
                rovergroupStartPosition = extractLocationFromString(line);
            }
            System.out.println(rovername + " START_LOC " + rovergroupStartPosition);


            // **** Request TARGET_LOC Location from SwarmServer ****
            out.println("TARGET_LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("TARGET_LOC")) {
                targetLocation = extractLocationFromString(line);
            }
            System.out.println(rovername + " TARGET_LOC " + targetLocation);


            boolean goingSouth = false;
            boolean stuck = false; // just means it did not change locations between requests,
            // could be velocity limit or obstruction etc.
            boolean blocked = false;

            String[] cardinals = new String[4];
            cardinals[0] = "N";
            cardinals[1] = "E";
            cardinals[2] = "S";
            cardinals[3] = "W";

            String currentDir = cardinals[0];
            Coord currentLoc = null;
            Coord previousLoc = null;

            // ******** communication server
            String url = "http://23.251.155.186:3000/api";
//            String url = "http://localhost:3000/api";
            String corp_secret = "0FSj7Pn23t";
            Communication com = new Communication(url, rovername, corp_secret);
            int comm_count = 0;


            SearchLogic search = new SearchLogic();
            Coord destination = targetLocation;
//            Coord destination = new Coord(76, 12);


            /**
             *  ####  Rover controller process loop  ####
             */
            while (true) {


                // **** Request Rover Location from SwarmServer ****
                out.println("LOC");
                line = in.readLine();
                if (line == null) {
                    System.out.println(rovername + " check connection to server");
                    line = "";
                }
                if (line.startsWith("LOC")) {
                    // loc = line.substring(4);
                    currentLoc = extractLocationFromString(line);

                }
                System.out.println(rovername + " currentLoc at start: " + currentLoc);

                // after getting location set previous equal current to be able to check for stuckness and blocked later
                previousLoc = currentLoc;


                // ***** do a SCAN *****

                // gets the scanMap from the server based on the Rover current location
                doScan();
                // prints the scanMap to the Console output for debug purposes
                scanMap.debugPrintMap();


                // ***** get TIMER remaining *****
                out.println("TIMER");
                line = in.readLine();
                if (line == null) {
                    System.out.println(rovername + " check connection to server");
                    line = "";
                }
                if (line.startsWith("TIMER")) {
                    String timeRemaining = line.substring(6);
                    System.out.println(rovername + " timeRemaining: " + timeRemaining);
                }


                // pull the MapTile array out of the ScanMap object
                MapTile[][] scanMapTiles = scanMap.getScanMap();
                int centerIndex = (scanMap.getEdgeSize() - 1) / 2;
                // tile S = y + 1; N = y - 1; E = x + 1; W = x - 1

                updateglobalMap(currentLoc, scanMapTiles);


                // ********* post your scanMapTiles to communication server
                // must be called AFTER doScan() and currentLoc
                com.postScanMapTiles(currentLoc, scanMapTiles);

                // reduce the number of calling globalmap for optimal traffic
                if (comm_count % 20 == 0) {
                    JSONArray array = com.getGlobalMap();
                    updateglobalMap(array);
                    // do something with this global map data
                }
                comm_count++;


                List<String> moves = search.Astar(currentLoc, destination, scanMapTiles, RoverDriveType.WHEELS, globalMap);
                System.out.println(rovername + "currentLoc: " + currentLoc + ", destination: " + destination);
                System.out.println(rovername + " moves: " + moves.toString());
//

                // if STILL MOVING
                if (!moves.isEmpty()) {
                    out.println("MOVE " + moves.get(0));
                    // if rover is next to the target
                    // System.out.println("Rover near destiation. distance: " + getDistance(currentLoc, destination));
                }


                System.out.println("destinations: " + destinations);


                // another call for current location
                out.println("LOC");
                line = in.readLine();
                if (line == null) {
                    System.out.println("ROVER_00 check connection to server");
                    line = "";
                }
                if (line.startsWith("LOC")) {
                    currentLoc = extractLocationFromString(line);

                }


                // test for stuckness
                stuck = currentLoc.equals(previousLoc);

                //System.out.println("ROVER_00 stuck test " + stuck);
                System.out.println("ROVER_00 blocked test " + blocked);

                // TODO - logic to calculate where to move next


                // this is the Rovers HeartBeat, it regulates how fast the Rover cycles through the control loop
                Thread.sleep(sleepTime);

                System.out.println("ROVER_00 ------------ bottom process control --------------");
            }

            // This catch block closes the open socket connection to the server
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("ROVER_00 problem closing socket");
                }
            }
        }

    } // END of Rover main control loop


    private void updateglobalMap(Coord currentLoc, MapTile[][] scanMapTiles) {
        int centerIndex = (scanMap.getEdgeSize() - 1) / 2;

        for (int row = 0; row < scanMapTiles.length; row++) {
            for (int col = 0; col < scanMapTiles[row].length; col++) {

                MapTile mapTile = scanMapTiles[col][row];

                int xp = currentLoc.xpos - centerIndex + col;
                int yp = currentLoc.ypos - centerIndex + row;
                Coord coord = new Coord(xp, yp);
                globalMap.put(coord, mapTile);
            }
        }
        // put my current position so it is walkable
        MapTile currentMapTile = scanMapTiles[centerIndex][centerIndex].getCopyOfMapTile();
        currentMapTile.setHasRoverFalse();
        globalMap.put(currentLoc, currentMapTile);
    }

    // get data from server and update field map
    private void updateglobalMap(JSONArray data) {

        for (Object o : data) {

            JSONObject jsonObj = (JSONObject) o;
            int x = (int) (long) jsonObj.get("x");
            int y = (int) (long) jsonObj.get("y");
            Coord coord = new Coord(x, y);

            // only bother to save if our globalMap doesn't contain the coordinate
            if (!globalMap.containsKey(coord)) {
                MapTile tile = CommunicationHelper.convertToMapTile(jsonObj);

                // if tile has science AND is not in sand
                if (tile.getScience() != Science.NONE && tile.getTerrain() != Terrain.SAND) {

                    // then add to the destination
                    if (!destinations.contains(coord))
                        destinations.add(coord);
                }

                globalMap.put(coord, tile);
            }
        }
    }


    // ####################### Support Methods #############################

    private void clearReadLineBuffer() throws IOException {
        while (in.ready()) {
            //System.out.println("ROVER_00 clearing readLine()");
            in.readLine();
        }
    }


    // method to retrieve a list of the rover's EQUIPMENT from the server
    private ArrayList<String> getEquipment() throws IOException {
        //System.out.println("ROVER_00 method getEquipment()");
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
        out.println("EQUIPMENT");

        String jsonEqListIn = in.readLine(); //grabs the string that was returned first
        if (jsonEqListIn == null) {
            jsonEqListIn = "";
        }
        StringBuilder jsonEqList = new StringBuilder();
        //System.out.println("ROVER_00 incomming EQUIPMENT result - first readline: " + jsonEqListIn);

        if (jsonEqListIn.startsWith("EQUIPMENT")) {
            while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
                if (jsonEqListIn == null) {
                    break;
                }
                //System.out.println("ROVER_00 incomming EQUIPMENT result: " + jsonEqListIn);
                jsonEqList.append(jsonEqListIn);
                jsonEqList.append("\n");
                //System.out.println("ROVER_00 doScan() bottom of while");
            }
        } else {
            // in case the server call gives unexpected results
            clearReadLineBuffer();
            return null; // server response did not start with "EQUIPMENT"
        }

        String jsonEqListString = jsonEqList.toString();
        ArrayList<String> returnList;
        returnList = gson.fromJson(jsonEqListString, new TypeToken<ArrayList<String>>() {
        }.getType());
        //System.out.println("ROVER_00 returnList " + returnList);

        return returnList;
    }


    // sends a SCAN request to the server and puts the result in the scanMap array
    public void doScan() throws IOException {
        //System.out.println("ROVER_00 method doScan()");
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .enableComplexMapKeySerialization()
                .create();
        out.println("SCAN");

        String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
        if (jsonScanMapIn == null) {
            System.out.println("ROVER_00 check connection to server");
            jsonScanMapIn = "";
        }
        StringBuilder jsonScanMap = new StringBuilder();
        System.out.println("ROVER_00 incomming SCAN result - first readline: " + jsonScanMapIn);

        if (jsonScanMapIn.startsWith("SCAN")) {
            while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
                //System.out.println("ROVER_00 incomming SCAN result: " + jsonScanMapIn);
                jsonScanMap.append(jsonScanMapIn);
                jsonScanMap.append("\n");
                //System.out.println("ROVER_00 doScan() bottom of while");
            }
        } else {
            // in case the server call gives unexpected results
            clearReadLineBuffer();
            return; // server response did not start with "SCAN"
        }
        //System.out.println("ROVER_00 finished scan while");

        String jsonScanMapString = jsonScanMap.toString();
        // debug print json object to a file
        //new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

        //System.out.println("ROVER_00 convert from json back to ScanMap class");
        // convert from the json string back to a ScanMap object
        scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);
    }


    // this takes the server response string, parses out the x and x values and
    // returns a Coord object
    public static Coord extractLocationFromString(String sStr) {
        int indexOf;
        indexOf = sStr.indexOf(" ");
        sStr = sStr.substring(indexOf + 1);
        if (sStr.lastIndexOf(" ") != -1) {
            String xStr = sStr.substring(0, sStr.lastIndexOf(" "));
            //System.out.println("extracted xStr " + xStr);

            String yStr = sStr.substring(sStr.lastIndexOf(" ") + 1);
            //System.out.println("extracted yStr " + yStr);
            return new Coord(Integer.parseInt(xStr), Integer.parseInt(yStr));
        }
        return null;
    }


    /**
     * Runs the client
     */
    public static void main(String[] args) throws Exception {
        ROVER_00_Comm client;
        // if a command line argument is included it is used as the map filename
        // if present uses an IP address instead of localhost

        if (!(args.length == 0)) {
            client = new ROVER_00_Comm(args[0]);
        } else {
            client = new ROVER_00_Comm();
        }

        client.run();
    }
}