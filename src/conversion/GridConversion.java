package conversion;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import ui.MapPolygon;

import java.util.ArrayList;

public class GridConversion {

    private static final int OUTSIDE = 0;
    private static final int INSIDE = 1;
    private static final int PURSUER = 2;
    private static final int INVADER = 3;

    public static void convert(ArrayList<MapPolygon> polygons, ArrayList<Circle> pursuers, ArrayList<Circle> invaders, double width, double height, double cellSize) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < polygons.get(0).getPoints().size(); i += 2) {
            if (polygons.get(0).getPoints().get(i) < minX) {
                minX = polygons.get(0).getPoints().get(i);
            }
            if (polygons.get(0).getPoints().get(i + 1) < minY) {
                minY = polygons.get(0).getPoints().get(i + 1);
            }

            if (polygons.get(0).getPoints().get(i) > maxX) {
                maxX = polygons.get(0).getPoints().get(i);
            }
            if (polygons.get(0).getPoints().get(i + 1) > maxY) {
                maxY = polygons.get(0).getPoints().get(i + 1);
            }
        }

        int startWidth = (int) (minX / cellSize);
        int startHeight = (int) (minY / cellSize);
        int gridWidth = (int) Math.ceil(maxX / cellSize);
        int gridHeight = (int) Math.ceil(maxY / cellSize);
        int[][] grid = new int[gridHeight - startHeight][gridWidth - startWidth];


        for (int i = startHeight; i < gridHeight; i++) {
            for (int j = startWidth; j < gridWidth; j++) {
                if (polygons.get(0).contains(j * cellSize + cellSize / 2, i * cellSize + cellSize / 2)) {
                    boolean inHole = false;
                    for (int k = 1; k < polygons.size(); k++) {
                        if (polygons.get(k).contains(j * cellSize + cellSize / 2, i * cellSize + cellSize / 2)) {
                            inHole = true;
                        }
                    }
                    if (!inHole) {
                        grid[i - startHeight][j - startWidth] = INSIDE;
                        for (Circle pursuer : pursuers) {
                            if (Math.abs(pursuer.getCenterX() - j * cellSize + cellSize / 2) <= cellSize / 2 && Math.abs(pursuer.getCenterY() - i * cellSize + cellSize / 2) <= cellSize / 2) {
                                grid[i - startHeight][j - startWidth] = PURSUER;
                            }
                        }
                        for (Circle invader : invaders) {
                            if (Math.abs(invader.getCenterX() - j * cellSize + cellSize / 2) <= cellSize / 2 && Math.abs(invader.getCenterY() - i * cellSize + cellSize / 2) <= cellSize / 2) {
                                grid[i - startHeight][j - startWidth] = INVADER;
                            }
                        }
                    }
                }
                //System.out.println("Checking (" + j + "|" + i + ")");
            }
        }

        printGrid(grid);

    }

    private static void printGrid(int[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                if (grid[i][j] == OUTSIDE) {
                    System.out.print(" ");
                } else if (grid[i][j] == INSIDE) {
                    System.out.print("-");
                } else if (grid[i][j] == PURSUER) {
                    System.out.print("O");
                } else if (grid[i][j] == INVADER) {
                    System.out.print("X");
                }
            }
            System.out.println();
        }
    }

}
