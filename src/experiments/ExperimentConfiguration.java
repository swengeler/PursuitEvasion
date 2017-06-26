package experiments;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;
import entities.base.*;
import entities.specific.*;
import entities.utils.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.stage.*;
import maps.MapRepresentation;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import simulation.Agent;
import simulation.AgentSettings;
import ui.Main;
import ui.ZoomablePane;

import java.io.*;
import java.util.*;

public class ExperimentConfiguration extends Application {

    private GridPane layout;

    private MapRepresentation map;
    private List<File> selectedFiles;
    private File directory;
    private String pursuerType;
    private String evaderType;
    private int nrRuns;

    private static boolean interruptCurrentRun;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.pane = new ZoomablePane();
        layout = new GridPane();

        /*
        for (nrOfRepetitions) {
            generate the best points for each line once for each map

            starting point for ALL pursuers:
            int index = (int) (Math.random() * mapRepresentation.getBorderLines().size());
            double xPos = mapRepresentation.getBorderLines().get(index).midpoint().x;
            double yPos = mapRepresentation.getBorderLines().get(index).midpoint().y;

            (in theory, should find something better though)
            double maxX, maxY;
            PlannedPath tempPath;
            int maxDistance = -Integer.MAX_VALUE, tempDistance;
            for (Line l : mapRepresentation.getAllLines()) {
                if (!mapRepresentation.getAllLines().indexOf(l) == index) {
                    tempPath = shortestPathRoadMap.getShortestPath(l.midpoint().x, l.midpoint().y, xPos, yPos);
                    tempDistance = tempPath.getPathVertices().size();
                    if (tempDistance > maxDistance) {
                        maxDistance = tempDistance;
                        maxDistancePath = tempPath;
                        maxX = l.midpoint().x;
                        maxY = l.midpoint().y;
                    }
                }
            }
        }
         */

        Button testButton = new Button("Test");
        testButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select maps to use");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            MapRepresentation mapRepresentation = loadMap(selectedFile);

