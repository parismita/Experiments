package generateConstraints;

import java.util.*;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;

import parsing.Column;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.ConstraintObject;

/**
 * This class contains methods to generate constraints for the given group by nodes of the given query block
 * @author mahesh
 *
 */
public class GenerateGroupByConstraints {

	
	
	public static String getGroupByConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock) throws Exception{

		return getGroupByConstraints(cvc, queryBlock.getGroupByNodes(), true, queryBlock.getNoOfGroups());

	}
	
	public static String getGroupByConstraints(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n) throws Exception{

		return getGroupByConstraintsWithExtraColumn(cvc, queryBlock.getGroupByNodes(), n, queryBlock.getNoOfGroups());
	}
	
	
	/**
	 * Generate the constraints for the group by nodes of the query block
	 * Generates constraints for the multiple groups 
	 * @param cvc
	 * @param queryBlock
	 * @param groupByNodes
	 * @param diffgroup: INdicates whether to return the constraints for the multiple groups
	 * @param noOfGroups
	 * @return
	 */
	public static String getGroupByConstraints(GenerateCVC1 cvc,  ArrayList<Node> groupByNodes, boolean diffgroup, int noOfGroups)  throws Exception {


		if(groupByNodes.size() == 0)
			return "";

		/**constraints for different group, for distinct tuples across multiple groups*/
		String diffGroup = " ";
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		/**constraints for tuples in the same group*/
		String sameGroup = "";
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		/**This has to be repeated for the 'numGroups' times*/
		for(int i=1; i <= noOfGroups; i++){
			//if ( noOfGroups != 1 ) diffGroup += "ASSERT ";
			
			/**for each group by attribute*/
			for(int j=0; j<groupByNodes.size();j++){
				Column g = groupByNodes.get(j).getColumn();
				String t = g.getTableName();

				/** Get the table details */
				String tableNameNo = groupByNodes.get(j).getTableNameNo();
				int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
				//int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());
				int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu

				int count = cvc.getNoOfTuples().get(tableNameNo);
				int group = (i-1)*count;/**get group number*/
				ConstraintObject constObj = new ConstraintObject();
				for(int k=1; k<count;k++){/**  Generate constraints for the same group */
					BoolExpr sameG = constraintGen.ctx.mkEq(constraintGen.smtMap(g, constraintGen.ctx.mkInt(group+k-1+offset)), constraintGen.smtMap(g, constraintGen.ctx.mkInt(group+k+offset)));
					if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
						Column cntCol1 = g.getTable().getColumn(g.getTable().getNoOfColumn()-1); // added by sunanda for count
						BoolExpr WithCount1 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(group+k-1+offset)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
						BoolExpr WithCount2 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(group+k+offset)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
						BoolExpr andCountSameG = ConstraintGenerator.ctx.mkAnd(sameG,WithCount1, WithCount2);
						sameGroup += "(assert " + andCountSameG.toString() + ")";					
						}
					else
						sameGroup += constraintGen.getAssertConstraint(t, g, (group+k-1+offset),Index, t, g, Integer.valueOf(group+k+offset),Integer.valueOf(Index), "=");

					//sameGroup += "ASSERT O_"+t+"["+(group+k-1+offset)+"]."+Index+ " =  O_"+t+"["+(group+k+offset)+"]."+Index+"; \n";
				}

				/**Generate constraints for the different group*/
				if( noOfGroups != 1 && i != noOfGroups){
					Column cntCol1 = g.getTable().getColumn(g.getTable().getNoOfColumn()-1); // added by sunanda for count
					constObj.setLeftConstraint(constraintGen.getDistinctConstraint(t, g, ((i-1)*count + 1 + offset -1), Index, t, g,((i)*count + 1+ offset -1), Index));
						
					constrList.add(constObj);
						//diffGroup +=  " DISTINCT ( O_"+t+"["+((i-1)*count + 1 + offset -1)+"]."+Index+ ",  O_"+t+"["+((i)*count + 1+ offset -1)+"]."+Index+") OR ";
				}
				else if ( noOfGroups != 1 ){
						//diffGroup += " DISTINCT ( O_"+t+"["+(i*count+ offset -1)+"]."+Index+ ",  O_"+t+"["+(1  + offset -1)+"]."+Index+") OR ";
					Column cntCol1 = g.getTable().getColumn(g.getTable().getNoOfColumn()-1); // added by sunanda for count
					BoolExpr WithCount1 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(i*count+ offset -1)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
					BoolExpr WithCount2 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(1  + offset -1)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
					constObj.setLeftConstraint(constraintGen.getDistinctConstraint(t, g, (i*count+ offset -1), Index, t, g, (1  + offset -1), Index));
					constrList.add(constObj);
				}
				
			}
			diffGroup += constraintGen.generateOrConstraintsWithAssert(constrList);
	
		}

		
		if(diffgroup) return sameGroup + diffGroup;/**If constraint for distinct tuples across multiple groups is needed */
		else return sameGroup;
	}
	
	public static String getGroupByConstraintsWithExtraColumn(GenerateCVC1 cvc, ArrayList<Node> groupByNodes, Node extraNode, int noOfGroups){
		String constraint = "";
		
		String diffGroup = "";
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		if(groupByNodes.size() == 0)
			return constraint;
		
		for(int j=0; j<groupByNodes.size();j++){
			Column g = groupByNodes.get(j).getColumn();
			String t = g.getTableName();

			/** Get the table details */
			String tableNameNo = groupByNodes.get(j).getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			//int Index = cvc.getTableMap().getTable(t).getColumnIndex(g.getColumnName());
			int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu

//			int count = cvc.getNoOfTuples().get(tableNameNo) * noOfGroups;
			int count = cvc.getNoOfTuples().get(tableNameNo);

			int group = 0;/**get group number*/

			for(int k=1; k<=count;k++){/**  Generate constraints for the same group */ 
				BoolExpr sameG = constraintGen.ctx.mkEq(constraintGen.smtMap(g, constraintGen.ctx.mkInt(group+k-1+offset)), constraintGen.smtMap(g, constraintGen.ctx.mkInt(group+k+offset)));
				if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
					Column cntCol1 = g.getTable().getColumn(g.getTable().getNoOfColumn()-1); // added by sunanda for count
					BoolExpr WithCount1 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(group+k-1+offset)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
					BoolExpr WithCount2 =  ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(group+k+offset)), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
					BoolExpr andCountSameG = ConstraintGenerator.ctx.mkAnd(sameG,WithCount1, WithCount2);
					constraint += "(assert " + andCountSameG.toString() + ")";					
					}
				else
					constraint += constraintGen.getAssertConstraint(t, g, (group+k-1+offset),Index, t, g, Integer.valueOf(group+k+offset),Integer.valueOf(Index), "=");

			}
		}
		
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		/**This has to be repeated for the 'numGroups' times*/
		for(int i=1; i <= noOfGroups; i=i+2){
			//if ( noOfGroups != 1 ) diffGroup += "ASSERT ";
			
			/**for each group by attribute*/
			Column g = extraNode.getColumn();
			String t = g.getTableName();

			/** Get the table details */
			String tableNameNo = extraNode.getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNo)[1];
			int Index = cvc.getTableMap().getTable(t.toUpperCase()).getColumnIndex(g.getColumnName()); //added by rambabu 
			int count = cvc.getNoOfTuples().get(tableNameNo)/2;
			if(cvc.getNoOfTuples().get(tableNameNo) == 1)
				count = 1;
			int group = (i-1)*count;/**get group number*/

			for(int k=1; k<=count;k++){/**  Generate constraints for the same group */ 

				ConstraintObject constObj = new ConstraintObject();
				
				if( noOfGroups != 1 && i != noOfGroups){
					constObj.setLeftConstraint(constraintGen.getDistinctConstraint(t, g, ((i-1)*count + 1 + offset -1), Index, t, g,((i)*count + 1+ offset -1), Index));		
					constrList.add(constObj);
					}
				else if ( noOfGroups != 1 ){
						//diffGroup += " DISTINCT ( O_"+t+"["+(i*count+ offset -1)+"]."+Index+ ",  O_"+t+"["+(1  + offset -1)+"]."+Index+") OR ";
						constObj.setLeftConstraint(constraintGen.getDistinctConstraint(t, g, (i*count+ offset -1), Index, t, g, (1  + offset -1), Index));
						constrList.add(constObj);
				}
			
		}

			diffGroup += constraintGen.generateOrConstraintsWithAssert(constrList);
			
			/*if ( noOfGroups != 1 ){
				int lastIndex = diffGroup.lastIndexOf("OR");
				diffGroup = diffGroup.substring(0, lastIndex-1) + " ;\n ";
			}*/
		}
		
		return constraint + diffGroup;
	}
}
