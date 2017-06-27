package experiments;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.math3.analysis.function.Max;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
        StandardDeviation stddev = new StandardDeviation();

        int numOfPursuers = 0;
        double simulations = 0;

        ArrayList<Integer> leafRunsArray = new ArrayList<>();
        ArrayList<Integer> leafRunsBeforeFindingArray = new ArrayList<>();
        ArrayList<Integer> stepsArray = new ArrayList<>();
        ArrayList<Integer> stepsArray2 = new ArrayList<>();
        ArrayList<Integer> stepsSearchingArray = new ArrayList<>();
        ArrayList<Integer> stepsFollowingArray = new ArrayList<>();
        ArrayList<Integer> stepsFindingAgainArray = new ArrayList<>();
        ArrayList<Integer> catchPercentageArray = new ArrayList<>();
        ArrayList<Integer> lostSightArray = new ArrayList<>();
        ArrayList<Integer> ticksTillSearchStartedArray = new ArrayList<>();

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
            } else if (line.startsWith("nrPursuers") && numOfPursuers == 0) {
                numOfPursuers = Integer.parseInt(line.split(" ")[1]);
            } else if (line.startsWith("nrLeafRuns")) {
                if (pursuerType.equals("DCRVEntity")) {
                    leafRunsArray.add(Integer.parseInt(line.split(" ")[1]));
                } else {
                    leafRunsBeforeFindingArray.add(Integer.parseInt(line.split(" ")[1]));
                }
            } else if (line.startsWith("nrSteps")) {
                String title = line.split(" ")[0];
                if (title.equals("nrSteps:")) {
                    stepsArray.add(Integer.parseInt(line.split(" ")[1]));
                }
                if (!pursuerType.startsWith("DCRVEntity")) {
                    if (line.startsWith("nrStepsSearching")) {
                        stepsSearchingArray.add(Integer.parseInt(line.split(" ")[1]));
                    }

                    if (line.startsWith("nrStepsFollowing")) {
                        stepsFollowingArray.add(Integer.parseInt(line.split(" ")[1]));
                    }

                    if (line.startsWith("nrStepsFindingAgain")) {
                        stepsFindingAgainArray.add(Integer.parseInt(line.split(" ")[1]));
                        int index = stepsFindingAgainArray.size() - 1;
                        stepsArray2.add(stepsSearchingArray.get(index) + stepsFindingAgainArray.get(index) + stepsFindingAgainArray.get(index));
                    }
                }
            } else if (line.startsWith("nrLostSight")) {
                lostSightArray.add(Integer.parseInt(line.split(" ")[1]));
            } else if (line.startsWith("ticksTillSearchStarted")) {
                ticksTillSearchStartedArray.add(Integer.parseInt(line.split(" ")[1]));
            } else if (line.startsWith("caughtByCatcher")) {
                boolean result = Boolean.parseBoolean(line.split(" ")[1]);
                if (result) {
                    catchPercentageArray.add(1);
                } else {
                    catchPercentageArray.add(0);
                }
            }
        }

        String directoryName = "\\results";
        String fileName = directoryName + "\\" + mapName + "_stats.txt";
        String stepsName = directoryName + "\\" + mapName + "_stepstats.txt";
        File directory = new File(file.getParentFile().getAbsolutePath() + directoryName);
        File fileToSave = new File(file.getParentFile().getCanonicalPath() + fileName);
        File stepsNameFile = new File(file.getParentFile().getCanonicalPath() + stepsName);

        boolean directoryCreated = false;
        boolean newStepsFile = false;
        boolean hasStepData = false;

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

        if (!stepsNameFile.getAbsoluteFile().exists()) {
            stepsNameFile.createNewFile();
            newStepsFile = true;
        }

        ArrayList<Integer> tmpa = new ArrayList<>();

        FileWriter fw = new FileWriter(stepsNameFile, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw);

        if (pursuerType.equals("DCRVEntity")) {
            out.print("step array:" + stepsArray);
            out.println("\n");
        }

        out.close();
        bw.close();
        fw.close();

        if (newFile) {
            FileWriter fileWriter = new FileWriter(fileToSave);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.write("pursuer type: " + pursuerType);
            bufferedWriter.write("\n");
            bufferedWriter.write("evader type: " + evaderType);
            bufferedWriter.write("\n");
            bufferedWriter.write("nr pursuers: " + numOfPursuers);
            bufferedWriter.write("\n");
            bufferedWriter.write("simulations: " + simulations);
            bufferedWriter.write("\n\n");

            if (!pursuerType.equals("DCRVEntity")) {
                double[] tmp = new double[leafRunsBeforeFindingArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = leafRunsBeforeFindingArray.get(i);
                }

                bufferedWriter.write("total leaf runs before finding: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean leaf runs before finding: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev leaf runs before finding: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max leaf runs before finding: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min leaf runs before finding: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");
            } else {
                double[] tmp = new double[leafRunsArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = leafRunsArray.get(i);
                }
                bufferedWriter.write("total leaf runs: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean leaf runs: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev leaf runs: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max leaf runs: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min leaf runs: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");
            }

            if (!pursuerType.equals("DCRVEntity")) {
                double[] tmp = new double[stepsSearchingArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = stepsSearchingArray.get(i);
                }

                bufferedWriter.write("total steps searching: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean steps searching: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev steps searching: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max steps searching: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min steps searching: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");

                tmp = new double[stepsFollowingArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = stepsFollowingArray.get(i);
                }
                bufferedWriter.write("total steps following: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean steps following: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev steps following: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max steps following: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min steps following: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");

                tmp = new double[stepsFindingAgainArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = stepsFindingAgainArray.get(i);
                }

                bufferedWriter.write("total steps finding again: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean steps finding again: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev steps finding again: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max steps finding again: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min steps finding again: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");
            } else {
                double[] tmp = new double[stepsArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = stepsArray.get(i);
                }
                bufferedWriter.write("total steps: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean steps: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev steps: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max steps: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min steps: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");
            }

            if (!pursuerType.equals("DCRVEntity")) {
                bufferedWriter.write("step array:" + stepsArray2.toString());
                bufferedWriter.write("\n\n");
            } else {
                bufferedWriter.write("step array:" + stepsArray.toString());
                bufferedWriter.write("\n\n");
            }
            
            double[] tmp = new double[catchPercentageArray.size()];
            for (int i = 0; i < tmp.length; i++) {
                tmp[i] = catchPercentageArray.get(i);
            }
            bufferedWriter.write("total catch percentage: " + StatUtils.sum(tmp));
            bufferedWriter.write("\n");
            bufferedWriter.write("mean catch percentage: " + StatUtils.mean(tmp));
            bufferedWriter.write("\n");
            bufferedWriter.write("stddev catch percentage: " + stddev.evaluate(tmp));
            bufferedWriter.write("\n");
            bufferedWriter.write("max catch percentage: " + StatUtils.max(tmp));
            bufferedWriter.write("\n");
            bufferedWriter.write("min catch percentage: " + StatUtils.min(tmp));
            bufferedWriter.write("\n\n");

            if (!pursuerType.equals("DCRVEntity")) {
                tmp = new double[lostSightArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = lostSightArray.get(i);
                }
                bufferedWriter.write("total lost sight: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean lost sight: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev lost sight: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max lost sight: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min lost sight: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");

                tmp = new double[ticksTillSearchStartedArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = ticksTillSearchStartedArray.get(i);
                }
                bufferedWriter.write("total ticks till search started: " + StatUtils.sum(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("mean ticks till search started: " + StatUtils.mean(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("stddev ticks till search started: " + stddev.evaluate(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("max ticks till search started: " + StatUtils.max(tmp));
                bufferedWriter.write("\n");
                bufferedWriter.write("min ticks till search started: " + StatUtils.min(tmp));
                bufferedWriter.write("\n\n");
            }

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

            String oldContent = "";
            String contentToPossiblyReplace = "";

            double existingSimulations = 0;
            
            double existingTotalLeafRunsBeforeFinding = 0;
            double existingMeanLeafRunsBeforeFinding = 0;
            double existingStddevLeafRunsBeforeFinding = 0;
            double existingMaxLeafRunsBeforeFinding = 0;
            double existingMinLeafRunsBeforeFinding = 0;
            
            double existingTotalLeafRuns = 0;
            double existingMeanLeafRuns = 0;
            double existingStddevLeafRuns = 0;
            double existingMaxLeafRuns = 0;
            double existingMinLeafRuns = 0;

            double existingTotalStepsSearching = 0;
            double existingMeanStepsSearching = 0;
            double existingStddevStepsSearching = 0;
            double existingMaxStepsSearching = 0;
            double existingMinStepsSearching = 0;

            double existingTotalStepsFinding = 0;
            double existingMeanStepsFinding = 0;
            double existingStddevStepsFinding = 0;
            double existingMaxStepsFinding = 0;
            double existingMinStepsFinding = 0;

            double existingTotalStepsFollowing = 0;
            double existingMeanStepsFollowing = 0;
            double existingStddevStepsFollowing = 0;
            double existingMaxStepsFollowing = 0;
            double existingMinStepsFollowing = 0;

            double existingTotalSteps = 0;
            double existingMeanSteps = 0;
            double existingStddevSteps = 0;
            double existingMaxSteps = 0;
            double existingMinSteps = 0;

            double existingTotalCatchPercentage = 0;
            double existingMeanCatchPercentage = 0;
            double existingStddevCatchPercentage = 0;
            double existingMaxCatchPercentage = 0;
            double existingMinCatchPercentage = 0;

            double existingTotalLostSight = 0;
            double existingMeanLostSight = 0;
            double existingStddevLostSight = 0;
            double existingMaxLostSight = 0;
            double existingMinLostSight = 0;

            double existingTotalTicksTillSearchStarted = 0;
            double existingMeanTicksTillSearchStarted = 0;
            double existingStddevTicksTillSearchStarted = 0;
            double existingMaxTicksTillSearchStarted = 0;
            double existingMinTicksTillSearchStarted = 0;

            ArrayList<Integer> existingStepsArray = new ArrayList<>();
            
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

                    if (!line.startsWith("evader type") && !line.startsWith("nr pursuers")) {
                        contentToPossiblyReplace = contentToPossiblyReplace + line + System.lineSeparator();
                    }

                    hasData = true;
                    if (line.startsWith("simulations")) {
                        existingSimulations = Double.parseDouble(line.split(" ")[1]);
                    } else if (line.startsWith("total leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingTotalLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingTotalLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("mean leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingMeanLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingMeanLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("stddev leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingStddevLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingStddevLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("max leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingMaxLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingMaxLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("min leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingMinLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingMinLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("mean leaf runs")) {
                        if (line.split(" ").length == 4) {
                            existingMeanLeafRuns = Double.parseDouble(line.split(" ")[3]);
                        } else {
                            existingMeanLeafRunsBeforeFinding = Double.parseDouble(line.split(" ")[5]);
                        }
                    } else if (line.startsWith("total steps")) {
                        if (line.split(" ").length == 3) {
                            existingTotalSteps = Double.parseDouble(line.split(" ")[2]);
                        } else {
                            if (line.split("")[2].equals("searching")) {
                                existingTotalStepsSearching = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("following")) {
                                existingTotalStepsFollowing = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("finding")) {
                                existingTotalStepsFinding = Double.parseDouble(line.split(" ")[4]);
                            }
                        }
                    } else if (line.startsWith("mean steps")) {
                        if (line.split(" ").length == 3) {
                            existingMeanSteps = Double.parseDouble(line.split(" ")[2]);
                        } else {
                            if (line.split("")[2].equals("searching")) {
                                existingMeanStepsSearching = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("following")) {
                                existingMeanStepsFollowing = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("finding")) {
                                existingMeanStepsFinding = Double.parseDouble(line.split(" ")[4]);
                            }
                        }
                    } else if (line.startsWith("stddev steps")) {
                        if (line.split(" ").length == 3) {
                            existingStddevSteps = Double.parseDouble(line.split(" ")[2]);
                        } else {
                            if (line.split("")[2].equals("searching")) {
                                existingStddevStepsSearching = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("following")) {
                                existingStddevStepsFollowing = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("finding")) {
                                existingStddevStepsFinding = Double.parseDouble(line.split(" ")[4]);
                            }
                        }
                    } else if (line.startsWith("max steps")) {
                        if (line.split(" ").length == 3) {
                            existingMaxSteps = Double.parseDouble(line.split(" ")[2]);
                        } else {
                            if (line.split("")[2].equals("searching")) {
                                existingMaxStepsSearching = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("following")) {
                                existingMaxStepsFollowing = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("finding")) {
                                existingMaxStepsFinding = Double.parseDouble(line.split(" ")[4]);
                            }
                        }
                    } else if (line.startsWith("min steps")) {
                        if (line.split(" ").length == 3) {
                            existingMinSteps = Double.parseDouble(line.split(" ")[2]);
                        } else {
                            if (line.split("")[2].equals("searching")) {
                                existingMinStepsSearching = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("following")) {
                                existingMinStepsFollowing = Double.parseDouble(line.split(" ")[3]);
                            } else if (line.split("")[2].equals("finding")) {
                                existingMinStepsFinding = Double.parseDouble(line.split(" ")[4]);
                            }
                        }
                    } else if (line.startsWith("step array")) {
                        String[] array = line.split(":");
                        String values = array[1];
                        values = values.substring(1, values.length()-1);
                        String[] actualValues = values.split(", ");
                        for (String s: actualValues) {
                            existingStepsArray.add(Integer.parseInt(s));
                        }
                    } else if (line.startsWith("total catch percentage")) {
                        existingTotalCatchPercentage = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("mean catch percentage")) {
                        existingMeanCatchPercentage = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("stddev catch percentage")) {
                        existingStddevCatchPercentage = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("max catch percentage")) {
                        existingMaxCatchPercentage = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("min catch percentage")) {
                        existingMinCatchPercentage = Double.parseDouble(line.split(" ")[3]);
                        if (pursuerType.equals("DCRVEntity")) {
                            flag = true;
                        }
                    } else if (line.startsWith("total catch percentage")) {
                        existingTotalLostSight = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("mean catch percentage")) {
                        existingMeanLostSight = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("stddev catch percentage")) {
                        existingStddevLostSight = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("max catch percentage")) {
                        existingMaxLostSight = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("min catch percentage")) {
                        existingMinLostSight = Double.parseDouble(line.split(" ")[3]);
                    } else if (line.startsWith("total ticks till search started")) {
                        existingTotalTicksTillSearchStarted = Double.parseDouble(line.split(" ")[5]);
                    } else if (line.startsWith("mean ticks till search started")) {
                        existingMeanTicksTillSearchStarted = Double.parseDouble(line.split(" ")[5]);
                    } else if (line.startsWith("stddev ticks till search started")) {
                        existingStddevTicksTillSearchStarted = Double.parseDouble(line.split(" ")[5]);
                    } else if (line.startsWith("max ticks till search started")) {
                        existingMaxTicksTillSearchStarted = Double.parseDouble(line.split(" ")[5]);
                    } else if (line.startsWith("min ticks till search started")) {
                        existingMinTicksTillSearchStarted = Double.parseDouble(line.split(" ")[5]);
                        if (!pursuerType.equals("DCRVEntity")) {
                            flag = true;
                        }
                    }
                }
            }

            if (hasData && !duplicate) {
                StringBuilder sb = new StringBuilder();
                sb.append("simulations: " + (simulations + existingSimulations));
                sb.append("\n\n");

                if (!pursuerType.equals("DCRVEntity")) {
                    double[] tmp = new double[leafRunsBeforeFindingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = leafRunsBeforeFindingArray.get(i);
                    }
                    sb.append("total leaf runs before finding: " + (existingTotalLeafRunsBeforeFinding + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean leaf runs before finding: " + (existingMeanLeafRunsBeforeFinding + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev leaf runs before finding: " + (existingStddevLeafRunsBeforeFinding + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max leaf runs before finding: " + Math.max(existingMaxLeafRunsBeforeFinding, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min leaf runs before finding: " + Math.min(existingMinLeafRunsBeforeFinding, StatUtils.max(tmp)));
                    sb.append("\n\n");
                } else {
                    double[] tmp = new double[leafRunsArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = leafRunsArray.get(i);
                    }
                    sb.append("total leaf runs: " + (existingTotalLeafRuns + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean leaf runs: " + (existingMeanLeafRuns + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev leaf runs: " + (existingStddevLeafRuns + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max leaf runs: " + Math.max(existingMaxLeafRuns, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min leaf runs: " + Math.min(existingMinLeafRuns, StatUtils.max(tmp)));
                    sb.append("\n\n");
                }

                if (!pursuerType.equals("DCRVEntity")) {
                    double[] tmp = new double[stepsSearchingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsSearchingArray.get(i);
                    }
                    sb.append("total steps searching: " + (existingTotalStepsSearching + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean steps searching: " + (existingMeanStepsSearching + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev steps searching: " + (existingStddevStepsSearching + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max steps searching: " + Math.max(existingMaxStepsSearching, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min steps searching: " + Math.min(existingMinStepsSearching, StatUtils.max(tmp)));
                    sb.append("\n\n");

                    tmp = new double[stepsFollowingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsFollowingArray.get(i);
                    }
                    sb.append("total steps following: " + (existingTotalStepsFollowing + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean steps following: " + (existingMeanStepsFollowing + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev steps following: " + (existingStddevStepsFollowing + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max steps following: " + Math.max(existingMaxStepsFollowing, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min steps following: " + Math.min(existingMinStepsFollowing, StatUtils.max(tmp)));
                    sb.append("\n\n");

                    tmp = new double[stepsFindingAgainArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsFindingAgainArray.get(i);
                    }
                    sb.append("total steps finding again: " + (existingTotalStepsFinding + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean steps finding again: " + (existingTotalStepsFinding + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev steps finding again: " + (existingStddevStepsFinding + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max steps finding again: " + Math.max(existingMaxStepsFinding, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min steps finding again: " + Math.min(existingMinStepsFinding, StatUtils.max(tmp)));
                    sb.append("\n\n");
                } else {
                    double[] tmp = new double[stepsArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsArray.get(i);
                    }
                    sb.append("total steps: " + (existingTotalSteps + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean steps: " + (existingMeanSteps + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev steps: " + (existingStddevSteps + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max steps: " + Math.max(existingMaxSteps, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min steps: " + Math.min(existingMinSteps, StatUtils.max(tmp)));
                    sb.append("\n\n");
                }

                if (!pursuerType.equals("DCRVEntity")) {
                    existingStepsArray.addAll(stepsArray2);
                    Collections.sort(existingStepsArray);
                    sb.append("step array:" + existingStepsArray.toString());
                    sb.append("\n\n");
                } else {
                    existingStepsArray.addAll(stepsArray);
                    Collections.sort(existingStepsArray);
                    sb.append("step array:" + existingStepsArray.toString());
                    sb.append("\n\n");
                }

                double[] tmp = new double[catchPercentageArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = catchPercentageArray.get(i);
                }
                sb.append("total catch percentage: " + (existingTotalCatchPercentage + StatUtils.sum(tmp)));
                sb.append("\n");
                sb.append("mean catch percentage: " + (existingMeanCatchPercentage + StatUtils.mean(tmp)) / 2);
                sb.append("\n");
                sb.append("stddev catch percentage: " + (existingStddevCatchPercentage + stddev.evaluate(tmp)) / 2);
                sb.append("\n");
                sb.append("max catch percentage: " + Math.max(existingMaxCatchPercentage, StatUtils.max(tmp)));
                sb.append("\n");
                sb.append("min catch percentage: " + Math.min(existingMinCatchPercentage, StatUtils.max(tmp)));
                sb.append("\n\n");

                if (!pursuerType.equals("DCRVEntity")) {
                    tmp = new double[lostSightArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = lostSightArray.get(i);
                    }
                    sb.append("total lost sight: " + (existingTotalLostSight + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean lost sight: " + (existingMeanLostSight + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev lost sight: " + (existingStddevLostSight + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max lost sight: " + Math.max(existingMaxLostSight, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min lost sight: " + Math.min(existingMinLostSight, StatUtils.max(tmp)));
                    sb.append("\n\n");

                    tmp = new double[ticksTillSearchStartedArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = ticksTillSearchStartedArray.get(i);
                    }
                    sb.append("total ticks till search started: " + (existingTotalTicksTillSearchStarted + StatUtils.sum(tmp)));
                    sb.append("\n");
                    sb.append("mean ticks till search started: " + (existingMeanTicksTillSearchStarted + StatUtils.mean(tmp)) / 2);
                    sb.append("\n");
                    sb.append("stddev ticks till search started: " + (existingStddevTicksTillSearchStarted + stddev.evaluate(tmp)) / 2);
                    sb.append("\n");
                    sb.append("max ticks till search started: " + Math.max(existingMaxTicksTillSearchStarted, StatUtils.max(tmp)));
                    sb.append("\n");
                    sb.append("min ticks till search started: " + Math.min(existingMinTicksTillSearchStarted, StatUtils.max(tmp)));
                    sb.append("\n\n");
                }
                
                String replacingContent = sb.toString();

                String newContent = oldContent.replace(contentToPossiblyReplace, replacingContent);

                System.out.println("old " + oldContent);
                System.out.println("content to replace " + contentToPossiblyReplace);
                System.out.println("replacing content " + replacingContent);

                FileWriter fileWriter = new FileWriter(fileToSave);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

                bufferedWriter.write(newContent);

                bufferedWriter.close();
                fileWriter.close();
            }

            if (!hasData) {
                fw = new FileWriter(fileToSave, true);
                bw = new BufferedWriter(fw);
                out = new PrintWriter(bw);

                out.print("pursuer type: " + pursuerType);
                out.print("\n");
                out.print("evader type: " + evaderType);
                out.print("\n");
                out.print("nr pursuers: " + numOfPursuers);
                out.print("\n");
                out.print("simulations: " + simulations);
                out.print("\n\n");

                if (!pursuerType.equals("DCRVEntity")) {
                    double[] tmp = new double[leafRunsBeforeFindingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = leafRunsBeforeFindingArray.get(i);
                    }
                    out.print("total leaf runs before finding: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean leaf runs before finding: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev leaf runs before finding: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max leaf runs before finding: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min leaf runs before finding: " + StatUtils.min(tmp));
                    out.print("\n\n");
                } else {
                    double[] tmp = new double[leafRunsArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = leafRunsArray.get(i);
                    }
                    out.print("total leaf runs: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean leaf runs: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev leaf runs: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max leaf runs: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min leaf runs: " + StatUtils.min(tmp));
                    out.print("\n\n");
                }

                if (!pursuerType.equals("DCRVEntity")) {
                    double[] tmp = new double[stepsSearchingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsSearchingArray.get(i);
                    }
                    out.print("total steps searching: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean steps searching: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev steps searching: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max steps searching: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min steps searching: " + StatUtils.min(tmp));
                    out.print("\n\n");

                    tmp = new double[stepsFollowingArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsFollowingArray.get(i);
                    }
                    out.print("total steps following: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean steps following: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev steps following: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max steps following: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min steps following: " + StatUtils.min(tmp));
                    out.print("\n\n");

                    tmp = new double[stepsFindingAgainArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsFindingAgainArray.get(i);
                    }
                    out.print("total steps finding again: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean steps finding again: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev steps finding again: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max steps finding again: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min steps finding again: " + StatUtils.min(tmp));
                    out.print("\n\n");
                } else {
                    double[] tmp = new double[stepsArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = stepsArray.get(i);
                    }
                    out.print("total steps: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean steps: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev steps: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max steps: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min steps: " + StatUtils.min(tmp));
                    out.print("\n\n");
                }

                if (!pursuerType.equals("DCRVEntity")) {
                    out.print("step array:" + stepsArray2.toString());
                    out.print("\n\n");
                } else {
                    out.print("step array:" + stepsArray.toString());
                    out.print("\n\n");
                }

                double[] tmp = new double[catchPercentageArray.size()];
                for (int i = 0; i < tmp.length; i++) {
                    tmp[i] = catchPercentageArray.get(i);
                }
                out.print("total catch percentage: " + StatUtils.sum(tmp));
                out.print("\n");
                out.print("mean catch percentage: " + StatUtils.mean(tmp));
                out.print("\n");
                out.print("stddev catch percentage: " + stddev.evaluate(tmp));
                out.print("\n");
                out.print("max catch percentage: " + StatUtils.max(tmp));
                out.print("\n");
                out.print("min catch percentage: " + StatUtils.min(tmp));
                out.print("\n\n");

                if (!pursuerType.equals("DCRVEntity")) {
                    tmp = new double[lostSightArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = lostSightArray.get(i);
                    }
                    out.print("total lost sight: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean lost sight: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev lost sight: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max lost sight: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min lost sight: " + StatUtils.min(tmp));
                    out.print("\n\n");

                    tmp = new double[ticksTillSearchStartedArray.size()];
                    for (int i = 0; i < tmp.length; i++) {
                        tmp[i] = ticksTillSearchStartedArray.get(i);
                    }
                    out.print("total ticks till search started: " + StatUtils.sum(tmp));
                    out.print("\n");
                    out.print("mean ticks till search started: " + StatUtils.mean(tmp));
                    out.print("\n");
                    out.print("stddev ticks till search started: " + stddev.evaluate(tmp));
                    out.print("\n");
                    out.print("max ticks till search started: " + StatUtils.max(tmp));
                    out.print("\n");
                    out.print("min ticks till search started: " + StatUtils.min(tmp));
                    out.print("\n\n");
                }

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
