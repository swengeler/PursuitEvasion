package maps;

import experiments.MapGenerator;
import javafx.scene.shape.*;
import javafx.stage.Stage;

public class SmoothGridMapGenerator extends MapGenerator {


    private final double CORRIDOR_WIDTH = 300.0;
    private final double SIDE_LENGTH = 800.0;
    private final double X_SCALE = 0.2;
    private final double Y_SCALE = 0.2;

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        generateMap();
        saveMap();
    }

    @Override
    protected void generateMap() {
        int[][] grid = new int[][]{
                {1, 0, 1, 1},
                {1, 1, 1, 1},
                {1, 1, 0, 1},
                {1, 1, 0, 1}
        };

        double totalWidth = (grid[0].length * SIDE_LENGTH + (grid[0].length + 1) * CORRIDOR_WIDTH) * X_SCALE;
        double totalHeight = (grid.length * SIDE_LENGTH + (grid.length + 1) * CORRIDOR_WIDTH) * Y_SCALE;

        Polygon outer = new Polygon(
                0.0, 0.0, 0.0, totalHeight, totalWidth, totalHeight, totalWidth, 0.0
        );
        mapPolygons.add(outer);

        Polygon inner;
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[j][i] == 1) {
                    inner = new Polygon(
                            (CORRIDOR_WIDTH + i * (CORRIDOR_WIDTH + SIDE_LENGTH)) * X_SCALE, (CORRIDOR_WIDTH + j * (CORRIDOR_WIDTH + SIDE_LENGTH)) * Y_SCALE,
                            (CORRIDOR_WIDTH + i * (CORRIDOR_WIDTH + SIDE_LENGTH) + SIDE_LENGTH) * X_SCALE, (CORRIDOR_WIDTH + j * (CORRIDOR_WIDTH + SIDE_LENGTH)) * Y_SCALE,
                            (CORRIDOR_WIDTH + i * (CORRIDOR_WIDTH + SIDE_LENGTH) + SIDE_LENGTH) * X_SCALE, (CORRIDOR_WIDTH + j * (CORRIDOR_WIDTH + SIDE_LENGTH) + SIDE_LENGTH) * Y_SCALE,
                            (CORRIDOR_WIDTH + i * (CORRIDOR_WIDTH + SIDE_LENGTH)) * X_SCALE, (CORRIDOR_WIDTH + j * (CORRIDOR_WIDTH + SIDE_LENGTH) + SIDE_LENGTH) * Y_SCALE
                    );
                    mapPolygons.add(inner);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
