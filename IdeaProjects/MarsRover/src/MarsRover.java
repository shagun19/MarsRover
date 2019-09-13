import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            return uscNodeCumulativeCost1.cumulativeCost;
        else if (uscNodeCumulativeCost1.cumulativeCost > uscNodeCumulativeCost2.cumulativeCost)
            return uscNodeCumulativeCost2.cumulativeCost;
        return 0;
    }
}

public class MarsRover {

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
        for(int i = zfactorValuesStartLine; i<inputStringList.size(); i++){
            for(int j=0; j<Integer.parseInt(inputStringList.get(1).split("\\s+")[0]); j++){
                zfactorArray[i-zfactorValuesStartLine][j] = Integer.parseInt(inputStringList.get(i).split("\\s+")[j]);
            }
        }
        return zfactorArray;
    }

    private List<List<Integer>> runBFS(int[][] inputData, List<Integer> source, List<Integer> target, int maxZ){
       int rowSize = inputData.length;
       int columnSize = inputData[0].length;
       int[] traverseX = {1,1,1,0,-1,-1,-1,0};
       int[] traverseY = {1,0,-1,-1,-1,0,1,1};
       List<Integer> retrieved  = new ArrayList<>();
       List<List<Integer>> outputSequence = new ArrayList<>();
       Map<List<Integer>,List<Integer>> parentMap = new HashMap<>();
       outputSequence.add(Arrays.asList(source.get(0),source.get(1)));
       parentMap.put(Arrays.asList(source.get(0),source.get(1)),null);
       Queue<List<Integer>> bfsQueue = new LinkedList<>();
       int sX = source.get(0), sY = source.get(1);
       while(true){
           for(int i=0;i<8;i++){ // add all children to queue
               if(sX+traverseX[i]>=0 && sY+traverseY[i]>=0 && sX+traverseX[i]<rowSize && sY+traverseY[i]<columnSize &&
                       Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY])<=maxZ) {
                   if (!parentMap.containsKey(Arrays.asList(sX + traverseX[i], sY + traverseY[i]))) {
                       bfsQueue.add(Arrays.asList(sX + traverseX[i], sY + traverseY[i]));
                       parentMap.putIfAbsent(Arrays.asList(sX + traverseX[i], sY + traverseY[i]), Arrays.asList(sX, sY));
                   }
               }
           }
           retrieved = bfsQueue.poll();
           if(retrieved==null) return new ArrayList<>();
           outputSequence.add(retrieved);
           if(retrieved.get(0).equals(target.get(0)) && retrieved.get(1).equals(target.get(1))) break;
           else {
               sX=retrieved.get(0);
               sY=retrieved.get(1);
           }
       }
       List<List<Integer>> shortestPath = new ArrayList<>();
       List<Integer> parent = parentMap.get(retrieved);
       shortestPath.add(Arrays.asList(retrieved.get(1),retrieved.get(0)));
       while(parent!=null){
           parent = parentMap.get(retrieved);
           if(parent!=null) shortestPath.add(0,Arrays.asList(parent.get(1),parent.get(0)));
           retrieved = parent;
       }
       return shortestPath;
   }

    private List<List<Integer>> runUCS(int[][] inputData, List<Integer> source, List<Integer> target, int maxZ){
        int rowSize = inputData.length;
        int columnSize = inputData[0].length;
        int[] traverseX = {1,0,-1,0,1,1,-1,-1};
        int[] traverseY = {0,-1,0,1,1,-1,1,-1};
        List<Integer> retrieved  = new ArrayList<>();
        List<List<Integer>> outputSequence = new ArrayList<>();
        Map<List<Integer>,List<Integer>> parentMap = new HashMap<>();
        outputSequence.add(Arrays.asList(source.get(0),source.get(1)));
        parentMap.put(Arrays.asList(source.get(0),source.get(1)),null);
        PriorityQueue<UCSNodeCumulativeCost> ucsQueue = new PriorityQueue<>(new UCSNodeCumulativeCostComparator());
        int sX = source.get(0), sY = source.get(1);
        UCSNodeCumulativeCost parentNodeCumulativeCost =
                new UCSNodeCumulativeCost(Arrays.asList(sX,sY),0);
        while(true){
            for(int i=0;i<8;i++){ // add all children to queue
                if(sX+traverseX[i]>=0 && sY+traverseY[i]>=0 && sX+traverseX[i]<rowSize && sY+traverseY[i]<columnSize &&
                        Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY])<=maxZ) {
                    if (!parentMap.containsKey(Arrays.asList(sX + traverseX[i], sY + traverseY[i]))) {
                        int obtainedZ = Math.abs(inputData[sX+traverseX[i]][sY+traverseY[i]]-inputData[sX][sY]);
                        if(i>=4) ucsQueue.add(new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i],
                                sY + traverseY[i]),parentNodeCumulativeCost.cumulativeCost+14+obtainedZ));
                        else ucsQueue.add(new UCSNodeCumulativeCost(Arrays.asList(sX + traverseX[i],
                                sY + traverseY[i]),parentNodeCumulativeCost.cumulativeCost+10+obtainedZ));
                        parentMap.putIfAbsent(Arrays.asList(sX + traverseX[i], sY + traverseY[i]), Arrays.asList(sX, sY));
                    }
                }
            }
            PriorityQueue<UCSNodeCumulativeCost> check = new PriorityQueue<>(ucsQueue);
            parentNodeCumulativeCost = ucsQueue.poll();
            retrieved = parentNodeCumulativeCost.node;
            if(retrieved==null) return new ArrayList<>();
            outputSequence.add(retrieved);
            if(retrieved.get(0).equals(target.get(0)) && retrieved.get(1).equals(target.get(1))) break;
            else {
                sX=retrieved.get(0);
                sY=retrieved.get(1);
            }
        }
        List<List<Integer>> shortestPath = new ArrayList<>();
        List<Integer> parent = parentMap.get(retrieved);
        shortestPath.add(Arrays.asList(retrieved.get(1),retrieved.get(0)));
        while(parent!=null){
            parent = parentMap.get(retrieved);
            if(parent!=null) shortestPath.add(0,Arrays.asList(parent.get(1),parent.get(0)));
            retrieved = parent;
        }
        return shortestPath;
    }

   private byte[] formatOutput(String shortestPath){
        return shortestPath.replace("[","").replace("],"," ").
                replace(", ",",").replace("]]","").getBytes();
   }

    public static void main(String[] args) throws IOException{
        MarsRover marsRover = new MarsRover();
        String fileName = System.getProperty("user.dir")+"/src/input.txt";
        List<String> inputList = Files.readAllLines(Paths.get(fileName));
        int[][] zfactorArray = marsRover.getZfactorArray(inputList);
        List<Integer> sourceNodes = marsRover.getSourceNode(inputList);
        List<List<Integer>> targetNodes = marsRover.getTargetNodes(inputList);
        int maxZfactor = Integer.parseInt(inputList.get(3));
        Path outputPath = Paths.get(System.getProperty("user.dir")+"/src/output.txt");
        String resultSet = "";
        List<List<Integer>> shortestPath = new ArrayList<>();
        if(marsRover.parseInputTxt(inputList).equals("BFS")){
            for(int i=0;i<targetNodes.size();i++){
                shortestPath = marsRover.runBFS(zfactorArray,sourceNodes,
                        targetNodes.get(i),maxZfactor);
                if(i!=targetNodes.size()-1){
                    if(shortestPath.isEmpty()) resultSet=resultSet.concat("FAIL\n");
                    else resultSet=resultSet.concat(shortestPath.toString()+"\n") ;
                }
                else{
                    if(shortestPath.isEmpty()) resultSet=resultSet.concat("FAIL");
                    else resultSet=resultSet.concat(shortestPath.toString()) ;
                }
            }
        }
        if(marsRover.parseInputTxt(inputList).equals("UCS")){
            for(int i=0;i<targetNodes.size();i++){
                shortestPath = marsRover.runUCS(zfactorArray,sourceNodes,
                        targetNodes.get(i),maxZfactor);
                if(i!=targetNodes.size()-1){
                    if(shortestPath.isEmpty()) resultSet=resultSet.concat("FAIL\n");
                    else resultSet=resultSet.concat(shortestPath.toString()+"\n") ;
                }
                else{
                    if(shortestPath.isEmpty()) resultSet=resultSet.concat("FAIL");
                    else resultSet=resultSet.concat(shortestPath.toString()) ;
                }
            }
        }
        Files.write(outputPath,marsRover.formatOutput(resultSet));
    }
}

