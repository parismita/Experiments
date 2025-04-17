
package testDataGen;

import generateConstraints.GetSolverHeaderAndFooter;
import generateConstraints.TupleRange;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.z3.FuncDecl;

import GenConstraints.GenConstraints;
import killMutations.GenerateDataForOriginalQuery;
import killMutations.MutationsInFromSubQuery;
import killMutations.MutationsInOuterBlock;
import killMutations.MutationsInWhereSubQuery;
import parsing.AppTest_Parameters;
import parsing.CaseCondition;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.ForeignKey;
import parsing.FromClauseElement;
import parsing.Node;
import parsing.Query;
import parsing.QueryParser;
import parsing.QueryStructure;
import parsing.QueryStructureForDataGen;
import parsing.Table;
import parsing.correlationStructure;
import parsing.MutationStructure;
import stringSolver.StringConstraintSolver;
import util.Configuration;
import util.TableMap;
import util.TagDatasets;
import util.TagDatasets.MutationType;
import util.TagDatasets.QueryBlock;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CaseStatement;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomCondition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.OrderObject.Dir;
import com.healthmarketscience.sqlbuilder.SelectQuery.JoinType;
import com.healthmarketscience.sqlbuilder.custom.mysql.MysLimitClause;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSchema;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbSpec;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbTable;


public class GenerateCVC1 implements Serializable{
	/**FIXME: appConstraints, NonEmptyConstraints...whether they are common for query or for each block of query*/

	/**
	 * 
	 */
	public int regNo;
	private static Logger logger = Logger.getLogger(GenerateCVC1.class.getName());
	private static final long serialVersionUID = 2996426044633967059L;
	/** Details about the the tables in the input database */
	private TableMap tableMap;
	private Query query; 
	public Query queryL;
	public Query queryR;
	public String setOpJoin;
	public String rawQuery;
	public int isAll;
	public HashMap<String, Vector<Column> > forMap;
	public HashMap<String,HashMap<String, Object>> jtObj;
	private Query topQuery;
	public int setOpNo;
	public QueryStructure qStructureL;
	public String queryStringL;
	public HashMap<String, Integer> currentIndexCountL;
	public HashMap<String, Integer> repeatedRelationCountL;
	public HashMap<String, String> baseRelationL;
	public QueryBlockDetails outerBlockL;
	public QueryStructure qStructureR;
	public String queryStringR;
	public HashMap<String, Integer> repeatedRelationCountR;
	public HashMap<String, Integer> currentIndexCountR;
	public HashMap<String, String> baseRelationR;
	public QueryBlockDetails outerBlockR;



	/** The parser stores the details of the query after the input query is parsed	 */
	private QueryParser qParser;
	//Application Testing
	private AppTest_Parameters DBAppparams;
	

	public AppTest_Parameters getDBAppparams() {
		return DBAppparams;
	}


	public void setDBAppparams(AppTest_Parameters dBAppparams) {
		DBAppparams = dBAppparams;
	}

	//end
	private QueryStructure qStructure;
	/** Stores the base relation for each repeated occurrence of a relation  */

	private HashMap<String,String> baseRelation; 
	private HashMap<String,String> baseSQRelation; 

	/** Maintains the increment for each repeated occurrence of a relation. For instance if R repeats twice as R1 and R2 
	 * then the currentIndexCount of R is 0, R1 is 1 and that of R2 is 2.  */
	private  HashMap<String,Integer> currentIndexCount; 

	/**  For each relation maintains a count of how many times it repeats. Indies of a relation should be incremented by this number */
	private HashMap<String,Integer> repeatedRelationCount;

	/** Stores the positions at which the indexes for each repeated relations start */
	private HashMap<String, Integer[]> repeatedRelNextTuplePos;

	/** Stores details about the number of tuples for each occurrence of the relation in the input query */
	private HashMap<String, Integer> noOfTuples;

	/** Stores the no of tuples to be generated for each relation */
	private HashMap<String,Integer> noOfOutputTuples;
	public HashMap<String,Integer> noOfMaxOutputTuples;

	/** Stores details about the outer block of the query */
	public QueryBlockDetails outerBlock;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<Node> foreignKeys;

	/** Stores details about the foreign keys of the tables of the query*/
	private ArrayList<ForeignKey> foreignKeysModified;

	/** Stores the list of constraints for this data generation step*/
	private ArrayList<String> constraints;

	/** Stores the list of string constraints for this data generation step*/
	private ArrayList<String> stringConstraints;

	private String CVCStr;

	private String constraintSolver;
	
	private HashMap<Table,Vector<String> > resultsetTableColumns1;

	private Vector<Column> resultsetColumns;

	/** Keeps track of which columns have which null values and which of them have been used*/
	private HashMap<Column, HashMap<String, Integer>> colNullValuesMap;	

	/** Reference to string solver */
	private StringConstraintSolver stringSolver;

	/**FORALL/ NOT EXISTS:	fne = true/false*/
	private boolean fne;		

	/**FIXME: Why this??*/
	private ArrayList<String> datatypeColumns;

	private String output;

	/** Used to store the English description of the query */
	private String queryString;

	/** Sets the path to the location where the file containing queries is located and where the output files will be generated */
	private String filePath;
	
	/** Holds the name of the schema file */
	private String schemaFile;
	
	/** Holds the name of the data file */
	private String dataFile;

	/** Used to number the data sets */
	private int count;
	private int tuplecount;

	private String solverSpecificCommentCharacter;
	
	private ArrayList<Table> resultsetTables;

	/** I/P DATABASE: ipdb = true/false*/
	private boolean ipdb;   		

	/**Stores CVC3 Header*/
	private String CVC3_HEADER;

	/**Stores SMTLIB Header**/
	private String SMTLIB_HEADER;
	/**stores details about the query, if query consists of set operations*/
	private GenerateUnionCVC unionCVC;


	/**details about branch queries of the input if any*/
	private BranchQueriesDetails branchQueries; 

	private Vector<Table> tablesOfOriginalQuery;

	/**Indicates the type of mutation we are trying to kill*/
	private String typeOfMutation;

	/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
	private HashMap<String, Integer[]> tableNames;

	/** Contains the list of equi join condition for each table in the given query. Used during foreign key constraints*/
	private HashMap<String, Vector<Vector<Node>> > equiJoins; 
	
	
	
	// Assignment Id
	private int assignmentId;
	private int questionId;
	private int queryId;
	private String courseId; 
	private String concatenatedQueryId;
	// Holds true if result is order Independent of projection columns
	private boolean orderindependent = true;
	
	private transient Connection connection;
	
	private Map<String, TupleRange> allowedTuples;

	//****************** TEST CODE: For merging all subquery strcuture*************
	public HashMap<String, SubqueryStructure> subqueryConstraintsMap ;

	
	//****************** TEST CODE: POOJA ***********************//
	// public HashMap<String,String > tempJoinDefine;  //key: tempJoin_table_name, value: datatype declaration of tempJoin table 
	// public HashMap<String,ArrayList<String> > tempJoinColumns; //stores columns in tempJoinTble
	// public HashMap<String,ArrayList<String> > tempJoinColumnsDataTypes; //stores columns datatypes in tempJoinTble
	// public HashMap<String,Vector<Node> > tempJoinSelectionAndJoinConds; //stores selection conditions from subquery
	// public HashMap<String,Vector<Node> > tempJoinCorrelationConds; //stores correlation conditions from subquery
	// public HashMap<String,Boolean> tempjoinWithEXISTS;
	// public HashMap<String,ArrayList<String> > tempJoinTableName;
	// //****************** TEST CODE END **************************//
	
	// //******************Test Code : Sunanda *********************//
	// public HashMap<String,String > fromTempJoinDefine;  //key: tempJoin_table_name, value: datatype declaration of tempJoin table 
	// public HashMap<String,ArrayList<String> > fromTempJoinColumns; //stores columns in tempJoinTble
	// public HashMap<String,ArrayList<String> > fromTempJoinColumnsDataTypes; //stores columns datatypes in tempJoinTble
	// public HashMap<String,Vector<Node> > fromTempJoinSelectionAndJoinConds; //stores selection conditions from subquery
	// public HashMap<String,Vector<Node> > fromTempJoinCorrelationConds; //stores correlation conditions from subquery
	// public HashMap<String,Boolean> tempjoinWithFROM;
	// public HashMap<String,ArrayList<String> > fromTempJoinTableName ;
	
	// //*****************Test Code for Having clause *******************
	// public HashMap<String,String > havingTempJoinDefine;  //key: tempJoin_table_name, value: datatype declaration of tempJoin table 
	// public HashMap<String,ArrayList<String> > havingTempJoinColumns; //stores columns in tempJoinTble
	// public HashMap<String,ArrayList<String> > havingTempJoinColumnsDataTypes; //stores columns datatypes in tempJoinTble
	// public HashMap<String,Vector<Node> > havingTempJoinSelectionAndJoinConds; //stores selection conditions from subquery
	// public HashMap<String,Vector<Node> > havingTempJoinCorrelationConds; //stores correlation conditions from subquery
	// public HashMap<String,Boolean> tempjoinWithHaving;
	// public HashMap<String,ArrayList<String> > havingTempJoinTableName ;
		
	//******************Test Code : end *********************//
	
	public static HashMap<String, FuncDecl> ctxFuncDecls;  // for storing Z3 context function declarations
	//**************** Test structure for aliasing */
	public  HashMap<String, levelStrcuture>	aliasMappingToLevels  ;
	//**************** Test structure for correlation */	
	public  HashMap<Node, correlationStructure> correlationHashMap ;
	public  HashMap<Integer, String> levelToQueryTypeHashMap ;
	public  HashMap<Integer, Integer> verticalLevelMap ;

	// Enumerated array index name - Sunanda
	public String enumArrayIndex ;
	public Vector<String>enumArrayIndexData ;
	public String enumIndexVar ;
	public String sampleDataJsonFilePath ;

