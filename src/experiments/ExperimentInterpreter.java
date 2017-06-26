package experiments;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.List;

public class ExperimentInterpreter extends Application {

    private GridPane layout;
    private List<File> selectedFiles;

    @Override
    public void start(Stage primaryStage) throws Exception {
        layout = new GridPane();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select experiment(s)");

        Button selectExperimentsButton = new Button("Select experiment(s)");
        selectExperimentsButton.setOnAction(ae -> {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Experiment file(s)", "*.txt"));
            selectedFiles = fileChooser.showOpenMultipleDialog(primaryStage);
            if (selectedFiles != null) {
                fileChooser.setInitialDirectory(selectedFiles.get(0).getParentFile());
            }
        });

        Button runButton = new Button("Run");
        runButton.setOnAction(ae -> {
            if (selectedFiles != null) {
                for (File file: selectedFiles) {
                    try {
                        interpret(file);
                    } catch(IOException e) {
                        System.err.println("Failed interpreting file " + file.getAbsolutePath());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("No files selected");
            }
        });

        layout.add(selectExperimentsButton, 0, 0);
        layout.add(runButton, 0, 1);

        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Experiment interpreter");
        primaryStage.show();
    }

    public static void interpret(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line;

        boolean newFile = true;

        double simulations = 0;
        int leafRuns = 0;
        int steps = 0;
        int timesCaught = 0;

        String mapName = null;
        String pursuerType = null;
        String evaderType = null;

        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("map name")) {
                mapName = line.split(" ")[2];
            } else if (line.startsWith("pursuer type")) {
                pursuerType = line.split(" ")[2];
            } else if (line.startsWith("evader type")) {
                evaderType = line.split(" ")[2];
            } else if (line.startsWith("SIMULATION RUN")) {
                simulations++;
            } else if (line.startsWith("nrLeafRuns")) {
                leafRuns += Integer.parseInt(line.split(" ")[1]);
            } else if (line.startsWith("nrSteps")) {
                steps += Integer.parseInt(line.split(" ")[1]);
            } else if (line.startsWith("caughtByCatcher: true")) {
                timesCaught++;
            }
        }

        String directoryName = "\\results";
        String fileName = directoryName + "\\" + mapName + "_stats.txt";
        File directory = new File(file.getParentFile().getAbsolutePath() + directoryName);
        File fileToSave = new File(file.getParentFile().getCanonicalPath() + fileName);

        boolean directoryCreated = false;

        if (!directory.exists()) {
            directoryCreated = directory.mkdir();
        } else {
            directoryCreated = true;
        }

        if (directoryCreated && !fileToSave.getAbsoluteFile().exists()) {
            fileToSave.createNewFile();
        } else {
            newFile = false;
        }

        if (newFile) {
            FileWriter fileWriter = new FileWriter(fileToSave);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("pursuer type: " + pursuerType);
            bufferedWriter.write("\n");
            bufferedWriter.write("evader type: " + evaderType);
            bufferedWriter.write("\n\n");
            bufferedWriter.write("simulations: " + simulations);
            bufferedWriter.write("\n");
            bufferedWriter.write("total leaf runs: " + leafRuns);
            bufferedWriter.write("\n");
            bufferedWriter.write("avg leaf runs: " + leafRuns / simulations);
            bufferedWriter.write("\n");
            bufferedWriter.write("total steps: " + steps);
            bufferedWriter.write("\n");
            bufferedWriter.write("avg steps: " + steps / simulations);
            bufferedWriter.write("\n");
            bufferedWriter.write("times caught: " + timesCaught);
            bufferedWriter.write("\n");
            bufferedWriter.write("catch percentage: " + timesCaught / simulations);
            bufferedWriter.write("\n");

            bufferedWriter.close();
            fileWriter.close();
        }

        bufferedReader.close();
        fileReader.close();
        line = null;

        boolean flag = false;
        boolean hasData = false;
        boolean duplicate = false;

        if (!newFile) {
            fileReader = new FileReader(fileToSave);
            bufferedReader = new BufferedReader(fileReader);

            double existingSimulations = 0;
            int existingTotalLeafRuns = 0;
            double existingAvgLeafRuns = 0;
            int existingTotalSteps = 0;
            double existingAvgSteps = 0;
            int existingTimesCaught = 0;
            double existingCatchPercentage = 0;
            String oldContent = "";
            String contentToPossiblyReplace = "";

            while ((line = bufferedReader.readLine()) != null) {
                oldContent = oldContent + line + System.lineSeparator();

                if (line.startsWith("pursuer type")) {
                    String existingPursuerType = line.split(" ")[2];
                    line = bufferedReader.readLine();
                    oldContent = oldContent + line + System.lineSeparator();
                    String existingEvaderType = line.split(" ")[2];

                    if (pursuerType.equals(existingPursuerType) && evaderType.equals(existingEvaderType)) {
                        System.out.println("Found existing");
                        flag = true;
                        //make sure data is different
                    }
                }

                if (flag) {

                    if (!line.startsWith("evader type")) {
                        contentToPossiblyReplace = contentToPossiblyReplace + line + System.lineSeparator();
                    }

                    hasData = true;
                    if (line.startsWith("simulations")) {
                        existingSimulations = Double.parseDouble(line.split(" ")[1]);
                    } else if (line.startsWith("total leaf runs")) {
                        existingTotalLeafRuns = Integer.parseInt(line.split(" ")[3]);
                    } else if (line.startsWith("avg leaf runs")) {
                        existingAvgLeafRuns = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("total steps")) {
                        existingTotalSteps = Integer.parseInt(line.split(" ")[2]);
                    } else if (line.startsWith("avg steps")) {
                        existingAvgSteps = Double.parseDouble(line.split(" ")[2]);
                    } else if (line.startsWith("times caught")) {
                        existingTimesCaught = Integer.parseInt(line.split(" ")[2]);
                    } else if (line.startsWith("catch percentage")) {
                        existingCatchPercentage = Double.parseDouble(line.split(" ")[2]);
                        flag = false;
                    }

                    if (!flag) {
                        if (existingSimulations == simulations && existingTotalLeafRuns == leafRuns
                                && existingAvgLeafRuns == leafRuns/simulations && existingTotalSteps == steps
                                && existingAvgSteps == steps/simulations && existingTimesCaught == timesCaught
                                && existingCatchPercentage == timesCaught/simulations) {
                            System.out.println("duplicate data!");
                            duplicate = true;
                        }
                    }
                }
            }

            if (hasData && !duplicate) {
                double newSimulations = simulations + existingSimulations;
                int newTotalLeafRuns = leafRuns + existingTotalLeafRuns;
                double newAvgLeafRuns = (leafRuns/simulations + existingAvgLeafRuns) / 2;
                int newTotalSteps = steps + existingTotalSteps;
                double newAvgSteps = (steps/simulations + existingAvgSteps) / 2;
                int newTimesCaught = timesCaught + existingTimesCaught;
                double newCatchPercentage = (timesCaught/simulations + existingCatchPercentage) / 2;

                StringBuilder sb = new StringBuilder();
                sb.append("\nsimulations: " + newSimulations);
                sb.append("\ntotal leaf runs: " + newTotalLeafRuns);
                sb.append("\navg leaf runs: " + newAvgLeafRuns);
                sb.append("\nstotal steps: " + newTotalSteps);
                sb.append("\navg steps: " + newAvgSteps);
                sb.append("\ntimes caught: " + newTimesCaught);
                sb.append("\ncatch percentage: " + newCatchPercentage);
                String replacingContent = sb.toString();

                String newContent = oldContent.replace(contentToPossiblyReplace, replacingContent);

                FileWriter fileWriter = new FileWriter(fileToSave);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                bufferedWriter.write(newContent);

                bufferedWriter.close();
                fileWriter.close();
            }

            if (!hasData) {
                FileWriter fw = new FileWriter(fileToSave, true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw);

                out.println();
                out.println("pursuer type: " + pursuerType);
                out.println("evader type: " + evaderType);
                out.println();
                out.println("simulations: " + simulations);
                out.println("total leaf runs: " + leafRuns);
                out.println("avg leaf runs: " + leafRuns / simulations);
                out.println("total steps: " + steps);
                out.println("avg steps: " + steps / simulations);
                out.println("times caught: " + timesCaught);
                out.println("catch percentage: " + timesCaught / simulations);

                out.close();
                bw.close();
                fw.close();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
