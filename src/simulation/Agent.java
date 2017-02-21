package simulation;

public class Agent {

    private boolean pursuer, movable, communicating;
    private int level, algo;
    protected double speed, turnSpeed, turnAngle, fieldOfViewAngle, fieldOfViewRange, xPos, yPos;

    public Agent(double speed, double turnSpeed, double fieldOfViewAngle, double fieldOfViewRange) {
        this.speed = speed;
        this.turnSpeed = turnSpeed;
        this.fieldOfViewAngle = fieldOfViewAngle;
        this.fieldOfViewRange = fieldOfViewRange;
    }

    public Agent(boolean hunter, int type) {
        this.pursuer = hunter;
        this.level = type;

        if (type == 1) {
            SetSpeed(type);
            SetFOV(180);
            SetAlgo(1);
            //this.algo= preset1;
            //this.movable=preset1;
            //this.communicating=preset1;
        } else if (type == 2) {
            SetSpeed(type);
            SetFOV(360);
            SetAlgo(2);
        }
        //etc

    }

    public Agent(boolean hunter, int type, int algorithm, boolean moveable, boolean communicating, double range) {
        this.pursuer = hunter;
        this.level = type;
        this.algo = algorithm;
        this.movable = moveable;
        this.communicating = communicating;
        this.fieldOfViewRange = range;


        if (type == 1) {
            SetSpeed(type);
            SetFOV(180);
            SetAlgo(1);
        } else if (type == 2) {
            SetSpeed(type);
            SetFOV(360);
            SetAlgo(2);
        }
        //etc

    }

    public void SetSpeed(int type) {
        if (this.movable) {
            this.speed = type * 1000;
        } else {
            this.speed = 0;
        }
    }

    public double getSpeed(Agent Agent) {
        return Agent.speed;
    }

    public void SetFOV(double fov) {
        this.fieldOfViewAngle = fov;
    }

    public double getFOV(Agent Agent) {
        return Agent.fieldOfViewRange;
    }

    public void SetAlgo(int algo) {
        this.algo = algo;
    }

    public double getalgo(Agent Agent) {
        return Agent.algo;
    }

    public void update() {

    }

    public void move(MapRepresentation map) {

    }

}