package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;

import parsing.Column;
import parsing.Node;

public class GroupByNodesData {
    ArrayList<Node> groupByNodes ;
    Vector<Column> groupByColumnsFromRefTables = new Vector<Column>() ;
    int noOfGroupByColumns ;
    Vector<Column> groupByColumnsFromJSQ = new Vector<Column>();

    public  GroupByNodesData(ArrayList<Node> groupByNodes){
        
        // this.groupByNodes = groupByNodes ;
        ArrayList<Node> newgbNodes = new ArrayList<Node>();

        for(int i=groupByNodes.size()-1; i>=0; i--){
            Node n1 = groupByNodes.get(i);
            int flag = 1;
            for(int j=i-1; j>=0; j--){
                Node n2 = groupByNodes.get(j);
                if(n1.getTable()!=null && n2.getTable()!=null && n1.getTable().getTableName().equalsIgnoreCase(n2.getTable().getTableName())){
                    if(n1.getColumn().getColumnName().equalsIgnoreCase(n2.getColumn().getColumnName())){
                        flag = 0;
                    }
                }
            }
            if(flag == 1)
            {
                newgbNodes.add(n1);
            }
        }
        this.groupByNodes = newgbNodes;
    }

    public ArrayList<Node> getGroupByNodes(){
        return this.groupByNodes ;
    }

    public void putGroupByNodes(ArrayList<Node> groupByNodes){
        this.groupByNodes = groupByNodes;
    }

}
