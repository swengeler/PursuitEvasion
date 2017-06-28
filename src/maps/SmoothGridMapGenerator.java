package maps;

import experiments.MapGenerator;
import javafx.scene.shape.*;
import javafx.stage.Stage;

public class SmoothGridMapGenerator extends MapGenerator {


    private final double CORRIDOR_WIDTH = 100.0;
    private final double SIDE_LENGTH = 1000.0;
    private final double X_SCALE = 0.25;
    private final double Y_SCALE = 0.25;

    private int[][] grid = new int[][]{
            {1, 0, 1, 1},
            {1, 1, 1, 1},
            {1, 1, 0, 1},
            {1, 1, 0, 1}
    };

    private Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        /*for (int i = 1; i <= 4; i++) {
            for (int j = i; j <= 4; j++) {*/
                grid = new int[2][2];
                //grid = new int[i][j];
                for (int x = 0; x < grid.length; x++) {
                    for (int y = 0; y < grid[x].length; y++) {
                        grid[x][y] = 1;
                    }
                }
                mapName = "smoothgridmap_" + grid[0].length + "_" + grid.length + "_";
                generateMap();

                for (Polygon p : mapPolygons) {
                    for (int k = 0; k < p.getPoints().size() - 2; k += 2) {
                        if (p.getPoints().get(k).equals(p.getPoints().get(k + 2)) && p.getPoints().get(k + 1).equals(p.getPoints().get(k + 3))) {
                            p.getPoints().remove(k + 2);
                            p.getPoints().remove(k + 2);
                            k -= 2;
                        }
                    }
                }

                saveMap();
                /*mapPolygons.clear();
            }
        }*/
        /*generateMap();
        saveMap();*/
        System.exit(0);
    }

    @Override
    protected void generateMap() {

        double totalWidth = (grid[0].length * SIDE_LENGTH + (grid[0].length + 1) * CORRIDOR_WIDTH) * X_SCALE;
        double totalHeight = (grid.length * SIDE_LENGTH + (grid.length + 1) * CORRIDOR_WIDTH) * Y_SCALE;

        Polygon outer = new Polygon(
                0.0, 0.0, 0.0, totalHeight, totalWidth, totalHeight, totalWidth, 0.0
        );
        mapPolygons.add(outer);

        Polygon inner;
        for (int i = 0; i < grid[0].length; i++) {
            for (int j = 0; j < grid.length; j++) {
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