	// Current Mutant
	public MutationStructure currentMutant ; 
	/** The constructor for this class */
	public GenerateCVC1 (){
		regNo=0;
		setOpJoin="";
		isAll=0;
		rawQuery = "";
		setOpNo = 0; 
		baseRelation = new HashMap<String, String>();
		baseSQRelation = new HashMap<String, String>();
		currentIndexCount = new HashMap<String, Integer>();
		repeatedRelationCount = new HashMap<String, Integer>();
		repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
		noOfTuples = new HashMap<String, Integer>();
		noOfOutputTuples = new HashMap<String, Integer>();
		noOfMaxOutputTuples = new HashMap<String, Integer>();
		outerBlock = new QueryBlockDetails();
		colNullValuesMap = new HashMap<Column, HashMap<String,Integer>>();
		datatypeColumns = new ArrayList<String>();
		resultsetColumns = new Vector<Column>();
		resultsetTableColumns1 = new HashMap<Table, Vector<String>>();
		resultsetTables = new ArrayList<Table>();
		stringSolver = new StringConstraintSolver();
		branchQueries = new BranchQueriesDetails();
		tableNames = new HashMap<String, Integer[]>();
		equiJoins = new HashMap<String, Vector<Vector<Node>>>();
		allowedTuples = new HashMap<String, TupleRange>();
		DBAppparams = new AppTest_Parameters();
		subqueryConstraintsMap = new HashMap<String, SubqueryStructure>();
		verticalLevelMap = new HashMap<Integer, Integer>();
		forMap = new HashMap<String,Vector<Column>>();
		jtObj = new HashMap<String,HashMap<String, Object>>();


		
		//**************** TEST CODE: POOJA *******************//
		// tempJoinDefine = new HashMap<String,String>();
		// tempJoinColumns = new HashMap<String,ArrayList<String>>();
		// tempJoinColumnsDataTypes = new HashMap<String,ArrayList<String>>();
		// tempJoinSelectionAndJoinConds = new HashMap<String,Vector<Node>>();
		// tempJoinCorrelationConds = new HashMap<String,Vector<Node>>();
		// tempjoinWithEXISTS = new HashMap<String,Boolean>();
		// tempJoinTableName = new HashMap<String,ArrayList<String>>();
		

		// fromTempJoinDefine = new HashMap<String,String>();
		// fromTempJoinColumns = new HashMap<String,ArrayList<String>>();
		// fromTempJoinColumnsDataTypes = new HashMap<String,ArrayList<String>>();
		// fromTempJoinSelectionAndJoinConds = new HashMap<String,Vector<Node>>();
		// fromTempJoinCorrelationConds = new HashMap<String,Vector<Node>>();
		// tempjoinWithFROM = new HashMap<String,Boolean>();
		// fromTempJoinTableName = new HashMap<String,ArrayList<String>>(); // need change sunanda

		//*****************Test Code for Having clause starts *******************
		
		// havingTempJoinDefine = new HashMap<String,String>();
		// havingTempJoinColumns = new HashMap<String,ArrayList<String>>();
		// havingTempJoinColumnsDataTypes = new HashMap<String,ArrayList<String>>();
		// havingTempJoinSelectionAndJoinConds = new HashMap<String,Vector<Node>>();
		// havingTempJoinCorrelationConds = new HashMap<String,Vector<Node>>();
		// tempjoinWithHaving = new HashMap<String,Boolean>();
		// havingTempJoinTableName = new HashMap<String,ArrayList<String>>(); // need change sunanda
		// Enumerated array index name set - Sunanda
		if(Configuration.isEnumInt.equalsIgnoreCase("true"))
			{enumArrayIndex = Configuration.getProperty("enumArrayIndex");
			enumIndexVar = "v";}
		else 
			{enumArrayIndex = "Int";
			enumIndexVar = "";}
		enumArrayIndexData = new Vector<String>();
		sampleDataJsonFilePath = "";

		//*****************Test Code for Having clause ends *******************
		
		ctxFuncDecls = new HashMap<String, FuncDecl>();
		
		//**************** TEST CODE END **********************//

		//**************** Test code for table aliasing
		aliasMappingToLevels = new HashMap<String, levelStrcuture>();

		//**************** Test structure for correlation */
		correlationHashMap = new HashMap<Node, correlationStructure> ();
		levelToQueryTypeHashMap = new HashMap<Integer, String>();
	}

	public void setEnumIntData(int numberOfTuples, String varName){
		if(numberOfTuples == 0)
			this.enumArrayIndexData.add(varName+1);
		if (this.enumArrayIndexData.size() < numberOfTuples)	
			for(int i=this.enumArrayIndexData.size()+1; i<=numberOfTuples; i++)
				this.enumArrayIndexData.add(varName+i);
	}

	public void closeConn() {
		try{
			this.connection.close();
		}catch(SQLException e){};
	}

	public void inititalizeSQDataset() throws Exception{
		// tempJoinDefine = new HashMap<String,String>();
		// tempJoinColumns = new HashMap<String,ArrayList<String>>();
		// tempJoinSelectionAndJoinConds = new HashMap<String,Vector<Node>>();
		// tempJoinCorrelationConds = new HashMap<String,Vector<Node>>();
		// tempjoinWithEXISTS = new HashMap<String,Boolean>();
		// tempJoinTableName = new HashMap<String,ArrayList<String>>();
		
		// havingTempJoinDefine = new HashMap<String,String>();
		// havingTempJoinColumns = new HashMap<String,ArrayList<String>>();
		// havingTempJoinColumnsDataTypes = new HashMap<String,ArrayList<String>>();
		// havingTempJoinSelectionAndJoinConds = new HashMap<String,Vector<Node>>();
		// havingTempJoinCorrelationConds = new HashMap<String,Vector<Node>>();
		// tempjoinWithHaving = new HashMap<String,Boolean>();
		// havingTempJoinTableName = new HashMap<String,ArrayList<String>>();

		subqueryConstraintsMap = new HashMap<String, SubqueryStructure>();
		return;
	}

	/** 
	 * This method initializes all the details about the given query whose details are stored in the query Parser
	 * @param qParser
	 */
	/*public void initializeQueryDetails (QueryParser queryParser) throws Exception{
		try{
			qParser = queryParser;
			this.setFne(false);
			query = qParser.getQuery();
			queryString = query.getQueryString();
			//currentIndex = query.getCurrentIndex();
			baseRelation = query.getBaseRelation();
			currentIndexCount = query.getCurrentIndexCount();
			repeatedRelationCount = query.getRepeatedRelationCount();
	
			//Initialize the foreign key details
			foreignKeys = new ArrayList<Node>( qParser.getForeignKeys());
			foreignKeysModified = new ArrayList<ForeignKey>( qParser.getForeignKeyVectorModified());		
	
			// Initiliaze the outer query block
			outerBlock = QueryBlockDetails.intializeQueryBlockDetails(queryParser);
				
			//It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]
			tableNames = qParser.getTableNames();
	
			//Initialize each from clause nested sub query blocks 
			for(QueryParser qp: qParser.getFromClauseSubqueries())
				outerBlock.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
	
			// Initialize the where clause nested sub query blocks 
			for(QueryParser qp: qParser.getWhereClauseSubqueries())
				outerBlock.getWhereClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(), e);
			//e.printStackTrace();
			throw e;
		}
	}
*/

