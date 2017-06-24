package experiments;

import com.vividsolutions.jts.geom.Coordinate;
import entities.base.*;
import entities.specific.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import maps.MapRepresentation;
import simulation.Agent;
import simulation.AgentSettings;
import ui.Main;
import ui.ZoomablePane;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExperimentConfiguration extends Application {

    private VBox layout;

    private List<File> selectedFiles;
    private MapRepresentation map;

    @Override
    public void start(Stage primaryStage) throws Exception {
        Main.pane = new ZoomablePane();
        layout = new VBox();

        Button testButton = new Button("Test");
        testButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select maps to use");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);

            MapRepresentation mapRepresentation = loadMap(selectedFile);

            PartitioningEntityRequirements requirements = new PartitioningEntityRequirements();

            for (int simulationCount = 0; simulationCount < 10; simulationCount++) {
                if (selectedFile != null) {
                    Coordinate c;
                    Agent a;
                    AgentSettings as;
                    // create entity and place agents
                    DCRVEntity dcrsEntity = new DCRVEntity(mapRepresentation, requirements);
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

                    DCRVStats stats = new DCRVStats(1, dcrsEntity.totalRequiredAgents());
                    for (int i = 0; i < dcrsEntity.getControlledAgents().size(); i++) {
                        stats.initPursuerPositions[i] = new Coordinate(dcrsEntity.getControlledAgents().get(i).getXPos(), dcrsEntity.getControlledAgents().get(i).getYPos());
                    }
                    stats.initEvaderPositions[0] = new Coordinate(straightLineEntity.getControlledAgents().get(0).getXPos(), straightLineEntity.getControlledAgents().get(0).getYPos());
                    dcrsEntity.trackStats(stats);

                    Agent catcher = null;
                    Agent target = straightLineEntity.getControlledAgents().get(0);

                    boolean simulationOver = false;
                    System.out.println("Start simulation");
                    long before = System.currentTimeMillis();
                    int counter = 0;
                    while (!simulationOver) {
                        System.out.println("wat1: " + stats.getCounter());
                        for (Entity entity : mapRepresentation.getEvadingEntities()) {
                            if (entity.isActive()) {
                                entity.move();
                            }
                        }
                        System.out.println("wat2: " + stats.getCounter());

                        for (Entity entity : mapRepresentation.getPursuingEntities()) {
                            System.out.println("wat2.5: " + stats.getCounter());
                            if (entity.isActive()) {
                                entity.move();
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
                    System.out.println("\nSimulation (" + simulationCount + ") took: " + (System.currentTimeMillis() - before) + " ms");
                    stats.print();
                    //Entity.reset();
                }
                System.out.println("\n\n\n");
            }
            Entity.reset();
        });

        Button selectMapButton = new Button("Select map");
        selectMapButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select maps to use");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Map data files", "*.mdo", "*.maa"));
            selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
        });

        Button runButton = new Button("Run");
        runButton.setAlignment(Pos.CENTER_RIGHT);
        layout.getChildren().addAll(testButton, selectMapButton, runButton);

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

    public static void main(String[] args) {
        launch(args);
    }

}
