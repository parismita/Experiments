package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.microsoft.z3.Sort;
import parsing.Column;

import parsing.Node;

public class AggregateDataStructure {
    // table names and array names
    String subqueryTableName ; 
    String baseTableName ;  
    String arrayNameOfBQInCtx ;
    String arrayNameOfSQInCtx ;

    // columns of agg subquery table
    Vector<String> attrColumnsOfSQ = new Vector<String>();
    // Vector<String> attrNamesOfSQ =  new Vector<String>();
    Vector<Sort> attrTypesOfSQ = new Vector<Sort>();

    // related to column data
    Vector<String>  primaryKeyColumnsofSQ = new Vector<String>(); // listOfPrimaryKeyOfSQTableNames ; 
    HashMap<String, Boolean> isColumnDistinctInSQ = new HashMap<String, Boolean>();
    HashMap<String, Integer> columnToIndexMapInSQ = new HashMap<String, Integer>();
    Column countColOfSQ;


    // columns of base table (JSQ)
    Vector<String> attrNamesBQ = new Vector<String>();
	Vector<Sort> attrTypesBQ = new Vector<Sort>();
    ArrayList<String> attrNamesOfBQ = new ArrayList<String>(); // is this required? duplicate with attrNamesBQ

    ArrayList<Node> aggConstraints  ;
    Boolean isConstrainedAgg ;
   
    public AggregateDataStructure(String subqueryTableName){
        this.subqueryTableName = subqueryTableName;
        this.arrayNameOfSQInCtx = "O_" +  subqueryTableName;
    }
    public void setArrayNameOfSQ(String arrName){
        this.arrayNameOfSQInCtx = arrName ;
    }
    public String getArrayNameOfSQ(){
        return this.arrayNameOfSQInCtx ;
    }
    public String getTableNameOfSQ(){
        return this.subqueryTableName ;
    }
    public void setCountColumnOfSQ(Column col){
        this.countColOfSQ = col;
    }
}
