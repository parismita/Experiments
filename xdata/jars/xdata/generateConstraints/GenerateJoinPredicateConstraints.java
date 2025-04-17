package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.EnumSort;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Sort;

import GenConstraints.GenConstraints;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.DisjunctQueryStructure;
import parsing.Node;
import parsing.QueryStructure;
import parsing.Table;
import parsing.correlationStructure;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.SubqueryStructure;
import util.Configuration;
import util.ConstraintObject;

/**
 * This class is used to generate constraints for the join predicates
 * The join predicates can be equi-join or non-equi join predicates
 * TODO: Handling join conditions which involve aggregations like SUM(A.x) = B.x
 * is part of future work
 * 
 * @author mahesh
 *
 */

public class GenerateJoinPredicateConstraints {

	private static Logger logger = Logger.getLogger(GenerateJoinPredicateConstraints.class.getName());

	private static boolean isTempJoin = false;
	private static Vector<String> tablesAdded = new Vector<String>();

	/**
	 * Constructor
	 */

	// added by deeksha : API call Context()
	// ----------------------------------------------------------------
	// public static Context ctx = new Context();
	// private static Solver solver = ctx.mkSolver();
	// private static HashMap<String, FuncDecl> ctxFuncDecls = new HashMap<String,
	// FuncDecl>(); // for storing Z3 context function declarations
	// static Solver dummySol = ctx.mkSolver();
	// ConstraintGenerator obj = new ConstraintGenerator();
	// --------------------------------------------------------------------
	public GenerateJoinPredicateConstraints() {

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			this.isTempJoin = true;
		} else {
			this.isTempJoin = false;
		}
	}

	public static String getConstraintsforEquivalenceClasses(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			ConjunctQueryStructure conjunct) throws Exception {
		String constraintString = "";
		if (queryBlock.getLevel() > 0)
			return constraintString;// parismita
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for (int k = 0; k < equivalenceClasses.size(); k++) {
			Vector<Node> ec = equivalenceClasses.get(k);
			for (int i = 0; i < ec.size() - 1; i++) {
				Node n1 = ec.get(i);
				Node n2 = ec.get(i + 1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins1(cvc, queryBlock, n1,
						n2);
			}
		}

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			isTempJoin = true;
		} else {
			isTempJoin = false;
		}
		/*
		 * if(isTempJoin) {
		 * String constr,declare="";
		 * int st_index=0,end_index=0;
		 * constr = constraintString;
		 * while(constr.indexOf("(declare-datatypes ()") != -1) {
		 * st_index = constr.indexOf("(declare-datatypes ()");
		 * end_index = constr.indexOf("_TupleType))")+12;
		 * if(!declare.contains(constr.substring(st_index, end_index)))
		 * declare += constr.substring(st_index, end_index) + " \n";
		 * constr = constr.substring(0, st_index)+constr.substring(end_index);
		 * }
		 * 
		 * constraintString = declare + constr;
		 * }
		 */

		return constraintString;
	}

	/**
	 * Get the constraints for equivalence Classes by Considering repeated relations
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2,
			String subqueryType) throws Exception {

		String constraintString = "";

		if (n1.getQueryType() == n2.getQueryType()) {
			/**
			 * If both nodes are of same type (i.e. either from clause sub qury nodes or
			 * where clause sub query nodes or outer query block nodes
			 */
			if (n1.getQueryIndex() != n2.getQueryIndex()) {/**
															 * This means these nodes correspond to two different from
															 * clause sub queries and are joined in the outer query
															 * block
															 */
				return getConstraintsForJoinsInDiffSubQueryBlocks(cvc, queryBlock, n1, n2, "=");
			} else {/** these are either correspond to from clause/ where clause/ outer clause */
				return getConstraintsForJoinsWithoutTemp(cvc, queryBlock, n1, n2, "=");
			}
		} else {/**
				 * This means one node correspond to from/Where clause sub query and other node
				 * correspond to outer query block
				 */
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(cvc, queryBlock, n1, n2, "=");
		}
	}

	/**
	 * Get the constraints for equivalence Classes by Considering repeated relations
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForEquiJoins1(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1, Node n2)
			throws Exception {

		String constraintString = "";

		if (n1.getQueryType() == n2.getQueryType()) {
			/**
			 * If both nodes are of same type (i.e. either from clause sub query nodes or
			 * where clause sub query nodes or outer query block nodes
			 */
			if (n1.getQueryIndex() != n2.getQueryIndex()) {/**
															 * This means these nodes correspond to two different from
															 * clause sub queries and are joined in the outer query
															 * block
															 */
				return getConstraintsForJoinsInDiffSubQueryBlocks1(cvc, queryBlock, n1, n2, "=");
			} else {/** these are either correspond to from clause/ where clause/ outer clause */
				return getConstraintsForJoinsInSameQueryBlock1(cvc, queryBlock, n1, n2, "=");
			}
		} else {/**
				 * This means one node correspond to from clause sub query and other node
				 * correspond to outer query block
				 */
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock1(cvc, queryBlock, n1, n2, "=");
		}
	}

	/**
	 * Wrapper method Used to generate constraints for the non equi join conditions
	 * of the conjunct
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param allConds
	 * @return
	 * @throws Exception
	 */

	public static String getConstraintsForNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Vector<Node> allConds, String fromwhere) throws Exception {

		String constraintString = "";
		for (Node n : allConds)
			constraintString += getConstraintsForNonEquiJoins(cvc, queryBlock, n.getLeft(), n.getRight(),
					n.getOperator(), fromwhere);
		return constraintString;
	}

	public static String getConstraintsForNonEquiJoinsTJ(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n,
			String fromwhere) throws Exception {

		String constraintString = "";
		constraintString += getConstraintsForNonEquiJoins(cvc, queryBlock, n.getLeft(), n.getRight(), n.getOperator(),
				fromwhere);
		return constraintString;
	}

	/**
	 * Wrapper method Used to generate constraints negative for the non equi join
	 * conditions of the conjunct
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param allConds
	 * @return
	 * @throws Exception
	 */
	public static String getNegativeConstraintsForNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Vector<Node> allConds) throws Exception {

		Vector<Node> allCondsDup = new Vector<Node>();

		for (Node node : allConds) {
			Node n = new Node(node);
			if (n.getOperator().equals("="))
				n.setOperator("/=");
			else if (n.getOperator().equals("/="))
				n.setOperator("=");
			else if (n.getOperator().equals(">"))
				n.setOperator("<=");
			else if (n.getOperator().equals("<"))
				n.setOperator(">=");
			else if (n.getOperator().equals("<="))
				n.setOperator(">");
			else if (n.getOperator().equals(">="))
				n.setOperator("<");
		}

		return getConstraintsForNonEquiJoins(cvc, queryBlock, allCondsDup, "");
	}

	/**
	 * Used to generate constraints for the non equi join conditions of the conjunct
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param left
	 * @param right
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node left,
			Node right, String operator, String subqueryType) throws Exception {

		if (queryBlock.getLevel() > 0)
			return "";
		if (left.getQueryType() == right.getQueryType()) {
			/**
			 * If both nodes are of same type (i.e. either from clause sub query nodes or
			 * where clause sub query nodes or outer query block nodes)
			 */
			if (left.getQueryIndex() != right.getQueryIndex()) {/**
																 * This means these nodes correspond to two different
																 * from clause sub queries and are joined in the outer
																 * query block
																 */
				return getConstraintsForJoinsInDiffSubQueryBlocks(cvc, queryBlock, left, right, operator);
			} else {/** these are either correspond to from clause/ where clause/ outer clause */
				// return getConstraintsForJoinsInSameQueryBlock(cvc, queryBlock, left, right,
				// operator, fromorwhere);
				return getConstraintsForJoinsInSameQueryBlock1(cvc, queryBlock, left, right, operator);

			}
		} else {/**
				 * This means one node correspond to from clause sub query and other node
				 * correspond to outer query block
				 */
			return getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(cvc, queryBlock, left, right, operator);
		}
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where one
	 * node is in outer query block and other node is in from clause sub query
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForEquiJoinsInSubQBlockAndOuterBlock(GenerateCVC1 cvc,
			QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {
		String constraintString = "";

		/** Let make n1 as sub query node and n2 as outer query node */
		if (n1.getQueryType() == 0) {
			Node temp = new Node(n1);
			n1 = new Node(n2);
			n2 = temp;
		}

		int leftGroup = 1;

		/** get number of groups for the from clause nested subquery block */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		/** get the details of each node */
		String t1 = getTableName(n1);
		String t2 = getTableName(n2);
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

		String r1 = getTableNameNo(n1);
		String r2 = getTableNameNo(n2);
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2 = 0;
		if (cvc.getNoOfTuples().containsKey(r1)) {
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if (cvc.getNoOfTuples().containsKey(r2)) {
			tuples2 = cvc.getNoOfTuples().get(r2);
		}
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

		/** Do a round robin for the smaller value of the group number */
		for (int k = 1, l = 1;; k++, l++) {
			// constraintString += "ASSERT ("+
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
			// ((k-1)*tuples1+offset1))+ operator +
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
			// (l+offset2-1))+");\n";

			/*
			 * ConstraintObject constrObj = new ConstraintObject();
			 * constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock,n1,
			 * ((k-1)*tuples1+offset1)));
			 * constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,
			 * n2, (l+offset2-1)));
			 * constrObj.setOperator(operator);
			 * constrObjList.add(constrObj);
			 */

			constraintString += constrGen.getAssertConstraint(
					constrGen.genPositiveCondsForPred(queryBlock, n1, ((k - 1) * tuples1 + offset1)), operator,
					constrGen.genPositiveCondsForPred(queryBlock, n2, (l + offset2 - 1)));

			if (leftGroup > tuples2) {
				if (l == tuples2 && k < leftGroup)
					l = 0;
				if (k >= leftGroup)
					break;
			} else if (leftGroup < tuples2) {
				if (l < tuples2 && k == leftGroup)
					k = 0;
				if (l >= tuples2)
					break;
			} else {// if tuples1==tuples2
				if (l == leftGroup)
					break;
			}
		}
		// constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where one
	 * node is in outer query block and other node is in from clause sub query
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForEquiJoinsInSubQBlockAndOuterBlock1(GenerateCVC1 cvc,
			QueryBlockDetails queryBlock, Node n1, Node n2, String operator) {
		String constraintString = "";

		/** Let make n1 as sub query node and n2 as outer query node */
		if (n1.getQueryType() == 0) {
			Node temp = new Node(n1);
			n1 = new Node(n2);
			n2 = temp;
		}

		int leftGroup = 1;

		/** get number of groups for the from clause nested subquery block */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		/** get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2 = 0;
		if (cvc.getNoOfTuples().containsKey(r1)) {
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if (cvc.getNoOfTuples().containsKey(r2)) {
			tuples2 = cvc.getNoOfTuples().get(r2);
		}

		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

		/** Do a round robin for the smaller value of the group number */
		for (int k = 1, l = 1;; k++, l++) {
			// constraintString += "("+
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
			// ((k-1)*tuples1+offset1))+ operator +
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
			// (l+offset2-1))+") AND ";
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(
					constrGen.genPositiveCondsForPred(queryBlock, n1, ((k - 1) * tuples1 + offset1)));
			constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock, n2, (l + offset2 - 1)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);
			if (leftGroup > tuples2) {
				if (l == tuples2 && k < leftGroup)
					l = 0;
				if (k >= leftGroup)
					break;
			} else if (leftGroup < tuples2) {
				if (l < tuples2 && k == leftGroup)
					k = 0;
				if (l >= tuples2)
					break;
			} else {// if tuples1==tuples2
				if (l == leftGroup)
					break;
			}
		}
		constraintString = constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	/**
	 * heuristics for multiplicity
	 * 
	 * @author parismita
	 * @param cvc
	 * @param conds
	 * @param tables
	 * @return
	 * @throws Exception
	 */
	public static int getOptimalTupleCountForJoinTable(GenerateCVC1 cvc, Vector<Node> conds, Vector<Table> tables)
			throws Exception {
		int tuplesInJoinTable = 1, tupleNo = 0;
		HashMap<Table, Integer> multiplicity = new HashMap<Table, Integer>();

		for (Table cvcTable : tables) {
			tupleNo = cvc.getNoOfOutputTuples(cvcTable.getTableName());
			if (cvcTable.getTableName().startsWith("JSQ") || cvcTable.getTableName().startsWith("GSQ"))
				tupleNo = 1;
			multiplicity.put(cvcTable, tupleNo);
		}
		// for every table 1 and 2 -> for every pkey1 -> check nodes for condition,
		// maintain a flag and remove node on cond sat
		// flag array int = case 1:const 1:pkey/npkey table1&2 3:pkey/pkey table1&2
		// 4:npkey/pkey table1&2

		// build a map <pair<table, table>, vector<nodes>>
		// traverse the map for every table, check if col is pkey if yes
		for (int i = 0; i < tables.size(); i++) {
			Table t1 = tables.get(i);
			// case1
			int pkey1[] = new int[t1.getPrimaryKey().size()];// 0-1 bool
			int flag = 1;
			for (int k = 0; k < t1.getPrimaryKey().size(); k++) {
				for (Node node : conds)
					if (node.getLeft() != null && t1.getPrimaryKey().get(k) == node.getLeft().getColumn()
							&& node.getRight().getType() == Node.getValType())
						pkey1[k] = 1;
			}
			for (int pk : pkey1)
				if (pk != 1)
					flag = 0;
			if (flag == 1) {
				multiplicity.put(t1, 1);
				continue;
			}
			// case 1 end

			for (int j = i + 1; j < tables.size(); j++) {// each pair of table compared once
				Table t2 = tables.get(j);
				int pkey2[] = new int[t2.getPrimaryKey().size()];
				pkey1 = new int[t1.getPrimaryKey().size()];
				int flag1 = 1, flag2 = 1;
				for (int k1 = 0; k1 < t1.getPrimaryKey().size(); k1++) {
					for (int k2 = 0; k2 < t2.getPrimaryKey().size(); k2++) {
						for (Node node : conds) {
							if (node.getOperator() != "=")
								continue;
							if (node.getLeft() != null && node.getLeft().getType() == Node.getColRefType()
									&& node.getRight() != null && node.getRight().getType() == Node.getColRefType()) {
								if ((t1.getPrimaryKey().get(k1) == node.getLeft().getColumn()
										&& t2 == node.getRight().getTable())
										||
										(t1.getPrimaryKey().get(k1) == node.getRight().getColumn()
										&& t2.getTableName().equalsIgnoreCase(node.getLeft().getTable().getTableName()))
										|| (t2 == node.getLeft().getTable()
												&& t1.getPrimaryKey().get(k1) == node.getRight().getColumn())) {
									pkey1[k1] = 1;
								}
								if ((t2.getPrimaryKey().get(k2) == node.getLeft().getColumn()
										&& t1 == node.getRight().getTable())
										||
										(t2.getPrimaryKey().get(k2) == node.getRight().getColumn()
										&& t1.getTableName().equalsIgnoreCase(node.getLeft().getTable().getTableName()))
										|| 
										(t1 == node.getLeft().getTable()
												&& t2.getPrimaryKey().get(k2) == node.getRight().getColumn())
									) 
									{
										pkey2[k2] = 1;
									}
							}
						}
					}
				}
				for (int pk : pkey1)
					if (pk != 1)
						flag1 = 0;
				for (int pk : pkey2)
					if (pk != 1)
						flag2 = 0;
				if (flag1 == 1 && flag2 == 1) {
					if (cvc.getNoOfOutputTuples(t1.getTableName()) >= cvc.getNoOfOutputTuples(t2.getTableName()))
						multiplicity.put(t1, 1);
					else
						multiplicity.put(t2, 1);
				} else if (flag1 == 1) {
					multiplicity.put(t1, 1);
				} else if (flag2 == 1) {
					multiplicity.put(t2, 1);
				}
			}

		}
		// case 4
		for (Node node : conds) {
			if (node.getOperator() != "=")
				continue;
			if (node.getLeft() != null && node.getLeft().getTable() != null
					&& node.getLeft().getType() == Node.getColRefType() && node.getRight() != null
					&& node.getRight().getTable() != null && node.getRight().getType() == Node.getColRefType()) {
				if (multiplicity.get(node.getLeft().getTable()) != null
						&& multiplicity.get(node.getRight().getTable()) != null
						&& multiplicity.get(node.getLeft().getTable()) != 1 && multiplicity
								.get(node.getLeft().getTable()) <= multiplicity.get(node.getRight().getTable())) {
					multiplicity.put(node.getLeft().getTable(), 2);
				} else if (multiplicity.get(node.getRight().getTable()) != null
						&& multiplicity.get(node.getLeft().getTable()) != null
						&& multiplicity.get(node.getRight().getTable()) != 1
						&& multiplicity.get(node.getLeft().getTable()) >= multiplicity.get(node.getRight().getTable()))
					multiplicity.put(node.getRight().getTable(), 2);
			}
		}
		// case 4 end
		tuplesInJoinTable = 1;
		for (Table cvcTable : tables) {
			tuplesInJoinTable = tuplesInJoinTable * multiplicity.get(cvcTable);
		}
		if(tuplesInJoinTable > 16){ // sunanda - heuristic
			return 16;
		}
		return tuplesInJoinTable;
	}

	public static List<List<Integer>> generateDomains(List<Integer> ks) {
		List<List<Integer>> domains = new ArrayList<>();
		for (Integer k : ks) {
			List<Integer> domain = new ArrayList<>();
			for (int i = 1; i <= k; i++) {
				domain.add(i);
			}
			domains.add(domain);
		}
		return domains;
	}

	public static List<List<Integer>> generateCombinations(List<List<Integer>> domains) {
		List<List<Integer>> combinations = new ArrayList<>();
		generateCombinationsRecursive(domains, 0, new ArrayList<>(), combinations);
		return combinations;
	}

	private static void generateCombinationsRecursive(List<List<Integer>> domains, int index,
			List<Integer> currentCombination, List<List<Integer>> combinations) {
		if (index == domains.size()) {
			combinations.add(new ArrayList<>(currentCombination));
			return;
		}

		for (Integer value : domains.get(index)) {
			currentCombination.add(value);
			generateCombinationsRecursive(domains, index + 1, currentCombination, combinations);
			currentCombination.remove(currentCombination.size() - 1);
		}
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions which are in
	 * same query block
	 * 
	 * @author parismita, sunanda
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForJoinsInSameQueryBlockWithCount(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Node n1, Node n2, String operator, boolean isExist, String subqueryType) throws Exception {

		String constraintString = "";
		Vector<Table> tables = new Vector<Table>();
		Vector<String> joinTables = new Vector<String>();
		Vector<Integer> tuplesInTable = new Vector<Integer>();
		String indexType = "Int";
		String enumIndexVar = "";
		Boolean primaryKeyMethod = false;
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			indexType = cvc.enumArrayIndex;
			enumIndexVar = cvc.enumIndexVar;
		}
		int level = queryBlock.getLevel();
		int tuplesInJoinTable = 1, tupleNo = 0;
		// need to check which SQ condition it is in its level -

		for (String baseRelations : queryBlock.getBaseRelations()) {
			Table cvcTable = cvc.getTableMap().getTable(baseRelations.replaceAll("\\d", "").toUpperCase());
			if (cvcTable == null) {
				// System.out.println(cvc.aliasMappingToLevels.get(baseRelations.replaceAll("\\d",
				// "").toUpperCase().toLowerCase()).repSQTable);;
				cvcTable = cvc.getTableMap().getSQTableByName(
						cvc.aliasMappingToLevels.get(baseRelations.replaceAll("\\d", "").toLowerCase()).repSQTable);
			}
			if (!tables.contains(cvcTable)) {
				tables.add(cvcTable);
				joinTables.add(cvcTable.getTableName());
				tupleNo = cvc.getNoOfOutputTuples(cvcTable.getTableName());
				tuplesInTable.add(tupleNo);
				tuplesInJoinTable = tuplesInJoinTable * tupleNo;// INIT
			}
		}
		for (String baseSQRelations : queryBlock.getBaseSQRelations()) {
			Table cvcTable = cvc.getTableMap().getSQTableByName(baseSQRelations.toUpperCase());
			if (cvcTable != null && !tables.contains(cvcTable)) {
				tables.add(cvcTable);
				joinTables.add(cvcTable.getTableName());
				tupleNo = cvc.getNoOfOutputTuples(cvcTable.getTableName());
				tuplesInTable.add(tupleNo);
			}
		}

		// int numberOfTuplesInJoin = 0;
		// if(Configuration.getProperty("tempJoins").equalsIgnoreCase("true") &&
		// checkIfTablesArePartOfSubQuery(cvc, joinTables, queryBlock.getLevel(),
		// subqueryType)){

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			ConstraintGenerator constrGen = new ConstraintGenerator();
			String joinTable = "JSQ" + String.valueOf(level);

			if(cvc.getqStructure().isSetOp == true && queryBlock.setNo==0)
			{
				joinTable = "JSQL" + String.valueOf(level);	
			}
			else if(cvc.getqStructure().isSetOp == true && queryBlock.setNo==1)
			{
				joinTable = "JSQR" + String.valueOf(level);	
			}

			if (tablesAdded.contains(joinTable)) {
				return "";
			}
			// *********************added by sunanda - end********************

			// pushing tables down for not exist correlation
			pushTablesFromAboveLevelsForCorrelation(cvc, tables, level, joinTables, tuplesInTable);

			HashMap<String, Object> jtColumnNamesAndDataTypes = createTempTableColumnsForJoins(cvc, joinTable, tables,
					isExist, subqueryType, level);

			cvc.jtObj.put(joinTable,jtColumnNamesAndDataTypes);


			ArrayList<String> jtColumns = (ArrayList<String>) jtColumnNamesAndDataTypes.get("Names");
			ArrayList<Sort> jtColumnDataTypes = (ArrayList<Sort>) jtColumnNamesAndDataTypes.get("DataTypes");
			ArrayList<String> primaryKeyOfJoinTable = (ArrayList<String>) jtColumnNamesAndDataTypes.get("PrimaryKey");
			if (cvc.getTableMap().getSQTables() == null)
				cvc.getTableMap().setSQTables((Map<String, Table>) jtColumnNamesAndDataTypes.get("TableMap"));
			else
				cvc.getTableMap().putSQTables((Map<String, Table>) jtColumnNamesAndDataTypes.get("TableMap"));
			Table sqTable = cvc.getTableMap().getSQTableByName(joinTable);

			// SubqueryStructure sqs = new SubqueryStructure();
			// sqs.SQTableName.put(joinTable, s);
			// sqs.SQjoinWithEXISTS.put(joinTable, true);
			// sqs.SQColumns.put(joinTable, jtColumns);

			// if (subqueryType.equalsIgnoreCase("from")) {
			// cvc.subqueryConstraintsMap.put("from", sqs);
			// } else if (subqueryType.equalsIgnoreCase("outer")) {
			// cvc.subqueryConstraintsMap.put("outer", sqs);
			// } else {
			// sqs.SQjoinWithEXISTS.put(joinTable, isExist);
			// cvc.subqueryConstraintsMap.put("where", sqs);
			// }

			// testcode by deeksha--------------------------------------------------
			String tempConstraints = "";
			Expr aex = ConstraintGenerator.putTableInCtx(cvc,
					jtColumns.toArray(new String[jtColumns.size()]),
					jtColumnDataTypes.toArray(new Sort[jtColumnDataTypes.size()]),
					joinTable); // will be stored table1's cols with count then table2's then 3's - parismita

			for (String statement : ConstraintGenerator.declareRelation(aex)) {
				if (statement.contains(joinTable + "_TupleType"))
					tempConstraints += statement;
			}

			// System.out.printldn(declareRelation(aex));
			// step 2 : sq1 and sq2 functions for matching join relation with original

			int joincol = 0;
			for (int i = 0; i < tables.size(); i++) {//
				HashMap<String, Column> columns = tables.get(i).getColumns();
				if ((tables.get(i).getTableName().contains("JSQ") || tables.get(i).getTableName().contains("GSQ")
						|| tables.get(i).getTableName().contains("DSQ"))
						&& !tables.get(i).getSQType().equalsIgnoreCase("from")) { // check if table is not from clause
																					// subquery
					HashMap<String, Column> columnsNeedMapping = getRequiredMappingFromSubquery(cvc, tables.get(i),
							level);
					if (columnsNeedMapping.size() == 0)
						continue;
					else {
						columns = columnsNeedMapping;
					}
				}
				Vector<String> columnString = new Vector<String>(columns.keySet());
				String fun = ("define-fun " + joinTable + "_map_" + joinTables.get(i) + "((x!0 " + indexType + ") (x!1 "
						+ indexType + ")) Bool\n");

				tempConstraints += "\n(" + fun + "(and \n";
				int col1 = 0;
				while (col1 < columns.size()) {
					Expr left = ConstraintGenerator.genSelectTest2(cvc, joinTables.get(i),
							tables.get(i).getColumnIndex(columnString.get(col1)), "x!0");
					Expr right = ConstraintGenerator.genSelectTest2(cvc, joinTable, joincol, "x!1");
					Expr eq = ConstraintGenerator.ctx.mkEq(left, right);
					tempConstraints += eq.toString() + "\n";
					col1++;
					joincol++;
				}
				tempConstraints += ")\n)\n";
			}

			// selection and join conditions from subquery :POOJA
			// String constr = "";
			String strConstr = "";
			// HashMap<Node, String> condToSolvedStrinHashMap = new HashMap<Node, String>();
			Vector<Node> selectionAndJoinConds = new Vector<Node>();
			Vector<Node> stringConds = new Vector<Node>();
			Vector<Node> inConds = new Vector<Node>();

			// tuples count - parismita
			Vector<Node> CondForCount = new Vector<>();

			CondForCount.addAll(queryBlock.getConjunctsQs().get(0).getSelectionConds());
			CondForCount.addAll(queryBlock.getConjunctsQs().get(0).getJoinCondsForEquivalenceClasses());
			CondForCount.addAll(queryBlock.getConjunctsQs().get(0).getJoinCondsAllOther());
			CondForCount.addAll(queryBlock.getConjunctsQs().get(0).getStringSelectionConds());

			// CondForCount.addAll(selectionAndJoinConds);
			// CondForCount.addAll(stringConds);
			tuplesInJoinTable = getOptimalTupleCountForJoinTable(cvc, CondForCount, tables);
			if (cvc.noOfOutputTuplesContains(joinTable))
				tuplesInJoinTable = Math.max(tuplesInJoinTable, cvc.getNoOfOutputTuples(joinTable));

			// power 2 hueristic - sunanda
			int base = 2;
			// base^tables.size();
			// hueristic for #tuples > 10
			int baseTableSize = 0;
			for (int i = 0; i < tables.size(); i++) {
				if (joinTables.size() == 1) {
					baseTableSize++;
				} else if (joinTables.get(i).contains("JSQ") && !tables.get(i).getSQType().equalsIgnoreCase("from")
						|| joinTables.get(i).contains("GSQ") || joinTables.get(i).contains("DSQ"))
					continue;
				else
					baseTableSize++;
			}

			int updatedTuplesInJoinTable = (int) Math.round(Math.pow(base, baseTableSize));
			// updatedTuplesInJoinTable = Math.max(updatedTuplesInJoinTable,
			// cvc.getNoOfOutputTuples(joinTable));
			tuplesInJoinTable = Math.max(tuplesInJoinTable, cvc.getNoOfMaxOutputTuples(joinTable));
			updatedTuplesInJoinTable = Math.max(Math.min(updatedTuplesInJoinTable, tuplesInJoinTable),
					cvc.getNoOfMaxOutputTuples(joinTable));
			if (tuplesInJoinTable > 10)
				tuplesInJoinTable = updatedTuplesInJoinTable;

			if (tuplesInJoinTable < 4) {
				if (cvc.getCurrentMutant() != null && (cvc.getCurrentMutant().getMutationTypeNumber() == 7
						|| cvc.getCurrentMutant().getMutationTypeNumber() == 8
						|| cvc.getCurrentMutant().getMutationTypeNumber() == 2)) {
					tuplesInJoinTable = tuplesInJoinTable + 3;
				}
				if (cvc.getCurrentMutant() != null && (cvc.getCurrentMutant().getMutationTypeNumber() == 5)) {
					tuplesInJoinTable = updatedTuplesInJoinTable;
				}
			}
			// hueristic commented for testing


			cvc.putNoOfOutputTuples(joinTable, tuplesInJoinTable);
			// test code for count starts - sunanda

			cvc.getNoOfTuples().put(joinTable, tuplesInJoinTable);

			// tuples count - parismita

			// step 1.5 : for loop for asserting count values for join subquery table
			Vector<String> cntColumnNames = new Vector<String>();
			// for (String col : jtColumns) {
			// if (col.split("__")[1].contains("XDATA_CNT")) {
			// cntColumnNames.add(col);
			// }
			// }
			cntColumnNames = (Vector<String>) jtColumnNamesAndDataTypes.get("CountColumns");
			Vector<ArithExpr> cntOfBaseColumnSQVector = new Vector<ArithExpr>();
			for (int i = 0; i < cntColumnNames.size() - 1; i++) {
				// || (cntColumnNames.get(i).contains("JSQ") )
				String tableTemp = cvc.getTableMap().getSQTableByName(joinTable).getColumn(cntColumnNames.get(i))
						.getBaseRelation();

				if (cntColumnNames.get(i).contains("JSQ") && cvc.getTableMap().getSQTableByName(tableTemp) != null
						&& cvc.getTableMap().getSQTableByName(tableTemp).getTableName().contains("JSQ")
						&& cvc.getTableMap().getSQTableByName(tableTemp).getSQType().equalsIgnoreCase("from")) {
					// System.out.println(cvc.getTableMap().getSQTableByName(joinTable).getColumn(cntColumnNames.get(i)));
					cntOfBaseColumnSQVector.add((ArithExpr) ConstraintGenerator.genSelectTest2(cvc, joinTable,
							jtColumns.indexOf(cntColumnNames.get(i)), "i1"));
					// continue;
				} else if (cntColumnNames.get(i).split("JSQ").length > 2
						|| cntColumnNames.get(i).split("GSQ").length > 2
						|| cntColumnNames.get(i).split("DSQ").length > 2) {
					continue;
				} else
					cntOfBaseColumnSQVector.add((ArithExpr) ConstraintGenerator.genSelectTest2(cvc, joinTable,
							jtColumns.indexOf(cntColumnNames.get(i)), "i1"));
			}
			ArithExpr[] cntOfBaseColumnSQ = new ArithExpr[cntOfBaseColumnSQVector.size()];
			for (int i = 0; i < cntOfBaseColumnSQVector.size(); i++) {
				cntOfBaseColumnSQ[i] = cntOfBaseColumnSQVector.get(i);
			}
			Expr cntOfSQColumnSQ = ConstraintGenerator.genSelectTest2(cvc, joinTable,
					jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), "i1");
			Expr[] andExprTemp = new Expr[3];
			andExprTemp[0] = ConstraintGenerator.ctx.mkGe((ArithExpr) cntOfSQColumnSQ,
					(ArithExpr) ConstraintGenerator.ctx.mkInt(0));
			andExprTemp[1] = ConstraintGenerator.ctx.mkLe((ArithExpr) cntOfSQColumnSQ,
					(ArithExpr) ConstraintGenerator.ctx.mkInt(tuplesInJoinTable));
			Expr multipExprOfCounts = ConstraintGenerator.ctx.mkMul(cntOfBaseColumnSQ);
			andExprTemp[2] = ConstraintGenerator.ctx.mkOr(
					ConstraintGenerator.ctx.mkEq(cntOfSQColumnSQ, multipExprOfCounts),
					ConstraintGenerator.ctx.mkEq(cntOfSQColumnSQ, ConstraintGenerator.ctx.mkInt(0)));
			Expr andExprForCountLimit = ConstraintGenerator.ctx.mkAnd((BoolExpr) andExprTemp[0],
					(BoolExpr) andExprTemp[1], (BoolExpr) andExprTemp[2]);
			tempConstraints += "\n(assert (forall ((i1 " + indexType + ")) \n\t" + andExprForCountLimit + "))\n";
			constraintString += tempConstraints;
			// end 1.5 step

			// step 1.75: Primary key for SQ table
			Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, joinTable,
					jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), "i1");
			Expr cntOfSQColumnSQj1 = ConstraintGenerator.genSelectTest2(cvc, joinTable,
					jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), "j1");
			Expr orExprTempForPK = ConstraintGenerator.ctx.mkOr(
					ConstraintGenerator.ctx.mkEq((ArithExpr) cntOfSQColumnSQi1,
							(ArithExpr) ConstraintGenerator.ctx.mkInt(0)),
					ConstraintGenerator.ctx.mkEq((ArithExpr) cntOfSQColumnSQj1,
							(ArithExpr) ConstraintGenerator.ctx.mkInt(0)));
			BoolExpr[] andexprConstraintsTemp = new BoolExpr[primaryKeyOfJoinTable.size() + 1];
			int p = 0;
			for (int i = 0; i < primaryKeyOfJoinTable.size(); i++) {
				Expr selecti1 = ConstraintGenerator.genSelectTest2(cvc, joinTable,
						jtColumns.indexOf(primaryKeyOfJoinTable.get(i)), "i1");
				;
				Expr selectj1 = ConstraintGenerator.genSelectTest2(cvc, joinTable,
						jtColumns.indexOf(primaryKeyOfJoinTable.get(i)), "j1");
				;
				andexprConstraintsTemp[p] = (BoolExpr) ConstraintGenerator.ctx.mkEq(selecti1, selectj1);
				p++;
			}
			andexprConstraintsTemp[p] = ConstraintGenerator.ctx.mkNot(ConstraintGenerator.ctx.mkEq(
					(Expr) ConstraintGenerator.ctx.mkIntConst("i1"), (Expr) ConstraintGenerator.ctx.mkIntConst("j1")));
			Expr andExprTempForPK = ConstraintGenerator.ctx.mkAnd(andexprConstraintsTemp);
			String pkconstUsingNeq = "";
			if (primaryKeyMethod == true) {
				ArrayList<Column> primaryKeycols = new ArrayList<>();
				for (String a : primaryKeyOfJoinTable) {
					primaryKeycols.add(cvc.getTableMap().getSQTableByName(joinTable).getColumn(a));
				}
				pkconstUsingNeq = AddDataBaseConstraints.getPrimaryKeyConstUsingNonEquiMethod(primaryKeycols,
						tuplesInJoinTable);

			}
			if (pkconstUsingNeq.equalsIgnoreCase("")) {
				tempConstraints = "\n(assert (forall ((i1 " + indexType + ") (j1 " + indexType + ")) \n\t (=> \n\t"
						+ andExprTempForPK.toString()
						+ "\n\t\t" + orExprTempForPK + ")))\n\n";
			} else {
				if (pkconstUsingNeq.contains("select"))
					tempConstraints = "\n(assert \n" + pkconstUsingNeq + "\n)";
				else
					tempConstraints = "";
			}

			constraintString += tempConstraints;

			// test code for count ends
			// count assert
			// && correlationConds.size() == 0
			List<Integer> ks = new ArrayList<>(); // Define the list of k's for each element
			for (Table table : tables) {
				String tablename = table.getTableName();
				ks.add(cvc.getNoOfOutputTuples(tablename));
			}
			int n = ks.size(); // Number of elements

			// Generate the domains for each element
			List<List<Integer>> domains = generateDomains(ks);

			// Generate all combinations
			List<List<Integer>> combinations = generateCombinations(domains);

			// Print the combinations
			// for (List<Integer> combination : combinations) {
			// System.out.println(combination);
			// }

			String constr = "";
			Vector<Node> correlationConds = new Vector<>();
			int cnt = 0;
			boolean isCorrCondPresent = false;
			String unrolldFor = "";
			for (List<Integer> indList : combinations) {
				// System.out.println(indList);
				Map<String, Object> returnVal = getForwardPassJoinandSelectionConstraints(cvc, queryBlock,
						queryBlock.getConjunctsQs().get(0), sqTable, joinTables,
						queryBlock.getConjunctsQs().get(0).getSelectionConds(),
						queryBlock.getConjunctsQs().get(0).getJoinCondsForEquivalenceClasses(),
						queryBlock.getConjunctsQs().get(0).getJoinCondsAllOther(),
						queryBlock.getConjunctsQs().get(0).getStringSelectionConds(),
						queryBlock.getConjunctsQs().get(0).getInClauseConds(),
						queryBlock.getConjunctsQs().get(0).getIsNullConds(), true, indList, cnt,isExist);
				isCorrCondPresent = (Boolean) returnVal.get("isCorrCondPresent");
				correlationConds = (Vector<Node>) returnVal.get("correlationConds");
				constr += (String) returnVal.get("constr");

				isCorrCondPresent = (Boolean) returnVal.get("isCorrCondPresent");

				if (cnt == 0 && (isExist || (!isExist)) && (level == 0 || !isCorrCondPresent)) {
					String OrExprForCount = "\n(assert\n\t(or ";

					if (!isExist && !isCorrCondPresent)
						OrExprForCount = "\n(assert\n\t(and ";
					for (int j = 1; j <= tuplesInJoinTable; j++) {
						Expr cntOfSQColumnSQforAllTuples = ConstraintGenerator.genSelectTest(cvc, joinTable,
								jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), j);

						
						Expr greaterCnt = ConstraintGenerator.ctx.mkGt((ArithExpr) cntOfSQColumnSQforAllTuples,
								(ArithExpr) ConstraintGenerator.ctx.mkInt(0));

						if (isCorrCondPresent)
							greaterCnt = ConstraintGenerator.ctx.mkGt((ArithExpr) cntOfSQColumnSQforAllTuples,
									(ArithExpr) ConstraintGenerator.ctx.mkInt(0));
						else if (!isExist)
							greaterCnt = ConstraintGenerator.ctx.mkEq((ArithExpr) cntOfSQColumnSQforAllTuples,
									(ArithExpr) ConstraintGenerator.ctx.mkInt(0));

						OrExprForCount += "\n\t\t" + greaterCnt.toString();
					}
					OrExprForCount += "\t))";
					// constraintString += "(assert (exists ((i1 Int)) "+greaterCnt+"))\n";

					if(cvc.getqStructure().isSetOp==false)
					constraintString += OrExprForCount + "\n";
				}

				Boolean existsUnroll;
				if (Configuration.getProperty("existsUnrollFlag").equalsIgnoreCase("true")) {
					existsUnroll = true;
				} else {
					existsUnroll = false;
				}
				String andExpr = "";
				if (existsUnroll) {
					// Subquery table forward pass with unrolled exist
					String tableIter = "";

					String cntColumnsInBaseTableValid = "";
					// Writing valid tuples in base table for forward pass implies condition
					for (int i = 0; i < tables.size(); i++) {
						tableIter += "(" + joinTables.get(i) + "_i  " + indexType + ")";// (i1 Int)(j1 Int)
						if (joinTables.get(i).contains("JSQ") && !tables.get(i).getIsExist())
							cntColumnsInBaseTableValid += ConstraintGenerator.ctx.mkEq(
									ConstraintGenerator.genSelectTest(cvc, joinTables.get(i),
											tables.get(i).getNoOfColumn() - 1, indList.get(i)),
									ConstraintGenerator.ctx.mkInt(0)).toString();
						else
							cntColumnsInBaseTableValid += ConstraintGenerator.ctx.mkGt(
									ConstraintGenerator.genSelectTest(cvc, joinTables.get(i),
											tables.get(i).getNoOfColumn() - 1, indList.get(i)),
									ConstraintGenerator.ctx.mkInt(0)).toString();
					}
					Expr cntColumnInBaseTable = ConstraintGenerator.ctx.mkTrue();

					// Writing forward pass mapping constraints

					for (int j = 1; j <= tuplesInJoinTable; j++) {
						String tempStr = "", tableCond = "";
						for (int i = 0; i < tables.size(); i++) {
							if (joinTables.get(i).contains("JSQ") && !tables.get(i).getSQType().equalsIgnoreCase("from")
									|| joinTables.get(i).contains("GSQ") || joinTables.get(i).contains("DSQ")) {
								cntColumnInBaseTable = ConstraintGenerator.genSelectTest(cvc, joinTables.get(i),
										tables.get(i).getNoOfColumn() - 1, j);
								HashMap<String, Column> columnsNeedMapping = getRequiredMappingFromSubquery(cvc,
										tables.get(i), level);
								if (columnsNeedMapping.size() == 0)
									continue;
							}
							// tableCond += "(" + joinTable + "_map_" + joinTables.get(i) + " " +
							// joinTables.get(i) + "_i "
							// + enumIndexVar + j
							// + ")\n";// (SQ1_pred2 j1 k1)\n

							tableCond += "(" + joinTable + "_map_" + joinTables.get(i) + " " + indList.get(i) + " "
									+ enumIndexVar + j
									+ ")\n";// (SQ1_pred2 j1 k1)\n

						}
						Expr cntOfSQColumnSQjth = ConstraintGenerator.genSelectTest(cvc, joinTable,
								jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), j);

						tempStr += "\t"
								+ ConstraintGenerator.ctx
										.mkGt((ArithExpr) cntOfSQColumnSQjth, ConstraintGenerator.ctx.mkInt(0))
										.toString()
								+ "\n";// constraints for value of cnt > 0

						andExpr += "\n\t(and \n\t\t" + tempStr + "\t\t" + tableCond + "\t)";
					}
					// System.out.println(andExpr);
					constr += cntColumnsInBaseTableValid;
					if (constr != "")
						constr = "(and \n" + constr + "\t\t)";
					// constraintString += "\n(assert (forall (" + tableIter + ") \n\t"
					// + (constr != "" ? ("(=> " + constr) : "") + " ";
					// unrolldFor += "\n(assert (and \n\t"
					// + (constr != "" ? ("(=> " + constr) : "") + " ";
					unrolldFor = "\n" + (constr != "" ? ("(=> " + constr) + "\n(or" + andExpr : "") + "))";
					constr = "";
					constraintString += "\n(assert\t " + (unrolldFor + "\n)");

					// constr += ConstraintGenerator.ctx
					// .mkGt(ConstraintGenerator.ctx.mkIntConst(joinTable + "_i"),
					// ConstraintGenerator.ctx.mkInt(0))
					// .toString() + "\n";

					// constr +=
					// ConstraintGenerator.ctx.mkLe(ConstraintGenerator.ctx.mkIntConst(joinTable +
					// "_i"),

					// ConstraintGenerator.ctx.mkInt(tuplesInJoinTable)).toString() + "\n";
					// constr += ConstraintGenerator.ctx.mkGt(
					// (ArithExpr) ConstraintGenerator.genSelectTest2(cvc, joinTable,
					// jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), joinTable +
					// "_i"),
					// ConstraintGenerator.ctx.mkInt(0)).toString() + "\n";

				} else {
					// Subquery table forward pass using exist in smt
					String tableIter = "", tableCond = "";
					for (int i = 0; i < tables.size(); i++) {
						tableIter += "(" + joinTables.get(i) + "_i  " + indexType + ")";// (i1 Int)(j1 Int)
						if (joinTables.get(i).contains("JSQ") && !tables.get(i).getSQType().equalsIgnoreCase("from")
								|| joinTables.get(i).contains("GSQ") || joinTables.get(i).contains("DSQ")) {
							HashMap<String, Column> columnsNeedMapping = getRequiredMappingFromSubquery(cvc,
									tables.get(i),
									level);
							if (columnsNeedMapping.size() == 0)
								continue;
						}
						tableCond += "(" + joinTable + "_map_" + joinTables.get(i) + " " + joinTables.get(i) + "_i "
								+ joinTable + "_i)\n";// (SQ1_pred2 j1 k1)\n

						Expr cntColumnInBaseTable = ConstraintGenerator.genSelectTest2(cvc, joinTables.get(i),
								tables.get(i).getNoOfColumn() - 1, joinTables.get(i) + "_i");
						if (isExist == false) // sunanda for Not
						{
							constr += ConstraintGenerator.ctx
									.mkEq((ArithExpr) cntColumnInBaseTable, ConstraintGenerator.ctx.mkInt(0)).toString()
									+ "\n";// constraints for value of cnt = 0

						} else
							constr += ConstraintGenerator.ctx
									.mkGt((ArithExpr) cntColumnInBaseTable, ConstraintGenerator.ctx.mkInt(0)).toString()
									+ "\n";// constraints for value of cnt > 0
					}
					constraintString += "(assert (forall (" + tableIter + ") \n\t"
							+ (constr != "" ? ("(=> " + constr) : "")
							+ "  ";
					constraintString += "\n \t(exists ((" + joinTable + "_i " + indexType + ")) ";
					constr = "";
					constr += ConstraintGenerator.ctx
							.mkGt(ConstraintGenerator.ctx.mkIntConst(joinTable + "_i"),
									ConstraintGenerator.ctx.mkInt(0))
							.toString() + "\n";
					constr += ConstraintGenerator.ctx.mkLe(ConstraintGenerator.ctx.mkIntConst(joinTable + "_i"),
							ConstraintGenerator.ctx.mkInt(tuplesInJoinTable)).toString() + "\n";
					constr += ConstraintGenerator.ctx.mkGt(
							(ArithExpr) ConstraintGenerator.genSelectTest2(cvc, joinTable,
									jtColumns.indexOf(cntColumnNames.get(cntColumnNames.size() - 1)), joinTable + "_i"),
							ConstraintGenerator.ctx.mkInt(0)).toString() + "\n";

					constraintString += "\n\t (and \n" + constr + tableCond + "))\n)\n)" + (constr != "" ? "\n)" : "");
				}
				cnt++;

			}

			// ------------------forward pass------------

			// constraintString += "\n \t(exists (("+joinTable+"_i Int)) ";

			// testcode by deeksha---------------------

			// (> k1 0)
			// (<= k1 2)
			// (> (JSQ1__XDATA_CNT_k (select O_JSQ1 k1)) 0)

			// constraintString += "\n\t (or \n"+andExpr+"))\n)"+(constr!=""?"\n)":"");

			String tempBoundaries = "";
			tempBoundaries += ConstraintGenerator.ctx
					.mkGt(ConstraintGenerator.ctx.mkIntConst("i1"), ConstraintGenerator.ctx.mkInt(0)).toString() + "\n";
			tempBoundaries += ConstraintGenerator.ctx
					.mkLe(ConstraintGenerator.ctx.mkIntConst("i1"), ConstraintGenerator.ctx.mkInt(tuplesInJoinTable))
					.toString() + "\n";
			constr = "\t";

			constr += getBackwardPassJoinandSelectionConstraints(cvc, queryBlock,
					queryBlock.getConjunctsQs().get(0), sqTable, tables,
					queryBlock.getConjunctsQs().get(0).getSelectionConds(),
					queryBlock.getConjunctsQs().get(0).getJoinCondsForEquivalenceClasses(),
					queryBlock.getConjunctsQs().get(0).getJoinCondsAllOther(),
					queryBlock.getConjunctsQs().get(0).getStringSelectionConds(),
					queryBlock.getConjunctsQs().get(0).getInClauseConds(),
					queryBlock.getConjunctsQs().get(0).getIsNullConds(),
					jtColumns, joinTable, true);

			tempConstraints = "";
			for (int index = 0; index < tables.size(); index++) {
				String tempconstrTable1 = "";
				String a = joinTables.get(index);
				int k = index;
				for (int i = 1; i <= tuplesInTable.get(index); i++) {
					if (joinTables.get(index).contains("JSQ") && !tables.get(index).getSQType().equalsIgnoreCase("from")
							|| joinTables.get(index).contains("GSQ") || joinTables.get(index).contains("DSQ")) {
						HashMap<String, Column> columnsNeedMapping = getRequiredMappingFromSubquery(cvc,
								tables.get(index), level);
						if (columnsNeedMapping.size() == 0 && !isCorrCondPresent) {
							if (!tables.get(index).getIsExist())
								tempconstrTable1 += ConstraintGenerator.getConstraintsForInValidCountUsingTable(
										tables.get(index), tables.get(index).getTableName(), i, 0);
							else
								tempconstrTable1 += ConstraintGenerator.getConstraintsForValidCountUsingTable(
										tables.get(index), tables.get(index).getTableName(), i, 0);
							// cntValid +=
							// ConstraintGenerator.getConstraintsForValidCountUsingTable(n.getLeft().getTable(),
							// n.getLeft().getTable().getTableName(), Integer.parseInt(tupleIdLeft), 0);
							// continue;

						}
					} else {
						tempconstrTable1 += "\t\t(" + joinTable + "_map_" + joinTables.get(index) + " \t"
								+ enumIndexVar + Integer.toString(i) + "\t" + "i1" + ")\n";
					}

				}
				if (!tempconstrTable1.isEmpty())
					tempConstraints += "\t(or " + tempconstrTable1 + "\n\t)\n ";
			}

			if (Configuration.isEnumInt.equalsIgnoreCase("true"))
				tempBoundaries = "";
			cntOfSQColumnSQi1 = ConstraintGenerator.ctx.mkGt((ArithExpr) cntOfSQColumnSQ,
					(ArithExpr) ConstraintGenerator.ctx.mkInt(0));
			String finalConstraints = "\n" + constr + "\n" + tempConstraints + "  )\n  ";
			finalConstraints = "\n(assert (forall ((" + "i1 " + indexType + ")) \n (=> " + cntOfSQColumnSQi1
					+ "\n(and \n\t"
					+ tempBoundaries + "\n\n\t" + finalConstraints + "\n )\n))";
			constraintString += finalConstraints;

			// end------------

			ArrayList<String> typeCastedJt = new ArrayList<String>();
			for (Sort s : jtColumnDataTypes) {
				typeCastedJt.add(s.toString());
			}
			ArrayList<String> s = new ArrayList<String>();
			s.addAll(0, joinTables);

			SubqueryStructure sqs = new SubqueryStructure();
			sqs.SQTableName.put(joinTable, s);
			sqs.SQjoinWithEXISTS.put(joinTable, true);
			sqs.SQDefine.put(joinTable, constraintString);
			sqs.SQColumnsDataTypes.put(joinTable, typeCastedJt);
			sqs.SQColumns.put(joinTable, jtColumns);
			sqs.SQJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
			sqs.SQJoinCorrelationConds.put(joinTable, correlationConds);

			if (subqueryType.equalsIgnoreCase("from")) {
				cvc.subqueryConstraintsMap.put("from", sqs);
			} else if (subqueryType.equalsIgnoreCase("outer")) {
				cvc.subqueryConstraintsMap.put("outer", sqs);
			} else {
				sqs.SQjoinWithEXISTS.put(joinTable, isExist);
				cvc.subqueryConstraintsMap.put("where", sqs);
			}
			if(cvc.getqStructure().isSetOp==true)
			{
				cvc.subqueryConstraintsMap.put(joinTable,sqs);
			}
			// System.out.println(constraintString);
			return "";
		}
		return constraintString;

	}

	private static String getBackwardPassJoinandSelectionConstraints(GenerateCVC1 cvc,
			QueryBlockDetails queryBlock, Object cqs, Table sqTable, Vector<Table> tables,
			Vector<Node> selConds,
			Vector<Node> joinConds,
			Vector<Node> otherConds,
			Vector<Node> stringSelectionConds,
			Vector<Node> inConds,
			Vector<Node> isNullNodes,
			ArrayList<String> jtColumns,
			String joinTable,
			Boolean andOr // false for or true for and
	) throws Exception {

		// Map<String, Object> returnValues = new HashMap<String, Object>();
		boolean isCorrCondPresent = false;
		Vector<Node> correlationConds = new Vector<Node>();
		ConstraintGenerator constrGen = new ConstraintGenerator();

		for (Node n : cvc.correlationHashMap.keySet()) {
			correlationStructure cs = cvc.correlationHashMap.get(n);
			if (cs.getProcessLevel() == queryBlock.getLevel()) {
				correlationConds.add(n);
			}
		}

		String constr = "";
		if (cqs == null) {
			// returnValues.put("constr", "");
			// returnValues.put("isCorrCondPresent", isCorrCondPresent);
			// returnValues.put("correlationConds", correlationConds);
			return "";
		}

		if (cqs instanceof ConjunctQueryStructure) {
			ConjunctQueryStructure cq = (ConjunctQueryStructure) cqs;
			if (cq.getDisjuncts() != null && cq.getDisjuncts().size() != 0) {
				String tempconstr = "";
				for (DisjunctQueryStructure dsj : cq.getDisjuncts()) {
					tempconstr += getBackwardPassJoinandSelectionConstraints(cvc, queryBlock, dsj, sqTable, tables,
							dsj.getSelectionConds(), dsj.getJoinCondsForEquivalenceClasses(),
							dsj.getJoinCondsAllOther(), dsj.getStringSelectionConds(), inConds, isNullNodes, jtColumns,
							joinTable,
							false);

				}
				constr += tempconstr;
				// constr += "\n\t(and " + tempconstr + ")\n" ;
			}
		}

		Vector<Node> selectionAndJoinConds = new Vector<Node>();
		selectionAndJoinConds.addAll(selConds);
		selectionAndJoinConds.addAll(joinConds);
		selectionAndJoinConds.addAll(otherConds);
		selectionAndJoinConds.addAll(isNullNodes);

		for (Node n : selectionAndJoinConds) {
			if (!n.isCorrelated)
				constr += generateConstraintsForSelectionConditions(cvc, n, jtColumns, joinTable, tables, "i1", "i1")
						+ "\n\t";
			// else{
			// correlationConds.add(n);
			// }
			// String nonConstr = constrGen.getAssertNotNullCondition(cvc, n.getLeft(),
			// "i1");
			// nonConstr += constrGen.getAssertNotNullCondition(cvc, n.getRight(), "i1");

		}
		String SolvedCondForThisConInBaseTable = "";
		String tempC = "";
		for (Node n : stringSelectionConds) {
			constr += generateConstraintsForStringConditions(cvc, n, jtColumns, joinTable, tables, "i1", "i1");
			// get non null constraints

		}

		String strConstr = "";
		for (Node n : inConds) {
			// For non-correlated valid conditions
			if (n != null && n.isCorrelated == false && n.getExpired() == false) {
				String s1 = generateConstraintsForSelectionConditions(cvc, n, jtColumns, joinTable, tables, "i1", "i1");
				String tempConstr = "";

				// Vector<String> s2 = new Vector<>();
				for (Node i : n.getRight().getComponentNodes()) {
					String s2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, i, "i1");
					String tempS = "\t\t" + ((n.isNegated) ? "(not " : " ") + "("
							+ (n.getType().equalsIgnoreCase("NOT IN") ? "not (= " : "=") + "  " + s1
							+ "  " + s2 + (n.getType().equalsIgnoreCase("NOT IN") ? " ) )" : " " + ")")
							+ ((n.isNegated) ? " )" : " ") + "\n";
					if (!n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("int")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("real") 
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("time")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("date")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("timestamp")) {
						Vector<String> instringConstraints = new Vector<String>();
						instringConstraints.add(tempS);
						strConstr = "";
						Vector<String> solvedStringConstraint = cvc.getStringSolver().solveConstraints(
								instringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);

						for (String str : solvedStringConstraint) {
							str = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));
							strConstr += "\t\t" + str.replace("assert", "") + "\n";
						}
						tempConstr += strConstr;
					} else {
						tempConstr += tempS;
					}

				}
				// constr += "(and " + tempConstr + nonConstr + ")\n\n"; // with non-null
				constr += "(or " + tempConstr + ")\n\n"; // without non-null

			}
		}

		// backward pass constraints for correlation
		for (Node n : correlationConds) {
			String temp = "";
			if (n.getLeft() != null && n.getLeft().getTableNameNo() != null
					&& (n.getLeft().getTableNameNo().contains("JSQ") || n.getLeft().getTableNameNo().contains("GSQ")
							|| n.getLeft().getTableNameNo().contains("DSQ"))) {
				for (int i = 1; i <= cvc.getNoOfOutputTuples(n.getLeft().getTableNameNo()); i++)
					temp += generateConstraintsForCorrelationConditionsBackwardPass(cvc, n, jtColumns, joinTable,
							tables, Integer.toString(i), true, "i1", false);
			}
			Boolean a = (n.getRight().getTableNameNo().contains("JSQ")
					|| n.getRight().getTableNameNo().contains("GSQ")
					|| n.getRight().getTableNameNo().contains("DSQ"));
			if (n.getRight() != null && n.getRight().getTableNameNo() != null//
					&& (n.getRight().getTableNameNo().contains("JSQ")
							|| n.getRight().getTableNameNo().contains("GSQ")
							|| n.getRight().getTableNameNo().contains("DSQ"))) {
				for (int i = 1; i <= cvc.getNoOfOutputTuples(n.getRight().getTableNameNo()); i++)
					temp += generateConstraintsForCorrelationConditionsBackwardPass(cvc, n, jtColumns, joinTable,
							tables, "i1", false, Integer.toString(i), true);
			}
			if (cvc.correlationHashMap.get(n).getQueryType().equalsIgnoreCase("EXISTS") && !temp.isEmpty())
				constr += "\n\t( or \n\t\t" + temp + " )";
			else if (cvc.correlationHashMap.get(n).getQueryType().equalsIgnoreCase("NOT EXISTS") && !temp.isEmpty())
				constr += "\n\t(not ( or \n\t\t" + temp + " ))";

		}
		String constrF = andOr ? "\n\t(and " : "\n\t(or ";
		constrF += constr + ")\n";
		if (!constr.equalsIgnoreCase(""))
			return constrF;
		else
			return "";

	}

	private static Map<String, Object> getForwardPassJoinandSelectionConstraints(GenerateCVC1 cvc,
			QueryBlockDetails queryBlock, Object cqs, Table sqTable, Vector<String> joinTables,
			Vector<Node> selConds,
			Vector<Node> joinConds,
			Vector<Node> otherConds,
			Vector<Node> stringSelectionConds,
			Vector<Node> inConds,
			Vector<Node> isNullConds,
			Boolean andOr, // false for or true for and
			List<Integer> indList, int cnt, boolean isExist) throws Exception {
		// TODO Auto-generated method stub
		String strConstr = "", constr = "";
		Map<String, Object> returnValues = new HashMap<String, Object>();
		boolean isCorrCondPresent = false;
		Vector<Node> correlationConds = new Vector<Node>();

		Vector<Node> selectionAndJoinConds = new Vector<Node>();
		Vector<Node> stringConds = new Vector<Node>();

		int level = queryBlock.getLevel();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		// for (ConjunctQueryStructure cqs : queryBlock.getConjunctsQs()) {
		// SELECTION and JOIN conditions
		// Vector<Node> selConds = cqs.getSelectionConds();
		// Vector<Node> joinConds = cqs.getJoinCondsForEquivalenceClasses();
		Vector<Node> Conds = new Vector<Node>();
		Conds.addAll(selConds);
		Conds.addAll(joinConds);
		Conds.addAll(otherConds);
		Conds.addAll(isNullConds);
		Conds.addAll(cvc.correlationHashMap.keySet());

		if (cqs instanceof ConjunctQueryStructure) {
			ConjunctQueryStructure cq = (ConjunctQueryStructure) cqs;
			if (cq.getDisjuncts() != null && cq.getDisjuncts().size() != 0) {
				String tempconstr = "";
				Vector<Node> inconds = new Vector<Node>();
				for (DisjunctQueryStructure dsj : cq.getDisjuncts()) {
					Map<String, Object> ret = getForwardPassJoinandSelectionConstraints(cvc, queryBlock, dsj, sqTable,
							joinTables,
							dsj.getSelectionConds(), dsj.getJoinCondsForEquivalenceClasses(),
							dsj.getJoinCondsAllOther(), dsj.getStringSelectionConds(), inconds, isNullConds,
							false, indList, cnt, isExist);
					tempconstr += (String) ret.get("constr");
					isCorrCondPresent = isCorrCondPresent || (Boolean) ret.get("isCorrCondPresent");
					correlationConds.addAll((Vector<Node>) ret.get("correlationConds"));
				}
				constr += tempconstr;
				// constr += "\n\t(and " + tempconstr + ")\n" ;
			}
		}

		if (cqs == null || Conds.size() == 0) {
			returnValues.put("constr", constr);
			returnValues.put("isCorrCondPresent", isCorrCondPresent);
			returnValues.put("correlationConds", correlationConds);
			return returnValues;
		}

		Conds.removeAll(cvc.correlationHashMap.keySet());
		Conds.removeAll(isNullConds);

		// Vector<Node> selConds = cqs.getSelectionConds();
		// Vector<Node> joinConds = cqs.getJoinCondsForEquivalenceClasses();
		// Vector<Node> otherConds = cqs.getJoinCondsAllOther();
		// Vector<Node> stringSelectionConds = cqs.getStringSelectionConds();

		// HashMap<Node, String> condToSolvedStrinHashMap = new HashMap<Node, String>();

		if(!cvc.getTypeOfMutation().equalsIgnoreCase("DATASET TO KILL SET OPERATOR MUTATIONS ")  && cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getIsExpired()==false)
		{
			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getQueryBlock().getLevel() == level)
			{
				Conds = GenConstraints.HandleSelectionMutations(Conds, cvc.getCurrentMutant());
				Conds = GenConstraints.HandleNonEquiJoinMutations(Conds, cvc.getCurrentMutant());
			}
			
		}
			
		

		// Adding global correlation conditions to this set
		HashMap<Node, correlationStructure> temp = new HashMap<Node, correlationStructure>();

		for (Node n : cvc.correlationHashMap.keySet()) {
			if (Conds.contains(n)) {
				Conds.remove(n);
			}

			if (n.isCorrelated == true && n.getExpired() == false) {
				correlationStructure cs = cvc.correlationHashMap.get(n);
				if (cs != null && cs.getProcessLevel() == level
						&& cs.isAggr == true
						&& cs.isPushedDown == true) {
					Node newn = n.clone();
					newn.isCorrelated = false;
					Conds.add(newn);
					// cvc.correlationHashMap.remove(n);
					// updateNodeandCorrelatedConditionifAggregate(cvc, n, sqTable, level);
				} else {
					temp.put(n, cs);
					Conds.add(n);
				}
			} else
				Conds.add(n);
		}
		cvc.correlationHashMap.putAll(temp);

		// Conds.addAll(cvc.correlationHashMap.keySet());

		for (Node n : Conds) {
			// For non-correlated valid conditions
			if (n != null && n.isCorrelated == false && n.getExpired() == false) {
				// if (bit == 0) {
				selectionAndJoinConds.add(n);
				String leftIndex = "i1";
				String rightIndex = "j1";
				// if (n.getLeft() != null && n.getLeft().getType() == Node.getColRefType())
				// leftIndex = n.getLeft().getTable() == null ? n.getLeft().getTableAlias() +
				// "_i"
				// : n.getLeft().getTable().getTableName() + "_i";
				// if (n.getRight() != null && n.getRight().getType() == Node.getColRefType())
				// // rightIndex = n.getRight().getTableAlias() + "_i";
				// rightIndex = n.getRight().getTable() == null ? n.getRight().getTableAlias() +
				// "_i"
				// : n.getRight().getTable().getTableName() + "_i";
				String leftTablename = "", leftColumnname="", rightColumnname = "", rightTableName = "";
				if (n.getLeft() != null && n.getLeft().getType() == Node.getColRefType()){
					leftTablename = n.getLeft().getTable() == null ? n.getLeft().getTableAlias()
							: n.getLeft().getTable().getTableName();
					leftColumnname = n.getLeft().getColumn().getColumnName();
				}
				
				if (n.getRight() != null && n.getRight().getType() == Node.getColRefType()){
					rightTableName = n.getRight().getTable() == null ? n.getRight().getTableAlias()
					: n.getRight().getTable().getTableName();
					rightColumnname = n.getRight().getColumn().getColumnName();

				}
					// rightIndex = n.getRight().getTableAlias() + "_i";
					
				int li = joinTables.indexOf(leftTablename);
				int ri = joinTables.indexOf(rightTableName);

				leftIndex = li != -1 ? indList.get(li) + "" : "";
				rightIndex = ri != -1 ? indList.get(ri) + "" : "";

				String s1 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n.getLeft(), leftIndex);
				String s2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n.getRight(), rightIndex);

				// get non null constraints
				String nonConstr = constrGen.getAssertNotNullCondition(cvc, n.getLeft(), leftIndex);
				nonConstr += constrGen.getAssertNotNullCondition(cvc, n.getRight(), rightIndex);

				String op = "";
				String tempConstr = "";
				if (s1 != null && s2 != null && s1.contains("select") && s2.contains("select")) {
					op = constrGen.getUserDefinedComparisionOperator(n.getOperator(), n.getLeft().getColumn(),
							s1, s2);
					tempConstr += "\t\t" + ((n.isNegated) ? "(not " : " ") + op + ((n.isNegated) ? " )" : " ")
							+ "\n";

				} else {
					tempConstr += "\t\t" + ((n.isNegated) ? "(not " : " ") + "("
							+ (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + "  " + s1
							+ "  " + s2 + (n.getOperator().equals("/=") ? " ) )" : " " + ")")
							+ ((n.isNegated) ? " )" : " ") + "\n";
				}
				// constr += "(and " + tempConstr + nonConstr + ")\n\n"; // with non-null
				// constr += andOr?"(and ":"(or " + tempConstr + ")\n\n"; // without non-null
				

				if(leftTablename.equalsIgnoreCase(rightTableName) 
					&& leftColumnname.equalsIgnoreCase(rightColumnname) 
					&& leftIndex.equalsIgnoreCase(rightIndex) && n.isNegated){
					constr += "";
				}else{
					if ((tempConstr + nonConstr) != "")
						constr += "(and " + tempConstr + nonConstr + ")"; // with non-null
				}
				// constr += tempConstr ; // without non-null

			}
			// For correlated valid conditions
			else if (n.getExpired() == false && cnt == 0) {
				correlationStructure cs = cvc.correlationHashMap.get(n);
				if (cs != null && cs.getProcessLevel() < level) {
					isCorrCondPresent = true;
					// update the table name and column in node from below level JSQ
					updateTableAndColumnOfCorrelatedConditionWithAboveLevel(cvc, queryBlock, n, sqTable, level, false);
				}
			}
		}

		for (Node nCond : isNullConds) {
			String leftIndex = "i1";
			// String rightIndex = "j1";
			// if (globalCorrConds.getLeft() != null
			// && (globalCorrConds.getLeft().getType() == Node.getColRefType()
			// || globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
			// leftIndex = globalCorrConds.getLeft().getTable().getTableName() + "_i";
			// if (globalCorrConds.getRight() != null
			// && (globalCorrConds.getRight().getType() == Node.getColRefType()
			// || globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
			// rightIndex = globalCorrConds.getRight().getTable().getTableName() + "_i";

			if (nCond.getLeft() != null
					&& (nCond.getLeft().getType() == Node.getColRefType()
							|| nCond.getRight().getType() == Node.getAggrNodeType()))
				leftIndex = nCond.getLeft().getTable().getTableName();
			// if (nCond.getRight() != null
			// && (nCond.getRight().getType() == Node.getColRefType()
			// || nCond.getRight().getType() == Node.getAggrNodeType()))
			// rightIndex = nCond.getRight().getTable().getTableName() ;

			int li = joinTables.indexOf(leftIndex);
			// int ri = joinTables.indexOf(rightIndex);

			leftIndex = li != -1 ? indList.get(li) + "" : "";
			// rightIndex = ri != -1 ? indList.get(ri)+"" : "";

			String nonConstr = constrGen.getAssertNotNullCondition(cvc, nCond.getLeft(), leftIndex);

			if(nCond.getNodeType().equalsIgnoreCase(nCond.getIsNullNodeType()) && nCond.getOperator().equalsIgnoreCase("=")){
				nonConstr = "(not " + nonConstr + ")";
			}

			constr += nonConstr;
			// nonConstr += constrGen.getAssertNotNullCondition(cvc, nCond.getRight(),
			// leftIndex);

		}

		// Looking at global correlation map and process correlation if is to be
		// processed at current level
		for (Node globalCorrConds : cvc.correlationHashMap.keySet()) {
			if (globalCorrConds.getLeft() == null && globalCorrConds.getRight() == null)
				continue;
			correlationStructure cs = cvc.correlationHashMap.get(globalCorrConds);
			if (cs.getProcessLevel() == level && globalCorrConds.isCorrelated == true) {
				isCorrCondPresent = true;
				String tempConstr = "";
				correlationConds.add(globalCorrConds);
				String leftIndex = "i1";
				String rightIndex = "j1";
				// if (globalCorrConds.getLeft() != null
				// && (globalCorrConds.getLeft().getType() == Node.getColRefType()
				// || globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
				// leftIndex = globalCorrConds.getLeft().getTable().getTableName() + "_i";
				// if (globalCorrConds.getRight() != null
				// && (globalCorrConds.getRight().getType() == Node.getColRefType()
				// || globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
				// rightIndex = globalCorrConds.getRight().getTable().getTableName() + "_i";

				if (globalCorrConds.getLeft() != null
						&& (globalCorrConds.getLeft().getType() == Node.getColRefType()
								|| globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
					leftIndex = globalCorrConds.getLeft().getTable().getTableName();
				if (globalCorrConds.getRight() != null
						&& (globalCorrConds.getRight().getType() == Node.getColRefType()
								|| globalCorrConds.getRight().getType() == Node.getAggrNodeType()))
					rightIndex = globalCorrConds.getRight().getTable().getTableName();

				int li = joinTables.indexOf(leftIndex);
				int ri = joinTables.indexOf(rightIndex);

				leftIndex = li != -1 ? indList.get(li) + "" : "";
				rightIndex = ri != -1 ? indList.get(ri) + "" : "";

				String s1 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, globalCorrConds.getLeft(),
						leftIndex);
				String s2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, globalCorrConds.getRight(),
						rightIndex);
				
				String nonConstr = constrGen.getAssertNotNullCondition(cvc, globalCorrConds.getLeft(), leftIndex);
				
				nonConstr += constrGen.getAssertNotNullCondition(cvc, globalCorrConds.getRight(), rightIndex);

				String op = "";

				if (s1.contains("select") && s2.contains("select")) {
					op = constrGen.getUserDefinedComparisionOperator(globalCorrConds.getOperator(),
							globalCorrConds.getLeft().getColumn(),
							s1, s2);
					if (cs.getQueryType().equalsIgnoreCase("EXISTS") || cs.getQueryType().equalsIgnoreCase("FROM")||cs.getQueryType().equalsIgnoreCase("NOT EXISTS"))
						tempConstr += "\t\t"
								+ ((globalCorrConds.getOperator().equals("/=")) || (globalCorrConds.isNegated)
										? "(not "
										: " ")
								+ op
								+ ((globalCorrConds.getOperator().equals("/=")) || (globalCorrConds.isNegated)
										? " )"
										: " ")
								+ "\n";
					// else if (cs.getQueryType().equalsIgnoreCase("NOT EXISTS"))
					// 	tempConstr += "\t\t(not "
					// 			+ ((globalCorrConds.getOperator().equals("/=")) || (globalCorrConds.isNegated)
					// 					? "(not "
					// 					: " ")
					// 			+ op
					// 			+ ((globalCorrConds.getOperator().equals("/=")) || (globalCorrConds.isNegated)
					// 					? " ))"
					// 					: " )")
					// 			+ "\n";

				} else {
					if (cs.getQueryType().equalsIgnoreCase("EXISTS") || cs.getQueryType().equalsIgnoreCase("FROM")||cs.getQueryType().equalsIgnoreCase("NOT EXISTS"))
						tempConstr += "\t\t ("
								+ (globalCorrConds.getOperator().equals("/=") ? "not (= "
										: globalCorrConds.getOperator())
								+ "  " + s1
								+ "  " + s2 + (globalCorrConds.getOperator().equals("/=") ? " ) )" : " " + ")")
								+ "\n";
					// else if (cs.getQueryType().equalsIgnoreCase("NOT EXISTS"))
					// 	tempConstr += "\t\t(not ("
					// 			+ (globalCorrConds.getOperator().equals("/=") ? "not (= "
					// 					: globalCorrConds.getOperator())
					// 			+ "  " + s1
					// 			+ "  " + s2 + (globalCorrConds.getOperator().equals("/=") ? " ) )" : " " + "))")
					// 			+ "\n";
				}
				// constr += "(and " + tempConstr + nonConstr + ")"; // with non-null
				// constr += andOr?"(and ":"(or "+ tempConstr + ")"; // without non-null
				// if (!isExist)
					constr += "(and " + tempConstr + nonConstr + ")"; // with non-null
				// constr += tempConstr; // without non-null

				globalCorrConds.setExpired(true);

				// get forward and backward pass constraints for correlation constraints
			}
		}

		// STRING SELECTION conditions
		// Vector<Node> stringSelectionConds = cqs.getStringSelectionConds();
		Vector<String> stringConstraints = new Vector<String>();
		stringConds.addAll(stringSelectionConds);
		
		if(!cvc.getTypeOfMutation().equalsIgnoreCase("DATASET TO KILL SET OPERATOR MUTATIONS ") && cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getIsExpired()==false)
			if (cvc.getCurrentMutant() != null &&  cvc.getCurrentMutant().getQueryBlock().getLevel() == level)
				stringConds = GenConstraints.HandleStringSelectionMutations(stringConds, cvc.getCurrentMutant());

		for (int k = 0; k < stringSelectionConds.size(); k++) {

			if (stringSelectionConds.get(k).isCorrelated == false
					&& stringSelectionConds.get(k).getExpired() == false) {
				stringConstraints.clear();
				String tableName = stringSelectionConds.get(k).getLeft().getTableNameNo().replaceAll("\\d", "")
						.toLowerCase();
				// String index = joinTables.contains(tableName) ? tableName + "_i" : "i1";
				String index = joinTables.contains(tableName) ? tableName : "i1";
				int ind = joinTables.contains(tableName) ? joinTables.indexOf(tableName) : 0;
				index = ind != -1 ? indList.get(ind) + "" : "";

				strConstr = constrGen.genPositiveCondsForPredF(cvc, queryBlock, stringSelectionConds.get(k),
						index);

				stringConstraints.add(strConstr);
				strConstr = "";
				Vector<String> solvedStringConstraint = cvc.getStringSolver().solveConstraints(
						stringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);
				// if (solvedStringConstraint.size() > 0)
				// condToSolvedStrinHashMap.put(stringSelectionConds.get(k),
				// solvedStringConstraint.get(0));
				for (String str : solvedStringConstraint) {
					str = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));
					strConstr += "\t\t" + str.replace("assert", "") + "\n";
				}
				constr += strConstr;
			} else if (stringSelectionConds.get(k).getExpired() == false) {
				correlationStructure cs = cvc.correlationHashMap.get(stringSelectionConds.get(k));
				if (cs.getProcessLevel() == level) {
					correlationConds.add(stringSelectionConds.get(k));
					stringSelectionConds.get(k).setExpired(true);
				} else {
					// update the table name and column in node from below level JSQ
					// This case means attributes need to be passed up
					if (cnt == 0)
						updateTableAndColumnOfCorrelatedConditionWithAboveLevel(cvc, queryBlock,
								stringSelectionConds.get(k),
								sqTable, level, false);
				}
			}

			// }

		}

		for (Node n : inConds) {
			// inConds.add(n);
			// For non-correlated valid conditions
			if (n != null && n.isCorrelated == false && n.getExpired() == false) {

				String leftIndex = "i1";
				String rightIndex = "j1";
				// if (n.getLeft() != null && n.getLeft().getType() == Node.getColRefType())
				// leftIndex = n.getLeft().getTable() == null ? n.getLeft().getTableAlias() +
				// "_i"
				// : n.getLeft().getTable().getTableName() + "_i";

				if (n.getLeft() != null && n.getLeft().getType() == Node.getColRefType())
					leftIndex = n.getLeft().getTable() == null ? n.getLeft().getTableAlias()
							: n.getLeft().getTable().getTableName();

				int li = joinTables.indexOf(leftIndex);
				// int ri = joinTables.indexOf(rightIndex);

				leftIndex = li != -1 ? indList.get(li) + "" : "";
				// rightIndex = indList.get(ri)+"";

				String s1 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n.getLeft(),
						leftIndex);
				String tempConstr = "";

				// Vector<String> s2 = new Vector<>();
				for (Node i : n.getRight().getComponentNodes()) {
					String s2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, i,
							rightIndex);

					String tempS = "\t\t" + ((n.isNegated) ? "(not " : " ") + "("
							+ (n.getType().equalsIgnoreCase("NOT IN") ? "not (= " : "=") + " " + s1
							+ " " + s2 + (n.getType().equalsIgnoreCase("NOT IN") ? " ) )" : " " + ")")
							+ ((n.isNegated) ? " )" : " ") + "\n";
					if (!n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("int")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("real") 
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("time")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("date")
							&& !n.getLeft().getColumn().getCvcDatatype().equalsIgnoreCase("timestamp")) {
						Vector<String> instringConstraints = new Vector<String>();
						instringConstraints.add(tempS);
						strConstr = "";
						Vector<String> solvedStringConstraint = cvc.getStringSolver().solveConstraints(
								instringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);

						for (String str : solvedStringConstraint) {
							str = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));
							strConstr += "\t\t" + str.replace("assert", "") + "\n";
						}
						tempConstr += strConstr;
					} else {
						tempConstr += tempS;
					}

				}
				// constr += "(and " + tempConstr + nonConstr + ")\n\n"; // with non-null
				constr += "(or " + tempConstr + ")\n\n"; // without non-null

			}
		}

		String constrF = andOr ? "(and \n\t" : "(or "; // without non-null
		constrF += constr + ")\n\n";
		returnValues.put("isCorrCondPresent", isCorrCondPresent);
		if (!constr.equalsIgnoreCase(""))
			returnValues.put("constr", constrF);
		else
			returnValues.put("constr", "");
		returnValues.put("correlationConds", correlationConds);
		returnValues.put(constr, returnValues);

		return returnValues;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getForwardPassJoinandSelectionConstraints'");
	}

	private static String generateConstraintsForCorrelationConditionsBackwardPass(GenerateCVC1 cvc, Node n,
			ArrayList<String> jtColumns,
			String joinTable, Vector<Table> tables, String tupleIdLeft, Boolean isLInt, String tupleIdRight,
			Boolean isRInt) {
		String constr = "";
		String left = "";
		String right = "";

		Boolean l_isSQ = false, r_isSQ = false;
		if (n.getLeft().getTableNameNo() != null && !n.getLeft().getTableNameNo().contains("JSQ")
				&& !n.getLeft().getTableNameNo().contains("GSQ") && !n.getLeft().getTableNameNo().contains("DSQ")) {
			left = n.getLeft().getTableNameNo() != null
					? (n.getLeft().getTableNameNo().replaceAll("\\d", "")).toLowerCase()
					: null;
		} else {
			left = n.getLeft().getTableNameNo() != null
					? (n.getLeft().getTableNameNo())
					: null;
			l_isSQ = true;
		}
		if (n.getRight().getTableNameNo() != null && !n.getRight().getTableNameNo().contains("JSQ")
				&& !n.getRight().getTableNameNo().contains("GSQ") && !n.getRight().getTableNameNo().contains("DSQ")) {
			right = n.getRight().getTableNameNo() != null
					? (n.getRight().getTableNameNo().replaceAll("\\d", "")).toLowerCase()
					: null;
		} else {
			right = n.getRight().getTableNameNo() != null
					? (n.getRight().getTableNameNo())
					: null;
			r_isSQ = true;
		}
		int l_index = 0, r_index = 0, l_flag = -1, r_flag = -1;
		// left
		for (Table table : tables) {
			if (table == null)
				continue;
			if (table.getTableName().equals(left)) { // gives col index of t1
				l_index += table.getColumnIndex(n.getLeft().getColumn().getColumnName());
				l_flag = 1;
				break;
			} else
				l_index += table.getNoOfColumn();
		}

		// right
		for (Table table : tables) {
			if (table == null)
				continue;
			if (table.getTableName().equals(right)) // gives col index of t1
			{
				r_index += table.getColumnIndex(n.getRight().getColumn().getColumnName());
				r_flag = 1;
				break;
			} else
				r_index += table.getNoOfColumn();
		}

		for (int i = 0; i < cvc.getTableMap().getSQTableByName(joinTable).getColumnIndexList().size(); i++) {
			String columnName = cvc.getTableMap().getSQTableByName(joinTable).getColumnIndexList().get(i);
			// System.out.println(left.contains("JSQ")?columnName.contains("_" +
			// n.getLeft().getColumn().getColumnName()):columnName.contains(left+"__" +
			// n.getLeft().getColumn().getColumnName()));
			// System.out.println("_" + n.getLeft().getColumn().getColumnName());
			if (n.getLeft() != null && n.getLeft().getColumn() != null
					&& ((left.contains("JSQ") || left.contains("GSQ") || left.contains("DSQ"))
							? columnName.contains("_" + n.getLeft().getColumn().getColumnName())
							: columnName.contains(left + "__" + n.getLeft().getColumn().getColumnName()))) {
				l_index = i;
				l_flag = 1;
			}
			if (n.getRight() != null && n.getRight().getColumn() != null
					&& ((right.contains("JSQ") || right.contains("GSQ") || left.contains("DSQ"))
							? columnName.contains("_" + n.getRight().getColumn().getColumnName())
							: columnName.contains(right + "__" + n.getRight().getColumn().getColumnName()))) {
				r_index = i;
				r_flag = 1;
			}

		}

		String cntValid = "";
		if (l_flag != -1 && !l_isSQ) {
			constr = "( " + jtColumns.get(l_index) + " (select O_" + joinTable + " " + tupleIdLeft + " )) ";
			if (isLInt)
				cntValid += ConstraintGenerator.getConstraintsForValidCountUsingTable(n.getLeft().getTable(),
						n.getLeft().getTable().getTableName(), Integer.parseInt(tupleIdLeft), 0);

		}
		if (r_flag != -1 && !r_isSQ) {
			constr += "( " + jtColumns.get(r_index) + " (select O_" + joinTable + " " + tupleIdRight + " )) ";
			if (isRInt)
				cntValid += ConstraintGenerator.getConstraintsForValidCountUsingTable(n.getRight().getTable(),
						n.getRight().getTable().getTableName(), Integer.parseInt(tupleIdRight), 0);
		}
		if (l_flag != -1 && l_isSQ) {
			constr += "( " + n.getLeft().getColumn().getColumnName() + " (select O_" + n.getLeft().getTableNameNo()
					+ " " + cvc.enumIndexVar + tupleIdLeft + " )) ";
			if (isLInt)
				cntValid += ConstraintGenerator.getConstraintsForValidCountUsingTable(n.getLeft().getTable(),
						n.getLeft().getTable().getTableName(), Integer.parseInt(tupleIdLeft), 0);
		}
		if (r_flag != -1 && r_isSQ) {
			constr += "( " + n.getRight().getColumn().getColumnName() + " (select O_" + n.getRight().getTableNameNo()
					+ " " + cvc.enumIndexVar + tupleIdRight + " )) ";
			if (isRInt)
				cntValid += ConstraintGenerator.getConstraintsForValidCountUsingTable(n.getRight().getTable(),
						n.getRight().getTable().getTableName(), Integer.parseInt(tupleIdRight), 0);
		}
		if (l_flag != -1 && r_flag == -1) {
			constr = "( " + jtColumns.get(l_index) + " (select O_" + joinTable + " " + tupleIdLeft + " )) ";
			constr += n.getRight().getStrConst();
		}
		if (r_flag != -1 && l_flag == -1) {
			constr = "( " + jtColumns.get(r_index) + " (select O_" + joinTable + " " + tupleIdLeft + " )) ";
			constr += n.getLeft().getStrConst();
		}

		if (n.getOperator().equals("/="))
			return "\n\t\t(and (not (= " + constr + "))\n\t\t" + cntValid + ")";

		String op = "";
		ConstraintGenerator cg = new ConstraintGenerator();
		op = cg.getUserDefinedComparisionOperator(n.getOperator(), n.getLeft().getColumn(), constr, op);
		if (!op.equalsIgnoreCase("")) {
			return "\n\t\t(and " + op + " \n\t\t" + cntValid + ")";

		}

		return "\n\t\t(and (" + n.getOperator() + " " + constr + " )\n\t\t" + cntValid + ")";

	}

	/**
	 * @author Sunanda
	 * @param cvc
	 * @param n
	 * @param sqTable
	 * @throws CloneNotSupportedException
	 * 
	 */
	public static void updateTableAndColumnOfCorrelatedConditionWithAboveLevel(GenerateCVC1 cvc, QueryBlockDetails qb,
			Node n, Table sqTable,
			int level, Boolean isAggr) throws CloneNotSupportedException {
		correlationStructure cs = cvc.correlationHashMap.get(n);
		int leftLevel = 0, rightLevel = 0;
		String sqTableAliasLeft = null, sqTableAliasRight = null;
		if (cs.isPushedDown) {
			leftLevel = cs.getOriginalLevelLeft();
			rightLevel = cs.getOriginalLevelRight();
		} else if (n.getLeft() != null && n.getLeft().getLevel() != null) {
			leftLevel = n.getLeft().getLevel();
		}
		if (n.getRight() != null && n.getRight().getLevel() != null) {
			rightLevel = n.getRight().getLevel();
		}
		if (n.getLeft() != null && n.getLeft().getTable() != null && n.getLeft().getColumn() != null) {
			String leftTableName = n.getLeft().getTable().getTableName();
			String leftColumnName = n.getLeft().getColumn().getColumnName();

			if (isAggr) {// this aggr is not the same as correlation structure aggr - this means its from
							// aggr part of the code
				if (leftTableName.endsWith(String.valueOf(level)) && leftTableName.startsWith("JSQ")) {
					leftTableName = sqTable.getTableName();
					// String sqTableLeftColumnName = leftTableName
					// + n.getRight().getTable().getColumnIndex(leftColumnName);
					if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType()))
						leftColumnName = ConstraintGenerator.getColumnNameForGSQ(leftTableName,
								n.getLeft().getOrgiColumn().getTableName(), n.getLeft().getAgg().getFunc(),
								n.getLeft().getOrgiColumn().getColumnName());

					else {
						leftColumnName = ConstraintGenerator.getColumnNameForGSQ(leftTableName,
								n.getLeft().getOrgiColumn().getTableName(), "",
								n.getLeft().getOrgiColumn().getColumnName());
					}

					Node newNode = n.clone();
					newNode.getLeft().setColumn(sqTable.getColumn(leftColumnName));
					newNode.getLeft().setTable(sqTable);
					newNode.getLeft().setTableNameNo(sqTable.getTableName());
					newNode.getLeft().setTableAlias(sqTable.getAliasName());

					if (n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())) {
						Node tempNode = n.getLeft().getAgg().getAggExp().clone();
						tempNode.setColumn(sqTable.getColumn(leftColumnName));
						tempNode.setTable(sqTable);
						tempNode.setTableNameNo(sqTable.getTableName());
						tempNode.setTableAlias(sqTable.getAliasName());
						newNode.getLeft().getAgg().setAggExp(tempNode);
					}

					if (newNode != null) {
						cvc.correlationHashMap.remove(n);
						cvc.correlationHashMap.put(newNode, cs);
					}

				}
			} else if (leftLevel == level) {
				String sqTableLeftColumnName = sqTable.getTableName() + "_" + leftTableName + "__" + leftColumnName
						+ n.getLeft().getTable().getColumnIndex(leftColumnName);
				if (leftTableName.contains("JSQ") || leftTableName.contains("GSQ") || leftTableName.contains("DSQ")) {
					sqTableLeftColumnName = sqTable.getTableName() + "_" + leftColumnName
							+ n.getLeft().getTable().getColumnIndex(leftColumnName);
				}

				// cvc.correlationHashMap.remove(n);
				Node newNode = n.clone();
				newNode.getLeft().setColumn(sqTable.getColumn(sqTableLeftColumnName));
				newNode.getLeft().setTable(sqTable);
				newNode.getLeft().setTableNameNo(sqTable.getTableName());
				newNode.getLeft().setTableAlias(sqTable.getAliasName());
				if (!QueryBlockDetails.isAggregationPresentInQuery(qb))
					newNode.getLeft().level = level - 1;

				if (n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())) {
					Node tempNode = n.getLeft().getAgg().getAggExp().clone();
					tempNode.setColumn(sqTable.getColumn(sqTableLeftColumnName));
					tempNode.setTable(sqTable);
					tempNode.setTableNameNo(sqTable.getTableName());
					tempNode.setTableAlias(sqTable.getAliasName());
					newNode.getLeft().getAgg().setAggExp(tempNode);
				}
				if (newNode != null) {
					cvc.correlationHashMap.remove(n);
					cvc.correlationHashMap.put(newNode, cs);
				}
			}
		}

		if (n.getRight() != null && n.getRight().getTable() != null && n.getRight().getColumn() != null) {
			String rightTableName = n.getRight().getTable().getTableName();
			String rightColumnName = n.getRight().getColumn().getColumnName();

			if (isAggr) {
				if (rightTableName.endsWith(String.valueOf(level)) && rightTableName.startsWith("JSQ")) {
					rightTableName = sqTable.getTableName();
					// rightColumnName = rightTableName + "_" +
					// rightColumnName.split("__")[1].replaceAll("\\d", "");

					if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType()))
						rightColumnName = ConstraintGenerator.getColumnNameForGSQ(rightTableName,
								n.getRight().getOrgiColumn().getTableName(), n.getRight().getAgg().getFunc(),
								n.getRight().getOrgiColumn().getColumnName());

					else {
						rightColumnName = ConstraintGenerator.getColumnNameForGSQ(rightTableName,
								n.getRight().getOrgiColumn().getTableName(), "",
								n.getRight().getOrgiColumn().getColumnName());
					}
					for (String c : sqTable.getColumns().keySet()) {
						if (c.contains(rightColumnName)) {
							rightColumnName = c;
							break;
						}
					}
					// cvc.correlationHashMap.remove(n);
					Node newNode = n.clone();
					newNode.getRight().setColumn(sqTable.getColumn(rightColumnName));
					newNode.getRight().setTable(sqTable);
					newNode.getRight().setTableNameNo(sqTable.getTableName());
					newNode.getRight().setTableAlias(sqTable.getAliasName());
					newNode.getRight().level = rightLevel;

					if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())) {
						Node tempNode = n.getRight().getAgg().getAggExp().clone();
						tempNode.setColumn(sqTable.getColumn(rightColumnName));
						tempNode.setTable(sqTable);
						tempNode.setTableNameNo(sqTable.getTableName());
						tempNode.setTableAlias(sqTable.getAliasName());
						newNode.getRight().getAgg().setAggExp(tempNode);
					}

					if (newNode != null) {
						cvc.correlationHashMap.remove(n);
						cvc.correlationHashMap.put(newNode, cs);
					}
				}
			} else if (rightLevel == level) {
				String sqTableRightColumnName = sqTable.getTableName() + "_" + rightTableName + "__" + rightColumnName
						+ n.getRight().getTable().getColumnIndex(rightColumnName);

				if (rightTableName.contains("JSQ") || rightTableName.contains("GSQ")
						|| rightTableName.contains("DSQ")) {
					sqTableRightColumnName = sqTable.getTableName() + "_" + rightColumnName +
							+n.getRight().getTable().getColumnIndex(rightColumnName);
				}
				// Node nn = cvc.correlationHashMap.get(n).getCondition();
				// correlationStructure cs = cvc.correlationHashMap.get(n);

				// cvc.correlationHashMap.remove(n);
				Node newNode = n.clone();
				newNode.getRight().setColumn(sqTable.getColumn(sqTableRightColumnName));
				newNode.getRight().setTable(sqTable);
				newNode.getRight().setTableNameNo(sqTable.getTableName());
				newNode.getRight().setTableAlias(sqTable.getAliasName());
				if (!QueryBlockDetails.isAggregationPresentInQuery(qb))
					newNode.getRight().level = level - 1;

				if (n.getRight().getType().equalsIgnoreCase(Node.getAggrNodeType())) {
					Node tempNode = newNode.getRight().getAgg().getAggExp().clone();
					tempNode.setColumn(sqTable.getColumn(sqTableRightColumnName));
					tempNode.setTable(sqTable);
					tempNode.setTableNameNo(sqTable.getTableName());
					tempNode.setTableAlias(sqTable.getAliasName());
					newNode.getRight().getAgg().setAggExp(tempNode);
				}

				if (newNode != null) {
					cvc.correlationHashMap.remove(n);
					cvc.correlationHashMap.put(newNode, cs);
				}
			}
		}

	}

	/**
	 * @author Sunanda
	 *         FIXME
	 */
	private static String getStringConstraintsForSQTableUsingBaseTables(String tempC,
			String solvedCondForThisConInBaseTable) {
		String Var = "";
		// plan to extract solution of string solver from one string and put it in
		// another

		return "";
	}

	/**
	 * Calculates level for now - need to include SQ table iterator info in CVC
	 * 
	 * @author parismita
	 * @param queryBlock
	 * @param fromorwhere
	 * @return
	 * @throws Exception
	 */
	public static int getSQLevel(QueryBlockDetails queryBlock, String fromorwhere) throws Exception {
		ArrayList<QueryBlockDetails> SubQBlocks = queryBlock.getWhereClauseSubQueries();
		if (SubQBlocks == null)
			return 1;
		int level = 1;
		// get max height of the SQ
		for (QueryBlockDetails SubQBlock : SubQBlocks) {
			int levelSQ = getSQLevel(SubQBlock, fromorwhere);
			level = Math.max(level, levelSQ + 1);
		}
		return level;
	}

	public static String getConstraintsForJoinsWithoutCount(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1,
			Node n2, String operator, boolean isExist, String fromorwhere) throws Exception {

		String constraintString = "";

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			isTempJoin = true;
		} else {
			isTempJoin = false;
		}
		if (!isTempJoin) {
			/** get the details of each node */
			String t1 = getTableName(n1);
			String t2 = getTableName(n2);

			// int pos1 =
			// cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
			// int pos2 =
			// cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

			// below two lines added by rambabu
			int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(getColumn(n1).getColumnName());
			int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(getColumn(n2).getColumnName());

			String r1 = getTableNameNo(n1);
			String r2 = getTableNameNo(n2);
			logger.log(Level.INFO, "relation2 name num  ---" + r2);

			int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

			/** Get number of tuples of each relation occurrence */
			int tuples1 = 0, tuples2 = 0;
			if (cvc.getNoOfTuples().containsKey(r1)) {

				tuples1 = cvc.getNoOfTuples().get(r1);
			}

			if (cvc.getNoOfTuples().containsKey(r2)) {

				tuples2 = cvc.getNoOfTuples().get(r2);
			}

			int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

			for (int i = 0; i < noOfgroups; i++) {
				/** Do a round robin for the smaller value */
				for (int k = 1, l = 1;; k++, l++) {

					// constraintString += "ASSERT ("+
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
					// ((i*tuples1)+k+offset1-1))+ operator +
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
					// ((i*tuples2)+l+offset2-1))+");\n";
					/*
					 * ConstraintObject constrObj = new ConstraintObject();
					 * constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1,
					 * ((i*tuples1)+k+offset1-1)));
					 * constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,
					 * n2, ((i*tuples2)+l+offset2-1)));
					 * constrObj.setOperator(operator);
					 * constrObjList.add(constrObj);
					 */

					constraintString += constrGen.getAssertConstraint(
							constrGen.genPositiveCondsForPred(queryBlock, n1, ((i * tuples1) + k + offset1 - 1)),
							operator,
							constrGen.genPositiveCondsForPred(queryBlock, n2, ((i * tuples2) + l + offset2 - 1)));

					if (tuples1 > tuples2) {
						if (l == tuples2 && k < tuples1)
							l = 0;
						if (k >= tuples1)
							break;
					} else if (tuples1 < tuples2) {
						if (l < tuples2 && k == tuples1)
							k = 0;
						if (l >= tuples2)
							break;
					} else {// if tuples1==tuples2
						if (l == tuples1)
							break;
					}
				}

			}
		} else if (isTempJoin) {
			Vector<Table> tables = new Vector<Table>();
			Vector<String> joinTables = new Vector<String>();
			int level = getSQLevel(queryBlock, fromorwhere);
			// need to check which SQ condition it is in its level

			for (String baseRelations : queryBlock.getBaseRelations()) {
				Table cvcTable = cvc.getTableMap().getTable(baseRelations.replaceAll("\\d", "").toUpperCase());
				if (!tables.contains(cvcTable)) {
					tables.add(cvcTable);
					joinTables.add(cvcTable.getTableName());
				}
			}

			if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true") &&
					checkIfTablesArePartOfSubQuery(cvc, joinTables, queryBlock.getLevel(), fromorwhere)) {
				// Join Temp table implementation

				String joinTable;
				ConstraintGenerator constrGen = new ConstraintGenerator();
				joinTable = "SQ" + String.valueOf(level);
				// joinTable = "SQ"+ String.valueOf(cvc.tempJoinDefine.size()+1);
				// test code added by deekshat
				// to pass join tables

				if (fromorwhere.equalsIgnoreCase("from")) {
					ArrayList<String> s = new ArrayList<String>();
					s.addAll(0, joinTables);

					cvc.subqueryConstraintsMap.get("from").SQTableName.put(joinTable, s);
					cvc.subqueryConstraintsMap.get("from").SQjoinWithEXISTS.put(joinTable, true);
				} else {
					ArrayList<String> s = new ArrayList<String>();
					s.addAll(0, joinTables);
					cvc.subqueryConstraintsMap.get("where").SQTableName.put(joinTable, s);
					cvc.subqueryConstraintsMap.get("where").SQjoinWithEXISTS.put(joinTable, isExist);
				}
				// added by deeksha
				// if SQi table is not added, break
				if (tablesAdded.contains(joinTable)) {
					return "";
				}
				// testcode by deeksha--------------------------------------------------
				HashMap<String, Object> jtColumnNamesAndDataTypes = createTempTableColumnsForJoins(cvc, joinTable,
						tables,
						isExist, fromorwhere, level);
				ArrayList<String> jtColumns = (ArrayList<String>) jtColumnNamesAndDataTypes.get("Names");
				ArrayList<Sort> jtColumnDataTypes = (ArrayList<Sort>) jtColumnNamesAndDataTypes.get("DataTypes");
				cvc.getTableMap().setSQTables((Map<String, Table>) jtColumnNamesAndDataTypes.get("TableMap"));
				Table sqTable = cvc.getTableMap().getSQTableByName(joinTable);

				String tempConstraints = "";

				// context setting
				if (fromorwhere.equalsIgnoreCase("from")) {
					cvc.subqueryConstraintsMap.get("from").SQColumns.put(joinTable, jtColumns);
				} else {
					cvc.subqueryConstraintsMap.get("where").SQColumns.put(joinTable, jtColumns);
				}
				// int noOfAttr = cvc.tempJoinColumns.get(tempjoinTable).size();

				Expr aex = ConstraintGenerator.putTableInCtx(cvc,
						jtColumns.toArray(new String[jtColumns.size()]),
						jtColumnDataTypes.toArray(new Sort[jtColumnDataTypes.size()]),
						joinTable);

				for (String statement : ConstraintGenerator.declareRelation(aex)) {
					if (statement.contains(joinTable))
						tempConstraints += statement;
				}

				// System.out.printldn(declareRelation(aex));
				// step 2 : sq1 and sq2 functions for matching join relation with original

				int joincol = 0;
				for (int i = 0; i < tables.size(); i++) {
					String fun = ("define-fun " + joinTable + "_map_" + joinTables.get(i)
							+ "((x!0 Int) (x!1 Int)) Bool\n");

					tempConstraints += "\n(" + fun + "(and \n";
					int col1 = 0;
					while (col1 < tables.get(i).getColumns().size()) {
						Expr left = ConstraintGenerator.genSelectTest2(cvc, joinTables.get(i), col1, "x!0");
						Expr right = ConstraintGenerator.genSelectTest2(cvc, joinTable, joincol, "x!1");

						Expr eq = ConstraintGenerator.ctx.mkEq(left, right);
						tempConstraints += eq.toString() + "\n";
						col1++;
						joincol++;

					}
					tempConstraints += ")\n)\n";
				}

				constraintString += tempConstraints;

				// selection and join conditions from subquery :POOJA
				String constr = "";
				String strConstr = "";
				HashMap<Node, String> CondToSolvedStringCondition = new HashMap<Node, String>();
				Vector<Node> selectionAndJoinConds = new Vector<Node>();
				Vector<Node> stringConds = new Vector<Node>();
				Vector<Node> correlationConds = new Vector<Node>();

				for (ConjunctQueryStructure cqs : queryBlock.getConjunctsQs()) {

					// SELECTION and JOIN conditions
					Vector<Node> selConds = cqs.getSelectionConds();
					Vector<Node> joinConds = cqs.getJoinCondsForEquivalenceClasses();
					Vector<Node> Conds = new Vector<Node>();
					Conds.addAll(selConds);
					Conds.addAll(joinConds);

					// Changes by sunanda for alias hashmap
					// for (Node n: Conds){
					// if(n.getLeft() != null && n.getLeft().getTable() != null)
					// cvc.aliasMappingToLevels.put(n.getLeft().getTable().getTableName(), );
					// if(n.getRight() != null && n.getRight().getTable() != null)
					// cvc.aliasMappingToLevels.get(n.getRight().getTable().getTableName()).add(n.getRight().getTableAlias());
					// }

					for (Node n : Conds) {
						// int bit = isCorrelated(n, cvc, sqTable, tables, fromorwhere, level);
						if (n.isCorrelated == false) {
							// if (bit == 0) {
							selectionAndJoinConds.add(n);

							String leftIndex = "i1";
							String rightIndex = "j1";
							if (n.getLeft() != null && n.getLeft().getType() == Node.getColRefType())
								leftIndex = n.getLeft().getTable().getTableName() + "_i";
							if (n.getRight() != null && n.getRight().getType() == Node.getColRefType())
								rightIndex = n.getRight().getTable().getTableName() + "_i";
							String s1 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n.getLeft(), leftIndex);
							String s2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n.getRight(), rightIndex);
							constr += "\t\t (" + (operator.equals("/=") ? "not (= " : operator) + "  " + s1 + "  " + s2
									+ (operator.equals("/=") ? " ) )" : " " + ")") + "\n";
						} else {
							// if (bit == 2) {
							// n.getLeft().setAliasName(sqTable.getAliasName());
							// n.getLeft().setTable(sqTable);
							// n.getLeft().setTableAlias(sqTable.getAliasName());
							// n.getLeft().setTableNameNo(sqTable.getTableName());
							// } else if (bit == 3) {
							// n.getRight().setAliasName(sqTable.getAliasName());
							// n.getRight().setTable(sqTable);
							// n.getRight().setTableAlias(sqTable.getAliasName());
							// n.getRight().setTableNameNo(sqTable.getTableName());// as sq tables already
							// numbered
							// }
							correlationConds.add(n);
						}
					}

					// STRING SELECTION conditions

					Vector<Node> stringSelectionConds = cqs.getStringSelectionConds();
					Vector<String> stringConstraints = new Vector<String>();
					stringConds.addAll(stringSelectionConds);
					for (int k = 0; k < stringSelectionConds.size(); k++) {
						// int bit = isCorrelated(stringSelectionConds.get(k), cvc, sqTable, tables,
						// fromorwhere, level);

						// if (bit == 0) {
						if (stringSelectionConds.get(k).isCorrelated == false) {
							stringConstraints.clear();
							String tableName = stringSelectionConds.get(k).getLeft().getTableNameNo()
									.replaceAll("\\d", "").toLowerCase();
							String index = joinTables.contains(tableName) ? tableName + "_i" : "i1";
							strConstr = constrGen.genPositiveCondsForPredF(cvc, queryBlock, stringSelectionConds.get(k),
									index); // FIXME: passing a temporary offset i.e. the last argument in this function
											// call

							stringConstraints.add(strConstr);
							strConstr = "";
							Vector<String> solvedStringConstraint = cvc.getStringSolver().solveConstraints(
									stringConstraints, cvc.getResultsetColumns(), cvc.getTableMap(), true);
							CondToSolvedStringCondition.put(stringSelectionConds.get(k), solvedStringConstraint.get(1));
							for (String str : solvedStringConstraint) {
								str = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));
								strConstr += "\t\t" + str.replace("assert", "") + "\n";
							}
							constr += strConstr;
						} else {
							Node n = stringSelectionConds.get(k);
							n.setIsString(true); // to differenciate string and others
							// if (bit == 2) {
							// n.getLeft().setAliasName(sqTable.getAliasName());
							// n.getLeft().setTable(sqTable);
							// n.getLeft().setTableAlias(sqTable.getAliasName());
							// n.getLeft().setTableNameNo(sqTable.getTableName());
							// } else if (bit == 3) {
							// n.getRight().setAliasName(sqTable.getAliasName());
							// n.getRight().setTable(sqTable);
							// n.getRight().setTableAlias(sqTable.getAliasName());
							// n.getRight().setTableNameNo(sqTable.getTableName());// as sq tables already
							// numbered
							// }
							correlationConds.add(n);
						}

					}
				}
				if (constr != "")
					constr = "(and \n" + constr + "\t\t)";
				String tableIter = "", tableCond = "";
				for (int i = 0; i < tables.size(); i++) {
					tableIter += "(" + joinTables.get(i) + "_i  Int)";
					tableCond += "(" + joinTable + "_map_" + joinTables.get(i) + " " + joinTables.get(i) + "_i "
							+ joinTable + "_i)\n";// (SQ1_pred2 j1 k1)\n
				}
				constraintString += "(assert (forall (" + tableIter + ") \n\t" + (constr != "" ? ("(=> " + constr) : "")
						+ "  ";
				constraintString += "\n \t(exists ((" + joinTable + "_i Int)) ";
				constraintString += "\n\t (and \n" + tableCond + "))\n)\n)" + (constr != "" ? "\n)" : "");
				/*
				 * Note: Parismita: Todo: Convert to API: make function in constraint generator
				 */

				// ********************changes made by
				// sunanda*************************************
				// //parismita - backward pass
				Vector<Integer> tuplesInTable = new Vector<Integer>();
				int tuplesInJoinTable = 1;

				for (String t : joinTables) {
					int noOfTuples = cvc.getNoOfOutputTuples(t);
					tuplesInTable.add(noOfTuples);
					tuplesInJoinTable *= noOfTuples;
				}
				;

				String finalConstraints = "";
				constr = "";
				for (int k = 1; k <= tuplesInJoinTable; k++) {
					tempConstraints = "";
					constr = "\t";
					for (Node n : selectionAndJoinConds) {
						constr += generateConstraintsForSelectionConditions(cvc, n, jtColumns, joinTable, tables, "i1",
								"i1")
								+ "\n\t";
					}
					for (Node n : stringConds) {
						constr += generateConstraintsForStringConditions(cvc, n, jtColumns, joinTable, tables, "i1",
								"i1") + "\n\t";
					}

					for (int index = 0; index < tables.size(); index++) {
						String tempconstrTable1 = "";
						for (int i = 1; i <= tuplesInTable.get(index); i++) {
							tempconstrTable1 += "\t\t(" + joinTable + "_map_" + joinTables.get(index) + " \t"
									+ Integer.toString(i) + "\t" + Integer.toString(k) + ")\n";
						}
						tempConstraints += "\t(or " + tempconstrTable1 + "\n\t)\n ";
					}
					finalConstraints += "(and \n" + constr + "\n" + tempConstraints + "  )\n  ";
				}
				// added by deeksha
				constraintString += "(assert \n (and \n  " + finalConstraints + "\n )\n)";

				// end------------

				ArrayList<String> typeCastedJt = new ArrayList<String>();
				for (Sort s : jtColumnDataTypes) {
					typeCastedJt.add(s.toString());
				}
				SubqueryStructure sqS = new SubqueryStructure();
				sqS.SQDefine.put(joinTable, constraintString);
				sqS.SQColumnsDataTypes.put(joinTable, typeCastedJt);
				sqS.SQColumns.put(joinTable, jtColumns);
				sqS.SQJoinSelectionAndJoinConds.put(joinTable, selectionAndJoinConds);
				sqS.SQJoinCorrelationConds.put(joinTable, correlationConds);

				if (fromorwhere.equalsIgnoreCase("from")) {
					cvc.subqueryConstraintsMap.put("from", sqS);
				} else if (fromorwhere.equalsIgnoreCase("outer")) {
					cvc.subqueryConstraintsMap.put("outer", sqS);
				} else {
					cvc.subqueryConstraintsMap.put("where", sqS);
				}
				constraintString = "";
			}
		}
		// constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	public static String getConstraintsForJoinsWithoutTemp(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n1,
			Node n2, String operator) throws Exception {

		String constraintString = "";

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("false")) {
			/** get the details of each node */
			String t1 = getTableName(n1);
			String t2 = getTableName(n2);

			// int pos1 =
			// cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
			// int pos2 =
			// cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

			// below two lines added by rambabu
			int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(getColumn(n1).getColumnName());
			int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(getColumn(n2).getColumnName());

			String r1 = getTableNameNo(n1);
			String r2 = getTableNameNo(n2);
			logger.log(Level.INFO, "relation2 name num  ---" + r2);

			int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

			/** Get number of tuples of each relation occurrence */
			int tuples1 = 0, tuples2 = 0;
			if (cvc.getNoOfTuples().containsKey(r1)) {

				tuples1 = cvc.getNoOfTuples().get(r1);
			}

			if (cvc.getNoOfTuples().containsKey(r2)) {

				tuples2 = cvc.getNoOfTuples().get(r2);
			}

			int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

			for (int i = 0; i < noOfgroups; i++) {
				/** Do a round robin for the smaller value */
				for (int k = 1, l = 1;; k++, l++) {

					// constraintString += "ASSERT ("+
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
					// ((i*tuples1)+k+offset1-1))+ operator +
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
					// ((i*tuples2)+l+offset2-1))+");\n";
					/*
					 * ConstraintObject constrObj = new ConstraintObject();
					 * constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1,
					 * ((i*tuples1)+k+offset1-1)));
					 * constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,
					 * n2, ((i*tuples2)+l+offset2-1)));
					 * constrObj.setOperator(operator);
					 * constrObjList.add(constrObj);
					 */

					constraintString += constrGen.getAssertConstraint(
							constrGen.genPositiveCondsForPred(queryBlock, n1, ((i * tuples1) + k + offset1 - 1)),
							operator,
							constrGen.genPositiveCondsForPred(queryBlock, n2, ((i * tuples2) + l + offset2 - 1)));

					if (tuples1 > tuples2) {
						if (l == tuples2 && k < tuples1)
							l = 0;
						if (k >= tuples1)
							break;
					} else if (tuples1 < tuples2) {
						if (l < tuples2 && k == tuples1)
							k = 0;
						if (l >= tuples2)
							break;
					} else {// if tuples1==tuples2
						if (l == tuples1)
							break;
					}
				}

			}
		}
		// constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	/**
	 * Updated: parismita
	 * TEMPCODE Rahul Sharma
	 * 
	 * @param cvc
	 * @param f1        : Table 1
	 * @param f2        : Table 2
	 * @param joinTable : Sub Query Table
	 * @return correlation constraints
	 */
	public static String generateConstraintsForCorrelationAttributes(GenerateCVC1 cvc, String joinTable,
			Vector<Node> correlationConds, int level) throws Exception {
		// TODO Auto-generated method stub
		String correlationConstraints = "";
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE");

		if (cvc.subqueryConstraintsMap.get("where").SQjoinWithEXISTS.isEmpty()
				|| cvc.subqueryConstraintsMap.get("where").SQjoinWithEXISTS.get(joinTable) == null)// from - not needed
																									// but
			// sanity check
			return "";

		if (cvc.correlationHashMap == null || cvc.correlationHashMap.isEmpty())
			return "";

		ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());

		if (cvc.subqueryConstraintsMap.get("where").SQjoinWithEXISTS.get(joinTable) == false) {
			// Constraints for not exists
			String constrCorrelationConds = "";
			if (!cvc.correlationHashMap.isEmpty() && level == 1) {
				// in case of NOT EXISTS negate correlation conditions so that there is no tuple
				// in subquery Table
				String str = "";
				for (Node n : cvc.correlationHashMap.keySet()) {
					if (cvc.correlationHashMap.get(n).getProcessLevel() == 0 && !n.getExpired()) {
						String t1 = n.getLeft().getTableNameNo();
						String t2 = n.getRight().getTableNameNo();
						String op = n.getOperator();
						// String innerTable,outerTable;
						if (outerTables.contains(t1)) {
							str += genNegativeConstraintsForCorrelationConds(cvc, n, t2, t1, joinTable, op) + "\n";
						} else if (outerTables.contains(t2)) {
							str += genNegativeConstraintsForCorrelationConds(cvc, n, t1, t2, joinTable, op) + "\n";
						}
					}
				}
				// Vector<String> cntColumnNames = new Vector<String>();
				// for (String col : cvc.tempJoinColumns.get(joinTable)) {
				// if (col.split("__")[1].contains("XDATA_CNT")) {
				// cntColumnNames.add(col);
				// }
				// }
				// Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, joinTable,
				// cvc.tempJoinColumns.get(joinTable).indexOf(cntColumnNames.get(cntColumnNames.size()
				// - 1)),
				// "k1");
				// String cnt = ConstraintGenerator.ctx.mkEq(cntOfSQColumnSQi1,
				// ConstraintGenerator.ctx.mkInt(0))
				// .toString();
				constrCorrelationConds += "\n\t\t" + str + "\n\t\n"; // Sunanda for
																		// correlation:
																		// Changed
																		// forall to
																		// exists for
																		// now, later
																		// will unroll
																		// it
				correlationConstraints += "\n(assert " + constrCorrelationConds + " )\n";
			}

			/**********************************************/

		} else {
			if (!cvc.correlationHashMap.isEmpty() && level == 1) {
				for (Node n : cvc.correlationHashMap.keySet()) {
					String table1 = n.getLeft().getTableNameNo();
					String table2 = n.getRight().getTableNameNo();
					String operator = n.getOperator();
					// String innerTable,outerTable;
					if (outerTables.contains(table1)) {
						correlationConstraints += generateCorrelationConstraints(cvc, n, table2, table1, joinTable,
								operator);
					} else if (outerTables.contains(table2)) {
						correlationConstraints += generateCorrelationConstraints(cvc, n, table1, table2, joinTable,
								operator);
					}
				}
			}
		}
		correlationConstraints += ConstraintGenerator.addCommentLine("CORRELATION CONSTRAINTS FOR SUB QUERY TABLE END");
		return correlationConstraints;
	}

	/**
	 * @author parismita
	 * @param cvc
	 * @param selectionCondition
	 * @param innerTable
	 * @param outerTable
	 * @param joinTable
	 * @param operator
	 * @return
	 */
	public static String genNegativeConstraintsForCorrelationConds(GenerateCVC1 cvc, Node selectionCondition,
			String innerTable, String outerTable, String joinTable, String operator) {
		String constraints = "";
		String sC = selectionCondition.toString();
		innerTable = innerTable.replaceAll("\\d", "").toLowerCase();
		outerTable = outerTable.replaceAll("\\d", "").toLowerCase();
		String correlationAttributeInJoinTable = "";
		String correlationAttributeInOuterTable = "";
		if (selectionCondition.getLeft().getTableNameNo().contains("JSQ")
				|| selectionCondition.getLeft().getTableNameNo().contains("GSQ")
				|| selectionCondition.getLeft().getTableNameNo().contains("DSQ"))
			correlationAttributeInJoinTable = selectionCondition.getLeft().getColumn().getColumnName();
		else
			correlationAttributeInOuterTable = selectionCondition.getLeft().getColumn().getColumnName();
		if (selectionCondition.getRight().getTableNameNo().contains("JSQ")
				|| selectionCondition.getRight().getTableNameNo().contains("GSQ")
				|| selectionCondition.getRight().getTableNameNo().contains("DSQ"))
			correlationAttributeInJoinTable = selectionCondition.getRight().getColumn().getColumnName();
		else
			correlationAttributeInOuterTable = selectionCondition.getRight().getColumn().getColumnName();

		String joinTableIndex = getTableAttributeIndexForJoinTable(cvc, joinTable, innerTable,
				correlationAttributeInJoinTable);
		String outerTableIndex = getTableAttributeIndex(cvc, outerTable, correlationAttributeInOuterTable);
		int numberOfTuplesInOuterTable = cvc.getNoOfOutputTuples(outerTable);
		// sunanda for correlation in not
		int numberOfTuplesInJoinTable = cvc.getNoOfOutputTuples(joinTable);
		String validTupleConstrainst = "(> "
				+ ConstraintGenerator.getConstraintsForValidCount(cvc, outerTable, 1, 0) + " 0)";
		// for (int i = 1; i <= numberOfTuplesInOuterTable; i++) {
		String tempC = "";

		for (int j = 1; j <= numberOfTuplesInJoinTable; j++) {

			Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest(
					cvc, joinTable, cvc.subqueryConstraintsMap.get("where").SQColumns
							.get(joinTable)
							.indexOf(cvc.subqueryConstraintsMap.get("where").SQColumns.get(joinTable)
									.get(cvc.subqueryConstraintsMap.get("where").SQColumns.get(joinTable).size() - 1)),
					j);
			String cnt = "\n\t\t" + ConstraintGenerator.ctx
					.mkEq((ArithExpr) cntOfSQColumnSQi1, ConstraintGenerator.ctx.mkInt(0)).toString();

			tempC += "\n\t(and \n\t\t(not(" + operator + " ("
					+ correlationAttributeInJoinTable + " (select O_" + joinTable + " " + cvc.enumIndexVar + j + " )) ("
					+ outerTable + "_"
					+ correlationAttributeInOuterTable + outerTableIndex + " (select O_" + outerTable + " "
					+ cvc.enumIndexVar + "1 "
					+ ")) ))\n\t"
					+ validTupleConstrainst + "\n\t" + cnt + "\n\t)\n\t";

		}
		constraints = "(or \n\t" + tempC + " )";
		// replace 1 with i

		// }

		return constraints;
	}

	// public static boolean isCorrelated(Node selectionCondition, GenerateCVC1 cvc)
	// {
	// if(selectionCondition.getRight().getColumn()!=null) {
	// String operator = selectionCondition.getOperator();
	// ArrayList<String> tablesInSelectionConditions =
	// getListOfTablesInSelectionConditions(selectionCondition.toString(),operator);
	// ArrayList<String> innerTables =
	// cvc.getqStructure().getWhereClauseSubqueries().get(0).getLstRelationInstances();
	// ArrayList<String> outerTables = getOuterTables(cvc.getBaseRelation());
	// if(innerTables.contains(tablesInSelectionConditions.get(0)) &&
	// outerTables.contains(tablesInSelectionConditions.get(1))) {
	// return true;
	// }
	// else if(innerTables.contains(tablesInSelectionConditions.get(1)) &&
	// outerTables.contains(tablesInSelectionConditions.get(0))){
	// return true;
	// }
	// }
	// return false;
	// }

	// changed/added by sunanda
	// /**
	// * @author parismita
	// * @param selectionCondition
	// * @param cvc
	// * @param sqTable
	// * @param subqueryType
	// * @return
	// */
	// public static Integer isCorrelated(Node selectionCondition, GenerateCVC1 cvc,
	// Table sqTable, Vector<Table> tables,
	// String subqueryType, int level) {
	// if (selectionCondition.getRight() != null &&
	// selectionCondition.getRight().getColumn() != null
	// && selectionCondition.getLeft() != null &&
	// selectionCondition.getLeft().getColumn() != null) {
	// Node left = selectionCondition.getLeft();
	// Node right = selectionCondition.getRight();

	// if (subqueryType.equalsIgnoreCase("from")) // cant have correlation
	// return 0;

	// else if (subqueryType.equalsIgnoreCase("outer")) // cant have crrelation
	// return 0;

	// else if (!(left.getType().equalsIgnoreCase(Node.getColRefType()))
	// || !(right.getType().equalsIgnoreCase(Node.getColRefType())))
	// return 0;

	// // if right or left has current table but not equal
	// // else
	// //
	// if(left.getTable()!=null&&right.getTable()!=null&&((left.getTable().getTableName().equalsIgnoreCase(sqTable.getTableName()))
	// //
	// ||(right.getTable().getTableName().equalsIgnoreCase(sqTable.getTableName())))
	// //
	// &&!left.getTable().getTableName().equalsIgnoreCase(right.getTable().getTableName())
	// // ) return 1;

	// // either of left right equal, both not same but not both then correlation
	// else {
	// Boolean l = true, r = true;
	// String rightTableName = right.getTableNameNo();
	// String leftTableName = left.getTableNameNo();
	// if (left.getTable() != null)
	// leftTableName = left.getTable().getTableName();
	// if (right.getTable() != null)
	// rightTableName = right.getTable().getTableName();

	// for (Table t : tables) {
	// if(t.getTableName().equalsIgnoreCase(leftTableName) && (left.getTableAlias()
	// == null || (cvc.aliasMappingToLevels.get(left.getTableAlias().toLowerCase())
	// == level)))
	// l = false;
	// if(t.getTableName().equalsIgnoreCase(rightTableName) &&
	// (right.getTableAlias() == null ||
	// (cvc.aliasMappingToLevels.get(right.getTableAlias().toLowerCase()) ==
	// level)))
	// r = false;
	// // if (((t.getTableName().equalsIgnoreCase(leftTableName)
	// // && (t.getAliasName() == null || left.getTableAlias() == null
	// // || (t.getAliasName().equalsIgnoreCase(left.getTableAlias()) &&
	// cvc.aliasMappingToLevels.get(t.getAliasName()) != level )))
	// // || (t.getTableName().equalsIgnoreCase(rightTableName)
	// // && (t.getAliasName() == null || right.getTableAlias() == null
	// // || (t.getAliasName().equalsIgnoreCase(right.getTableAlias()) &&
	// cvc.aliasMappingToLevels.get(t.getAliasName()) != level ))))
	// // && (!leftTableName.equalsIgnoreCase(rightTableName)
	// // || !(left.getTableAlias() != null && right.getTableAlias() != null
	// // && left.getTableAlias().equalsIgnoreCase(right.getTableAlias()) &&
	// cvc.aliasMappingToLevels.get(left.getAliasName()) != level &&
	// cvc.aliasMappingToLevels.get(right.getAliasName()) != level))) {
	// // l = l || t.getTableName().equalsIgnoreCase(leftTableName) // left equal
	// // || (t.getAliasName() != null && left.getTableAlias() != null
	// // && t.getAliasName().equalsIgnoreCase(left.getTableAlias()) &&
	// cvc.aliasMappingToLevels.get(t.getAliasName()) != level);
	// // r = r || t.getTableName().equalsIgnoreCase(rightTableName)
	// // || (t.getAliasName() != null && right.getTableAlias() != null
	// // && t.getAliasName().equalsIgnoreCase(right.getTableAlias()) &&
	// cvc.aliasMappingToLevels.get(t.getAliasName()) != level);
	// // }

	// }
	// if (!l && !r)
	// return 0;

	// if (l && r){
	// selectionCondition.isCorrelated = true;
	// return 1;
	// }

	// if (l){
	// selectionCondition.isCorrelated = true;
	// return 2;
	// }

	// if (r){
	// selectionCondition.isCorrelated = true;
	// return 3;
	// }
	// return 0;

	// }

	// }
	// return 0;
	// }

	private static String getTableAttributeIndex(GenerateCVC1 cvc, String table, String attribute) {
		// TODO Auto-generated method stub
		Vector<String> columnIndices = cvc.getTableMap().getTable(table.toUpperCase()).getColumnIndexList();
		return columnIndices.indexOf(attribute) + "";
	}

	private static String getTableAttributeIndexForJoinTable(GenerateCVC1 cvc, String joinTable, String innerTable,
			String attribute) {
		// TODO Auto-generated method stub

		// added comment by we cannot rename directly to sqtable1 because of following
		// code
		// String[] tblList = joinTable.split("join");

		int index = 0;
		// try {
		// for(int i=0; i<tblList.length; i++) {
		// String tbl = tblList[i];
		// Vector<String> columnIndices =
		// cvc.getTableMap().getTable(tbl.toUpperCase()).getColumnIndexList();
		// if(tbl.equals(innerTable)) {
		// index += columnIndices.indexOf(attribute);
		// break;
		// }
		// else
		// index += cvc.getTableMap().getTable(tbl.toUpperCase()).getNoOfColumn();
		// }
		// }
		// try {
		// // for (int i = 0; i < cvc.tempJoinTableName.get(joinTable).size(); i++) {
		// // String tbl = cvc.tempJoinTableName.get(joinTable).get(i);
		// // if (tbl.equals(innerTable)) {
		// // index =
		// cvc.getTableMap().getTable(tbl.toUpperCase()).getColumnIndex(attribute);
		// // break;
		// // } else
		// // index += cvc.getTableMap().getTable(tbl.toUpperCase()).getNoOfColumn();
		// // }
		// } catch (Exception e) {
		// System.out.println(e);
		// }
		return "" + cvc.getTableMap().getSQTableByName(joinTable).getColumnIndex(attribute);

	}

	// parismita
	// FIXME: Sunanda Nov 11
	private static String generateCorrelationConstraints(GenerateCVC1 cvc, Node selectionCondition, String innerTable,
			String outerTable, String joinTable, String operator) {
		// TODO Auto-generated method stub
		String constraints = "";
		if (selectionCondition == null)
			return "";
		String sC = selectionCondition.toString();
		innerTable = innerTable.replaceAll("\\d", "").toLowerCase();
		outerTable = outerTable.replaceAll("\\d", "").toLowerCase();

		String correlationAttributeInJoinTable = "";
		String correlationAttributeInOuterTable = "";
		if (selectionCondition.getLeft().getTableNameNo().contains("JSQ")
				|| selectionCondition.getLeft().getTableNameNo().contains("GSQ")
				|| selectionCondition.getLeft().getTableNameNo().contains("DSQ"))
			correlationAttributeInJoinTable = selectionCondition.getLeft().getColumn().getColumnName();
		else
			correlationAttributeInOuterTable = selectionCondition.getLeft().getColumn().getColumnName();
		if (selectionCondition.getRight().getTableNameNo().contains("JSQ")
				|| selectionCondition.getRight().getTableNameNo().contains("GSQ")
				|| selectionCondition.getRight().getTableNameNo().contains("DSQ"))
			correlationAttributeInJoinTable = selectionCondition.getRight().getColumn().getColumnName();
		else
			correlationAttributeInOuterTable = selectionCondition.getRight().getColumn().getColumnName();

		String correlationAttribute = sC.substring(sC.indexOf(".") + 1, sC.indexOf(operator));
		// String joinTableIndex = getTableAttributeIndexForJoinTable(cvc, joinTable,
		// innerTable, correlationAttribute);
		// String outerTableIndex = getTableAttributeIndex(cvc, outerTable,
		// correlationAttribute);
		String joinTableIndex = getTableAttributeIndexForJoinTable(cvc, joinTable, innerTable,
				correlationAttributeInJoinTable);
		String outerTableIndex = getTableAttributeIndex(cvc, outerTable, correlationAttributeInOuterTable);
		// int numberOfTuplesInOuterTable = cvc.getNoOfOutputTuples(outerTable);
		// // sunanda for correlation in not
		// int numberOfTuplesInJoinTable = cvc.getNoOfOutputTuples(joinTable);
		int tuplesInJoinTable = 1, tuplesInOuterTable = cvc.getNoOfOutputTuples(outerTable);
		// parismita
		Vector<String> joinTables = new Vector<String>();
		Vector<Table> tables = new Vector<Table>();
		Vector<Integer> tuplesInTable = new Vector<Integer>();

		if (cvc.subqueryConstraintsMap.get("where").SQTableName.get(joinTable) != null)
			for (String t : cvc.subqueryConstraintsMap.get("where").SQTableName.get(joinTable)) {
				joinTables.add(t.toLowerCase());
				tables.add(cvc.getTableMap().getTable(t.toUpperCase()));
				int noOfTuples = cvc.getNoOfOutputTuples(t);
				tuplesInTable.add(noOfTuples);
				tuplesInJoinTable *= noOfTuples;
			}
		;
		// testcode by deeksha
		// kasture---------------------------------------------------------
		String tempConstraints = "";

		Expr cntOfOuter = ConstraintGenerator.genSelectTest(cvc, outerTable,
				cvc.getTableMap().getTable(outerTable.toUpperCase()).getNoOfColumn() - 1, 1);
		// String validTupleConstrainst = "(> "
		// + ConstraintGenerator.getConstraintsForValidCount(cvc, outerTable, 1, 0) + "
		// 0)";
		// cnt += "\n\t\t"
		// + ConstraintGenerator.ctx.mkGt((ArithExpr) cntOfOuter,
		// ConstraintGenerator.ctx.mkInt(0)).toString();

		int numberOfTuplesInOuterTable = cvc.getNoOfOutputTuples(outerTable);
		int numberOfTuplesInJoinTable = cvc.getNoOfOutputTuples(joinTable);
		// sunanda for correlation in not
		String tempC = "";

		// for (int i = 1; i <= numberOfTuplesInOuterTable; i++) {

		String validTupleConstrainst = "(> "
				+ ConstraintGenerator.getConstraintsForValidCount(cvc, outerTable, 1, 0) + " 0)"; // replace 1 with i
		// for (int j = 1; j <= numberOfTuplesInJoinTable; j++) {

		// Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest(cvc, joinTable,
		// cvc.tempJoinColumns
		// .get(joinTable)
		// .indexOf(cvc.tempJoinColumns.get(joinTable).get(cvc.tempJoinColumns.get(joinTable).size()
		// - 1)), j);
		// String cnt = "\n\t\t" + ConstraintGenerator.ctx
		// .mkEq((ArithExpr) cntOfSQColumnSQi1,
		// ConstraintGenerator.ctx.mkInt(0)).toString();

		// tempC += "\n\t(and \n\t\t(not(" + operator + " ("
		// + correlationAttributeInJoinTable + " (select O_" + joinTable + " " + j + "
		// )) (" + outerTable + "_"
		// + correlationAttributeInOuterTable + outerTableIndex + " (select O_" +
		// outerTable + " 1 "
		// + ")) ))\n\t"
		// + validTupleConstrainst + "\n\t" + cnt + "\n\t)\n\t";

		// }

		for (int j = 1; j <= numberOfTuplesInJoinTable; j++) {
			Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest(
					cvc, joinTable, cvc.subqueryConstraintsMap.get("where").SQColumns
							.get(joinTable)
							.indexOf(cvc.subqueryConstraintsMap.get("where").SQColumns.get(joinTable)
									.get(cvc.subqueryConstraintsMap.get("where").SQColumns.get(joinTable).size() - 1)),
					j);
			String cnt = "\n\t\t" + ConstraintGenerator.ctx
					.mkGt((ArithExpr) cntOfSQColumnSQi1, ConstraintGenerator.ctx.mkInt(0)).toString();

			String temp = "";
			if (operator.equals("/="))
				temp += "\n\t\t" + "(not " + ConstraintGenerator.genEquiCondTest(cvc, joinTable,
						Integer.parseInt(joinTableIndex), j, outerTable, Integer.parseInt(outerTableIndex), 1) // replace
																												// 1
																												// with
																												// i
						+ "\n)";
			else
				temp += "\n\t\t" + ConstraintGenerator.genEquiCondTest(cvc, joinTable,
						Integer.parseInt(joinTableIndex), j, outerTable, Integer.parseInt(outerTableIndex), 1) // replace
																												// 1
																												// with
																												// i
						+ "\n";
			tempC += "(and " + temp + "\n\t" + validTupleConstrainst + "\n\t" + cnt + ")";
			// }

			// constraints += "\n\t(and \n\t\t(not("+operator+" ("+
			// joinTable+"_"+innerTable+"__"+correlationAttribute+joinTableIndex+" (select
			// O_"+joinTable+" k1)) ("+outerTable+"_"+correlationAttribute+outerTableIndex+"
			// (select O_"+outerTable+" " + i +")) ))\n\t" + validTupleConstrainst +
			// "\n\t)\n\t";
		}
		tempConstraints = "(assert \n\t(or \n\t\t" + tempC + "\n\t) \n)";
		return tempConstraints;
		//
		// end--------------

	}

	private static ArrayList<String> getOuterTables(HashMap<String, String> baseRelation) {
		// TODO Auto-generated method stub
		Iterator<Entry<String, String>> it = baseRelation.entrySet().iterator();
		ArrayList<String> tables = new ArrayList<String>();
		while (it.hasNext()) {
			Map.Entry<String, String> temp = (Map.Entry<String, String>) it.next();
			tables.add(temp.getValue());
		}
		return tables;
	}

	private static ArrayList<String> getListOfTablesInSelectionConditions(String selectionCondition, String operator) {
		// TODO Auto-generated method stub
		StringTokenizer st = new StringTokenizer(selectionCondition, operator);
		String table1 = st.nextToken();
		String table2 = st.nextToken();
		table1 = table1.substring(1, table1.indexOf('.'));
		table2 = table2.substring(0, table2.indexOf('.'));
		ArrayList<String> tables = new ArrayList<String>();
		tables.add(table1);
		tables.add(table2);
		return tables;
	}

	public static String getTableName(Node n1) {
		if (n1.getColumn() != null)
			return n1.getColumn().getTableName();
		else if (n1.getLeft().getColumn() != null)
			return n1.getLeft().getColumn().getTableName();
		else
			return n1.getLeft().getColumn().getTableName();
	}

	public static String getTableNameNo(Node n1) {
		if (n1.getTableNameNo() != null)
			return n1.getTableNameNo();
		else if (n1.getLeft().getTableNameNo() != null)
			return n1.getLeft().getTableNameNo();
		else
			return n1.getLeft().getTableNameNo();
	}

	public static Column getColumn(Node n1) {
		if (n1.getColumn() != null)
			return n1.getColumn();
		else if (n1.getLeft().getColumn() != null)
			return n1.getLeft().getColumn();
		else
			return n1.getLeft().getColumn();
	}

	/**
	 * @author - parismita
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param operator
	 * @return
	 */
	public static String getConstraintsForJoinsInSameQueryBlock2(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Node n1, Node n2, String operator) {
		// block2 for old code - no need now
		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2 = 0;
		if (cvc.getNoOfTuples().containsKey(r1)) {

			tuples1 = cvc.getNoOfTuples().get(r1) * UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
		}

		if (cvc.getNoOfTuples().containsKey(r2)) {

			tuples2 = cvc.getNoOfTuples().get(r2) * UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);
		}

		int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

		for (int i = 0; i < noOfgroups; i++) {
			/** Do a round robin for the smaller value */
			for (int k = 1, l = 1;; k++, l++) {

				// constraintString += "("+
				// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
				// ((i*tuples1)+k+offset1-1))+ operator +
				// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
				// ((i*tuples2)+l+offset2-1))+") AND ";
				ConstraintObject constrObj = new ConstraintObject();
				constrObj.setLeftConstraint(
						constrGen.genPositiveCondsForPred(queryBlock, n1, ((i * tuples1) + k + offset1 - 1)));
				constrObj.setRightConstraint(
						constrGen.genPositiveCondsForPred(queryBlock, n2, ((i * tuples2) + l + offset2 - 1)));
				constrObj.setOperator(operator);
				constrObjList.add(constrObj);

				if (tuples1 > tuples2) {
					if (l == tuples2 && k < tuples1)
						l = 0;
					if (k >= tuples1)
						break;
				} else if (tuples1 < tuples2) {
					if (l < tuples2 && k == tuples1)
						k = 0;
					if (l >= tuples2)
						break;
				} else {/** if tuples1==tuples2 */
					if (l == tuples1)
						break;
				}
			}
		}
		return constrGen.generateANDConstraintsWithAssert(constrObjList);
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions which are in
	 * same query block
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param string
	 * @return
	 */
	public static String getConstraintsForJoinsInSameQueryBlock1(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Node n1, Node n2, String operator) {

		String constraintString = "";

		/** get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		// int pos1 =
		// cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		// int pos2 =
		// cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(n1.getColumn().getColumnName()); // added
																												// by
																												// rambabu
		int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(n2.getColumn().getColumnName()); // added
																												// by
																												// rambabu

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			isTempJoin = true;
		} else {
			isTempJoin = false;
		}

		if (!isTempJoin) {
			String r1 = n1.getTableNameNo();
			String r2 = n2.getTableNameNo();
			int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

			/** Get number of tuples of each relation occurrence */
			int tuples1 = 0, tuples2 = 0;
			if (cvc.getNoOfTuples().containsKey(r1)) {

				tuples1 = cvc.getNoOfTuples().get(r1)
						* UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
			}

			if (cvc.getNoOfTuples().containsKey(r2)) {

				tuples2 = cvc.getNoOfTuples().get(r2)
						* UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);
			}

			int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

			for (int i = 0; i < noOfgroups; i++) {
				/** Do a round robin for the smaller value */
				for (int k = 1, l = 1;; k++, l++) {

					// constraintString += "("+
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
					// ((i*tuples1)+k+offset1-1))+ operator +
					// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
					// ((i*tuples2)+l+offset2-1))+") AND ";
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(
							constrGen.genPositiveCondsForPred(queryBlock, n1, ((i * tuples1) + k + offset1 - 1)));
					constrObj.setRightConstraint(
							constrGen.genPositiveCondsForPred(queryBlock, n2, ((i * tuples2) + l + offset2 - 1)));
					constrObj.setOperator(operator);
					constrObjList.add(constrObj);

					if (tuples1 > tuples2) {
						if (l == tuples2 && k < tuples1)
							l = 0;
						if (k >= tuples1)
							break;
					} else if (tuples1 < tuples2) {
						if (l < tuples2 && k == tuples1)
							k = 0;
						if (l >= tuples2)
							break;
					} else {/** if tuples1==tuples2 */
						if (l == tuples1)
							break;
					}
				}
			}
			constraintString = constrGen.generateANDConstraintsWithAssert(constrObjList);
		} else if (isTempJoin) {
			// Join Temp table implementation
			Vector<String> tablesAdded = new Vector<String>();
			Table f1, f2;
			String temp1, temp2, joinTable, ColName;
			int t1Columnindex, t2Columnindex;
			int findex = 0;
			f1 = n1.getTable();
			f2 = n2.getTable();
			temp1 = f1.getTableName();
			temp2 = f2.getTableName();
			// TEMPCODE Rahul Sharma : Check if the tables are part of nested query, if so
			// proceed further to generate sub query table constraints, otherwise break
			boolean isPartOfSubQuery = checkIfTablesArePartOfSubQuery(cvc, temp1, temp2, queryBlock.getLevel(), "");

			if (isPartOfSubQuery) {

				joinTable = temp1 + "join" + temp2;
				// added by deeksha
				// joinTable = "SQtable1";
				if (!tablesAdded.contains(joinTable)) {
					// constraintString += "\n (declare-datatypes () (("+joinTable +"_TupleType" +
					// "("+joinTable +"_TupleType ";
					// TEMPCODE START : Rahul Sharma
					// handled incorrect parenthesis
					constraintString = "(declare-datatypes ((" + joinTable + "_TupleType 0))" + "(((" + joinTable
							+ "_TupleType ";
					// TEMPCODE END : Rahul Sharma

					for (String key : f1.getColumns().keySet()) {
						ColName = f1.getColumns().get(key).getColumnName();
						String s = f1.getColumns().get(key).getCvcDatatype();
						if (s != null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME")
								|| s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "(" + joinTable + "_" + f1.getColumns().get(key) + findex + " " + s
									+ ") ";
						else
							constraintString += "(" + joinTable + "_" + f1.getColumns().get(key) + findex + " "
									+ ColName + ") ";
						findex++;
					}
					int delimit = findex;
					for (String key : f2.getColumns().keySet()) {
						ColName = f2.getColumns().get(key).getColumnName();
						String s = f2.getColumns().get(key).getCvcDatatype();
						if (s != null && (s.equalsIgnoreCase("Int") || s.equalsIgnoreCase("Real") || s.equals("TIME")
								|| s.equals("DATE") || s.equals("TIMESTAMP")))
							constraintString += "(" + joinTable + "_" + f2.getColumns().get(key) + findex + " " + s
									+ ") ";
						else
							constraintString += "(" + joinTable + "_" + f2.getColumns().get(key) + findex + " "
									+ ColName + ") ";
						findex++;
					}
					constraintString += ") )) )\n";
					// Now create the Array for this TupleType
					constraintString += "(declare-fun O_" + joinTable + " () (Array Int " + joinTable
							+ "_TupleType))\n\n";

					t1Columnindex = n1.getColumn().getTable().getColumnIndex(n1.getColumn().getColumnName());
					t2Columnindex = n2.getColumn().getTable().getColumnIndex(n2.getColumn().getColumnName());

					ConstraintGenerator constrGen = new ConstraintGenerator();

					String constraint1 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n1, "i1");
					String constraint2 = constrGen.genPositiveCondsForPredF(cvc, queryBlock, n2, "j1");

					constraintString += "(assert (forall ((i1 Int)(j1 Int))(=> ("
							+ (operator.equals("/=") ? "not (= " : operator) + "  " + constraint1 + "  " + constraint2
							+ (operator.equals("/=") ? " )" : " " + ") \n");
					// constraintString += "(forall ((i1 Int)(j1 Int))(=> ("+
					// (operator.equals("/=")? "not (= ": operator) +" "+constraint1+ "
					// "+constraint2+ (operator.equals("/=")? " )":" "+ ") \n");

					// String constraint3 =
					// "("+joinTable+"_"+n1.getColumn().getColumnName()+t1Columnindex;
					// constraint3 += "("+" select O_"+joinTable+" "+" k1 ) )";

					// constraintString += "(exists ((k1 Int)) (and (" + (operator.equals("/=")?
					// "not (= ": operator) +" "+constraint1+ " "+constraint3+
					// (operator.equals("/=")? " )":" "+ ") \n"); // TEMPCODE Rahul Sharma :
					// Commented
					constraintString += "(exists ((k1 Int)) ";

					t2Columnindex += delimit;
					// String constraint4 =
					// "("+joinTable+"_"+n2.getColumn().getColumnName()+t2Columnindex;
					// constraint4 += "("+" select O_"+joinTable+" "+" k1 ) )";
					//
					// constraintString += " (" + (operator.equals("/=")? "not (= ": operator) +"
					// "+constraint2+ " "+constraint4+ (operator.equals("/=")? " )":" "+ ")))))
					// )\n");

					// TEMPCODE START : Rahul Sharma
					ArrayList<String> jt = new ArrayList<String>();
					jt = createTempTableColumns(joinTable, f1, f2).get("Names");
					constraintString += generateConstraintsForAllAttributes(f1, f2, jt, joinTable) + ") ) ) )";
					// System.out.println(constraintString);
					// TEMPCODE END : Rahul Sharma

					// TEMPCODE START : Rahul Sharma
					// commented these lines, [FIXME: this constraints leads to infinite loops]
					// constraintString += "(assert (forall ((k1 Int)) (exists ((i1 Int)(j1 Int))
					// (and (" + (operator.equals("/=")? "not (= ": operator) +" "+constraint1+ "
					// "+constraint3+ (operator.equals("/=")? " )":" " + " )\n");

					// constraintString += "(assert (forall ((k1 Int)) (=> (and (<= 0 k1) (<= k1
					// 10))"
					// + "(exists ((i1 Int)(j1 Int)) (and (and (<= 0 i1) (<= i1 10)) (and (<= 0 j1)
					// (<= j1 10)) "
					// + " (" + (operator.equals("/=")? "not (= ": operator) +" "+constraint1+ "
					// "+constraint3+ (operator.equals("/=")? " )":" " + " ))\n");

					// constraintString += "("+ (operator.equals("/=")? "not (= ": operator) +"
					// "+constraint2+ " "+constraint4+ (operator.equals("/=")? " )":" " +
					// ")))))\n");
					constraintString += generateConstraintsForAllAndExistsAttributes(cvc, n1, n2, f1, f2, jt, joinTable,
							1,
							new Vector<Node>());
					// TEMPCODE END : Rahul Sharma
				}
			} else {
				// TEST CODE: Pooja

				String r1 = n1.getTableNameNo();
				String r2 = n2.getTableNameNo();
				int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
				int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

				/** Get number of tuples of each relation occurrence */
				int tuples1 = 0, tuples2 = 0;
				if (cvc.getNoOfTuples().containsKey(r1)) {

					tuples1 = cvc.getNoOfTuples().get(r1)
							* UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
				}

				if (cvc.getNoOfTuples().containsKey(r2)) {

					tuples2 = cvc.getNoOfTuples().get(r2)
							* UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);
				}

				int noOfgroups = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);
				ConstraintGenerator constrGen = new ConstraintGenerator();
				ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

				for (int i = 0; i < noOfgroups; i++) {
					/** Do a round robin for the smaller value */
					for (int k = 1, l = 1;; k++, l++) {

						// constraintString += "("+
						// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
						// ((i*tuples1)+k+offset1-1))+ operator +
						// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
						// ((i*tuples2)+l+offset2-1))+") AND ";
						ConstraintObject constrObj = new ConstraintObject();

						constrObj.setLeftConstraint(
								constrGen.genPositiveCondsForPred(queryBlock, n1, ((i * tuples1) + k + offset1 - 1)));
						constrObj.setRightConstraint(
								constrGen.genPositiveCondsForPred(queryBlock, n2, ((i * tuples2) + l + offset2 - 1)));
						constrObj.setOperator(operator);
						constrObjList.add(constrObj);
						int i1 = (i * tuples1) + k + offset1 - 1;
						Column cntCol1 = n1.getTable().getColumn(n1.getTable().getNoOfColumn() - 1); // added by sunanda
																										// for count
						// BoolExpr WithCount1 =
						// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1,
						// ConstraintGenerator.ctx.mkInt(i1)), ConstraintGenerator.ctx.mkInt(0)); //
						// added by sunanda for count

						constrObj = new ConstraintObject();
						EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);

						if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
							constrObj.setLeftConstraint(
									ConstraintGenerator
											.smtMap(cntCol1,
													ConstraintGenerator.ctx.mkConst(cvc.enumIndexVar + i1, currentSort))
											.toString());
						} else {
							constrObj.setLeftConstraint(
									ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(i1)).toString());
						}

						constrObj.setOperator(">");
						constrObj.setRightConstraint(ConstraintGenerator.ctx.mkInt(0).toString());

						ConstraintObject constrObj1 = new ConstraintObject();
						int i2 = (i * tuples2) + l + offset2 - 1;
						Column cntCol2 = n2.getTable().getColumn(n2.getTable().getNoOfColumn() - 1); // added by sunanda
																										// for count
						if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
							constrObj1.setLeftConstraint(
									ConstraintGenerator
											.smtMap(cntCol2,
													ConstraintGenerator.ctx.mkConst(cvc.enumIndexVar + i2, currentSort))
											.toString());
						} else {
							constrObj1.setLeftConstraint(
									ConstraintGenerator.smtMap(cntCol2, ConstraintGenerator.ctx.mkInt(i2)).toString());
						}

						constrObj1.setOperator(">");
						constrObj1.setRightConstraint(ConstraintGenerator.ctx.mkInt(0).toString());

						if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
							constrObjList.add(constrObj);
							constrObjList.add(constrObj1);
						}
						// BoolExpr WithCount2 =
						// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol2,
						// ConstraintGenerator.ctx.mkInt(i1)), ConstraintGenerator.ctx.mkInt(0)); //
						// added by sunanda for count

						if (tuples1 > tuples2) {
							if (l == tuples2 && k < tuples1)
								l = 0;
							if (k >= tuples1)
								break;
						} else if (tuples1 < tuples2) {
							if (l < tuples2 && k == tuples1)
								k = 0;
							if (l >= tuples2)
								break;
						} else {/** if tuples1==tuples2 */
							if (l == tuples1)
								break;
						}
					}
				}
				constraintString = constrGen.generateANDConstraintsWithAssert(constrObjList);

			}

		}
		// Join Temp table implementation end

		return constraintString;
	}

	/**
	 * TEMPCODE Rahul Sharma : To check if the tables are part of sub query - to
	 * generate sub query table constraints
	 * 
	 * @param queryBlock : query structure
	 * @param table1     : table1 name
	 * @param table2     : table2 name
	 * @return true if tables are a part of subquery, false otherwise
	 */
	// changes made by sunanda
	private static boolean checkIfTablesArePartOfSubQuery(GenerateCVC1 cvc, String table1, String table2, int level,
			String subqueryType) {
		// TODO Auto-generated method stub
		Vector<QueryStructure> subqueries = new Vector<QueryStructure>();
		if (subqueryType.equalsIgnoreCase("from"))
			subqueries = cvc.getqStructure().getFromClauseSubqueries();
		else if (subqueryType.equalsIgnoreCase("outer"))
			return true;
		else
			subqueries = cvc.getqStructure().getWhereClauseSubqueries();
		// if(level != 0)
		// subqueries.add(getqStructureByLevel(cvc.getqStructure(), level));

		for (int i = 0; i < subqueries.size(); i++) {
			ArrayList<String> baseRelations = subqueries.get(i).getLstRelations();
			ArrayList<String> relations = new ArrayList<String>();
			for (int j = 0; j < baseRelations.size(); j++) {
				relations.add(baseRelations.get(j).replaceAll("\\d", "").toLowerCase());
				// relations.add(baseRelations.get(j).replaceAll("\\d", "").toUpperCase());
			}
			if (relations.contains(table1) && relations.contains(table2) && !table1.equals(table2))
				return true;
		}
		return false;
	}

	private static QueryStructure getqStructureByLevel(QueryStructure qs, int level) {
		QueryStructure qstruct = qs;
		if (qs.getWhereClauseSubqueries() == null && qs.getFromClauseSubqueries() == null)
			return qs;
		int flag = 0;
		for (int i = 0; i < qs.getWhereClauseSubqueries().size(); i++) {
			qstruct = getqStructureByLevel(qs.getWhereClauseSubqueries().get(i), level);
			if (qstruct != null && qstruct.getLevel() == level) {
				flag = 1;
				break;
			}
		}
		if (flag == 0)
			for (int i = 0; i < qs.getFromClauseSubqueries().size(); i++) {
				qstruct = getqStructureByLevel(qs.getFromClauseSubqueries().get(i), level);
				if (qstruct != null && qstruct.getLevel() == level)
					break;
			}
		return qstruct;
	}

	/**
	 * function overloaded
	 * TEMPCODE Rahul Sharma : To check if the tables are part of sub query - to
	 * generate sub query table constraints
	 * 
	 * @param queryBlock : query structure
	 * @param tables     : table1 name
	 * @author parismita
	 * @return true if any subquery has all the tables, false otherwise
	 */
	// changes made by sunanda
	private static boolean checkIfTablesArePartOfSubQuery(GenerateCVC1 cvc, Vector<String> tables, int level,
			String subqueryType) {

		// if(level == 0)
		// return false;
		QueryStructure subqueries = getqStructureByLevel(cvc.getqStructure(), level);

		ArrayList<String> baseRelations = subqueries.getLstRelations();
		ArrayList<String> relations = new ArrayList<String>();
		for (int j = 0; j < baseRelations.size(); j++) {
			relations.add(baseRelations.get(j).replaceAll("\\d", "").toLowerCase());
		}
		int flag = 0;
		for (String table : tables) {
			if (!relations.contains(table) && !table.startsWith("JSQ") && !table.startsWith("GSQ")
					&& !table.startsWith("DSQ"))
				flag = 1;
		}
		if (flag == 0)
			return true;
		// return true if any subquery has all the tables

		return false;
	}

	/**
	 * TEMPCODE Rahul Sharma : subquery table column names, datatypes and primary
	 * keys
	 * 
	 * @param joinTable
	 * @param t1
	 * @param t2
	 * @author parismita
	 * @return
	 * @throws Exception
	 */
	private static HashMap<String, Object> createTempTableColumnsForJoins(GenerateCVC1 cvc, String joinTable,
			Vector<Table> tables,
			boolean isExist, String sqType, int level) throws Exception {
		HashMap<String, Object> columnNameAndDataTypes = new HashMap<String, Object>();
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<Sort> columnDataTypes = new ArrayList<Sort>();
		ArrayList<String> primaryKey = new ArrayList<String>();
		int count = 0;
		String columnName = "";
		Table sqi = new Table(joinTable);
		Map<String, Table> tableMap = new HashMap<String, Table>();
		int f = 0;
		Vector<Table> removeT = new Vector<Table>();
		Vector<String> countColumnsOfBaseTables = new Vector<String>();

		for (Table t : tables) {
			HashMap<String, Column> t1_columns = t.getColumns();
			Boolean skipPrimaryKey = false;
			if ((t.getTableName().startsWith("JSQ") || t.getTableName().startsWith("GSQ")
					|| t.getTableName().startsWith("DSQ")) && !t.getSQType().equalsIgnoreCase("from")) {
				f = 1;
				// Sunanda for subquery with corr
				HashMap<String, Column> columnsNeedMapping = getRequiredMappingFromSubquery(cvc, t, level);
				if (columnsNeedMapping.size() == 0)
					continue;
				else {
					t1_columns = columnsNeedMapping;
					skipPrimaryKey = true; // FIXME
				}
			}

			for (Column c : t1_columns.values()) {
				if ((t.getTableName().startsWith("JSQ") || t.getTableName().startsWith("GSQ")
						|| t.getTableName().startsWith("DSQ"))) {
					columnName = joinTable + "_" + c.getColumnName()
							+ t.getColumnIndex(c.getColumnName());
				} else {
					columnName = joinTable + (f == 0 ? ("_" + t.getTableName() + "_") : "") + "_" + c.getColumnName()
							+ t.getColumnIndex(c.getColumnName());
				}

				// + count; // commented by sunanda testtt
				columns.add(columnName);

				columnDataTypes.add(ConstraintGenerator.getColumnSort(c.getCvcDatatype()));
				int flag = 0;
				if (t.getPrimaryKey().contains(c) && !skipPrimaryKey) {
					primaryKey.add(columnName);
					flag = 1;
				}
				count++;
				Column col = new Column(c);
				col.setColumnName(columnName);
				col.setTableName(joinTable);
				col.setTable(sqi);
				col.setBaseRelation(t.getTableName());
				sqi.addColumn(col);
				if (flag == 1)
					sqi.addColumnInPrimaryKey(col);

				if (columnName.contains("XDATA_CNT"))
					countColumnsOfBaseTables.add(columnName);
			}
			f = 0;

		}
		if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
			columns.add(joinTable + "__XDATA_CNT");
			columnDataTypes.add(ConstraintGenerator.ctx.getIntSort());
			Column col = new Column(joinTable + "__XDATA_CNT", sqi);
			col.setDataType(4);// INT
			col.setCvcDatatype("INT");
			col.setColumnSize(0);
			sqi.addColumn(col);
			countColumnsOfBaseTables.add(joinTable + "__XDATA_CNT");
		}

		sqi.setSQType(sqType);
		sqi.setIsExist(isExist);
		tableMap.put(joinTable, sqi);
		columnNameAndDataTypes.put("Names", columns);
		columnNameAndDataTypes.put("DataTypes", columnDataTypes);
		columnNameAndDataTypes.put("PrimaryKey", primaryKey);
		columnNameAndDataTypes.put("TableMap", tableMap);
		columnNameAndDataTypes.put("CountColumns", countColumnsOfBaseTables);
		return columnNameAndDataTypes;
	}

	/**
	 * This method is used to push tables from above levels in case of correlation
	 * with not exists connective
	 * 
	 * @author Sunanda
	 * @param GenerateCVC1    cvc
	 * @param Vector
	 *                        <Table>
	 *                        table
	 * @param int             level
	 * @param Vector<String>  joinTables
	 * @param Vector<Integer> tuplesInJoin
	 * @return constraints
	 */
	private static void pushTablesFromAboveLevelsForCorrelation(GenerateCVC1 cvc, Vector<Table> table, int level,
			Vector<String> joinTables, Vector<Integer> tuplesInJoin) throws Exception {
		HashMap<Node, correlationStructure> temp = new HashMap<Node, correlationStructure>();
		for (Node n : cvc.correlationHashMap.keySet()) {
			correlationStructure cs = cvc.correlationHashMap.get(n);
			if ((cs.queryType.equalsIgnoreCase("NOT EXISTS") || cs.getIsEquiJoin() == false) && !n.getExpired()) {
				if (cs.isPushedDown && (cs.getPushTableDownLevel() != 0 && cs.getPushTableDownLevel() >= level)) {
					if (n.getLeft() != null && n.getLeft().getLevel() < level) {
						// Left table to be pushed down
						// Will work for single condition
						// Pushed down the entire table
						Table pushedDownTable = (Table) n.getLeft().getTable().clone();
						// pushedDownTable.getColumns().clear();
						Vector<String> columnnames = new Vector<String>(pushedDownTable.getColumns().keySet());

						Vector<Sort> columntypes = new Vector<Sort>();
						for (String col : columnnames) {
							columntypes.add(
									ConstraintGenerator.getColumnSort(pushedDownTable.getColumn(col).getCvcDatatype()));
							// pushedDownTable.getColumns().put(col, ((Table)
							// n.getLeft().getTable()).getColumn(col));
						}

						// pushedDownTable.getColumns().putAll();
						pushedDownTable.setAliasName("pushed_" + pushedDownTable.getTableName() + level);
						for (String colName : pushedDownTable.getColumns().keySet()) {
							pushedDownTable.getColumn(colName).setTable(pushedDownTable);
						}
						Map<String, Table> tablesOfMap = new HashMap<String, Table>();
						tablesOfMap.put("pushed_" + pushedDownTable.getTableName() + level, pushedDownTable);

						cvc.getTableMap().putTables(tablesOfMap);

						// put table in context

						ConstraintGenerator.putTableInCtx(cvc, columnnames, columntypes,
								"pushed_" + pushedDownTable.getTableName() + level);
						// will be stored table1's cols with count then table2's then 3's -
						// parismita

						// pushedDownTable.addColumn(n.getLeft().getColumn()); // put relavent column

						if (!table.contains(pushedDownTable)) {
							table.add(pushedDownTable); // Need aliasing of this table, need to make it unique
							joinTables.add(pushedDownTable.getAliasName());
							tuplesInJoin.add(cvc.getNoOfOutputTuples(pushedDownTable.getAliasName()));
						}
						// if(cs.isAggr==true) continue; //parismita
						// Add correlation condition of passing down
						// Create left node of condition
						Node rightNode;// = Node.createNode(n.getRight().getColumn(), n.getRight().getTable());
						// rightNode.setTableAlias(n.getRight().getTable().getAliasName());
						// rightNode.setTableNameNo(n.getRight().getTable().getTableName());
						// rightNode.level = cs.getOriginalLevelRight();
						rightNode = n.getRight().clone();
						// Create right node of condition
						// Outer table or right of condtion?

						// new table
						Node leftNode = Node.createNode(
								pushedDownTable.getColumn(n.getLeft().getColumn().getColumnName()), pushedDownTable);
						leftNode.setTableAlias(pushedDownTable.getAliasName());
						leftNode.setTableNameNo(pushedDownTable.getTableName());
						leftNode.level = level;

						// Create condition
						Node condition = new Node();
						condition.setLeft(leftNode);
						condition.setRight(rightNode);
						condition.level = level;
						condition.setOperator(n.getOperator());
						condition.setExpired(false);
						condition.isCorrelated = true;
						condition.setType(n.getType());
						// more params need to be added from node n - parismita (but currently
						// sufficient)

						// Put condition into correlation map
						correlationStructure csTemp = new correlationStructure(condition);
						csTemp.setQueryType(cvc.levelToQueryTypeHashMap.get(level));
						csTemp.setProcessLevel(cs.getPushTableDownLevel());
						csTemp.setIsPushedDown(true);
						csTemp.setIsEquiJoin(cs.getIsEquiJoin());
						csTemp.setIsAggr(cs.isAggr);
						// note: if isAggr on level - then make it true but how to find that? -
						// parismita
						temp.put(condition, csTemp);

					} else if (n.getRight() != null && n.getRight().getLevel() < level) {
						// Right table to be pushed down
						// Add correlation condition of passing down
						Table pushedDownTable = (Table) n.getRight().getTable().clone();
						pushedDownTable.getColumns().putAll(((Table) n.getLeft().getTable()).getColumns());

						pushedDownTable.setAliasName("pushed_" + pushedDownTable.getTableName() + level); // temporary
																											// table
																											// alias
						for (String colName : pushedDownTable.getColumns().keySet()) {
							pushedDownTable.getColumn(colName).setTable(pushedDownTable);
						}
						// pushedDownTable.addColumn(n.getLeft().getColumn()); // put relavent column

						Map<String, Table> tablesOfMap = new HashMap<String, Table>();
						tablesOfMap.put("pushed_" + pushedDownTable.getTableName() + level, pushedDownTable);
						cvc.getTableMap().putTables(tablesOfMap);

						// put table in context
						Vector<String> columnnames = new Vector<String>(pushedDownTable.getColumns().keySet());

						Vector<Sort> columntypes = new Vector<Sort>();
						for (String col : columnnames) {
							columntypes.add(ConstraintGenerator.getColumnSort(col));
						}

						ConstraintGenerator.putTableInCtx(cvc, columnnames, columntypes,
								"pushed_" + pushedDownTable.getTableName() + level);

						if (!table.contains(pushedDownTable)) {
							table.add(pushedDownTable); // Need aliasing of this table, need to make it unique
							joinTables.add(pushedDownTable.getTableName());
							tuplesInJoin.add(cvc.getNoOfOutputTuples(pushedDownTable.getTableName()));
						}
						// if(cs.isAggr==true) continue;
						// Add correlation condition of passing down

						// Create left node of condition
						Node leftNode;// = Node.createNode(n.getLeft().getColumn(), n.getLeft().getTable());
						// leftNode.setTableAlias(n.getLeft().getTable().getAliasName());
						// leftNode.setTableNameNo(n.getLeft().getTable().getTableName());
						// leftNode.level = cs.getOriginalLevelLeft();
						leftNode = n.getLeft().clone();

						// Create right node of condition
						Node rightNode = Node.createNode(
								pushedDownTable.getColumn(n.getRight().getColumn().getColumnName()), pushedDownTable);
						rightNode.level = level;
						rightNode.setTableAlias(pushedDownTable.getAliasName());
						rightNode.setTableNameNo(pushedDownTable.getTableName());

						// Create condition
						Node condition = new Node();
						condition.setLeft(leftNode);
						condition.setRight(rightNode);
						condition.level = level;
						condition.setOperator(n.getOperator());
						condition.setExpired(false);
						condition.isCorrelated = true;

						// Put condition into correlation map
						correlationStructure csTemp = new correlationStructure(condition);
						csTemp.setQueryType(cvc.levelToQueryTypeHashMap.get(level));
						csTemp.setProcessLevel(cs.getPushTableDownLevel());
						csTemp.setIsPushedDown(true);
						csTemp.setIsAggr(cs.isAggr);
						temp.put(condition, csTemp);
					}
				}
			}
		}
		cvc.correlationHashMap.putAll(temp);
	}

	private static HashMap<String, Column> getRequiredMappingFromSubquery(GenerateCVC1 cvc, Table t, int level) {
		HashMap<String, Column> columns = new LinkedHashMap<String, Column>();
		for (Node n : cvc.correlationHashMap.keySet()) {
			if (n.isCorrelated && n.getExpired() == false) {
				correlationStructure cs = cvc.correlationHashMap.get(n);
				if (cs != null && cs.getProcessLevel() < level) {
					// do something
					// update the table name and column in node from below level JSQ
					if (n.getLeft() != null && n.getLeft().getTableNameNo() != null
							&& n.getLeft().getTableNameNo().equalsIgnoreCase(t.getTableName())) {
						columns.put(n.getLeft().getColumn().getColumnName(), n.getLeft().getColumn());
						columns.put(t.getColumn(t.getColumns().size() - 1).getColumnName(),
								t.getColumn(t.getColumns().size() - 1));

					}
					if (n.getRight() != null && n.getRight().getTableNameNo() != null
							&& n.getRight().getTableNameNo().equalsIgnoreCase(t.getTableName())) {
						columns.put(n.getRight().getColumn().getColumnName(), n.getRight().getColumn());
						columns.put(t.getColumn(t.getColumns().size() - 1).getColumnName(),
								t.getColumn(t.getColumns().size() - 1));
					}
				}
			}
		}
		return columns;
	}

	/**
	 * TEMPCODE Rahul Sharma
	 * 
	 * @param joinTable
	 * @param t1
	 * @param t2
	 * @return
	 */
	private static HashMap<String, ArrayList<String>> createTempTableColumns(String joinTable, Table t1, Table t2) {
		// TODO Auto-generated method stub
		HashMap<String, ArrayList<String>> columnNameAndDataTypes = new HashMap<String, ArrayList<String>>();
		ArrayList<String> columns = new ArrayList<String>();
		ArrayList<String> columnDataTypes = new ArrayList<String>();
		ArrayList<String> primaryKey = new ArrayList<String>();
		int count = 0;
		String columnName;
		HashMap<String, Column> t1_columns = t1.getColumns();

		// for (Column c : t1.getPrimaryKey()) {
		// primaryKey.add(c);
		// }
		// for (Column c : t2.getPrimaryKey()) {
		// primaryKey.add(c);
		// }
		for (Column c : t1_columns.values()) {

			columnName = joinTable + "_" + t1.getTableName() + "__" + c.getColumnName() + count;
			columns.add(columnName);
			columnDataTypes.add(c.getCvcDatatype());
			if (t1.getPrimaryKey().contains(c)) {
				primaryKey.add(columnName);
			}
			count++;
		}

		HashMap<String, Column> t2_columns = t2.getColumns();
		for (Column c : t2_columns.values()) {
			columnName = joinTable + "_" + t2.getTableName() + "__" + c.getColumnName() + count;
			columns.add(columnName);
			columnDataTypes.add(c.getCvcDatatype());
			if (t2.getPrimaryKey().contains(c)) {
				primaryKey.add(columnName);
			}
			count++;
		}

		// Column cntCol = new Column(joinTable + "__XDATA_CNT_k" , joinTable);
		// cntCol.setCvcDatatype("Int");
		columns.add(joinTable + "__XDATA_CNT_k");
		columnDataTypes.add("Int");
		columnNameAndDataTypes.put("Names", columns);
		columnNameAndDataTypes.put("DataTypes", columnDataTypes);
		columnNameAndDataTypes.put("PrimaryKey", primaryKey);

		return columnNameAndDataTypes;
	}

	/**
	 * TEMPCODE Rahul Sharma
	 * 
	 * @param f1 : Table 1
	 * @param f2 : Table 2
	 * @return : constraints with all attributes present for quantifiers [forall /
	 *         exists]
	 */
	private static String generateConstraintsForAllAttributes(Table t1, Table t2, ArrayList<String> jtColumns,
			String jtName) {
		// TODO Auto-generated method stub
		String constraintString = "";
		int numberOfConstraints = jtColumns.size();
		String constraints[] = new String[numberOfConstraints];

		String t1_name = t1.getTableName().toLowerCase();
		String t2_name = t2.getTableName().toLowerCase();
		String jt_name = jtName.toLowerCase();

		// String t1_name = t1.getTableName().toUpperCase();
		// String t2_name = t2.getTableName().toUpperCase();
		// String jt_name = jtName.toUpperCase();

		int count = 0, count1 = 0;
		for (String key : t1.getColumns().keySet()) {
			constraints[count] = "(= (" + t1_name + "_" + key + count1 + " (select O_" + t1_name + " i1)) ("
					+ jtColumns.get(count) + " (select O_" + jt_name + " k1)))";
			constraintString += constraints[count];
			count++;
			count1++;
		}

		int count2 = 0;
		for (String key : t2.getColumns().keySet()) {
			constraints[count] = "(= (" + t2_name + "_" + key + count2 + " (select O_" + t2_name + " j1)) ("
					+ jtColumns.get(count) + " (select O_" + jt_name + " k1)))";
			constraintString += constraints[count];
			count++;
			count2++;
		}

		int index = 0;
		String finalConstraints = constraints[index++];
		while (index < numberOfConstraints) {
			finalConstraints = finalConstraints + "\n\t" + constraints[index++];
		}
		finalConstraints = "(and \n\t" + finalConstraints + "\n)";
		return finalConstraints;
	}

	/**
	 * TEMPCODE Rahul Sharma
	 * 
	 * @param f1 : Table 1
	 * @param f2 : Table 2
	 * @return : constraints with all attributes present for quantifiers [forall /
	 *         exists]
	 */
	private static String generateConstraintsForAllAndExistsAttributes(GenerateCVC1 cvc, Node n1, Node n2, Table t1,
			Table t2,
			ArrayList<String> jtColumns, String jtName, int tuplesInJoinTable, Vector<Node> selectionAndJoinConds) {
		// TODO Auto-generated method stub

		int tuplesInTable1 = 2;
		int tuplesInTable2 = 3;
		tuplesInJoinTable = 1;

		String t1_name = t1.getTableName().toLowerCase();
		String t2_name = t2.getTableName().toLowerCase();
		jtName = jtName.toLowerCase();
		String constraintString = "";
		String finalConstraints = "";
		Vector<Table> t = new Vector<>();
		t.add(t1);
		t.add(t2);
		for (int k = 1; k <= tuplesInJoinTable; k++) {
			String constr = "\t";
			// selection conditions, Skipping string selection conditions for now FIXME
			for (Node n : selectionAndJoinConds) {

				constr += generateConstraintsForSelectionConditions(cvc, n, jtColumns, jtName, t, Integer.toString(k),
						Integer.toString(k)) + "\n\t";
			}

			String constrTable1 = "";
			int count = 0;
			for (int i = 1; i <= tuplesInTable1; i++) {
				String temp = "\t\t";
				int count1 = 0;
				int countJT = count;
				for (String key : t1.getColumns().keySet()) {
					temp += "(= (" + t1_name + "_" + key + count1 + " (select O_" + t1_name + " " + i + ")) ("
							+ jtColumns.get(countJT) + " (select O_" + jtName + " " + k + ")))\n\t\t";
					countJT++;
					count1++;
				}
				temp = "\n\t  (and \n" + temp + "\n\t  )";
				constrTable1 += temp;
			}
			constrTable1 = "\t(or " + constrTable1 + "\n\t)";

			count = t1.getNoOfColumn();
			String constrTable2 = "";
			for (int j = 1; j <= tuplesInTable2; j++) {
				String temp = "\t\t";
				int count2 = 0;
				int countJT = count;
				for (String key : t2.getColumns().keySet()) {
					temp += "(= (" + t2_name + "_" + key + count2 + " (select O_" + t2_name + " " + j + ")) ("
							+ jtColumns.get(countJT) + " (select O_" + jtName + " " + k + ")))\n\t\t";
					countJT++;
					count2++;
				}
				temp = "\n\t  (and \n" + temp + "\n\t  )";
				constrTable2 += temp;
			}
			constrTable2 = "\t(or " + constrTable2 + "\n\t)";

			finalConstraints += "(and \n" + constr + "\n" + constrTable1 + "\n" + constrTable2 + "\n  )\n  ";
		}
		finalConstraints = "(assert \n (and \n  " + finalConstraints + "\n )\n)";

		return finalConstraints;
	}

	/********************** TESE CODE END ********************/
	// parismita
	// this assumes left is always colref do we need to extend this?
	public static String generateConstraintsForSelectionConditions(GenerateCVC1 cvc, Node n,
			ArrayList<String> jtColumns, String jtName,
			Vector<Table> tables, String tupleIdRight, String tupleIdLeft) {
		String constr = "";
		String left = "";
		String right = "";

		if (n.getLeft().getTableNameNo() != null && !n.getLeft().getTableNameNo().contains("JSQ")
				&& !n.getLeft().getTableNameNo().contains("GSQ") && !n.getLeft().getTableNameNo().contains("DSQ"))
			left = n.getLeft().getTableNameNo().replaceAll("\\d", "").toLowerCase();
		else if (n.getLeft().getTableNameNo() != null && (n.getLeft().getTableNameNo().contains("JSQ")
				|| n.getLeft().getTableNameNo().contains("GSQ") || n.getLeft().getTableNameNo().contains("DSQ"))) {
			left = n.getLeft().getTableNameNo();
		} else
			left = null;
		if (n.getNodeType() != null && !n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType())) {
			if (n.getRight().getTableNameNo() != null && !n.getRight().getTableNameNo().contains("JSQ")
					&& !n.getRight().getTableNameNo().contains("GSQ") && !n.getRight().getTableNameNo().contains("DSQ"))
				right = n.getRight().getTableNameNo().replaceAll("\\d", "").toLowerCase();

			else if (n.getRight().getTableNameNo() != null && (n.getRight().getTableNameNo().contains("JSQ")
					|| n.getRight().getTableNameNo().contains("GSQ")
					|| n.getRight().getTableNameNo().contains("DSQ"))) {
				right = (n.getRight().getTableNameNo());
			} else
				right = null;
		}

		int l_index = 0, r_index = 0, l_flag = -1, r_flag = -1;
		// left
		for (Table table : tables) {
			if (table == null)
				continue;
			if (table.getTableName().equals(left)) { // gives col index of t1
				for (int i = l_index; i < l_index + table.getNoOfColumn(); i++) {
					if (jtColumns.get(i).split("__")[1].replaceAll("\\d", "")
							.equalsIgnoreCase(n.getLeft().getColumn().getColumnName().replaceAll("\\d", ""))) {
						l_index = i;
						l_flag = 1;
						break;
					}
				}
				if (l_flag == 1)
					break;
			} else//
				l_index = l_index + table.getNoOfColumn();
		}

		// right
		if (n.getNodeType() != null && !n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType())) {

			for (Table table : tables) {
				if (table == null)
					continue;
				if (table.getTableName().equals(right)) // gives col index of t1
				{
					for (int i = r_index; i < r_index + table.getNoOfColumn(); i++) {
						if (jtColumns.get(i).split("__")[1].replaceAll("\\d", "")
								.equalsIgnoreCase(n.getRight().getColumn().getColumnName().replaceAll("\\d", ""))) {
							r_index = i;
							r_flag = 1;
							break;
						}
					}
					if (r_flag == 1)
						break;
				} else
					r_index = r_index + table.getNoOfColumn();
			}
		}
		ConstraintGenerator cg = new ConstraintGenerator();
		String constrL = "", constrR = "";
		String nonNullConstr = "";
		if (l_flag != -1 && r_flag != -1) {
			if (n.getNodeType() != null && !n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType())) {

				constr = "( " + jtColumns.get(l_index) + " (select O_" + jtName + " " + tupleIdLeft + " )) ";
				constrL = "( " + jtColumns.get(l_index) + " (select O_" + jtName + " " + tupleIdLeft + " )) ";
			}
			nonNullConstr += cg.getAssertNotNullConditionForSQTable(cvc, n.getLeft(), tupleIdLeft,
					cvc.getTableMap().getSQTableByName(jtName), l_index);

			if (n.getNodeType() != null && n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType()) && n.getOperator().equalsIgnoreCase("=")) {
				nonNullConstr = "(not " + nonNullConstr + ")";
			}
			if (n.getNodeType() != null && !n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType())) {

				constr += "( " + jtColumns.get(r_index) + " (select O_" + jtName + " " + tupleIdRight + " )) ";
				constrR = "( " + jtColumns.get(r_index) + " (select O_" + jtName + " " + tupleIdRight + " )) ";
				nonNullConstr += cg.getAssertNotNullConditionForSQTable(cvc, n.getRight(), tupleIdRight,
						cvc.getTableMap().getSQTableByName(jtName), r_index);
			}
			if(left.equals(right) && r_index == l_index && n.isNegated )
				return "";

		} else {
			if (n.getNodeType() != null && !n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType())) {

				constr = "(" + jtColumns.get(l_index) + " (select O_" + jtName + " " + tupleIdLeft + " )) ";
				if (n.getType() != null
						&& (n.getType().equalsIgnoreCase("IN") || n.getType().equalsIgnoreCase("NOT IN"))) {
					return constr;
				}
				if(n.getRight().getStrConst().contains("/")){
					String[] values = n.getRight().getStrConst().split("/");
					constr += "(/ " + values[0] + " " + values[1] + ")";
				}
				else
					constr += n.getRight().getStrConst();
				nonNullConstr += cg.getAssertNotNullConditionForSQTable(cvc, n.getLeft(), tupleIdLeft,
						cvc.getTableMap().getSQTableByName(jtName), l_index);
			} else {
				nonNullConstr += cg.getAssertNotNullConditionForSQTable(cvc, n.getLeft(), tupleIdLeft,
						cvc.getTableMap().getSQTableByName(jtName), l_index);
				if (n.getNodeType() != null && n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType()) && n.getOperator().equalsIgnoreCase("=")) {
					nonNullConstr = "(not " + nonNullConstr + ")";
				}
			}
		}

		String op = cg.getUserDefinedComparisionOperator(n.getOperator(), n.getLeft().getColumn(), constrL, constrR);

		String returnStr = "";
		if (op != "") {
			returnStr += ((n.isNegated) ? "(not " : " ") + op + ((n.isNegated) ? " )" : " ");
			// return ((n.isNegated) ? "(not " : " ") + op + ((n.isNegated) ? " )" : " ");
		} else if (n.getOperator().equals("/="))
			returnStr += ((n.isNegated) ? "(not " : " ") + "(not (= " + constr + ") )" + ((n.isNegated) ? " )" : " ");
		// return ((n.isNegated) ? "(not " : " ") + "(not (= " + constr + ") )" +
		// ((n.isNegated) ? " )" : " ");
		else
			returnStr += ((n.isNegated) ? "(not " : " ") + "(" + n.getOperator() + " " + constr + " )"
					+ ((n.isNegated) ? " )" : " ");

		// return ((n.isNegated) ? "(not " : " ") + "(" + n.getOperator() + " " + constr
		// + " )"
		// + ((n.isNegated) ? " )" : " ");
		if (n.getNodeType() != null && n.getNodeType().equalsIgnoreCase(n.getIsNullNodeType()))
			returnStr = "";
		
			
		if ((returnStr + nonNullConstr) != "")
			return "\n(and" + returnStr + nonNullConstr + ")\n";
		else
			return "";

	}

	/**
	 * @author parismita
	 * @param n
	 * @param jtColumns
	 * @param jtName
	 * @param tables
	 * @param tupleIdRight
	 * @param tupleIdLeft
	 * @return
	 */
	public static String generateConstraintsForStringConditions(GenerateCVC1 cvc, Node n, ArrayList<String> jtColumns,
			String jtName, Vector<Table> tables, String tupleIdRight, String tupleIdLeft) throws Exception {
		String constr = "";// string
		String left = n.getLeft().getTableNameNo() != null
				? (n.getLeft().getTableNameNo().replaceAll("\\d", "")).toLowerCase()
				: null;
		String right = n.getRight().getTableNameNo() != null
				? (n.getRight().getTableNameNo().replaceAll("\\d", "")).toLowerCase()
				: null;
		int l_index = 0, r_index = 0, l_flag = -1, r_flag = -1;
		// left
		for (Table table : tables) {
			if (table == null)
				continue;
			if (table.getTableName().equals(left)) { // gives col index of t1
				l_index += table.getColumnIndex(n.getLeft().getColumn().getColumnName());
				l_flag = 1;
				break;
			} else
				l_index += table.getNoOfColumn();
		}

		// right
		for (Table table : tables) {
			if (table == null)
				continue;
			if (table.getTableName().equals(right)) // gives col index of t1
			{
				r_index += table.getColumnIndex(n.getRight().getColumn().getColumnName());
				r_flag = 1;
				break;
			} else
				r_index = r_index + table.getNoOfColumn();
		}
		boolean stringFlag = false;
		if (l_flag != -1 && r_flag != -1) {
			constr = "(" + jtColumns.get(l_index) + " (select O_" + jtName + " " + tupleIdLeft + " )) ";
			constr += "(" + jtColumns.get(r_index) + " (select O_" + jtName + " " + tupleIdRight + " )) ";
		} else {
			constr = "(" + jtColumns.get(l_index) + " (select O_" + jtName + " " + tupleIdLeft + " )) ";
			// constr += n.getOperator().equalsIgnoreCase("/=")?"!=":n.getOperator();
			constr += n.getRight().getStrConst();
			stringFlag = true;
		}

		// if(n.getOperator().equals("/="))
		// return "(not (= "+constr + ") )";
		Vector<String> strConstr = new Vector<>();

		strConstr.add("(" + (n.getOperator().equalsIgnoreCase("/=") ? "( not (= " : n.getOperator()) + " " + constr
				+ " )" + (n.getOperator().equalsIgnoreCase("/=") ? ")" : ""));

		// Uncomment later: right now generate whatever string solver is solving
		// if(jtName.contains("JSQ") || jtName.contains("GSQ")){ // Sunanda for
		// conditions on subquery table backward pass
		// return "(" + n.getOperator() + " " + constr + " )" ;
		// }
		constr = "";
		Vector<String> solvedStringConstraint = cvc.getStringSolver().solveConstraints(strConstr,
				cvc.getResultsetColumns(), cvc.getTableMap(), true);
		for (String str : solvedStringConstraint) {
			str = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));
			constr += "\t\t" + str.replace("assert", "") + "\n";
		}
		return constr;

	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where each
	 * node is in different from clause sub queries
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForJoinsInDiffSubQueryBlocks(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Node n1, Node n2, String operator) throws Exception {
		String constraintString = "";

		int leftGroup = 1, rightGroup = 1;

		/** get number of groups for each node */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		rightGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);

		/** Get the details of each node */
		String t1 = getTableName(n1);
		String t2 = getTableName(n2);
		int pos1 = cvc.getTableMap().getTable(t1).getColumnIndex(getColumn(n1).getColumnName());
		int pos2 = cvc.getTableMap().getTable(t2).getColumnIndex(getColumn(n2).getColumnName());

		String r1 = getTableNameNo(n1);
		String r2 = getTableNameNo(n2);
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2 = 0;
		if (cvc.getNoOfTuples().containsKey(r1)) {
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if (cvc.getNoOfTuples().containsKey(r2)) {
			tuples2 = cvc.getNoOfTuples().get(r2);
		}
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();

		/** Do a round robin for the smaller value of the group number */
		for (int k = 1, l = 1;; k++, l++) {
			// constraintString += "ASSERT ("+
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
			// ((k-1)*tuples1+offset1))+ operator +
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
			// ((l-1)*tuples2+offset2))+");\n";

			/*
			 * ConstraintObject constrObj = new ConstraintObject();
			 * constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(queryBlock, n1,
			 * ((k-1)*tuples1+offset1)));
			 * constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(queryBlock,
			 * n2, ((l-1)*tuples2+offset2)));
			 * constrObj.setOperator(operator);
			 * constrObjList.add(constrObj);
			 */
			constraintString += constrGen.getAssertConstraint(
					constrGen.genPositiveCondsForPred(queryBlock, n1, ((k - 1) * tuples1 + offset1)), operator,
					constrGen.genPositiveCondsForPred(queryBlock, n2, ((l - 1) * tuples2 + offset2)));
			if (leftGroup > rightGroup) {
				if (l == rightGroup && k < leftGroup)
					l = 0;
				if (k >= leftGroup)
					break;
			} else if (leftGroup < rightGroup) {
				if (l < rightGroup && k == leftGroup)
					k = 0;
				if (l >= rightGroup)
					break;
			} else {/** if tuples1==tuples2 */
				if (l == leftGroup)
					break;
			}
		}
		// constraintString =constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	/**
	 * Gets constraints for nodes which are involved in join conditions where each
	 * node is in different from clause sub queries
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n1
	 * @param n2
	 * @param operator
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForJoinsInDiffSubQueryBlocks1(GenerateCVC1 cvc, QueryBlockDetails queryBlock,
			Node n1, Node n2, String operator) throws Exception {
		String constraintString = "";

		int leftGroup = 1, rightGroup = 1;

		/** get number of groups for each node */
		leftGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n1);

		rightGroup = UtilsRelatedToNode.getNoOfGroupsForThisNode(cvc, queryBlock, n2);

		/** Get the details of each node */
		String t1 = n1.getColumn().getTableName();
		String t2 = n2.getColumn().getTableName();
		// int pos1 =
		// cvc.getTableMap().getTable(t1).getColumnIndex(n1.getColumn().getColumnName());
		// int pos2 =
		// cvc.getTableMap().getTable(t2).getColumnIndex(n2.getColumn().getColumnName());

		int pos1 = cvc.getTableMap().getTable(t1.toUpperCase()).getColumnIndex(n1.getColumn().getColumnName()); // added
																												// by
																												// rambabu
		int pos2 = cvc.getTableMap().getTable(t2.toUpperCase()).getColumnIndex(n2.getColumn().getColumnName()); // added
																												// by
																												// rambabu

		String r1 = n1.getTableNameNo();
		String r2 = n2.getTableNameNo();
		int offset1 = cvc.getRepeatedRelNextTuplePos().get(r1)[1];
		int offset2 = cvc.getRepeatedRelNextTuplePos().get(r2)[1];

		/** Get number of tuples of each relation occurrence */
		int tuples1 = 0, tuples2 = 0;
		if (cvc.getNoOfTuples().containsKey(r1)) {
			tuples1 = cvc.getNoOfTuples().get(r1);
		}
		if (cvc.getNoOfTuples().containsKey(r2)) {
			tuples2 = cvc.getNoOfTuples().get(r2);
		}
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrObjList = new ArrayList<ConstraintObject>();
		/** Do a round robin for the smaller value of the group number */
		for (int k = 1, l = 1;; k++, l++) {
			// Populate constraint Object list and call AND function
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(
					constrGen.genPositiveCondsForPred(queryBlock, n1, ((k - 1) * tuples1 + offset1)));
			constrObj.setRightConstraint(
					constrGen.genPositiveCondsForPred(queryBlock, n2, ((l - 1) * tuples2 + offset2)));
			constrObj.setOperator(operator);
			constrObjList.add(constrObj);

			// constraintString += "("+
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n1,
			// ((k-1)*tuples1+offset1))+ operator +
			// GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, n2,
			// ((l-1)*tuples2+offset2))+") AND ";
			if (leftGroup > rightGroup) {
				if (l == rightGroup && k < leftGroup)
					l = 0;
				if (k >= leftGroup)
					break;
			} else if (leftGroup < rightGroup) {
				if (l < rightGroup && k == leftGroup)
					k = 0;
				if (l >= rightGroup)
					break;
			} else {/** if tuples1==tuples2 */
				if (l == leftGroup)
					break;
			}
		}
		constraintString = constrGen.generateANDConstraintsWithAssert(constrObjList);
		return constraintString;
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param nulled
	 * @param P0
	 * @return
	 * @throws Exception
	 */
	/** FIXME: What if there are multiple groups in this query block */
	public static String genNegativeConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node nulled, Node P0)
			throws Exception {
		String constraintString = new String();

		if (cvc.isFne()) {
			String tableName = nulled.getTable().getTableName();
			constraintString += "ASSERT NOT EXISTS (i: O_" + tableName + "_INDEX_INT): " +
					"(O_" + GenerateCVCConstraintForNode.cvcMap(nulled.getColumn(), "i") +
					" = O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ");";
		} else {
			/**
			 * Open up FORALL and NOT EXISTS
			 */
			/** Get table names */
			String nulledTableNameNo = nulled.getTableNameNo();
			String tablenameno = P0.getTableNameNo();

			int count1 = -1, count2 = -1;

			/** Get the number of tuples for the both nodes */
			count1 = UtilsRelatedToNode.getNoOfTuplesForThisNode(cvc, queryBlock, nulled);
			count2 = UtilsRelatedToNode.getNoOfTuplesForThisNode(cvc, queryBlock, P0);

			/** Get next position for these tuples */
			int offset1 = cvc.getRepeatedRelNextTuplePos().get(nulledTableNameNo)[1];
			int offset2 = cvc.getRepeatedRelNextTuplePos().get(tablenameno)[1];
			ConstraintGenerator constrGen = new ConstraintGenerator();
			ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();

			// constraintString += "ASSERT ";

			String tempconstr = "";
			for (int i = 1; i <= count1; i++) {
				for (int j = 1; j <= count2; j++) {
					String left = "", right = "";
					int i1, i2;
					if (nulled.getQueryType() == 1 && queryBlock.getFromClauseSubQueries() != null
							&& queryBlock.getFromClauseSubQueries().size() != 0) {
						left = ConstraintGenerator.getSolverMapping(nulled.getColumn(),
								(i - 1) * cvc.getNoOfTuples().get(nulled.getTableNameNo()) + offset1 + "");
						i1 = (i - 1) * cvc.getNoOfTuples().get(nulled.getTableNameNo()) + offset1;
					} else {
						left = ConstraintGenerator.getSolverMapping(nulled.getColumn(), i + offset1 - 1 + "");
						i1 = i + offset1 - 1;
					}

					if (P0.getQueryType() == 1 && queryBlock.getFromClauseSubQueries() != null
							&& queryBlock.getFromClauseSubQueries().size() != 0) {
						right = ConstraintGenerator.getSolverMapping(P0.getColumn(),
								(j - 1) * cvc.getNoOfTuples().get(P0.getTableNameNo()) + offset2 + "");
						i2 = (j - 1) * cvc.getNoOfTuples().get(P0.getTableNameNo()) + offset2;
					} else {
						right = ConstraintGenerator.getSolverMapping(P0.getColumn(), j + offset2 - 1 + "");
						i2 = j + offset2 - 1;
					}
					ConstraintObject conObj = new ConstraintObject();
					conObj.setLeftConstraint(left);
					conObj.setRightConstraint(right);
					conObj.setOperator("/=");
					constrList.add(conObj);

					Column cntCol1 = nulled.getTable().getColumn(nulled.getTable().getNoOfColumn() - 1); // added by
																											// sunanda
																											// for count
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(
							ConstraintGenerator.smtMap(cntCol1, ConstraintGenerator.ctx.mkInt(i1)).toString());
					constrObj.setOperator(">");
					constrObj.setRightConstraint(ConstraintGenerator.ctx.mkInt(0).toString());

					ConstraintObject constrObj1 = new ConstraintObject();

					Column cntCol2 = P0.getTable().getColumn(P0.getTable().getNoOfColumn() - 1); // added by sunanda for
																									// count
					constrObj1.setLeftConstraint(
							ConstraintGenerator.smtMap(cntCol2, ConstraintGenerator.ctx.mkInt(i2)).toString());
					constrObj1.setOperator(">");
					constrObj1.setRightConstraint(ConstraintGenerator.ctx.mkInt(0).toString());

					if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
						constrList.add(constrObj);
						constrList.add(constrObj1);
					}
					tempconstr += constrGen.generateSMTAndConstraints(constrList, null);
				}
			}
			constraintString = constrGen.getAssertConstraint(constrGen.generateSMTOrConstraints(null, tempconstr));
			// constraintString = constrGen.generateANDConstraintsWithAssert(constrList);//
			// constraintString.substring(0,
			// constraintString.length()-4);
			// constraintString += ";";
		}
		return constraintString;
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param nulled
	 * @param P0
	 * @return
	 */
	/** FIXME: What if there are multiple groups in this query block */
	public static String genNegativeConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Column nulled, Node P0) {
		String constraintString = new String();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		ConstraintGenerator constrGen = new ConstraintGenerator();

		if (cvc.isFne()) {
			constraintString += "ASSERT NOT EXISTS (i: O_" + nulled.getTableName() + "_INDEX_INT): " +
					"(O_" + GenerateCVCConstraintForNode.cvcMap(nulled, "i") + " = O_"
					+ GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ");";
		} else {

			/** Open up FORALL and NOT EXISTS */

			// constraintString += "ASSERT ";
			checkRepeatedRelations(cvc, cvc.cloneNoOfOutputTuples()); // TEMPCODE Rahul Sharma to handle repeated
																		// relations
			for (int i = 1; i <= cvc.getNoOfOutputTuples(nulled.getTableName()); i++) {/**
																						 * FIXME: Handle repeated
																						 * relations
																						 */
				// constraintString += "(O_" + GenerateCVCConstraintForNode.cvcMap(nulled, i +
				// "") + " /= O_" + GenerateCVCConstraintForNode.cvcMap(P0.getColumn(), P0) + ")
				// AND ";
				ConstraintObject constr = new ConstraintObject();
				constr.setLeftConstraint(ConstraintGenerator.getSolverMapping(nulled, i + ""));
				constr.setOperator("/=");
				constr.setRightConstraint(ConstraintGenerator.getSolverMapping(P0.getColumn(), P0));
				constrList.add(constr);
			}
			constraintString = constrGen.generateANDConstraintsWithAssert(constrList);// constraintString.substring(0,
																						// constraintString.length()-4);
			// constraintString += ";";
		}
		return constraintString;
	}

	/**
	 * TEMPCODE Rahul Sharma : to check if there is repeated relations, and remove
	 * them
	 * 
	 * @param cvc
	 * @param noOfOutputTuples
	 */
	private static void checkRepeatedRelations(GenerateCVC1 cvc, HashMap<String, Integer> noOfOutputTuples) {
		// TODO Auto-generated method stub
		HashMap<String, Integer> tempMap = new HashMap<>(noOfOutputTuples.size());
		for (Map.Entry<String, Integer> entry : noOfOutputTuples.entrySet()) {
			// tempMap.put(entry.getKey().toLowerCase(), entry.getValue());
			tempMap.put(entry.getKey().toUpperCase(), entry.getValue());
		}
		cvc.setNoOfOutputTuples(tempMap);
	}

	public static String genNegativeCondsEqClass(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1, Node c2,
			int tuple) {
		String constraintString = new String();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		ConstraintGenerator constrGen = new ConstraintGenerator();

		for (int i = 1; i <= cvc.getNoOfOutputTuples(c1.getTable().getTableName()); i++) {
			ConstraintObject constr = new ConstraintObject();
			constr.setLeftConstraint(ConstraintGenerator.getSolverMapping(c1.getColumn(), i + ""));
			constr.setOperator("/=");
			constr.setRightConstraint(ConstraintGenerator.getSolverMapping(c2.getColumn(), tuple + ""));
			constrList.add(constr);
		}
		// constraintString = constraintString.substring(0,
		// constraintString.length()-4);
		constraintString = constrGen.generateANDConstraintsWithAssert(constrList);
		return constraintString.trim();
	}

	public static String genNegativeCondsEqClassForTuplePair(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node c1,
			Node c2, int tupleIndex1, int tupleIndex2) {

		String constraintString = new String();
		ConstraintGenerator constrGen = new ConstraintGenerator();

		constraintString = constrGen.getAssertConstraint(cvc, c1.getColumn(), tupleIndex1, c2.getColumn(), tupleIndex2,
				"/=");

		/*
		 * constraintString += "ASSERT ";
		 * constraintString += "(O_" +
		 * GenerateCVCConstraintForNode.cvcMap(c1.getColumn(), tupleIndex1 + "") +
		 * " /= O_" + GenerateCVCConstraintForNode.cvcMap(c2.getColumn(), tupleIndex2 +
		 * "") + ") AND ";
		 * 
		 * constraintString = constraintString.substring(0,
		 * constraintString.length()-4);
		 * constraintString += ";";
		 */

		return constraintString;
	}

	public static ArrayList<ConstraintObject> genNegativeCondsEqClassForAllTuplePairs(GenerateCVC1 cvc,
			QueryBlockDetails queryBlock, Node c1, Node c2, int tupleIndex1, int tupleIndex2) {
		String constraintString = new String();
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();

		for (int i = 1; i <= tupleIndex1; i++) {
			for (int j = 1; j <= tupleIndex2; j++) {

				ConstraintObject constr = new ConstraintObject();
				constr.setLeftConstraint(ConstraintGenerator.getSolverMapping(c1.getColumn(), i + ""));
				constr.setOperator("/=");
				constr.setRightConstraint(ConstraintGenerator.getSolverMapping(c2.getColumn(), j + ""));
				constrList.add(constr);
			}
		}
		// constraintString = constrGen.generateANDConstraintsWithAssert(constrList);
		// return constraintString.trim();
		return constrList;
	}

	/**
	 * Generates positive constraints for the given set of nodes
	 * 
	 * @param ec
	 */
	public static String genPositiveConds(GenerateCVC1 cvc, Vector<Node> ec) {

		String constraintString = "";

		for (int i = 0; i < ec.size() - 1; i++) {
			Column col1 = ec.get(i).getColumn();
			Column col2 = ec.get(i + 1).getColumn();

			constraintString += ConstraintGenerator.getPositiveStatement(col1, ec.get(i), col2, ec.get(i + 1));
		}
		return constraintString;
	}

}
