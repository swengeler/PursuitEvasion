package experiments;

import com.vividsolutions.jts.geom.Coordinate;
import entities.base.*;
import entities.specific.*;
import entities.utils.PathVertex;
import entities.utils.ShortestPathRoadMap;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

    private List<File> selectedFiles;
    private MapRepresentation map;

    private static boolean interruptCurrentRun;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.pane = new ZoomablePane();
        layout = new GridPane();

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
                            break;
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
                Coordinate c;
                Agent a;
                AgentSettings as;
                // create entity and place agents
                DCRLEntity dcrsEntity;
                if (lineGuardInfo == null) {
                    dcrsEntity = new DCRLEntity(mapRepresentation, requirements, null);
                } else {
                    dcrsEntity = new DCRLEntity(mapRepresentation, requirements, lineGuardInfo);
                }
                mapRepresentation.getPursuingEntities().add(dcrsEntity);
                for (int i = 0; i < dcrsEntity.totalRequiredAgents(); i++) {
                    c = mapRepresentation.getRandomPosition();
                    as = new AgentSettings();
                    as.setXPos(c.x);
                    as.setYPos(c.y);
                    a = new Agent(as);
                    dcrsEntity.addAgent(a);
                }

                DistributedEntity straightLineEntity = new StraightLineEntity(mapRepresentation);
                c = mapRepresentation.getRandomPosition();
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

                DCRLStats stats = new DCRLStats(2, dcrsEntity.totalRequiredAgents());
                /*for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                    stats.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                }
                stats.initEvaderPositions[0] = new Coordinate(straightLineEntity.getControlledAgents().get(0).getXPos(), straightLineEntity.getControlledAgents().get(0).getYPos());
                stats.initEvaderPositions[1] = new Coordinate(randomEntity.getControlledAgents().get(0).getXPos(), randomEntity.getControlledAgents().get(0).getYPos());*/
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
                    //stats.print();
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

        ComboBox<String> selectPursuerBox = new ComboBox<>();
        selectPursuerBox.getItems().addAll("DCRVEntity", "DCRLEntity", "DCRSEntity");
        selectPursuerBox.setValue("DCRVEntity");

        ComboBox<String> selectEvaderBox = new ComboBox<>();
        selectEvaderBox.getItems().addAll("Hiding", "Straight line", "Random");
        selectEvaderBox.setValue("Hiding");

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
            } else if (!newValue.equals("") && Integer.parseInt(newValue) > 2000) {
                nrRunsInput.setText(2000 + "");
            }
        });

        CheckBox hiddenAtStartBox = new CheckBox();
        hiddenAtStartBox.setSelected(false);

        CheckBox sameInitPositions = new CheckBox();
        hiddenAtStartBox.setSelected(false);

        Button runButton = new Button("Run");

        //leftLayout.getChildren().addAll(new Label(), new Label("Select pursuer: "), new Label("Select evader: "), new Label("Number evaders: "), new Label("Number runs: "), new Label("Hidden at start: "), new Label(), new Label());
        layout.getChildren().addAll(testButton, selectPursuerBox, selectEvaderBox, nrEvadersInput, nrRunsInput, hiddenAtStartBox, sameInitPositions, selectMapButton, runButton);

        layout.add(new Label("Select pursuer: "), 0, 1);
        layout.add(new Label("Select evader: "), 0, 2);
        layout.add(new Label("Number evaders: "), 0, 3);
        layout.add(new Label("Number runs: "), 0, 4);
        layout.add(new Label("Hidden at start: "), 0, 5);
        layout.add(new Label("Same init positions: "), 0, 6);

        GridPane.setConstraints(testButton, 1, 0);
        GridPane.setConstraints(selectPursuerBox, 1, 1);
        GridPane.setConstraints(selectEvaderBox, 1, 2);
        GridPane.setConstraints(nrEvadersInput, 1, 3);
        GridPane.setConstraints(nrRunsInput, 1, 4);
        GridPane.setConstraints(hiddenAtStartBox, 1, 5);
        GridPane.setConstraints(sameInitPositions, 1, 6);
        GridPane.setConstraints(selectMapButton, 1, 7);
        GridPane.setConstraints(runButton, 1, 8);

        runButton.setOnAction((e -> {
            File file = new File("res/maps");
            File[] directoryListing = file.listFiles();
            if (directoryListing != null) {
                for (File f : directoryListing) {
                    if (f.getName().contains("searcher_catcher_test") && f.getName().contains(".mdo")) {
                        System.out.println(f.getName());
                    }
                }
            }

            /*System.out.println(file.getAbsolutePath());
            System.out.println(file.getParentFile().getAbsolutePath());
            System.out.println(file);*/
        }));

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
                System.out.println(polygons.size());
                return new MapRepresentation(polygons);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static void interruptCurrentRun() {
        interruptCurrentRun = true;
    }

    public static void main(String[] args) {
        launch(args);
    }

}
