package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;

import parsing.Column;
import parsing.Node;

public class ProjectedNodesData {
    ArrayList<Node> projectedNodes ;
    int noOfProjectedColumns ;
    Vector<Column> aggregateColumnsFromRefTables = new Vector<Column>();
    Vector<Column> aggregateColumnsFromJSQ = new Vector<Column>();
    Vector<Integer> indexOfProjectedColmnsInSQ = new Vector<Integer>();

    public ProjectedNodesData(){
        
    }
    public  ProjectedNodesData(ArrayList<Node> projectedNodes){
        this.projectedNodes = projectedNodes ;
    }
    public void putProjectedNodes(ArrayList<Node> projectedNodes){
        this.projectedNodes = projectedNodes;
    }
    public ArrayList<Node> getProjectedNodes(){
        return this.projectedNodes ;
    }
}
