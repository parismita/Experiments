package killMutations.outerQueryBlock;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.ConstraintObject;
import util.TagDatasets.MutationType;
import util.TagDatasets.QueryBlock;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.z3.EnumSort;

public class ColumnReplacementMutations {
	
	private static Logger logger = Logger.getLogger(ColumnReplacementMutations.class.getName());
	
	
	public static void generateDataForkillingColumnReplacementMutationsInProjection(GenerateCVC1 cvc) throws Exception{
		try {
			
			/** Initialize the data structures for generating the data to kill this mutation */
			cvc.inititalizeForDatasetQs();
		
			/**set the type of mutation we are trying to kill*/
			cvc.setTypeOfMutation(MutationType.COLUMNREPLACEMENT, QueryBlock.NONE);
	
			/** get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation*/
			if(GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return ;
	
			/**Get the constraints for all the blocks of the query */ // changes made by sunanda
			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc,true); 

			
			/**Get the constraints for all the blocks of the query */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc));
			
			Vector<Node> projectedColumns = cvc.getqStructure().getProjectedCols();
			
			Map<Node, ArrayList<Column>> compatibleNodes = new HashMap<Node, ArrayList<Column>>();
			
			ArrayList<Node> candidateNodes = new ArrayList<Node>();
			
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" COLUMN REPLACEMENT"));
			logger.log(Level.INFO,ConstraintGenerator.addCommentLine(" COLUMN REPLACEMENT"));
			
			// Only non-string fields are considered here. The string fields are handled automatically by ensuring that enumeration has different values for
			// different fields in CVC.
			for(Node n: projectedColumns){
				if(n != null && n.getType()!= null && !n.getType().equals(Node.getAggrNodeType()) && n.getColumn() != null && n.getColumn().getDataType() != 12){
					candidateNodes.add(n);
					ArrayList<Column> columns = new ArrayList<Column>();						
					
					logger.log(Level.INFO,""+n.getColumn().getDataType());
					logger.log(Level.INFO,n.getTableNameNo());
					
					
					Vector<Table> tables = cvc.getTablesOfOriginalQuery();
					int size ;
					
					for(Table table: tables){
//						noOfColumn - 1 is done by Sunanda to ignore count column	
						if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true"))	
							size = table.getNoOfColumn()-1;
						else 
							size = table.getNoOfColumn();
//						end
						
						for(int j=0;j<size;j++){/**For each column of this table*/ // 
	
							/**Get this column */
							Column col = table.getColumn(j);
							
							if(col.getDataType() == n.getColumn().getDataType()){
								columns.add(col);
							}						
						}
					}
					
					compatibleNodes.put(n, columns);
				}
				else if(n!= null && n.getType() != null && n.getType().equals(Node.getBaoNodeType())){
					Node newNode = getBAONode(n);
					candidateNodes.add(newNode);
						
					ArrayList<Column> columns = new ArrayList<Column>();						
					
					logger.log(Level.INFO,""+newNode.getColumn().getDataType());
					logger.log(Level.INFO,newNode.getTableNameNo());
					
					
					Vector<Table> tables = cvc.getTablesOfOriginalQuery();
					int size ;
					for(Table table: tables){
//						noOfColumn - 1 is done by Sunanda to ignore count column	
						if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true"))	
							size = table.getNoOfColumn()-1;
						else 
							size = table.getNoOfColumn();
//						end
						
						for(int j=0;j<size;j++){/**For each column of this table*/ // noOfColumn - 1 is done by Sunanda to ignore count column
	
							/**Get this column */
							Column col = table.getColumn(j);
							
							if(col.getDataType() == newNode.getColumn().getDataType()){
								columns.add(col);
							}						
						}
					}
					
					compatibleNodes.put(newNode, columns);
				}
			}
			
			for(Node candidate : candidateNodes){
				String tableName = candidate.getTableNameNo();
				
				for(Map.Entry<String, Integer> e: cvc.getNoOfTuples().entrySet()){
					
					String table = e.getKey();
					if(table.contains("SQ")) //  added by Sunanda to ignore Subquery table from column replacement
						continue;
					Table requiredTable = null;				
					for(Table t: cvc.getTablesOfOriginalQuery()){
						//if(table.startsWith(t.getTableName())){
						if(table.startsWith(t.getTableName().toUpperCase())){ //added by rambabu
							requiredTable =t;
							
							break;
						}
					}
					int size ;
//					noOfColumn - 1 is done by Sunanda to ignore count column	
					if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true"))	
						size = requiredTable.getNoOfColumn()-1;
					else 
						size = requiredTable.getNoOfColumn();
//					end
					
					for(int j = 0; j < size; j++){/**For each column of this table*/ // noOfColumn - 1 is done by Sunanda to ignore count column
	
						/**Get this column */
						Column col = requiredTable.getColumn(j);
						
						if(col.getDataType() == candidate.getColumn().getDataType()){
							Node replacement = Node.createNode(col, requiredTable);
							replacement.setTableNameNo(table);
							
							if(replacement.equals(candidate)){
								
							}
							else{
								
								Boolean isFKRelation = false;
								
								for(int l=0; l < cvc.getForeignKeys().size(); l++){								
									/** In ForeignKeys Left points to the foreign key attribute while the right points to the Primary or the referenced column */
									Node fk = cvc.getForeignKeys().get(l);
									
									if((fk.getLeft().getTable() == candidate.getTable() && fk.getLeft().getColumn() == candidate.getColumn()) 
											&& (fk.getRight().getTable() == requiredTable && fk.getRight().getColumn() == col)){
										
										isFKRelation = true;
										break;
									}					
								}
								
								if(isFKRelation)
									continue;
								
								/**get the list of equivalence classes in this query block*/
								Vector<Vector<Node>> eqClasses = new Vector< Vector<Node>>();
								
								Boolean isPartOfEqClass = false;
								for(Vector<Node> ec : eqClasses){
									Vector<Node> temp = null;
									for(Node node : ec){
										if(node.equals(candidate)){
											temp = ec;
											break;
										}
									}
									
									if(temp != null){
										for(Node node : ec){
											if(node.equals(replacement)){
												isPartOfEqClass = true;
												break;
											}
										}
									}
									
									if(isPartOfEqClass)
										break;
								}
								
								if(isPartOfEqClass)
									continue;
	
								for(ConjunctQueryStructure con: cvc.getOuterBlock().getConjunctsQs())
									eqClasses.addAll(con.getEquivalenceClasses());
								
								String cond = ""; 
								Vector<String> conds = new Vector<String>();
								ArrayList <ConstraintObject> constrList = new ArrayList<ConstraintObject>();
								ConstraintGenerator constrGen = new ConstraintGenerator();
								
								for(int i = 1; i <= cvc.getNoOfTuples().get(tableName); i++){
									for(int k = 1; k <= cvc.getNoOfTuples().get(table); k++){
										ConstraintObject constrObj = new ConstraintObject();
										int offset1 = cvc.getRepeatedRelNextTuplePos().get(tableName)[1];
										int offset2 = cvc.getRepeatedRelNextTuplePos().get(table)[1];
										// if(Configuration.isEnumInt.equalsIgnoreCase("true")){
										// 	EnumSort currentSort = (EnumSort)ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
										// 	smtIndex1 = ctx.mkConst(cvc.enumIndexVar+Integer.toString(offset1),currentSort);
										// 	smtIndex2 = ctx.mkConst(cvc.enumIndexVar+Integer.toString(offset2),currentSort);

										// }
										constrObj.setLeftConstraint(constrGen.getMapNode(candidate,i + offset1-1 + ""));
										constrObj.setRightConstraint(constrGen.getMapNode(replacement, k + offset2-1 + ""));
										constrObj.setOperator(" /= ");
										String temp = constrGen.getAssertConstraint(cvc,candidate.getColumn(),(i + offset1 - 1), replacement.getColumn(), (k + offset2 - 1), " /= ");
										
										if(Configuration.getProperty("cntFlag").equalsIgnoreCase("false")) {
											temp += "ASSERT ("+GenerateCVCConstraintForNode.cvcMapNode(candidate, Integer.toString(i + offset1 - 1)) +" /= " +GenerateCVCConstraintForNode.cvcMapNode(replacement, Integer.toString(k + offset2 - 1)) +");\n";
											cond += temp;
											conds.add(temp);
										}
										if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
											if(temp.contains("assert")) {
												temp = temp.split("assert")[1].trim();
												temp = temp.substring(0,temp.lastIndexOf(")")).trim();									
											}
											cond += temp;
											conds.add(temp);
											temp = constrGen.getConstraintsForValidCountUsingTable(candidate.getTable(), tableName, i+offset1-1 , 0).toString() + "\n\n";
											cond += temp;
											conds.add(temp);
											temp = constrGen.getConstraintsForValidCountUsingTable(replacement.getTable(), tableName, k+offset2-1 , 0).toString() + " \n\n";
											cond += temp;
											conds.add(temp);
											cond = "(assert (and " + cond + "))\n\n";
										}
									}
								}
						
								
								logger.log(Level.INFO,replacement.toString());
								logger.log(Level.INFO,candidate.toString());
								logger.log(Level.INFO,cond.toString());
								
								cvc.getConstraints().add(cond);
							}
						}						
					}
				}
			}
			
			cvc.getConstraints().add(ConstraintGenerator.addCommentLine(" END OF COLUMN REPLACEMENT"));
			logger.log(Level.INFO,"\n"+ConstraintGenerator.addCommentLine("END OF COLUMN REPLACEMENT"));
			
			/** Call the method for the data generation*/
			GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc, true);
	}catch (TimeoutException ex){
		logger.log(Level.SEVERE,ex.getMessage(),ex);		
		throw ex;
	}
	catch(Exception ex){
		logger.log(Level.SEVERE,ex.getMessage(), ex);
		//ex.printStackTrace();
		throw ex;
	}
	}
	
	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static Node getBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft();	
		}
		else {
			if(n1.getLeft() != null){
				return getBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getBAONode(n1.getRight());
			}
		}
		return null;
		
	}
	
}
