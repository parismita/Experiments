package testDataGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.Node;

public class SubqueryStructure {
   
    public HashMap<String,String > SQDefine;  //key: tempJoin_table_name, value: datatype declaration of tempJoin table 
	public HashMap<String,ArrayList<String> > SQColumns; //stores columns in tempJoinTble
	public HashMap<String,ArrayList<String> > SQColumnsDataTypes; //stores columns datatypes in tempJoinTble
	public HashMap<String,Vector<Node> > SQJoinSelectionAndJoinConds; //stores selection conditions from subquery
	public HashMap<String,Vector<Node> > SQJoinCorrelationConds; //stores correlation conditions from subquery
	public HashMap<String,Boolean> SQjoinWithEXISTS;
	public HashMap<String,ArrayList<String> > SQTableName;

    public SubqueryStructure(){
        SQDefine = new HashMap<String,String >();
        SQColumns = new HashMap<String,ArrayList<String> >();
        SQColumnsDataTypes = new  HashMap<String,ArrayList<String> >();
        SQJoinSelectionAndJoinConds = new HashMap<String,Vector<Node> >();
        SQJoinCorrelationConds = new  HashMap<String,Vector<Node> >();
        SQjoinWithEXISTS = new HashMap<String,Boolean>();
        SQTableName = new HashMap<String,ArrayList<String> >();
    }
}