            File parent = selectedFile.getParentFile();
            ShortestPathRoadMap shortestPathRoadMap = null;
            if (parent != null) {
                File[] directory = parent.listFiles();
                if (directory != null) {
                    for (File f : directory) {
                        if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".spm")) {
                            try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                String line = in.readLine();
                                String[] numbers;
                                double[] coordinates;
                                int[] indeces;
                                while ((line = in.readLine()) != null && !line.contains("ip")) {
                                    numbers = line.split(" ");
                                    coordinates = new double[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        coordinates[i] = Double.parseDouble(numbers[i]);
                                    }
                                    pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                }

                                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                while ((line = in.readLine()) != null) {
                                    numbers = line.split(" ");
                                    indeces = new int[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        indeces[i] = Integer.parseInt(numbers[i]);
                                    }
                                    indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                }

                                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                for (PathVertex pv : pathVertices) {
                                    tempSWG.addVertex(pv);
                                }
                                for (IndexPair ip : indexPairs) {
                                    tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                }
                                shortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                Entity.initialise(shortestPathRoadMap.getMap(), shortestPathRoadMap);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".lrs")) {
                            try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                String line = in.readLine();
                                String[] numbers;
                                double[] coordinates;
                                int[] indeces;
                                while ((line = in.readLine()) != null && !line.contains("ip")) {
                                    numbers = line.split(" ");
                                    coordinates = new double[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        coordinates[i] = Double.parseDouble(numbers[i]);
                                    }
                                    pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                }

                                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                while ((line = in.readLine()) != null) {
                                    numbers = line.split(" ");
                                    indeces = new int[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        indeces[i] = Integer.parseInt(numbers[i]);
                                    }
                                    indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                }

                                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                for (PathVertex pv : pathVertices) {
                                    tempSWG.addVertex(pv);
                                }
                                for (IndexPair ip : indexPairs) {
                                    tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                }
                                shortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                Entity.initialise(shortestPathRoadMap.getMap(), shortestPathRoadMap);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        } else if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".trs")) {
                            try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                String line = in.readLine();
                                String[] numbers;
                                double[] coordinates;
                                int[] indeces;
                                while ((line = in.readLine()) != null && !line.contains("ip")) {
                                    numbers = line.split(" ");
                                    coordinates = new double[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        coordinates[i] = Double.parseDouble(numbers[i]);
                                    }
                                    pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                }

                                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                while ((line = in.readLine()) != null) {
                                    numbers = line.split(" ");
                                    indeces = new int[numbers.length];
                                    for (int i = 0; i < numbers.length; i++) {
                                        indeces[i] = Integer.parseInt(numbers[i]);
                                    }
                                    indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                }

                                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                for (PathVertex pv : pathVertices) {
                                    tempSWG.addVertex(pv);
                                }
                                for (IndexPair ip : indexPairs) {
                                    tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                }
                                shortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                Entity.initialise(shortestPathRoadMap.getMap(), shortestPathRoadMap);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
            ArrayList<ArrayList<Coordinate>> lineGuardInfo = null;
            if (parent != null) {
                File[] directory = parent.listFiles();
                if (directory != null) {
                    for (File f : directory) {
                        if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".lgi")) {
                            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                lineGuardInfo = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
                            } catch (IOException | ClassNotFoundException ex) {
                                ex.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
            ArrayList<SquareGuardInfo> squareGuardInfo = null;
            if (parent != null) {
                File[] directory = parent.listFiles();
                if (directory != null) {
                    for (File f : directory) {
                        if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".sgi")) {
                            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                squareGuardInfo = (ArrayList<SquareGuardInfo>) ois.readObject();
                            } catch (IOException | ClassNotFoundException ex) {
                                ex.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
            ArrayList<ArrayList<Coordinate>> triangleGuardInfo = null;
            if (parent != null) {
                File[] directory = parent.listFiles();
                if (directory != null) {
                    for (File f : directory) {
                        if (f.getName().startsWith(selectedFile.getName().substring(0, selectedFile.getName().length() - 4)) && f.getName().endsWith(".tgi")) {
                            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                triangleGuardInfo = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
                            } catch (IOException | ClassNotFoundException ex) {
                                ex.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }

            PartitioningEntityRequirements requirements = new PartitioningEntityRequirements();

            int evaderCounter = 0;
            for (int simulationCount = 0; simulationCount < 10; simulationCount++) {
                mapRepresentation.getEvadingEntities().clear();
                mapRepresentation.getPursuingEntities().clear();

                int index = (int) (Math.random() * mapRepresentation.getBorderLines().size());
                double xPos = mapRepresentation.getBorderLines().get(index).midPoint().x;
                double yPos = mapRepresentation.getBorderLines().get(index).midPoint().y;

                double maxX = 0, maxY = 0;
                PlannedPath tempPath, maxDistancePath = null;
                int maxDistance = -Integer.MAX_VALUE, tempDistance;
                for (LineSegment line : mapRepresentation.getAllLines()) {
                    tempPath = shortestPathRoadMap.getShortestPath(line.midPoint().x, line.midPoint().y, xPos, yPos);
                    tempDistance = tempPath.getPathVertices().size();
                    if (tempDistance > maxDistance) {
                        maxDistance = tempDistance;
                        maxDistancePath = tempPath;
                        maxX = line.midPoint().x;
                        maxY = line.midPoint().y;
                    }
                }

                Coordinate c;
                c = mapRepresentation.getBorderLines().get(index).midPoint();
                Agent a;
                AgentSettings as;
                // create entity and place agents
                DCRLEntity dcrsEntity;
                if (triangleGuardInfo == null) {
                    dcrsEntity = new DCRLEntity(mapRepresentation, requirements, null);
                } else {
                    dcrsEntity = new DCRLEntity(mapRepresentation, requirements, triangleGuardInfo);
                }
                mapRepresentation.getPursuingEntities().add(dcrsEntity);
                for (int i = 0; i < dcrsEntity.totalRequiredAgents(); i++) {
                    //c = mapRepresentation.getRandomPosition();
                    as = new AgentSettings();
                    as.setXPos(c.x);
                    as.setYPos(c.y);
                    a = new Agent(as);
                    dcrsEntity.addAgent(a);
                }

                System.out.println("Initial starting position pursuers: " + c.x + ", " + c.y);
                System.out.println("Initial starting position evader: " + maxX + ", " + maxY + " (" + maxDistancePath.getPathVertices().size() + ")");

                DistributedEntity straightLineEntity = new StaticEntity(mapRepresentation);
                c = new Coordinate(maxX, maxY);
                /*c = mapRepresentation.getRandomPosition();
                for (int i = 0; i < 10000; i++) {
                    c = mapRepresentation.getRandomPosition();
                    boolean visible = false;
                    for (Agent agent : dcrsEntity.getControlledAgents()) {
                        if (mapRepresentation.isVisible(agent.getXPos(), agent.getYPos(), c.x, c.y)) {
                            visible = true;
                            break;
                        }
                    }
                    if (!visible) {
                        break;
                    }
                }
                boolean visible = false;
                for (Agent agent : dcrsEntity.getControlledAgents()) {
                    if (mapRepresentation.isVisible(agent.getXPos(), agent.getYPos(), c.x, c.y)) {
                        visible = true;
                        break;
                    }
                }
                System.out.println("is visible: " + visible);*/
                as = new AgentSettings();
                as.setXPos(c.x);
                as.setYPos(c.y);
                a = new Agent(as);
                straightLineEntity.setAgent(a);
                mapRepresentation.getEvadingEntities().add(straightLineEntity);

                /*DistributedEntity randomEntity = new RandomEntity(mapRepresentation);
                c = mapRepresentation.getRandomPosition();
                as = new AgentSettings();
                as.setXPos(c.x);
                as.setYPos(c.y);
                a = new Agent(as);
                randomEntity.setAgent(a);
                mapRepresentation.getEvadingEntities().add(randomEntity);*/

                DCRLStats stats = new DCRLStats(1, dcrsEntity.totalRequiredAgents());
                for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                    stats.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                }
                stats.initEvaderPositions[0] = new Coordinate(straightLineEntity.getControlledAgents().get(0).getXPos(), straightLineEntity.getControlledAgents().get(0).getYPos());
                //stats.initEvaderPositions[1] = new Coordinate(randomEntity.getControlledAgents().get(0).getXPos(), randomEntity.getControlledAgents().get(0).getYPos());
                dcrsEntity.trackStats(stats);

                Agent catcher = null;
                Agent target = straightLineEntity.getControlledAgents().get(0);

                boolean simulationOver = false;
                System.out.println("Start simulation");
                long before = System.currentTimeMillis();
                int counter = 0;
                ArrayList<Agent> disabledAgents = new ArrayList<>();
                while (!simulationOver && !interruptCurrentRun) {
                    for (Entity entity : mapRepresentation.getEvadingEntities()) {
                        if (entity.isActive()) {
                            entity.move();
                        }
                    }

                    //System.out.println("wat-1: " + dcrsEntity.evaderCounter);
                    for (Entity entity : mapRepresentation.getPursuingEntities()) {
                        if (entity.isActive()) {
                            entity.move();
                        }
                    }

                    for (Entity entity : mapRepresentation.getEvadingEntities()) {
                        for (Agent a1 : entity.getControlledAgents()) {
                            if (!a1.isActive() && !disabledAgents.contains(a1)) {
                                disabledAgents.add(a1);
                                //dcrsEntity.evaderCounter++;
                            }
                        }
                    }

                        /*if (catcher == null) {
                            catcher = ((DCRSEntity) dcrsEntity).getCatcher();
                        }*/

                        /*System.out.println("Catcher position: (" + catcher.getXPos() + "|" + catcher.getYPos() + ")");
                        System.out.println("Target position: (" + target.getXPos() + "|" + target.getYPos() + ")");*/

                    simulationOver = true;
                    for (Entity entity : mapRepresentation.getEvadingEntities()) {
                        if (entity.isActive()) {
                            simulationOver = false;
                            break;
                        }
                    }
                    System.out.print(".");
                    counter++;
                    if (counter % 100 == 0) {
                        System.out.println();
                    }
                }
                //dcrsEntity.evaderCounter = new Integer(0);
                System.out.println("\nSimulation (" + simulationCount + ") took: " + (System.currentTimeMillis() - before) + " ms");
                if (!interruptCurrentRun) {
                    stats.print();
                }
                System.out.println("\n\n\n");
                interruptCurrentRun = false;
                //Entity.reset();
            }
            Entity.reset();
        });

        Button selectMapButton = new Button("Select map(s)");
        selectMapButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select maps to use");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
            selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
        });

        Button selectOutputFolderButton = new Button("Select output folder");
        selectOutputFolderButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select maps to use");
            directory = directoryChooser.showDialog(primaryStage);
        });

        ComboBox<String> selectPursuerBox = new ComboBox<>();
        selectPursuerBox.getItems().addAll("DCRVEntity", "DCRLEntity", "DCRSEntity");
        selectPursuerBox.valueProperty().addListener((ov, oldValue, newValue) -> {
            pursuerType = newValue;
        });
        selectPursuerBox.setValue("DCRVEntity");
        pursuerType = "DCRVEntity";

        ComboBox<String> selectEvaderBox = new ComboBox<>();
        selectEvaderBox.getItems().addAll("Hiding", "Straight line", "Random");
        selectEvaderBox.valueProperty().addListener((ov, oldValue, newValue) -> {
            evaderType = newValue;
        });
        selectEvaderBox.setValue("Straight line");
        evaderType = "Straight line";

        TextField nrEvadersInput = new TextField();
        nrEvadersInput.textProperty().addListener((ov, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                nrEvadersInput.setText(newValue.replaceAll("[^\\d]", ""));
            } else if (!newValue.equals("") && Integer.parseInt(newValue) > 10) {
                nrEvadersInput.setText(10 + "");
            }
        });

        TextField nrRunsInput = new TextField();
        nrRunsInput.textProperty().addListener((ov, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                nrRunsInput.setText(newValue.replaceAll("[^\\d]", ""));
            } else if (!newValue.equals("") && Integer.parseInt(newValue) > 500) {
                nrRunsInput.setText(500 + "");
                nrRuns = 500;
            } else {
                nrRuns = Integer.parseInt(newValue);
            }
        });

        CheckBox hiddenAtStartBox = new CheckBox();
        hiddenAtStartBox.setSelected(false);

        CheckBox sameInitPositions = new CheckBox();
        sameInitPositions.setSelected(false);

        Button runButton = new Button("Run");
        runButton.setOnAction((event) -> {
            if (selectedFiles != null) {
                for (File file : selectedFiles) {
                    String mapName = file.getName().substring(0, file.getName().length() - 4);
                    MapRepresentation mapRepresentation = loadMap(file);

                    File parent = file.getParentFile();
                    //File parent = directory;
                    ShortestPathRoadMap shortestPathRoadMap = null, lineRestrictedShortestPathRoadMap = null, triangleRestrictedShortestPathRoadMap = null;
                    int max = -1, temp, curIndex = 0;
                    if (parent != null) {
                        File[] directory = parent.listFiles();
                        File[] targetDirectory = this.directory.listFiles();
                        if (directory != null && targetDirectory != null) {
                            String tempString;
                            for (File f : targetDirectory) {
                                if (f.getName().startsWith(mapName) && f.getName().endsWith(".txt")) {
                                    tempString = f.getName().substring(f.getName().lastIndexOf("_") + 1, f.getName().length() - 4);
                                    System.out.println(tempString);
                                    temp = Integer.parseInt(tempString);
                                    if (temp > max) {
                                        max = temp;
                                    }
                                }
                            }
                            curIndex = max + 1;

                            for (File f : directory) {
                                if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".spm")) {
                                    try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                        ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                        String line = in.readLine();
                                        String[] numbers;
                                        double[] coordinates;
                                        int[] indeces;
                                        while ((line = in.readLine()) != null && !line.contains("ip")) {
                                            numbers = line.split(" ");
                                            coordinates = new double[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                coordinates[i] = Double.parseDouble(numbers[i]);
                                            }
                                            pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                        }

                                        ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                        while ((line = in.readLine()) != null) {
                                            numbers = line.split(" ");
                                            indeces = new int[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                indeces[i] = Integer.parseInt(numbers[i]);
                                            }
                                            indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                        }

                                        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                        for (PathVertex pv : pathVertices) {
                                            tempSWG.addVertex(pv);
                                        }
                                        for (IndexPair ip : indexPairs) {
                                            tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                        }
                                        shortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                        Entity.initialise(shortestPathRoadMap.getMap(), shortestPathRoadMap);
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                } else if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".lrs")) {
                                    try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                        ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                        String line = in.readLine();
                                        String[] numbers;
                                        double[] coordinates;
                                        int[] indeces;
                                        while ((line = in.readLine()) != null && !line.contains("ip")) {
                                            numbers = line.split(" ");
                                            coordinates = new double[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                coordinates[i] = Double.parseDouble(numbers[i]);
                                            }
                                            pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                        }

                                        ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                        while ((line = in.readLine()) != null) {
                                            numbers = line.split(" ");
                                            indeces = new int[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                indeces[i] = Integer.parseInt(numbers[i]);
                                            }
                                            indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                        }

                                        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                        for (PathVertex pv : pathVertices) {
                                            tempSWG.addVertex(pv);
                                        }
                                        for (IndexPair ip : indexPairs) {
                                            tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                        }
                                        lineRestrictedShortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                        //Entity.initialiseRestricted(lineRestrictedShortestPathRoadMap);
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }  else if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".trs")) {
                                    try (BufferedReader in = new BufferedReader(new FileReader(f))) {
                                        ArrayList<PathVertex> pathVertices = new ArrayList<>();
                                        String line = in.readLine();
                                        String[] numbers;
                                        double[] coordinates;
                                        int[] indeces;
                                        while ((line = in.readLine()) != null && !line.contains("ip")) {
                                            numbers = line.split(" ");
                                            coordinates = new double[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                coordinates[i] = Double.parseDouble(numbers[i]);
                                            }
                                            pathVertices.add(new PathVertex(coordinates[2], coordinates[3], coordinates[0], coordinates[1]));
                                        }

                                        ArrayList<IndexPair> indexPairs = new ArrayList<>();
                                        while ((line = in.readLine()) != null) {
                                            numbers = line.split(" ");
                                            indeces = new int[numbers.length];
                                            for (int i = 0; i < numbers.length; i++) {
                                                indeces[i] = Integer.parseInt(numbers[i]);
                                            }
                                            indexPairs.add(new IndexPair(indeces[0], indeces[1]));
                                        }

                                        SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> tempSWG = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

                                        for (PathVertex pv : pathVertices) {
                                            tempSWG.addVertex(pv);
                                        }
                                        for (IndexPair ip : indexPairs) {
                                            tempSWG.addEdge(pathVertices.get(ip.index1), pathVertices.get(ip.index2));
                                        }
                                        triangleRestrictedShortestPathRoadMap = new ShortestPathRoadMap(mapRepresentation, tempSWG);
                                        //Entity.initialiseRestricted(triangleRestrictedShortestPathRoadMap);
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    ArrayList<ArrayList<Coordinate>> lineGuardInfo = null;
                    if (parent != null) {
                        File[] directory = parent.listFiles();
                        if (directory != null) {
                            for (File f : directory) {
                                if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".lgi")) {
                                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                        lineGuardInfo = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
                                    } catch (IOException | ClassNotFoundException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    ArrayList<SquareGuardInfo> squareGuardInfo = null;
                    if (parent != null) {
                        File[] directory = parent.listFiles();
                        if (directory != null) {
                            for (File f : directory) {
                                if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".sgi")) {
                                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                        squareGuardInfo = (ArrayList<SquareGuardInfo>) ois.readObject();
                                    } catch (IOException | ClassNotFoundException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    ArrayList<ArrayList<Coordinate>> triangleGuardInfo = null;
                    if (parent != null) {
                        File[] directory = parent.listFiles();
                        if (directory != null) {
                            for (File f : directory) {
                                if (f.getName().startsWith(file.getName().substring(0, file.getName().length() - 4)) && f.getName().endsWith(".tgi")) {
                                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                                        triangleGuardInfo = (ArrayList<ArrayList<Coordinate>>) ois.readObject();
                                    } catch (IOException | ClassNotFoundException ex) {
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        }
                    }

                    PartitioningEntityRequirements requirements = new PartitioningEntityRequirements();

                    File logFile = new File(this.directory.getAbsolutePath() +"/" + mapName + "_exp_" + curIndex + ".txt");
                    try (PrintWriter out = new PrintWriter(new FileOutputStream(logFile, true))) {
                        out.println("map name: " + mapName);
                        out.println("pursuer type: " + selectPursuerBox.getValue());
                        out.println("evader type: " + selectEvaderBox.getValue());
                        out.println("number of runs: " + nrRuns);
                        out.println();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    for (int simulationCount = 0; simulationCount < nrRuns; simulationCount++) {
                        mapRepresentation.getEvadingEntities().clear();
                        mapRepresentation.getPursuingEntities().clear();

                        int index = (int) (Math.random() * mapRepresentation.getBorderLines().size());
                        double xPos = mapRepresentation.getBorderLines().get(index).midPoint().x;
                        double yPos = mapRepresentation.getBorderLines().get(index).midPoint().y;

                        double maxX = 0, maxY = 0;
                        PlannedPath tempPath, maxDistancePath = null;
                        int maxDistance = -Integer.MAX_VALUE, tempDistance;
                        for (LineSegment line : mapRepresentation.getAllLines()) {
                            tempPath = shortestPathRoadMap.getShortestPath(line.midPoint().x, line.midPoint().y, xPos, yPos);
                            if (tempPath != null) {
                                tempDistance = tempPath.getPathVertices().size();
                                if (tempDistance > maxDistance) {
                                    maxDistance = tempDistance;
                                    maxDistancePath = tempPath;
                                    maxX = line.midPoint().x;
                                    maxY = line.midPoint().y;
                                }
                            }
                        }

                        Coordinate c;
                        c = mapRepresentation.getBorderLines().get(index).midPoint();
                        Agent a;
                        AgentSettings as;
                        // create entity and place agents
                        CentralisedEntity dcrsEntity = null;
                        if (selectPursuerBox.getValue().equals("DCRVEntity")) {
                            if (triangleGuardInfo == null) {
                                dcrsEntity = new DCRVEntity(mapRepresentation, requirements, null);
                            } else {
                                dcrsEntity = new DCRVEntity(mapRepresentation, requirements, triangleGuardInfo);
                            }
                            if (triangleRestrictedShortestPathRoadMap != null) {
                                Entity.initialiseRestricted(triangleRestrictedShortestPathRoadMap);
                            }
                        } else if (selectPursuerBox.getValue().equals("DCRSEntity")) {
                            if (squareGuardInfo == null) {
                                dcrsEntity = new DCRSEntity(mapRepresentation, requirements, null);
                            } else {
                                dcrsEntity = new DCRSEntity(mapRepresentation, requirements, squareGuardInfo);
                            }
                            if (lineRestrictedShortestPathRoadMap != null) {
                                Entity.initialiseRestricted(lineRestrictedShortestPathRoadMap);
                            }
                        } else if (selectPursuerBox.getValue().equals("DCRLEntity")) {
                            if (lineGuardInfo == null) {
                                dcrsEntity = new DCRLEntity(mapRepresentation, requirements, null);
                            } else {
                                dcrsEntity = new DCRLEntity(mapRepresentation, requirements, lineGuardInfo);
                            }
                            if (lineRestrictedShortestPathRoadMap != null) {
                                Entity.initialiseRestricted(lineRestrictedShortestPathRoadMap);
                            }
                        }

                        mapRepresentation.getPursuingEntities().add(dcrsEntity);
                        for (int i = 0; i < dcrsEntity.totalRequiredAgents(); i++) {
                            as = new AgentSettings();
                            as.setXPos(c.x);
                            as.setYPos(c.y);
                            a = new Agent(as);
                            dcrsEntity.addAgent(a);
                        }

                        DistributedEntity evaderEntity = null;
                        if (selectEvaderBox.getValue().equals("Hiding")) {
                            evaderEntity = new HideEntity(mapRepresentation);
                        } else if (selectEvaderBox.getValue().equals("Straight line")) {
                            evaderEntity = new StraightLineEntity(mapRepresentation);
                        } else if (selectEvaderBox.getValue().equals("Random")) {
                            evaderEntity = new RandomEntity(mapRepresentation);
                        } else if (selectEvaderBox.getValue().equals("Static")) {
                            evaderEntity = new StaticEntity(mapRepresentation);
                        }
                        c = new Coordinate(maxX, maxY);
                        as = new AgentSettings();
                        as.setXPos(c.x);
                        as.setYPos(c.y);
                        a = new Agent(as);
                        evaderEntity.setAgent(a);
                        mapRepresentation.getEvadingEntities().add(evaderEntity);

                        DCRVStats stats1 = null;
                        DCRSStats stats2 = null;
                        DCRLStats stats3 = null;
                        if (selectPursuerBox.getValue().equals("DCRVEntity")) {
                            stats1 = new DCRVStats(1, dcrsEntity.totalRequiredAgents());
                            for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                                stats1.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                            }
                            stats1.initEvaderPositions[0] = new Coordinate(evaderEntity.getControlledAgents().get(0).getXPos(), evaderEntity.getControlledAgents().get(0).getYPos());
                            ((DCRVEntity) dcrsEntity).trackStats(stats1);
                        } else if (selectPursuerBox.getValue().equals("DCRSEntity")) {
                            stats2 = new DCRSStats(1, dcrsEntity.totalRequiredAgents());
                            for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                                stats2.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                            }
                            stats2.initEvaderPositions[0] = new Coordinate(evaderEntity.getControlledAgents().get(0).getXPos(), evaderEntity.getControlledAgents().get(0).getYPos());
                            ((DCRSEntity) dcrsEntity).trackStats(stats2);
                        } else if (selectPursuerBox.getValue().equals("DCRLEntity")) {
                            stats3 = new DCRLStats(1, dcrsEntity.totalRequiredAgents());
                            for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                                stats3.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                            }
                            stats3.initEvaderPositions[0] = new Coordinate(evaderEntity.getControlledAgents().get(0).getXPos(), evaderEntity.getControlledAgents().get(0).getYPos());
                            ((DCRLEntity) dcrsEntity).trackStats(stats3);
                        }

                        boolean simulationOver = false;
                        System.out.println("Start simulation");
                        long before = System.currentTimeMillis();
                        int counter = 0;
                        try {
                            while (!simulationOver && !interruptCurrentRun) {
                                for (Entity entity : mapRepresentation.getEvadingEntities()) {
                                    if (entity.isActive()) {
                                        entity.move();
                                    }
                                }

                                //System.out.println("wat-1: " + dcrsEntity.evaderCounter);
                                for (Entity entity : mapRepresentation.getPursuingEntities()) {
                                    if (entity.isActive()) {
                                        entity.move();
                                    }
                                }

                                simulationOver = true;
                                for (Entity entity : mapRepresentation.getEvadingEntities()) {
                                    if (entity.isActive()) {
                                        simulationOver = false;
                                        break;
                                    }
                                }
                                System.out.print(".");
                                counter++;
                                if (counter % 100 == 0) {
                                    System.out.println();
                                }
                            }
                        } catch (Error | Exception e) {
                            interruptCurrentRun();
                            e.printStackTrace();
                        }
                        System.out.println("\nSimulation (" + simulationCount + ") took: " + (System.currentTimeMillis() - before) + " ms");
                        if (!interruptCurrentRun) {
                            try (PrintWriter out = new PrintWriter(new FileOutputStream(logFile, true))) {
                                out.println("SIMULATION RUN " + simulationCount + ":");
                                if (selectPursuerBox.getValue().equals("DCRVEntity")) {
                                    out.println(stats1);
                                } else if (selectPursuerBox.getValue().equals("DCRSEntity")) {
                                    out.println(stats2);
                                } else if (selectPursuerBox.getValue().equals("DCRLEntity")) {
                                    out.println(stats3);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println("\n\n\n");
                        interruptCurrentRun = false;
                    }
                    Entity.reset();
                }
                System.out.println("All maps processed");
            }
        });

        //leftLayout.getChildren().addAll(new Label(), new Label("Select pursuer: "), new Label("Select evader: "), new Label("Number evaders: "), new Label("Number runs: "), new Label("Hidden at start: "), new Label(), new Label());
        layout.getChildren().addAll(testButton, selectPursuerBox, selectEvaderBox, nrRunsInput, selectMapButton, selectOutputFolderButton, runButton);

        layout.add(new Label("Select pursuer: "), 0, 1);
        layout.add(new Label("Select evader: "), 0, 2);
        layout.add(new Label("Number runs: "), 0, 3);

        GridPane.setConstraints(testButton, 1, 0);
        GridPane.setConstraints(selectPursuerBox, 1, 1);
        GridPane.setConstraints(nrEvadersInput, 1, 2);
        GridPane.setConstraints(nrRunsInput, 1, 3);
        GridPane.setConstraints(selectMapButton, 1, 4);
        GridPane.setConstraints(selectOutputFolderButton, 1, 5);
        GridPane.setConstraints(runButton, 1, 6);

        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Experiment Configuration");
        primaryStage.show();
    }

    public static MapRepresentation loadMap(File fileToOpen) {
        ArrayList<Polygon> polygons;
        if (fileToOpen != null) {
            polygons = new ArrayList<>();
            Polygon tempPolygon;
            try (BufferedReader in = new BufferedReader(new FileReader(fileToOpen))) {
                // read in the map and file
                String line = in.readLine();
                if (line.contains("map")) {
                    String[] coords;
                    double[] coordsDouble;
                    while ((line = in.readLine()) != null && !line.contains("agents")) {
                        tempPolygon = new Polygon();
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coordsDouble.length; i += 2) {
                            tempPolygon.getPoints().addAll(coordsDouble[i], coordsDouble[i + 1]);
                        }
                        polygons.add(tempPolygon);
                    }
                } else {
                    System.out.println(line);
                    String[] coords;
                    double[] coordsDouble;
                    boolean firstLoop = true;
                    while (firstLoop || ((line = in.readLine()) != null && !line.isEmpty())) {
                        firstLoop = false;
                        tempPolygon = new Polygon();
                        coords = line.split(" ");
                        coordsDouble = new double[coords.length];
                        for (int i = 0; i < coords.length; i++) {
                            coordsDouble[i] = Double.parseDouble(coords[i]);
                        }

                        for (int i = 0; i < coordsDouble.length - 2; i += 2) {
                            tempPolygon.getPoints().addAll(coordsDouble[i], coordsDouble[i + 1]);
                            //System.out.println("x: " + coordsDouble[i] + ", y: " + coordsDouble[i + 1]);
                        }
                        polygons.add(tempPolygon);
                    }
                }
                return new MapRepresentation(polygons);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static void interruptCurrentRun() {
        System.err.println("Current simulation run interrupted");
        interruptCurrentRun = true;
    }

    public static void main(String[] args) {
        launch(args);
    }

}
