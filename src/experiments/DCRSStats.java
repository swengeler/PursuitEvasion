package experiments;

import com.vividsolutions.jts.geom.Coordinate;

public class DCRSStats {

    private int nrEvaders;
    private int nrPursuers;
    public int currentEvader;
    public Coordinate[] initEvaderPositions;
    public Coordinate[] initPursuerPositions;
    public int[] nrLeafRuns;
    public int[] nrSteps;
    public int[] nrLostSight;
    public int[] nrStepsSearching;
    public int[] nrStepsFollowing;
    public int[] nrStepsFindingAgain;
    public int[] ticksTillSearchStarted;
    public boolean[] caughtBySearcher;

    public DCRSStats(int nrEvaders, int nrPursuers) {
        this.nrEvaders = nrEvaders;
        this.nrPursuers = nrPursuers;
        currentEvader = 0;
        initEvaderPositions = new Coordinate[nrEvaders];
        initPursuerPositions = new Coordinate[nrPursuers];
        nrLeafRuns = new int[nrPursuers];
        nrSteps = new int[nrPursuers];
        nrLostSight = new int[nrPursuers];
        nrStepsSearching = new int[nrPursuers];
        nrStepsFollowing = new int[nrPursuers];
        nrStepsFindingAgain = new int[nrPursuers];
        caughtBySearcher = new boolean[nrPursuers];
    }

}
