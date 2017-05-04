package simulation;

public class PolicyFactory {

    private static PolicyFactory instance = new PolicyFactory();

    public static PolicyFactory getInstance() {
        return instance;
    }

    private PolicyFactory() {
    }

    public MovePolicy getPolicy(String policyType, Agent agent) {
        return null;
    }

}
