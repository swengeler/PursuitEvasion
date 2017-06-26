package experiments;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.Arrays;
import java.util.stream.IntStream;

public class DCRSStats {

    public String mapName;
    public int nrEvaders;
    public int nrPursuers;
    public int currentEvader;
    public Coordinate[] initEvaderPositions;
    public Coordinate[] initPursuerPositions;
    public int[] nrLeafRunsBeforeFinding;
    public int[] nrSteps;
    public int[] nrLostSight;
    public int[] nrStepsSearching;
    public int[] nrStepsFollowing;
    public int[] nrStepsFindingAgain;
    public int[] ticksTillSearchStarted;
    public boolean[] caughtByCatcher;

    public DCRSStats(int nrEvaders, int nrPursuers) {
        this.nrEvaders = nrEvaders;
        this.nrPursuers = nrPursuers;
        currentEvader = 0;
        initEvaderPositions = new Coordinate[nrEvaders];
        initPursuerPositions = new Coordinate[nrPursuers];
        nrLeafRunsBeforeFinding = new int[nrEvaders];
        nrSteps = new int[nrEvaders];
        nrLostSight = new int[nrEvaders];
        nrStepsSearching = new int[nrEvaders];
        nrStepsFollowing = new int[nrEvaders];
        nrStepsFindingAgain = new int[nrEvaders];
        ticksTillSearchStarted = new int[nrEvaders];
        caughtByCatcher = new boolean[nrEvaders];
    }

    public void print() {
        System.out.println("Number of evaders: " + nrEvaders);
        System.out.println("Initial evader positions: ");
        System.out.println(Arrays.toString(initEvaderPositions));
        /*for (int i = 0; i < initEvaderPositions.length; i++) {
            System.out.println((i + 1) + ": (" + initEvaderPositions[i].x + "|" + initEvaderPositions[i].y + ")");
        }*/
        System.out.println("Number of pursuers: " + nrPursuers);
        System.out.println(Arrays.toString(initPursuerPositions));
        /*System.out.println("Initial pursuer positions: ");
        for (int i = 0; i < initPursuerPositions.length; i++) {
            System.out.println((i + 1) + ": (" + initPursuerPositions[i].x + "|" + initPursuerPositions[i].y + ")");
        }*/
        System.out.println("Total number of leaf runs till found: " + IntStream.of(nrLeafRunsBeforeFinding).sum());
        System.out.println("Number of leaf runs per evader: ");
        for (int i = 0; i < nrLeafRunsBeforeFinding.length; i++) {
            System.out.println((i + 1) + ": " + nrLeafRunsBeforeFinding[i]);
        }
        System.out.println("Number of steps per evader: ");
        for (int i = 0; i < nrSteps.length; i++) {
            System.out.println((i + 1) + ": " + nrSteps[i]);
        }
        System.out.println("nrLostSight: " + nrLostSight[0]);
        System.out.println("nrStepsSearching: " + nrStepsSearching[0]);
        System.out.println("nrStepsFollowing: " + nrStepsFollowing[0]);
        System.out.println("nrStepsFindingAgain: " + nrStepsFindingAgain[0]);
        System.out.println("ticksTillSearchStarted: " + ticksTillSearchStarted[0]);
        System.out.println("Caught by catcher?: ");
        for (int i = 0; i < caughtByCatcher.length; i++) {
            System.out.println((i + 1) + ": " + caughtByCatcher[i]);
        }
    }

    @Override
    public String toString() {
        String result = "nrEvaders: " + nrEvaders + "\r\n";
        result += "nrPursuers: " + nrPursuers + "\r\n";
        result += "initEvaderPositions: " + Arrays.toString(initEvaderPositions) + "\r\n";
        result += "initPursuerPositions: " + Arrays.toString(initPursuerPositions) + "\r\n";
        result += "nrLeafRunsBeforeFinding: " + nrLeafRunsBeforeFinding[0] + "\r\n";
        result += "nrSteps: " + nrSteps[0] + "\r\n";
        result += "nrLostSight: " + nrLostSight[0] + "\r\n";
        result += "nrStepsSearching: " + nrStepsSearching[0] + "\r\n";
        result += "nrStepsFollowing: " + nrStepsFollowing[0] + "\r\n";
        result += "nrStepsFindingAgain: " + nrStepsFindingAgain[0] + "\r\n";
        result += "ticksTillSearchStarted: " + ticksTillSearchStarted[0] + "\r\n";
        result += "caughtByCatcher: " + caughtByCatcher[0] + "\r\n";
        return result;
    }

}
