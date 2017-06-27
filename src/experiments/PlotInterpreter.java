package experiments;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import sun.plugin.javascript.navig.Array;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PlotInterpreter extends Application {

    private GridPane layout;
    private List<File> selectedFiles;
    static ArrayList<ArrayList<Pair<Double, String>>> mapData = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        layout = new GridPane();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select result(s)");

        Button selectResultsButton = new Button("Select result(s)");
        selectResultsButton.setOnAction(ae -> {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Result file(s)", "*.txt"));
            selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
            if (selectedFiles != null) {
                fileChooser.setInitialDirectory(selectedFiles.get(0).getParentFile());
            }
        });

        Button runButton = new Button("Run");
        runButton.setOnAction(ae -> {
            if (selectedFiles != null) {
                for (File file : selectedFiles) {
                    try {
                        interpret(file);
                    } catch (IOException e) {
                        System.err.println("Failed interpreting file " + file.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("No files selected");
            }

            System.out.println(mapData);
        });

        layout.add(selectResultsButton, 0, 0);
        layout.add(runButton, 0, 1);

        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Plot interpreter");
        primaryStage.show();
    }

    public static void interpret(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;

        ArrayList<Pair<Double, String>> thisMapData = new ArrayList<>();

        String pursuerType = "";
        boolean onlySteps = false;
        boolean sumSteps = false;
        double searchingSteps = 0;
        double followingSteps = 0;
        double findingSteps = 0;

        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("pursuer type")) {
                if (line.split(" ")[2].equals("DCRVEntity")) {
                    onlySteps = true;
                } else {
                    pursuerType = line.split(" ")[2];
                    //sumSteps = true;
                }
            } else if (line.startsWith("total steps")) {
                if (onlySteps) {
                    thisMapData.add(new Pair(Double.parseDouble(line.split(" ")[2]), "DCRV"));
                    onlySteps = false;
                } else if (sumSteps) {
                    if (line.split(" ")[2].equals("searching:")) {
                        searchingSteps = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.split(" ")[2].equals("following:")) {
                        followingSteps = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.split(" ")[2].equals("finding")) {
                        findingSteps = Double.parseDouble(line.split(" ")[4]);
                        double total = searchingSteps + followingSteps + findingSteps;

                        if (pursuerType.equals("DCRSEntity")) {
                            thisMapData.add(new Pair(total, "DCRS"));
                        } else if (pursuerType.equals("DCRLEntity")) {
                            thisMapData.add(new Pair(total, "DCRL"));
                        }

                        searchingSteps = 0;
                        followingSteps = 0;
                        findingSteps = 0;
                        pursuerType = "";
                        sumSteps = false;
                    }
                }
            }
        }

        mapData.add(thisMapData);

        bufferedReader.close();
        fileReader.close();
    }
}