	/**
	 * Initializes the elements necessary for data generation
	 * Call this function after the previous data generation has been done and 
	 * constraints for the current data generation have not been added
	 */
	public void inititalizeForDataset() throws Exception{
		constraints = new ArrayList<String>();
		stringConstraints = new ArrayList<String>();
		CVCStr = "";
		//typeOfMutation = "";
		
		try{
			
			/** Add additional groupBy attributes if the relation of groupby attributes references to any other relation**/
			// Make the referenced relation a join with existing relation
			//Update no of output tuples, no of groups, repeated relation count, table occurrences to add additional
			//datasets
			
			
			/** initialize the no of output tuples*/
			noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
	
			/**Merging noOfOutputTuples, if input query has set operations*/
			if(qParser != null && qParser.setOperator!=null && qParser.setOperator.length()>0){  
	
				/**Initialize the number of tuples in left side query of the set operation*/
				noOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCleft().query.getRepeatedRelationCount().clone();
	
				/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
				HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCright().query.getRepeatedRelationCount().clone();
	
				/**get iterator*/
				Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
	
				/**while there are values in the hash map*/
				while(rt.hasNext()){
					Map.Entry pairs=(Entry) rt.next();
	
					/**get table name*/
					String table=(String) pairs.getKey();
				
					/**get the number of tuples*/
					int noOfTuples = (Integer) pairs.getValue();
	
					/**Update the data structure*/
					if(noOfOutputTuplesContains(table) && getNoOfOutputTuples(table)<noOfTuples){
						putNoOfOutputTuples(table, noOfTuples);
					}
					if(!noOfOutputTuplesContains(table)){
						putNoOfOutputTuples(table, noOfTuples);
					}
				}
			}else if(qParser != null && !qParser.getFromClauseSubqueries().isEmpty()
					&& qParser.getFromClauseSubqueries().get(0) != null){
				QueryParser qp = new QueryParser(qParser.getTableMap());
				qp = qParser.getFromClauseSubqueries().get(0);
				
				if(qp.setOperator!=null && qp.setOperator.length()>0){
					
					/**Initialize the number of tuples in left side query of the set operation*/
					noOfOutputTuples = (HashMap<String,Integer>)qp.getLeftQuery().getQuery().getRepeatedRelationCount().clone();
		
					/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
					HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)qp.getRightQuery().getQuery().getRepeatedRelationCount().clone();
		
					/**get iterator*/
					Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
		
					/**while there are values in the hash map*/
					while(rt.hasNext()){
						Map.Entry pairs=(Entry) rt.next();
		
						/**get table name*/
						String table=(String) pairs.getKey();
		
						/**get the number of tuples*/
						int noOfTuples = (Integer) pairs.getValue();
		
						/**Update the data structure*/
						if(noOfOutputTuplesContains(table) && getNoOfOutputTuples(table)<noOfTuples){
							putNoOfOutputTuples(table, noOfTuples);
						}
						if(!noOfOutputTuplesContains(table)){
							putNoOfOutputTuples(table, noOfTuples);
						}
					}
					
				}
			}
			
			else{		
				this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
			}
			for(String tempTable : noOfOutputTuples.keySet())
				if(getNoOfOutputTuples(tempTable) != -1 && getNoOfOutputTuples(tempTable) >= 1)
					logger.log(Level.INFO,"START COUNT for " + tempTable + " = " + getNoOfOutputTuples(tempTable));
	
			repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
	
			/** Update repeated relation next position etc..*/
			Iterator<String> itr = repeatedRelationCount.keySet().iterator();
			while(itr.hasNext()){
				String tableName = itr.next();
				int c =  repeatedRelationCount.get(tableName);
				for(int i=1;i<=c;i++){
					Integer[] tuplePos = new Integer[32];
					tuplePos[1] = i;//Meaning first tuple is at pos i
					repeatedRelNextTuplePos.put(tableName+i, tuplePos);
					noOfTuples.put(tableName+i, 1);
					currentIndexCount.put(tableName+i, i);
				}
			}
	
			/** Initializes the data structures that are used/updated by the tuple assignment method*/
			initilizeDataStructuresForTupleAssignment(outerBlock);
			for(QueryBlockDetails qbt: getOuterBlock().getFromClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			for(QueryBlockDetails qbt: getOuterBlock().getWhereClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			
			/**get the list of equi join conditions for each table in the query*/
			GenerateCVC1.getListOfEquiJoinConditions( this );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}
	
	public void initializeQueryDetailsQStructureForSet(QueryStructure queryStructure, String pos) throws Exception{
		try{

			if(pos=="left")
			{
				ParseForDataGeneration parseData = new ParseForDataGeneration();
				qStructureL = parseData.parseForDataGeneration(queryStructure);
				
				this.setFne(false);
				queryL = queryStructure.getQuery();	
				queryStringL = query.getQueryString();
				
				currentIndexCountL = query.getCurrentIndexCount();
				repeatedRelationCountL = query.getRepeatedRelationCount();
				
				/**Update the foreign key details in qStructure along with tableNames, NoOfOutputTuples, NoOfTuples, etc.,*/
				QueryStructureForDataGen qd = new QueryStructureForDataGen();
				
				qd.foreignKeyClosure(queryStructure);
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
					qd.foreignKeyClosure(qp);
				}
				for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
					qd.foreignKeyClosure(qp);
				}
				
				baseRelationL = query.getBaseRelation();
				//Get lstRelationInstances (holds table name number), add them to table names map in CVC - with query index. Repeat same for subQ's
				//QIndex 0 for outer block, 1 for from subQ's and 2 for WhereSubQ's
				
				/** Initialize the foreign key details*/
				foreignKeys = new ArrayList<Node>( queryStructure.getForeignKeys());
				foreignKeysModified = new ArrayList<ForeignKey>( queryStructure.getForeignKeyVectorModified());	
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){	
					if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
						for(ForeignKey fk : qp.getForeignKeyVectorModified()){
							if(!foreignKeysModified.contains(fk)){
								foreignKeysModified.add(fk);
							}
						}
					}
				}
				for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
					if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
						for(ForeignKey fk : qp.getForeignKeyVectorModified()){
							if(!foreignKeysModified.contains(fk)){
								foreignKeysModified.add(fk);
							}
						}
					}
				}
				qd.updateBaseRelations(qStructureL,this);
				qd.updateTableNames(qStructureL, this);
				setTablesOfOriginalQuery( new Vector<Table>() );
				qd.updateTableNamesOfOriginalQuery(qStructureL, this);
				/** Initiliaze the outer query block*/
				outerBlockL = QueryBlockDetails.intializeQueryBlockDetails(queryStructure);
					
				/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
		
				/** Initialize each from clause nested sub query blocks */
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
					outerBlockL.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
				}
			}
			else
			{
				ParseForDataGeneration parseData = new ParseForDataGeneration();
				qStructureR = parseData.parseForDataGeneration(queryStructure);
				
				this.setFne(false);
				queryR = queryStructure.getQuery();	
				queryStringR = query.getQueryString();
				
				currentIndexCountR = query.getCurrentIndexCount();
				repeatedRelationCountR = query.getRepeatedRelationCount();
				
				/**Update the foreign key details in qStructure along with tableNames, NoOfOutputTuples, NoOfTuples, etc.,*/
				QueryStructureForDataGen qd = new QueryStructureForDataGen();
				
				qd.foreignKeyClosure(queryStructure);
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
					qd.foreignKeyClosure(qp);
				}
				for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
					qd.foreignKeyClosure(qp);
				}
				
				baseRelationR = query.getBaseRelation();
				//Get lstRelationInstances (holds table name number), add them to table names map in CVC - with query index. Repeat same for subQ's
				//QIndex 0 for outer block, 1 for from subQ's and 2 for WhereSubQ's
				
				/** Initialize the foreign key details*/
				foreignKeys = new ArrayList<Node>( queryStructure.getForeignKeys());
				foreignKeysModified = new ArrayList<ForeignKey>( queryStructure.getForeignKeyVectorModified());	
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){	
					if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
						for(ForeignKey fk : qp.getForeignKeyVectorModified()){
							if(!foreignKeysModified.contains(fk)){
								foreignKeysModified.add(fk);
							}
						}
					}
				}
				for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
					if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
						for(ForeignKey fk : qp.getForeignKeyVectorModified()){
							if(!foreignKeysModified.contains(fk)){
								foreignKeysModified.add(fk);
							}
						}
					}
				}
				qd.updateBaseRelations(qStructureR,this);
				qd.updateTableNames(qStructureR, this);
				setTablesOfOriginalQuery( new Vector<Table>() );
				qd.updateTableNamesOfOriginalQuery(qStructureR, this);
				/** Initiliaze the outer query block*/
				outerBlockR = QueryBlockDetails.intializeQueryBlockDetails(queryStructure);
					
				/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
		
				/** Initialize each from clause nested sub query blocks */
				for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
					outerBlockR.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
				}
			}
			
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(), e);
			//e.printStackTrace();
			throw e;
		}
	}

	/** 
	 * This method initializes all the details about the given query whose details are stored in the query Parser
	 * @param qParser
	 */
	public void initializeQueryDetailsQStructure (QueryStructure queryStructure) throws Exception{
		try{
			ParseForDataGeneration parseData = new ParseForDataGeneration();
			qStructure = parseData.parseForDataGeneration(queryStructure);
			
			this.setFne(false);
			query = queryStructure.getQuery();	
			queryString = query.getQueryString();
			//currentIndex = query.getCurrentIndex();
			//baseRelation = query.getBaseRelation();
			currentIndexCount = query.getCurrentIndexCount();
			repeatedRelationCount = query.getRepeatedRelationCount();
			
			/**Update the foreign key details in qStructure along with tableNames, NoOfOutputTuples, NoOfTuples, etc.,*/
			QueryStructureForDataGen qd = new QueryStructureForDataGen();
			
			qd.foreignKeyClosure(queryStructure);
			for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
				qd.foreignKeyClosure(qp);
			}
			for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
				qd.foreignKeyClosure(qp);
			}
			baseRelation = query.getBaseRelation();
			//query.getFromTables().put(queryStructure.getQuery().addFromTable(new Table ));
			//qd.populateFromTables(queryStructure,this);
			//Get lstRelationInstances (holds table name number), add them to table names map in CVC - with query index. Repeat same for subQ's
			//QIndex 0 for outer block, 1 for from subQ's and 2 for WhereSubQ's
			
			/** Initialize the foreign key details*/
			foreignKeys = new ArrayList<Node>( queryStructure.getForeignKeys());
			foreignKeysModified = new ArrayList<ForeignKey>( queryStructure.getForeignKeyVectorModified());	
			for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){	
				if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
					for(ForeignKey fk : qp.getForeignKeyVectorModified()){
						if(!foreignKeysModified.contains(fk)){
							foreignKeysModified.add(fk);
						}
					}
				}
			}
			for(QueryStructure qp: queryStructure.getWhereClauseSubqueries()){
				if(qp.getForeignKeyVectorModified() != null && !qp.getForeignKeyVectorModified().isEmpty()){
					for(ForeignKey fk : qp.getForeignKeyVectorModified()){
						if(!foreignKeysModified.contains(fk)){
							foreignKeysModified.add(fk);
						}
					}
				}
			}
			qd.updateBaseRelations(qStructure,this);
			qd.updateTableNames(qStructure, this);
			setTablesOfOriginalQuery( new Vector<Table>() );
			qd.updateTableNamesOfOriginalQuery(qStructure, this);
			/** Initiliaze the outer query block*/
			outerBlock = QueryBlockDetails.intializeQueryBlockDetails(queryStructure);
				 
			/**It stores which occurrence of relation occurred in which block of the query, the value contains [queryType, queryIndex]*/
			//tableNames = queryStructure.getTableNames();
	
			/** Initialize each from clause nested sub query blocks */
			initialiseFromClauseSubqueryQueryBlockRecursively(queryStructure, outerBlock);
			
			// for(QueryStructure qp: queryStructure.getFromClauseSubqueries()){				
			// 	//qd.foreignKeyClosure(qp);
			// 	outerBlock.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qp) );
			// }
			
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(), e);
			//e.printStackTrace();
			throw e;
		}
	}
	

	private void initialiseFromClauseSubqueryQueryBlockRecursively(QueryStructure qs,
			QueryBlockDetails ob) {
		// TODO Auto-generated method stub
		
		for(int i=0; i<qs.getFromClauseSubqueries().size(); i++){
			ob.getFromClauseSubQueries().add( QueryBlockDetails.intializeQueryBlockDetails(qs.getFromClauseSubqueries().get(i)) );

			initialiseFromClauseSubqueryQueryBlockRecursively(qs.getFromClauseSubqueries().get(i), ob.getFromClauseSubQueries().get(i));
		}


		// throw new UnsupportedOperationException("Unimplemented method 'initialiseFromClauseSubqueryQueryBlockRecursively'");
	}


	public void initializeOtherDetails() throws Exception{

		try{
			/**Update the  base relations in each block of the query*/
			//commented as it is not required for the new query structure
			//RelatedToPreprocessing.getRelationOccurredInEachQueryBlok(this);
	
			/**Sort the foreign keys based on topological sorting of foreign keys*/
			RelatedToPreprocessing.sortForeignKeys(this);
			this.setConstraintSolver(Configuration.getProperty("smtsolver"));
			
			if(Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3")){
				this.setSolverSpecificCommentCharacter("%");
			}else{
				this.setSolverSpecificCommentCharacter(";");
			}
			
			if(Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3")){
					/**Generate CVC3 Header, This is need to initialize the CVC3 Data Type field of each column of each table */
					this.setCVC3_HEADER( GetSolverHeaderAndFooter.generateSolver_Header(this) );
			}else{
				this.setSMTLIB_HEADER(GetSolverHeaderAndFooter.generateSolver_Header(this));
			}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}


	/**
	 * Initializes the elements necessary for data generation
	 * Call this function after the previous data generation has been done and 
	 * constraints for the current data generation have not been added
	 */
	/*
	public void inititalizeForDataset() throws Exception{

		constraints = new ArrayList<String>();
		stringConstraints = new ArrayList<String>();
		CVCStr = "";
		typeOfMutation = "";
		try{
			

			//Add additional groupBy attributes if the relation of groupby attributes references to any other relation
			// Make the referenced relation a join with existing relation
			//Update no of output tuples, no of groups, repeated relation count, table occurrences to add additional
			//datasets
			
			
			//initialize the no of output tuples
			noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
	
			//Merging noOfOutputTuples, if input query has set operations
			if(qParser.setOperator!=null && qParser.setOperator.length()>0){  
	
				//Initialize the number of tuples in left side query of the set operation
				noOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCleft().query.getRepeatedRelationCount().clone();
	
				//Now get the no of tuples for each relation on right side query of the set operation and add to the data structure
				HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)unionCVC.getGenCVCright().query.getRepeatedRelationCount().clone();
	
				//get iterator
				Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
	
				//while there are values in the hash map
				while(rt.hasNext()){
					Map.Entry pairs=(Entry) rt.next();
	
					//get table name
					String table=(String) pairs.getKey();
	
					//get the number of tuples
					int noOfTuples = (Integer) pairs.getValue();
	
					//Update the data structure
					if(noOfOutputTuplesContains(table)&&noOfOutputTuples.get(table)<noOfTuples){
						noOfOutputTuples.put(table, noOfTuples);
					}
					if(!noOfOutputTuplesContains(table)){
						noOfOutputTuples.put(table, noOfTuples);
					}
				}
			}else if(!qParser.getFromClauseSubqueries().isEmpty()
					&& qParser.getFromClauseSubqueries().get(0) != null){
				QueryParser qp = new QueryParser(qParser.getTableMap());
				qp = qParser.getFromClauseSubqueries().get(0);
				
				if(qp.setOperator!=null && qp.setOperator.length()>0){
					
					//Initialize the number of tuples in left side query of the set operation
					noOfOutputTuples = (HashMap<String,Integer>)qp.getLeftQuery().getQuery().getRepeatedRelationCount().clone();
		
					//Now get the no of tuples for each relation on right side query of the set operation and add to the data structure
					HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)qp.getRightQuery().getQuery().getRepeatedRelationCount().clone();
		
					//get iterator
					Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
		
					//while there are values in the hash map
					while(rt.hasNext()){
						Map.Entry pairs=(Entry) rt.next();
		
						//get table name
						String table=(String) pairs.getKey();
		
						//get the number of tuples
						int noOfTuples = (Integer) pairs.getValue();
		
						//Update the data structure
						if(noOfOutputTuplesContains(table)&&noOfOutputTuples.get(table)<noOfTuples){
							noOfOutputTuples.put(table, noOfTuples);
						}
						if(!noOfOutputTuplesContains(table)){
							noOfOutputTuples.put(table, noOfTuples);
						}
					}
					
				}
			}
			
			else{		
				this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
			}
			for(String tempTable : noOfOutputTuples.keySet())
				if(noOfOutputTuples.get(tempTable) != null && noOfOutputTuples.get(tempTable) >= 1)
					logger.log(Level.INFO,"START COUNT for " + tempTable + " = " + noOfOutputTuples.get(tempTable));
	
			repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
	
			// Update repeated relation next position etc..
			Iterator<String> itr = repeatedRelationCount.keySet().iterator();
			while(itr.hasNext()){
				String tableName = itr.next();
				int c =  repeatedRelationCount.get(tableName);
				for(int i=1;i<=c;i++){
					Integer[] tuplePos = new Integer[32];
					tuplePos[1] = i;//Meaning first tuple is at pos i
					repeatedRelNextTuplePos.put(tableName+i, tuplePos);
					noOfTuples.put(tableName+i, 1);
					currentIndexCount.put(tableName+i, i);
				}
			}
	
			//Initializes the data structures that are used/updated by the tuple assignment method
			initilizeDataStructuresForTupleAssignment(outerBlock);
			for(QueryBlockDetails qbt: getOuterBlock().getFromClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			for(QueryBlockDetails qbt: getOuterBlock().getWhereClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			
			//get the list of equi join conditions for each table in the query
			GenerateCVC1.getListOfEquiJoinConditions( this );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}*/

	/*
	 * @Author: Sunanda
	 * 
	 */


	public Integer getTableAliasName(String tableName){
		tableName = tableName.toUpperCase();
		if(aliasMappingToLevels.containsKey(tableName) ){
			return aliasMappingToLevels.get(tableName).getQueryLevel();
		}
		return null;
	}

	//Start using QueryStructure
	/**
	 * Initializes the elements necessary for data generation
	 * Call this function after the previous data generation has been done and 
	 * constraints for the current data generation have not been added
	 */
	public void inititalizeForDatasetQs() throws Exception{

		constraints = new ArrayList<String>();
		stringConstraints = new ArrayList<String>();
		CVCStr = "";
		typeOfMutation = "";
		
		inititalizeSQDataset();
		correlationHashMap = new HashMap<Node, correlationStructure>();
		tableMap.setSQTables(new HashMap<String,Table>());

		try{
			

			/** Add additional groupBy attributes if the relation of groupby attributes references to any other relation**/
			// Make the referenced relation a join with existing relation
			//Update no of output tuples, no of groups, repeated relation count, table occurrences to add additional
			//datasets
			
			
			/** initialize the no of output tuples*/
	
			if(qStructure.isSetOp == false)
			{
				noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
	
				if(!qStructure.getFromClauseSubqueries().isEmpty()
						&& qStructure.getFromClauseSubqueries().get(0) != null){
					QueryStructure qs = new QueryStructure(qStructure.getTableMap());
					qs = qStructure.getFromClauseSubqueries().get(0);
					
					if(qs.setOperator!=null && qs.setOperator.length()>0){
						
						/**Initialize the number of tuples in left side query of the set operation*/
						noOfOutputTuples = (HashMap<String,Integer>)qs.getLeftQuery().getQuery().getRepeatedRelationCount().clone();
			
						/**Now get the no of tuples for each relation on right side query of the set operation and add to the data structure*/
						HashMap<String,Integer> RightnoOfOutputTuples = (HashMap<String,Integer>)qs.getRightQuery().getQuery().getRepeatedRelationCount().clone();
			
						/**get iterator*/
						Iterator rt=RightnoOfOutputTuples.entrySet().iterator();
			
						/**while there are values in the hash map*/
						while(rt.hasNext()){
							Map.Entry pairs=(Entry) rt.next();

							/**get table name*/
							String table=(String) pairs.getKey();
			
							/**get the number of tuples*/
							int noOfTuples = (Integer) pairs.getValue();
			
							/**Update the data structure*/
							if(noOfOutputTuplesContains(table) && getNoOfOutputTuples(table)<noOfTuples){
								putNoOfOutputTuples(table, noOfTuples);
							}
							if(!noOfOutputTuplesContains(table)){
								putNoOfOutputTuples(table, noOfTuples);
							}
						}	
					}
				}
				
				else{		
					this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
				}

			}
			else
			{
				this.noOfOutputTuples = (HashMap<String,Integer>)qStructure.leftQuery.getQuery().getRepeatedRelationCount().clone();	
				// this.noOfOutputTuplesR = (HashMap<String,Integer>)queryR.getRepeatedRelationCount().clone();
			
			}
			for(String tempTable : noOfOutputTuples.keySet())
				if(getNoOfOutputTuples(tempTable) != -1 && getNoOfOutputTuples(tempTable) >= 1)
					logger.log(Level.INFO,"START COUNT for " + tempTable + " = " + getNoOfOutputTuples(tempTable));
	
			repeatedRelNextTuplePos = new HashMap<String, Integer[]>();
	
			/** Update repeated relation next position etc..*/
			Iterator<String> itr = repeatedRelationCount.keySet().iterator();
			while(itr.hasNext()){
				String tableName = itr.next();
				int c =  repeatedRelationCount.get(tableName);
				for(int i=1;i<=c;i++){
					Integer[] tuplePos = new Integer[32];
					tuplePos[1] = i; //Meaning first tuple is at pos i
					repeatedRelNextTuplePos.put(tableName+i, tuplePos);
					noOfTuples.put(tableName+i, 1);
					currentIndexCount.put(tableName+i, i);
				}
			}
			//this.noOfOutputTuples = (HashMap<String,Integer>)query.getRepeatedRelationCount().clone();
			/** Initializes the data structures that are used/updated by the tuple assignment method*/
			initilizeDataStructuresForTupleAssignment(outerBlock);
			for(QueryBlockDetails qbt: getOuterBlock().getFromClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			for(QueryBlockDetails qbt: getOuterBlock().getWhereClauseSubQueries())
				initilizeDataStructuresForTupleAssignment(qbt);
			
			/**get the list of equi join conditions for each table in the query*/
			GenerateCVC1.getListOfEquiJoinConditions( this );
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch (Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}


	//End of QStructure method
	/**
	 * This method initializes the data structures that are used by the tuple assignment method
	 * @param queryBlock
	 */
	public void initilizeDataStructuresForTupleAssignment(QueryBlockDetails queryBlock){
		
		/** neha -- added to initialize the param data structure**/
		RelatedToParameters.setupDataStructuresForParamConstraints(this,queryBlock);
		/** Add constraints related to parameters*/
		this.getConstraints().add(RelatedToParameters.addDatatypeForParameters( this, queryBlock));

		/**initialize other elements*/
		queryBlock.setUniqueElements(new HashSet<HashSet<Node>>());
		queryBlock.setUniqueElementsAdd(new HashSet<HashSet<Node>>());

		queryBlock.setSingleValuedAttributes(new HashSet<Node>());
		queryBlock.setSingleValuedAttributesAdd(new HashSet<Node>());

		queryBlock.setNoOfGroups(1);
		queryBlock.setFinalCount(1);

		queryBlock.setEquivalenceClassesKilled( new Vector<Node>());

	}

	public String findNextWord(String input, String p) {
		String output = "";
        // Define the pattern to match "order by" followed by any word
        Pattern pattern = Pattern.compile("\\b"+p+"\\s+\\w+", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        
        // Find the "order by" keyword and its next word
        if (matcher.find()) {
            output = matcher.group();
        }
		return output;
    }

	HashMap<Integer, Node> levelToNodeMap = new HashMap<Integer, Node>();		
	/**
	 * @author parismita
	 * @param cvc
	 * @return
	 */
	public String printSQL(QueryBlockDetails qbd, MutationStructure mutStruct) throws Exception{
		String str = ""; Boolean dist = false;

		for(ConjunctQueryStructure cs : qbd.getConjunctsQs()){
			for(Node n: cs.getAllSubQueryConds()){
				int l = n.getSubQueryStructure().getLevel();
				levelToNodeMap.put(l, n);
			}
		}

		//distinct
		if(qbd.isDistinct()) dist = true;
		SelectQuery print = new SelectQuery(dist);
		HashMap<Node, String> subwhere = new HashMap<Node, String>();
		HashMap<Node, String> subfrom = new HashMap<Node, String>();
		//if(mutStruct == null) return this.queryString;
		
		for (int i = 0; i < qbd.getWhereClauseSubQueries().size(); i++) {
			QueryBlockDetails q = qbd.getWhereClauseSubQueries().get(i);
			if (q != null) {
				Node n = new Node();
				if(levelToNodeMap.containsKey(q.getLevel())&&levelToNodeMap.get(q.getLevel())!=null)
					subwhere.put(levelToNodeMap.get(q.getLevel()), printSQL(q, mutStruct));
				else 
					subwhere.put(n, printSQL(q, mutStruct));
			}
		}
		for (int i = 0; i < qbd.getFromClauseSubQueries().size(); i++) {
			QueryBlockDetails q = qbd.getFromClauseSubQueries().get(i);
			if (q != null) {
				Node n = new Node();
				if(levelToNodeMap.containsKey(q.getLevel()))
					subfrom.put(levelToNodeMap.get(q.getLevel()), printSQL(q, mutStruct));
				else 
					subfrom.put(n, printSQL(q, mutStruct));
			}
		}
		
		Vector<Table> tables = new Vector<Table>();
		HashMap<Table, String> alias = new HashMap<Table, String> ();
		int NoOfCol = 0;
		for (String baseRelations : qbd.getBaseRelations()) {
			Table cvcTable = this.getTableMap().getTable(baseRelations.replaceAll("\\d", "").toUpperCase());
			if(!tables.contains(cvcTable)){
				tables.add(cvcTable);
				alias.put(cvcTable, baseRelations);
				NoOfCol += cvcTable.getNoOfColumn();
			}
		}
		
		//group by
		for(Node n: qbd.getGroupByNodes())
			print.addCustomGroupings(n);
		Boolean isAggr = false;
		if (qbd.getGroupByNodes().size() != 0) isAggr =true;
		for (Node prjColumn : qbd.getProjectedCols()) {
			if (prjColumn.getType().equalsIgnoreCase("AGGREGATE NODE") && qbd.getBaseRelations().contains(prjColumn.getTable().getTableName()))
			isAggr =true;
		}
		//having 
		if(qbd.getHavingClause()!=null) {
			String having = qbd.getHavingClause().toString();
			having = having.substring(1, having.length()-1);
			print.addHaving(new CustomCondition(having));}
		
		
		//case
		boolean isCase = false;
		for(Node c : qbd.getProjectedCols()){
			if(c.getType().equals(Node.getCaseNodeType())){
				isCase=true;
				ArrayList<CaseCondition> arr = c.getCaseExpression().getWhenConditionals();
				Node els = c.getCaseExpression().getElseConditional().getThenNode();
				CaseStatement st = new CaseStatement();
				for(CaseCondition ct : arr){
					String when = ct.getWhenNode().toString();
					when = when.replaceAll("i~", " NOT LIKE ").replaceAll("!~", " LIKE ");
					when = when.replaceAll("!i~", " LIKE ").replaceAll("~", " LIKE ").replaceAll("/=", "!=");
					when = when.substring(1, when.length()-1);
					st.addWhen(new CustomCondition(when), ct.getThenNode());
				}
				print.addAliasedColumn(st.addElse(els), c.getAliasName());
			}
		}

		//select
		if(!isAggr&&!isCase&&qbd.getProjectedCols().size() == NoOfCol) print.addCustomColumns(new CustomSql("*"));
		else{
			Node loc=null, node=null;
			if(mutStruct!=null&&mutStruct.getMutationTypeNumber()==20){
			loc = (Node) mutStruct.getMutationLoc();
			node = (Node) mutStruct.getMutationNode();}
			for(Node c : qbd.getProjectedCols()){
				if(!c.getType().equals(Node.getCaseNodeType())){
					if(c.getColumn()!=null&&c.getColumn().getColumnName().equalsIgnoreCase("xdata_cnt")) continue;
					if(mutStruct!=null&&c!=null&&node!=null&c==loc) print.addCustomColumns(new CustomSql(node));
					else print.addCustomColumns(new CustomSql(c));
				}		
			}
		}
		//from
		class Join{
			public Table left;
			public Table right;
			public String leftname;
			public String rightname;
			public String joinType;
			Join(Table l, Table r, String ln, String rn, String t) {left=l; right=r; leftname=ln;rightname=rn; joinType=t; } 
			public boolean equals(Object o) {
				if (this == o) return true;
				if (o == null || getClass() != o.getClass()) return false;
				Join join = (Join) o;
				return Objects.equals(left, join.left) &&
					   Objects.equals(right, join.right) &&
					   Objects.equals(leftname, join.leftname) &&
					   Objects.equals(rightname, join.rightname) &&
					   Objects.equals(joinType, join.joinType);
			}
			public int hashCode() {
				return Objects.hash(left, right, leftname, rightname, joinType);
			}
		};
		HashMap<Join, Vector<Node>> fromMap = new HashMap<Join, Vector<Node>>();
		for(ConjunctQueryStructure cs : qbd.getConjunctsQs()){
			Vector<Node> ojoinConds = cs.getJoinCondsAllOther();
			Vector<Node> eqjoinConds = cs.getJoinCondsForEquivalenceClasses();
			Vector<Node> Conds = new Vector<Node>();
			Conds.addAll(ojoinConds);
			Conds.addAll(eqjoinConds);
			for(Node n: Conds){
				String leftname = n.getLeft().getTable().getTableName() + " as "+ n.getLeft().getTableNameNo();
				String rightname = n.getRight().getTable().getTableName() + " as "+ n.getRight().getTableNameNo();
				Join l = new Join(n.getLeft().getTable(), n.getRight().getTable(), leftname, rightname, n.getJoinType());
				Join r = new Join(n.getRight().getTable(), n.getLeft().getTable(), rightname,  leftname, n.getJoinType());

				if(n.getJoinType()!=null&&n.getJoinType().equalsIgnoreCase("inner join")){
					if(fromMap.containsKey(l)) fromMap.get(l).add(n);	
					else if(fromMap.containsKey(r)) fromMap.get(r).add(n);	
					else {fromMap.put(l, new Vector<>()); fromMap.get(l).add(n);}
				}
				else if(n.getJoinType()!=null){
					if(fromMap.containsKey(l)) fromMap.get(l).add(n);	
					else {fromMap.put(l, new Vector<>()); fromMap.get(l).add(n);}
				}	
			}
		}
		
		Vector<String> notinTables = new Vector<>();
		for(Join join: fromMap.keySet()){
			JoinType tt=SelectQuery.JoinType.INNER; String cond = "";
			for(Node n: fromMap.get(join)){
				cond += n + " and ";
			}
			notinTables.add(join.left.getAliasName().toUpperCase());
			notinTables.add(join.right.getAliasName().toUpperCase());
			cond = cond.substring(0, cond.length()-4);
			cond = cond.replaceAll("!~", " NOT LIKE ").replaceAll("i~", " NOT LIKE ");
			cond = cond.replaceAll("!i~", " LIKE ").replaceAll("~", " LIKE ");
			cond = cond.replaceAll("/=", "!=");
			if(join.joinType.equalsIgnoreCase("left outer join")) tt = SelectQuery.JoinType.LEFT_OUTER;
			if(join.joinType.equalsIgnoreCase("right outer join")) tt = SelectQuery.JoinType.RIGHT_OUTER;
			if(join.joinType.equalsIgnoreCase("full outer join")) tt = SelectQuery.JoinType.FULL_OUTER;
			print.addCustomJoin(tt, join.leftname,join.rightname, new CustomCondition(cond));
			//doesnt have subquery of from clause
		}
		for(Table t: alias.keySet()){
			if(t!=null&&!notinTables.contains(alias.get(t)))
			print.addCustomFromTable(new CustomSql(t.getTableName() + " as "+ alias.get(t)));
		}
		//where
		for(ConjunctQueryStructure cs : qbd.getConjunctsQs()){
			Vector<Node> oConds = cs.getSelectionConds();
			Vector<Node> eConds = cs.getStringSelectionConds();
			Vector<Node> iConds = cs.getInClauseConds();
			Vector<Node> Conds = new Vector<Node>();
			Conds.addAll(oConds);
			Conds.addAll(eConds);
			Conds.addAll(iConds);
			for(Node n: Conds){
				String where = n.toString();
				where = where.replaceAll("!~", " NOT LIKE ").replaceAll("i~", " NOT LIKE ");
				where = where.replaceAll("!i~", " LIKE ").replaceAll("~", " LIKE ");
				where = where.replaceAll("/=", "!=");
				where = where.substring(1, where.length()-1);
				print.addCondition(new CustomCondition(where));	
			}
		}
		for(Node sub: subwhere.keySet()){
			if(sub.getType().equals(Node.getExistsNodeType())||sub.getType().equals(Node.getNotExistsNodeType()))
				print.addCondition(new CustomCondition(sub.getType() +" ("+ subwhere.get(sub).toString()+")"));	
			else if(sub.getLeft()!=null&&sub.getLhsRhs()!=null&&sub.getType().equals(Node.getBroNodeSubQType())){
				print.addCondition(new CustomCondition(sub.getLeft()+" "+sub.getOperator() +" ("+ subwhere.get(sub).toString()+")"));	
			}
			else if(sub.getRight()!=null&&sub.getLhsRhs()!=null&&sub.getType().equals(Node.getBroNodeSubQType())){
				print.addCondition(new CustomCondition("("+subwhere.get(sub).toString()+") "+sub.getOperator()+ " "+sub.getRight()));	
			}
			else{
				print.addCondition(new CustomCondition(subwhere.get(sub).toString()));
			}
		}

		//order by
		if(qbd.getOrderByNodes().size() > 0) {
			for(Node c : qbd.getOrderByNodes()){
				if(c.getColumn().getColumnName().equalsIgnoreCase("xdata_cnt")) continue;
				Dir dir = c.orderByDirAsc==true?Dir.ASCENDING:Dir.DESCENDING;
				print.addCustomOrdering(new CustomSql(c), dir);
			}
		}
		//limit
		if(qbd.getLimit()>0){
		MysLimitClause limit = new MysLimitClause(qbd.getLimit()); 
		print.addCustomization(limit);}

		str = print.toString();
		//reconfirm joins - multiple joins for same condition, condicion incorrectness as table not present, topo sort need etc, 
		//column replaement
		return str;
	}

	/**
	 * Generates datasets to kill each type of mutation for the original query
	 * @throws Exception
	 */
	public void generateDatasetsToKillMutations() throws Exception {

		try{
			/**Generate data for the original query*/
			
			String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
			
			// old code - comment after everything works

			if(!Configuration.getProperty("regressDS0").equalsIgnoreCase("true"))
			{
				// Refactoring starts - Sunanda

				// template for mutStruct
				// Node targetNode = null;
				// mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
				// Integer mutationTypeNumber = TagDatasets.mutationTypeNumber.ORIGINAL.getMutationType() ;
				// MutationStructure mutStruct = new MutationStructure(mutationType, mutationTypeNumber, targetNode);

				// generate dataset for non-empty results

				// Class name can be little bit short
				// GenConstraints
				// System.out.println( this.queryString);

				GenConstraints.generateDatasetForNonEmptyDataset(this);
				
				
				GenConstraints.generateDatasetForNonEmptyDataset(this);
				//GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.COLUMNREPLACEMENT.getMutationType(), TagDatasets.mutationTypeNumber.COLUMNREPLACEMENT.getMutationType());
				
				// generate dataset to kill equivalence class mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.EQUIVALENCE.getMutationType(), TagDatasets.mutationTypeNumber.EQUIVALENCE.getMutationType());
				// 
				// // generate dataset to kill selection mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.SELCTION.getMutationType(), TagDatasets.mutationTypeNumber.SELCTION.getMutationType());
				
				// // // generate dataset to kill string, like, pattern mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.STRING.getMutationType(), TagDatasets.mutationTypeNumber.STRING.getMutationType());
				
				// // // generate dataset to kill exist-notexist mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.WHERECONNECTIVE.getMutationType(), TagDatasets.mutationTypeNumber.WHERECONNECTIVE.getMutationType());
				
				// // // generate dataset to kill non-equi join mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.NONEQUIJOIN.getMutationType(), TagDatasets.mutationTypeNumber.NONEQUIJOIN.getMutationType());

				// // // generate dataset to kill column replacement mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.COLUMNREPLACEMENT.getMutationType(), TagDatasets.mutationTypeNumber.COLUMNREPLACEMENT.getMutationType());
				
				// // // generate dataset to kill extra group by attributes mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.EXTRAGROUPBY.getMutationType(), TagDatasets.mutationTypeNumber.EXTRAGROUPBY.getMutationType());

				// // // // // generate dataset to kill missing group by attributes mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.PARTIALGROUPBY1.getMutationType(), TagDatasets.mutationTypeNumber.PARTIALGROUPBY1.getMutationType());

				// // // // // generate dataset to kill agg fun mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.AGG.getMutationType(), TagDatasets.mutationTypeNumber.AGG.getMutationType());
				
				// // generate dataset to kill distinct attributes mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.DISTINCT.getMutationType(), TagDatasets.mutationTypeNumber.DISTINCT.getMutationType());

				// GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.ADDITIONALDISTINCT.getMutationType(), TagDatasets.mutationTypeNumber.ADDITIONALDISTINCT.getMutationType());

				// // // // generate dataset to kill having clause mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.HAVING.getMutationType(), TagDatasets.mutationTypeNumber.HAVING.getMutationType());

				// // // // generate dataset to kill unintended join mutations
				GenConstraints.generateConstraintsToKillMutations(this, TagDatasets.MutationType.UNINTENDED.getMutationType(), TagDatasets.mutationTypeNumber.UNINTENDED.getMutationType());

				// // // generate dataset to kill setop mutations
				GenConstraints.generateConstraintsToKillMutationsForSet(this, TagDatasets.MutationType.SETOP.getMutationType(), TagDatasets.mutationTypeNumber.SETOP.getMutationType());
				
				
				
				// old code

				// /**Generate data sets to kill mutations in outer query block */
				// MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(this);
		
				// /**Generate data sets to kill mutations in from clause nested sub query blocks */
				// MutationsInFromSubQuery.generateDataForKillingMutantsInFromSubQuery(this);
		
				// /**Generate data sets to kill mutations in where clause nested sub query blocks */
				// MutationsInWhereSubQuery.generateDataForKillingMutantsInWhereSubQuery(this);

				// old code ends
			}
			else{			
					GenerateDataForOriginalQuery.generateDataForOriginalQuery(this, mutationType);		
			}
			
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			this.closeConn();
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			e.printStackTrace(); // TEMPCODE Rahul Sharma : for debugging
			this.closeConn();
			throw new Exception("Internal Error", e);
		}
	}

	/*public void generateDatasetsToKillMutationsUsingSMT() throws Exception{
		try{
			String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType() + TagDatasets.QueryBlock.NONE.getQueryBlock();
			GenerateDataForOriginalQuery.generateDataForOriginalQuery(this, mutationType);		
			
			//**Generate data sets to kill mutations in outer query block 
			//MutationsInOuterBlock.generateDataForKillingMutantsInOuterQueryBlock(this);
	
			//**Generate data sets  to kill mutations in from clause nested sub query blocks 
			//MutationsInFromSubQuery.generateDataForKillingMutantsInFromSubQuery(this);
	
			//**Generate data sets  to kill mutations in where clause nested sub query blocks
			//MutationsInWhereSubQuery.generateDataForKillingMutantsInWhereSubQuery(this);
			
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			this.closeConn();
			throw new Exception("Internal Error", e);
		}
	}*/
	
	

	/**
	 * A wrapper method that is used to get the number of tuples for each base relation occurrence 
	 * in each block of the query
	 */
	public static boolean tupleAssignmentForQuery(GenerateCVC1 cvc) throws Exception{

		try{
			//  if( CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, cvc.getOuterBlock()) == false)
			//  	return false;
	
			TraverseforAliasing(cvc, cvc.getOuterBlock());
			//return getTupleAssignmentForSubQueries(cvc);
			return TraverseforTupleAssignment(cvc, cvc.getOuterBlock(), 0, 0);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

	public static boolean TraverseforTupleAssignment(GenerateCVC1 cvc, QueryBlockDetails qb, int minTuple, int flag) throws Exception {
		// TODO Auto-generated method stub
		boolean possible = true;
		if( CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qb) == false)
				return false;
		//min max - act like unconstrained
		if(qb.getAggConstraints() != null && qb.getAggConstraints().size()!=0) flag=1;
		
		if(flag==1) minTuple = CountEstimationRelated.UpdateNoOfOutputTuples(cvc, qb, minTuple);

		for (int i = 0; i < qb.getWhereClauseSubQueries().size(); i++) {
			QueryBlockDetails n = qb.getWhereClauseSubQueries().get(i);
			if (n != null) {
				possible = TraverseforTupleAssignment(cvc, n, minTuple, flag);
				if(possible==false) return false;
			}
		}
		for (int i = 0; i < qb.getFromClauseSubQueries().size(); i++) {
			QueryBlockDetails n = qb.getFromClauseSubQueries().get(i);
			if (n != null) {
				possible = TraverseforTupleAssignment(cvc, n, minTuple, flag);
				if(possible==false) return false;
			}
		}
		return possible;
	}
	public static void TraverseforAliasing(GenerateCVC1 cvc, QueryBlockDetails qb) throws Exception {
		// TODO Auto-generated method stub

		for (int i = 0; i < qb.getWhereClauseSubQueries().size(); i++) {
			QueryBlockDetails n = qb.getWhereClauseSubQueries().get(i);
			if (n != null) {
				TraverseforAliasing(cvc, n);
			}
		}
		for (int i = 0; i < qb.getFromClauseSubQueries().size(); i++) {
			QueryBlockDetails n = qb.getFromClauseSubQueries().get(i);
			if (n != null) {
				TraverseforAliasing(cvc, n);
			}
		}
		//issue - if t1.id is present in more than one level -> it only stores the bottom most level
		//need for a vector where t1.id is present - and check if current level is one of the levels in vector
		for(String rel: qb.baseRelations) {
			levelStrcuture lvs = new levelStrcuture(qb.getLevel(), false);
			if(rel!=null&&!cvc.aliasMappingToLevels.containsKey(rel.toLowerCase())){
				cvc.aliasMappingToLevels.put(rel.toLowerCase(), lvs);
				}
			}
		}
	/**
	 * estimate the number of tuples for each relation in each sub query block
	 * @throws Exception
	 */
	public static boolean getTupleAssignmentForSubQueries(GenerateCVC1 cvc) throws Exception{

		/** flag to indicate whether tuple assignment is possible or not*/
		boolean possible ;
		try{
			/** get tuple assignment for each from clause sub query block*/
			for(QueryBlockDetails qbt: cvc.getOuterBlock().getFromClauseSubQueries() ){
	
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					return false;
			}
	
			/** get tuple assignment for each where clause sub query block*/
			for(QueryBlockDetails qbt: cvc.getOuterBlock().getWhereClauseSubQueries()){
	
				possible = CountEstimationRelated.estimateCountAndgetTupleAssignmentForQueryBlock(cvc, qbt);
	
				/** If tuple assignment is not possible*/
				if(possible == false)
					return false;
			}
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
		/** For all blocks the tuple assignment is successful*/
		return true;

	}



	/**
	 * Gets the list of all equi join conditions on each column of each table 
	 */
	public static void getListOfEquiJoinConditions(GenerateCVC1 cvc) throws Exception{

		try{
			cvc.setEquiJoins( new HashMap<String, Vector<Vector<Node>>>());
			/**get list of equi joins in outer query block*/
			getListOfEquiJoinConditionsInQueryBlock(cvc, cvc.getOuterBlock());
	
			/**get list of join conditions in each from clause nested sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getFromClauseSubQueries())
				getListOfEquiJoinConditionsInQueryBlock(cvc, qb);
	
			/**get list of join conditions in each where clause nested sub query block*/
			for(QueryBlockDetails qb: cvc.getOuterBlock().getWhereClauseSubQueries())
				getListOfEquiJoinConditionsInQueryBlock(cvc, qb);
		}catch(Exception e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw new Exception("Internal Error", e);
		}
	}

	/**
	 * Gets the list of equi join conditions in this query block
	 * @param cvc
	 * @param outerBlock2
	 */
	public static void getListOfEquiJoinConditionsInQueryBlock(	GenerateCVC1 cvc, QueryBlockDetails queryBlock) {

		/**for each conjunct*/
		for(ConjunctQueryStructure con: queryBlock.getConjunctsQs()){
			
			/**get the list of equi join conditions*/
			Vector<Vector<Node>> eqClass = con.getEquivalenceClasses();
			
			/**for every equivalence class*/
			for(Vector<Node> ec: eqClass){
				
				/**for every node in this equivalence class*/
				for(Node n: ec){
					
					String key =  n.getTable().getTableName() ;
					/**if this relation is present in the hash map*/
					if( cvc.getEquiJoins() != null && cvc.getEquiJoins().containsKey(key) ){
						
						/**add this equivalence class to the list, if already not added*/
						if( !cvc.getEquiJoins().get(key).contains(ec) ){
							
							cvc.getEquiJoins().get(key).add(ec);
						}
					}
					else{
						
						Vector< Vector< Node >> eq = new Vector<Vector<Node>>();
						eq.add(ec);
						cvc.getEquiJoins().put(key, eq);
					}
				}
			}
		}

	}


	/**Below are the setters and getters for the variables of this class */
	public TableMap getTableMap() {
		return tableMap;
	}

	public void setTableMap(TableMap tableMap) {
		this.tableMap = tableMap;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public Query getTopQuery() {
		return topQuery;
	}

	public void setTopQuery(Query topQuery) {
		this.topQuery = topQuery;
	}

	/*public QueryParser getqParser() {
		return qParser;
	}

	public void setqParser(QueryParser qParser) {
		this.qParser = qParser;
	}*/

	public HashMap<String, String> getBaseRelation() {
		return baseRelation;
	}

	public void setBaseRelation(HashMap<String, String> baseRelation) {
		this.baseRelation = baseRelation;
	}

	//parismita
	public HashMap<String, String> getBaseSQRelation() {
		return baseSQRelation;
	}

	public void setBaseSQRelation(HashMap<String, String> baseSQRelation) {
		this.baseSQRelation = baseSQRelation;
	}

	public HashMap<String, Integer> getCurrentIndexCount() {
		return currentIndexCount;
	}

	public void setCurrentIndexCount(HashMap<String, Integer> currentIndexCount) {
		this.currentIndexCount = currentIndexCount;
	}

	public HashMap<String, Integer> getRepeatedRelationCount() {
		return repeatedRelationCount;
	}

	public void setRepeatedRelationCount(
			HashMap<String, Integer> repeatedRelationCount) {
		this.repeatedRelationCount = repeatedRelationCount;
	}

	public HashMap<String, Integer[]> getRepeatedRelNextTuplePos() {
		return repeatedRelNextTuplePos;
	}

	public void setRepeatedRelNextTuplePos(
			HashMap<String, Integer[]> repeatedRelNextTuplePos) {
		this.repeatedRelNextTuplePos = repeatedRelNextTuplePos;
	}

	public HashMap<String, Integer> getNoOfTuples() {
		return noOfTuples;
	}

	public void setNoOfTuples(HashMap<String, Integer> noOfTuples) {
		this.noOfTuples = noOfTuples;
	}
/*<<<<<<<<<<<<<<<<<<<<<<<<<< NEW CODE : Pooja >>>>>>>>>>>>>>>>>>>>>>>>>>*/
	public int getNoOfOutputTuples(String tableName) {
		if(!noOfOutputTuples.containsKey(tableName.toUpperCase()))
			return -1;
		return noOfOutputTuples.get(tableName.toUpperCase());
	}
	public int getNoOfMaxOutputTuples(String tableName) {
		if(!noOfMaxOutputTuples.containsKey(tableName.toUpperCase()))
			return -1;
		return noOfMaxOutputTuples.get(tableName.toUpperCase());
	}
	
	public boolean noOfOutputTuplesContains(String tableName) {
		if(this.noOfOutputTuples.containsKey(tableName.toUpperCase()))
			return true;
		return false;
	}
	public boolean noOfMaxOutputTuplesContains(String tableName) {
		if(this.noOfMaxOutputTuples.containsKey(tableName.toUpperCase()))
			return true;
		return false;
	}
	
	public void putNoOfOutputTuples(String tableName,int val) {
		noOfOutputTuples.put(tableName.toUpperCase(), val);
	}
	public void putNoOfMaxOutputTuples(String tableName,int val) {
		noOfMaxOutputTuples.put(tableName.toUpperCase(), val);
	}
	
	public HashMap<String, Integer> cloneNoOfOutputTuples(){
		return (HashMap<String,Integer>) noOfOutputTuples.clone();
	}
	
	public void setNoOfOutputTuples(HashMap<String, Integer> noOfOutputTuples) {
		this.noOfOutputTuples.clear();
		for (Map.Entry<String, Integer> entry : noOfOutputTuples.entrySet()) {
            this.noOfOutputTuples.put(entry.getKey().toUpperCase(),
                           entry.getValue());
        }
		//this.noOfOutputTuples = noOfOutputTuples;
	}
   /*<<<<<<<<<<<<<<<<<<<<<<<<<<<<< END >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>*/
	public QueryBlockDetails getOuterBlock() {
		return outerBlock;
	}

	public void setOuterBlock(QueryBlockDetails outerBlock) {
		this.outerBlock = outerBlock;
	}


	public ArrayList<Node> getForeignKeys() {
		return foreignKeys;
	}


	public void setForeignKeys(ArrayList<Node> foreignKeys) {
		this.foreignKeys = foreignKeys;
	}


	public ArrayList<ForeignKey> getForeignKeysModified() {
		return foreignKeysModified;
	}


	public void setForeignKeysModified(ArrayList<ForeignKey> foreignKeysModified) {
		this.foreignKeysModified = foreignKeysModified;
	}


	public ArrayList<String> getConstraints() {
		return constraints;
	}


	public void setConstraints(ArrayList<String> constraints) {
		this.constraints = constraints;
	}


	public ArrayList<String> getStringConstraints() {
		return stringConstraints;
	}


	public void setStringConstraints(ArrayList<String> stringConstraints) {
		this.stringConstraints = stringConstraints;
	}


	public String getCVCStr() {
		return CVCStr;
	}


	public void setCVCStr(String cVCStr) {
		CVCStr = cVCStr;
	}


	public HashMap<Table, Vector<String>> getResultsetTableColumns1() {
		return resultsetTableColumns1;
	}


	public void setResultsetTableColumns1(HashMap<Table, Vector<String>> resultsetTableColumns1) {
		this.resultsetTableColumns1 = resultsetTableColumns1;
	}


	public HashMap<Column, HashMap<String, Integer>> getColNullValuesMap() {
		return colNullValuesMap;
	}


	public void setColNullValuesMap(HashMap<Column, HashMap<String, Integer>> colNullValuesMap) {
		this.colNullValuesMap = colNullValuesMap;
	}

	public StringConstraintSolver getStringSolver() {
		return stringSolver;
	}


	public void setStringSolver(StringConstraintSolver stringSolver) {
		this.stringSolver = stringSolver;
	}


	public Vector<Column> getResultsetColumns() {
		return resultsetColumns;
	}


	public void setResultsetColumns(Vector<Column> resultsetColumns) {
		this.resultsetColumns = resultsetColumns;
	}


	public boolean isFne() {
		return fne;
	}


	public void setFne(boolean fne) {
		this.fne = fne;
	}


	public ArrayList<String> getDatatypeColumns() {
		return datatypeColumns;
	}


	public void setDatatypeColumns(ArrayList<String> datatypeColumns) {
		this.datatypeColumns = datatypeColumns;
	}


	public String getOutput() {
		return output;
	}


	public void setOutput(String output) {
		this.output = output;
	}


	public String getQueryString() {
		return queryString;
	}


	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}


	public String getFilePath() {
		return filePath;
	}


	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}


	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}

	public int getTupleCount() {
		return tuplecount;
	}

	public int calcTupleCount() { //total tuple count accross datasets
		if(count==0) tuplecount=0;
		for(Integer i: noOfOutputTuples.values()){
			tuplecount+=i;
		}
		return tuplecount;
	}


	public ArrayList<Table> getResultsetTables() {
		return resultsetTables;
	}


	public void setResultsetTables(ArrayList<Table> resultsetTables) {
		this.resultsetTables = resultsetTables;
	}


	public boolean isIpdb() {
		return ipdb;
	}


	public void setIpdb(boolean ipdb) {
		this.ipdb = ipdb;
	}


	public String getCVC3_HEADER() {
		return CVC3_HEADER;
	}


	public void setCVC3_HEADER(String cVC3_HEADER) {
		CVC3_HEADER = cVC3_HEADER;
	}

	public String getSMTLIB_HEADER() {
		return SMTLIB_HEADER;
	}


	public void setSMTLIB_HEADER(String SMTLIB_HEADER) {
		SMTLIB_HEADER = SMTLIB_HEADER;
	}

	public GenerateUnionCVC getUnionCVC() {
		return unionCVC;
	}


	public void setUnionCVC(GenerateUnionCVC unionCVC) {
		this.unionCVC = unionCVC;
	}
	
	public int getAssignmentId() {
		return assignmentId;
	} 


	public void setAssignmentId(int id) {
		this.assignmentId = id;
	}
	
	public int getQuestionId() {
		return questionId;
	}


	public void setQuestionId(int qId) {
		this.questionId = qId;
	}	
	public Map<String, TupleRange> getTupleRange(){
		return this.allowedTuples;
	}
	
	public void updateTupleRange(String relation, int x, int y){
		this.allowedTuples.put(relation, new TupleRange(x, y));
	}
	
	public void setTupleRange(Map<String, TupleRange> tupleRange){
		this.allowedTuples = tupleRange;
	}
	//**************** TEST CODE: POOJA *******************//
	//TODO getter and setter for tempJoin tables
	//**************** TEST CODE END **********************//
	/**
	 * This function is used to update the total number of output tuples data structure,
	 * @param queryBlock
	 * @param noOfGroups: Specifies the number of groups to be generated by this query block
	 */
	public  void updateTotalNoOfOutputTuples(QueryBlockDetails queryBlock, int noOfGroups) {

		/**for each base relation in the query block*/
		for(String tableNameNo: queryBlock.getBaseRelations()){

			/**Indicates the count of relation*/
			int prevCount, prevTotCount;

			if( noOfTuples.get( tableNameNo ) != null){

				/**get the count*/
				prevCount = noOfTuples.get(tableNameNo);

				/**total count contributed by this relation*/
				prevTotCount = prevCount * queryBlock.getNoOfGroups();			


				/**get the new total count contributed by this relation*/
				int totCount = prevCount * noOfGroups;

				/**get table name */
				String tableName = tableNameNo.substring(0, tableNameNo.length()-1);
				
				/**update the total number of output tuples data structre*/
				if( getNoOfOutputTuples(tableName) == -1)
					putNoOfOutputTuples(tableNameNo, totCount );
				else
					putNoOfOutputTuples(tableName, getNoOfOutputTuples(tableName)+ totCount - prevTotCount );
			}
		}

		/**Update the number of groups*/
		queryBlock.setNoOfGroups(noOfGroups);

	}


	public BranchQueriesDetails getBranchQueries() {
		return branchQueries;
	}


	public void setBranchQueries(BranchQueriesDetails branchQueries) {
		this.branchQueries = branchQueries;
	}


	public Vector<Table> getTablesOfOriginalQuery() {
		return tablesOfOriginalQuery;
	}


	public void setTablesOfOriginalQuery(Vector<Table> tablesOfOriginalQuery) {
		this.tablesOfOriginalQuery = tablesOfOriginalQuery;
	}


	public String getTypeOfMutation() {
		return typeOfMutation;
	}


	/**sets the type of mutation we are trying to kill*/
	public void setTypeOfMutation(MutationType mutationType, QueryBlock queryBlock) {

		this.typeOfMutation = mutationType.getMutationType() + queryBlock.getQueryBlock();
	}


	public void setTypeOfMutation(String typeOfMutation) {
		this.typeOfMutation = typeOfMutation;
	}


	public HashMap<String, Integer[]> getTableNames() {
		return tableNames;
	}


	public void setTableNames(HashMap<String, Integer[]> tableNames) {
		this.tableNames = tableNames;
	}


	public HashMap<String, Vector<Vector<Node>>> getEquiJoins() {
		return equiJoins;
	}


	public void setEquiJoins(HashMap<String, Vector<Vector<Node>>> equiJoins) {
		this.equiJoins = equiJoins;
	}
	
	public Connection getConnection() {
		return this.connection;
	}
	
	public void setConnection(Connection conn){
		this.connection = conn;
	}
	
	/*public void initializeConnectionDetails() throws Exception {
		
		try{
			Connection assignmentConn = this.getConnection();
			PopulateTestData p = new PopulateTestData();
			p.deleteAllTempTablesFromTestUser(assignmentConn);
			byte[] dataBytes = null;
			String tempFile = "";
			FileOutputStream fos = null;
			ArrayList<String> listOfQueries = null;
			ArrayList<String> listOfDDLQueries = new ArrayList<String>();
			String[] inst = null;
			
			String fileContent= getSchemaFile();
			dataBytes = fileContent.getBytes();
			tempFile = "/tmp/dummyschema.sql";
			
			 fos = new FileOutputStream(tempFile);
			fos.write(dataBytes);
			fos.close();
			listOfQueries = Utilities.createQueries(tempFile);
			inst = listOfQueries.toArray(new String[listOfQueries.size()]);
			listOfDDLQueries.addAll(listOfQueries);
			for (int i = 0; i < inst.length; i++) {
				// we ensure that there is no spaces before or after the request string  
				// in order to not execute empty statements  
				if (!inst[i].trim().equals("") && ! inst[i].trim().contains("drop table")) {
					//Changed for MSSQL testing
					String temp = inst[i].trim().replaceAll("(?i)^\\s*create\\s+table\\s+", "create temporary table ");
					PreparedStatement stmt2 = assignmentConn.prepareStatement(temp);
						stmt2.executeUpdate();	
					stmt2.close();
				
					    
				}
			}
			String sdContent= getDataFile();
			dataBytes = sdContent.getBytes(); 
			fos = new FileOutputStream(tempFile);
			fos.write(dataBytes);
			fos.close();
			
				listOfQueries = Utilities.createQueries(tempFile);
			inst = listOfQueries.toArray(new String[listOfQueries.size()]);
			 
			for (int i = 0; i < inst.length; i++) {
				// we ensure that there is no spaces before or after the request string  
				// in order to not execute empty statements  
				 if (!inst[i].trim().equals("") && !inst[i].contains("drop table") && !inst[i].contains("delete from")) {
					//Changed for MSSQL TESTING
					PreparedStatement stmt3 = assignmentConn.prepareStatement(inst[i]);
						stmt3.executeUpdate();							
						stmt3.close();
				}
			}
			this.connection = assignmentConn;	
		}
			
		catch(Exception ex){
			logger.log(Level.SEVERE,ex.getMessage(), ex);
			throw ex;
		}
	}
	 
	/**
	 * This method creates the tables from the sql file
	 * @param dbConnDetails
	 */
	/*protected void executeSqlFile(DatabaseConnectionDetails dbConnDetails) {
		
	    final class SqlExecuter extends SQLExec {
	        public SqlExecuter() {
	            Project project = new Project();
	            project.init();
	            setProject(project);
	            setTaskType("sql");
	            setTaskName("sql");
	        }
	    }
	    try{
   			String hostName = dbConnDetails.getJdbc_Url().substring(0,dbConnDetails.getJdbc_Url().indexOf(":"));
	         String portNumber = dbConnDetails.getJdbc_Url().substring(dbConnDetails.getJdbc_Url().indexOf(":")+1,dbConnDetails.getJdbc_Url().length());
	       
	    SqlExecuter executer = new SqlExecuter(); 
	    executer.setSrc(new File(dbConnDetails.getFileName()));
	    //poolProp.setUrl("jdbc:postgresql://"+dbDetails.getJdbc_Url()+"/"+dbDetails.getDbName());
	    executer.setDriver("org.postgresql.Driver");
	    executer.setPassword(dbConnDetails.getDbPwd());
	    executer.setUserid(dbConnDetails.getDbUser());
	    executer.setUrl("jdbc:postgresql://"+dbConnDetails.getJdbc_Url()+"/"+dbConnDetails.getDbName());
	    
	    executer.execute(); 
	    }catch(Exception ex){
	    	logger.log(Level.SEVERE,ex.getMessage(), ex);
			ex.printStackTrace();
			throw ex;
	    }
	}*/
	
	public GenerateCVC1 copy() throws Exception{
		//TODO: change implementation to provide faster copy
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(this);
        out.flush();
        out.close();
        
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
        GenerateCVC1    obj = (GenerateCVC1)in.readObject();
		return obj;
	}


	public int getAssignId() {
		// TODO Auto-generated method stub
		return this.getAssignmentId();
	}


	public int getQueryId() {
		return queryId;
	}


	public void setQueryId(int queryId) {
		this.queryId = queryId;
	}


	public String getCourseId() {
		return courseId;
	}


	public void setCourseId(String courseId) {
		this.courseId = courseId;
	}


	public String getSchemaFile() {
		return schemaFile;
	}


	public void setSchemaFile(String schemaFile) {
		this.schemaFile = schemaFile;
	}


	public String getDataFile() {
		return dataFile;
	}


	public void setDataFileName(String dataFile) {
		this.dataFile = dataFile;
	}


	public boolean isOrderindependent() {
		return orderindependent;
	}


	public void setOrderindependent(boolean orderindependent) {
		this.orderindependent = orderindependent;
	}


	public String getConcatenatedQueryId() {
		return concatenatedQueryId;
	}


	public void setConcatenatedQueryId(String concatenatedQueryId) {
		this.concatenatedQueryId = concatenatedQueryId;
	}


	public QueryStructure getqStructure() {
		return qStructure;
	}


	public void setqStructure(QueryStructure qStructure) {
		this.qStructure = qStructure;
	}


	/**
	 * @return the constraintSolver
	 */
	public String getConstraintSolver() {
		return constraintSolver;
	}


	/**
	 * @param constraintSolver the constraintSolver to set
	 */
	public void setConstraintSolver(String constraintSolver) {
		this.constraintSolver = constraintSolver;
	}


	public String getSolverSpecificCommentCharacter() {
		return solverSpecificCommentCharacter;
	}


	public void setSolverSpecificCommentCharacter(String solverSpecialCharacter) {
		this.solverSpecificCommentCharacter = solverSpecialCharacter;
	}
	
	public int getMaxNumberOfOutputTuples(){
		int maxtuple = 0;
		for(String table: this.noOfOutputTuples.keySet()){
			maxtuple = Math.max(noOfOutputTuples.get(table), maxtuple);
		}
		return maxtuple;
	}

	public void setSampleDataJsonFilePath(String sampleDataJsonFilePath){
		this.sampleDataJsonFilePath = sampleDataJsonFilePath;
	}
	public String getSampleDataJsonFilePath(){
		return sampleDataJsonFilePath;
	}
	public MutationStructure getCurrentMutant(){
		return this.currentMutant ;
	}
	public void setCurrentMutant(MutationStructure mutant){
		this.currentMutant = mutant;
	}
	
}

