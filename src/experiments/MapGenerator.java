package experiments;

import additionalOperations.Tuple;
import com.vividsolutions.jts.geom.*;
import entities.base.PartitioningEntity;
import entities.guarding.*;
import entities.specific.*;
import entities.utils.PathVertex;
import entities.utils.ShortestPathRoadMap;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.shape.*;
import javafx.scene.shape.Polygon;
import javafx.stage.*;
import maps.MapRepresentation;
import org.javatuples.Triplet;
import org.jdelaunay.delaunay.error.DelaunayError;
import org.jdelaunay.delaunay.geometries.DEdge;
import org.jdelaunay.delaunay.geometries.DTriangle;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import ui.Main;
import ui.ZoomablePane;

import java.io.*;
import java.util.*;

public abstract class MapGenerator extends Application {

    protected ArrayList<Polygon> mapPolygons = new ArrayList<>();
    protected String mapName;

    protected Stage stage;

    protected MapGenerator() {
        Main.pane = new ZoomablePane();
    }

    protected abstract void generateMap();

    protected void saveMap() {
        /*FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save the current map");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Map data only file", "*.mdo"));
        File selectedFile = fileChooser.showSaveDialog(stage);*/

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Save map data");
        File directory = directoryChooser.showDialog(stage);
        File[] fileListing = null;
        if (directory != null) {
            fileListing = directory.listFiles();
            if (fileListing != null) {
                for (File f : fileListing) {
                    System.out.println(f.getName());
                }
            }
        }

        if (directory != null && fileListing != null) {
            int max = -1, temp, curIndex;
            String tempString;
            for (File f : fileListing) {
                if (f.getName().startsWith(mapName) && f.getName().endsWith(".mdo")) {
                    tempString = f.getName().substring(mapName.length(), f.getName().length() - 4);
                    try {
                        temp = Integer.parseInt(tempString);
                    } catch (NumberFormatException e) {
                        temp = -1;
                    }
                    if (temp > max) {
                        max = temp;
                    }
                }
            }
            curIndex = max + 1;

//            for (Polygon p : mapPolygons) {
//                for (int i = 0; i < p.getPoints().size(); i++) {
//                    p.getPoints().set(i, (p.getPoints().get(i) + (0.1 * (Math.random() - 0.5))));
//                }
//            }

            MapRepresentation map = new MapRepresentation(mapPolygons);
            ShortestPathRoadMap sprm = new ShortestPathRoadMap(map);

            List<Coordinate> vertices = Arrays.asList(map.getPolygon().getCoordinates());
            ArrayList<Coordinate> reflexVertices = new ArrayList<>();
            Set<PathVertex> tempReflex = sprm.getVertices();
            for (PathVertex pv : tempReflex) {
                reflexVertices.add(new Coordinate(pv.getRealX(), pv.getRealY()));
            }

            ArrayList<Tuple<Geometry, Group>> fullVisibilityInfo = new ArrayList<>(vertices.size());
            ArrayList<Geometry> visibilityInfo = new ArrayList<>(vertices.size());
            for (Coordinate c1 : reflexVertices) {
                fullVisibilityInfo.add(DCRLEntity.computeVisibilityPolygon(c1, vertices, map));
                visibilityInfo.add(fullVisibilityInfo.get(fullVisibilityInfo.size() - 1).getFirst());
            }

            // write map to file
            File mapGeometryFile = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".mdo");
            System.out.println("mapGeometryFile:" + mapGeometryFile.getAbsolutePath());
            try (PrintWriter out = new PrintWriter(new FileOutputStream(mapGeometryFile))) {
                for (int i = 0; i < mapPolygons.size(); i++) {
                    for (int j = 0; j < mapPolygons.get(i).getPoints().size(); j++) {
                        out.print(mapPolygons.get(i).getPoints().get(j) + " ");
                    }
                    out.print(mapPolygons.get(i).getPoints().get(0) + " " + mapPolygons.get(i).getPoints().get(1) + " ");
                    out.println();
                }
                out.println();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // write shortest path map to file
            File shortestPathMapFile = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".spm");
            // write object to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(shortestPathMapFile))) {
                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph = sprm.getShortestPathGraph();

                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                pathVertices.addAll(shortestPathGraph.vertexSet());

                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                for (PathVertex pv1 : shortestPathGraph.vertexSet()) {
                    for (PathVertex pv2 : shortestPathGraph.vertexSet()) {
                        IndexPair pair = new IndexPair(pathVertices.indexOf(pv1), pathVertices.indexOf(pv2));
                        if (!pv1.equals(pv2) && !indexPairs.contains(pair) && shortestPathGraph.containsEdge(pv1, pv2)) {
                            indexPairs.add(pair);
                        }
                    }
                }


                out.println("pv");
                for (PathVertex pv : pathVertices) {
                    out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                    //System.out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                }
                out.println("ip");
                for (IndexPair ip : indexPairs) {
                    out.println(ip.index1 + " " + ip.index2);
                    //System.out.println(ip.index1 + " " + ip.index2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            // time intensive computation stuff
            Tuple<ArrayList<DTriangle>, ArrayList<DTriangle>> triangles = null;
            try {
                triangles = PartitioningEntity.triangulate(map);
            } catch (DelaunayError delaunayError) {
                delaunayError.printStackTrace();
            }
            ArrayList<DTriangle> nodes = triangles.getFirst();
            ArrayList<DTriangle> holeTriangles = triangles.getSecond();

            // computing adjacency between the triangles in the map -> modelling it as a graph
            Tuple<int[][], int[]> matrices = PartitioningEntity.computeAdjacency(nodes);
            int[][] originalAdjacencyMatrix = matrices.getFirst();
            int[] degreeMatrix = matrices.getSecond();

            // grouping hole triangles by holes (through adjacency)
            ArrayList<ArrayList<DTriangle>> holes = PartitioningEntity.computeHoles(holeTriangles);

            // compute separating triangles and the updated adjacency matrix
            Triplet<ArrayList<DTriangle>, ArrayList<DEdge>, int[][]> separation = PartitioningEntity.computeSeparatingTriangles(nodes, holes, originalAdjacencyMatrix, degreeMatrix);
            ArrayList<DTriangle> separatingTriangles = separation.getValue0();
            ArrayList<DEdge> nonSeparatingLines = separation.getValue1();

            Triplet<ArrayList<Line>, ArrayList<DEdge>, ArrayList<DEdge>> lineSeparation = PartitioningEntity.computeGuardingLines(separatingTriangles, nonSeparatingLines);
            ArrayList<Line> separatingLines = lineSeparation.getValue0();

            ArrayList<GuardManager> guardManagersDCRS = DCRSEntity.computeGuardManagers(separatingLines, map);
            ArrayList<SquareGuardInfo> guardManagerInfo = new ArrayList<>(guardManagersDCRS.size());
            for (GuardManager gm : guardManagersDCRS) {
                guardManagerInfo.add(((SquareGuardManager) gm).getInfo());
            }

            ArrayList<GuardManager> guardManagersDCRL = DCRLEntity.computeGuardManagers(separatingLines, fullVisibilityInfo, sprm);
            ArrayList<ArrayList<Coordinate>> originalPositionsDCRL = new ArrayList<>(guardManagersDCRL.size());
            for (GuardManager gm : guardManagersDCRL) {
                originalPositionsDCRL.add(gm.getOriginalPositions());
            }

            ArrayList<GuardManager> guardManagersDCRV = DCRVEntity.computeGuardManagers(separatingTriangles, map);
            ArrayList<ArrayList<Coordinate>> originalPositionsDCRV = new ArrayList<>(guardManagersDCRV.size());
            for (GuardManager gm : guardManagersDCRV) {
                originalPositionsDCRV.add(gm.getOriginalPositions());
            }


            File squareGuardInfo = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".sgi");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(squareGuardInfo))) {
                oos.writeObject(guardManagerInfo);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            File lineGuardInfo = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".lgi");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(lineGuardInfo))) {
                oos.writeObject(originalPositionsDCRL);
                System.out.println("originalPositionsDCRL.size(): " + originalPositionsDCRL.size());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            File triangleGuardInfo = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".tgi");
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(triangleGuardInfo))) {
                oos.writeObject(originalPositionsDCRV);
                System.out.println("originalPositionsDCRV.size(): " + originalPositionsDCRV.size());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // write shortest path map to file
            ShortestPathRoadMap sprmLR = new ShortestPathRoadMap(separatingLines, map);
            File lineRestrictedShortestPathMapFile = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".lrs");
            // write object to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(lineRestrictedShortestPathMapFile))) {
                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph = sprmLR.getShortestPathGraph();

                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                pathVertices.addAll(shortestPathGraph.vertexSet());

                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                for (PathVertex pv1 : shortestPathGraph.vertexSet()) {
                    for (PathVertex pv2 : shortestPathGraph.vertexSet()) {
                        IndexPair pair = new IndexPair(pathVertices.indexOf(pv1), pathVertices.indexOf(pv2));
                        if (!pv1.equals(pv2) && !indexPairs.contains(pair) && shortestPathGraph.containsEdge(pv1, pv2)) {
                            indexPairs.add(pair);
                        }
                    }
                }


                out.println("pv");
                for (PathVertex pv : pathVertices) {
                    out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                    //System.out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                }
                out.println("ip");
                for (IndexPair ip : indexPairs) {
                    out.println(ip.index1 + " " + ip.index2);
                    //System.out.println(ip.index1 + " " + ip.index2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // write shortest path map to file
            ShortestPathRoadMap sprmTR = new ShortestPathRoadMap(separatingLines, map);
            File triangleRestrictedShortestPathMapFile = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".trs");
            // write object to file
            try (PrintWriter out = new PrintWriter(new FileOutputStream(triangleRestrictedShortestPathMapFile))) {
                SimpleWeightedGraph<PathVertex, DefaultWeightedEdge> shortestPathGraph = sprmTR.getShortestPathGraph();

                ArrayList<PathVertex> pathVertices = new ArrayList<>();
                pathVertices.addAll(shortestPathGraph.vertexSet());

                ArrayList<IndexPair> indexPairs = new ArrayList<>();
                for (PathVertex pv1 : shortestPathGraph.vertexSet()) {
                    for (PathVertex pv2 : shortestPathGraph.vertexSet()) {
                        IndexPair pair = new IndexPair(pathVertices.indexOf(pv1), pathVertices.indexOf(pv2));
                        if (!pv1.equals(pv2) && !indexPairs.contains(pair) && shortestPathGraph.containsEdge(pv1, pv2)) {
                            indexPairs.add(pair);
                        }
                    }
                }


                out.println("pv");
                for (PathVertex pv : pathVertices) {
                    out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                    //System.out.println(pv.getRealX() + " " + pv.getRealY() + " " + pv.getEstX() + " " + pv.getEstY());
                }
                out.println("ip");
                for (IndexPair ip : indexPairs) {
                    out.println(ip.index1 + " " + ip.index2);
                    //System.out.println(ip.index1 + " " + ip.index2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            File infoFile = new File(directory.getAbsolutePath() + "/" + mapName + curIndex + ".info");
            try (PrintWriter out = new PrintWriter(new FileOutputStream(infoFile))) {
                out.print("map name: " + mapName);
                out.print("vertices: " + vertices.size());
                out.print("reflex vertices: " + reflexVertices.size());
                out.print("holes: " + holes.size());
                out.print("area: " + map.getPolygon().getArea());
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }

}
