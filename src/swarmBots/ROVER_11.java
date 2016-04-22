package swarmBots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import common.Coord;
import common.MapTile;
import common.ScanMap;
import enums.Terrain;
import javafx.scene.layout.Priority;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by samskim on 4/21/16.
 */
public class ROVER_11 {

    BufferedReader in;
    PrintWriter out;
    String rovername;
    ScanMap scanMap;
    int sleepTime;
    String SERVER_ADDRESS = "localhost";
    static final int PORT_ADDRESS = 9537;
    Map<Coord, Integer> fieldMap;

    public ROVER_11() {
        // constructor
        rovername = "ROVER_11";
        System.out.println(rovername + " rover object constructed");
        SERVER_ADDRESS = "localhost";
        // this should be a safe but slow timer value
        sleepTime = 300; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
        fieldMap = new HashMap<>();
    }

    public ROVER_11(String serverAddress) {
        // constructor
        System.out.println(rovername + " rover object constructed");
        rovername = "ROVER_11";
        SERVER_ADDRESS = serverAddress;
        sleepTime = 200; // in milliseconds - smaller is faster, but the server will cut connection if it is too small
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException, InterruptedException {

        // Make connection and initialize streams
        //TODO - need to close this socket
        Socket socket = new Socket(SERVER_ADDRESS, PORT_ADDRESS); // set port here
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Process all messages from server, wait until server requests Rover ID
        // name
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(rovername); // This sets the name of this instance
                // of a swarmBot for identifying the
                // thread to the server
                break;
            }
        }

        // ******** Rover logic *********
        // int cnt=0;
        String line = "";

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


        // ******* destination *******
        // TODO: implement destination assessment - go fetch science!

        // Get destinations from Sensor group. I am a driller!
        Queue<Coord> destinations = new LinkedList<>();
        destinations.add(new Coord(5, 15));
        destinations.add(new Coord(7, 16));
        destinations.add(new Coord(8, 17));
        destinations.add(new Coord(10, 17));
        destinations.add(new Coord(7, 19));
        destinations.add(new Coord(7, 20));
        destinations.add(new Coord(13, 18));
        destinations.add(new Coord(13, 23));
        destinations.add(new Coord(14, 15));
        destinations.add(new Coord(16, 11));
        destinations.add(new Coord(17, 12));
        destinations.add(new Coord(19, 15));
        destinations.add(new Coord(19, 18));
        destinations.add(new Coord(20, 17));
        destinations.add(new Coord(21, 16));
        destinations.add(new Coord(22, 18));

        Coord destination = destinations.poll();


