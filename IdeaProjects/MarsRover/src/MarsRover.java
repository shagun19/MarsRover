import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

class UCSNodeCumulativeCost {
    List<Integer> node;
    int cumulativeCost;
    public UCSNodeCumulativeCost(List<Integer> node, int cumulativeCost) {
        this.node = node;
        this.cumulativeCost = cumulativeCost;
    }
}

class UCSNodeCumulativeCostComparator implements Comparator<UCSNodeCumulativeCost>{
    public int compare(UCSNodeCumulativeCost uscNodeCumulativeCost1, UCSNodeCumulativeCost uscNodeCumulativeCost2) {
        if (uscNodeCumulativeCost1.cumulativeCost <= uscNodeCumulativeCost2.cumulativeCost)
            return -1;
        else return 1;
    }
}

public class MarsRover {
    private final int DIAGONAL_COST = 14;
    private final int REGULAR_COST = 10;
    private final String INPUT_TXT = "/input.txt";
    private final String OUTPUT_TXT = "/output.txt";
    private final String FAILURE_MSG = "FAIL";
    private final int[] traverseX = {1,0,-1,0,1,1,-1,-1};
    private final int[] traverseY = {0,-1,0,1,1,-1,1,-1};
    private List<Integer> retrieved;
    private Map<List<Integer>,UCSNodeCumulativeCost> parentMap = new HashMap<>();

    private String parseInputTxt(List<String> inputStringList) throws IOException {
        return inputStringList.get(0);
    }

    private List<Integer> getSourceNode(List<String> inputStringList) throws IOException {
        int sourceX = Integer.parseInt(inputStringList.get(2).split("\\s+")[1]);
        int sourceY = Integer.parseInt(inputStringList.get(2).split("\\s+")[0]);
        return Arrays.asList(sourceX,sourceY);
    }

    private List<List<Integer>> getTargetNodes(List<String> inputStringList) throws IOException{
        int numberOfTargets = Integer.parseInt(inputStringList.get(4));
        List<List<Integer>> targetNodes = new ArrayList<>();
        for(int i=0;i<numberOfTargets;i++){
            int sourceX = Integer.parseInt(inputStringList.get(5+i).split("\\s+")[1]);
            int sourceY = Integer.parseInt(inputStringList.get(5+i).split("\\s+")[0]);
            targetNodes.add(Arrays.asList(sourceX,sourceY));
        }
        return targetNodes;
    }

    private int[][] getZfactorArray(List<String> inputStringList) throws IOException{
        int[][] zfactorArray = new int[Integer.parseInt(inputStringList.get(1).split("\\s")[1])]
                [Integer.parseInt(inputStringList.get(1).split("\\s+")[0])];
        int zfactorValuesStartLine = inputStringList.size() -
                Integer.parseInt(inputStringList.get(1).split("\\s+")[1]);
        ArrayList<String> check = new ArrayList<>(inputStringList);
        int checkSize = check.size();
        for(int i = zfactorValuesStartLine; i<checkSize; i++){
        //    Instant start = Instant.now();
            String[] temp= inputStringList.get(i).split("\\s+");
            for(int j=0; j<Integer.parseInt(inputStringList.get(1).split("\\s+")[0]); j++){
                zfactorArray[i-zfactorValuesStartLine][j] = Integer.parseInt(temp[j]);
            }
       //     Instant stop = Instant.now();
        //    System.out.println(Duration.between(start,stop).toMillis());
        }
        return zfactorArray;
    }

    private List<List<Integer>> getShortestPath(Map<List<Integer>,UCSNodeCumulativeCost> parentMap, List<Integer> retrievedNode){
        List<List<Integer>> shortestPath = new ArrayList<>();
        List<Integer> parent = parentMap.get(retrievedNode).node;
        shortestPath.add(Arrays.asList(retrievedNode.get(1),retrievedNode.get(0)));
        while(parent!=null){
            parent = parentMap.get(retrievedNode).node;
            if(parent!=null) shortestPath.add(0,Arrays.asList(parent.get(1),parent.get(0)));
            retrievedNode = parent;
        }
        return shortestPath;
    }

