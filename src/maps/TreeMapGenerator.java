package maps;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.geom.util.AffineTransformation;
import entities.Tree;
import experiments.MapGenerator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.util.ArrayList;

public class TreeMapGenerator extends MapGenerator {

    private class TreeNode {

        private TreeNode parent;
        private ArrayList<TreeNode> children;
        final int ID;

        TreeNode(int ID) {
            this.ID = ID;
            children = new ArrayList<>();
        }

        void generateChildren(int[] branchingFactors, int currentDepth) {
            if (currentDepth >= branchingFactors.length) {
                return;
            }

            TreeNode tempChild;
            for (int i = 0; i < branchingFactors[currentDepth]; i++) {
                tempChild = new TreeNode(idCounter++);
                tempChild.generateChildren(branchingFactors, currentDepth + 1);
                addChild(tempChild);
            }
        }

        void addChild(TreeNode child) {
            children.add(child);
            child.parent = this;
        }

        void addChildren(ArrayList<TreeNode> children) {
            for (TreeNode tn : children) {
                children.add(tn);
                tn.parent = this;
            }
        }

        ArrayList<TreeNode> getChildren() {
            return children;
        }

        TreeNode getParent() {
            return parent;
        }

    }

    private final double[] DEFAULT_COORDS = {
            0.0, 0.0, 0.0, 350.0, 125.0, 350.0, 125.0, 650.0, 0.0, 650.0, 0.0, 1000.0,
            100.0, 1000.0, 100.0, 750.0, 225.0, 750.0, 225.0, 250.0, 100.0, 250.0, 100.0, 0.0
    };

    private final double[] SMOOTH_COORDS = {
            0.0, 0.0, 0.0, 350.0, 125.0, 350.0, 125.0, 650.0, 0.0, 650.0, 0.0, 1000.0,
            100.0, 1000.0, 100.0, 750.0, 225.0, 750.0, 225.0, 250.0, 100.0, 250.0, 100.0, 0.0
    };

    private final double DISTANCE_BETWEEN_SIBLINGS = 200.0;
    private final boolean INCLUDE_ROOT = true;
    private final double X_SCALE = 1.0;
    private final double Y_SCALE = 1.0;
    private final boolean X_STRETCH = true;
    private final boolean Y_STRETCH = true;
    private double width = DEFAULT_COORDS[16] * (X_STRETCH ? X_SCALE : 1.0);
    private double height = DEFAULT_COORDS[11] * Y_SCALE;

    private int[] branchingFactors = new int[]{2, 2};
    private int[] depthCount = new int[branchingFactors.length + 1];
    private int[] nodeCounter = new int[branchingFactors.length + 1];
    private double[] offset = new double[branchingFactors.length + 1];
    private double[] spacing = new double[branchingFactors.length + 1];
    private int idCounter = 0;

    private Stage stage;
    private Pane pane;

    private TreeNode root;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        pane = new Pane();
        Scene scene = new Scene(pane, 1700, 900);
        stage.setScene(scene);
        stage.show();

        mapName = "treemap_";
        for (int i = 0; i < branchingFactors.length; i++) {
            mapName += branchingFactors[i] + "_";
        }
        mapPolygons.add(new Polygon());