        // start Rover controller process
        while (true) {

            // currently the requirements allow sensor calls to be made with no
            // simulated resource cost


            // **** location call ****
            out.println("LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + " check connection to server");
                line = "";
            }
            if (line.startsWith("LOC")) {
                // loc = line.substring(4);
                currentLoc = extractLOC(line);
            }
            System.out.println(rovername + " currentLoc at start: " + currentLoc);

            // after getting location set previous equal current to be able to check for stuckness and blocked later
            previousLoc = currentLoc;


            // **** get equipment listing ****
            ArrayList<String> equipment = new ArrayList<String>();
            equipment = getEquipment();
            //System.out.println("ROVER_11 equipment list results drive " + equipment.get(0));
            System.out.println(rovername + " equipment list results " + equipment + "\n");


            // ***** do a SCAN *****
            //System.out.println("ROVER_11 sending SCAN request");
            this.doScan();
            scanMap.debugPrintMap();

            // upon scan, update my field map
            MapTile[][] scanMapTiles = scanMap.getScanMap();
            updateFieldMap(currentLoc, scanMapTiles);


            // ***** MOVING *****

            // our starting position is xpos=1, ypos=5
            // direction Queue for direction


            List<String> moves = Astar(currentLoc, destination, scanMapTiles);

            System.out.println(rovername + "currentLoc: " + currentLoc + ", destination: " + destination);
            System.out.println(rovername + " moves: " + moves.toString());
            if (!moves.isEmpty()) {
                out.println("MOVE " + moves.get(0));
            } else {
                // check if rover is at the destination, drill
                if (currentLoc.equals(destination)) {
                    out.println("GATHER");
                    System.out.println(rovername + " arrived destination. Now gathering.");
                    if (!destinations.isEmpty()) {
                        destination = destinations.poll();
                        System.out.println(rovername + " going to next destination at: " + destination);
                    } else {
                        System.out.println("Nowhere else to go. Relax..");
                    }

                } else {

//                    // TODO: path blocked.
//                    String move = cardinals[(int) (Math.random() * 4)];
//                    System.out.println("MOVE " + move);
//                    out.println("MOVE " + move);
                }
            }


            // another call for current location
            out.println("LOC");
            line = in.readLine();
            if (line == null) {
                System.out.println(rovername + "ROVER_11 check connection to server");
                line = "";
            }
            if (line.startsWith("LOC")) {


                currentLoc = extractLOC(line);
            }

            //System.out.println("ROVER_11 currentLoc after recheck: " + currentLoc);
            //System.out.println("ROVER_11 previousLoc: " + previousLoc);

            // test for stuckness
            stuck = currentLoc.equals(previousLoc);

            //System.out.println("ROVER_11 stuck test " + stuck);
            //System.out.println(rovername + " blocked test " + blocked);

            // TODO - logic to calculate where to move next


            Thread.sleep(sleepTime);

            System.out.println(rovername + " ------------ bottom process control --------------");
        }

    }


    // ******* Search Methods
    public List<String> Astar(Coord current, Coord dest, MapTile[][] scanMapTiles) {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Set<Node> closed = new HashSet<>();

        // for back tracing
        Map<Node, Double> distanceMemory = new HashMap<>();
        Map<Node, Node> parentMemory = new LinkedHashMap<>();

        open.add(new Node(current, 0));
        Node destNode = new Node(dest, 0);

        // while there is a node to check in open list
        Node u = null;
        while (!open.isEmpty()) {

            u = open.poll(); // poll the closest one
            closed.add(u); // put it in closed list, to not check anymore

            // if u is destination, break;
            if (u.getCoord().equals(dest)) {
                destNode = u;
                break;
            }

            for (Coord c : getAdjacentCoordinates(u.getCoord(), scanMapTiles, current)) {
                // if this node hasn't already been checked
                if (!closed.contains(new Node(c, 0)) && fieldMap.get(c) != null && fieldMap.get(c) != 0) {

                    // TODO: MAYBE: assess cost depending on the tile's terrain, science, etc
                    double g = u.getData() + 1; // each move cost is 1, for now
                    double h = getDistance(c, dest); // distance from neighbor to destination
                    double f = h + g; // total heuristic of this neighbor c
                    Node n = new Node(c, f);

                    // for back tracing, store in hashmap
                    if (distanceMemory.containsKey(n)) {

                        // if distance of this neighboring node is less than memory, update
                        // else, leave as it is
                        if (distanceMemory.get(n) > f) {
                            distanceMemory.put(n, f);
                            open.remove(n);  // also update from open list
                            open.add(n);
                            parentMemory.put(n, u); // add in parent
                        }


                    } else {
                        // if this neighbor node is new, then add to memory
                        distanceMemory.put(n, f);
                        parentMemory.put(n, u);
                        open.add(n);
                    }

                }

            }

        }


        List<String> moves = getTrace(destNode, parentMemory);
        return moves;
    }

    private List<String> getTrace(Node dest, Map<Node, Node> parents) {
        Node backTrack = dest;
        double mindist = Double.MAX_VALUE;
        for (Node n : parents.keySet()) {
            if (n.equals(dest)) {
                backTrack = dest;
                break;
            } else {
                double distance = getDistance(dest.getCoord(), n.getCoord());
                if (distance < mindist) {
                    mindist = distance;
                    backTrack = n;
                }

            }
        }


        List<String> moves = new ArrayList<>();

        while (backTrack != null) {
            Node parent = parents.get(backTrack);
            if (parent != null) {
                int parentX = parent.getCoord().xpos;
                int parentY = parent.getCoord().ypos;
                int currentX = backTrack.getCoord().xpos;
                int currentY = backTrack.getCoord().ypos;
                if (currentX == parentX) {
                    if (parentY < currentY) {
                        moves.add(0, "S");
                    } else {
                        moves.add(0, "N");
                    }

                } else {
                    if (parentX < currentX) {
                        moves.add(0, "E");
                    } else {
                        moves.add(0, "W");
                    }
                }
            }
            backTrack = parent;

        }
        return moves;
    }

    // to check neighbors for heuristics
    public List<Coord> getAdjacentCoordinates(Coord coord, MapTile[][] scanMapTiles, Coord current) {
        List<Coord> list = new ArrayList<>();

        // coordinates
        int west = coord.xpos - 1;
        int east = coord.xpos + 1;
        int north = coord.ypos - 1;
        int south = coord.ypos + 1;

        Coord s = new Coord(coord.xpos, south); // S
        Coord e = new Coord(east, coord.ypos); // E
        Coord w = new Coord(west, coord.ypos); // W
        Coord n = new Coord(coord.xpos, north); // N

        list.add(e);
        list.add(w);
        list.add(s);
        list.add(n);

        return list;
    }

    private void updateFieldMap(Coord currentLoc, MapTile[][] scanMapTiles) {
        int centerIndex = (scanMap.getEdgeSize() - 1) / 2;


        for (int row = 0; row < scanMapTiles.length; row++) {
            for (int col = 0; col < scanMapTiles[row].length; col++) {

                MapTile mapTile = scanMapTiles[col][row];

                int xp = currentLoc.xpos - centerIndex + col;
                int yp = currentLoc.ypos - centerIndex + row;
                Coord coord = new Coord(xp, yp);
                fieldMap.put(coord, analyzeTile(mapTile, coord));
            }
        }
        // put my current position so it is walkable
        fieldMap.put(currentLoc, 1);
    }

    public int updateScanMapIndex(int currentLoc, int traceLoc, int edgeSize) {
        return ((edgeSize - 1) / 2) + (currentLoc - traceLoc);
    }

    // check if my rover can pass: 1: pass, 0: no pass,
    // TODO: implement if this terrain has science
    public int analyzeTile(MapTile maptile, Coord coord) {
//        System.out.println("hasrover: " + maptile.getHasRover() + ", terrain: " + maptile.getTerrain());
        Terrain terrain = maptile.getTerrain();
        boolean hasRover = maptile.getHasRover();

        if (hasRover || terrain == Terrain.NONE || terrain == Terrain.SAND) {
            return 0;
        }
        return 1;
    }


    public double getDistance(Coord current, Coord dest) {
        double dx = current.xpos - dest.xpos;
        double dy = current.ypos - dest.ypos;
        return Math.sqrt((dx * dx) + (dy * dy)) * 100;
    }


    // ################ Support Methods ###########################

    private void clearReadLineBuffer() throws IOException {
        while (in.ready()) {
            //System.out.println("ROVER_11 clearing readLine()");
            String garbage = in.readLine();
        }
    }


    // method to retrieve a list of the rover's equipment from the server
    private ArrayList<String> getEquipment() throws IOException {
        //System.out.println("ROVER_11 method getEquipment()");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        out.println("EQUIPMENT");

        String jsonEqListIn = in.readLine(); //grabs the string that was returned first
        if (jsonEqListIn == null) {
            jsonEqListIn = "";
        }
        StringBuilder jsonEqList = new StringBuilder();
        //System.out.println("ROVER_11 incomming EQUIPMENT result - first readline: " + jsonEqListIn);

        if (jsonEqListIn.startsWith("EQUIPMENT")) {
            while (!(jsonEqListIn = in.readLine()).equals("EQUIPMENT_END")) {
                if (jsonEqListIn == null) {
                    break;
                }
                //System.out.println("ROVER_11 incomming EQUIPMENT result: " + jsonEqListIn);
                jsonEqList.append(jsonEqListIn);
                jsonEqList.append("\n");
                //System.out.println("ROVER_11 doScan() bottom of while");
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
        //System.out.println("ROVER_11 returnList " + returnList);

        return returnList;
    }


    // sends a SCAN request to the server and puts the result in the scanMap array
    public void doScan() throws IOException {
        //System.out.println("ROVER_11 method doScan()");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        out.println("SCAN");

        String jsonScanMapIn = in.readLine(); //grabs the string that was returned first
        if (jsonScanMapIn == null) {
            System.out.println(rovername + " check connection to server");
            jsonScanMapIn = "";
        }
        StringBuilder jsonScanMap = new StringBuilder();
        System.out.println(rovername + " incomming SCAN result - first readline: " + jsonScanMapIn);

        if (jsonScanMapIn.startsWith("SCAN")) {
            while (!(jsonScanMapIn = in.readLine()).equals("SCAN_END")) {
                //System.out.println("ROVER_11 incomming SCAN result: " + jsonScanMapIn);
                jsonScanMap.append(jsonScanMapIn);
                jsonScanMap.append("\n");
                //System.out.println("ROVER_11 doScan() bottom of while");
            }
        } else {
            // in case the server call gives unexpected results
            clearReadLineBuffer();
            return; // server response did not start with "SCAN"
        }
        //System.out.println("ROVER_11 finished scan while");

        String jsonScanMapString = jsonScanMap.toString();
        // debug print json object to a file
        //new MyWriter( jsonScanMapString, 0);  //gives a strange result - prints the \n instead of newline character in the file

        //System.out.println("ROVER_11 convert from json back to ScanMap class");
        // convert from the json string back to a ScanMap object
        scanMap = gson.fromJson(jsonScanMapString, ScanMap.class);
    }


    // this takes the LOC response string, parses out the x and x values and
    // returns a Coord object
    public static Coord extractLOC(String sStr) {
        sStr = sStr.substring(4);
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
        ROVER_11 client = new ROVER_11();
        client.run();
    }


    // Node class for Astar search
    public class Node implements Comparable<Node> {
        private Coord coord;
        private double data;
        //   private Node parent;

        public Node(Coord coord, double data) {
            this.coord = coord;
            this.data = data;
        }

        public Coord getCoord() {
            return coord;
        }

        public void setCoord(Coord coord) {
            this.coord = coord;
        }

        public double getData() {
            return data;
        }

        public void setData(double data) {
            this.data = data;
        }

        @Override
        public int compareTo(Node other) {
            return (int) Math.ceil(this.data - other.data) * 10;
        }

        // only check by its coordinate, not data
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node))
                return false;
            if (this == o)
                return true;
            Node other = (Node) o;
            return this.getCoord().equals(other.getCoord());
        }

        @Override
        public int hashCode() {
            return this.getCoord().hashCode();
        }

        public String toString() {
            String str = "";
            str += "coord: " + coord + ", data: " + data;
            return str;
        }

    }
}