    private int heuristic(List<Integer> currentNode, List<Integer> targetNode, int[][] inputData){
        int sourceTargetX = 10*(targetNode.get(0)-currentNode.get(0));
        int sourceTargetY = 10*(targetNode.get(1)-currentNode.get(1));
        int zDifference =
                inputData[currentNode.get(0)][currentNode.get(1)]-inputData[targetNode.get(0)][targetNode.get(1)];
        double result = Math.sqrt(Math.pow(sourceTargetX,2)+Math.pow(sourceTargetY,2)+Math.pow(zDifference,2));
        return (int) Math.floor(result);
    }

    public List<List<Integer>> runBFS(int[][] inputData, List<Integer> source, List<Integer> target, int maxZ){
       if(source.equals(target)){
           List<List<Integer>> shortestPath = new ArrayList<>();
           shortestPath.add(Arrays.asList(source.get(1),source.get(0)));
           return shortestPath;
       }
       int rowSize = inputData.length;
       int columnSize = inputData[0].length;
       parentMap.put(Arrays.asList(source.get(0),source.get(1)),new UCSNodeCumulativeCost(null,0));
       Queue<List<Integer>> bfsQueue = new LinkedList<>();
       int sX = source.get(0), sY = source.get(1);
       while(true){
           for(int i=0;i<8;i++){ // add all children to queue
               if(sX+traverseX[i]>=0 && sY+traverseY[i]>=0 && sX+traverseX[i]<rowSize && sY+traverseY[i]<columnSize &&
                       Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY])<=maxZ) {
                   if (!parentMap.containsKey(Arrays.asList(sX + traverseX[i], sY + traverseY[i]))) {
                       bfsQueue.add(Arrays.asList(sX + traverseX[i], sY + traverseY[i]));
                       parentMap.putIfAbsent(Arrays.asList(sX + traverseX[i], sY + traverseY[i]),
                               new UCSNodeCumulativeCost(Arrays.asList(sX, sY),0));
                   }
               }
           }
           retrieved = bfsQueue.poll();
           if(retrieved==null) return new ArrayList<>();
           if(retrieved.get(0).equals(target.get(0)) && retrieved.get(1).equals(target.get(1))) break;
           else {
               sX=retrieved.get(0);
               sY=retrieved.get(1);
           }
       }
       return getShortestPath(parentMap,retrieved);
   }

    private List<List<Integer>> runUCS(int[][] inputData, List<Integer> source, List<Integer> target, int maxZ){
        //ucs -- update the distance if it still exists
        if(source.equals(target)){
            List<List<Integer>> shortestPath = new ArrayList<>();
            shortestPath.add(Arrays.asList(source.get(1),source.get(0)));
            return shortestPath;
        }
        int rowSize = inputData.length;
        int columnSize = inputData[0].length;
        parentMap.put(Arrays.asList(source.get(0),source.get(1)),new UCSNodeCumulativeCost(null,0));
        PriorityQueue<UCSNodeCumulativeCost> ucsQueue = new PriorityQueue<>(new UCSNodeCumulativeCostComparator());
        int sX = source.get(0), sY = source.get(1);
        UCSNodeCumulativeCost parentNodeCumulativeCost =
                new UCSNodeCumulativeCost(Arrays.asList(sX,sY),0);
        while(true){
            for(int i=0;i<8;i++){ // add all children to queue
                if(sX+traverseX[i]>=0 && sY+traverseY[i]>=0 && sX+traverseX[i]<rowSize && sY+traverseY[i]<columnSize &&
                        Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY])<=maxZ) {
                    int totalCost = parentNodeCumulativeCost.cumulativeCost+REGULAR_COST;
                    if(i>=4) totalCost+=4;
                    if (!parentMap.containsKey(Arrays.asList(sX + traverseX[i], sY + traverseY[i]))) {
                        ucsQueue.add(new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i],
                                sY + traverseY[i]),totalCost));
                        parentMap.putIfAbsent(Arrays.asList(sX + traverseX[i], sY + traverseY[i]),
                                new UCSNodeCumulativeCost(Arrays.asList(sX, sY),totalCost));
                   }
                   else{
                       if(totalCost<parentMap.get(Arrays.asList(sX + traverseX[i], sY + traverseY[i])).cumulativeCost){
                           parentMap.put(Arrays.asList(sX + traverseX[i], sY + traverseY[i]),
                                   new UCSNodeCumulativeCost(Arrays.asList(sX,sY),totalCost));
                           UCSNodeCumulativeCost existingNode = new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i]
                                   , sY + traverseY[i]),
                                   parentMap.get(Arrays.asList(sX + traverseX[i], sY + traverseY[i])).cumulativeCost);
                           ucsQueue.remove(existingNode);
                           existingNode.cumulativeCost=totalCost;
                           ucsQueue.add(existingNode);
                       }
                    }
                }
            }
            parentNodeCumulativeCost = ucsQueue.poll();
            if(parentNodeCumulativeCost==null) return new ArrayList<>();
            retrieved = parentNodeCumulativeCost.node;
            if(retrieved.get(0).equals(target.get(0)) && retrieved.get(1).equals(target.get(1))) break;
            else {
                sX=retrieved.get(0);
                sY=retrieved.get(1);
            }
        }
        System.out.println(parentNodeCumulativeCost.cumulativeCost);
        return getShortestPath(parentMap,retrieved);
    }

    private List<List<Integer>> runAStar(int[][] inputData, List<Integer> source, List<Integer> target, int maxZ){
        if(source.equals(target)){
            List<List<Integer>> shortestPath = new ArrayList<>();
            shortestPath.add(Arrays.asList(source.get(1),source.get(0)));
            return shortestPath;
        }
        int rowSize = inputData.length;
        int columnSize = inputData[0].length;
        parentMap.put(Arrays.asList(source.get(0),source.get(1)),new UCSNodeCumulativeCost(null,0));
        PriorityQueue<UCSNodeCumulativeCost> aStarQueue = new PriorityQueue<>(new UCSNodeCumulativeCostComparator());
        int sX = source.get(0), sY = source.get(1);
        UCSNodeCumulativeCost parentNodeCumulativeCost ;
        int totalCost;
        while(true){
            for(int i=0;i<8;i++){ // add all children to queue
                if(sX+traverseX[i]>=0 && sY+traverseY[i]>=0 && sX+traverseX[i]<rowSize && sY+traverseY[i]<columnSize &&
                        Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY])<=maxZ) {
                    int obtainedZ = Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY]);
                    int diagonalValue = REGULAR_COST;
                    if(i>=4) diagonalValue = DIAGONAL_COST;
                    if (!parentMap.containsKey(Arrays.asList(sX + traverseX[i], sY + traverseY[i]))) {
                        totalCost = parentMap.get(Arrays.asList(sX,sY)).cumulativeCost+diagonalValue+obtainedZ+
                                heuristic(Arrays.asList(sX+traverseX[i],sY+traverseY[i]), target, inputData);
                        /*totalCost=Math.max(totalCost,
                                heuristic(Arrays.asList(sX,sY),target,inputData)+parentMap.get(Arrays.asList(sX,sY))
                                .cumulativeCost);*/
                         parentMap.putIfAbsent(Arrays.asList(sX + traverseX[i], sY + traverseY[i]),
                                new UCSNodeCumulativeCost(Arrays.asList(sX, sY),
                                        parentMap.get(Arrays.asList(sX, sY)).cumulativeCost+diagonalValue+obtainedZ));
                         aStarQueue.add(new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i],
                                sY + traverseY[i]),totalCost));
                    }
                    else{
                        if(parentMap.get(Arrays.asList(sX,sY)).cumulativeCost+diagonalValue+obtainedZ
                                <parentMap.get(Arrays.asList(sX+traverseX[i], sY+traverseY[i])).cumulativeCost){
                            parentMap.put(Arrays.asList(sX + traverseX[i], sY + traverseY[i]),
                                    new UCSNodeCumulativeCost(Arrays.asList(sX,sY), parentMap.
                                            get(Arrays.asList(sX, sY)).cumulativeCost+diagonalValue+obtainedZ));
                            int heuristicCurrentNode = heuristic(Arrays.asList(sX+traverseX[i],
                                    sY+traverseY[i]), target, inputData);
                            totalCost =
                                    parentMap.get(Arrays.asList(sX+traverseX[i], sY+traverseY[i])).cumulativeCost+heuristicCurrentNode;
                            UCSNodeCumulativeCost existingNode =
                                    new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i], sY + traverseY[i]), totalCost);
                            aStarQueue.remove(existingNode);
                            totalCost =
                                    parentMap.get(Arrays.asList(sX,sY)).cumulativeCost+diagonalValue+obtainedZ+heuristicCurrentNode;
                           /* totalCost=Math.max(totalCost,
                                    heuristic(Arrays.asList(sX,sY),target,inputData)+parentMap.get(Arrays.asList(sX,
                                    sY)).cumulativeCost);*/
                            existingNode.cumulativeCost=totalCost;
                            aStarQueue.add(existingNode);
                        }
                    }
                }
            }
            parentNodeCumulativeCost = aStarQueue.poll();
            if(parentNodeCumulativeCost==null) return new ArrayList<>();
            retrieved = parentNodeCumulativeCost.node;
            if(retrieved.get(0).equals(target.get(0)) && retrieved.get(1).equals(target.get(1))) break;
            else {
                sX=retrieved.get(0);
                sY=retrieved.get(1);
            }
        }
        System.out.println(parentMap.get(retrieved).cumulativeCost);
        return getShortestPath(parentMap,retrieved);
        }


   private byte[] formatOutput(String shortestPath){
        return shortestPath.replace("[","").replace("],"," ").
                replace(", ",",").replace("]]","").replace("  "," ").getBytes();
   }

    public static void main(String[] args) throws IOException{
        MarsRover marsRover = new MarsRover();
        String fileName = System.getProperty("user.dir")+marsRover.INPUT_TXT;
        List<String> inputList = Files.readAllLines(Paths.get(fileName));
        int[][] zfactorArray = marsRover.getZfactorArray(inputList);
        List<Integer> sourceNodes = marsRover.getSourceNode(inputList);
        List<List<Integer>> targetNodes = marsRover.getTargetNodes(inputList);
        int maxZfactor = Integer.parseInt(inputList.get(3));
        Path outputPath = Paths.get(System.getProperty("user.dir")+marsRover.OUTPUT_TXT);
        String resultSet = "";
        List<List<Integer>> shortestPath = null;
        String algorithm = marsRover.parseInputTxt(inputList);
        inputList.clear();
        for (int i = 0; i < targetNodes.size(); i++) {
            switch (algorithm){
                case "BFS":
                    shortestPath = marsRover.runBFS(zfactorArray, sourceNodes,
                            targetNodes.get(i), maxZfactor);
                    break;
                case "UCS":
                    shortestPath = marsRover.runUCS(zfactorArray, sourceNodes,
                            targetNodes.get(i), maxZfactor);
                    break;
                case "A*":
                    shortestPath = marsRover.runAStar(zfactorArray, sourceNodes,
                            targetNodes.get(i), maxZfactor);
                    break;
                default:
                    Files.write(outputPath, marsRover.FAILURE_MSG.getBytes());
            }
            if (i != targetNodes.size() - 1) {
                if (shortestPath.isEmpty()) resultSet = resultSet.concat(marsRover.FAILURE_MSG + "\n");
                else resultSet = resultSet.concat(shortestPath.toString() + "\n");
            } else {
                if (shortestPath.isEmpty()) resultSet = resultSet.concat(marsRover.FAILURE_MSG);
                else resultSet = resultSet.concat(shortestPath.toString());
            }
            marsRover.parentMap.clear();
        }
        Files.write(outputPath,marsRover.formatOutput(resultSet));
    }
}