        generateMap();
        saveMap();
        //System.exit(1);
    }

    @Override
    protected void generateMap() {
        determineOffsetAndSpacing();
        buildTree();
        preOrderTraversal(root, 0);
        for (int i = 0; i < mapPolygons.get(0).getPoints().size(); i++) {
            if (!INCLUDE_ROOT && (i % 2 == 1)) {
                mapPolygons.get(0).getPoints().set(i, mapPolygons.get(0).getPoints().get(i) - height);
            }
            mapPolygons.get(0).getPoints().set(i, mapPolygons.get(0).getPoints().get(i) * 0.1);
        }
    }

    private void buildTree() {
        root = new TreeNode(idCounter++);
        root.generateChildren(branchingFactors, 0);
    }

    private void determineOffsetAndSpacing() {
        // first determine what the spacing of the lowest level (highest depth) nodes should be
        offset[offset.length - 1] = 0;
        spacing[spacing.length - 1] = DISTANCE_BETWEEN_SIBLINGS * X_SCALE + width;

        for (int i = offset.length - 2; i >= 0; i--) {
            // current spacing = current branching factor * previous spacing
            spacing[i] = branchingFactors[i] * spacing[i + 1];
            // current offset = previous offset + (previous spacing) * 0.5
            offset[i] = offset[i + 1] + spacing[i + 1] * 0.5 * (branchingFactors[i] - 1) - ((branchingFactors[i] % 2 == 0) || (branchingFactors[i] == 1) ? 0.0 : spacing[i] / branchingFactors[i] * 0.5);
        }
        for (int i = 0; i < offset.length; i++) {
            System.out.println("DEPTH " + i + " - offset: " + offset[i] + ", spacing: " + spacing[i]);
        }

        depthCount[0] = 1;
        for (int i = 1; i < depthCount.length; i++) {
            depthCount[i] = depthCount[i - 1] * branchingFactors[i - 1];
        }
    }

    private int traversalCounter = 0;

    private void preOrderTraversal(TreeNode root, int depth) {
        if (root == null) {
            return;
        }

        // pre-visit this node
        System.out.print("ID: " + root.ID + ", depth: " + depth + ", nodeCounter: " + nodeCounter[depth]);
        System.out.println(", xpos: " + (offset[depth] + nodeCounter[depth] * spacing[depth]));
        Label l = new Label("" + traversalCounter++);
        l.setTranslateX(offset[depth] + nodeCounter[depth] * spacing[depth] - 20);
        l.setTranslateY(((height + 20.0) * depth) * 0.25);
       //pane.getChildren().addAll(l, new Circle((offset[depth] + nodeCounter[depth] * spacing[depth]), ((height + 20.0) * depth) * 0.25, 5, Color.GREEN));

        if (depth != 0 || INCLUDE_ROOT) {
            if (depth > 0 && (nodeCounter[depth] % branchingFactors[depth - 1]) == 0) {
                mapPolygons.get(0).getPoints().addAll(
                        offset[depth] + nodeCounter[depth] * spacing[depth], (height + 100.0) * depth - 100.0
                );
            } else {
                mapPolygons.get(0).getPoints().addAll(
                        DEFAULT_COORDS[0] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[1] + (height + 100.0) * depth
                );
            }

            mapPolygons.get(0).getPoints().addAll(
                    DEFAULT_COORDS[2] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[3] + (height + 100.0) * depth,
                    DEFAULT_COORDS[4] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[5] + (height + 100.0) * depth,
                    DEFAULT_COORDS[6] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[7] + (height + 100.0) * depth,
                    DEFAULT_COORDS[8] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[9] + (height + 100.0) * depth,
                    DEFAULT_COORDS[10] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[11] + (height + 100.0) * depth
            );
        }

        // first traverse children
        for (TreeNode tn : root.getChildren()) {
            preOrderTraversal(tn, depth + 1);
        }

        // post-visit this node
        l = new Label("" + traversalCounter++);
        l.setTranslateX(offset[depth] + (nodeCounter[depth] - 1) * spacing[depth] + 5);
        l.setTranslateY(((height + 20.0) * depth) * 0.25);
        //pane.getChildren().addAll(l);

        if (depth != 0 || INCLUDE_ROOT) {
            mapPolygons.get(0).getPoints().addAll(
                    DEFAULT_COORDS[12] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[13] + (height + 100.0) * depth,
                    DEFAULT_COORDS[14] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[15] + (height + 100.0) * depth,
                    DEFAULT_COORDS[16] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[17] + (height + 100.0) * depth,
                    DEFAULT_COORDS[18] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[19] + (height + 100.0) * depth,
                    DEFAULT_COORDS[20] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[21] + (height + 100.0) * depth
            );

            if (depth > 0 && depth <= branchingFactors.length && ((nodeCounter[depth] + 1) % branchingFactors[depth - 1]) == 0) {
                mapPolygons.get(0).getPoints().addAll(
                        DEFAULT_COORDS[22] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[23] + (height + 100.0) * depth - 100.0
                );
            } else {
                mapPolygons.get(0).getPoints().addAll(
                        DEFAULT_COORDS[22] + (offset[depth] + nodeCounter[depth] * spacing[depth]), DEFAULT_COORDS[23] + (height + 100.0) * depth
                );
            }
        }

        nodeCounter[depth]++;

    }

    public static void main(String[] args) {
        launch(args);
    }

}
