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
            setSpeed(type);
            setFieldOfViewAngle(180);
            setAlgo(1);
            //this.algo= preset1;
            //this.movable=preset1;
            //this.communicating=preset1;
        } else if (type == 2) {
            setSpeed(type);
            setFieldOfViewAngle(360);
            setAlgo(2);
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
            setSpeed(type);
            setFieldOfViewAngle(180);
            setAlgo(1);
        } else if (type == 2) {
            setSpeed(type);
            setFieldOfViewAngle(360);
            setAlgo(2);
        }
        //etc

    }

    public void setSpeed(int type) {
        if (this.movable) {
            this.speed = type * 1000;
        } else {
            this.speed = 0;
        }
    }

    public double getSpeed(Agent Agent) {
        return Agent.speed;
    }

    public void setFieldOfViewAngle(double fov) {
        this.fieldOfViewAngle = fov;
    }

    public double getFieldOfViewAngle(Agent Agent) {
        return Agent.fieldOfViewRange;
    }

    public void setAlgo(int algo) {
        this.algo = algo;
    }

    public double getAlgo(Agent Agent) {
        return Agent.algo;
    }

    public void update() {

    }

    public void move(MapRepresentation map) {

    }

}