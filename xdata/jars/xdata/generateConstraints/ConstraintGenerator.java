package generateConstraints;

import java.io.File;
//imports by deeksha
import java.io.FileWriter;
import java.io.IOException;
// import javax.swing.Group;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.ArraySort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Constructor;
import com.microsoft.z3.Context;
import com.microsoft.z3.DatatypeExpr;
import com.microsoft.z3.DatatypeSort;
import com.microsoft.z3.EnumSort;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.IntExpr;
import com.microsoft.z3.IntNum;
import com.microsoft.z3.Params;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.RealExpr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;

import GenConstraints.GenConstraints;
import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.MutationStructure;
import parsing.Node;
import parsing.Table;
import parsing.correlationStructure;
import stringSolver.StringConstraint;
import testDataGen.DataType;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.ConstraintObject;
import util.Utilities;

/**
 * This class generates the constraints based on solver in XData.Properties file
 * 
 * @author shree
 *
 */
// FIXME: Needs fine tuning - methods can be combined adding more parameters in
// constraint object.

public class ConstraintGenerator {
	private static Logger logger = Logger.getLogger(ConstraintGenerator.class.getName());
	private static boolean isCVC3 = false;
	private String constraintSolver;
	private static String solverSpecificCommentCharacter;
	private static boolean isTempJoin = false;
	private static boolean usingCnt = false;

	// deeksha comment : API call Context()
	// ----------------------------------------------------------------
	public static Context ctx = new Context();
	public static Solver solver = ctx.mkSolver();
	// --------------------------------------------------------------------
	// TODO: rename ctxSorts to something more meaningful; it has declarations other
	// than sorts
	public static HashMap<String, Sort> ctxSorts = new HashMap<String, Sort>(); // for storing Z3 context sorts; not
																				// able to extract directly from ctx.
	private static HashMap<String, FuncDecl> ctxFuncDecls = new HashMap<String, FuncDecl>(); // for storing Z3 context
																								// function declarations
	public static HashMap<String, Expr> ctxConsts = new HashMap<String, Expr>();

	private static IntExpr intNull = ctx.mkIntConst("intNullVal");
	private static RealExpr realNull = ctx.mkRealConst("realNullVal");
	public static String enumArrayIndex = "";
	public static String enumIndexVar = "";

	/**
	 * Constructor
	 */
	public ConstraintGenerator() {
		setConstraintSolver(Configuration.getProperty("smtsolver"));
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			enumArrayIndex = Configuration.enumArrayIndex;
			enumIndexVar = Configuration.enumIndexVar;
		}
		// enumArrayIndex = enumArrayIndex.replaceAll("^\"|\"$", "");
		// enumIndexVar = enumIndexVar.replaceAll("^\"|\"$", "");
		if (Configuration.getProperty("smtsolver").equalsIgnoreCase("cvc3")) {
			this.isCVC3 = true;
			solverSpecificCommentCharacter = "%";
		} else {
			this.isCVC3 = false;
			solverSpecificCommentCharacter = ";";
		}

		if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
			this.isTempJoin = true;
		} else {
			this.isTempJoin = false;
		}
	}

	/*
	 * Returns the Z3 context
	 */
	public Context getCtx() {
		return ctx;
	}
	public void clearContext(){
		ctxSorts = new HashMap<String, Sort>(); // for storing Z3 context sorts; not																	// able to extract directly from ctx.
		ctxFuncDecls = new HashMap<String, FuncDecl>(); // for storing Z3 context																					// function declarations
		ctxConsts = new HashMap<String, Expr>();
		// ctx = new Context();
		// solver = ctx.mkSolver();
	}

	/*
	 * Returns the Z3 solver corresponding to context
	 */
	public Solver getSolver() {
		return solver;
	}

	/*
	 * Returns the CtxFuncDecl structure
	 */
	public static HashMap<String, FuncDecl> getCtxFuncDecl() {
		return ctxFuncDecls;
	}

	/**
	 * This method takes in the col name, table name, offset and position for
	 * columns on which the constraint is to be generated.
	 * This also takes the operator that joins the constraints. Checks the
	 * ConstraintContext for CVC/SMT.
	 * If it is CVC, it returns CVC format constraint, otherwise return SMT format
	 * constraint.
	 * 
	 * @param cvc
	 * @param tableName1
	 * @param offset1
	 * @param pos1
	 * @param tableName2
	 * @param offset2
	 * @param pos2
	 * @param col1
	 * @param col2
	 * @param operator
	 * @return
	 */

	public ConstraintObject getConstraint(String tableName1, Integer offset1, Integer pos1, String tableName2,
			Integer offset2, Integer pos2,
			Column col1, Column col2, String operator) {

		ConstraintObject con = new ConstraintObject();
		if (isCVC3) {
			con.setLeftConstraint("O_" + tableName1 + "[" + offset1 + "]." + pos1);
			con.setRightConstraint("O_" + tableName2 + "[" + offset2 + "]." + pos2);
			con.setOperator(operator);
		} else {

			con.setLeftConstraint(
					tableName1 + "_" + col1.getColumnName() + pos1 + " (select O_" + tableName1 + " " + offset1 + ")");
			con.setRightConstraint(
					tableName2 + "_" + col2.getColumnName() + pos2 + " (select O_" + tableName2 + " " + offset2 + ")");
			con.setOperator(operator.toLowerCase());
		}
		return con;
	}

	/**
	 * This method returns an assert constraint statement for the passed in columns
	 * and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getAssertConstraint(GenerateCVC1 cvc, Column c1, Integer index1, Column c2, Integer index2,
			String operator) {

		String constraint = "";
		if (isCVC3) {
			constraint += "ASSERT (" + cvcMap(c1, index1 + "") + " " + operator + " " + cvcMap(c2, index2 + "")
					+ " );\n";
		} else {
			Expr smtIndex1 = (IntExpr) ctx.mkInt(index1);
			Expr smtIndex2 = (IntExpr) ctx.mkInt(index2);
			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
				smtIndex1 = ctx.mkConst(cvc.enumIndexVar + Integer.toString(index1), currentSort);
				smtIndex2 = ctx.mkConst(cvc.enumIndexVar + Integer.toString(index1), currentSort);

			}
			constraint += "(assert (" + (operator.trim().equals("/=") ? "not (= " : operator) + " "
					+ smtMap(c1, smtIndex1) + " " + smtMap(c2, smtIndex2) + (operator.trim().equals("/=") ? ")" : "")
					+ ")) \n";
		}
		return constraint;
	}

	/**
	 * This method returns an assert constraint statement for the passed in columns
	 * and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getAssertConstraint(String constraint) {

		String constr = "";
		if (isCVC3) {
			constr += "ASSERT (" + constraint + " );\n";
		} else {
			constr += "(assert " + constraint + ") \n";
		}
		return constr;
	}

	/**
	 * 
	 * @param constraint1
	 * @param operator
	 * @param constraint2
	 * @return
	 */
	public String getAssertConstraint(String constraint1, String operator, String constraint2) {
		String constraint = "";

		if (isCVC3) {
			constraint += "ASSERT (" + constraint1 + " " + operator + " " + constraint2 + " );\n";
			if (constraint.length() <= 9) {
				return "";
			}
		} else {

			constraint += "(assert (" + (operator.equals("/=") ? "not (= " : operator) + "  " + constraint1 + "  "
					+ constraint2 + (operator.equals("/=") ? " )" : " ") + " )) \n";
			if (constraint.length() < 11) {
				return "";
			}
		}

		return constraint;
	}

	/**
	 * This method gets the Assert Constraint with MAX function call.
	 * 
	 * @param constraint1
	 * @param operator
	 * @param constraint2
	 * @return
	 */
	public String getMaxAssertConstraintForSubQ(String constraint1, String operator, String constraint2) {
		String constraint = "";

		if (isCVC3) {
			// returnStr+= "ASSERT (MAX_"+columnName+n.getOperator()+"+O_"+
			// ConstraintGenerator.cvcMap(n.getRight().getColumn(), outerTupleNo+"")+");\n";
			constraint += "ASSERT (MAX_" + constraint1 + " " + operator + " " + constraint2 + " );\n";
		} else {

			constraint += "(assert (" + operator + " (getMAX_" + constraint1 + " " + constraint2 + ") true)) \n";
		}
		return constraint;
	}

	/**
	 * This method gets the Assert Constraint with MAX function call.
	 * 
	 * @param constraint1
	 * @param operator
	 * @param constraint2
	 * @return
	 */
	public String getMinAssertConstraintForSubQ(String constraint1, String operator, String constraint2) {
		String constraint = "";

		if (isCVC3) {
			// returnStr+= "ASSERT (MAX_"+columnName+n.getOperator()+"+O_"+
			// ConstraintGenerator.cvcMap(n.getRight().getColumn(), outerTupleNo+"")+");\n";
			constraint += "ASSERT (MIN_" + constraint1 + " " + operator + " " + constraint2 + " );\n";
		} else {

			constraint += "(assert (" + operator + " (getMIN_" + constraint1 + " " + constraint2 + ") true)) \n";
		}
		return constraint;
	}

	/**
	 * This method returns an assert constraint statement for the passed in columns
	 * and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getAssertConstraint(String tableName1, Column col1, Integer index1, Integer pos1, String tableName2,
			Column col2, Integer index2, Integer pos2, String operator) {

		String constraint = "";
		if (isCVC3) {
			constraint += "ASSERT (" + "O_" + tableName1 + "[" + index1 + "]." + pos1 + " " + operator + " " + "O_"
					+ tableName2 + "[" + index2 + "]." + pos2 + " );\n";
		} else {

			constraint += "(assert (" + (operator.equals("/=") ? "not (= " : operator) +
					" (" + tableName1 + "_" + col1.getColumnName() + pos1 + " (select O_" + tableName1 + " " + index1
					+ "))" + " ("
					+ tableName2 + "_" + col2.getColumnName() + pos2 + " (select O_" + tableName2 + " " + index2 + "))"
					+ (operator.equals("/=") ? ")" : "") + ")) \n";
		}
		return constraint;
	}

	/**
	 * This method returns an DISTINCT constraint statement for the passed in
	 * columns and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getDistinctConstraint(String tableName1, Column col1, Integer index1, Integer pos1, String tableName2,
			Column col2, Integer index2, Integer pos2) {

		String constraint = "";
		if (isCVC3) {
			constraint += "DISTINCT (O_" + tableName1 + "[" + index1 + "]." + pos1 + ",  O_" + tableName2 + "[" + index2
					+ "]." + pos2 + ");\n";
		} else {

			constraint += "not (= (" + tableName1 + "_" + col1.getColumnName() + pos1 + " (select O_" + tableName1 + " "
					+ index1 + ")" + ") ("
					+ tableName2 + "_" + col2.getColumnName() + pos2 + " (select O_" + tableName2 + " " + index2 + ")"
					+ ")) \n";
		}
		return constraint;
	}

	/**
	 * This method returns an DISTINCT constraint statement for the passed in
	 * columns and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getDistinctConstraints(String tableName1, Column col1, Integer totalno, Integer pos1) {

		String constraint = "DISTINCT (";
		if (isCVC3) {
			for (int index1 = 1; index1 <= totalno; index1++) {
				if (index1 != 1)
					constraint += ", ";
				constraint += "O_" + tableName1 + "[" + index1 + "]." + pos1;
			}
			constraint += ")";
		}

		return constraint;
	}

	/**
	 * This method returns an DISTINCT constraint statement for the passed in
	 * columns and tupleIndices based on the solver.
	 * 
	 * @param cvc
	 * @param c1
	 * @param index1
	 * @param c2
	 * @param index2
	 * @param operator
	 * @return
	 */
	public String getDistinctConstraint(Node agg, Integer index) {

		String constraint = "";
		if (isCVC3) {
			constraint += "DISTINCT( " + ConstraintGenerator.cvcMapNode(agg, (index) + "") + " , " +
					ConstraintGenerator.cvcMapNode(agg, (index) + "") + ")";
		}

		else {
			constraint += "(not (= " + smtMapNode(agg, index + "") + " " + smtMapNode(agg, index + "") + "))\n";
		}
		return constraint;
	}

	/**
	 * This method returns an assert constraint statement for the passed in columns
	 * and tuple Indices based on the solver.
	 * 
	 * @param tableName1
	 * @param col1
	 * @param index1
	 * @param pos1
	 * @param tableName2
	 * @param col2
	 * @param index2
	 * @param pos2
	 * @return
	 */
	public String getAssertDistinctConstraint(String tableName1, Column col1, Integer index1, Integer pos1,
			String tableName2, Column col2, Integer index2, Integer pos2) {

		String constraint = "";
		if (isCVC3) {
			constraint += "ASSERT DISTINCT (O_" + tableName1 + "[" + index1 + "]." + pos1 + ",  O_" + tableName2 + "["
					+ index2 + "]." + pos2 + ");\n";
		} else {
			constraint += "(assert (distinct (" + tableName1 + "_" + col1.getColumnName() + pos1 + " (select O_"
					+ tableName1 + " " + index1 + ")" + ") ("
					+ tableName2 + "_" + col2.getColumnName() + pos2 + " (select O_" + tableName2 + " " + index2 + ")"
					+ "))) \n";
		}
		return constraint;
	}

	/**
	 * This method returns ASSERT TRUE constraint based on the solver
	 * 
	 * @return
	 */
	public String getAssertTrue() {
		String constraint = "";
		if (isCVC3) {
			constraint += "ASSERT TRUE;\n";
		} else {
			constraint += "(assert true) \n";
		}
		return constraint;
	}

	public String getNegatedConstraint(String constraint) {
		String negConstraint = constraint;
		String returnStr = "";
		if (isCVC3) {
			negConstraint = negConstraint.replaceFirst("ASSERT ", "ASSERT NOT(");
			negConstraint = negConstraint.replace(";", ");");
		} else {
			if (!constraint.equalsIgnoreCase("")) {
				String temp = "";
				// negConstraint = negConstraint.replace("(assert ","(assert (not " );
				// negConstraint = negConstraint.replace(") ",")) " );
				if (negConstraint.contains("assert")) {
					for (int i = 1; i < negConstraint.split("assert").length; i++) {
						temp = negConstraint.split("assert")[i].trim();
						temp = temp.substring(0, temp.lastIndexOf(")")).trim();

						if (temp.contains("CHECKALL_NULL"))
							returnStr += "(assert " + temp + ")\n";
						else
							returnStr += "(assert \n\t(not " + temp + "))\n";
					}
				}
				// negConstraint += ") \n";
			}

		}

		return returnStr;
	}

	/**
	 * Generates positive CVC3 constraint for given nodes and columns
	 * 
	 * @param col1
	 * @param n1
	 * @param col2
	 * @param n2
	 * @return
	 */
	public static String getPositiveStatement(Column col1, Node n1, Column col2, Node n2) {

		if (isCVC3) {
			return "ASSERT " + cvcMap(col1, n1) + " = " + cvcMap(col2, n2) + ";\n";
		} else {
			return "(assert (=" + " " + smtMap(col1, n1) + " " + smtMap(col2, n2) + "))\n";
		}
	}

	/**
	 * This method returns a String with ISNULL constraint
	 * 
	 * @param cvc
	 * @param col
	 * @param offSet
	 * @return
	 */
	public String getIsNullCondition(String tableName, Column col, String offSet) {

		String isNullConstraint = "";

		if (col.getCvcDatatype().equals("INT") || col.getCvcDatatype().equals("REAL")
				|| col.getCvcDatatype().equals("DATE")
				|| col.getCvcDatatype().equals("TIME") || col.getCvcDatatype().equals("TIMESTAMP")) {
			isNullConstraint = "ISNULL_" + col.getColumnName() + "(" + cvcMap(col, offSet + "") + ")";
		} else {
			isNullConstraint = "ISNULL_" + col.getCvcDatatype() + "(" + cvcMap(col, offSet + "") + ")";
		}
		return isNullConstraint;
	}

	/**
	 * This method returns an Expr with ISNULL constraint (for Z3)
	 * 
	 * @param cvc
	 * @param col
	 * @param offSet
	 * @return
	 */
	public BoolExpr getIsNullConditionZ3(GenerateCVC1 cvc, String tableName, Column col, String offSet) {
		String suffix;
		// isnull function required add count > 0 in above function sunanda

		if (col.getCvcDatatype().equalsIgnoreCase("INT") || col.getCvcDatatype().equalsIgnoreCase("REAL")
				|| col.getCvcDatatype().equalsIgnoreCase("DATE")
				|| col.getCvcDatatype().equalsIgnoreCase("TIME")
				|| col.getCvcDatatype().equalsIgnoreCase("TIMESTAMP")) {
			suffix = col.getColumnName();
		} else {
			suffix = col.getCvcDatatype();
		}

		FuncDecl isNullDecl = ctxFuncDecls.get("ISNULL_" + suffix);
		BoolExpr isNullApply = (BoolExpr) isNullDecl.apply(smtMap(col, (IntExpr) ctx.mkInt(offSet)));
		// Column cntCol =
		// cvc.getTableMap().getTable(tableName.toUpperCase()).getColumn(cvc.getTableMap().getTable(tableName.toUpperCase()).getNoOfColumn()-1);
		// BoolExpr andWithCount = ctx.mkAnd(ctx.mkGt((ArithExpr)smtMap(cntCol,
		// (IntExpr) ctx.mkInt(offSet)), ctx.mkInt(0)), isNullApply);
		return isNullApply;
	}

	/**
	 * This method returns the constraint String that holds MAX constraint for
	 * SubQuery Aggregate condition.
	 * 
	 * @param aggNode
	 * @param index
	 * @param columnName
	 * @param myCount
	 * @return
	 */
	public static String getMaxConstraintForSubQ(Node aggNode, Integer index, Column column, Integer myCount) {
		String returnStr = "";
		String maxStr = "";

		if (isCVC3) {

			maxStr = "MAX_" + column.getColumnName() + ": " + column.getColumnName() + ";\n ASSERT(";
			for (int i = 1; i <= myCount; i++) {
				maxStr += "(" + ConstraintGenerator.cvcMapNode(aggNode, index + "") + "=MAX_" + column.getColumnName()
						+ ") OR";
				returnStr += "ASSERT (" + ConstraintGenerator.cvcMapNode(aggNode, index + "") + "<=MAX_"
						+ column.getColumnName() + ");\n";
			}
			maxStr = maxStr.substring(0, maxStr.length() - 3) + ");\n";
			return maxStr + returnStr;

		} else {

			maxStr = "(define-sort MAX_" + column.getColumnName() + " " + column.getCvcDatatype() + ")\n";
			for (int i = 1; i <= myCount; i++) {
				maxStr += "(define-fun getMAX" + column.getColumnName() + " ((MAX_" + column.getColumnName() + " "
						+ column.getCvcDatatype() + ")) Bool \n\t\t\t";
				maxStr += "(or (= " + ConstraintGenerator.cvcMapNode(aggNode, index + "") + " (MAX_"
						+ column.getColumnName() + "))\n\t\t\t  "
						+ "(<= " + ConstraintGenerator.cvcMapNode(aggNode, index + "") + " " + " (MAX_"
						+ column.getColumnName() + ")))\n";
				maxStr += ")\n";

				returnStr += "(assert (= (getMAX" + column.getColumnName() + " "
						+ ConstraintGenerator.cvcMapNode(aggNode, index + "") + ") true))\n";
			}
			return maxStr + returnStr;
		}
	}

	/**
	 * This method returns the constraint String that holds MIN constraint for
	 * SubQuery Aggregate condition.
	 * 
	 * @param aggNode
	 * @param index
	 * @param columnName
	 * @param myCount
	 * @return
	 */
	public static String getMinConstraintForSubQ(Node aggNode, Integer index, Column column, Integer myCount) {
		String returnStr = "";
		String maxStr = "";

		if (isCVC3) {

			maxStr = "MIN_" + column.getColumnName() + ": " + column.getColumnName() + ";\n ASSERT(";
			for (int i = 1; i <= myCount; i++) {
				maxStr += "(" + ConstraintGenerator.cvcMapNode(aggNode, index + "") + "=MIN_" + column.getColumnName()
						+ ") OR";
				returnStr += "ASSERT (" + ConstraintGenerator.cvcMapNode(aggNode, index + "") + ">=MIN_"
						+ column.getColumnName() + ");\n";
			}
			maxStr = maxStr.substring(0, maxStr.length() - 3) + ");\n";
			return maxStr + returnStr;

		} else {

			maxStr = "(define-sort MIN_" + column.getColumnName() + " " + column.getCvcDatatype() + ")\n";
			for (int i = 1; i <= myCount; i++) {
				maxStr += "(define-fun getMIN" + column.getColumnName() + " ((MIN_" + column.getColumnName() + " "
						+ column.getCvcDatatype() + ")) Bool \n\t\t\t";
				maxStr += "(or (= " + ConstraintGenerator.cvcMapNode(aggNode, index + "") + " (MIN_"
						+ column.getColumnName() + "))\n\t\t\t  "
						+ "(>= " + ConstraintGenerator.cvcMapNode(aggNode, index + "") + " " + " (MIN_"
						+ column.getColumnName() + ")))\n";
				maxStr += ")\n";

				returnStr += "(assert (= (getMIN" + column.getColumnName() + " "
						+ ConstraintGenerator.cvcMapNode(aggNode, index + "") + ") true))\n";
			}
			return maxStr + returnStr;
		}
	}

	/**
	 * This method defines the solver function for getting MAX value constraint in
	 * CVC/SMT format.
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public String getMaxConstraint(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, Boolean isLeftConstraint) {
		String returnStr = "";
		if (isLeftConstraint) {
			if (isCVC3) {
				returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator()
						+ n.getRight().toCVCString(10, queryBlock.getParamMap());
				if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
					returnStr += " AND x > 0 );";
				} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
					returnStr += " AND x < 10000000 );";
				} else {// operator is = or /=
					returnStr += ");";
				}
			} else {
				String dataType = n.getLeft().getColumn().getCvcDatatype();
				/*
				 * if(dataType != null){
				 * returnStr = "(define-fun MAX_"+n.getLeft().getColumn()+" ((x "+dataType+")) "
				 * +dataType+" ("+n.getOperator()+" x "+n.getRight().toCVCString(10,
				 * queryBlock.getParamMap())+") (";
				 * if(n.getOperator().equalsIgnoreCase("<") ||
				 * n.getOperator().equalsIgnoreCase("<=")){
				 * returnStr += "> x  0)";
				 * }
				 * else if(n.getOperator().equalsIgnoreCase(">") ||
				 * n.getOperator().equalsIgnoreCase(">=")){
				 * returnStr += " < x 10000000 )";
				 * }else{
				 * returnStr += ")";
				 * }
				 * 
				 * returnStr += ")";
				 * }
				 */

				if (dataType != null) {
					// FuncDecl r = cvc.ctxFuncDecls.get("MAX_"+n.getLeft().getColumn());
					// if(cvc.ctxFuncDecls.get("MAX_"+n.getLeft().getColumn()) == null)
					// {
					//
					// FuncDecl s = ctx.mkConstDecl("MAX_"+n.getLeft().getColumn(),
					// getColumnSort(dataType));
					//
					// if(!isTupleCalculation)
					// cvc.ctxFuncDecls.put("MAX_"+n.getLeft().getColumn(), s);

					returnStr = "(declare-const MAX_" + n.getLeft().getColumn() + " " + dataType + ")\n";
					returnStr += "(assert (" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " MAX_"
							+ n.getLeft().getColumn() + " " + n.getRight().toCVCString(10, queryBlock.getParamMap())
							+ ")) " + (n.getOperator().equals("/=") ? ")\n " : "\n");

					if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
						returnStr += "(assert (> MAX_" + n.getLeft().getColumn() + " 0))\n";
					} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
						returnStr += "(assert (< MAX_" + n.getLeft().getColumn() + " 10000000))\n";
					} else {
						returnStr += "\n"; // changes made by sunanda removed ")"
						// }
					}

				}
			}
		} else {
			if (isCVC3) {
				returnStr = "MAX: TYPE = SUBTYPE(LAMBDA(x: INT): "
						+ n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
				if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
					returnStr += " AND x < 10000000 );";
				} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
					returnStr += " AND x > 0 );";
				} else {// operator is = or /=
					returnStr += ";";
				}
			} else {
				String dataType = n.getRight().getColumn().getCvcDatatype();
				if (dataType != null) {
					// FuncDecl r = ctxFuncDecls.get("MAX_"+n.getLeft().getColumn());
					// if(ctxFuncDecls.get("MAX_"+n.getLeft().getColumn()) == null)
					// {
					//
					// FuncDecl s = ctx.mkConstDecl("MAX_"+n.getLeft().getColumn(),
					// getColumnSort(dataType));
					//
					// if(!isTupleCalculation)
					// ctxFuncDecls.put("MAX_"+n.getLeft().getColumn(), s);

					returnStr = "(declare-const MAX_" + n.getRight().getColumn() + " " + dataType + ")\n";
					returnStr = "(assert (" + n.getOperator() + " "
							+ n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " MAX_" + n.getRight().getColumn()
							+ "))\n ";
					// returnStr = "(define-fun MAX_"+n.getRight().getColumn()+" ((x "+dataType+"))
					// "+dataType+" ("+n.getOperator()+" "+n.getLeft().toCVCString(10,
					// queryBlock.getParamMap())+ "x) (";

					if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
						returnStr += "< x 10000000 )";
					} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
						returnStr += " > x  0)";
					} else {
						returnStr += "\n"; // changes made by sunanda removed ")"
					}
					// }

					returnStr += ")";
				}
			}

		}
		return returnStr;
	}

	/**
	 * This method defines the solver function for getting MAX value constraint in
	 * CVC/SMT format.
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public String getMinConstraint(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n, Boolean isLeftConstraint) {
		String returnStr = "";

		if (isLeftConstraint) {
			if (isCVC3) {
				returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): " + " x " + n.getOperator()
						+ n.getRight().toCVCString(10, queryBlock.getParamMap());
				if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
					returnStr += " AND x > 0 );";
				} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
					returnStr += " AND x < 10000000 );";
				} else {// operator is = or /=
					returnStr += ");";
				}
			} else {
				String dataType = n.getLeft().getColumn().getCvcDatatype();

				if (dataType != null) {

					// FuncDecl r = cvc.ctxFuncDecls.get("MIN_"+n.getLeft().getColumn());
					// if(cvc.ctxFuncDecls.get("MIN_"+n.getLeft().getColumn()) == null)
					// {
					//
					// FuncDecl s = ctx.mkConstDecl("MIN_"+n.getLeft().getColumn(),
					// getColumnSort(dataType));
					//
					// if(!isTupleCalculation)
					// cvc.ctxFuncDecls.put("MIN_"+n.getLeft().getColumn(), s);

					returnStr = "(declare-const MIN_" + n.getLeft().getColumn() + " " + dataType + ")\n";

					returnStr += "(assert (" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " MIN_"
							+ n.getLeft().getColumn() + " " + n.getRight().toCVCString(10, queryBlock.getParamMap())
							+ ")) " + (n.getOperator().equals("/=") ? ")\n " : "\n");
					if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
						returnStr += "(assert (> MIN_" + n.getLeft().getColumn() + " 0))\n";
					} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
						returnStr += "(assert (< MIN_" + n.getLeft().getColumn() + " 10000000))\n";
					} else {
						returnStr += "\n"; // changes made by sunanda removed ")"
					}
					// }

				}
			}
		} else {
			if (isCVC3) {
				returnStr = "MIN: TYPE = SUBTYPE(LAMBDA(x: INT): "
						+ n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " " + n.getOperator() + " x ";
				if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
					returnStr += " AND x < 10000000 );";
				} else if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
					returnStr += " AND x > 0 );";
				} else {// operator is = or /=
					returnStr += ");";
				}
			} else {

				String dataType = n.getRight().getColumn().getCvcDatatype();
				if (dataType != null) {
					// FuncDecl r = ctxFuncDecls.get("MIN_"+n.getLeft().getColumn());
					// if(ctxFuncDecls.get("MIN_"+n.getLeft().getColumn()) == null)
					// {
					//
					// FuncDecl s = ctx.mkConstDecl("MIN_"+n.getLeft().getColumn(),
					// getColumnSort(dataType));
					//
					// if(!isTupleCalculation)
					// ctxFuncDecls.put("MIN_"+n.getLeft().getColumn(), s);
					//
					returnStr = "(declare-const MIN_" + n.getRight().getColumn() + " " + dataType + ")\n";
					returnStr += "(assert (" + n.getOperator() + " "
							+ n.getLeft().toCVCString(10, queryBlock.getParamMap()) + " MIN_" + n.getRight().getColumn()
							+ "))\n ";

					if (n.getOperator().equalsIgnoreCase(">") || n.getOperator().equalsIgnoreCase(">=")) {
						returnStr += "(assert (> MIN_" + n.getRight().getColumn() + " 0))\n";
					} else if (n.getOperator().equalsIgnoreCase("<") || n.getOperator().equalsIgnoreCase("<=")) {
						returnStr += "(assert (< MIN_" + n.getRight().getColumn() + " 10000000))\n";
					} else {
						returnStr += "\n"; // changes made by sunanda removed ")"
					}
					// }
				}
			}

		}
		return returnStr;
	}

	/**
	 * This method is used to get the SMT / CVC constraints for aggregation function
	 * : AVG
	 * 
	 * @param cvc
	 * @param myCount
	 * @param groupNumber
	 * @param multiples
	 * @param totalRows
	 * @param agg
	 * @param offset
	 * @return
	 */
	public String getAVGConstraint(Integer myCount, Integer groupNumber, Integer multiples, Integer totalRows, Node agg,
			Integer offset) {

		String returnStr = "";
		int extras = totalRows % myCount;
		if (isCVC3) {

			returnStr += "\n ASSERT ";
			for (int i = 1, j = 0; i <= myCount; i++, j++) {

				int tuplePos = (groupNumber) * myCount + i;

				if (j < extras)
					returnStr += (multiples + 1) + "*(" + getMapNode(agg, (tuplePos + offset - 1) + "") + ")";
				else
					returnStr += (multiples) + "*(" + getMapNode(agg, (tuplePos + offset - 1) + "") + ")";

				if (i < myCount) {
					returnStr += "+";
				}
			}
			return returnStr + ") / " + totalRows + "; \n";
		} else {
			// Check if it is correct -
			// returnStr += " (/ (";
			String str = "";

			for (int i = 1, j = 0; i <= myCount; i++, j++) {

				String str1 = str;
				int tuplePos = (groupNumber) * myCount + i;

				if (i <= myCount && str1 != null && !str1.isEmpty()) {
					returnStr += "(+ " + str + " ";
				}
				String function = "SUM_REPLACE_NULL_" + agg.getColumn().getCvcDatatype();

				if (j < extras)
					str = "\n \t (* " + (multiples + 1) + " (" + function
							+ getMapNode(agg, (tuplePos + offset - 1) + "") + "))";
				else
					str = "\n \t (* " + (multiples) + " (" + function + getMapNode(agg, (tuplePos + offset - 1) + "")
							+ "))";

				if (i <= myCount && str1 != null && !str1.isEmpty()) {
					returnStr += str + ") ";
				}
				if (returnStr != null && !returnStr.isEmpty()) {
					str = returnStr;
					if (i != myCount) {
						returnStr = "";
					}
				}

				if (myCount == 1 && i == myCount) {
					returnStr = str;
				}

			}
			return "(/  \n " + returnStr + " \n" + myCount + ")";

			/*
			 * for(int i=1,j=0;i<=myCount;i++,j++){
			 * 
			 * int tuplePos=(groupNumber)*myCount+i;
			 * 
			 * if(i<=myCount && str != null && !str.isEmpty()){
			 * returnStr += "(+ "+str+" (";
			 * }
			 * 
			 * if(j<extras)
			 * str += "* "+(multiples+1)+" "+ getMapNode(agg, (tuplePos+offset-1)+"")+"";
			 * else
			 * str += "* "+(multiples)+" "+ getMapNode(agg, (tuplePos+offset-1)+"")+"";
			 * 
			 * if(i<myCount){
			 * //returnStr += ")) ";
			 * }
			 * if(i==1 && j==0){
			 * returnStr += str;
			 * }
			 * }
			 * return returnStr + ") "+totalRows + ") \n";
			 * 
			 */
		}
	}

	/**
	 * This method is used to get the SMT / CVC constraints for aggregation function
	 * : AVG
	 * 
	 * @param cvc
	 * @param myCount
	 * @param groupNumber
	 * @param multiples
	 * @param totalRows
	 * @param agg
	 * @param offset
	 * @return
	 */
	public String getAVGConstraintForSubQ(Integer myCount, Integer groupNumber, Integer multiples, Integer totalRows,
			Node agg, Integer offset) {

		String returnStr = "";
		int extras = totalRows % myCount;
		int groupOffset = groupNumber * myCount;

		if (isCVC3) {

			returnStr += "\n ASSERT ";
			for (int i = 1, j = 0; i <= myCount; i++, j++) {
				if (j < extras)
					returnStr += (multiples + 1) + "*(" + getMapNode(agg, (groupOffset + offset + i) + "") + ")";
				else
					returnStr += (multiples) + "*(" + getMapNode(agg, (groupOffset + offset + i) + "") + ")";

				if (i < myCount) {
					returnStr += "+";
				}
			}
			return returnStr + ") / " + totalRows + "; \n";
		} else {
			// Check if it is correct -
			returnStr += "(assert (/ (";
			for (int i = 1, j = 0; i <= myCount; i++, j++) {
				String str = "";

				if (i < myCount && str != null && !str.isEmpty()) {
					returnStr += "(+ " + str + " (";
				}

				if (j < extras)
					str += "* " + (multiples + 1) + " " + getMapNode(agg, (groupOffset + offset + i) + "") + "";
				else
					str += "* " + (multiples) + " " + getMapNode(agg, (groupOffset + offset + i) + "") + "";

				if (i < myCount) {
					returnStr += ")) ";
				}
				if (i == 1 && j == 0) {
					returnStr += str;
				}
			}
			return returnStr + ") " + totalRows + ")) \n";
		}
	}

	/**
	 * This method is used to get the SMT / CVC constraints for aggregation function
	 * : MIN
	 * 
	 * @param innerTableNo
	 * @param groupNumber
	 * @param totalRows
	 * @param cvc
	 * @param af
	 * @return
	 */
	public String getMinAssertConstraint(String innerTableNo, Integer groupNumber, int totalRows, GenerateCVC1 cvc,
			AggregateFunction af) {

		String returnStr = "";

		int myCount = cvc.getNoOfTuples().get(innerTableNo);
		int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];

		if (isCVC3) {

			returnStr += "\nASSERT EXISTS(i: MIN): (";

			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos + offset - 1 + "")
						+ " >= " + "i ";
				if (i < totalRows) {
					returnStr += " AND ";
				}
			}
			returnStr += ") AND (";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos + offset - 1 + "") + " = "
						+ "i ";
				if (i < totalRows) {
					returnStr += " OR ";
				}
			}
			returnStr += ");";
		} else {

			String tableName = af.getAggExp().getColumn().getTable().getTableName();
			String columnName = af.getAggExp().getColumn().getColumnName();
			int index = af.getAggExp().getColumn().getTable().getColumnIndex(columnName);

			myCount = cvc.getNoOfTuples().get(innerTableNo);
			offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
			String function = "MIN_REPLACE_NULL_" + af.getAggExp().getColumn().getCvcDatatype();
			String Nullfunction = "CHECKALL_NULL" + af.getAggExp().getColumn().getCvcDatatype(); // changes made by
																									// sunanda ("_"
																									// removed)

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(and";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				returnStr += "\n ( >= MIN_" + columnName + " (" + function + "(" + tableName + "_" + columnName + index
						+ "(select O_" + tableName + " " + row + ")) ))";
			}
			if (totalRows > 1)
				returnStr += ")";
			returnStr += ")\n";

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(or";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				returnStr += "\n ( = MIN_" + columnName + " (" + function + "(" + tableName + "_" + columnName + index
						+ "(select O_" + tableName + " " + row + ") )))";
			}
			if (totalRows > 1)
				returnStr += ")";
			returnStr += ")\n";

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(and";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				if (totalRows > 1)
					returnStr += "\n (not (" + Nullfunction + "(" + tableName + "_" + columnName + index + " (select O_"
							+ tableName + " " + row + ")))) "; // changes made by sunanda "not" added
				else
					returnStr += "\n (not (" + Nullfunction + "(" + tableName + "_" + columnName + index + " (select O_"
							+ tableName + " " + row + "))) "; // changes made by sunanda "not" added
			}
			if (totalRows > 1)
				returnStr += "\n";
			returnStr += "))";
		}

		return returnStr;

	}

	/**
	 * This method is used to get the SMT / CVC constraints for aggregation function
	 * : MIN
	 * 
	 * @param innerTableNo
	 * @param groupNumber
	 * @param totalRows
	 * @param cvc
	 * @param af
	 * @return
	 */
	public String getMaxAssertConstraint(String innerTableNo, Integer groupNumber, int totalRows, GenerateCVC1 cvc,
			AggregateFunction af) {

		String returnStr = "";

		int myCount = cvc.getNoOfTuples().get(innerTableNo);
		int offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];

		if (isCVC3) {

			returnStr += "\nASSERT EXISTS(i: MAX): (";

			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos + offset - 1 + "")
						+ " >= " + "i ";
				if (i < totalRows) {
					returnStr += " AND ";
				}
			}
			returnStr += ") AND (";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				returnStr += GenerateCVCConstraintForNode.cvcMapNode(af.getAggExp(), tuplePos + offset - 1 + "") + " = "
						+ "i ";
				if (i < totalRows) {
					returnStr += " OR ";
				}
			}
			returnStr += ");";
		} else {

			String tableName = af.getAggExp().getColumn().getTable().getTableName();
			String columnName = af.getAggExp().getColumn().getColumnName();
			int index = af.getAggExp().getColumn().getTable().getColumnIndex(columnName);

			myCount = cvc.getNoOfTuples().get(innerTableNo);
			offset = cvc.getRepeatedRelNextTuplePos().get(innerTableNo)[1];
			String function = "MAX_REPLACE_NULL_" + af.getAggExp().getColumn().getCvcDatatype();
			String Nullfunction = "CHECKALL_NULL" + af.getAggExp().getColumn().getCvcDatatype(); // changes made by
																									// sunanda ("_"
																									// removed)

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(and";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				returnStr += "\n ( >= MAX_" + columnName + " (" + function + "(" + tableName + "_" + columnName + index
						+ "(select O_" + tableName + " " + row + ")) ))";
			}
			if (totalRows > 1)
				returnStr += ")";
			returnStr += ")\n";

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(or";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				returnStr += "\n ( = MAX_" + columnName + " (" + function + "(" + tableName + "_" + columnName + index
						+ "(select O_" + tableName + " " + row + ") )))";
			}
			if (totalRows > 1)
				returnStr += ")";
			returnStr += ")\n";

			returnStr += "\n(assert ";
			if (totalRows > 1)
				returnStr += "(and";
			for (int i = 1; i <= totalRows; i++) {
				int tuplePos = (groupNumber) * myCount + i;
				int row = tuplePos + offset - 1;
				if (totalRows > 1)
					returnStr += "\n (not (" + Nullfunction + "(" + tableName + "_" + columnName + index + " (select O_"
							+ tableName + " " + row + ")))) "; // changes made by sunanda "not" added
				else
					returnStr += "\n (not (" + Nullfunction + "(" + tableName + "_" + columnName + index + " (select O_"
							+ tableName + " " + row + "))) "; // changes made by sunanda "not" added
			}
			if (totalRows > 1)
				returnStr += "\n";
			returnStr += "))";
		}

		return returnStr;

	}

	/**
	 * 
	 * @param cvc
	 * @param myCount
	 * @param groupNumber
	 * @param multiples
	 * @param totalRows
	 * @param agg
	 * @param offset
	 * @return
	 */
	public String getSUMConstraint(Integer myCount, Integer groupNumber, Integer multiples, Integer totalRows, Node agg,
			Integer offset) {

		String returnStr = "";
		int extras = totalRows % myCount;
		if (isCVC3) {

			for (int i = 1, j = 0; i <= myCount; i++, j++) {
				int tuplePos = (groupNumber) * myCount + i;

				if (j < extras)
					returnStr += (multiples + 1) + "*("
							+ GenerateCVCConstraintForNode.cvcMapNode(agg, tuplePos + offset - 1 + "") + ")";
				else
					returnStr += (multiples) + "*("
							+ GenerateCVCConstraintForNode.cvcMapNode(agg, tuplePos + offset - 1 + "") + ")";

				if (i < myCount) {
					returnStr += "+";
				}
			}
			return returnStr + "\n";
		} else {
			// returnStr += "(assert ";
			String str = "";
			for (int i = 1, j = 0; i <= myCount; i++, j++) {

				String str1 = str;
				int tuplePos = (groupNumber) * myCount + i;

				if (i <= myCount && str1 != null && !str1.isEmpty()) {
					returnStr += "(+ " + str + " ";
				}
				String function = "SUM_REPLACE_NULL_" + agg.getColumn().getCvcDatatype();

				if (j < extras)
					str = "\n \t (* " + (multiples + 1) + " (" + function
							+ getMapNode(agg, (tuplePos + offset - 1) + "") + "))";
				else
					str = "\n \t (* " + (multiples) + " (" + function + getMapNode(agg, (tuplePos + offset - 1) + "")
							+ "))";

				if (i <= myCount && str1 != null && !str1.isEmpty()) {
					returnStr += str + ") ";
				}
				if (returnStr != null && !returnStr.isEmpty()) {
					str = returnStr;
					if (i != myCount) {
						returnStr = "";
					}
				}

				if (myCount == 1 && i == myCount) {
					returnStr = str;
				}

			}
			return returnStr + "\n";
		}

	}

	/**
	 * This method returns Constraint String for Aggregate node SUM in the subquery
	 * 
	 * @param myCount
	 * @param groupNumber
	 * @param multiples
	 * @param totalRows
	 * @param agg
	 * @param offset
	 * @return
	 */
	public String getSUMConstraintForSubQ(Integer myCount, Integer groupNumber, Integer multiples, Integer totalRows,
			Node agg, Integer offset) {

		String returnStr = "";
		int extras = totalRows % myCount;
		boolean isDistinct = agg.isDistinct();
		int groupOffset = groupNumber * myCount;

		if (isCVC3) {

			if (isDistinct && myCount > 1) {// mahesh: add
				// there will be three elements in the group
				int ind = 0;
				for (int m = 0; m < 2; m++) {
					returnStr += "(";
					for (int i = 0, j = 0; i < myCount - 1; i++, j++) {
						if (j < extras)
							returnStr += (multiples + 1) + "*("
									+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + groupOffset) + "") + ")";
						else
							returnStr += (multiples) + "*("
									+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + groupOffset) + "") + ")";
						// " DISTINCT (O_"+ cvcMap(col,group+offset-1+"") +", O_"+
						// cvcMap(col,(group+aliasCount-1+offset)+"") +") "
						if (i < myCount - 2) {
							returnStr += "+";
						}
					}
					// add contsraint for distinct
					returnStr += " )) AND ";
					for (int i = 0, j = 0; i < myCount - 2; i++, j++) {
						returnStr += "DISTINCT( "
								+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + groupOffset) + "") + " , "
								+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + 1 + groupOffset) + "")
								+ ") AND ";
					}
					returnStr = returnStr.substring(0, returnStr.lastIndexOf("AND") - 1);
					ind++;
					returnStr += ") OR ";
				}
				if (returnStr.contains("OR"))
					returnStr = returnStr.substring(0, returnStr.lastIndexOf("OR") - 1);
				return returnStr;
			}
			for (int i = 0, j = 0; i < myCount; i++, j++) {
				if (j < extras)
					returnStr += (multiples + 1) + "*("
							+ ConstraintGenerator.cvcMapNode(agg, (i + offset + groupOffset) + "") + ")";
				else
					returnStr += (multiples) + "*("
							+ ConstraintGenerator.cvcMapNode(agg, (i + offset + groupOffset) + "") + ")";

				if (i < myCount - 1) {
					returnStr += "+";
				}
			}
			return returnStr;
		} else {
			ConstraintObject constrObj = new ConstraintObject();
			ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();

			ConstraintObject constrObj1 = new ConstraintObject();
			ArrayList<ConstraintObject> constrList1 = new ArrayList<ConstraintObject>();

			if (isDistinct && myCount > 1) {// mahesh: add
				// there will be three elements in the group.
				constrObj1 = new ConstraintObject();
				int ind = 0;
				for (int m = 0; m < 2; m++) {
					// returnStr +="(and (";
					for (int i = 0, j = 0; i < myCount - 1; i++, j++) {

						if (i < myCount - 2) {
							returnStr += "(+ (";
						}

						constrObj = new ConstraintObject();
						if (j < extras)
							returnStr += "(* " + (multiples + 1) + " "
									+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + groupOffset) + "") + ")";
						else
							returnStr += "(* " + (multiples) + " "
									+ ConstraintGenerator.cvcMapNode(agg, (i + offset + ind + groupOffset) + "") + ")";
						// " DISTINCT (O_"+ cvcMap(col,group+offset-1+"") +", O_"+
						// cvcMap(col,(group+aliasCount-1+offset)+"") +") "
						if (i < myCount - 2) {
							returnStr += "))";
						}
						constrObj.setLeftConstraint(returnStr);
						constrList.add(constrObj);
					}
					returnStr += "( assert " + generateANDConstraints(constrList) + " )\n";
					// add contsraint for distinct
					// returnStr+=" )) ";
					constrList = new ArrayList<ConstraintObject>();
					for (int i = 0, j = 0; i < myCount - 2; i++, j++) {
						String ret = "";
						constrObj = new ConstraintObject();
						ret += getDistinctConstraint(agg, i + offset + ind + groupOffset);

						constrObj.setLeftConstraint(ret);
						constrList.add(constrObj);
						// "DISTINCT( "+ ConstraintGenerator.cvcMapNode(agg,
						// (i+offset+ind+groupOffset)+"")+" , "+ ConstraintGenerator.cvcMapNode(agg,
						// (i+offset+ind+1+groupOffset)+"")+") AND ";
					}
					returnStr += "( assert " + generateANDConstraints(constrList) + " )\n";
					ind++;

					constrObj1.setLeftConstraint(returnStr);
					constrList1.add(constrObj1);
					// returnStr +=") OR ";
				}
				returnStr = generateOrConstraints(constrList1);
				// if(returnStr.contains("OR"))
				// returnStr = returnStr.substring(0,returnStr.lastIndexOf("OR")-1);
				return returnStr;
			}
			for (int i = 0, j = 0; i < myCount; i++, j++) {
				if (i < myCount - 1) {
					returnStr += "(+ ";
				}
				if (j < extras)
					returnStr += "(* " + (multiples + 1) + " "
							+ ConstraintGenerator.cvcMapNode(agg, (i + offset + groupOffset) + "") + ")";
				else
					returnStr += "(* " + (multiples) + " "
							+ ConstraintGenerator.cvcMapNode(agg, (i + offset + groupOffset) + "") + ")";

				if (i < myCount - 1) {
					returnStr += ")";
				}
			}
			return returnStr;

		}
	}

	/**
	 * This method takes list of constraints of type String and returns AND'ed
	 * constraint String based on the solver used.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String getNullConditionConjuncts(ArrayList<String> constraintList) {
		String constraint = "";
		if (isCVC3) {
			for (String con : constraintList) {
				constraint += con + " AND ";
			}
			if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" AND ")) {
				constraint = constraint.substring(0, constraint.length() - 5);
			}
		} else {
			String constr1 = "";
			for (String con : constraintList) {
				constr1 = getNullConditionForStrings(con, constr1);
				constraint = constr1;
			}

		}
		return constraint;
	}

	/**
	 * This method gets tow string constraints and returns a single OR'red
	 * constraint for foreign keys
	 * 
	 * @param cvc
	 * @param fkConstraint
	 * @param nullConstraint
	 * @return
	 */
	public String getFKConstraint(String fkConstraint, String nullConstraint) {

		String fkConstraints = "";
		if (isCVC3) {
			if (nullConstraint != null && !nullConstraint.isEmpty()) {
				fkConstraints += "ASSERT (" + fkConstraint + ") OR (" + nullConstraint + ");\n";
			} else {
				fkConstraints += "ASSERT (" + fkConstraint + ");\n";
			}
		} else {
			if (nullConstraint != null && !nullConstraint.isEmpty()) {
				fkConstraints = "(assert (or " + fkConstraint + " " + nullConstraint + "))\n";
			} else {
				fkConstraints = "(assert " + fkConstraint + " ) \n";
			}
		}
		return fkConstraints;
	}

	/**
	 * This method will return SMT constraints of the form (or (StringValue)
	 * (ISNULL_COLNAME (colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested
	 * structure as required for SMT.
	 * 
	 * @param con
	 * @param s1
	 * @return
	 */
	public String getNullConditionForStrings(String con, String s1) {

		String cvcStr = "";

		if (s1 != null && !s1.isEmpty()) {
			cvcStr += " (and ";
			cvcStr += s1;
		}
		if (con != null) {
			cvcStr += con;
		}
		if (s1 != null && !s1.isEmpty()) {
			cvcStr += ")  ";
		}
		return cvcStr;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates a
	 * constraint String AND + OR conditions.
	 * The returned string holds the AND + ORconstraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateAndOrConstraints(ArrayList<ConstraintObject> AndConstraintList,
			ArrayList<ConstraintObject> OrConstraintList) {
		String constraint = "";
		if (isCVC3) {
			constraint += "\nASSERT ";
			constraint += generateCVCAndConstraints(AndConstraintList);
			if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" AND ")) {
				constraint = constraint.substring(0, constraint.length() - 5);
			}
			constraint += generateCVCOrConstraints(OrConstraintList);
			if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" OR ")) {
				constraint = constraint.substring(0, constraint.length() - 4);
			}
			constraint += ";\n";

		} else {
			constraint += "\n (assert ";
			constraint += generateSMTAndConstraints(AndConstraintList, null);
			constraint += generateSMTOrConstraints(OrConstraintList, constraint);
			constraint += " ) \n";
		}

		return constraint;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates a
	 * constraint String OR + AND all conditions.
	 * The returned string holds the OR + AND constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateOrAndConstraints(ArrayList<ConstraintObject> AndConstraintList,
			ArrayList<ConstraintObject> OrConstraintList) {
		String constraint = "";
		if (isCVC3) {
			constraint += "\nASSERT ";
			constraint += generateCVCOrConstraints(OrConstraintList);
			if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" OR ")) {
				constraint = constraint.substring(0, constraint.length() - 4);
			}
			constraint += generateCVCAndConstraints(AndConstraintList);
			if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" AND ")) {
				constraint = constraint.substring(0, constraint.length() - 5);
			}
			constraint += ";\n";

		} else {
			constraint += "\n (assert ";
			constraint += generateSMTOrConstraints(OrConstraintList, null);
			constraint += generateSMTAndConstraints(AndConstraintList, constraint);
			constraint += " ) \n";
		}

		return constraint;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates a
	 * constraint String AND'ing all conditions.
	 * The returned string holds the AND'ed constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateANDConstraints(ArrayList<ConstraintObject> constraintList) {
		String constraint = "";
		if (isCVC3) {
			constraint = generateCVCAndConstraints(constraintList);
		} else {
			constraint = generateSMTAndConstraints(constraintList, null);
		}
		return constraint;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates an
	 * ASSERT constraint String AND'ing all conditions.
	 * The returned string holds the AND'ed constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */

	public String generateANDConstraintsWithAssert(ArrayList<ConstraintObject> constraintList) {
		String constraint = "";
		if (isCVC3) {
			constraint = "\n ASSERT " + generateCVCAndConstraints(constraintList) + "; \n";
		} else {
			constraint = "\n (assert " + generateSMTAndConstraints(constraintList, null) + ") \n";
		}

		return constraint;
	}

	public String revertAndToOR(String constraintString) {
		String constraint = "";
		String constraint1 = "";

		if (isCVC3) {
			/*
			 * for(String str: constraintString.split(" AND ") )
			 * if( str.length() >= 7)
			 * constraint1 += str.substring(7, str.length()) + " OR ";
			 * 
			 * if(constraint1.length() >= 4 )
			 * constraint += "ASSERT " + constraint1.substring(0, constraint1.length() - 3)
			 * + ";\n";
			 */

			for (String str : constraintString.split(" AND ")) {
				constraint1 += str.substring(7, str.length()) + " OR ";
				constraint += "ASSERT " + constraint1.substring(0, constraint1.length() - 3) + ";\n";
			}

		} else {
			if (constraintString.contains("(and ")) {
				constraint1 += constraintString.replaceAll("\\(and", "(or");

				constraint += "(assert " + constraint1 + ") \n";
			} else
				constraint = constraintString;

		}
		return constraint;
	}

	public String replaceOrByOperator(String right, Node n, String left) {
		String constraint = "";
		String constraint1 = "";
		String returnValue = "";// ASSERT (";

		if (isCVC3) {
			returnValue = "ASSERT (";
			if (right.contains(" OR ")) {
				String split[] = right.split(" OR ");
				for (int i = 0; i < split.length; i++)
					returnValue += "(" + left + "" + n.getOperator() + split[i] + " OR ( ";
				returnValue = returnValue.substring(0, returnValue.lastIndexOf("OR") - 1) + ";\n";
				return returnValue;
			}

		} else {
			if (right.contains("(or") || right.contains("( or ")) {

			}
			constraint += "(assert " + constraint1 + ") \n";
		}
		return constraint;
	}

	/**
	 * This method returns a constraint for null value in the query depending on
	 * solver.
	 * 
	 * @param cvc
	 * @param c
	 * @param index
	 * @param nullVal
	 * @return
	 */
	public String getAssertNullValue(Column c, String index, String nullVal) {
		if (isCVC3) {
			return "\nASSERT " + cvcMap(c, index) + " = " + nullVal + "; \n";
		} else {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);

			if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
				Column cntCol1 = c.getTable().getColumn(c.getTable().getNoOfColumn() - 1); // added by sunanda for count
				String WithCount1 = getConstraintsForValidCountUsingTable(c.getTable(), c.getTableName(),
						Integer.parseInt(index), 0).toString();
				;

				if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
					// WithCount1 = ConstraintGenerator.ctx.mkGt(
					// (ArithExpr) ConstraintGenerator.smtMap(cntCol1,
					// ctx.mkConst(enumIndexVar+index, currentSort)),
					// ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
					return "\n (assert (and (= " + smtMap(c, ctx.mkConst(enumIndexVar + index, currentSort)) + " "
							+ nullVal + "  )\n\t"
							+ WithCount1 + " )) \n";
				}

				else {
					return "\n (assert (and (= " + smtMap(c, (IntExpr) ctx.mkInt(index)) + " " + nullVal + "  )\n\t"
							+ WithCount1.toString() + " )) \n";
				}

			} else {
				if (Configuration.isEnumInt.equalsIgnoreCase("true"))
					return "\n (assert (= " + smtMap(c, ctx.mkConst(enumIndexVar + index, currentSort)) + " " + nullVal
							+ "  )) \n";

				else
					return "\n (assert (= " + smtMap(c, (IntExpr) ctx.mkInt(index)) + " " + nullVal + "  )) \n";

			}
		}
	}

	public String getAssertNotNullValue(Column c, String index, String nullVal) {
		EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);

		if (isCVC3) {
			return "\nASSERT " + cvcMap(c, index) + " = " + nullVal + "; \n";
		} else {
			if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
				Column cntCol1 = c.getTable().getColumn(c.getTable().getNoOfColumn() - 1); // added by sunanda for count
				String WithCount1 = getConstraintsForValidCountUsingTable(c.getTable(), c.getTableName(),
						Integer.parseInt(index), 0).toString();
				if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
					return "\n (assert (and (not (= " + smtMap(c, ctx.mkConst(enumIndexVar + index, currentSort)) + " "
							+ nullVal + "  ))\n\t"
							+ WithCount1 + " )) \n";
				} else
					return "\n (assert (and (not (= " + smtMap(c, (IntExpr) ctx.mkInt(index)) + " " + nullVal
							+ "  ))\n\t"
							+ WithCount1 + " )) \n";
			} else {
				if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
					return "\n (assert (not (= " + smtMap(c, (ctx.mkConst(enumIndexVar + index, currentSort))) + " "
							+ nullVal + "  ))) \n";
				} else
					return "\n (assert (not (= " + smtMap(c, (IntExpr) ctx.mkInt(index)) + " " + nullVal + "  ))) \n";

			}
		}
	}

	/**
	 * This method returns constraintString for primary keys with =>. It takes left
	 * and right constraint strings and adds => on them and returns new string.
	 * 
	 * @param cvc
	 * @param impliedConObj
	 * @param isImplied
	 * @return
	 */
	public String getImpliedConstraints(ConstraintObject impliedConObj, boolean isImplied) {
		String constrString = "";
		if (isCVC3) {
			if (isImplied) {
				constrString = "ASSERT (" + impliedConObj.getLeftConstraint() + " " + impliedConObj.getOperator() + " "
						+ impliedConObj.getRightConstraint() + ");\n";
			} else {
				constrString = "ASSERT (" + impliedConObj.getLeftConstraint() + ") " + impliedConObj.getOperator()
						+ " TRUE; \n";
			}
		} else {
			if (isImplied) {
				constrString = "\n (assert (=> \n\t" + impliedConObj.getLeftConstraint() + " "
						+ impliedConObj.getRightConstraint() + "\n))";
			} else {
				constrString = "\n (assert (=> \n\t" + impliedConObj.getLeftConstraint() + " true)) ";
			}
		}
		return constrString;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates a
	 * constraint String OR'ing all conditions.
	 * The returned string holds the OR'ed constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateOrConstraints(ArrayList<ConstraintObject> constraintList) {
		String constraint = "";
		if (isCVC3) {
			constraint = generateCVCOrConstraints(constraintList);
		} else {
			constraint = generateSMTOrConstraints(constraintList, null);
		}
		return constraint;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates an
	 * ASSERT constraint String OR'ing all conditions.
	 * The returned string holds the OR'ed constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 */
	public String generateOrConstraintsWithAssert(ArrayList<ConstraintObject> constraintList) {
		String constraint = "";
		if (isCVC3) {
			if (generateCVCOrConstraints(constraintList) != null
					&& !generateCVCOrConstraints(constraintList).isEmpty()) {
				constraint = "\n ASSERT" + generateCVCOrConstraints(constraintList) + "; \n";
			}
		} else {
			if (generateSMTOrConstraints(constraintList, null) != null
					&& !generateSMTOrConstraints(constraintList, null).isEmpty()) {
				/*
				 * if(isTempJoin) {
				 * String constr,declare="";
				 * int st_index=0,end_index=0;
				 * constr = generateSMTOrConstraints(constraintList,null);
				 * while(constr.indexOf("(declare-datatypes ()") != -1) {
				 * st_index = constr.indexOf("(declare-datatypes ()");
				 * end_index = constr.indexOf("_TupleType))")+12;
				 * if(!declare.contains(constr.substring(st_index, end_index)))
				 * declare += constr.substring(st_index, end_index) + " \n";
				 * constr = constr.substring(0, st_index)+constr.substring(end_index);
				 * }
				 * 
				 * constraint = declare + "\n (assert "+constr+") \n";
				 * }
				 */

				if (Configuration.getProperty("tempJoins").equalsIgnoreCase("true")) {
					isTempJoin = true;
				} else {
					isTempJoin = false;
				}

				if (isTempJoin) {
					String constr, declare = "";
					int st_index = 0, end_index = 0;
					boolean inside = false;
					constr = generateSMTOrConstraints(constraintList, null);
					while (constr.indexOf("(declare-datatypes ()") != -1) {
						inside = true;
						st_index = constr.indexOf("(declare-datatypes ()");
						end_index = constr.indexOf("_TupleType))") + 12;
						if (!declare.contains(constr.substring(st_index, end_index)))
							declare += constr.substring(st_index, end_index) + " \n";
						constr = constr.substring(0, st_index) + constr.substring(end_index);
					}
					if (constr.isEmpty())
						constraint = "";
					else
						constraint = declare + "\n (assert " + constr + ") \n";
				} else
					constraint = "\n (assert " + generateSMTOrConstraints(constraintList, null) + ") \n";
			}
		}

		return constraint;
	}

	/**
	 * This method takes in the ConstraintObject List as input and generates a
	 * constraint String with NOT conditions.
	 * The returned string holds the NOT constraints in SMT or CVC format.
	 * 
	 * @param cvc
	 * @param constraintList
	 * @return
	 * 
	 *         public String generateNotConstraints(GenerateCVC1 cvc,
	 *         ArrayList<ConstraintObject> constraintList){
	 *         String constraint = "";
	 *         if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
	 *         constraint = generateCVCNotConstraints(constraintList);
	 *         }else{
	 *         constraint = generateSMTNotConstraints(cvc,constraintList);
	 *         }
	 *         return constraint;
	 *         }
	 */

	/**
	 * This method returns AND'ed constraints with Assert and ; as required for CVC
	 * solver.
	 * 
	 * @param constraintList
	 * @return
	 */
	public String generateCVCAndConstraints(ArrayList<ConstraintObject> constraintList) {

		String constraint = "";
		// constraint += "(";
		for (ConstraintObject con : constraintList) {
			constraint += "(" + con.getLeftConstraint() + " " + con.getOperator() + " " + con.getRightConstraint()
					+ ") AND ";
		}
		if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" AND ")) {
			constraint = constraint.substring(0, constraint.length() - 5);
		}
		// constraint += ")";
		if (constraint == null || constraint == "") {
			return "";
		}
		return constraint;
	}

	/**
	 * This method will return SMT constraints of the form (or (StringValue)
	 * (and (operator (colname tableNameNo_colName)(colName tableNameNo_colName))
	 * (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested
	 * structure as required for SMT.
	 * 
	 */
	/*
	 * public String generateSMTAndConstraints(ArrayList<ConstraintObject>
	 * constraintList){
	 * 
	 * String constraintStr = "";
	 * String constr1 ="";
	 * for(ConstraintObject con : constraintList){
	 * constr1 = getSMTAndConstraint(con,constr1);
	 * constraintStr = constr1;
	 * }
	 * return constraintStr;
	 * }
	 */

	public String generateSMTAndConstraints(ArrayList<ConstraintObject> constraintList, String constraintStr) {
		String constr1 = "";

		for (ConstraintObject con : constraintList) {
			constr1 = getSMTAndConstraint(con, constr1, constraintList);

			constraintStr = constr1 + "\t";

		}

		if (constraintStr == null || constraintStr == "") {
			return "";
		} else if (constraintList != null) {
			constraintStr = " (and " + constraintStr + ")";
		}

		//
		// //testcode by deeksha
		// File testFile = null;
		//
		// testFile = new
		// File(Configuration.homeDir+"/temp_smt/testconstraints"+".smt");
		//
		// if(!testFile.exists())
		// {
		// testFile.delete();
		// }
		// try
		// {
		// testFile.createNewFile();
		//
		// //constraints for and
		// Solver dummySol = ctx.mkSolver();
		//
		//
		//
		//
		// Expr left = (Expr)ctx.mkString(constraintStr);
		// Expr right = (Expr)ctx.mkString(constraintStr);
		//
		//
		// Expr eq = ctx.mkEq(left, right);
		//
		//
		//
		// }
		// catch(IOException e)
		// {
		// e.printStackTrace();
		// }
		//
		//

		// code ends here
		return constraintStr;
	}

	/**
	 * This method will return SMT constraints of the form (or (StringValue)
	 * (or (operator (colname tableNameNo_colName)(colName tableNameNo_colName))
	 * (operator (colname tableNameNo_colName)(colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested
	 * structure as required for SMT.
	 * 
	 */
	/*
	 * public String generateSMTOrConstraints(ArrayList<ConstraintObject>
	 * constraintList){
	 * 
	 * String constraintStr = "";
	 * for(ConstraintObject con : constraintList){
	 * constraintStr += getSMTOrConstraint(con,constraintStr);
	 * }
	 * return constraintStr;
	 * }
	 */

	public String generateSMTOrConstraints(ArrayList<ConstraintObject> constraintList, String constraintStr) {

		String constr1 = "";
		if (constraintList == null && constraintStr != null) {
			return " (or " + constraintStr + ")";
		}
		for (ConstraintObject con : constraintList) {
			// constraintStr += getSMTOrConstraint(con,constraintStr);
			constr1 = getSMTOrConstraint(con, constr1, constraintList);
			if (!constr1.isEmpty())
				constraintStr = constr1 + "\n\t\t";
		}

		if (constraintStr == null || constraintStr == "") {
			constraintStr = "";
		} else if (constraintList != null && constraintList.size() > 1) {
			constraintStr = " (or " + constraintStr + ")";
		}
		return constraintStr;
	}

	/**
	 * This method will return SMT constraints of the form (or (StringValue)
	 * (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested
	 * structure as required for SMT.
	 * 
	 * @param con
	 * @param s1
	 * @return
	 */

	public String getSMTAndConstraint(ConstraintObject con, String s1, ArrayList<ConstraintObject> conList) {
		// to be done using api comment by deeksha
		String cvcStr = "";

		if (s1 != null && !s1.isEmpty() || (s1 != null && !s1.isEmpty() && conList != null && conList.size() > 1)) {
			// cvcStr += " (and ";
			cvcStr += s1;
		}
		if (con != null) {

			String Rightconstr = "";
			String Leftconstr = "";
			///
			if ((con.getRightConstraint() != null && !con.getRightConstraint().isEmpty())) {
				Rightconstr = con.getRightConstraint().trim();

				int st_index = Rightconstr.indexOf("(assert");
				while (st_index != -1) {
					// Rightconstr.replaceFirst("(assert", " ");
					Leftconstr = Rightconstr.substring(0, st_index) + Leftconstr.substring(st_index + 7);
					int count = 1;
					int end_index = st_index + 1;
					for (; end_index < Rightconstr.length(); end_index++) {
						if (Rightconstr.charAt(end_index) == '(')
							count++;
						else if (Rightconstr.charAt(end_index) == ')')
							count--;
						if (count == 0) {
							Rightconstr = Rightconstr.substring(0, end_index) + Rightconstr.substring(end_index + 1);
							st_index = Rightconstr.indexOf("(assert");
							break;
						}

					}
					if (end_index == Rightconstr.length())
						break;
				}

			}

			if ((con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty())) {
				Leftconstr = con.getLeftConstraint().trim();
				int st_index = Leftconstr.indexOf("(assert");
				while (st_index != -1) {
					Leftconstr = Leftconstr.substring(0, st_index) + Leftconstr.substring(st_index + 7);
					// Leftconstr.replaceFirst(" assert ", " ");
					int count = 1;
					int end_index = st_index + 1;
					for (; end_index < Leftconstr.length(); end_index++) {
						if (Leftconstr.charAt(end_index) == '(')
							count++;
						else if (Leftconstr.charAt(end_index) == ')')
							count--;
						if (count == 0) {
							Leftconstr = Leftconstr.substring(0, end_index) + Leftconstr.substring(end_index + 1);
							st_index = Leftconstr.indexOf("(assert");
							break;
						}

					}
					if (end_index == Leftconstr.length())
						break;
				}

			}

			// If else statements are added to get the matching brackets based on the
			// operator and constraint

			if ((con.getOperator() != null && !con.getOperator().isEmpty())
					&& (con.getRightConstraint() != null && !con.getRightConstraint().isEmpty())) {

				cvcStr += "";
				if ((con.getOperator() != null && !(con.getOperator().isEmpty()))) {
					cvcStr += "(";
					if (con.getOperator().equals("/=")) {
						cvcStr += "not (= ";
					} else {
						cvcStr += con.getOperator() + " ";
					}
				} else {
					cvcStr += " ";
				}

				if (con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty()) {
					if (Leftconstr.trim().startsWith("(")) {
						cvcStr += Leftconstr + " ";
					} else {
						cvcStr += " (" + Leftconstr + ") ";
					}
				} else {
					cvcStr += " ";
				}

				if (con.getRightConstraint() != null && !con.getRightConstraint().isEmpty()) {
					if (isIntOrReal(con.getRightConstraint()) || Rightconstr.trim().startsWith("(")) {
						cvcStr += Rightconstr;
					} else {
						cvcStr += "(" + Rightconstr + ")";
					}
				} else {
					cvcStr += " ";
				}

				if ((con.getOperator() != null && !(con.getOperator().isEmpty()))) {
					if (con.getOperator().equals("/=")) {
						cvcStr += "))";
					} else {
						cvcStr += ")";
					}
				} else {
					cvcStr += " ";
				}

				// cvcStr += ((con.getOperator() != null && ! (con.getOperator().isEmpty()))
				// ? "("+(con.getOperator().equals("/=")? "not (= ": con.getOperator()+" ") : "
				// ")

				// + ((con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty())
				// ? (con.getLeftConstraint().startsWith("(")? con.getLeftConstraint()+" " : "
				// ("+con.getLeftConstraint()+") "):" ")

				// +(con.getRightConstraint() !=null && ! con.getRightConstraint().isEmpty()
				// ? ((isIntOrReal(con.getRightConstraint())
				// || con.getRightConstraint().startsWith("(")) ? con.getRightConstraint() :
				// "("+con.getRightConstraint()+")") :" ")

				// + ((con.getOperator() != null && ! (con.getOperator().isEmpty())) ?
				// (con.getOperator().equals("/=")? "))": ")"): " ");

			} else {
				if (!Leftconstr.isEmpty())
					cvcStr += Leftconstr.trim().startsWith("(") ? " " + Leftconstr + " " : " (" + Leftconstr + ") ";
			}
		}
		if (s1 != null && !s1.isEmpty() || (s1 != null && !s1.isEmpty() && conList != null && conList.size() > 1)) {
			// cvcStr +=") ";
		}
		return cvcStr;
	}

	/**
	 * This method will return SMT constraints of the form (or (StringValue)
	 * (distinct (colname tableNameNo_colName)(colName tableNameNo_colName)) )
	 * StringValue holds the previous constraint of same form thus forming nested
	 * structure as required for SMT.
	 * 
	 * @param con
	 * @param s1
	 * @return
	 */
	public String getSMTOrConstraint(ConstraintObject con, String s1, ArrayList<ConstraintObject> conList) {

		String cvcStr = "";

		if (s1 != null && !s1.isEmpty() || (s1 != null && !s1.isEmpty() && conList != null && conList.size() > 1)) {
			// cvcStr += " (or ";
			cvcStr += s1;
		}

		if (con != null) {

			String Rightconstr = "";
			String Leftconstr = "";
			/// Code to remove nested assert string
			if ((con.getRightConstraint() != null && !con.getRightConstraint().isEmpty())) {
				Rightconstr = con.getRightConstraint().trim();

				int st_index = Rightconstr.indexOf("(assert");
				while (st_index != -1) {
					// Rightconstr.replaceFirst("(assert", " ");
					Leftconstr = Rightconstr.substring(0, st_index) + Leftconstr.substring(st_index + 7);
					int count = 1;
					int end_index = st_index + 1;
					for (; end_index < Rightconstr.length(); end_index++) {
						if (Rightconstr.charAt(end_index) == '(')
							count++;
						else if (Rightconstr.charAt(end_index) == ')')
							count--;
						if (count == 0) {
							Rightconstr = Rightconstr.substring(0, end_index) + Rightconstr.substring(end_index + 1);
							st_index = Rightconstr.indexOf("(assert");
							break;
						}

					}
					if (end_index == Rightconstr.length())
						break;
				}

			}

			if ((con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty())) {
				Leftconstr = con.getLeftConstraint().trim();
				int st_index = Leftconstr.indexOf("(assert");
				while (st_index != -1) {
					Leftconstr = Leftconstr.substring(0, st_index) + Leftconstr.substring(st_index + 7);
					// Leftconstr.replaceFirst(" assert ", " ");
					int count = 1;
					int end_index = st_index + 1;
					for (; end_index < Leftconstr.length(); end_index++) {
						if (Leftconstr.charAt(end_index) == '(')
							count++;
						else if (Leftconstr.charAt(end_index) == ')')
							count--;
						if (count == 0) {
							Leftconstr = Leftconstr.substring(0, end_index) + Leftconstr.substring(end_index + 1);
							st_index = Leftconstr.indexOf("(assert");
							break;
						}

					}
					if (end_index == Leftconstr.length())
						break;
				}

			}
			///

			if ((con.getOperator() != null && !con.getOperator().isEmpty()) && (con.getRightConstraint() != null
					&& !con.getRightConstraint().isEmpty())) {

				cvcStr += "";
				// If else statements are added to get the matching brackets based on the
				// operator and constraint

				if (con.getOperator() != null && !(con.getOperator().isEmpty())) {
					cvcStr += "(";
					if (con.getOperator().equals("/=")) {
						cvcStr += "not (= ";
					} else {
						cvcStr += con.getOperator() + " ";
					}
				} else {
					cvcStr += " ";
				}

				if (con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty()) {
					if (Leftconstr.trim().startsWith("(")) {
						cvcStr += Leftconstr + " ";
					} else {
						cvcStr += " (" + Leftconstr + ") ";
					}
				} else {
					cvcStr += " ";
				}

				if (con.getRightConstraint() != null && !con.getRightConstraint().isEmpty()) {
					if ((isIntOrReal(con.getRightConstraint()) || Rightconstr.trim().startsWith("("))) {
						cvcStr += Rightconstr;
					} else {
						cvcStr += "(" + Rightconstr + ")";
					}
				} else {
					cvcStr += " ";
				}

				if (con.getOperator() != null && !(con.getOperator().isEmpty())) {
					if (con.getOperator().equals("/=")) {
						cvcStr += "))";
					} else {
						cvcStr += ")";
					}
				} else {
					cvcStr += " ";
				}

				// ((con.getOperator() != null && ! (con.getOperator().isEmpty())) ?
				// "("+(con.getOperator().equals("/=")? "not (= ": con.getOperator()+" ") : " ")

				// + ((con.getLeftConstraint() != null && !con.getLeftConstraint().isEmpty())
				// ? (con.getLeftConstraint().trim().startsWith("(")? con.getLeftConstraint()+"
				// " : " ("+con.getLeftConstraint()+") "):" ")

				// +(con.getRightConstraint() !=null && ! con.getRightConstraint().isEmpty()
				// ? ((isIntOrReal(con.getRightConstraint()) ||
				// con.getRightConstraint().trim().startsWith("("))
				// ? con.getRightConstraint() : "("+con.getRightConstraint()+")") :" ")

				// + ((con.getOperator() != null && ! (con.getOperator().isEmpty())) ?
				// (con.getOperator().equals("/=")? "))": ")"): " ");

			} else {
				if (!Leftconstr.isEmpty())
					cvcStr += Leftconstr.trim().startsWith("(") ? (" " + Leftconstr + " ")
							: " (" + con.getLeftConstraint() + ") ";
			}
		}
		if (s1 != null && !s1.isEmpty() || (s1 != null && !s1.isEmpty() && conList != null && conList.size() > 1)) {
			// cvcStr +=") ";
		}

		// cvcStr = "(or "+cvcStr+")";
		return cvcStr;
	}

	/**
	 * This method checks if the parameter passed is Integer or real.
	 * 
	 * @param constraint
	 * @return
	 */
	public boolean isIntOrReal(String constraint) {
		if (constraint.length() > 0 && constraint.matches("[0-9]+")) {
			return true;
		} else if (constraint.length() > 0 && constraint.matches("[0-9]+.[0-9]+")) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This method returns OR'ed constraints with Assert and ; as required for CVC3
	 * solver.
	 * 
	 * @param constraintList
	 * @return
	 */
	public String generateCVCOrConstraints(ArrayList<ConstraintObject> constraintList) {

		String constraint = "";

		for (ConstraintObject con : constraintList) {
			constraint += "(" + con.getLeftConstraint() + " " + con.getOperator() + " " + con.getRightConstraint()
					+ ") OR ";
		}
		if (constraint != null && !constraint.isEmpty() && constraint.endsWith(" OR ")) {
			constraint = constraint.substring(0, constraint.length() - 4);
		}

		return constraint;
	}

	/**
	 * Used to get CVC3 constraint for this column for the given tuple position
	 * 
	 * @param col
	 * @param index
	 * @return
	 */
	public static String cvcMap(Column col, String index) {
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		int pos = table.getColumnIndex(columnName);
		index = index.trim();
		return "O_" + tableName + "[" + index + "]." + pos;
	}

	/**
	 * Used to get CVC3 constraint for this column
	 * 
	 * @param col
	 * @param n
	 * @return
	 */
	public static String cvcMap(Column col, Node n) {
		Table table = col.getTable();
		String tableName = col.getTableName();
		String aliasName = col.getAliasName();
		String columnName = col.getColumnName();
		String tableNo = n.getTableNameNo();
		int index = Integer.parseInt(tableNo.substring(tableNo.length() - 1));
		int pos = table.getColumnIndex(columnName);
		return "O_" + tableName + "[" + index + "]." + pos;
	}

	/**
	 * Used to get CVC3 constraint for the given node for the given tuple position
	 * 
	 * @param n
	 * @param index
	 * @return
	 */
	public static String cvcMapNode(Node n, String index) {
		if (n.getType().equalsIgnoreCase(Node.getValType())) {
			return n.getStrConst();
		} else if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			return cvcMap(n.getColumn(), index);
		} else if (n.getType().toString().equalsIgnoreCase("i")) {
			return "i";
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())) {
			return "(" + cvcMapNode(n.getLeft(), index) + n.getOperator() + cvcMapNode(n.getRight(), index) + ")";
		} else
			return "";
	}

	/**
	 * Used to get SMT LIB constraint for this column for the given tuple position
	 * 
	 * @param col
	 * @param fIndex
	 * @return
	 */
	public static Expr smtMap(Column col, Expr fIndex) {
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		int pos = table.getColumnIndex(columnName);
		Expr colValue = null;
		// relations are represented as ArrayExpr's
		ArrayExpr relation = (ArrayExpr) ctxConsts.get("O_" + tableName);
		// tuples are represented as DatatypeExpr's
		// if(relation != null)
		// {
		DatatypeExpr tuple = (DatatypeExpr) ctx.mkSelect(relation, fIndex);

		DatatypeSort tupSort = (DatatypeSort) tuple.getSort();
		FuncDecl[] tupAccessors = tupSort.getAccessors()[0]; // tuples declared will have only one constructor,
																// hence[0].
		FuncDecl colAccessor = tupAccessors[pos];
		colValue = colAccessor.apply(tuple);
		// }
		return colValue;
	}

	/**
	 * Used to get SMT LIB constraint for this column
	 * 
	 * @param col
	 * @param n
	 * @return
	 */
	public static String smtMap(Column col, Node n) {
		Table table = col.getTable();
		String tableName = col.getTableName();
		String columnName = col.getColumnName();
		String tableNo = n.getTableNameNo();

		int index = Integer.parseInt(tableNo.substring(tableNo.length() - 1));
		int pos = table.getColumnIndex(columnName);

		String smtCond = "";
		// String colName =tableName+"_"+columnName;
		String colName = tableName + "_" + columnName + pos;
		smtCond = "(" + colName + " " + "(select O_" + tableName + " " + index + ") )";
		return smtCond;
	}

	public static Expr smtMap(String arrNameSQ, String index, int isIndexInt, int colIndex) {
		// Parameters :
		// arrNameSQ :- NAME Of relation
		// index :- row number to be selected
		// isIndexInt :- if you want to select variables "i", "j", then pass -1, else
		// pass the integer you want to use, in that case index parameter will be
		// ignored
		// colIndex :- index of column to be selected

		ArrayExpr subqueryRelationSQ = (ArrayExpr) ctxConsts.get(arrNameSQ);
		DatatypeExpr tupleSQ;
		EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);

		if (isIndexInt == -1) {
			Expr[] arrForIndexI = new Expr[] { ctx.mkIntConst(index) };
			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				arrForIndexI = new Expr[] { ctx.mkConst(index, currentSort) };
			}
			tupleSQ = (DatatypeExpr) ctx.mkSelect(subqueryRelationSQ, arrForIndexI[0]);

		} else {
			Expr arrForIndex1 = (IntExpr) ctx.mkInt(isIndexInt);
			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				arrForIndex1 = ctx.mkConst(enumIndexVar + Integer.toString(isIndexInt), currentSort);
			}
			tupleSQ = (DatatypeExpr) ctx.mkSelect(subqueryRelationSQ, arrForIndex1);

		}

		DatatypeSort tupSortSQ = (DatatypeSort) tupleSQ.getSort();
		FuncDecl[] tupAccessorsSQ = tupSortSQ.getAccessors()[0]; // tuples declared will have only one constructor,
																	// hence[0].

		// Indexing tuple accessor will give you value of particular column after
		// applying on tuple
		FuncDecl colAccessorSQ1 = tupAccessorsSQ[colIndex];

		Expr colValueSQ1 = colAccessorSQ1.apply(tupleSQ);
		return colValueSQ1;

	}

	/**
	 * This method gets the SMT / CVC constraint String mapping the table name,
	 * column name, index, position.
	 * 
	 * @param cvc
	 * @param col
	 * @param n
	 * @return
	 */
	public static String getSolverMapping(Column col, Node n) {
		if (isCVC3) {
			return cvcMap(col, n);
		} else {
			return smtMap(col, n);
		}
	}

	/**
	 * This method gets the SMT/CVC constraint string mapping for table name, column
	 * name, index, position.
	 * 
	 * @param cvc
	 * @param col
	 * @param index
	 * @return
	 */
	public static String getSolverMapping(Column col, String index) {
		if (isCVC3) {
			return cvcMap(col, index);
		} else {
			return smtMap(col, (IntExpr) ctx.mkInt(Integer.parseInt(index))).toString();
		}
	}

	/**
	 * Used to get SMT constraint for the given node for the given tuple position
	 * 
	 * @param n
	 * @param index
	 * @return
	 */
	public static String smtMapNode(Node n, String index) {

		if (n.getType().equalsIgnoreCase(Node.getValType())) {
			return n.getStrConst();
		} else if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
			if (Configuration.isEnumInt.equalsIgnoreCase("true"))
				return smtMap(n.getColumn(), ctx.mkConst(enumIndexVar + index, currentSort)).toString();
			else
				return smtMap(n.getColumn(), (IntExpr) ctx.mkInt(Integer.parseInt(index))).toString();

		} else if (n.getType().toString().equalsIgnoreCase("i")) {
			return "i";
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())) {

			return "(" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
					+ smtMapNode(n.getLeft(), index) + " " + smtMapNode(n.getRight(), index)
					+ (n.getOperator().equals("/=") ? ")" : "") + ")";
		} else
			return "";
	}

	public static String smtMapNode(Node n, String index1, String index2) {

		if (n.getType().equalsIgnoreCase(Node.getValType())) {
			return n.getStrConst();
		} else if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
			if (Configuration.isEnumInt.equalsIgnoreCase("true"))
				return smtMap(n.getColumn(), ctx.mkConst(enumIndexVar + index1, currentSort)).toString();
			else
				return smtMap(n.getColumn(), (IntExpr) ctx.mkInt(Integer.parseInt(index1))).toString();

		} else if (n.getType().toString().equalsIgnoreCase("i")) {
			return "i";
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())) {

			return "(" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
					+ smtMapNode(n.getLeft(), index1) + " " + smtMapNode(n.getRight(), index2)
					+ (n.getOperator().equals("/=") ? ")" : "") + ")";
		} else
			return "";
	}

	/**
	 * This method returns the constraint for the node and index position based on
	 * solver
	 * 
	 * @param cvc
	 * @param n
	 * @param index
	 * @return
	 */
	public static String getMapNode(Node n, String index) {
		if (isCVC3) {
			if (cvcMapNode(n, index) != null && !cvcMapNode(n, index).isEmpty()) {
				return cvcMapNode(n, index);
			} else
				return "";
		} else {
			if (smtMapNode(n, index) != null && !smtMapNode(n, index).isEmpty()) {
				return smtMapNode(n, index);
			} else {
				return "";
			}
		}
	}

	// Specific methods for each datatype in solver file
	/**
	 * This method returns the header value for the Solver depending on whether
	 * solver is CVC / SMT
	 * If solver is CVC - it returns nothing
	 * If solver is SMT - it returns all options required for producing required
	 * output.
	 * 
	 * @param cvc
	 * @return
	 */
	public String getHeader() {
		String header = "";
		if (isCVC3) {
			return header;
		} else {
			Params p = ctx.mkParams();
			p.add("produce-models", true); // other options invalid in z3 API
			p.add("smt.macro_finder", true); // check whether this is the right option name
			// p.add("model_compress", false); <-- Gives error for some reason, however this
			// param is set successfully in the .smt file
			solver.setParameters(p); // NOTE: these should be the settings for all solvers

			// header = "(set-logic ALL_SUPPORTED)";
			header += "(set-option :produce-models true)\n (set-option :smt.macro_finder true) \n (set-option :smt.mbqi true) \n";

			BoolExpr[] assertionIntNull = new BoolExpr[2];

			assertionIntNull[0] = ctx.mkEq(ConstraintGenerator.intNull, ctx.mkInt(-99999));
			// BoolExpr assertionRealNull = ctx.mkEq(ConstraintGenerator.realNull,
			// ctx.mkReal("-99996.0"));
			assertionIntNull[1] = ctx.mkEq(ConstraintGenerator.realNull, ctx.mkReal("-99999.0"));

			header += "\n(declare-fun " + ConstraintGenerator.intNull + " () Int)\n"; // function defination using
																						// "define-fun"
			header += "(assert " + assertionIntNull[0].toString() + ")\n";
			header += "\n(declare-fun " + ConstraintGenerator.realNull + " () Real)\n";
			header += "(assert " + assertionIntNull[1].toString() + ")\n";

		}
		return header + "\n\n";
	}

	/**
	 * This method returns the Constraint String for defining and declaring integer
	 * type data
	 * 
	 * @param cvc
	 * @param col
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	// public String getIntegerDatatypes(Column col, int minVal, int maxVal){
	//
	// String constraint ="";
	// if (isCVC3) {
	// constraint = "\n"+col+" : TYPE = SUBTYPE (LAMBDA (x: INT) : (x >
	// "+(minVal-4)+" AND x < "+(maxVal+4)+") OR (x > -100000 AND x < -99995));\n";
	// }
	// else {
	// String funcName = "get"+col;
	// FuncDecl getCol = ctx.mkFuncDecl(funcName, ctx.mkIntSort(),
	// ctx.mkBoolSort());
	// ctxFuncDecls.put(funcName, getCol);
	// constraint = getCol.toString() + "\n";
	// IntExpr[] iColArray = new IntExpr[]{ctx.mkIntConst("i_"+col)};
	// Expr getColCall = getCol.apply(iColArray);
	// //Expr condition = ctx.mkAnd(ctx.mkGt(iColArray[0],
	// ctx.mkInt((minVal-4)>0?(minVal-4):0)), ctx.mkLt(iColArray[0],
	// ctx.mkInt(maxVal+4)));
	// //Expr condition = ctx.mkAnd(ctx.mkGt(iColArray[0],
	// ctx.mkInt((minVal-4)>0?(minVal-4):0)), ctx.mkLt(iColArray[0],
	// ctx.mkInt(maxVal == 2147483647 ? maxVal : maxVal+4)));
	// Expr condition = ctx.mkAnd(ctx.mkGt(iColArray[0],
	// ctx.mkInt((minVal-4)>0?(minVal-4):0)), ctx.mkLt(iColArray[0],
	// ctx.mkInt("9223372036854775807")));
	//
	// Expr body = ctx.mkEq(getColCall, condition);
	// Expr funcQuantifier = ctx.mkForall(iColArray, body, 1, null, null, null,
	// null);
	// constraint += "(assert " + funcQuantifier.toString() + ")";
	// }
	// return constraint +"\n\n";
	// }
	/**
	 * This method returns the Constraint String for defining and declaring int,
	 * long and BigInt type data
	 * 
	 * @param cvc
	 * @param col
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public String getIntegerDatatypes(Column col, long minVal, long maxVal) {

		String constraint = "";
		if (isCVC3) {
			constraint = "\n" + col + " : TYPE = SUBTYPE (LAMBDA (x: INT) : (x > " + (minVal - 4) + " AND x < "
					+ (maxVal + 4) + ") OR (x > -100000 AND x < -99995));\n";
		} else {
			String funcName = "check" + col;
			FuncDecl getCol = ctx.mkFuncDecl(funcName, ctx.mkIntSort(), ctx.mkBoolSort());
			ctxFuncDecls.put(funcName, getCol);
			constraint = getCol.toString() + "\n";
			IntExpr[] iColArray = new IntExpr[] { ctx.mkIntConst("i_" + col) };
			Expr getColCall = getCol.apply(iColArray);

			minVal = minVal > Long.MIN_VALUE ? minVal - 1 : minVal; // TEST CODE: Pooja
			maxVal = maxVal < Long.MAX_VALUE ? maxVal + 1 : maxVal;

			// Expr condition = ctx.mkAnd(ctx.mkGt(iColArray[0], ctx.mkInt(minVal)),
			// ctx.mkLt(iColArray[0], ctx.mkInt(maxVal)));

			// Testing.....
			Expr condition = ctx.mkOr(
					ctx.mkAnd(ctx.mkGt(iColArray[0], ctx.mkInt(minVal)), ctx.mkLt(iColArray[0], ctx.mkInt(maxVal))),
					ctx.mkEq(iColArray[0], ctx.mkInt(-99999)));

			Expr body = ctx.mkEq(getColCall, condition);
			Expr funcQuantifier = ctx.mkForall(iColArray, body, 1, null, null, null, null);
			constraint += "(assert " + funcQuantifier.toString() + ")";
		}
		return constraint + "\n\n";
	}

	/**
	 * This method returns a constraint string that holds the allowed null values
	 * for the integer data defined
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getIntegerNullDataValues(Column col) {
		String constraint = "";
		String isNullMembers = "";
		if (isCVC3) {

			constraint = "ISNULL_" + col + " : " + col + " -> BOOLEAN;\n";
			for (int k = -99996; k >= -99999; k--) {
				isNullMembers += "ASSERT ISNULL_" + col + "(" + k + ");\n";
			}
			constraint += isNullMembers;
		} else {
			HashMap<Expr, Integer> nullValuesInt = new HashMap<Expr, Integer>();
			/* Removing NUll enumerations */
			/*
			 * for(int k=-99996;k>=-99999;k--){
			 * nullValuesInt.put(k+"",0);
			 * }
			 */
			nullValuesInt.put(ConstraintGenerator.intNull, 0);
			constraint += defineIsNull(nullValuesInt, col);
		}
		return constraint + "\n\n";
	}

	/**
	 * This method returns the Constraint String for defining and declaring Real
	 * type data
	 * 
	 * @param cvc
	 * @param col
	 * @param minVal
	 * @param maxVal
	 * @return
	 */
	public String getRealDatatypes(Column col, double minVal, double maxVal) {
		String constraint = "";
		if (isCVC3) {
			String maxStr = util.Utilities.covertDecimalToFraction(maxVal + "");
			String minStr = util.Utilities.covertDecimalToFraction(minVal + "");
			constraint = "\n" + col + " : TYPE = SUBTYPE (LAMBDA (x: REAL) : (x >= " + (minStr) + " AND x <= "
					+ (maxStr) + ") OR (x > -100000 AND x < -99995));\n";
		} else {
			String funcName = "check" + col;
			FuncDecl getCol = ctx.mkFuncDecl(funcName, ctx.mkRealSort(), ctx.mkBoolSort());
			ctxFuncDecls.put(funcName, getCol);
			constraint = getCol.toString() + "\n";
			RealExpr[] rColArray = new RealExpr[] { ctx.mkRealConst("r_" + col) };
			Expr getColCall = getCol.apply(rColArray);
			// Expr condition = ctx.mkAnd(ctx.mkGe(rColArray[0],
			// ctx.mkReal(String.valueOf(minVal))), ctx.mkLe(rColArray[0],
			// ctx.mkReal(String.valueOf(maxVal))));
			Expr condition = ctx.mkOr(
					ctx.mkAnd(ctx.mkGe(rColArray[0], ctx.mkReal(String.valueOf(minVal))),
							ctx.mkLe(rColArray[0], ctx.mkReal(String.valueOf(maxVal)))),
					ctx.mkEq(rColArray[0], ctx.mkReal(-99999)));

			Expr body = ctx.mkEq(getColCall, condition);
			Expr funcQuantifier = ctx.mkForall(rColArray, body, 1, null, null, null, null);

			constraint += "(assert " + funcQuantifier.toString() + ")";
		}
		return constraint + "\n\n";
	}

	/**
	 * This method returns a constraint string that holds the allowed null values
	 * for the Real data defined
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getRealNullDataValues(Column col) {
		String constraint = "";
		String isNullMembers = "";
		if (isCVC3) {

			constraint += "ISNULL_" + col + " : " + col + " -> BOOLEAN;\n";
			for (int k = -99996; k >= -99999; k--) {
				isNullMembers += "ASSERT ISNULL_" + col + "(" + k + ");\n";

			}
			constraint += isNullMembers;
		} else {
			HashMap<Expr, Integer> nullValuesReal = new HashMap<Expr, Integer>();
			/* Removing NUll enumerations */
			/*
			 * for(int k=-99996;k>=-99999;k--){
			 * nullValuesInt.put(k+"",0);
			 * }
			 */
			nullValuesReal.put(ConstraintGenerator.realNull, 0);
			constraint += defineIsNull(nullValuesReal, col);
		}
		return constraint + "\n\n";
	}

	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Null
	 * values for given column
	 * 
	 * @param colValueMap
	 * @param col
	 * @return
	 */

	public static String defineIsNull(HashMap<Expr, Integer> colValueMap, Column col) {

		String IsNullValueString = "";
		Expr colNull = null;
		if (col.getCvcDatatype() != null && col.getCvcDatatype().equalsIgnoreCase("Int")) {
			colNull = ctx.mkConst(col.getColumnName().toLowerCase(), ctx.getIntSort());
		} else if (col.getCvcDatatype() != null && col.getCvcDatatype().equalsIgnoreCase("Real")) {
			colNull = ctx.mkConst(col.getColumnName().toLowerCase(), ctx.getRealSort());
		} else {
			colNull = ctx.mkConst(col.getColumnName().toLowerCase(), ctxSorts.get(col.getColumnName()));
		}

		String funcName = "ISNULL_" + col;
		FuncDecl isNullCol = ctx.mkFuncDecl(funcName, colNull.getSort(), ctx.mkBoolSort());
		ctxFuncDecls.put(funcName, isNullCol);
		IsNullValueString += isNullCol.toString() + "\n";

		Expr[] nullColArray = new Expr[] { colNull };
		Expr isNullColCall = isNullCol.apply(nullColArray);

		BoolExpr[] nullEqualityConds = colValueMap.keySet().stream().map(
				colValue -> ctx.mkEq(nullColArray[0], colValue)).toArray(
						size -> new BoolExpr[size]);
		BoolExpr nullValsOrCond = ctx.mkOr(nullEqualityConds);

		Expr body = ctx.mkEq(isNullColCall, nullValsOrCond);
		Expr funcQuantifier = ctx.mkForall(nullColArray, body, 1, null, null, null, null);
		IsNullValueString += "(assert " + funcQuantifier.toString() + ")\n";

		return IsNullValueString + "\n\n";
	}

	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Not-Null
	 * values for given column
	 * 
	 * @param colValueMap
	 * @param col
	 * @return
	 */
	public static String defineNotIsNull(HashMap<String, Integer> colValueMap, Column col) {
		String NotIsNullValueString = "";

		if (col.getCvcDatatype() != null && col.getCvcDatatype().equalsIgnoreCase("Int")) {
			NotIsNullValueString += "\n(declare-const notnull_" + col + " i_" + col + ")"; // declare constant of form
																							// (declare-const
																							// null_column_name
																							// ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_" + col + " ((notnull_" + col + " i_" + col + ")) Bool ";
		} else if (col.getCvcDatatype() != null && col.getCvcDatatype().equalsIgnoreCase("Real")) {
			NotIsNullValueString += "\n(declare-const notnull_" + col + " r_" + col + ")"; // declare constant of form
																							// (declare-const
																							// null_column_name
																							// ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_" + col + " ((notnull_" + col + " r_" + col + ")) Bool ";
		} else {
			NotIsNullValueString += "\n(declare-const notnull_" + col + " " + col + ")"; // declare constant of form
																							// (declare-const
																							// null_column_name
																							// ColumnName)
			NotIsNullValueString += "\n(define-fun NOTISNULL_" + col + " ((notnull_" + col + " " + col + ")) Bool ";
		}

		NotIsNullValueString += getOrForNullDataTypes("notnull_" + col, colValueMap.keySet(), "");
		;// Get OR of all non-null columns

		NotIsNullValueString += ")";
		return NotIsNullValueString + "\n";
	}

	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Null
	 * values for a column concatenated with OR
	 * 
	 * @param colconst
	 * @param columnValues
	 * @param tempString
	 * @return
	 */

	public static String getOrForNullDataTypes(String colconst, Set<String> columnValues, String tempString) {

		Iterator it = columnValues.iterator();
		int index = 0;
		while (it.hasNext()) {
			index++;
			tempString = getIsNullOrString(colconst, ((String) it.next()), tempString);
		}
		return tempString;
	}

	/**
	 * This method returns the <b>SMT Constraint</b> that holds the allowed Not-Null
	 * values for a column concatenated with OR
	 * 
	 * @param colconst
	 * @param colValue
	 * @param tempstring
	 * @return
	 */
	public static String getIsNullOrString(String colconst, String colValue, String tempstring) {

		String tStr = "";

		if (tempstring != null && !tempstring.isEmpty()) {
			tStr = "(or " + tempstring;
		}
		if (colValue != null && colValue.startsWith("-")) {
			tStr += " (= " + colconst + " -" + colValue.substring(1, colValue.length()) + ")";
		} else {
			tStr += " (= " + colconst + " " + colValue + ")";
		}

		if (tempstring != null && !tempstring.isEmpty()) {
			tStr += ")";
		}

		return tStr;
	}

	/**
	 * This method returns the Constraint String for defining and declaring String /
	 * VARCHAR type data
	 * 
	 * @param cvc
	 * @param columnValue
	 * @param col
	 * @param unique
	 * @return
	 * @throws Exception
	 */
	public String getStringDataTypes(Vector<String> columnValue, Column col, boolean unique) throws Exception {
		String constraint = "";
		String colValue = "";
		HashSet<String> uniqueValues = new HashSet<String>();
		String isNullMembers = "";
		checkAndRemoveDuplicateColumns(col);
		if (isCVC3) {
			// If CVC Solver
			constraint = "\nDATATYPE \n" + col + " = ";
			if (columnValue.size() > 0) {
				if (!unique || !uniqueValues.contains(columnValue.get(0))) {
					colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
							+ Utilities.escapeCharacters(columnValue.get(0));// .trim());
					constraint += "_" + colValue;
					isNullMembers += "ASSERT NOT ISNULL_" + col + "(_" + colValue + ");\n";
					uniqueValues.add(columnValue.get(0));
				}
				colValue = "";
				for (int j = 1; j < columnValue.size() || j < 4; j++) {
					if (j < columnValue.size()) {
						if (!unique || !uniqueValues.contains(columnValue.get(j))) {
							colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
									+ Utilities.escapeCharacters(columnValue.get(j));
						}
					} else {
						if (!uniqueValues.contains(((Integer) j).toString())) {
							colValue = Utilities.escapeCharacters(col.getColumnName()) + "__" + j;
						} else {
							continue;
						}
					}
					if (!colValue.isEmpty()) {
						constraint = constraint + " | " + "_" + colValue;
						isNullMembers += "ASSERT NOT ISNULL_" + col + "(_" + colValue + ");\n";
					}
				}
			}
			// Adding support for NULLs
			if (columnValue.size() != 0) {
				constraint += " | ";
			}
			for (int k = 1; k <= 4; k++) {
				constraint += "NULL_" + col + "_" + k;
				if (k < 4) {
					constraint += " | ";
				}
			}
			constraint = constraint + " END\n;";
			constraint += "ISNULL_" + col + " : " + col + " -> BOOLEAN;\n";
			HashMap<String, Integer> nullValuesChar = new HashMap<String, Integer>();
			for (int k = 1; k <= 4; k++) {
				isNullMembers += "ASSERT ISNULL_" + col + "(NULL_" + col + "_" + k + ");\n";
				nullValuesChar.put("NULL_" + col + "_" + k, 0);
			}
			constraint += isNullMembers;

		} else { // if another SMT SOLVER
			HashMap<Expr, Integer> nullValuesChar = new HashMap<Expr, Integer>();
			HashMap<Expr, Integer> notnullValuesChar = new HashMap<Expr, Integer>();
			Vector<String> colValues = new Vector<String>();

			if (columnValue.size() > 0) {
				if (!unique || !uniqueValues.contains(columnValue.get(0))) {
					colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
							+ Utilities.escapeCharacters(columnValue.get(0));// .trim());
					colValues.add("_" + colValue);
					uniqueValues.add(columnValue.get(0));
				}
				colValue = "";
				for (int j = 1; j < columnValue.size() || j < 4; j++) {
					colValue = "";
					if (j < columnValue.size()) {
						if (!unique || !uniqueValues.contains(columnValue.get(j))) {
							if (columnValue.get(j) != null) {
								colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
										+ Utilities.escapeCharacters(columnValue.get(j));
								uniqueValues.add(columnValue.get(j));
							}

						}
					} else {
						if (!uniqueValues.contains(((Integer) j).toString())) {
							colValue = Utilities.escapeCharacters(col.getColumnName()) + "__" + j;
						} else {
							continue;
						}
					}

					if (!colValue.isEmpty()) {
						colValues.add("_" + colValue);
					}
					// System.out.println(colValue);
				}

			}
			for (int j = 0; j < col.getCheckValues().size() ; j++) {
				colValue = "";
				if (j < col.getCheckValues().size()) {
					if (!uniqueValues.contains(col.getCheckValues().get(j))) {
						if (col.getCheckValues().get(j) != null) {
							colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
									+ Utilities.escapeCharacters(col.getCheckValues().get(j));
							uniqueValues.add(col.getCheckValues().get(j));
						}

					}
				}

				if (!colValue.isEmpty()) {
					colValues.add("_" + colValue);
				}
				// System.out.println(colValue);
			}

			String nullVal = "NULL_" + col + "_1";
			colValues.add(nullVal);

			EnumSort colSort = ctx.mkEnumSort(col.getColumnName(), colValues.toArray(new String[colValues.size()]));

			for (int i = 0; i < colSort.getConsts().length - 1; i++) { // all but last value, which is for null
				notnullValuesChar.put(colSort.getConsts()[i], 0);
			}

			nullValuesChar.put(colSort.getConst(colSort.getConsts().length - 1), 0); // put the null one in
																						// nullValuesChar

			ctxSorts.put(col.getColumnName(), colSort);
			solver = ctx.mkSolver();
			solver.push();

			// as of this writing, the API doesn't serialize unused declarations, therefore
			// dummy assertions are used
			Expr dummyVal = ctx.mkConst("dummy", colSort);
			BoolExpr dummyAssert = ctx.mkDistinct(dummyVal);
			solver.add(dummyAssert);
			String z3APIString = solver.toString();
			solver.pop(1); // pop out dummyVal and dummyAssert
			constraint = z3APIString.substring(0, z3APIString.indexOf("(declare-fun")) + "\n";

			// added for enum sort comparison
			Collections.sort(colValues);

			constraint += GetSolverHeaderAndFooter.generateCustomComparisonFunctionForEnumTypes(col.getColumnName(),
					colValues, "map");

			constraint += GetSolverHeaderAndFooter.generateCustomComparisonFunctionForEnumTypes(col.getColumnName(),
					colValues, "gt");
			Collections.sort(colValues, Collections.reverseOrder());

			constraint += GetSolverHeaderAndFooter.generateCustomComparisonFunctionForEnumTypes(col.getColumnName(),
					colValues, "lt");

			constraint += defineIsNull(nullValuesChar, col) + "\n";

			// string check constraints - sunanda

			if (col.getCheckValues().size() > 0) {
				String funcName = "check" + col;
				FuncDecl getCol = ctx.mkFuncDecl(funcName, ctxSorts.get(col.getColumnName()), ctx.mkBoolSort());
				ctxFuncDecls.put(funcName, getCol);
				String constrainttemp = getCol.toString() + "\n";
				// ctx.mkConst("r_"+col, ctxSorts.get(col.getColumnName()));

				Expr[] rColArray = new Expr[] { ctx.mkConst("r_" + col, ctxSorts.get(col.getColumnName())) };
				Expr getColCall = getCol.apply(rColArray);
				Expr[] orArray = new Expr[col.getCheckValues().size()];
				int i = 0;
				for (String s : col.getCheckValues()) {
					colValue = Utilities.escapeCharacters(col.getColumnName()) + "__"
							+ Utilities.escapeCharacters(s);
					orArray[i] = ctx.mkEq(rColArray[0], ctx.mkConst("_" + colValue, ctxSorts.get(col.getColumnName())));
					i++;
				}
				Expr condition = ctx.mkOr(orArray);

				Expr body = ctx.mkEq(getColCall, condition);
				Expr funcQuantifier = ctx.mkForall(rColArray, body, 1, null, null, null, null);

				constrainttemp += "\n(assert " + funcQuantifier + ")\n\n";
				constraint += constrainttemp ;
			}

		}
		return constraint;
	}

	/**
	 * TEMPCODE Rahul Sharma
	 * TO check and remove duplicate entries from the column values
	 * 
	 * @param col : A column of a table
	 */

	private void checkAndRemoveDuplicateColumns(Column col) {
		Vector<String> columnValues = col.getColumnValues();
		Vector<String> uniqueColumnValues = new Vector<String>();
		uniqueColumnValues.addAll(columnValues);
		// columnValues.clear();
		// columnValues.addAll(uniqueColumnValues);
		col.getColumnValues().clear();
		for (String s : uniqueColumnValues) {
			if (!col.getColumnValues().contains(s))
				col.addColumnValues(s);
		}

	}

	/**
	 * This method returns the null integer values for CVC data type.
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	public String getNullMembers(Column col) {
		String isNullMembers = "";

		if (isCVC3) {
			for (int k = -99996; k >= -99999; k--) {
				isNullMembers += "ASSERT ISNULL_" + col + "(" + k + ");\n";
			}
		} else {
			isNullMembers = "";
		}
		return isNullMembers;
	}

	/**
	 * This method returns the Footer Constraints based on the solver
	 * 
	 * @param cvc
	 * @return
	 */
	public String getFooter(GenerateCVC1 cvc) {

		String temp = "";
		if (isCVC3) {
			temp += "\n\nQUERY FALSE;"; // need to right generalize one
			temp += "\nCOUNTERMODEL;";
		} else {
			temp += "\n\n(check-sat)"; // need to right generalize one
			// for(Table t : cvc.getResultsetTables()){
			// temp+= "\n (get-value (O_"+t.getTableName()+"))";
			// }

			temp += "\n(get-model)";

		}
		return temp;
	}

	/**
	 * This method returns the Footer Constraints based on the solver
	 * 
	 * @param cvc
	 * @return
	 */
	public String getFooterForAgg(GenerateCVC1 cvc) {

		String temp = "";
		if (isCVC3) {
			temp += "\n\nQUERY FALSE;"; // need to right generalize one
			temp += "\nCOUNTERMODEL;";
		} else {
			temp += "\n\n(check-sat)\n"; // need to right generalize one

		}
		return temp;
	}

	/**
	 * This method returns the constraint String that holds the Tuple Types based on
	 * solver
	 * 
	 * @param cvc
	 * @param col
	 * @return
	 */
	// saurabh code begins

	public static String getTupleTypesForSolver(GenerateCVC1 cvc) {

		String tempStr = "";
		Table t;
		String temp;
		Vector<String> tablesAdded = new Vector<String>();
		tempStr += addCommentLine(" Tuple Types for Relations\n ");
		// System.out.println("here in tupletype");

		if (cvc.getConstraintSolver().equalsIgnoreCase("cvc3")) {
			Column c;

			tempStr += ConstraintGenerator.addCommentLine(" Tuple Types for Relations\n ");
			for (int i = 0; i < cvc.getResultsetTables().size(); i++) {
				t = cvc.getResultsetTables().get(i);
				temp = t.getTableName();
				if (!tablesAdded.contains(temp)) {
					tempStr += temp + "_TupleType: TYPE = [";
				}
				for (int j = 0; j < cvc.getResultsetColumns().size(); j++) {
					c = cvc.getResultsetColumns().get(j);
					if (c.getTableName().equalsIgnoreCase(temp)) {
						String s = c.getCvcDatatype();
						if (s != null && (s.equalsIgnoreCase("INT") || s.equalsIgnoreCase("REAL")
								|| s.equalsIgnoreCase("TIME") || s.equalsIgnoreCase("DATE")
								|| s.equalsIgnoreCase("TIMESTAMP")))
							tempStr += c.getColumnName() + ", ";
						else
							tempStr += c.getCvcDatatype() + ", ";
					}
				}
				tempStr = tempStr.substring(0, tempStr.length() - 2);
				tempStr += "];\n";
				/*
				 * Now create the Array for this TupleType
				 */
				tempStr += "O_" + temp + ": ARRAY INT OF " + temp + "_TupleType;\n";
			}
		} else {

			Solver dummySol = ctx.mkSolver(); // for getting string form of z3 context declarations

			String[] tablenames = new String[cvc.getResultsetTables().size()];
			for (int i = 0; i < cvc.getResultsetTables().size(); i++) {
				tablenames[i] = cvc.getResultsetTables().get(i).getTableName();
			}
			// added by deeksha
			if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
				usingCnt = true;
			} else {
				usingCnt = false;
			}
			// end
			for (int i = 0; i < cvc.getResultsetTables().size(); i++) {
				int index = 0;
				t = cvc.getResultsetTables().get(i);
				temp = t.getTableName();
				// added by deeksha
				Boolean cntFlag = false;
				String[] attrNames = new String[t.getNoOfColumn()];
				Sort[] attrTypes = new Sort[t.getNoOfColumn()];

				checkResultSetColumns(cvc, cvc.getResultsetColumns()); // TEMPCODE Rahul Sharma // added to check if
																		// the resultsetcolumns contains duplicate
																		// columns

				for (Column c : cvc.getResultsetColumns()) {
					if (c.getTableName().equalsIgnoreCase(temp)) {
						String s = c.getCvcDatatype();
						if (s != null && (s.equalsIgnoreCase("Int") || s.equals("TIME") || s.equals("DATE")
								|| s.equals("TIMESTAMP"))) { // TODO: check datetime types
							attrTypes[index] = ctx.getIntSort();
						} else if (s != null && (s.equalsIgnoreCase("Real"))) {
							attrTypes[index] = ctx.getRealSort();
						} else {
							attrTypes[index] = ctxSorts.get(s);
						}

						attrNames[index] = temp + "_" + c + index;
						index++;
					}
				}

				String tupleTypeName = temp + "_TupleType";
				Constructor[] cons = new Constructor[] {
						ctx.mkConstructor(tupleTypeName, "is_" + tupleTypeName, attrNames, attrTypes, null) };
				// API USAGE
				DatatypeSort tupleType = ctx.mkDatatypeSort(tupleTypeName, cons);
				ctxSorts.put(tupleTypeName, tupleType);
				ArraySort asort = ctx.mkArraySort(ctx.getIntSort(), tupleType);

				if (Configuration.isEnumInt.equalsIgnoreCase("true") && ctxSorts.containsKey(cvc.enumArrayIndex)) {
					asort = ctx.mkArraySort(getColumnSort(cvc.enumArrayIndex), tupleType);
				}
				String arrName = "O_" + temp;
				Expr aex = ctx.mkConst(arrName, asort);
				ctxSorts.put(arrName, asort);
				// System.out.println("heyyy");
				ctxConsts.put(arrName, aex);
				// adding dummy asserts so that solver has relevant declarations in string
				// returned by toString()
				BoolExpr dummyAssert = ctx.mkDistinct(aex);
				dummySol.add(dummyAssert);
				// }

				// testcode end----------------------------------------------

			}

			// Temporary procedure to extract relevant declarations from the solver string
			String[] z3Statements = dummySol.toString().split("\n");
			Vector<String> includedStatements = new Vector<String>();

			for (String statement : z3Statements) {
				if (statement.contains("_TupleType ") || statement.contains("declare-fun O_")) {
					includedStatements.add(statement);
				}
				if (statement.contains("(assert")) {
					break;
				}
			}
			tempStr += String.join("\n\n", includedStatements);

		}
		// System.out.println(tempStr);

		return tempStr;
	}

	/**
	 * TEMPCODE Rahul Sharma : to check if there is any duplicate columns in
	 * resultsetcolumns
	 * 
	 * @param cvc
	 * @param resultsetColumns
	 */
	private static void checkResultSetColumns(GenerateCVC1 cvc, Vector<Column> resultsetColumns) {
		LinkedHashSet<Column> hashSet = new LinkedHashSet<Column>(resultsetColumns);
		resultsetColumns.clear();
		resultsetColumns.addAll(hashSet);
		cvc.setResultsetColumns(resultsetColumns);
	}

	public static String getAssertNotCondition(QueryBlockDetails queryBlock, Node n, int index) {

		String subQueryConstraints = "";// "ASSERT NOT ";

		if (isCVC3) {
			subQueryConstraints += "ASSERT NOT " + genPositiveCondsForPred(queryBlock, n, index);
		} else {
			subQueryConstraints += "(assert (not " + genPositiveCondsForPred(queryBlock, n, index);
		}

		if (subQueryConstraints.endsWith("ASSERT NOT ("))
			subQueryConstraints += "(1 /= 1) ";
		else if (subQueryConstraints.endsWith("(assert (not "))
			subQueryConstraints += "(/= 1 1) ";

		if (isCVC3) {
			subQueryConstraints += ";\n";
		} else {
			subQueryConstraints += "))\n";
		}

		return subQueryConstraints;
	}

	/**
	 * 
	 * @param constr1
	 * @param operator
	 * @param constr2
	 * @return
	 */
	public static String getAssertNotCondition(String constr1, String operator, String constr2) {

		String subQueryConstraints = "";// "ASSERT NOT ";

		if (isCVC3) {
			subQueryConstraints += "ASSERT NOT (" + constr1 + " " + operator + " " + constr2;
		} else {

			subQueryConstraints += "(assert (not " + (operator.equals("/=") ? "not (= " : operator) + " " + constr1
					+ " " + constr2 + (operator.equals("/=") ? ")" : "");
		}

		if (subQueryConstraints.endsWith("ASSERT NOT ("))
			subQueryConstraints += "(1 /= 1) ";
		else if (subQueryConstraints.endsWith("(assert (not "))
			subQueryConstraints += "(/= 1 1) ";

		if (isCVC3) {
			subQueryConstraints += "; \n";
		} else {
			subQueryConstraints += "))\n";
		}

		return subQueryConstraints;
	}

	public static String getConstraintsForValidCount(GenerateCVC1 cvc, String tableName, int tupleIndex,
			int countValue) {
		Column cntCol = cvc.getTableMap().getTable(tableName.toUpperCase())
				.getColumn(cvc.getTableMap().getTable(tableName.toUpperCase()).getNoOfColumn() - 1);
		Expr tempExpr = (IntExpr) ctx.mkInt(tupleIndex);
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
			tempExpr = ctx.mkConst(cvc.enumIndexVar + Integer.toString(tupleIndex), currentSort);
		}
		Expr andWithCount = (ArithExpr) smtMap(cntCol, tempExpr);

		return andWithCount.toString();
	}

	public static Expr getConstraintsForValidCountUsingTable(Table table, String tableName, int tupleIndex,
			int countValue) {
		Column cntCol = table.getColumn(table.getNoOfColumn() - 1);
		Expr andWithCount;

		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
			andWithCount = (ArithExpr) smtMap(cntCol, ctx.mkConst(enumIndexVar + tupleIndex, currentSort));
		} else {
			andWithCount = (ArithExpr) smtMap(cntCol, (IntExpr) ctx.mkInt(tupleIndex));
		}

		// return andWithCount;
		return ctx.mkGt(andWithCount, ConstraintGenerator.ctx.mkInt(countValue));
	}
	public static Expr getConstraintsForInValidCountUsingTable(Table table, String tableName, int tupleIndex,
			int countValue) {
		Column cntCol = table.getColumn(table.getNoOfColumn() - 1);
		Expr andWithCount;

		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
			andWithCount = (ArithExpr) smtMap(cntCol, ctx.mkConst(enumIndexVar + tupleIndex, currentSort));
		} else {
			andWithCount = (ArithExpr) smtMap(cntCol, (IntExpr) ctx.mkInt(tupleIndex));
		}

		// return andWithCount;
		return ctx.mkEq(andWithCount, ConstraintGenerator.ctx.mkInt(countValue));
	}

	/*
	 * Generate not null check constraints in assert statements
	 * 
	 */
	public static String genNULLCheckConstraints(Node n, int index) {
		/* Removing NUll enumerations */

		String constraint = "";
		Column col = n.getColumn();
		if (!isCVC3) {
			Table table = col.getTable();
			String type = col.getCvcDatatype();
			String tableName = col.getTableName();
			String columnName = col.getColumnName();
			int pos = table.getColumnIndex(columnName);
			String datatype = col.getCvcDatatype();

			String smtCond = "";
			String colName = tableName + "_" + columnName + pos;
			if (datatype != null && (datatype.equalsIgnoreCase("INT") || datatype.equalsIgnoreCase("REAL")
					|| datatype.equalsIgnoreCase("TIME") || datatype.equalsIgnoreCase("DATE")
					|| datatype.equalsIgnoreCase("TIMESTAMP")))
				constraint = "check" + n.getColumn().getColumnName() + "(" + colName + " " + "(select O_" + tableName
						+ " " + enumIndexVar + index + ") )";
			else
				constraint = "not (ISNULL_" + n.getColumn().getColumnName() + " (" + colName + " " + "(select O_"
						+ tableName + " " + enumIndexVar + index + ") ))";
		}

		return constraint;
	}

	/**
	 * Generate CVC3 constraints for the given node and its tuple position
	 * 
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @return
	 */

	// testcode by
	// sunanda-----------------------------------------------------------------------------------------------------------------------------------------------------------
	public static Expr putTableInCtx(GenerateCVC1 cvc, Vector<String> attrNames, Vector<Sort> attrTypes,
			String table_name) {
		String tupleTypeName = table_name + "_TupleType";
		Constructor[] cons = new Constructor[] {
				ctx.mkConstructor(tupleTypeName, "is_" + tupleTypeName, attrNames.toArray(new String[attrNames.size()]),
						attrTypes.toArray(new Sort[attrTypes.size()]), null) };
		// fatal error due to below statement
		DatatypeSort tupleType = ctx.mkDatatypeSort(tupleTypeName, cons);
		ctxSorts.put(tupleTypeName, tupleType);
		ArraySort asort = ctx.mkArraySort(ctx.getIntSort(), tupleType);
		if (Configuration.isEnumInt.equalsIgnoreCase("true") && ctxSorts.containsKey(cvc.enumArrayIndex)) {
			asort = ctx.mkArraySort(getColumnSort(cvc.enumArrayIndex), tupleType);
		}
		String arrName = "O_" + table_name;
		Expr aex = ctx.mkConst(arrName, asort);
		ctxSorts.put(arrName, asort);
		ctxConsts.put(arrName, aex);
		// ctxConsts.remove(arrName,aex);

		return aex;
	}

	public static Sort getColumnSort(String dataTypeOfColumn) {
		if (dataTypeOfColumn.equalsIgnoreCase("int") || dataTypeOfColumn.equalsIgnoreCase("date")) {
			return ctx.getIntSort();
		} else if (dataTypeOfColumn.equalsIgnoreCase("real")) {
			return ctx.getRealSort();
		} else {
			return ctxSorts.get(dataTypeOfColumn);
		}
	}
	// testcode
	// end---------------------------------------------------------------------------------------------

	// test code by deeksha
	// -------------------------------------------------------------------
	// for declaration of a table
	/**
	 * Function to generate constraints for declaration
	 * 
	 * @param aex : expression generated after putting table in context
	 * @return Vector of strings containing constraints
	 */
	public static Vector<String> declareRelation(Expr aex) {

		// Solver dummySol = ctx.mkSolver();
		BoolExpr dummyAssert = ctx.mkDistinct(aex);
		// System.out.println("check exception");
		solver.add(dummyAssert);
		// System.out.println("check exception");
		String[] constraints = solver.toString().split("\n");
		Vector<String> includedStatements = new Vector<String>();

		// constraints contain extra datatype declarartions so we will filter only those
		// required for us
		for (String statement : constraints) {
			if (statement.contains("_TupleType ") || statement.contains("declare-fun O_")) {
				includedStatements.add(statement + "\n");
			}
			if (statement.contains("(assert")) {
				break;
			}
		}
		return includedStatements;

	}
	// for selection expression of a tuple from a relation using api

	public static Expr genSelectTest(GenerateCVC1 cvc, String tableName, int colIndex, int index) {
		// Solver dummySol = ctx.mkSolver();

		Expr pos = (IntExpr) ctx.mkInt(index);
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
			pos = ctx.mkConst(cvc.enumIndexVar + Integer.toString(index), currentSort);
		}
		// check if subquerytable is in context or not
		if (ctxConsts.get("O_" + tableName) == null) {
			int noOfAttr = cvc.subqueryConstraintsMap.get("where").SQTableName.get(tableName).size();
			ArrayList<String> listOfAttr = cvc.subqueryConstraintsMap.get("from").SQColumns.get(tableName);
			Vector<String> attrNames = new Vector<String>();
			Vector<Sort> attrType = new Vector<Sort>();

			for (int i = 0; i < noOfAttr; i++) {
				attrNames.add(listOfAttr.get(i));
				Column column = cvc.getResultsetColumns().get(i + 1);
				attrType.add(getColumnSort(column.getCvcDatatype()));
			}
			Expr aex = putTableInCtx(cvc, attrNames, attrType, tableName);

			for (String statement : declareRelation(aex)) {
				// System.out.print(statement);
			}

			// System.out.printldn(declareRelation(aex));

			// declaration of function check
			String functionName = "GETAGGVAL";

			// FuncDecl getAggVal = ctx.mkFuncDecl(functionName , ctx.getIntSort() ,
			// ctx.mkBoolSort());
			Sort[] agg = new Sort[2];

			FuncDecl getAggVal = ctx.mkFuncDecl(functionName, attrType.toArray(new Sort[attrType.size()]),
					ctx.mkBoolSort());
			ctxFuncDecls.put(functionName, getAggVal);

			// System.out.println("heyyy" + getAggVal.toString());
			// -----------------------------------------------------
		}

		// constraints for selection expression
		ArrayExpr subqueryRelation = (ArrayExpr) ctxConsts.get("O_" + tableName);

		DatatypeExpr tuple = (DatatypeExpr) ctx.mkSelect(subqueryRelation, pos);

		DatatypeSort tupSort = (DatatypeSort) tuple.getSort();
		FuncDecl[] tupAccessors = tupSort.getAccessors()[0];
		FuncDecl colAccessor = tupAccessors[colIndex];
		Expr resultantExpr = colAccessor.apply(tuple);

		return resultantExpr;
	}

	/**
	 * 
	 * @param cvc
	 * @param tableName
	 * @param colIndex
	 * @param index
	 * @return Expr : It is selection expression which selects col of index tuple
	 *         from given tablename
	 */
	public static Expr genSelectTest2(GenerateCVC1 cvc, String tableName, int colIndex, String index) {
		// Solver dummySol = ctx.mkSolver();

		// if offset / tuple no is string
		Expr[] nullArr = new Expr[] { ctx.mkIntConst(String.valueOf(index)) }; // : this will work with string like i ,
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
			nullArr = new Expr[] { ctx.mkConst(String.valueOf(index), currentSort) };
		} // j
		if (!tableName.contains("O_"))
			tableName = "O_" + tableName;
		// check if subquerytable is in context or not
		if (ctxConsts.get(tableName) == null) {
			int noOfAttr = cvc.subqueryConstraintsMap.get("where").SQTableName.get(tableName).size();
			ArrayList<String> listOfAttr = cvc.subqueryConstraintsMap.get("where").SQColumns.get(tableName);
			Vector<String> attrNames = new Vector<String>();
			Vector<Sort> attrType = new Vector<Sort>();

			for (int i = 0; i < noOfAttr; i++) {
				attrNames.add(listOfAttr.get(i));
				Column column = cvc.getResultsetColumns().get(i + 1);
				attrType.add(getColumnSort(column.getCvcDatatype()));
			}
			Expr aex = putTableInCtx(cvc, attrNames, attrType, tableName);

			// call to declare relation in constraints
			for (String statement : declareRelation(aex)) {
				// System.out.print(statement);
			}
		}
		ArrayExpr subqueryRelation;
		// constraints for selection expression

		subqueryRelation = (ArrayExpr) ctxConsts.get(tableName);

		DatatypeExpr tuple = (DatatypeExpr) ctx.mkSelect(subqueryRelation, nullArr);

		DatatypeSort tupSort = (DatatypeSort) tuple.getSort();
		FuncDecl[] tupAccessors = tupSort.getAccessors()[0];
		FuncDecl colAccessor = tupAccessors[colIndex];
		Expr resultantExpr = colAccessor.apply(tuple);

		return resultantExpr;
	}

	/**
	 * This function returns string having constraint of type "equation"
	 * 
	 * @param cvc
	 * @param table1
	 * @param col1
	 * @param offset1
	 * @param table2
	 * @param col2
	 * @param offset2
	 * @return
	 */

	public static String genEquiCondTest(GenerateCVC1 cvc, String table1, int col1, int offset1, String table2,
			int col2, int offset2) {

		// Expr temp = genSelectTest2(cvc, table1 , col1 , "x!0" );
		Expr left = genSelectTest(cvc, table1, col1, offset1);
		Expr right = genSelectTest(cvc, table2, col2, offset2);

		// equation expression
		Expr eq = ctx.mkEq(left, right);

		// System.out.println(eq.toString());

		return eq.toString();
	}

	// end of testcode
	public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n, int index) {

		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			if (isCVC3) {
				return cvcMap(n.getColumn(), index + " ");
			} else {
				Expr temp = ctx.mkInt(index);
				if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
					EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
					temp = ctx.mkConst(enumIndexVar + Integer.toString(index), currentSort);
				}
				return smtMap(n.getColumn(), temp).toString();
			}
		} else if (n.getType().equalsIgnoreCase(Node.getValType())) {
			if (!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())
				|| n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType())
				|| n.getType().equalsIgnoreCase(Node.getOrNodeType())) {
			if (isCVC3) {

				return "( " + genPositiveCondsForPred(queryBlock, n.getLeft(), index) + " " + n.getOperator() + " " +
						genPositiveCondsForPred(queryBlock, n.getRight(), index) + ")";
			} else {
				/* Removing NUll enumerations */

				// end

				// Column cntCol =
				// cvc.getTableMap().getTable(tableName.toUpperCase()).getColumn(cvc.getTableMap().getTable(tableName.toUpperCase()).getNoOfColumn()-1);
				// // added by sunanda for count
				// if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
				//
				// if(n.getRight().getColumn() != null && n.getLeft() != null) {
				// Column cntCol1 =
				// n.getRight().getColumn().getTable().getColumn(n.getRight().getColumn().getTable().getNoOfColumn()-1);
				// BoolExpr WithCount1 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				//
				// Column cntCol2 =
				// n.getLeft().getColumn().getTable().getColumn(n.getLeft().getColumn().getTable().getNoOfColumn()-1);
				// BoolExpr WithCount2 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol2,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				////
				// return "(and ( "+ (n.getOperator().equals("/=")? "not (= ": n.getOperator())
				// + " " +genPositiveCondsForPred(queryBlock, n.getLeft(), index) +" "+
				// genPositiveCondsForPred(queryBlock, n.getRight(), index)
				// +(n.getOperator().equals("/=")? " )": "")+" ) "+WithCount1.toString() + " " +
				// WithCount2.toString() + " )";
				//
				// }
				//
				// else if(n.getRight().getColumn() == null ) {
				//// Column cntCol1 =
				// n.getRight().getColumn().getTable().getColumn(n.getRight().getColumn().getTable().getNoOfColumn()-1);
				//// BoolExpr WithCount1 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				////
				// Column cntCol2 =
				// n.getLeft().getColumn().getTable().getColumn(n.getLeft().getColumn().getTable().getNoOfColumn()-1);
				// BoolExpr WithCount2 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol2,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				////
				// return "(and ( "+ (n.getOperator().equals("/=")? "not (= ": n.getOperator())
				// + " " +genPositiveCondsForPred(queryBlock, n.getLeft(), index) +" "+
				// genPositiveCondsForPred(queryBlock, n.getRight(), index)
				// +(n.getOperator().equals("/=")? " )": "")+" ) "+ " " + WithCount2.toString()
				// + " )";
				//
				// }
				// else if(n.getLeft().getColumn() == null ) {
				// Column cntCol1 =
				// n.getRight().getColumn().getTable().getColumn(n.getRight().getColumn().getTable().getNoOfColumn()-1);
				// BoolExpr WithCount1 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol1,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				//
				//// Column cntCol2 =
				// n.getLeft().getColumn().getTable().getColumn(n.getRight().getColumn().getTable().getNoOfColumn()-1);
				//// BoolExpr WithCount2 =
				// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol2,
				// ConstraintGenerator.ctx.mkInt(index)), ConstraintGenerator.ctx.mkInt(0)); //
				// added by sunanda for count
				////
				// return "(and ( "+ (n.getOperator().equals("/=")? "not (= ": n.getOperator())
				// + " " +genPositiveCondsForPred(queryBlock, n.getLeft(), index) +" "+
				// genPositiveCondsForPred(queryBlock, n.getRight(), index)
				// +(n.getOperator().equals("/=")? " )": "")+" ) "+ " " + WithCount1.toString()
				// + " )";
				//
				// }

				// }
				return "( " + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
						+ genPositiveCondsForPred(queryBlock, n.getLeft(), index) + " " +
						genPositiveCondsForPred(queryBlock, n.getRight(), index)
						+ (n.getOperator().equals("/=") ? " )" : "") + " )";

				/*---------------
				if(col != null && col.getCvcDatatype() != null && (col.getCvcDatatype().equalsIgnoreCase("INT") || col.getCvcDatatype().equalsIgnoreCase("REAL") || col.getCvcDatatype().equalsIgnoreCase("TIME")
						||col.getCvcDatatype().equalsIgnoreCase("TIMESTAMP") || col.getCvcDatatype().equalsIgnoreCase("DATE")))
				{
					if(isCVC3){
						constraint = "\nASSERT NOT ISNULL_"+col.getColumnName()+"("+cvcMap(col, index+"")+");";
					}else{
						//	constraint += "\n (assert NOTISNULL_"+col.getColumnName()+" "+smtMap(col,Integer.toString(index))+")";
						constraint = "\n (assert (get"+col.getColumnName()+" "+smtMap(col,Integer.toString(index))+"))";
					}
				
				}
				else{
					if(isCVC3){
						constraint = "\nASSERT NOT ISNULL_"+col.getCvcDatatype()+"("+cvcMap(col, index+"")+");";
					}else{
						constraint = "\n (assert (NOTISNULL_"+col.getCvcDatatype()+" " +smtMap(col,Integer.toString(index))+"))";
					}
				}
				
				---------------*/

			}
		}
		return null;
	}

	public static String genPositiveCondsForPredF(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n,
			String index) {
		if (n != null) {
			if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
				if (isCVC3) {
					return cvcMap(n.getColumn(), index + " ");
				} else {
					// Expr tempExpr = ctx.mkIntConst(index);
					Expr tempExpr = ctx.mkInt(index);

					if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
						EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
						tempExpr = ctx.mkConst(index, currentSort);
					}
					return smtMap(n.getColumn(), tempExpr).toString();
				}
			} else if (n.getType().equalsIgnoreCase(Node.getValType())) {
				if (!n.getStrConst().contains("$")){
					if(n.getStrConst().contains("/")){
						String[] values = n.getStrConst().split("/");
						return "(/ " + values[0] + " " + values[1] + " )";
					}
					return n.getStrConst();
				}
					
				else
					return queryBlock.getParamMap().get(n.getStrConst());
			} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
					|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())
					|| n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
					n.getType().equalsIgnoreCase(Node.getAndNodeType())
					|| n.getType().equalsIgnoreCase(Node.getOrNodeType())) {
				if (isCVC3) {
					return "( " + genPositiveCondsForPredF(cvc, queryBlock, n.getLeft(), index) + " " + n.getOperator()
							+ " " +
							genPositiveCondsForPredF(cvc, queryBlock, n.getRight(), index) + ")";
				} else {

					return "( " + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
							+ genPositiveCondsForPredF(cvc, queryBlock, n.getLeft(), index) + " " +
							genPositiveCondsForPredF(cvc, queryBlock, n.getRight(), index)
							+ (n.getOperator().equals("/=") ? " )" : "") + " )";
				}
			} else if (n.getType().equalsIgnoreCase(Node.getAggrNodeType())) {
				// Expr tempExpr = ctx.mkIntConst(index);
				Expr tempExpr = ctx.mkInt(index);

				return smtMap(n.getColumn(), tempExpr).toString();

			}

		}
		return null;
	}

	/**
	 * Generate SMT/CVC constraints for the given node
	 * 
	 * @param queryBlock
	 * @param n
	 * @return
	 */

	public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n) {
		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			if (isCVC3) {
				return cvcMap(n.getColumn(), n);
			} else {
				return smtMap(n.getColumn(), n);
			}
		} else if (n.getType().equalsIgnoreCase(Node.getValType())) {

			if (!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())
				|| n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType())
				|| n.getType().equalsIgnoreCase(Node.getOrNodeType())) {

			if (isCVC3) {
				return "(" + genPositiveCondsForPred(queryBlock, n.getLeft()) + " " + n.getOperator() + " " +
						genPositiveCondsForPred(queryBlock, n.getRight()) + ")";
			} else {
				return "(" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
						+ genPositiveCondsForPred(queryBlock, n.getLeft()) + " " +
						genPositiveCondsForPred(queryBlock, n.getRight()) + (n.getOperator().equals("/=") ? ")" : "")
						+ ")";
			}

		}
		return null;
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @param paramId
	 * @return
	 */
	public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n, int index, String paramId) {// For
																													// parameters

		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		return constraintGenerator.genPositiveCondsForPred(queryBlock, n, index);
	}

	/**
	 * 
	 * @param cvc
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @param paramId
	 * @return
	 */
	public static String genPositiveCondsForPredWithAssert(QueryBlockDetails queryBlock, Node n, int index,
			String paramId) {// For parameters

		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		if (isCVC3) {
			return "ASSERT " + constraintGenerator.genPositiveCondsForPred(queryBlock, n, index) + ";\n";
		} else {
			return "(assert " + constraintGenerator.genPositiveCondsForPred(queryBlock, n, index) + ")\n";
		}
	}

	/**
	 * 
	 * @param queryBlock
	 * @param n
	 * @param hm
	 * @return
	 */
	public static String genPositiveCondsForPred(QueryBlockDetails queryBlock, Node n, Map<String, Character> hm) {
		Character index = null;
		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			if (hm.containsKey(n.getTable().getTableName())) {
				index = hm.get(n.getTable().getTableName());
			} else {
				Iterator it = hm.entrySet().iterator();
				index = 'i';
				while (it.hasNext()) {
					Map.Entry pairs = (Map.Entry) it.next();
					char temp = (Character) pairs.getValue();
					if (temp > index)
						index = temp;
				}
				index++;
				hm.put(n.getTable().getTableName(), index);
			}
			// return cvcMap(n.getColumn(), index+"");
			if (isCVC3) {
				return cvcMap(n.getColumn(), index + "");
			} else {
				return smtMap(n.getColumn(), ctx.mkIntConst(index + "")).toString();
			}
		} else if (n.getType().equalsIgnoreCase(Node.getValType())) {
			if (!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())
				|| n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType())
				|| n.getType().equalsIgnoreCase(Node.getOrNodeType())) {

			if (isCVC3) {
				return "(" + genPositiveCondsForPred(queryBlock, n.getLeft(), hm) + " " + n.getOperator() + " " +
						genPositiveCondsForPred(queryBlock, n.getRight(), hm) + ")";
			} else {

				// return "("+(n.getOperator().equals("/=")? "not (= ": n.getOperator())+ " "
				// +genPositiveCondsForPred(queryBlock, n.getLeft(),hm) +" "+
				// genPositiveCondsForPred(queryBlock, n.getRight(),hm)
				// +(n.getOperator().equals("/=")? ")": "")+")";

				return "(and " + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
						+ genPositiveCondsForPred(queryBlock, n.getLeft(), hm) + " " +
						genPositiveCondsForPred(queryBlock, n.getRight(), hm)
						+ (n.getOperator().equals("/=") ? ")" : "");
			}
		}
		return "";
	}

	/**
	 * Generates negative constraints for the given string selection node
	 * 
	 * @param queryBlock
	 * @param n
	 * @return
	 */
	public static String genNegativeStringCond(QueryBlockDetails queryBlock, Node n) {
		logger.log(Level.INFO, "Node type: " + n.getType() + n.getLeft() + n.getOperator() + n.getRight());
		ConstraintGenerator constraintGenerator = new ConstraintGenerator();
		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {

			if (isCVC3) {
				return cvcMap(n.getColumn(), n);
			} else {
				return smtMap(n.getColumn(), n);
			}

		} else if (n.getType().equalsIgnoreCase(Node.getValType())) {

			if (!n.getStrConst().contains("$"))
				return n.getStrConst();
			else
				return queryBlock.getParamMap().get(n.getStrConst());
		} else if (n.getType().equalsIgnoreCase(Node.getBroNodeType())
				|| n.getType().equalsIgnoreCase(Node.getBaoNodeType())
				|| n.getType().equalsIgnoreCase(Node.getLikeNodeType()) ||
				n.getType().equalsIgnoreCase(Node.getAndNodeType())
				|| n.getType().equalsIgnoreCase(Node.getOrNodeType())) {
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
			else if (n.getOperator().equalsIgnoreCase("~"))
				n.setOperator("!i~");

			if (isCVC3) {
				return "(" + genPositiveCondsForPred(queryBlock, n.getLeft()) + " " + n.getOperator() + " " +
						genPositiveCondsForPred(queryBlock, n.getRight()) + ")";
			} else {

				return "(" + (n.getOperator().equals("/=") ? "not (= " : n.getOperator()) + " "
						+ genPositiveCondsForPred(queryBlock, n.getLeft()) + " " +
						genPositiveCondsForPred(queryBlock, n.getRight()) + (n.getOperator().equals("/=") ? ")" : "")
						+ ")";
			}

		}
		return null;
	}

	/**
	 * Generate CVC3 constraints for the given node and its tuple position.
	 * 
	 * @param queryBlock
	 * @param n
	 * @param index
	 * @return
	 */
	public static String genPositiveCondsForPredAsString(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Node n,
			int index) {

		if (isCVC3) {
			return "" + genPositiveCondsForPred(queryBlock, n, index) + "";
		} else {
			return "" + genPositiveCondsForPred(queryBlock, n, index) + "";
		}
	}

	/**
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static String processImpliedConstraints(String left, String right) {
		String result = "";

		if (isCVC3) {
			result = "ASSERT " + left + " => " + right + ";\n";
		} else {
			result = "(assert " + "(=> " + left + " " + right + ")) \n";
		}
		return result;
	}

	/**
	 * 
	 * @param col
	 * @param index
	 * @param constraint
	 * @return
	 */
	public String getAndSetNotNullValuesBeforeFooter(Column col, Integer index) {

		String constraint;

		if (col != null && col.getCvcDatatype() != null
				&& (col.getCvcDatatype().equalsIgnoreCase("INT") || col.getCvcDatatype().equalsIgnoreCase("REAL")
						|| col.getCvcDatatype().equalsIgnoreCase("TIME")
						|| col.getCvcDatatype().equalsIgnoreCase("TIMESTAMP")
						|| col.getCvcDatatype().equalsIgnoreCase("DATE"))) {
			if (isCVC3) {
				constraint = "\nASSERT NOT ISNULL_" + col.getColumnName() + "(" + cvcMap(col, index + "") + ");";
			} else {
				// constraint += "\n (assert NOTISNULL_"+col.getColumnName()+"
				// "+smtMap(col,Integer.toString(index))+")";
				constraint = "\n (assert (get" + col.getColumnName() + " "
						+ smtMap(col, (IntExpr) ctx.mkInt(index)).toString() + "))";
			}

		} else {
			if (isCVC3) {
				constraint = "\nASSERT NOT ISNULL_" + col.getCvcDatatype() + "(" + cvcMap(col, index + "") + ");";
			} else {
				constraint = "\n (assert (NOTISNULL_" + col.getCvcDatatype() + " "
						+ smtMap(col, (IntExpr) ctx.mkInt(index)).toString() + "))";
			}
		}
		return constraint;
	}

	/**
	 * This method is used by GenerateConstraintsRelatedToBranchQuery class for
	 * generating ProjectedColBranchQuery constraints
	 * 
	 * @param cvc
	 * @param tempTable
	 * @param index
	 * @param pos
	 * @param operator
	 * @param rightConstraint
	 * @param col
	 * @return
	 */
	public String getStringConstraints(Table tempTable, Integer index, Integer pos, String operator,
			String rightConstraint, Column col) {

		String constraint = "";
		String smtCond = "";

		if (isCVC3) {
			constraint = "\n ASSERT (O_" + tempTable + "[" + index + "]." + pos + " " + operator + " " + rightConstraint
					+ ");\n";

		} else {
			String colName = col.getColumnName() + pos;
			smtCond = "(" + colName + " " + "(select O_" + tempTable + " " + index + ") )";

			constraint = "\n (assert (" + (operator.equals("/=") ? "not (= " : operator) + " " + rightConstraint + " "
					+ ((smtCond != null && !smtCond.isEmpty()) ? smtCond : "") +
					(operator.equals("/=") ? ")" : "") + "))\n";

		}

		return constraint;
	}

	/**
	 * This method is used by GenerateConstraintsRelatedToBranchQuery class for
	 * generating StringConstraintsForBranchQuery constraints
	 * 
	 * @param cvc
	 * @param tempTable
	 * @param index
	 * @param pos
	 * @param operator
	 * @param tempTableRight
	 * @param indexRight
	 * @param posRight
	 * @param col
	 * @return
	 */
	public String getStringConstraints(Table tempTable, Integer index, Integer pos, String operator,
			Table tempTableRight, Integer indexRight, Integer posRight, Column leftCol, Column rightCol) {

		String constraint = "";
		String smtCond1 = "";
		String smtCond2 = "";

		if (isCVC3) {

			constraint += "\n ASSERT ";
			if (operator.equals("!=")) {
				constraint += " NOT ";
				operator = "=";
			}
			constraint += "(O_" + tempTable.getTableName() + "[" + index + "]." + pos;
			constraint += operator + " O_" + tempTableRight.getTableName() + "[" + indexRight + "]." + posRight + ");";

		} else {
			String colName = leftCol.getColumnName() + pos;

			smtCond1 = "(" + colName + " " + " (select O_" + tempTable + " " + index + ") )";
			smtCond2 = "(" + rightCol.getColumnName() + "" + posRight + " " + " (select O_" + tempTableRight + " "
					+ indexRight + ") )";

			constraint = "\n (assert (" + (operator.equals("/=") ? "not (= " : operator) + " " + smtCond1 + " "
					+ smtCond2 + (operator.equals("/=") ? ")" : "") + "))";

		}

		return constraint;
	}

	public String getConstraintsForSUMWithAssert(String operator, String opVal, String constraint,
			Table tempHavingTable, Integer j, Integer tempHavingColIndex, Column col) {

		String addConstraint = "";
		if (isCVC3) {

			if (constraint == null || constraint.isEmpty()) {
				addConstraint = "\n ASSERT (";
			}

			addConstraint += constraint + " + " + "O_" + tempHavingTable.getTableName() + "[" + j + "]."
					+ tempHavingColIndex;

			if (constraint == null || constraint.isEmpty()) {
				addConstraint += ");";
			}
		} else {

			if (constraint == null || constraint.isEmpty()) {
				addConstraint = "\n (assert (";
			}
			String colName = col.getColumnName() + tempHavingColIndex;
			addConstraint += " + (" + constraint + ") (" + colName + " " + "(select O_" + tempHavingTable + " " + j
					+ "))";

			if (constraint == null || constraint.isEmpty()) {
				addConstraint += "));";
			}

		}

		return addConstraint;

	}

	public String getConstraintsForSUMWithoutAssert(String operator, String opVal, String constraint,
			Table tempHavingTable, Integer j, Integer tempHavingColIndex, Column col) {

		String addConstraint = "";
		if (isCVC3) {
			addConstraint += constraint + " + " + "O_" + tempHavingTable.getTableName() + "[" + j + "]."
					+ tempHavingColIndex;
		} else {
			String colName = col.getColumnName() + tempHavingColIndex;
			addConstraint += " + (" + constraint + ") (" + colName + " " + "(select O_" + tempHavingTable + " " + j
					+ "))";
		}
		return addConstraint;

	}

	public String getConstraintsForAVG(String constraint, int count, String operator, String value, Column col) {
		String addConstraint = "";

		if (isCVC3) {
			addConstraint += "\n ASSERT ((";
			addConstraint += constraint + ") / " + count;
			addConstraint += " " + operator + " " + value + ");";

		} else {

			addConstraint += "\n(assert (" + (operator.equals("/=") ? "not (= " : operator) + " (/ " + constraint + " "
					+ count + ") " + value + (operator.equals("/=") ? ")" : "") + "))";
		}
		return addConstraint;
	}

	public static String removeAllDigit(String str) {
		// Converting the given string
		// into a character array
		char[] charArray = str.toCharArray();
		String result = "";

		// Traverse the character array
		for (int i = 0; i < charArray.length; i++) {

			// Check if the specified character is not digit
			// then add this character into result variable
			if (!Character.isDigit(charArray[i])) {
				result = result + charArray[i];
			}
		}

		// Return result
		return result;
	}

	public static String GetGroupByConstraintsForSubqueryTableWithoutCount(GenerateCVC1 cvc, QueryBlockDetails qbt,
			String subqueryType) {
		File testFile = null;

		// write api code here to generate string
		testFile = new File(Configuration.homeDir + "/temp_smt/groupbyconstraints" + ".smt");
		if (!testFile.exists()) {

			testFile.delete();
		}

		String ConstraintsStr = "";

		try {
			testFile.createNewFile();
			// number of tuples
			int numberOfTuples = 3;
			int numberOfGroups = 2;

			ArrayList<Node> groupByNodes;
			ArrayList<Node> projectedNodes = new ArrayList<>();

			int i, j;
			groupByNodes = qbt.getGroupByNodes();

			for (i = 0; i < qbt.getProjectedCols().size(); i++) {
				if (qbt.getProjectedCols().get(i).getAgg() != null) {
					projectedNodes.add(qbt.getProjectedCols().get(i));
				}
			}

			boolean isJoin = false;
			if (qbt.getBaseRelations().size() > 1)
				isJoin = true;

			Node[] projectedColumnNodes = new Node[projectedNodes.size()]; // all the columns which are projected
			Node[] groupByColumnNodes = new Node[groupByNodes.size()]; // all the columns which are in group by

			int numberOfGroupByColumns = groupByNodes.size();
			int numberOfPRojectedColumns = projectedNodes.size();

			// String[] attrNamesSQ = new String[numberOfGroupByColumns +
			// numberOfPRojectedColumns]; // names of attributes
			Vector<String> attrNamesSQ = new Vector<String>(); // in SQ table
			Vector<Sort> attrTypesSQ = new Vector<Sort>();
			// Sort[] attrTypesSQ = new Sort[numberOfGroupByColumns +
			// numberOfPRojectedColumns]; // types of attributes in
			// SQ table

			String subquery_table_name = "GSQ" + qbt.getLevel(); // hardcoded now, need changes when multilevel approach
																	// is implemented
			String[] groupby_column = new String[numberOfGroupByColumns];

			for (i = 0; i < numberOfPRojectedColumns; i++) {
				projectedColumnNodes[i] = projectedNodes.get(i);
			}

			for (i = 0; i < groupByNodes.size(); i++) {
				groupByColumnNodes[i] = groupByNodes.get(i);
				groupby_column[i] = groupByColumnNodes[i].getColumn().getColumnName();
				attrNamesSQ.add(groupby_column[i]);
				String groupby_column_dt = groupByColumnNodes[i].getColumn().getCvcDatatype();

				attrTypesSQ.add(getColumnSort(groupby_column_dt));
				//
				attrNamesSQ.set(i, subquery_table_name + "_" + attrNamesSQ.get(i) + i);
			}

			String[] agg_functions = new String[projectedNodes.size()];
			String[] agg_column_name = new String[projectedNodes.size()];
			String[] agg_column_dt = new String[projectedNodes.size()];
			String agg_table = removeAllDigit(qbt.getBaseRelations().get(0)).toLowerCase();

			AggregateFunction[] aggColumn = new AggregateFunction[projectedNodes.size()];
			Solver dummySol = ctx.mkSolver(); // for getting string form of z3 context declarations
			int[] indexOfProjectedColmnsInSQ = new int[projectedNodes.size()];

			for (j = 0; j < projectedNodes.size(); j++) {
				aggColumn[j] = projectedColumnNodes[j].getAgg();
				agg_functions[j] = aggColumn[j].getFunc().toString();
				agg_column_name[j] = aggColumn[j].getAggExp().getColumn().getColumnName();
				attrNamesSQ.add(agg_column_name[j]); // i last element

				agg_column_dt[j] = aggColumn[j].getAggExp().getColumn().getCvcDatatype();
				agg_table = aggColumn[j].getAggExp().getTable().getTableName();
				attrTypesSQ.add(getColumnSort(agg_column_dt[j]));
				attrNamesSQ.set(i + j, subquery_table_name + "_" + agg_functions[j] + attrNamesSQ.get(i + j));
				indexOfProjectedColmnsInSQ[j] = i + j;
			}

			String arrNameSQ = "O_" + subquery_table_name;

			// creating context for subquery_table_name
			Expr aex = putTableInCtx(cvc, attrNamesSQ, attrTypesSQ, subquery_table_name);

			ArrayList attrNamesBQArrayList = cvc.getDatatypeColumns();
			// if join present in subquery - base table for aggregate will be taken from
			// fromTempJoinTableName

			if (isJoin) {
				agg_table = cvc.subqueryConstraintsMap.get("from").SQTableName.keySet().iterator().next();
				attrNamesBQArrayList = cvc.subqueryConstraintsMap.get("from").SQColumns.get(agg_table);
			}

			Vector<String> attrNamesBQ = new Vector<String>();
			Vector<Sort> attrTypesBQ = new Vector<Sort>();

			// creating context for base_table_name

			String arrNameBQ = "O_" + agg_table;

			if (isJoin) {
				// attrNamesBQ = new String[attrNamesBQArrayList.size()];
				// attrTypesBQ = new Sort[attrNamesBQArrayList.size()];
				for (int k = 0; k < attrNamesBQArrayList.size(); k++) {
					// Column column = cvc.getResultsetColumns().get(k);

					arrNameBQ = "O_" + cvc.subqueryConstraintsMap.get("from").SQTableName.keySet().iterator().next();

					attrNamesBQ.add("" + attrNamesBQArrayList.get(k));
					attrTypesBQ.add(getColumnSort(
							cvc.subqueryConstraintsMap.get("from").SQColumnsDataTypes.get(agg_table).get(k)));
				}
			} else {
				// attrNamesBQ = new String[attrNamesBQArrayList.size() - 1];
				// attrTypesBQ = new Sort[attrNamesBQArrayList.size() - 1];
				for (int k = 0; k < attrNamesBQArrayList.size() - 1; k++) {
					Column column = cvc.getResultsetColumns().get(k + 1);

					arrNameBQ = "O_" + agg_table;
					attrNamesBQ.add(agg_table + "_" + column.getColumnName() + k);
					attrTypesBQ.add(getColumnSort(column.getCvcDatatype()));
				}
			}

			// putting base table in context
			Expr aexBQ = putTableInCtx(cvc, attrNamesBQ, attrTypesBQ, agg_table);
			// end putting base table in context

			// adding dummy asserts so that solver has relevant declarations in string
			// returned by toString()
			BoolExpr dummyAssert = ctx.mkDistinct(aex);

			dummySol.add(dummyAssert);

			String[] constraints = dummySol.toString().split("\n");

			String tempStr = "";

			Vector<String> includedStatements = new Vector<String>();

			for (String statement : constraints) {
				if (statement.contains("_TupleType ") || statement.contains("declare-fun O_")) {
					includedStatements.add(statement);
				}
				if (statement.contains("(assert")) {
					break;
				}
			}
			tempStr += String.join("\n\n", includedStatements);

			// Tuple type declaration constraints of Subquery table
			// ***************************************************************************************************
			// Aggregate functions

			Expr[] assertAggValDef = new Expr[projectedNodes.size()];
			Expr[] assertAggValCall = new Expr[projectedNodes.size()];
			String[] functionName = new String[projectedNodes.size()];
			Expr[] arrForIndexI = new Expr[] { ctx.mkIntConst("i") };

			IntExpr[] arrForIntIndex = new IntExpr[numberOfTuples + 1];

			arrForIntIndex[0] = (IntExpr) ctx.mkInt(0);

			FuncDecl[] DeclAssertAggVal = new FuncDecl[projectedNodes.size()]; // Function declaration for asserting agg
																				// val fun
			String[] DefineAssertAggVal = new String[projectedNodes.size()]; // Function defination for asserting agg
																				// val fun

			for (int p = 0; p < numberOfTuples; p++) {
				arrForIntIndex[p] = (IntExpr) ctx.mkInt(p + 1);
			}

			if (projectedNodes.size() > 0) {

				Expr eqs = ctx.mkEq(dummyAssert, dummyAssert);

				for (i = 0; i < projectedNodes.size(); i++) {

					functionName[i] = "GETAGGVAL" + projectedColumnNodes[i].getAgg().getFunc()
							+ projectedColumnNodes[i].getColumn().getColumnName(); // function names

					DeclAssertAggVal[i] = ctx.mkFuncDecl(functionName[i], ctx.getIntSort(), ctx.mkBoolSort()); // creating
																												// function
																												// declarations

					ctxFuncDecls.put(functionName[i], DeclAssertAggVal[i]); // putting function declaration in context

					DefineAssertAggVal[i] = "(define-fun " + functionName[i] + "((i Int))Bool"; // function defination
																								// using "define-fun"

					if (projectedColumnNodes[i].getAgg().getFunc().equals("SUM")) {
						// function definations if it is SUM

						Expr[] colValueSQ = new Expr[numberOfGroupByColumns + 1];
						for (int s = 0; s < numberOfGroupByColumns; s++) {
							colValueSQ[s] = smtMap(arrNameSQ, "i", -1, s);
						}
						Expr colValueSQ1 = smtMap(arrNameSQ, "i", -1, indexOfProjectedColmnsInSQ[i]);

						Expr[][] colValueBQ = new Expr[numberOfGroupByColumns + 1][numberOfTuples];
						BoolExpr[][] calculateAggregate = new BoolExpr[numberOfTuples][numberOfGroupByColumns];
						Expr[] ifthenelseExpr = new Expr[numberOfTuples];
						ArithExpr[][] multieExpr = new ArithExpr[numberOfTuples][2];
						Expr[] multiplyexpr = new Expr[numberOfTuples];
						ArithExpr[] addExpr = new ArithExpr[numberOfTuples];
						Expr[] andForIfthenelse = new Expr[numberOfTuples];

						// Done for base table
						// arrName contains name if table which will be used in select
						// Creating base relation and finally tuple accessor for the table

						// Expr t1 = ctx.mkInt(1);
						// Expr t2 = ctx.mkInt(0);
						// Function 1

						String temp = "";

						if (isJoin) {
							String joinTable = cvc.subqueryConstraintsMap.get("from").SQTableName.keySet().iterator()
									.next();
							for (int q = 0; q < cvc.subqueryConstraintsMap.get("from").SQColumns.get(joinTable)
									.size(); q++) {

								attrNamesBQ.set(q, removeAllDigit(attrNamesBQ.get(q).split("__")[1]));

							}
						}

						for (int p = 0; p < numberOfTuples; p++) {
							int s;
							for (s = 0; s < numberOfGroupByColumns; s++) {
								colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
										cvc.getDatatypeColumns().indexOf(groupby_column[s]));
								calculateAggregate[p][s] = (BoolExpr) ctx.mkEq(colValueSQ[s], colValueBQ[s][p]);

							}
							andForIfthenelse[p] = ctx.mkAnd(calculateAggregate[p]);
							colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
									cvc.getDatatypeColumns().indexOf(agg_column_name[i]));

							ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], ctx.mkInt(1), ctx.mkInt(0));
							multieExpr[p][1] = (ArithExpr) colValueBQ[numberOfGroupByColumns][p];
							multieExpr[p][0] = (ArithExpr) ifthenelseExpr[p];
							multiplyexpr[p] = ctx.mkMul(multieExpr[p]);
							addExpr[p] = (ArithExpr) multiplyexpr[p];

						}

						Expr additionexpr1 = ctx.mkAdd(addExpr);
						Expr eqexpr2 = ctx.mkEq(colValueSQ1, additionexpr1);
						assertAggValCall[i] = DeclAssertAggVal[i].apply(arrForIndexI);
						assertAggValDef[i] = eqexpr2;

					}

					else if (projectedColumnNodes[i].getAgg().getFunc().equals("COUNT")) {
						Expr[] colValueSQ = new Expr[numberOfGroupByColumns + 1];
						for (int s = 0; s < numberOfGroupByColumns; s++) {
							colValueSQ[s] = smtMap(arrNameSQ, "i", -1, s);
						}
						Expr colValueSQ1 = smtMap(arrNameSQ, "i", -1, indexOfProjectedColmnsInSQ[i]);
						Expr[][] colValueBQ = new Expr[numberOfGroupByColumns + 1][numberOfTuples];
						BoolExpr[][] calculateAggregate = new BoolExpr[numberOfTuples][numberOfGroupByColumns];
						Expr[] ifthenelseExpr = new Expr[numberOfTuples];
						ArithExpr[][] multieExpr = new ArithExpr[numberOfTuples][2];
						Expr[] multiplyexpr = new Expr[numberOfTuples];
						ArithExpr[] addExpr = new ArithExpr[numberOfTuples];
						Expr[] andForIfthenelse = new Expr[numberOfTuples];

						// Done for base table
						// arrName contains name if table which will be used in select
						// Creating base relation and finally tuple accessor for the table

						// Function 1

						for (int p = 0; p < numberOfTuples; p++) {
							int s;
							for (s = 0; s < numberOfGroupByColumns; s++) {
								colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
										cvc.getDatatypeColumns().indexOf(groupby_column[s]));
								calculateAggregate[p][s] = (BoolExpr) ctx.mkEq(colValueSQ[s], colValueBQ[s][p]);

							}
							andForIfthenelse[p] = ctx.mkAnd(calculateAggregate[p]);
							colValueBQ[s][p] = smtMap(arrNameBQ, "i", p,
									cvc.getDatatypeColumns().indexOf(agg_column_name[i]));

							ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], ctx.mkInt(1), ctx.mkInt(0));
							multieExpr[p][0] = (ArithExpr) ifthenelseExpr[p];
							addExpr[p] = (ArithExpr) ifthenelseExpr[p];
						}
						Expr additionexpr1 = ctx.mkAdd(addExpr);
						Expr eqexpr2 = ctx.mkEq(colValueSQ1, additionexpr1);
						assertAggValCall[i] = DeclAssertAggVal[i].apply(arrForIndexI);
						assertAggValDef[i] = eqexpr2;
					}

				}
			}

			// Function 2 SQ Forward function
			String functionName2 = "SQTABLE_" + qbt.getLevel() + "_FORWARD";
			FuncDecl sqTableIForward = ctx.mkFuncDecl(functionName2, ctx.getIntSort(), ctx.mkBoolSort());
			ctxFuncDecls.put(functionName2, sqTableIForward);

			String DefinesqTableIForward = "(define-fun " + functionName2 + "((i1 Int))Bool";

			Expr[] arrForIndexj1 = new Expr[] { ctx.mkIntConst("j1") };

			Expr compareExpr1 = ctx.mkLt((ArithExpr) (IntExpr) ctx.mkInt(0), (ArithExpr) ctx.mkIntConst("j1"));
			Expr compareExpr2 = ctx.mkLe((ArithExpr) ctx.mkIntConst("j1"),
					(ArithExpr) (IntExpr) ctx.mkInt(numberOfGroups));
			Expr andExpr1 = ctx.mkAnd((BoolExpr) compareExpr1, (BoolExpr) compareExpr2);

			Expr[] colValueBQForForward = new Expr[numberOfGroupByColumns];
			Expr[] colValueSQForForward = new Expr[numberOfGroupByColumns];
			BoolExpr[] eqExprForward = new BoolExpr[numberOfGroupByColumns + 1];

			for (i = 0; i < numberOfGroupByColumns; i++) {
				colValueSQForForward[i] = smtMap(arrNameSQ, "j1", -1, i);
				colValueBQForForward[i] = smtMap(arrNameBQ, "i1", -1,
						cvc.getDatatypeColumns().indexOf(groupby_column[i]));
				eqExprForward[i] = (BoolExpr) ctx.mkEq(colValueSQForForward[i], colValueBQForForward[i]);
			}
			eqExprForward[i] = (BoolExpr) andExpr1;

			Expr andExpr2 = ctx.mkAnd(eqExprForward);
			Expr exitsExpr1 = ctx.mkExists(arrForIndexj1, andExpr2, 1, null, null, null, null);

			// Function 3 SQ Backward function
			String functionName3 = "SQTABLE_" + qbt.getLevel() + "_BACKWARD";
			FuncDecl sqTableIBackward = ctx.mkFuncDecl(functionName3, ctx.getIntSort(), ctx.mkBoolSort());
			ctxFuncDecls.put(functionName3, sqTableIBackward);
			String DefinesqTableIBackward = "(define-fun " + functionName3 + "((k1 Int))Bool";

			Expr[] arrForIndexK1 = new Expr[] { ctx.mkIntConst("k1") };
			Expr[] arrForIndexL1 = new Expr[] { ctx.mkIntConst("l1") };

			Expr compareExpr3 = ctx.mkLt((ArithExpr) (IntExpr) ctx.mkInt(0), (ArithExpr) ctx.mkIntConst("l1"));
			Expr compareExpr4 = ctx.mkLe((ArithExpr) ctx.mkIntConst("l1"),
					(ArithExpr) (IntExpr) ctx.mkInt(numberOfTuples));

			Expr andExpr3 = ctx.mkAnd((BoolExpr) compareExpr3, (BoolExpr) compareExpr4);

			Expr[] colValueBQForBackward = new Expr[numberOfGroupByColumns];
			Expr[] colValueSQForBackward = new Expr[numberOfGroupByColumns];
			BoolExpr[] eqExprBackward = new BoolExpr[numberOfGroupByColumns + 1];

			for (i = 0; i < numberOfGroupByColumns; i++) {
				colValueSQForBackward[i] = smtMap(arrNameSQ, "k1", -1, i);
				colValueBQForBackward[i] = smtMap(arrNameBQ, "l1", -1,
						cvc.getDatatypeColumns().indexOf(groupby_column[i]));
				eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
			}
			eqExprBackward[i] = (BoolExpr) andExpr3;

			Expr andExpr4 = ctx.mkAnd(eqExprBackward);

			Expr exitsExpr2 = ctx.mkExists(arrForIndexL1, andExpr4, 1, null, null, null, null);

			//
			ConstraintsStr += "\n\n" + tempStr;
			ConstraintsStr += "\n\n" + DefinesqTableIBackward.toString() + "\n\n " + exitsExpr2.toString() + ")"
					+ "\n\n";
			ConstraintsStr += "\n\n" + DefinesqTableIForward.toString() + "\n\n " + exitsExpr1.toString() + ")"
					+ "\n\n";

			BoolExpr[] assertSqTableIForward1 = new BoolExpr[numberOfTuples];
			BoolExpr[] assertSqTableIBackward1 = new BoolExpr[numberOfGroups];
			BoolExpr[][] assertAggValFunsCall = new BoolExpr[projectedNodes.size()][numberOfGroups];

			for (int s = 0; s < numberOfTuples; s++) {
				assertSqTableIForward1[s] = (BoolExpr) sqTableIForward.apply(arrForIntIndex[s]);
			}
			for (int s = 0; s < numberOfGroups; s++) {
				assertSqTableIBackward1[s] = (BoolExpr) sqTableIBackward.apply(arrForIntIndex[s]);
			}

			BoolExpr andAssertsForward = ctx.mkAnd(assertSqTableIForward1);
			BoolExpr andAssertsBackward = ctx.mkAnd(assertSqTableIBackward1);
			//
			// function asserts for number of tuples

			for (i = 0; i < projectedNodes.size(); i++) {
				ConstraintsStr += "\n\n" + DefineAssertAggVal[i] + "\n\n " + assertAggValDef[i].toString() + ")"
						+ "\n\n";
				for (j = 0; j < numberOfGroups; j++) {
					assertAggValFunsCall[i][j] = (BoolExpr) DeclAssertAggVal[i].apply(arrForIntIndex[j]);
				}
				BoolExpr andAssertAggValFunsCall = ctx.mkAnd(assertAggValFunsCall[i]);
				ConstraintsStr += "\n (assert " + andAssertAggValFunsCall + " )\n";

			}

			ConstraintsStr += "\n\n (assert " + andAssertsForward.toString() + ")" + "\n\n";
			ConstraintsStr += "\n\n (assert " + andAssertsBackward.toString() + ")" + "\n\n";

			FileWriter myWriter = new FileWriter(Configuration.homeDir + "/temp_smt/groupbyconstraints" + ".smt");
			myWriter.write(ConstraintsStr);
			myWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ConstraintsStr;

	}

	public static String GetGroupByConstraintsForSubqueryTableWithCount(GenerateCVC1 cvc, QueryBlockDetails qbt,
			String subqueryType, String SQtableName) throws CloneNotSupportedException {
		File testFile = null;
		String indexType = "Int";
		if (Configuration.getProperty("isEnumInt").equalsIgnoreCase("true"))
			indexType = cvc.enumArrayIndex;
		// write api code here to generate string
		testFile = new File(Configuration.homeDir + "/temp_smt/groupbyconstraints" + ".smt");
		if (!testFile.exists()) {

			testFile.delete();
		}
		String ConstraintsStr = "";

		try {
			testFile.createNewFile();
			// number of tuples
			int numberOfTuples = 0; // get the number of tuples in base table from cvc
			int numberOfGroups = 0;

			AggregateDataStructure aggregateDS = new AggregateDataStructure(
					SQtableName == "" ? "GSQ" + qbt.getLevel() : SQtableName + qbt.getLevel());
			ProjectedNodesData projectedNodeDs = new ProjectedNodesData();

			// ArrayList<Node> groupByNodes; // comment
			ArrayList<Node> projectedNodes = new ArrayList<>();

			int i, j;

			// groupByNodes = qbt.getGroupByNodes(); // comment

			// groupByNodes = GenConstraints.HandleExtraGroupByMutations(groupByNodes,
			// cvc.getCurrentMutant()); // comment

			for (i = 0; i < qbt.getProjectedCols().size(); i++) {
				if (qbt.getProjectedCols().get(i).getAgg() != null) {
					projectedNodes.add(qbt.getProjectedCols().get(i));
				}
			}
			GroupByNodesData groupByNodeDS = new GroupByNodesData(qbt.getGroupByNodes());
			groupByNodeDS.noOfGroupByColumns = groupByNodeDS.getGroupByNodes().size();

			projectedNodeDs.putProjectedNodes(projectedNodes);
			projectedNodeDs.noOfProjectedColumns = projectedNodes.size();

			// to handle extra group by column mutations
			GenConstraints.HandleExtraGroupByMutations(groupByNodeDS.getGroupByNodes(), cvc.getCurrentMutant());

			// checking if join subquery table created
			boolean isJoin = true;
			// uncomment later

			Vector<String> primaryKeySQ = new Vector<String>(); // comment

			String cntColumnName = aggregateDS.subqueryTableName + "_XDATA_CNT";

			Solver dummySol = ctx.mkSolver(); // for getting string form of z3 context declarations

			// int[] indexOfProjectedColmnsInSQ = new int[projectedNodes.size()]; // comment

			HashMap<String, Object> aggTableDetails = createTempTableColumnsForAggregates(aggregateDS, groupByNodeDS,
					projectedNodeDs, subqueryType);

			cvc.getTableMap().putSQTables((Map<String, Table>) aggTableDetails.get("TableMap"));


			Expr aex = putTableInCtx(cvc, aggregateDS.attrColumnsOfSQ,
					aggregateDS.attrTypesOfSQ, aggregateDS.subqueryTableName); // uncomment

			ArrayList attrNamesBQArrayList = cvc.getDatatypeColumns();

			String agg_table = "";
			if (isJoin) {
				if (subqueryType.equalsIgnoreCase(subqueryType)&&cvc.subqueryConstraintsMap.containsKey(subqueryType)) {
					aggregateDS.baseTableName = cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet()
							.iterator().next();
					agg_table = cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
					numberOfTuples = cvc.getNoOfTuples()
							.get(cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next());
					attrNamesBQArrayList = cvc.subqueryConstraintsMap.get(subqueryType).SQColumns.get(agg_table);
					Table baseTable = cvc.getTableMap().getSQTableByName(agg_table);
					for (i = 0; i < groupByNodeDS.groupByColumnsFromRefTables.size(); i++) {
						for (j = 0; j < attrNamesBQArrayList.size(); j++) {

							if (attrNamesBQArrayList.get(j).toString()
									.contains(groupByNodeDS.groupByColumnsFromRefTables.get(i).getColumnName())
									&& attrNamesBQArrayList.get(j).toString().contains(
											groupByNodeDS.groupByColumnsFromRefTables.get(i).getTableName())) {
										if(i>groupByNodeDS.groupByColumnsFromJSQ.size()) 
											groupByNodeDS.groupByColumnsFromJSQ.add(
										baseTable.getColumn(attrNamesBQArrayList.get(j).toString()));
										else
											groupByNodeDS.groupByColumnsFromJSQ.add(i,
										baseTable.getColumn(attrNamesBQArrayList.get(j).toString()));
								break;
							}
						}

					}
					for (i = 0; i < projectedNodes.size(); i++) {
						for (j = 0; j < attrNamesBQArrayList.size(); j++) {

							if (attrNamesBQArrayList.get(j).toString()
									.contains(projectedNodes.get(i).getColumn().getColumnName())
									&& attrNamesBQArrayList.get(j).toString()
											.contains(projectedNodes.get(i).getColumn().getTableName())) {
								projectedNodeDs.aggregateColumnsFromJSQ.add(i,
										baseTable.getColumn(attrNamesBQArrayList.get(j).toString()));
								break;
							}
						}

					}
				}
			}

			Vector<String> attrNamesBQ = new Vector<String>();
			Vector<Sort> attrTypesBQ = new Vector<Sort>();

			// creating context for base_table_name

			Table sqi = cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName);
			boolean isExits = cvc.getTableMap().getSQTableByName(agg_table).getIsExist();

			sqi.setIsExist(isExits);

			// String arrNameBQ = "O_" + agg_table; // comment
			aggregateDS.arrayNameOfBQInCtx = "O_" + agg_table;
			if (isJoin) {
				// attrNamesBQ = new String[attrNamesBQArrayList.size()];
				// attrTypesBQ = new Sort[attrNamesBQArrayList.size()];
				for (int k = 0; k < attrNamesBQArrayList.size(); k++) {
					// Column column = cvc.getResultsetColumns().get(k);

					if (subqueryType.equalsIgnoreCase(subqueryType)&&cvc.subqueryConstraintsMap.containsKey(subqueryType)) {
						// arrNameBQ = "O_"
						// +
						// cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
						// // comment
						aggregateDS.arrayNameOfBQInCtx = "O_"
								+ cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
						aggregateDS.attrNamesBQ.add("" + attrNamesBQArrayList.get(k));
						aggregateDS.attrTypesBQ.add(getColumnSort(
								cvc.subqueryConstraintsMap.get(subqueryType).SQColumnsDataTypes.get(agg_table).get(k)));
						// attrNamesBQ.add("" + attrNamesBQArrayList.get(k)); // comment
						// attrTypesBQ.add(getColumnSort(
						// cvc.subqueryConstraintsMap.get(subqueryType).SQColumnsDataTypes.get(agg_table).get(k)));
						// // comment
					}
					// else if (subqueryType.equalsIgnoreCase("outer")) {
					// arrNameBQ = "O_" +
					// cvc.subqueryConstraintsMap.get("outer").SQTableName.keySet().iterator().next();
					// attrNamesBQ.add("" + attrNamesBQArrayList.get(k));
					// attrTypesBQ.add(getColumnSort(cvc.subqueryConstraintsMap.get("outer").SQColumnsDataTypes.get(agg_table).get(k)));
					// }
					// else if (subqueryType.equalsIgnoreCase("where")) {
					// arrNameBQ = "O_" +
					// cvc.subqueryConstraintsMap.get("where").SQTableName.keySet().iterator().next();
					// attrNamesBQ.add("" + attrNamesBQArrayList.get(k));
					// attrTypesBQ.add(getColumnSort(cvc.subqueryConstraintsMap.get("where").SQColumnsDataTypes.get(agg_table).get(k)));
					// }

				}
			}

			// putting base table in context
			// Expr aexBQ = putTableInCtx(cvc, attrNamesBQ, attrTypesBQ, agg_table); //
			// comment
			Expr aexBQ = putTableInCtx(cvc, aggregateDS.attrNamesBQ,
					aggregateDS.attrTypesBQ, aggregateDS.baseTableName); // uncomment

			// end putting base table in context

			cvc.getNoOfTuples().put(aggregateDS.subqueryTableName, numberOfTuples);
			cvc.putNoOfOutputTuples(aggregateDS.subqueryTableName, numberOfTuples);

			// cvc.getNoOfTuples().put(subquery_table_name, numberOfTuples);
			// cvc.putNoOfOutputTuples(subquery_table_name, numberOfTuples);
			// adding dummy asserts so that solver has relevant declarations in string
			// returned by toString()
			BoolExpr dummyAssert = ctx.mkDistinct(aex);

			dummySol.add(dummyAssert);

			String[] constraints = dummySol.toString().split("\n");

			String tempStr = "";

			Vector<String> includedStatements = new Vector<String>();

			for (String statement : constraints) {
				if (statement.contains("_TupleType ") || statement.contains("declare-fun O_")) {
					includedStatements.add(statement);
				}
				if (statement.contains("(assert")) {
					break;
				}
			}
			tempStr += String.join("\n\n", includedStatements);

			// Tuple type declaration constraints of Subquery table

			// ***************************************************************************************************
			// primary key constraints for SQ table using COUNT

			Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
					aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), "i1");
			Expr cntOfSQColumnSQj1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
					aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), "j1");

			// ***************************************************************************************************

			// }
			String primaryKeyConstraintsOfSQ = getPrimaryKeyConstraintsForGSQTable(cvc, aggregateDS, indexType,
					numberOfTuples);

			// Aggregate functions

			Expr[] arrForIndexI = new Expr[] { ctx.mkIntConst("i") };

			int cntColumnBQIndex = aggregateDS.attrNamesOfBQ.size() - 1;
			Expr[] arrForIntIndex = new Expr[numberOfTuples + 1];
			// Boolean ifSumPresent = false;
			// Boolean ifCountPresent = false;
			int p;
			for (p = 0; p < numberOfTuples; p++) {
				arrForIntIndex[p + 1] = (IntExpr) ctx.mkInt(p + 1);
			}
			arrForIntIndex[0] = (IntExpr) ctx.mkInt(0);

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
				arrForIndexI = new Expr[] { ctx.mkConst("i", currentSort) };
				arrForIntIndex[0] = ctx.mkConst(cvc.enumIndexVar + 0, currentSort);
				for (p = 0; p < numberOfTuples; p++) {
					arrForIntIndex[p] = ctx.mkConst(cvc.enumIndexVar + (p + 1), currentSort);
				}
			}
			if (isJoin) {
				cntColumnBQIndex = attrNamesBQArrayList.size() - 1;
				// selectCntBQ = smtMap(arrNameBQ, "i1", p, cntColumnBQIndex);

			}

			HashMap<String, Object> aggCalls = generateAggregateConstraints(cvc, qbt, aggregateDS, projectedNodeDs,
					groupByNodeDS, arrForIndexI, numberOfTuples, cntColumnBQIndex);

			Vector<Expr> assertAggValDef = (Vector<Expr>) aggCalls.get("assertAggValDef");
			Vector<Expr> assertAggValCall = (Vector<Expr>) aggCalls.get("assertAggValCall");
			Vector<String> functionName = (Vector<String>) aggCalls.get("functionName");
			Vector<FuncDecl> DeclAssertAggVal = (Vector<FuncDecl>) aggCalls.get("DeclAssertAggVal"); // Function
																										// declaration
																										// for asserting
																										// agg val
			Vector<String> DefineAssertAggVal = (Vector<String>) aggCalls.get("DefineAssertAggVal"); // Function
																										// defination
																										// for asserting
																										// agg val fun

			Expr selectCntBQ;
			selectCntBQ = genSelectTest2(cvc, aggregateDS.arrayNameOfBQInCtx, cntColumnBQIndex, "i1");
			Expr checkIfBQTupleIsValid = ctx.mkGt((ArithExpr) selectCntBQ, ctx.mkInt(0));

			// Function 2 SQ Forward function
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);

			aggregateDS.attrNamesOfBQ = attrNamesBQArrayList;
			HashMap<String, Object> forwardPassData = generateForwardPassConstraints(cvc, qbt, aggregateDS,
					groupByNodeDS, projectedNodeDs, cntColumnBQIndex,
					numberOfTuples, checkIfBQTupleIsValid, currentSort, indexType, 1);

			FuncDecl sqTableIForward = (FuncDecl) forwardPassData.get("sqTableIForward");
			String DefinesqTableIForward = (String) forwardPassData.get("DefinesqTableIForward");
			Expr exitsExpr1 = (Expr) forwardPassData.get("exitsExpr1");

			// Function 3 SQ Backward function

			HashMap<String, Object> backwardPassData = generateBackwardPassConstraints(cvc, qbt, aggregateDS,
					groupByNodeDS, projectedNodeDs, cntColumnBQIndex,
					numberOfTuples, checkIfBQTupleIsValid, currentSort, indexType);

			FuncDecl sqTableIBackward = (FuncDecl) backwardPassData.get("sqTableIBackward");
			String DefinesqTableIBackward = (String) backwardPassData.get("DefinesqTableIBackward");
			Expr impliesExprBackward = (Expr) backwardPassData.get("impliesExprBackward");
			Expr[] eqExprBackward = (Expr[]) backwardPassData.get("eqExprBackward");
			Expr[] colValueBQForBackward = (Expr[]) backwardPassData.get("colValueBQForBackward");
			Expr[] colValueSQForBackward = (Expr[]) backwardPassData.get("colValueSQForBackward");

			// // end of backward function

			ConstraintsStr += "\n\n" + tempStr;
			ConstraintsStr += primaryKeyConstraintsOfSQ;
			ConstraintsStr += "\n\n" + DefinesqTableIForward.toString() + "\n\n " + "(and \n\t (=> \n\t\t"
					+ checkIfBQTupleIsValid + "\n\n\t" + exitsExpr1.toString() + ")))" + "\n\n";
			ConstraintsStr += "\n\n" + DefinesqTableIBackward.toString() + "\n\n " + impliesExprBackward.toString()
					+ ")" + "\n\n";

			BoolExpr[] assertSqTableIForward1 = new BoolExpr[numberOfTuples];
			Vector<BoolExpr> assertAggValFunsCall = new Vector<BoolExpr>();

			for (int s = 0; s < numberOfTuples; s++) {
				assertSqTableIForward1[s] = (BoolExpr) sqTableIForward.apply(arrForIntIndex[s + 1]);
			}

			BoolExpr andAssertsForward = ctx.mkAnd(assertSqTableIForward1);

			Expr assertSqTableIBackward1;

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkConst("i1", currentSort));
			} else
				assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkIntConst("i1"));

			// function asserts for number of tuples
			Expr backwardExprAnd = ctx.mkAnd((BoolExpr) assertSqTableIBackward1);
			i = 0;

			for (i = 0; i < assertAggValDef.size(); i++) {
				if (assertAggValDef.get(i) != null) {
					ConstraintsStr += "\n\n" + DefineAssertAggVal.get(i) + "\n\n " + assertAggValDef.get(i).toString()
							+ ")" + "\n\n";
					if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
						assertAggValFunsCall
								.add((BoolExpr) DeclAssertAggVal.get(i).apply(ctx.mkConst("i1", currentSort)));
					} else
						// assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkIntConst("i1"));
						assertAggValFunsCall.add((BoolExpr) DeclAssertAggVal.get(i).apply(ctx.mkIntConst("i1")));
				} else {
					assertAggValFunsCall.add(ctx.mkTrue());
					System.out.println(
							"************ Not supported for this Aggregate Function Name: " + functionName.get(i));
				}
			}

			assertAggValFunsCall.add((BoolExpr) assertSqTableIBackward1);

			Object[] temp = assertAggValFunsCall.toArray();
			Expr[] array = Arrays.copyOf(
					temp, temp.length, Expr[].class);

			BoolExpr andAssertBackwardAndAggValFunsCall = ctx.mkAnd(array); // work from here
			Expr[] iColArray = new IntExpr[] { ctx.mkIntConst("i1") };

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				iColArray = new Expr[] { ctx.mkConst("i1", currentSort) };
			}

			Expr backwardAsssertsFinal = ctx.mkForall((Expr[]) iColArray, (Expr) andAssertBackwardAndAggValFunsCall, 1,
					null, null, null, null);

			ConstraintsStr += "\n\n (assert " + andAssertsForward.toString() + ")" + "\n\n";
			ConstraintsStr += "\n\n (assert " + backwardAsssertsFinal.toString() + ")" + "\n\n";

			// Constraints for constrained aggregation - test code for having - start

			String constr = "";
			if (qbt.isConstrainedAggregation()
					&& ((cvc.getCurrentMutant() == null)
							|| cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() != 7
									&& cvc.getCurrentMutant().getMutationTypeNumber() != 8)) {
				ArrayList<Node> aggConstraints = qbt.getAggConstraints();
				String tempstragg = "";
				
				for(i=1; i<=numberOfTuples; i++){
					String tep = "";
					Expr cntOfSQColumnSQint = ConstraintGenerator.genSelectTest(cvc, aggregateDS.subqueryTableName,
					aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), i);

					for (j = 0; j < aggConstraints.size(); j++) {
						
						tep += getAggregateConstraintsWithCount(cvc, aggConstraints.get(j),
								aggregateDS.subqueryTableName,
								aggregateDS.attrColumnsOfSQ, aggregateDS.attrTypesOfSQ, aggregateDS.columnToIndexMapInSQ, i);
					}
					tempstragg += "(and "  + ctx.mkGt(cntOfSQColumnSQint, ctx.mkInt(0)).toString() + "\n\t" + tep + " )";
				}
				constr += "(assert(or" + tempstragg +"))";
				// constr = "(assert (exists ((i1 " + indexType + "))"
				// //  \n\t(=> "
						
				
				// 		+ "(and "  + ctx.mkGt(cntOfSQColumnSQi1, ctx.mkInt(0)).toString() + "\n\t" + constr + " )))";
			}

			// aggregate Constraints end

			// extra group by attribute killing constraints - start

			Expr mutantConstrExpr = ctx.mkTrue();
			if (cvc.getCurrentMutant() != null && (cvc.getCurrentMutant().getMutationTypeNumber() == 7
					|| cvc.getCurrentMutant().getMutationTypeNumber() == 8)) {

				Expr cntValidSQ = getConstraintsForValidCountUsingTable(
						cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName),
						aggregateDS.subqueryTableName, 1, 0);
				int indSq = getIndexOfMutantColumnCount(aggregateDS.attrColumnsOfSQ, cvc.getCurrentMutant(), "COUNT",
						1);
				int indBq = getIndexOfMutantColumnCount(
						cvc.getTableMap().getSQTableByName(aggregateDS.baseTableName).getColumnIndexList(),
						cvc.getCurrentMutant(), "COUNT", 0);
				Expr countMutant = ctx.mkEq(smtMap(aggregateDS.arrayNameOfSQInCtx, "1", 1, indSq), ctx.mkInt(2));
				Expr backGroupByi, backGroupByj;

				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {
					colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", 1, i);
					colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "i", -1,
							attrNamesBQArrayList.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
					else
						eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
				}

				ArrayList<Node> aggConstraints = qbt.getAggConstraints();

				Expr[] constAggExpr = new Expr[aggConstraints.size()];
				Expr isNotNull;
				for (i = 0; i < aggConstraints.size(); i++) {
					Column c;
					AggregateFunction agg;
					Node n = aggConstraints.get(i);
					if (n.getLeft() != null) {
						c = n.getLeft().getColumn();
						agg = n.getLeft().getAgg();
					} else {
						c = n.getRight().getColumn();
						agg = n.getRight().getAgg();
					}
					int index = projectedNodeDs.aggregateColumnsFromRefTables.indexOf(c);
					Column constrColumn = projectedNodeDs.aggregateColumnsFromJSQ.get(index);
					index = cvc.getTableMap().getSQTableByName(constrColumn.getTableName())
							.getColumnIndex(constrColumn.getColumnName());

					Expr constrAggExpri, constrAggExprj;
					if (agg.getFunc().equalsIgnoreCase(n.getAgg().getAggCOUNT())) {
						constrAggExpri = ctx.mkTrue();
						constrAggExprj = ctx.mkTrue();
					} else {
						constrAggExpri = getConstraintAggregationForMutants(cvc, constrColumn, n, index, "i");
						constrAggExprj = getConstraintAggregationForMutants(cvc, constrColumn, n, index, "j");

					}

					Expr isNotNulli = ctx.mkNot(
							ctxFuncDecls
									.get("ISNULL_" + c.getColumnName())
									.apply(ConstraintGenerator.genSelectTest2(cvc, constrColumn.getTableName(), index,
											"i")));
					Expr isNotNullj = ctx.mkNot(
							ctxFuncDecls
									.get("ISNULL_" + c.getColumnName())
									.apply(ConstraintGenerator.genSelectTest2(cvc, constrColumn.getTableName(), index,
											"j")));
					constAggExpr[i] = ctx.mkAnd(constrAggExpri, constrAggExprj, isNotNulli, isNotNullj);
				}

				Expr finalConstAggExpr = ctx.mkAnd(constAggExpr);
				Expr andExpr4 = ctx.mkAnd(eqExprBackward);
				backGroupByi = ctx.mkAnd((BoolExpr) andExpr4,
						ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "i", -1, cntColumnBQIndex), ctx.mkInt(0)));

				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {

					colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", 1, i);
					colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "j", -1,
							attrNamesBQArrayList.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
					else
						eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
				}
				andExpr4 = ctx.mkAnd(eqExprBackward);
				backGroupByj = ctx.mkAnd((BoolExpr) andExpr4,
						ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "j", -1, cntColumnBQIndex), ctx.mkInt(0)));

				// }

				Expr mutantEq = ctx
						.mkNot(ctx.mkEq(smtMap(aggregateDS.arrayNameOfBQInCtx, "i", -1, indBq),
								smtMap(aggregateDS.arrayNameOfBQInCtx, "j", -1, indBq)));

				Expr[] arrForIndex = new Expr[] { ctx.mkIntConst("i"), ctx.mkIntConst("j") };

				mutantConstrExpr = ctx.mkAnd(cntValidSQ, countMutant, ctx.mkExists(arrForIndex,
						ctx.mkAnd(backGroupByi, backGroupByj, mutantEq), 1, null, null, null, null));
				mutantConstrExpr = ctx.mkAnd(cntValidSQ, ctx.mkExists(arrForIndex,
						ctx.mkAnd(backGroupByi, backGroupByj, mutantEq, finalConstAggExpr), 1, null, null, null, null));
				ctx.mkOr(ctx.mkTrue());

			}

			// For Agg mutant

			boolean skipMut2 = false;
			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() == 2) {
				Column mutColumn = ((Node) cvc.getCurrentMutant().getMutationLoc()).getColumn();
				if (!mutColumn.getCvcDatatype().equalsIgnoreCase("int")
						&& !mutColumn.getCvcDatatype().equalsIgnoreCase("real"))
					skipMut2 = true;
			}
			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() == 2) {
				Expr cntValidSQ = getConstraintsForValidCountUsingTable(
						cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName),
						aggregateDS.subqueryTableName, 1, 0);

				Expr[] arrForIndex = new Expr[] { ctx.mkIntConst("i"), ctx.mkIntConst("j"), ctx.mkIntConst("k") };

				String aggFun = "COUNT";
				int indSq = getIndexOfMutantColumnCount(aggregateDS.attrColumnsOfSQ, cvc.getCurrentMutant(), aggFun, 1);
				// int indBq = getIndexOfMutantColumnCount(
				// cvc.getTableMap().getSQTableByName(aggregateDS.baseTableName).getColumnIndexList(),
				// cvc.getCurrentMutant(), 0);

				Expr countMutant = ctx.mkEq(smtMap(aggregateDS.arrayNameOfSQInCtx, "1", 1, indSq), ctx.mkInt(3));
				Expr backGroupByi, backGroupByj, backGroupByk;

				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {

					colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", 1, i);
					colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "i", -1,
							attrNamesBQArrayList.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
					else
						eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
				}
				Expr andExpr4 = ctx.mkAnd(eqExprBackward);
				backGroupByi = ctx.mkAnd((BoolExpr) andExpr4,
						ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "i", -1, cntColumnBQIndex), ctx.mkInt(0)));

				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {

					colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", 1, i);
					colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "j", -1,
							attrNamesBQArrayList.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
					else
						eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
				}
				andExpr4 = ctx.mkAnd(eqExprBackward);
				backGroupByj = ctx.mkAnd((BoolExpr) andExpr4,
						ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "j", -1, cntColumnBQIndex), ctx.mkInt(0)));

				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {

					colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", 1, i);
					colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "k", -1,
							attrNamesBQArrayList.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
					else
						eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
				}
				andExpr4 = ctx.mkAnd(eqExprBackward);
				backGroupByk = ctx.mkAnd((BoolExpr) andExpr4,
						ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "k", -1, cntColumnBQIndex), ctx.mkInt(0)));

				Column mutColumn = ((Node) cvc.getCurrentMutant().getMutationLoc()).getColumn();
				int ind = projectedNodeDs.aggregateColumnsFromRefTables.indexOf(mutColumn);
				Column mutColumnBQ = projectedNodeDs.aggregateColumnsFromJSQ.get(ind);
				ind = aggregateDS.attrNamesOfBQ.indexOf(mutColumnBQ.getColumnName());

				Expr aggColi = genSelectTest2(cvc, mutColumnBQ.getTableName(), ind, "i");
				Expr aggColj = genSelectTest2(cvc, mutColumnBQ.getTableName(), ind, "j");
				Expr aggColk = genSelectTest2(cvc, mutColumnBQ.getTableName(), ind, "k");

				Expr indexNotEq1 = ctx.mkNot(ctx.mkEq(ctx.mkIntConst("i"), ctx.mkIntConst("j")));
				Expr indexNotEq2 = ctx.mkNot(ctx.mkEq(ctx.mkIntConst("j"), ctx.mkIntConst("k")));
				Expr indexNotEq3 = ctx.mkNot(ctx.mkEq(ctx.mkIntConst("k"), ctx.mkIntConst("i")));

				Expr isNotNulli, isNotNullj, isNotNullk;

				if (mutColumn.getCvcDatatype().equalsIgnoreCase("int")
						|| mutColumn.getCvcDatatype().equalsIgnoreCase("real")) {
					isNotNulli = ctx.mkNot(
							ctxFuncDecls
									.get("CHECKALL_NULL"
											+ (mutColumn.getCvcDatatype().equalsIgnoreCase("Int") ? "Int" : "Real"))
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"i")));

					isNotNullj = ctx.mkNot(
							ctxFuncDecls
									.get("CHECKALL_NULL"
											+ (mutColumn.getCvcDatatype().equalsIgnoreCase("Int") ? "Int" : "Real"))
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"j")));

					isNotNullk = ctx.mkNot(
							ctxFuncDecls
									.get("CHECKALL_NULL"
											+ (mutColumn.getCvcDatatype().equalsIgnoreCase("Int") ? "Int" : "Real"))
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"k")));
				} else {
					isNotNulli = ctx.mkNot(
							ctxFuncDecls
									.get("ISNULL_"
											+ (mutColumn.getCvcDatatype()))
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"i")));
					isNotNullj = ctx.mkNot(
							ctxFuncDecls
									.get("ISNULL_"
											+ mutColumn.getCvcDatatype())
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"j")));
					isNotNullk = ctx.mkNot(
							ctxFuncDecls
									.get("ISNULL_"
											+ (mutColumn.getCvcDatatype()))
									.apply(ConstraintGenerator.genSelectTest2(cvc, mutColumnBQ.getTableName(), ind,
											"k")));
				}
				Expr mutantEq;
				if (!skipMut2)
					mutantEq = ctx
							.mkAnd(ctx.mkEq(aggColi, aggColj), ctx.mkGt(aggColj, ctx.mkAdd(aggColk, ctx.mkInt(1))));
				else
					mutantEq = ctx
							.mkAnd(ctx.mkEq(aggColi, aggColj), ctx.mkNot(ctx.mkEq(aggColk, aggColj)));

				mutantConstrExpr = ctx.mkAnd(cntValidSQ, countMutant, ctx.mkExists(arrForIndex,
						ctx.mkAnd(backGroupByi, backGroupByj, backGroupByk, mutantEq, indexNotEq1, indexNotEq2,
								indexNotEq3, isNotNulli, isNotNullj, isNotNullk),
						1, null, null, null, null));
			}

			constr += "\n\t(assert\n" + mutantConstrExpr.toString() + "\n)\n";

			ConstraintsStr += constr;

			// write correlation update here - parismita
			// Looking at global correlation map and process correlation if is to be
			// processed at current level
			Table sqTable = cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName);
			Set<Node> corrConds = cvc.correlationHashMap.keySet();
			Vector<Node> corrC = new Vector<Node>(corrConds);
			for (Node globalCorrConds : corrC) {
				// correlationStructure cs = cvc.correlationHashMap.get(globalCorrConds);
				if ((globalCorrConds.getLeft().getLevel() == qbt.getLevel()-1) || (globalCorrConds.getRight().getLevel() == qbt.getLevel()-1)) {
					GenerateJoinPredicateConstraints.updateTableAndColumnOfCorrelatedConditionWithAboveLevel(cvc,qbt,
							globalCorrConds, sqTable, qbt.getLevel(), true);
					globalCorrConds.setExpired(true);
				}
				else if(cvc.correlationHashMap.get(globalCorrConds).getQueryType().equalsIgnoreCase("FROM")){
					int leftlevel = globalCorrConds.getLeft().getLevel();
					int rghtlevel = globalCorrConds.getRight().getLevel();
					int nodelevel = globalCorrConds.getLevel();
					int processlevel = cvc.correlationHashMap.get(globalCorrConds).getProcessLevel();
					if(processlevel < qbt.getLevel()){
							GenerateJoinPredicateConstraints.updateTableAndColumnOfCorrelatedConditionWithAboveLevel(cvc,qbt,
							globalCorrConds, sqTable, qbt.getLevel(), true);
							globalCorrConds.setExpired(true);
						}
				}
			}
			FileWriter myWriter = new FileWriter(Configuration.homeDir + "/temp_smt/groupbyconstraints" + ".smt");
			myWriter.write(ConstraintsStr);
			myWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ConstraintsStr;

	}

	private static Expr getConstraintAggregationForMutants(GenerateCVC1 cvc, Column constrColumn, Node n, int index,
			String tupleNo) {
		Expr SQColumnSQi1;
		if (n.getLeft() == null && n.getRight() == null) {
			return ctx.mkTrue();
		} else if (n.getRight() != null && n.getLeft().getType().equalsIgnoreCase("VALUE")) {
			// generate constraints
			// System.out.println(n.getRight().getAgg().getAggExp().getAggFuncFromNode().getFunc());
			String AggColInSQTable = n.getRight().getAgg().getFunc()
					+ n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
			SQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, constrColumn.getTableName(), index, tupleNo);

			return getComparisionConstraintsForOperator(n.getOperator(), ctx.mkInt(n.getRight().getStrConst()),
					SQColumnSQi1);

		}

		else if (n.getLeft() != null && n.getRight().getType().equalsIgnoreCase("VALUE")) {
			// generate constraints
			String AggColInSQTable = n.getLeft().getAgg().getFunc()
					+ n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
			String baseTableOfAgg = n.getLeft().getAgg().getAggExp().getColumn().getTableName();
			SQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, constrColumn.getTableName(), index, tupleNo);

			return getComparisionConstraintsForOperator(n.getOperator(), SQColumnSQi1,
					ctx.mkInt(n.getRight().getStrConst()));

		}
		return ctx.mkTrue();
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getConstraintAggregationForMutants'");
	}

	private static HashMap<String, Object> generateBackwardPassConstraints(GenerateCVC1 cvc, QueryBlockDetails qbt,
			AggregateDataStructure aggregateDS, GroupByNodesData groupByNodeDS, ProjectedNodesData projectedNodeDs,
			int cntColumnBQIndex, int numberOfTuples, Expr checkIfBQTupleIsValid, EnumSort currentSort,
			String indexType) {
		HashMap<String, Object> returnData = new HashMap<String, Object>();
		String functionName3 = "SQTABLE_" + qbt.getLevel() + "_BACKWARD";

		FuncDecl sqTableIBackward = ctx.mkFuncDecl(functionName3, ctx.getIntSort(), ctx.mkBoolSort());
		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			sqTableIBackward = ctx.mkFuncDecl(functionName3, currentSort, ctx.mkBoolSort());
		}
		ctxFuncDecls.put(functionName3, sqTableIBackward);
		String DefinesqTableIBackward = "(define-fun " + functionName3 + "((k1 " + indexType + "))Bool";

		Expr compareExpr3 = ctx.mkLt((ArithExpr) (IntExpr) ctx.mkInt(0), (ArithExpr) ctx.mkIntConst("k1"));
		Expr compareExpr4 = ctx.mkLe((ArithExpr) ctx.mkIntConst("k1"),
				(ArithExpr) (IntExpr) ctx.mkInt(numberOfTuples));

		Expr andExpr3 = ctx.mkAnd((BoolExpr) compareExpr3, (BoolExpr) compareExpr4,
				ctx.mkEq((ArithExpr) smtMap(aggregateDS.arrayNameOfSQInCtx, "k1", -1,
						aggregateDS.attrColumnsOfSQ.size() - 1), ctx.mkInt(1)));
		andExpr3 = ctx
				.mkAnd(ctx.mkEq((ArithExpr) smtMap(aggregateDS.arrayNameOfSQInCtx, "k1", -1,
						aggregateDS.attrColumnsOfSQ.size() - 1), ctx.mkInt(1)));

		Expr[] colValueBQForBackward = new Expr[groupByNodeDS.noOfGroupByColumns];

		Expr[] colValueSQForBackward = new Expr[groupByNodeDS.noOfGroupByColumns];
		BoolExpr[] eqExprBackward = new BoolExpr[groupByNodeDS.noOfGroupByColumns];
		BoolExpr[] orBackward = new BoolExpr[numberOfTuples];
		BoolExpr[] cntColBQ = new BoolExpr[numberOfTuples];
		int i, j;
		for (j = 0; j < numberOfTuples; j++) {
			for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {
				colValueSQForBackward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "k1", -1, i);
				colValueBQForBackward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "", j + 1,
						aggregateDS.attrNamesOfBQ.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
				if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
					eqExprBackward[i] = (BoolExpr) ctx.mkTrue();
				// eqExprBackward[i]=ctx.mkNot((BoolExpr) ctx.mkEq(colValueSQForBackward[i],
				// colValueBQForBackward[i]));
				else
					eqExprBackward[i] = (BoolExpr) ctx.mkEq(colValueSQForBackward[i], colValueBQForBackward[i]);
			}
			cntColBQ[j] = ctx.mkGt(smtMap(aggregateDS.arrayNameOfBQInCtx, "", j + 1, cntColumnBQIndex), ctx.mkInt(0));
			Expr andExpr4 = ctx.mkAnd(eqExprBackward);
			orBackward[j] = ctx.mkAnd((BoolExpr) andExpr4, cntColBQ[j]);
		}

		BoolExpr orExprBackward = ctx.mkOr(orBackward);
		// Expr exitsExpr2 = ctx.mkExists(arrForIndexL1, andExpr4, 1, null, null, null,
		// null);
		Expr impliesExprBackward = ctx.mkImplies((BoolExpr) andExpr3, orExprBackward);

		returnData.put("DefinesqTableIBackward", DefinesqTableIBackward);
		returnData.put("colValueSQForBackward", colValueSQForBackward);
		returnData.put("eqExprBackward", eqExprBackward);
		returnData.put("sqTableIBackward", sqTableIBackward);
		returnData.put("impliesExprBackward", impliesExprBackward);
		returnData.put("colValueBQForBackward", colValueBQForBackward);
		return returnData;
	}

	private static HashMap<String, Object> generateForwardPassConstraints(GenerateCVC1 cvc, QueryBlockDetails qbt,
			AggregateDataStructure aggregateDS, GroupByNodesData groupByNodeDS,
			ProjectedNodesData projectedNodeDs, int cntColumnBQIndex, int numberOfTuples,
			Expr checkIfBQTupleIsValid, EnumSort currentSort, String indexType, int countValue) {

		Boolean isExist = Configuration.getProperty("existsUnrollFlag").equalsIgnoreCase("true");
		// isExist = false;
		HashMap<String, Object> retunData = new HashMap<String, Object>();

		int i;
		String functionName2 = "SQTABLE_" + qbt.getLevel() + "_FORWARD";
		// for enum_int - Sunanda

		FuncDecl sqTableIForward = ctx.mkFuncDecl(functionName2, ctx.getIntSort(), ctx.mkBoolSort());

		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			sqTableIForward = ctx.mkFuncDecl(functionName2, currentSort, ctx.mkBoolSort());
		}
		ctxFuncDecls.put(functionName2, sqTableIForward);

		String DefinesqTableIForward = "(define-fun " + functionName2 + "((i1 " + indexType + "))Bool";

		// selectCntBQ = smtMap(arrNameBQ, "i1", -1, cntColumnBQIndex);

		Expr[] arrForIndexj1 = new Expr[] { ctx.mkIntConst("j1") };
		Expr[] arrForIndexK1 = new Expr[] { ctx.mkIntConst("k1") };
		Expr[] arrForIndexL1 = new Expr[] { ctx.mkIntConst("l1") };

		if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
			arrForIndexj1 = new Expr[] { ctx.mkConst("j1", currentSort) };
			arrForIndexK1 = new Expr[] { ctx.mkConst("k1", currentSort) };
			arrForIndexL1 = new Expr[] { ctx.mkConst("l1", currentSort) };
		}
		Expr compareExpr1 = ctx.mkLt((ArithExpr) (IntExpr) ctx.mkInt(0), (ArithExpr) ctx.mkIntConst("j1"));
		Expr compareExpr2 = ctx.mkLe((ArithExpr) ctx.mkIntConst("j1"),
				(ArithExpr) (IntExpr) ctx.mkInt(numberOfTuples));
		// Expr andExpr1 = ctx.mkAnd((BoolExpr) compareExpr1, (BoolExpr)compareExpr2,
		// (BoolExpr) ctx.mkEq(smtMap(arrNameSQ, "j1", -1, attrNamesSQ.length-1),
		// ctx.mkInt(1)));

		Expr[] colValueBQForForward = new Expr[groupByNodeDS.noOfGroupByColumns];
		Expr[] colValueSQForForward = new Expr[groupByNodeDS.noOfGroupByColumns];
		BoolExpr[] eqExprForward = new BoolExpr[groupByNodeDS.noOfGroupByColumns + 1];
		// System.out.println();

		Expr exitsExpr1;
		if (isExist) {
			Expr[] andExpr2 = new Expr[numberOfTuples];
			for (int j = 1; j <= numberOfTuples; j++) {
				for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {
					colValueSQForForward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "", j, i);

					colValueBQForForward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "i1", -1,
							aggregateDS.attrNamesOfBQ
									.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
					if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
						eqExprForward[i] = (BoolExpr) ctx.mkTrue();

					else
						eqExprForward[i] = (BoolExpr) ctx.mkEq(colValueSQForForward[i], colValueBQForForward[i]);

				}
				if (countValue == 1) {
					eqExprForward[i] = (BoolExpr) ctx.mkEq(
							smtMap(aggregateDS.arrayNameOfSQInCtx, "", j, aggregateDS.attrColumnsOfSQ.size() - 1),
							ctx.mkInt(1));
				} else
					eqExprForward[i] = (BoolExpr) ctx.mkGe(
							smtMap(aggregateDS.arrayNameOfSQInCtx, "", j, aggregateDS.attrColumnsOfSQ.size() - 1),
							ctx.mkInt(2));
				andExpr2[j - 1] = ctx.mkAnd(eqExprForward);
			}
			exitsExpr1 = ctx.mkOr(andExpr2);

		} else {

			for (i = 0; i < groupByNodeDS.noOfGroupByColumns; i++) {
				colValueSQForForward[i] = smtMap(aggregateDS.arrayNameOfSQInCtx, "j1", -1, i);

				colValueBQForForward[i] = smtMap(aggregateDS.arrayNameOfBQInCtx, "i1", -1,
						aggregateDS.attrNamesOfBQ.indexOf(groupByNodeDS.groupByColumnsFromJSQ.get(i).getColumnName()));
				if (groupByNodeDS.groupByNodes.get(i).getIsMutant())
					eqExprForward[i] = (BoolExpr) ctx.mkTrue();

				else
					eqExprForward[i] = (BoolExpr) ctx.mkEq(colValueSQForForward[i], colValueBQForForward[i]);

			}

			eqExprForward[i] = (BoolExpr) ctx.mkEq(
					smtMap(aggregateDS.arrayNameOfSQInCtx, "j1", -1, aggregateDS.attrColumnsOfSQ.size() - 1),
					ctx.mkInt(1));
			Expr andExpr2 = ctx.mkAnd(eqExprForward[i]);

			exitsExpr1 = ctx.mkExists(arrForIndexj1, andExpr2, 1, null, null, null, null);

		}

		retunData.put("exitsExpr1", exitsExpr1);
		retunData.put("DefinesqTableIForward", DefinesqTableIForward);
		retunData.put("sqTableIForward", sqTableIForward);
		return retunData;
	}

	private static HashMap<String, Object> generateAggregateConstraints(GenerateCVC1 cvc, QueryBlockDetails qbt,
			AggregateDataStructure aggregateDS,
			ProjectedNodesData projectedNodeDs, GroupByNodesData groupByNodeDS, Expr[] arrForIndexI, int numberOfTuples,
			int cntColumnBQIndex) {
		// TODO Auto-generated method stub
		HashMap<String, Object> returnData = new HashMap<String, Object>();
		Vector<Expr> assertAggValDef = new Vector<Expr>();
		Vector<Expr> assertAggValCall = new Vector<Expr>();
		Vector<String> functionName = new Vector<String>();
		Vector<FuncDecl> DeclAssertAggVal = new Vector<FuncDecl>(); // Function declaration for asserting agg val
		Vector<String> DefineAssertAggVal = new Vector<String>(); // Function defination for asserting agg val fun

		HashMap<String, Boolean> isPresent = new HashMap<String, Boolean>();
		int i, j;
		// for(i=0; i<projectedNodes.size(); i++){
		// if (projectedColumnNodes[i].getAgg().getFunc().equalsIgnoreCase("SUM"))
		// isPresent.putIfAbsent("SUM"+projectedColumnNodes[i].getColumn().getTableName()+"_"+projectedColumnNodes[i].getColumn().getColumnName(),
		// false);
		// if (projectedColumnNodes[i].getAgg().getFunc().equalsIgnoreCase("COUNT"))
		// isPresent.putIfAbsent("COUNT"+projectedColumnNodes[i].getColumn().getTableName()+"_"+projectedColumnNodes[i].getColumn().getColumnName(),
		// false);

		// }
		if (projectedNodeDs.projectedNodes.size() > 0) {

			// if (isJoin) {
			// if (subqueryType.equalsIgnoreCase("from")) {
			// String joinTable =
			// cvc.subqueryConstraintsMap.get("from").SQTableName.keySet().iterator().next();
			// for (int q = 0; q <
			// cvc.subqueryConstraintsMap.get("from").SQColumns.get(joinTable).size(); q++)
			// {
			// attrNamesBQ.set(q, removeAllDigit(attrNamesBQ.get(q).split("__")[1]));
			// }
			// } else if (subqueryType.equalsIgnoreCase("outer")) {
			// String joinTable =
			// cvc.subqueryConstraintsMap.get("outer").SQTableName.keySet().iterator().next();
			// for (int q = 0; q <
			// cvc.subqueryConstraintsMap.get("outer").SQColumns.get(joinTable).size(); q++)
			// {
			// attrNamesBQ.set(q, removeAllDigit(attrNamesBQ.get(q).split("__")[1]));
			// }
			// }
			// else if (subqueryType.equalsIgnoreCase("where")) {
			// String joinTable =
			// cvc.subqueryConstraintsMap.get("where").SQTableName.keySet().iterator().next();
			// for (int q = 0; q <
			// cvc.subqueryConstraintsMap.get("where").SQColumns.get(joinTable).size(); q++)
			// {
			// attrNamesBQ.set(q, removeAllDigit(attrNamesBQ.get(q).split("__")[1]));
			// }
			// }
			// }
			for (i = 0; i < projectedNodeDs.projectedNodes.size(); i++) {
				isPresent.put("SUM" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
						+ projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName(), false);
				isPresent.put("COUNT" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
						+ projectedNodeDs.projectedNodes.get(i).getColumn(), false);

			}

			for (i = 0; i < projectedNodeDs.projectedNodes.size(); i++) {

				if (projectedNodeDs.projectedNodes.get(i).getAgg().getFunc().equalsIgnoreCase("SUM")) {
					// function definations if it is SUM
					// isPresent.putIfAbsent("SUM"+projectedColumnNodes[i].getColumn().getColumnName(),
					// true);
					if (isPresent.get("SUM" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
							+ projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName()))
						continue;
					isPresent.put("SUM" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
							+ projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName(), true);

					putFunDeclAndAssertInStruct(qbt, i, "SUM", projectedNodeDs.projectedNodes, functionName,
							DeclAssertAggVal,
							ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

					assertAggValDef.add(getAggConstrintsForSUM(cvc, aggregateDS, groupByNodeDS, projectedNodeDs, i,
							numberOfTuples, cntColumnBQIndex, false));
				}

				else if (projectedNodeDs.projectedNodes.get(i).getAgg().getFunc().equalsIgnoreCase("COUNT")) {
					if (isPresent.get("COUNT" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName()
							+ "_" + projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName()))
						continue;
					// ifCountPresent = true;
					isPresent.put("COUNT" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
							+ projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName(), true);
					putFunDeclAndAssertInStruct(qbt, i, "COUNT", projectedNodeDs.projectedNodes, functionName,
							DeclAssertAggVal,
							ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

					assertAggValDef.add(getAggConstrintsForCOUNT(cvc, aggregateDS, groupByNodeDS, projectedNodeDs,
							i, numberOfTuples, cntColumnBQIndex, false));
				}

				else if (projectedNodeDs.projectedNodes.get(i).getAgg().getFunc().equalsIgnoreCase("MIN")) {
					putFunDeclAndAssertInStruct(qbt, i, "MIN", projectedNodeDs.projectedNodes, functionName,
							DeclAssertAggVal,
							ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

					assertAggValDef.add(getAggConstrintsForMINMAX(cvc, aggregateDS, groupByNodeDS, projectedNodeDs,
							i, numberOfTuples, cntColumnBQIndex,
							projectedNodeDs.projectedNodes.get(i).getAgg().getFunc()));

				}

				else if (projectedNodeDs.projectedNodes.get(i).getAgg().getFunc().equalsIgnoreCase("MAX")) {
					putFunDeclAndAssertInStruct(qbt, i, "MAX", projectedNodeDs.projectedNodes, functionName,
							DeclAssertAggVal,
							ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

					assertAggValDef.add(getAggConstrintsForMINMAX(cvc, aggregateDS, groupByNodeDS, projectedNodeDs,
							i, numberOfTuples, cntColumnBQIndex,
							projectedNodeDs.projectedNodes.get(i).getAgg().getFunc()));

				}

				else if (projectedNodeDs.projectedNodes.get(i).getAgg().getFunc().equalsIgnoreCase("AVG")) {

					if (!isPresent.get("SUM" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName()
							+ "_" + projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName())) {
						putFunDeclAndAssertInStruct(qbt, i, "SUM", projectedNodeDs.projectedNodes, functionName,
								DeclAssertAggVal,
								ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

						Expr constraintsForSum = getAggConstrintsForSUM(cvc, aggregateDS, groupByNodeDS,
								projectedNodeDs, i, numberOfTuples, cntColumnBQIndex, true);
						assertAggValDef.add(constraintsForSum);

						isPresent.put("SUM" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName() + "_"
								+ projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName(), true);
					}

					if (!isPresent.get("COUNT" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName()
							+ "_" + projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName())) {
						putFunDeclAndAssertInStruct(qbt, i, "COUNT", projectedNodeDs.projectedNodes, functionName,
								DeclAssertAggVal,
								ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

						Expr constraintsForCount = getAggConstrintsForCOUNT(cvc, aggregateDS, groupByNodeDS,
								projectedNodeDs, i, numberOfTuples, cntColumnBQIndex, true);

						assertAggValDef.add(constraintsForCount);

						isPresent.put("COUNT" + projectedNodeDs.projectedNodes.get(i).getColumn().getTableName()
								+ "_" + projectedNodeDs.projectedNodes.get(i).getColumn().getColumnName(), true);
					}

					putFunDeclAndAssertInStruct(qbt, i, "AVG", projectedNodeDs.projectedNodes, functionName,
							DeclAssertAggVal,
							ctxFuncDecls, DefineAssertAggVal, assertAggValCall, arrForIndexI);

					assertAggValDef.add(getAggConstrintsForAVG(cvc, aggregateDS, groupByNodeDS, projectedNodeDs, i,
							numberOfTuples, cntColumnBQIndex,
							projectedNodeDs.projectedNodes.get(i).getAgg().getFunc()));

				}

			}
		}

		returnData.put("DefineAssertAggVal", DefineAssertAggVal);
		returnData.put("DeclAssertAggVal", DeclAssertAggVal);
		returnData.put("functionName", functionName);
		returnData.put("assertAggValCall", assertAggValCall);
		returnData.put("assertAggValDef", assertAggValDef);

		return returnData;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'generateAggregateConstraints'");
	}

	private static String getPrimaryKeyConstraintsForGSQTable(GenerateCVC1 cvc,
			AggregateDataStructure aggregateDS, String indexType, Integer numberOfTuples) {
		Boolean primaryKeyMethod = false;
		Vector<String> primaryKeySQ = aggregateDS.primaryKeyColumnsofSQ;
		Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
				aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), "i1");
		Expr cntOfSQColumnSQj1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
				aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), "j1");

		Expr orExprTempForPK = ConstraintGenerator.ctx.mkOr(
				ConstraintGenerator.ctx.mkEq((ArithExpr) cntOfSQColumnSQi1,
						(ArithExpr) ConstraintGenerator.ctx.mkInt(0)),
				ConstraintGenerator.ctx.mkEq((ArithExpr) cntOfSQColumnSQj1,
						(ArithExpr) ConstraintGenerator.ctx.mkInt(0)));

		BoolExpr[] andexprConstraintsTemp = new BoolExpr[primaryKeySQ.size() + 1];
		int p = 0;
		String primaryKeyConstraintsOfSQ = "";

		primaryKeySQ = aggregateDS.primaryKeyColumnsofSQ;

		if (primaryKeySQ.size() > 0) {
			for (int i = 0; i < primaryKeySQ.size(); i++) {
				Expr selecti1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
						aggregateDS.attrColumnsOfSQ.indexOf(primaryKeySQ.get(i)), "i1");
				;
				Expr selectj1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
						aggregateDS.attrColumnsOfSQ.indexOf(primaryKeySQ.get(i)), "j1");
				;
				andexprConstraintsTemp[p] = (BoolExpr) ConstraintGenerator.ctx.mkEq(selecti1, selectj1);
				p++;
			}
			for (int k = primaryKeySQ.size(); k < andexprConstraintsTemp.length; k++) {
				andexprConstraintsTemp[k] = ctx.mkTrue();
			}
			andexprConstraintsTemp[p] = ConstraintGenerator.ctx.mkNot(ConstraintGenerator.ctx.mkEq(
					(Expr) ConstraintGenerator.ctx.mkIntConst("i1"),
					(Expr) ConstraintGenerator.ctx.mkIntConst("j1")));

			Expr andExprTempForPK = ConstraintGenerator.ctx.mkAnd(andexprConstraintsTemp);

			String pkconstUsingNeq = "";
			if (primaryKeyMethod == true) {
				ArrayList<Column> primaryKeycols = new ArrayList<>();
				for (String a : primaryKeySQ) {
					primaryKeycols.add(cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName).getColumn(a));
				}
				pkconstUsingNeq = AddDataBaseConstraints.getPrimaryKeyConstUsingNonEquiMethod(primaryKeycols,
						numberOfTuples);

			}
			if (pkconstUsingNeq.equalsIgnoreCase("")) {
				primaryKeyConstraintsOfSQ += "\n(assert (forall ((i1 " + indexType + ") (j1 " + indexType
						+ ")) \n\t (=> \n\t"
						+ andExprTempForPK.toString() + "\n\t\t" + orExprTempForPK + ")))\n\n";

			} else {
				if (pkconstUsingNeq.contains("select"))
					primaryKeyConstraintsOfSQ = "\n(assert " + pkconstUsingNeq + ")\n";
				else
					primaryKeyConstraintsOfSQ = "";
			}

			// ***************************************************************************************************
			// function for count either equal to 0 or 1

			

			// ***************************************************************************************************

		}

		Expr cnt0or1 = ctx.mkOr(ctx.mkEq(cntOfSQColumnSQi1, ctx.mkInt(0)),
					ctx.mkEq(cntOfSQColumnSQi1, ctx.mkInt(1)));
		primaryKeyConstraintsOfSQ += "\n(assert (forall ((i1 " + indexType + ")) \n\t " + cnt0or1.toString()
					+ "))\n\n";

		return primaryKeyConstraintsOfSQ;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getPrimaryKeyConstraintsForGSQTable'");
	}

	private static void getTransformedAttributesFromRefToJSQTables(GenerateCVC1 cvc, ArrayList attrNamesBQArrayList,
			Vector<String> groupby_column, ArrayList<Node> groupByNodes, Vector<String> agg_column_name,
			ArrayList<Node> projectedNodes, String subqueryType) {
		if (subqueryType.equalsIgnoreCase(subqueryType)) {
			String agg_table = cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
			Integer numberOfTuples = cvc.getNoOfTuples()
					.get(cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next());
			attrNamesBQArrayList = cvc.subqueryConstraintsMap.get(subqueryType).SQColumns.get(agg_table);
			for (int i = 0; i < groupByNodes.size(); i++) {
				for (int j = 0; j < attrNamesBQArrayList.size(); j++) {

					if (attrNamesBQArrayList.get(j).toString().contains(groupByNodes.get(i).getColumn().getColumnName())
							&& attrNamesBQArrayList.get(j).toString()
									.contains(groupByNodes.get(i).getColumn().getTableName())) {
						groupby_column.set(i, attrNamesBQArrayList.get(j).toString());
						break;
					}
				}

			}
			for (int i = 0; i < projectedNodes.size(); i++) {
				for (int j = 0; j < attrNamesBQArrayList.size(); j++) {

					if (attrNamesBQArrayList.get(j).toString()
							.contains(projectedNodes.get(i).getColumn().getColumnName())
							&& attrNamesBQArrayList.get(j).toString()
									.contains(projectedNodes.get(i).getColumn().getTableName())) {
						agg_column_name.set(i, attrNamesBQArrayList.get(j).toString());
						break;
					}
				}

			}
		}
	}

	// SqorBq = 1 if is asking for count index in Sq else anything
	private static int getIndexOfMutantColumnCount(Vector<String> attrNamesSQ, MutationStructure currentMutant,
			String checkAggFun,
			int SqorBq) {
		// TODO Auto-generated method stub
		// Vector<Integer> index = new Vector<Integer>();
		// int ind = 0;

		int i = 0;
		int k = 0;
		// Node n = (Node)currentMutant.getMutationLoc();
		// String aggFun = n.getAggFuncFromNode().getFunc();
		String aggFun = checkAggFun;
		Column c = (Column) currentMutant.getMutationNode();
		for (int j = 0; j < attrNamesSQ.size(); j++) {
			if (SqorBq == 1 && attrNamesSQ.get(j).contains(aggFun)
					&& attrNamesSQ.get(j).contains(removeAllDigit(c.getColumnName()))) {
				i = j;
				break;
			}
			if (SqorBq != 1 && attrNamesSQ.get(j).contains(removeAllDigit(c.getColumnName()))
					&& attrNamesSQ.get(j).contains(c.getTable().getTableName())) {
				i = j;
				break;
			}
		}
		return i;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getIndexOfMutantColumn'");
	}

	private static void putFunDeclAndAssertInStruct(QueryBlockDetails qbt, int indexOfProjectedNode, String operation,
			ArrayList<Node> projectedColumnNodes, Vector<String> functionName, Vector<FuncDecl> declAssertAggVal,
			HashMap<String, FuncDecl> ctxFuncDecls2, Vector<String> defineAssertAggVal, Vector<Expr> assertAggValCall,
			Expr[] arrForIndexI) {
		String indexType = "Int";
		if (!functionName.contains(
				"GETAGGVAL" + operation + projectedColumnNodes.get(indexOfProjectedNode).getColumn().getColumnName()
						+ qbt.getLevel())) {
			functionName.add(
					"GETAGGVAL" + operation + projectedColumnNodes.get(indexOfProjectedNode).getColumn().getColumnName()
							+ qbt.getLevel()); // function names

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(enumArrayIndex);
				declAssertAggVal
						.add(ctx.mkFuncDecl(functionName.get(functionName.size() - 1), currentSort, ctx.mkBoolSort())); // creating
																														// function
				indexType = enumArrayIndex; // declarations
			} else {
				declAssertAggVal
						.add(ctx.mkFuncDecl(functionName.get(functionName.size() - 1), ctx.getIntSort(),
								ctx.mkBoolSort())); // creating
													// function
													// declarations

			}
			ctxFuncDecls.put(operation, declAssertAggVal.get(declAssertAggVal.size() - 1)); // putting function
			// declaration in context
			defineAssertAggVal
					.add("(define-fun " + functionName.get(functionName.size() - 1) + "((i " + indexType + "))Bool"); // function
			// defination
			// using
			// "define-fun"

			assertAggValCall.add(declAssertAggVal.get(declAssertAggVal.size() - 1).apply(arrForIndexI));
		}

	}

	private static Expr getAggConstrintsForAVG(GenerateCVC1 cvc, AggregateDataStructure aggDs, GroupByNodesData gbDs,
			ProjectedNodesData prDs,
			int i, int numberOfTuples, int cntColumnBQIndex, String func) {

		int numberOfGroupByColumns = gbDs.noOfGroupByColumns;
		String subquery_table_name = aggDs.subqueryTableName;
		String arrNameSQ = aggDs.arrayNameOfSQInCtx;
		String arrNameBQ = aggDs.arrayNameOfBQInCtx;
		Vector<Integer> indexOfProjectedColmnsInSQ = prDs.indexOfProjectedColmnsInSQ;
		Vector<String> attrNamesBQArrayList = aggDs.attrNamesBQ;
		Vector<String> agg_column_name = new Vector<String>();
		for (Column c : prDs.aggregateColumnsFromJSQ) {
			agg_column_name.add(c.getColumnName());
		}
		Vector<Column> agg_columns_reftables = prDs.aggregateColumnsFromRefTables;

		Vector<String> attrNamesSQ = aggDs.attrColumnsOfSQ;
		Vector<String> groupby_column = new Vector<String>();
		Vector<Sort> attrTypesSQ = aggDs.attrTypesOfSQ;
		HashMap<String, Boolean> agg_column_isDictinct = aggDs.isColumnDistinctInSQ;

		ArrayList<Node> groupByNodes = gbDs.groupByNodes;

		for (Column c : gbDs.groupByColumnsFromJSQ) {
			groupby_column.add(c.getColumnName());
		}

		String attrCountAggFun = getColumnNameForGSQ(subquery_table_name, agg_columns_reftables.get(i).getTableName(),
				"COUNT", removeAllDigit(agg_column_name.get(i).split("__")[1]));
		// subquery_table_name + "_COUNT" +
		// agg_columns_reftables.get(i).getTableName()+"__" +
		// removeAllDigit(agg_column_name.get(i).split("__")[1]);
		String attrSumAggFun = getColumnNameForGSQ(subquery_table_name, agg_columns_reftables.get(i).getTableName(),
				"SUM", removeAllDigit(agg_column_name.get(i).split("__")[1]));

		String attrAvgAggFun = getColumnNameForGSQ(subquery_table_name, agg_columns_reftables.get(i).getTableName(),
				"AVG", removeAllDigit(agg_column_name.get(i).split("__")[1]));
		// IntExpr[] iColArray = new IntExpr[] { ctx.mkIntConst("i") };
		// List<String> list = Arrays. asList(attrNamesSQ);
		// Vector<String> attrNamesSQVec = new Vector<String>( list );

		Expr eqExpr = ctx.mkEq(smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(attrAvgAggFun)),
				ctx.mkDiv(smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(attrSumAggFun)),
						smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(attrCountAggFun))));

		Expr iteExpr = ctx.mkITE(ctxFuncDecls.get("ISNULL_" + removeAllDigit(agg_column_name.get(i).split("__")[1]))
				.apply(smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(attrSumAggFun))),
				ctxFuncDecls.get("ISNULL_" + removeAllDigit(agg_column_name.get(i).split("__")[1]))
						.apply(smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(attrAvgAggFun))),
				eqExpr);

		Expr mkInvalid = ctx.mkEq(smtMap(arrNameSQ, "i", -1, attrNamesSQ.size() - 1), ctx.mkInt(0));

		// System.out.println(eqExpr.toString());

		return ctx.mkOr(iteExpr, mkInvalid);
	}

	private static HashMap<String, Object> createTempTableColumnsForAggregates(AggregateDataStructure agDs,
			GroupByNodesData gbDS, ProjectedNodesData prDS, String subqueryType) {
		// String aggTable,
		// ArrayList<Node> groupByNodes, ArrayList<Node> projectedNodes

		HashMap<String, Object> columnNameAndDataTypes = new HashMap<String, Object>();
		Vector<String> columns = new Vector<String>();
		Vector<Sort> columnDataTypes = new Vector<Sort>();
		Vector<String> primaryKey = new Vector<String>();
		HashMap<String, Integer> AggColIndexMap = new HashMap<String, Integer>();
		HashMap<String, Boolean> agg_column_isDictinct = new HashMap<String, Boolean>();
		Vector<String> agg_column_name = new Vector<String>();
		Vector<String> groupby_column = new Vector<String>();
		int count = 0;
		String columnName;

		String subqueryTableName = agDs.getTableNameOfSQ();
		ArrayList<Node> groupByNodes = gbDS.getGroupByNodes();
		ArrayList<Node> projectedNodes = prDS.getProjectedNodes();

		// int[] indexOfProjectedColmnsInSQ = new int[projectedNodes.size()]; // -
		// comment

		Table sqi = new Table(subqueryTableName);
			
		Map<String, Table> tableMap = new HashMap<String, Table>();
		int i;
		for (i = 0; i < groupByNodes.size(); i++) {
			gbDS.groupByColumnsFromRefTables.add(groupByNodes.get(i).getColumn());
			// groupby_column.add(groupByNodes.get(i).getColumn().getColumnName()); //
			// comment

			columnName = getColumnNameForGSQ(subqueryTableName, groupByNodes.get(i).getColumn().getTableName(), "",
					groupByNodes.get(i).getColumn().getColumnName());
			// columnName = subqueryTableName + "_" +
			// groupByNodes.get(i).getColumn().getTableName() + "__"
			// + groupByNodes.get(i).getColumn().getColumnName() + i;
			columns.add(columnName);
			String groupby_column_dt = groupByNodes.get(i).getColumn().getCvcDatatype();
			columnDataTypes.add(getColumnSort(groupby_column_dt));

			Column col = new Column(groupByNodes.get(i).getColumn());
			col.setColumnName(columnName);
			col.setTableName(subqueryTableName);
			col.setTable(sqi);
			col.setBaseRelation(groupByNodes.get(i).getColumn().getTable().getTableName());
			sqi.addColumn(col);
			if (!groupByNodes.get(i).getIsMutant()) {
				sqi.addColumnInPrimaryKey(col);
				primaryKey.add(columnName);
			}
		}
		int index = 0;
		for (int j = 0; j < projectedNodes.size(); j++) {
			AggregateFunction aggColumn = projectedNodes.get(j).getAgg();
			String agg_function = aggColumn.getFunc().toString();
			String columnNameBase = "";
			if (projectedNodes.get(j).getOrgiColumn() != null) // in case of transformed corr condition
				columnNameBase = projectedNodes.get(j).getOrgiColumn().getColumnName();
			else
				columnNameBase = projectedNodes.get(j).getColumn().getColumnName();
			prDS.aggregateColumnsFromRefTables.add(aggColumn.getAggExp().getColumn());

			String tableName = "";

			if (projectedNodes.get(j).getOrgiColumn() != null) // in case of transformed corr condition
				tableName = projectedNodes.get(j).getOrgiColumn().getTableName();
			else
				tableName = projectedNodes.get(j).getColumn().getTableName();

			columnName = getColumnNameForGSQ(subqueryTableName, tableName,
					agg_function, columnNameBase);
			// columnName = subqueryTableName + "_" + agg_function
			// +projectedNodes.get(j).getColumn().getTableName() + "__"
			// + columnName;
			if (columns.contains(columnName)) {
				index--;
				continue;
			}

			if (agg_function.equalsIgnoreCase("AVG")) {

				String temColumnName = getColumnNameForGSQ(subqueryTableName,
						tableName, "COUNT",
						columnNameBase);
				if (!columns.contains(temColumnName)) {
					columns.add(
							temColumnName);

					agDs.isColumnDistinctInSQ.put(temColumnName, aggColumn.isDistinct());

					index++;

					agDs.columnToIndexMapInSQ.put(temColumnName, columns.indexOf(temColumnName));
					// AggColIndexMap.put(columnName, i + j); // comment
					Column col = new Column(aggColumn.getAggCol());
					col.setColumnName(temColumnName);
					col.setTableName(subqueryTableName);
					col.setTable(sqi);
					col.setBaseRelation(aggColumn.getAggExp().getColumn().getTable().getTableName());
					// if (agg_function.equalsIgnoreCase("COUNT")) {
					columnDataTypes.add(getColumnSort("INT"));
					col.setCvcDatatype("INT");

					// }

					// else {
					// columnDataTypes.add(getColumnSort(aggColumn.getAggExp().getColumn().getCvcDatatype()));
					// col.setCvcDatatype(aggColumn.getAggExp().getColumn().getCvcDatatype());
					// }

					sqi.addColumn(col);

					prDS.indexOfProjectedColmnsInSQ.add(columns.indexOf(temColumnName));
				}
				temColumnName = getColumnNameForGSQ(subqueryTableName, tableName,
						"SUM", columnNameBase);

				if (!columns.contains(
						temColumnName)) {
					columns.add(
							temColumnName);

					agDs.isColumnDistinctInSQ.put(
							temColumnName,
							aggColumn.isDistinct());

					agDs.columnToIndexMapInSQ.put(
							temColumnName,
							columns.indexOf(temColumnName));

					index++;
					agDs.columnToIndexMapInSQ.put(temColumnName, columns.indexOf(temColumnName));
					// AggColIndexMap.put(columnName, i + j); // comment
					Column col = new Column(aggColumn.getAggCol());
					col.setColumnName(temColumnName);
					col.setTableName(subqueryTableName);
					col.setTable(sqi);
					col.setBaseRelation(aggColumn.getAggExp().getColumn().getTable().getTableName());
					if (agg_function.equalsIgnoreCase("COUNT")) {
						columnDataTypes.add(getColumnSort("INT"));
						col.setCvcDatatype("INT");

					}

					else {
						columnDataTypes.add(getColumnSort(aggColumn.getAggExp().getColumn().getCvcDatatype()));
						col.setCvcDatatype(aggColumn.getAggExp().getColumn().getCvcDatatype());
					}

					sqi.addColumn(col);

					prDS.indexOfProjectedColmnsInSQ.add(columns.indexOf(temColumnName));
				}
				index++;
			}
			if (i + j + index == 0)
				i++;
			columns.add(columnName);
			agDs.isColumnDistinctInSQ.put(columnName, aggColumn.isDistinct());

			columns.indexOf(columnName);
			agDs.columnToIndexMapInSQ.put(columnName, columns.indexOf(columnName));
			// AggColIndexMap.put(columnName, i + j); // comment
			Column col = new Column(aggColumn.getAggCol());
			col.setColumnName(columnName);
			col.setTableName(subqueryTableName);
			col.setTable(sqi);
			col.setBaseRelation(aggColumn.getAggExp().getColumn().getTable().getTableName());
			// agg_column_dt[j] = aggColumn[j].getAggExp().getColumn().getCvcDatatype();
			// agg_table = aggColumn[j].getAggExp().getTable().getTableName();
			if (agg_function.equalsIgnoreCase("COUNT")) {
				columnDataTypes.add(getColumnSort("INT"));
				col.setCvcDatatype("INT");

			}

			else {
				columnDataTypes.add(getColumnSort(aggColumn.getAggExp().getColumn().getCvcDatatype()));
				col.setCvcDatatype(aggColumn.getAggExp().getColumn().getCvcDatatype());
			}

			sqi.addColumn(col);

			prDS.indexOfProjectedColmnsInSQ.add(columns.indexOf(columnName));

		}

		if (Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
			columns.add(subqueryTableName + "_XDATA_CNT");
			columnDataTypes.add(getColumnSort("INT"));
			Column col = new Column(subqueryTableName + "_XDATA_CNT", sqi);
			col.setDataType(4);// INT
			col.setCvcDatatype("INT");
			col.setColumnSize(0);
			agDs.setCountColumnOfSQ(col);

			sqi.addColumn(col);
		}
		sqi.setSQType(subqueryType);
		tableMap.put(subqueryTableName, sqi);

		agDs.primaryKeyColumnsofSQ.addAll(primaryKey);
		agDs.attrColumnsOfSQ.addAll(columns);
		agDs.attrTypesOfSQ.addAll(columnDataTypes);
		columnNameAndDataTypes.put("TableMap", tableMap);

		// comment next
		// columnNameAndDataTypes.put("Names", columns); // - done
		// columnNameAndDataTypes.put("DataTypes", columnDataTypes); // - done
		// columnNameAndDataTypes.put("PrimaryKey", primaryKey); // - done
		// columnNameAndDataTypes.put("indexOfProjectedColmnsInSQ",
		// indexOfProjectedColmnsInSQ);// - done
		// columnNameAndDataTypes.put("AggColIndexMap", AggColIndexMap); // - done
		// columnNameAndDataTypes.put("agg_column_isDictinct", agg_column_isDictinct);
		// // - done
		// columnNameAndDataTypes.put("agg_column_name", agg_column_name); // - done
		// columnNameAndDataTypes.put("groupby_column", groupby_column); // - done

		return columnNameAndDataTypes;
	}

	private static Expr getAggConstrintsForSUM(GenerateCVC1 cvc, AggregateDataStructure aggDs, GroupByNodesData gbDs,
			ProjectedNodesData prDs,
			int indexOfProjectedNode, int numberOfTuples, int cntColumnBQIndex, Boolean callForAVG) {
		int numberOfGroupByColumns = gbDs.noOfGroupByColumns;
		String subquery_table_name = aggDs.subqueryTableName;
		String arrNameSQ = aggDs.arrayNameOfSQInCtx;
		String arrNameBQ = aggDs.arrayNameOfBQInCtx;
		Vector<Integer> indexOfProjectedColmnsInSQ = prDs.indexOfProjectedColmnsInSQ;
		Vector<String> attrNamesBQArrayList = aggDs.attrNamesBQ;
		Vector<String> agg_column_name = new Vector<String>();
		for (Column c : prDs.aggregateColumnsFromJSQ) {
			agg_column_name.add(c.getColumnName());
		}
		Vector<Column> agg_columns_reftables = prDs.aggregateColumnsFromRefTables;
		Vector<String> attrNamesSQ = aggDs.attrColumnsOfSQ;
		Vector<String> groupby_column = new Vector<String>();
		Vector<Sort> attrTypesSQ = aggDs.attrTypesOfSQ;
		HashMap<String, Boolean> agg_column_isDictinct = aggDs.isColumnDistinctInSQ;

		ArrayList<Node> groupByNodes = gbDs.groupByNodes;

		for (Column c : gbDs.groupByColumnsFromJSQ) {
			groupby_column.add(c.getColumnName());
		}

		Expr selectCntBQ;
		Expr[] colValueSQ = new Expr[numberOfGroupByColumns + 1];
		for (int s = 0; s < numberOfGroupByColumns; s++) {
			colValueSQ[s] = smtMap(arrNameSQ, "i", -1, s);
		}
		Expr colValueSQ1;


		String columnNameFromSQorBQ = agg_columns_reftables.get(indexOfProjectedNode).getBaseRelation() == null ? agg_columns_reftables.get(indexOfProjectedNode).getTableName() : agg_columns_reftables.get(indexOfProjectedNode).getBaseRelation();
		String tempColumnName = getColumnNameForGSQ(subquery_table_name,
						columnNameFromSQorBQ, "SUM",
						removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]));
		
		if (!callForAVG)
			colValueSQ1 = smtMap(arrNameSQ, "i", -1, aggDs.attrColumnsOfSQ.indexOf(tempColumnName));
		else {
			// System.out.println(subquery_table_name + "_SUM" +
			// agg_columns_reftables.get(indexOfProjectedNode).getTableName() + "__"
			// + removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]));

			colValueSQ1 = smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(tempColumnName));
		}

		Expr[][] colValueBQ = new Expr[numberOfGroupByColumns + 1][numberOfTuples];
		BoolExpr[][] calculateAggregate = new BoolExpr[numberOfTuples][numberOfGroupByColumns];
		Expr[] ifthenelseExpr = new Expr[numberOfTuples];
		ArithExpr[][] multieExpr = new ArithExpr[numberOfTuples][2];
		Expr[] multiplyexpr = new Expr[numberOfTuples];
		ArithExpr[] addExpr = new ArithExpr[numberOfTuples];
		Expr[] andGroupByAttr = new Expr[numberOfTuples];
		Expr[] andForIfthenelse = new Expr[numberOfTuples];
		Expr[] isNullExprs = new Expr[numberOfTuples];

		String temp = "";
		String functionName = "";
		for (int p = 0; p < numberOfTuples; p++) {
			int s;
			for (s = 0; s < numberOfGroupByColumns; s++) {
				colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1, attrNamesBQArrayList.indexOf(groupby_column.get(s)));
				if (groupByNodes.get(s).getIsMutant())
					calculateAggregate[p][s] = (BoolExpr) ctx.mkTrue();
				// calculateAggregate[p][s] = ctx.mkNot((BoolExpr) ctx.mkEq(colValueSQ[s],
				// colValueBQ[s][p]));
				else
					calculateAggregate[p][s] = (BoolExpr) ctx.mkEq(colValueSQ[s], colValueBQ[s][p]);
				// if (calculateAggregate[p].length == 0) {
				// calculateAggregate[p][0] = ctx.mkTrue();
				// }
			}
			colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
					attrNamesBQArrayList.indexOf(agg_column_name.get(indexOfProjectedNode)));

			// System.out.println("ISNULL_" +
			// removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[0]));
			String dataType = cvc.getTableMap().getSQTableByName(arrNameBQ.split("_")[1])
					.getColumn(agg_column_name.get(indexOfProjectedNode)).getCvcDatatype();
			functionName = dataType;

			// removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]);
			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real"))
				functionName = removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]);

			isNullExprs[p] = ctx.mkNot(
					ctxFuncDecls
							.get("ISNULL_" + functionName)
							.apply(colValueBQ[s][p]));

			selectCntBQ = smtMap(arrNameBQ, "i1", p + 1, cntColumnBQIndex);

			andGroupByAttr[p] = ctx.mkAnd(calculateAggregate[p]);
			andForIfthenelse[p] = ctx.mkAnd(andGroupByAttr[p], isNullExprs[p]);
			String refTableName = removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("_")[1]);

			String aggAttr = tempColumnName;
			if (agg_column_isDictinct.get(aggAttr))
				ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], ctx.mkInt(1), ctx.mkInt(0));
			else
				ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], selectCntBQ, ctx.mkInt(0));

			multieExpr[p][1] = (ArithExpr) colValueBQ[numberOfGroupByColumns][p];
			multieExpr[p][0] = (ArithExpr) ifthenelseExpr[p];
			multiplyexpr[p] = ctx.mkMul(multieExpr[p]);
			addExpr[p] = (ArithExpr) multiplyexpr[p];

		}
		Expr additionexpr1 = ctx.mkAdd(addExpr);
		Expr eqexpr2 = ctx.mkEq(colValueSQ1, additionexpr1);
		Expr eqexprNull = ctxFuncDecls
				.get("ISNULL_" + functionName)
				.apply(colValueSQ1);

		Expr orForFirstIfthenelse = ctx.mkOr(andForIfthenelse);

		Expr orExprGetAggValFinal = ctx.mkOr((BoolExpr) ctx.mkITE(orForFirstIfthenelse, eqexpr2, eqexprNull),
				(BoolExpr) ctx.mkEq((ArithExpr) smtMap(arrNameSQ, "i", -1, attrNamesSQ.size() - 1), ctx.mkInt(0)));

		return orExprGetAggValFinal;

	}

	private static Expr getAggConstrintsForCOUNT(GenerateCVC1 cvc, AggregateDataStructure aggDs, GroupByNodesData gbDs,
			ProjectedNodesData prDs,
			int indexOfProjectedNode, int numberOfTuples, int cntColumnBQIndex, Boolean callForAVG) {

		int numberOfGroupByColumns = gbDs.noOfGroupByColumns;
		String subquery_table_name = aggDs.subqueryTableName;
		String arrNameSQ = aggDs.arrayNameOfSQInCtx;
		String arrNameBQ = aggDs.arrayNameOfBQInCtx;
		Vector<Integer> indexOfProjectedColmnsInSQ = prDs.indexOfProjectedColmnsInSQ;
		Vector<String> attrNamesBQArrayList = aggDs.attrNamesBQ;
		Vector<String> agg_column_name = new Vector<String>();
		for (Column c : prDs.aggregateColumnsFromJSQ) {
			agg_column_name.add(c.getColumnName());
		}
		Vector<String> attrNamesSQ = aggDs.attrColumnsOfSQ;
		Vector<String> groupby_column = new Vector<String>();
		Vector<Sort> attrTypesSQ = aggDs.attrTypesOfSQ;
		HashMap<String, Boolean> agg_column_isDictinct = aggDs.isColumnDistinctInSQ;

		ArrayList<Node> groupByNodes = gbDs.groupByNodes;

		for (Column c : gbDs.groupByColumnsFromJSQ) {
			groupby_column.add(c.getColumnName());
		}
		Vector<Column> agg_columns_reftables = prDs.aggregateColumnsFromRefTables;

		Expr selectCntBQ;
		Expr[] colValueSQ = new Expr[numberOfGroupByColumns + 1];

		for (int s = 0; s < numberOfGroupByColumns; s++) {
			colValueSQ[s] = smtMap(arrNameSQ, "i", -1, s);
		}
		Expr colValueSQ1;
		String columnNameFromSQorBQ = agg_columns_reftables.get(indexOfProjectedNode).getBaseRelation() == null ? agg_columns_reftables.get(indexOfProjectedNode).getTableName() : agg_columns_reftables.get(indexOfProjectedNode).getBaseRelation();
		String tempColumnName = getColumnNameForGSQ(subquery_table_name,
				columnNameFromSQorBQ, "COUNT",
				removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]));

		if (!callForAVG)
			colValueSQ1 = smtMap(arrNameSQ, "i", -1, aggDs.attrColumnsOfSQ.indexOf(tempColumnName));
		else {
			// System.out.println(subquery_table_name +"_COUNT"
			// + agg_columns_reftables.get(indexOfProjectedNode).getTableName()+ "__" +
			// removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]));
			// System.out.println(attrNamesSQ.indexOf(tempColumnName));
			colValueSQ1 = smtMap(arrNameSQ, "i", -1, attrNamesSQ.indexOf(tempColumnName));

		}

		Expr[][] colValueBQ = new Expr[numberOfGroupByColumns + 1][numberOfTuples];
		BoolExpr[][] calculateAggregate = new BoolExpr[numberOfTuples][numberOfGroupByColumns];
		Expr[] ifthenelseExpr = new Expr[numberOfTuples];
		ArithExpr[][] multieExpr = new ArithExpr[numberOfTuples][2];
		Expr[] multiplyexpr = new Expr[numberOfTuples];
		ArithExpr[] addExpr = new ArithExpr[numberOfTuples];
		Expr[] andGroupByAttr = new Expr[numberOfTuples];
		Expr[] andForIfthenelse = new Expr[numberOfTuples];
		Expr[] isNullExprs = new Expr[numberOfTuples];

		for (int p = 0; p < numberOfTuples; p++) {
			int s;

			for (s = 0; s < numberOfGroupByColumns; s++) {
				colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1, attrNamesBQArrayList.indexOf(groupby_column.get(s)));
				if (groupByNodes.get(s).getIsMutant())
					calculateAggregate[p][s] = (BoolExpr) ctx.mkTrue();
				// calculateAggregate[p][s] = ctx.mkNot((BoolExpr) ctx.mkEq(colValueSQ[s],
				// colValueBQ[s][p]));
				else
					calculateAggregate[p][s] = (BoolExpr) ctx.mkEq(colValueSQ[s], colValueBQ[s][p]);
			}

			colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
					attrNamesBQArrayList.indexOf(agg_column_name.get(indexOfProjectedNode)));
			String dataType = cvc.getTableMap().getSQTableByName(arrNameBQ.split("_")[1])
					.getColumn(agg_column_name.get(indexOfProjectedNode)).getCvcDatatype();
			String functionName = dataType;

			// removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]);
			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real")) {
				isNullExprs[p] = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (dataType.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(colValueBQ[s][p]));
			} else {
				isNullExprs[p] = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + functionName)
								.apply(colValueBQ[s][p]));
			}

			andGroupByAttr[p] = ctx.mkAnd(calculateAggregate[p]);
			andForIfthenelse[p] = ctx.mkAnd(andGroupByAttr[p], isNullExprs[p]);

			selectCntBQ = smtMap(arrNameBQ, "i1", p + 1, cntColumnBQIndex);
			// ctxFuncDecls.get("ISNULL_" + )
			String aggAttr = tempColumnName;

			if (agg_column_isDictinct.get(aggAttr))
				ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], ctx.mkInt(1), ctx.mkInt(0));
			else
				ifthenelseExpr[p] = ctx.mkITE((BoolExpr) andForIfthenelse[p], selectCntBQ, ctx.mkInt(0));
			multieExpr[p][0] = (ArithExpr) ifthenelseExpr[p];
			addExpr[p] = (ArithExpr) ifthenelseExpr[p];
		}
		Expr additionexpr1 = ctx.mkAdd(addExpr);
		Expr eqexpr2 = ctx.mkEq((IntExpr) colValueSQ1, additionexpr1);
		Expr orExprGetAggValFinal = ctx.mkOr((BoolExpr) eqexpr2,
				(BoolExpr) ctx.mkEq((ArithExpr) smtMap(arrNameSQ, "i", -1, attrNamesSQ.size() - 1), ctx.mkInt(0)));

		return orExprGetAggValFinal;
	}

	private static Expr getAggConstrintsForMINMAX(GenerateCVC1 cvc, AggregateDataStructure aggDs, GroupByNodesData gbDs,
			ProjectedNodesData prDs, int indexOfProjectedNode,
			int numberOfTuples, int cntColumnBQIndex, String minOrMax) {

		int numberOfGroupByColumns = gbDs.noOfGroupByColumns;
		String subquery_table_name = aggDs.subqueryTableName;
		String arrNameSQ = aggDs.arrayNameOfSQInCtx;
		String arrNameBQ = aggDs.arrayNameOfBQInCtx;
		Vector<Integer> indexOfProjectedColmnsInSQ = prDs.indexOfProjectedColmnsInSQ;
		Vector<String> attrNamesBQArrayList = aggDs.attrNamesBQ;
		Vector<String> agg_column_name = new Vector<String>();
		for (Column c : prDs.aggregateColumnsFromJSQ) {
			agg_column_name.add(c.getColumnName());
		}
		Vector<String> attrNamesSQ = aggDs.attrColumnsOfSQ;
		Vector<String> groupby_column = new Vector<String>();
		Vector<Sort> attrTypesSQ = aggDs.attrTypesOfSQ;
		HashMap<String, Boolean> agg_column_isDictinct = aggDs.isColumnDistinctInSQ;

		ArrayList<Node> groupByNodes = gbDs.groupByNodes;

		for (Column c : gbDs.groupByColumnsFromJSQ) {
			groupby_column.add(c.getColumnName());
		}

		Expr selectCntBQ;
		Expr[] colValueSQ = new Expr[numberOfGroupByColumns + 1];

		for (int s = 0; s < numberOfGroupByColumns; s++) {
			colValueSQ[s] = smtMap(arrNameSQ, "i", -1, s);
		}

		Expr colValueSQ1 = smtMap(arrNameSQ, "i", -1, indexOfProjectedColmnsInSQ.get(indexOfProjectedNode));
		Expr[][] colValueBQ = new Expr[numberOfGroupByColumns + 1][numberOfTuples];
		BoolExpr[][] calculateAggregate = new BoolExpr[numberOfTuples][numberOfGroupByColumns];
		Expr[] andEqualsConditions = new Expr[numberOfTuples];
		Expr[] andGTEqualsConditions = new Expr[numberOfTuples];
		Expr[] andIsNullExprs = new Expr[numberOfTuples];
		Expr[] isNullExprs = new Expr[numberOfTuples];

		ArithExpr[] addExpr = new ArithExpr[numberOfTuples];
		Expr[] andOfGBAttrs = new Expr[numberOfTuples];
		Expr[] impliesExprForEquals = new Expr[numberOfTuples];
		String functionName = "", dataType = "";

		for (int p = 0; p < numberOfTuples; p++) {
			int s;
			for (s = 0; s < numberOfGroupByColumns; s++) {
				colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1, attrNamesBQArrayList.indexOf(groupby_column.get(s)));
				if (groupByNodes.get(s).getIsMutant())
					calculateAggregate[p][s] = (BoolExpr) ctx.mkTrue();

				// calculateAggregate[p][s] = ctx.mkNot((BoolExpr) ctx.mkEq(colValueSQ[s],
				// colValueBQ[s][p]));
				else
					calculateAggregate[p][s] = (BoolExpr) ctx.mkEq(colValueSQ[s], colValueBQ[s][p]);

				// if (calculateAggregate[p].length == 0) {
				// calculateAggregate[p][0] = ctx.mkTrue();
				// }
			}
			colValueBQ[s][p] = smtMap(arrNameBQ, "i", p + 1,
					attrNamesBQArrayList.indexOf(agg_column_name.get(indexOfProjectedNode)));
			dataType = cvc.getTableMap().getSQTableByName(arrNameBQ.split("_")[1])
					.getColumn(agg_column_name.get(indexOfProjectedNode)).getCvcDatatype();
			functionName = dataType;

			// removeAllDigit(agg_column_name.get(indexOfProjectedNode).split("__")[1]);
			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real")) {
				isNullExprs[p] = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (dataType.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(colValueBQ[s][p]));
			} else {
				isNullExprs[p] = // ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + functionName)
								.apply(colValueBQ[s][p]);
			}

			// )

			andOfGBAttrs[p] = ctx.mkAnd(calculateAggregate[p]);
			impliesExprForEquals[p] = ctx.mkImplies(andOfGBAttrs[p], isNullExprs[p]);

			selectCntBQ = smtMap(arrNameBQ, "i1", p + 1, cntColumnBQIndex);
			andEqualsConditions[p] = ctx.mkAnd(ctx.mkAnd(ctx.mkNot(isNullExprs[p]), andOfGBAttrs[p],
					ctx.mkGe(selectCntBQ, ctx.mkInt(1))),
					ctx.mkAnd(ctx.mkEq(colValueSQ1, colValueBQ[numberOfGroupByColumns][p])));
			// impliesExprForEquals[p] = ctx.mkAnd(andEqualsConditions[p],
			// ctx.mkEq(colValueSQ1, colValueBQ[numberOfGroupByColumns][p]));
			if (minOrMax.equalsIgnoreCase("MIN"))
				andGTEqualsConditions[p] = ctx.mkImplies(ctx.mkAnd(andOfGBAttrs[p], ctx.mkNot(isNullExprs[p])),
						ctx.mkLe(colValueSQ1, colValueBQ[numberOfGroupByColumns][p]));
			else
				andGTEqualsConditions[p] = ctx.mkImplies(ctx.mkAnd(andOfGBAttrs[p], ctx.mkNot(isNullExprs[p])),
						ctx.mkGe(colValueSQ1, colValueBQ[numberOfGroupByColumns][p]));

		}

		Expr thenExpr;

		if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real")) {
			thenExpr = ctx.mkNot(
					ctxFuncDecls
							.get("CHECKALL_NULL" + (dataType.equalsIgnoreCase("Int") ? "Int" : "Real"))
							.apply(colValueSQ1));
		} else {
			thenExpr = // ctx.mkNot(
					ctxFuncDecls
							.get("ISNULL_" + functionName)
							.apply(colValueSQ1);
		}

		Expr andOfImplies = ctx.mkAnd(impliesExprForEquals);

		Expr firstAnd = ctx.mkAnd(ctx.mkOr(andEqualsConditions), ctx.mkAnd(andGTEqualsConditions));
		Expr elseExpr = ctx.mkOr(firstAnd,
				ctx.mkEq((ArithExpr) smtMap(arrNameSQ, "i", -1, attrNamesSQ.size() - 1), ctx.mkInt(0)));
		return ctx.mkITE(andOfImplies, thenExpr, elseExpr);
	}

	private static String getAggregateConstraintsWithCount(GenerateCVC1 cvc, Node n, String SQTableName,
			Vector<String> attrNamesSQ, Vector<Sort> attrTypesSQ, HashMap<String, Integer> AggColIndexMap, int i) {
		Expr SQColumnSQi1;
		if (n.getLeft() == null && n.getRight() == null) {
			return "";
		}

		else if (n.getRight() != null && n.getLeft().getType().equalsIgnoreCase("VALUE")) {
			// generate constraints
			// System.out.println(n.getRight().getAgg().getAggExp().getAggFuncFromNode().getFunc());
			String columnName = n.getRight().getAgg().getAggExp().getColumn().getColumnName();
			String AggColInSQTable = n.getRight().getAgg().getFunc()
					+ n.getRight().getAgg().getAggExp().getColumn().getColumnName();

			String aggFun = n.getRight().getAgg().getFunc();
			String columnNameFromBaseTable = n.getRight().getAgg().getAggExp().getColumn().getColumnName();
			String baseTableOfAgg = n.getRight().getAgg().getAggExp().getColumn().getTableName();

			String colN = getColumnNameForGSQ(SQTableName, baseTableOfAgg, aggFun, columnNameFromBaseTable);

			int index = AggColIndexMap.get(colN);
			SQColumnSQi1 = ConstraintGenerator.genSelectTest(cvc, SQTableName, index, i);
			String datatype = cvc.getTableMap().getSQTableByName(SQTableName).getColumn(colN).getCvcDatatype();
			Expr isNotNulli;
			if (datatype.equalsIgnoreCase("int") || datatype.equalsIgnoreCase("real")) {
				isNotNulli = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (datatype.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(SQColumnSQi1));
			} else {
				isNotNulli = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + datatype)
								.apply(SQColumnSQi1));
			}

			return ctx.mkAnd(getComparisionConstraintsForOperator(n.getOperator(),
					ctx.mkInt(n.getRight().getStrConst()), SQColumnSQi1), isNotNulli).toString();

		}

		else if (n.getLeft() != null && n.getRight().getType().equalsIgnoreCase("VALUE")) {
			// generate constraints
			String columnName = n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
			String AggColInSQTable = "";

			String aggFun = n.getLeft().getAgg().getFunc();
			String columnNameFromBaseTable = n.getLeft().getAgg().getAggExp().getColumn().getColumnName();
			String baseTableOfAgg = n.getLeft().getAgg().getAggExp().getColumn().getTableName();
			String colN = getColumnNameForGSQ(SQTableName, baseTableOfAgg, aggFun, columnNameFromBaseTable);

			int index = AggColIndexMap.get(colN);
			SQColumnSQi1 = ConstraintGenerator.genSelectTest(cvc, SQTableName, index, i);

			String datatype = cvc.getTableMap().getSQTableByName(SQTableName).getColumn(colN).getCvcDatatype();
			// n.getLeft().getAgg().getAggExp().getColumn().getCvcDatatype();
			Expr isNotNulli;
			if (datatype.equalsIgnoreCase("int") || datatype.equalsIgnoreCase("real")) {
				isNotNulli = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (datatype.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(SQColumnSQi1));
			} else {
				isNotNulli = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + datatype)
								.apply(SQColumnSQi1));
			}

			return ctx.mkAnd(getComparisionConstraintsForOperator(n.getOperator(), SQColumnSQi1,
					ctx.mkInt(n.getRight().getStrConst())), isNotNulli).toString();

		}

		else if (n.getRight() != null && n.getLeft() != null && n.getType().equalsIgnoreCase("AGGREGATE NODE")) {
			// generate constraints
			Column columnName1 = n.getLeft().getAgg().getAggExp().getColumn();
			Column columnName2 = n.getRight().getAgg().getAggExp().getColumn();
			String AggFun1 = n.getLeft().getAgg().getFunc();
			String AggFun2 = n.getRight().getAgg().getFunc();

			String colN1 = getColumnNameForGSQ(SQTableName, columnName1.getTableName(), AggFun1,
					columnName1.getColumnName());
			String colN2 = getColumnNameForGSQ(SQTableName, columnName2.getTableName(), AggFun2,
					columnName2.getColumnName());

			int index1 = AggColIndexMap.get(colN1);

			int index2 = AggColIndexMap.get(colN2);

			SQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, SQTableName, index1, "i1");
			Expr SQColumnSQi2 = ConstraintGenerator.genSelectTest2(cvc, SQTableName, index2, "i1");

			String datatype1 = cvc.getTableMap().getSQTableByName(SQTableName).getColumn(colN1).getCvcDatatype();
			String datatype2 = cvc.getTableMap().getSQTableByName(SQTableName).getColumn(colN2).getCvcDatatype();

			Expr isNotNulli1;
			Expr isNotNulli2;

			if (datatype1.equalsIgnoreCase("int") || datatype1.equalsIgnoreCase("real")) {
				isNotNulli1 = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (datatype1.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(SQColumnSQi1));
			} else {
				isNotNulli1 = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + datatype1)
								.apply(SQColumnSQi1));
			}
			if (datatype2.equalsIgnoreCase("int") || datatype2.equalsIgnoreCase("real")) {
				isNotNulli2 = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (datatype2.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(SQColumnSQi2));
			} else {
				isNotNulli2 = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + datatype2)
								.apply(SQColumnSQi2));
			}

			return ctx.mkAnd(getComparisionConstraintsForOperator(n.getOperator(), SQColumnSQi1, SQColumnSQi2),
					isNotNulli1, isNotNulli2).toString();

		} else {
			getAggregateConstraintsWithCount(cvc, n.getLeft(), SQTableName, attrNamesSQ, attrTypesSQ, AggColIndexMap, i);
			getAggregateConstraintsWithCount(cvc, n.getRight(), SQTableName, attrNamesSQ, attrTypesSQ, AggColIndexMap, i);
		}
		return "";

	}

	public static String getColumnNameForGSQ(String sQTableName, String baseTableOfAgg, String aggFun,
			String columnNameFromBaseTable) {
		// TODO Auto-generated method stub
		return sQTableName + "_" + aggFun + baseTableOfAgg + "__" + columnNameFromBaseTable;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getColumnNameForGSQ'");
	}

	private static Expr getComparisionConstraintsForOperator(String operator, Expr left, Expr right) {
		if (operator.equalsIgnoreCase("="))
			return ctx.mkEq(left, right);
		if (operator.equalsIgnoreCase("<"))
			return ctx.mkLt(left, right);
		if (operator.equalsIgnoreCase(">"))
			return ctx.mkGt(left, right);
		if (operator.equalsIgnoreCase(">="))
			return ctx.mkGe(left, right);
		if (operator.equalsIgnoreCase("<="))
			return ctx.mkLe(left, right);
		if (operator.equalsIgnoreCase("/="))
			return ctx.mkNot(ctx.mkEq(left, right));
		return null;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getComparisionConstraintsForOperator'");
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	public StringConstraint getStringConstraint(String str) {

		StringConstraint s = null;
		if (isCVC3) {
			s = new StringConstraint(str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"))); // removing brackets
		} else {
			s = new StringConstraint(str.substring(str.indexOf(" (") + 1, str.lastIndexOf(")"))); // removing brackets
			s = new StringConstraint(str.substring(str.indexOf(" (") + 1, str.lastIndexOf(")")));
		}
		return s;
	}

	/**
	 * 
	 * @param var
	 * @return
	 */
	public String getTableName(String var) {

		String table = "";
		String tableName = "";

		if (isCVC3) {
			table = (var.split("\\["))[0];
			tableName = table.split("O_")[1];

		} else {
			tableName = var.substring(0, var.indexOf("_"));
		}
		return tableName;
	}

	/**
	 * 
	 * @param var
	 * @return
	 */
	public int getColumnIndex(String var) {

		int columnIndex = 0;
		if (isCVC3) {
			columnIndex = Integer.parseInt((var.split("\\."))[1]);
		} else {
			String newStr = var.substring(0, var.indexOf(" (select "));
			columnIndex = Integer.parseInt(newStr.substring(newStr.length() - 1, newStr.length()));

		}
		return columnIndex;
	}

	/**
	 * 
	 * @param commentLine
	 * @return
	 */
	public static String addCommentLine(String commentLine) {
		return "\n\n" + solverSpecificCommentCharacter
				+ "------------------------------------------------------------\n"
				+ solverSpecificCommentCharacter + commentLine +
				"\n" + solverSpecificCommentCharacter
				+ "------------------------------------------------------------\n";
	}

	public String getConstraintSolver() {
		return constraintSolver;
	}

	public void setConstraintSolver(String constraintSolver) {
		this.constraintSolver = constraintSolver;
	}

	/**
	 * 
	 * @param queryBlock
	 * @param vn
	 * @param c
	 * @param countVal
	 * @return
	 */
	public static String generateCVCForCNTForPositiveINT(QueryBlockDetails queryBlock, ArrayList<Node> vn, Column c,
			int countVal) {
		String CVCStr = "";
		if (isCVC3) {

			int min = 0, min1 = 0, max = 0, max1 = 0;

			CVCStr += "SUM: INT;\nMIN: INT;\nMAX: INT;\nAVG: REAL;\nCOUNT: INT;";
			CVCStr += "\nMIN1: INT;\nMAX1: INT;\n\n";

			if (countVal == 0) {
				CVCStr += "ASSERT (COUNT <  32);\n";// 30 because CNT is always CNT+2 = 32 (max in CVC)
			} else {
				CVCStr += "ASSERT (COUNT = " + countVal + ");\n";
			}

			CVCStr += "\n\nASSERT (MIN1 <= MIN);\nASSERT (MAX1 >= MAX);\n";
			CVCStr += "ASSERT (COUNT > 0);\nASSERT (MIN <= MAX);\n";
			CVCStr += "ASSERT (MAX1 >= AVG);\nASSERT (AVG >= MIN1);\n";

			CVCStr += "ASSERT (SUM  >= MIN * COUNT);\n " +
					" ASSERT (SUM <= MAX * COUNT);\n";
			CVCStr += "ASSERT (AVG * COUNT = SUM);\n";

			DataType dt = new DataType();

			if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2) && c.getMinVal() != -1) {
				Vector<Node> selectionConds = new Vector<Node>();
				for (ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
					selectionConds.addAll(conjunct.getSelectionConds());

				/** if there is a selection condition on c that limits the min val of c */
				min = (UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[1];
				CVCStr += "\nASSERT (MIN1 = " + min + ");\n";
			}
			if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2) && c.getMaxVal() != -1) {

				Vector<Node> selectionConds = new Vector<Node>();
				for (ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
					selectionConds.addAll(conjunct.getSelectionConds());

				/** if there is a selection condition on c that limits the max val of c */
				max = (UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[0];
				CVCStr += "\nASSERT (MAX1 = " + max + ");\n";
			}
			for (String s : queryBlock.getParamMap().values()) {
				s = s.trim();
				if (s.contains("PARAM")) {
					CVCStr += "\n" + s + ": BITVECTOR(20);";
				}
			}

			for (Node n : vn) {
				if (n.getType().equalsIgnoreCase(Node.getAggrNodeType())
						|| n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())
						|| n.getRight().getNodeType().equalsIgnoreCase(Node.getAggrNodeType())) {
					AggregateFunction agg = null;
					if (n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()))
						agg = n.getLeft().getAgg();
					else
						agg = n.getRight().getAgg();

					if (agg.getAggExp().getLeft() != null
							&& agg.getAggExp().getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())) {

						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMinVal() != -1) {
							min1 = min + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);
							CVCStr = CVCStr.replace("\nASSERT (MIN1 = " + min + ");\n",
									"\nASSERT (MIN1 = " + min1 + ");\n");
						}
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMaxVal() != -1) {
							max1 = max + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);

							CVCStr = CVCStr.replace("\nASSERT (MAX1 = " + max + ");\n",
									"\nASSERT (MAX1 = " + max1 + ");\n");
						}

					} else if (agg.getAggExp().getRight() != null
							&& agg.getAggExp().getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())) {
						// int constValue = this.getConstantVal(n);
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMinVal() != -1) {
							min1 = min + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);
							CVCStr = CVCStr.replace("\nASSERT (MIN1 = " + min + ");\n",
									"\nASSERT (MIN1 = " + min1 + ");\n");
						}
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMaxVal() != -1) {

							max1 = max + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);
							CVCStr = CVCStr.replace("\nASSERT (MAX1 = " + max + ");\n",
									"\nASSERT (MAX1 = " + max1 + ");\n");
						}

					}
				}
				CVCStr += "\nASSERT " + n.toCVCString(10, queryBlock.getParamMap()) + ";";
			}
			CVCStr += "\n\nQUERY FALSE;\nCOUNTEREXAMPLE;\nCOUNTERMODEL;";
			return CVCStr;

		} else {

			int min = 0, min1 = 0, max = 0, max1 = 0;
			CVCStr += "(set-option:produce-models true)\n (set-option :smt.macro_finder true) \n";
			CVCStr += "(declare-const SUM Int) \n (declare-const MIN Int) \n (declare-const MAX Int) \n (declare-const AVG Real) \n (declare-const COUNT Int) \n";
			CVCStr += "(declare-const CNT Real) \n (declare-const MIN1 Int) \n (declare-const MAX1 Int) \n\n";

			if (countVal == 0) {
				CVCStr += "(assert (< COUNT 32))\n";// 30 because CNT is always CNT+2 = 32 (max in CVC)
			} else {
				CVCStr += "(assert (= COUNT " + countVal + "))\n";
			}

			CVCStr += "\n\n(assert (<= MIN1 MIN)) \n ";
			CVCStr += "(assert (>= MAX1 MAX))\n";
			CVCStr += "(assert (> COUNT 0))\n";
			CVCStr += "(assert (<= MIN MAX))\n";
			CVCStr += "(assert (>= MAX1 AVG))\n";
			CVCStr += "(assert (>= AVG MIN1))\n";

			CVCStr += "(assert (>= SUM  (* MIN COUNT)))\n " +
					" (assert (<= SUM (* MAX COUNT)))\n";
			CVCStr += "(assert (= SUM (* AVG COUNT)))\n";

			DataType dt = new DataType();

			if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2) && c.getMinVal() != -1) {
				Vector<Node> selectionConds = new Vector<Node>();
				for (ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
					selectionConds.addAll(conjunct.getSelectionConds());

				/** if there is a selection condition on c that limits the min val of c */
				min = (UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[1];
				CVCStr += "\n(assert (= MIN1 " + min + "))\n";
			}
			if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2) && c.getMaxVal() != -1) {

				Vector<Node> selectionConds = new Vector<Node>();
				for (ConjunctQueryStructure conjunct : queryBlock.getConjunctsQs())
					selectionConds.addAll(conjunct.getSelectionConds());

				/** if there is a selection condition on c that limits the max val of c */
				max = (UtilsRelatedToNode.getMaxMinForIntCol(c, selectionConds))[0];
				CVCStr += "\n(assert (= MAX1 " + max + "))\n";
			}
			for (String s : queryBlock.getParamMap().values()) {
				s = s.trim();
				if (s.contains("PARAM")) {
					CVCStr += "\n (declare-const " + s + " (_ BitVec 32))";
				}
			}

			for (Node n : vn) {
				if (n.getType().equalsIgnoreCase(Node.getAggrNodeType())
						|| n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType())
						|| n.getRight().getNodeType().equalsIgnoreCase(Node.getAggrNodeType())) {
					AggregateFunction agg = null;
					if (n.getLeft().getType().equalsIgnoreCase(Node.getAggrNodeType()))
						agg = n.getLeft().getAgg();
					else
						agg = n.getRight().getAgg();

					if (agg.getAggExp().getLeft() != null
							&& agg.getAggExp().getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType())) {

						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMinVal() != -1) {
							min1 = min + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);
							CVCStr = CVCStr.replace("\n(assert (= MIN1 " + min + ")) \n",
									"\n (assert (= MIN1 " + min1 + "))\n");
						}
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMaxVal() != -1) {
							max1 = max + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);

							CVCStr = CVCStr.replace("\n(assert (= MAX1 " + max + "))\n",
									"\n(assert (= MAX1 " + max1 + "))\n");
						}

					} else if (agg.getAggExp().getRight() != null
							&& agg.getAggExp().getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())) {
						// int constValue = this.getConstantVal(n);
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMinVal() != -1) {
							min1 = min + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[1]);
							CVCStr = CVCStr.replace("\n(assert (= MIN1 " + min + "))\n",
									"\n(assert (= MIN1 " + min1 + "))\n");
						}
						if ((dt.getDataType(c.getDataType()) == 1 || dt.getDataType(c.getDataType()) == 2)
								&& c.getMaxVal() != -1) {

							max1 = max + ((UtilsRelatedToNode.getMaxMinForHaving(c, agg.getAggExp()))[0]);
							CVCStr = CVCStr.replace("\nassert (= MAX1 " + max + "))\n",
									"\n(assert (= MAX1 " + max1 + "))\n");
						}

					}
				}
				CVCStr += "\n(assert (" + n.toSMTString(10, queryBlock.getParamMap()) + "))";
			}

			CVCStr += "\n (check-sat) \n (get-value (SUM)) \n (get-value (MIN)) \n (get-value (MAX)) \n (get-value (AVG)) \n (get-value (COUNT))  \n (get-value (CNT))"
					+ "\n (get-value (MIN1)) \n (get-value (MAX1)) \n \n";
			return CVCStr;

		}
	}

	/**
	 * 
	 * @param filePath
	 * @param cmdString
	 * @param cvc
	 */
	public static void getCountExeFile(String filePath, String cmdString, GenerateCVC1 cvc) {
		Runtime r = Runtime.getRuntime();
		cmdString = "";
		cmdString = "#!/bin/bash\n";

		if (isCVC3) {
			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/getCount.cvc > " + Configuration.homeDir + "/temp_smt" + filePath + "/COUNTCVC \n";
			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/getCount.cvc | grep -e 'Valid' > isNotValid \n";
			cmdString += "grep -e 'COUNT = ' " + Configuration.homeDir + "/temp_smt" + filePath + "/COUNTCVC"
					+ " | awk -F \" \" '{print $4}' | awk -F \")\" '{print $1}' > " + Configuration.homeDir
					+ "/temp_smt" + filePath + "/COUNT\n";
		} else {

			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/getCount.smt > " + Configuration.homeDir + "/temp_smt" + filePath + "/COUNTCVC \n";
			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/getCount.smt | grep -e 'unsat' > isNotValid \n";

			// cmdString += Configuration.smtsolver+" --lang smtlib "+
			// Configuration.homeDir+"/temp_smt" +filePath+ "/getCount.cvc >
			// "+Configuration.homeDir+"/temp_smt" + filePath + "/COUNTCVC \n";
			// cmdString +=Configuration.smtsolver+" --lang smtlib "+
			// Configuration.homeDir+"/temp_smt" + filePath + "/getCount.cvc | grep -e
			// 'unsat' > isNotValid \n";
			cmdString += "grep -e '((COUNT' " + Configuration.homeDir + "/temp_smt" + filePath + "/COUNTCVC"
					+ " | awk -F \" \" '{print $2}' | awk -F \")\" '{print $1}' > " + Configuration.homeDir
					+ "/temp_smt" + filePath + "/COUNT\n";
		}

		Utilities.writeFile(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/execCOUNT", cmdString);
	}

	/**
	 * 
	 * @param filePath
	 * @param cvc
	 */
	public static void getAggConstraintExeFile(String filePath, GenerateCVC1 cvc) {

		String cmdString = "";
		cmdString = "#!/bin/bash\n";
		if (isCVC3) {
			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/checkAggConstraints.cvc | grep -e 'Invalid' > isValid \n";
		} else {
			cmdString += Configuration.smtsolver + " " + Configuration.homeDir + "/temp_smt" + filePath
					+ "/checkAggConstraints.smt | grep -e 'sat' > isValid \n";
			// cmdString += Configuration.smtsolver+" --lang smtlib "+
			// Configuration.homeDir+"/temp_smt" +filePath+ "/checkAggConstraints.cvc | grep
			// -e 'true' > isValid \n";
		}
		Utilities.writeFile(Configuration.homeDir + "/temp_smt" + cvc.getFilePath() + "/checkAggConstraints",
				cmdString);
	}

	/**
	 * 
	 * @param param
	 * @param datatype
	 * @param retVal
	 * @return
	 */
	public static String getParamRelation(String param, String datatype, String retVal, String cvcDataType) {
		String constr = "";
		if (isCVC3) {
			constr = param + " : " + datatype + ";\n" + retVal;
		} else {
			if (datatype.equalsIgnoreCase("INT")) {
				constr += "(declare-const " + param + " Int" + ") \n" + retVal;
			} else if (datatype.equalsIgnoreCase("REAL")) {
				constr += "(declare-const " + param + " Real" + ") \n" + retVal;
			} else {
				if (cvcDataType != null && cvcDataType.equalsIgnoreCase("Int")) {
					constr += "(declare-const " + param + " i_" + datatype + ") \n" + retVal;
				} else if (cvcDataType != null && cvcDataType.equalsIgnoreCase("Real")) {
					constr += "(declare-const " + param + " r_" + datatype + ") \n" + retVal;
				} else {
					constr += "(declare-const " + param + " " + datatype + ") \n" + retVal;
				}
			}
		}
		return constr;
	}

	public String getStrConstWithScale(String strConstant, Integer epsilon, String operator) {
		String strConst = "";
		if (isCVC3) {
			strConst = "(" + strConstant + " " + operator + " 1/" + epsilon + ")";

		} else {
			strConst = "(" + operator + " " + strConstant + " (/ 1 " + epsilon + " )" + ")";
		}
		return strConst;

	}

	public String generateCVCForNullCheckInHaving() {
		if (isCVC3) {
			return "";
		}

		String returnStr = "";
		String[] Datatypes = new String[] { "Real", "Int" };

		for (String Datatype : Datatypes) {
			String columnName = Datatype + "col";
			Expr[] nullArr = null;
			ArithExpr nullVal = null;
			ArithExpr zeroVal = null;
			Sort type = null;

			if (Datatype.endsWith("Real")) {
				nullArr = new Expr[] { ctx.mkRealConst(columnName) };
				type = ctx.getRealSort();
				nullVal = ConstraintGenerator.realNull;
				zeroVal = ctx.mkReal("0.0");
			} else { // Int case
				nullArr = new Expr[] { ctx.mkIntConst(columnName) };
				type = ctx.getIntSort();
				nullVal = ConstraintGenerator.intNull;
				zeroVal = ctx.mkInt(0);
			}

			String checkAllNullName = "CHECKALL_NULL" + Datatype;
			FuncDecl checkAllNull = ctx.mkFuncDecl(checkAllNullName, type, ctx.mkBoolSort());
			ctxFuncDecls.put(checkAllNullName, checkAllNull);

			String maxRepNullName = "MAX_REPLACE_NULL_" + Datatype;
			FuncDecl maxRepNull = ctx.mkFuncDecl(maxRepNullName, type, type);
			ctxFuncDecls.put(maxRepNullName, maxRepNull);

			String sumRepNullName = "SUM_REPLACE_NULL_" + Datatype;
			FuncDecl sumRepNull = ctx.mkFuncDecl(sumRepNullName, type, type);
			ctxFuncDecls.put(sumRepNullName, sumRepNull);

			String minRepNullName = "MIN_REPLACE_NULL_" + Datatype;
			FuncDecl minRepNull = ctx.mkFuncDecl(minRepNullName, type, type);
			ctxFuncDecls.put(minRepNullName, minRepNull);

			// CHECKALL_NULL_*
			Expr checkAllNullCall = checkAllNull.apply(nullArr);
			Expr funcBody = ctx.mkEq(nullArr[0], nullVal);
			Expr quantBody = ctx.mkEq(checkAllNullCall, funcBody);
			Expr funcQuantifier = ctx.mkForall(nullArr, quantBody, 1, null, null, null, null);
			returnStr += "\n" + checkAllNull.toString() + "\n(assert " + funcQuantifier.toString() + ")\n";

			// MAX_REPLACE_NULL_* - why do we need this?
			Expr maxRepNullCall = maxRepNull.apply(nullArr);
			funcBody = ctx.mkITE(ctx.mkEq(nullArr[0], nullVal), nullVal, nullArr[0]);
			quantBody = ctx.mkEq(maxRepNullCall, funcBody);
			funcQuantifier = ctx.mkForall(nullArr, quantBody, 1, null, null, null, null);
			returnStr += "\n" + maxRepNull.toString() + "\n(assert " + funcQuantifier.toString() + ")\n";

			// SUM_REPLACE_NULL_*
			Expr sumRepNullCall = sumRepNull.apply(nullArr);
			funcBody = ctx.mkITE(ctx.mkEq(nullArr[0], nullVal), zeroVal, nullArr[0]);
			quantBody = ctx.mkEq(sumRepNullCall, funcBody);
			funcQuantifier = ctx.mkForall(nullArr, quantBody, 1, null, null, null, null);
			returnStr += "\n" + sumRepNull.toString() + "\n(assert " + funcQuantifier.toString() + ")\n";

			// MIN_REPLACE_NULL_*
			Expr minRepNullCall = minRepNull.apply(nullArr);
			funcBody = ctx.mkITE(ctx.mkEq(nullArr[0], nullVal), ctx.mkSub(zeroVal, nullVal), nullArr[0]);
			quantBody = ctx.mkEq(minRepNullCall, funcBody);
			funcQuantifier = ctx.mkForall(nullArr, quantBody, 1, null, null, null, null);
			returnStr += "\n" + minRepNull.toString() + "\n(assert " + funcQuantifier.toString() + ")\n";
		}

		return returnStr;
	}

	public Vector<Quantifier> getDomainConstraintsforZ3(GenerateCVC1 cvc) {

		Vector<Quantifier> domainConstraints = new Vector<Quantifier>();

		int turn = 0;

		for (int i = 0; i < cvc.getResultsetTables().size(); i++) {
			turn = 0;

			Table table = cvc.getResultsetTables().get(i);

			String tableName = table.getTableName();

			for (String col : table.getColumns().keySet()) {
				if ((table.getColumn(col).getCvcDatatype()).equalsIgnoreCase("INT")
						|| (table.getColumn(col).getCvcDatatype()).equalsIgnoreCase("REAL") || table.getColumn(col).getCheckValues().size() > 0) {
					if (col.equalsIgnoreCase("XDATA_CNT")) // added by sunanda for count
					{
						continue;
					}
					// if (turn++ == 0) {
						Expr[] qVarArray = new Expr[1];

						Expr qVar = ctx.mkIntConst("i"); // i should not conflict with any global i
						qVar = ctx.mkConst("i", ctx.getIntSort());
						// Expr <Sort> currentSort = ctx.mkConst("i",ctx.getIntSort());

						if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
							EnumSort currentSort = (EnumSort) ctxSorts.get(cvc.enumArrayIndex);
							qVar = ctx.mkConst("i", currentSort);
						}
						BoolExpr antecedant;
						if (Configuration.isEnumInt.equalsIgnoreCase("true"))
							antecedant = ctx.mkTrue();
						else {
							BoolExpr ac1 = ctx.mkLe(ctx.mkInt("1"), qVar);
							// BoolExpr ac2 = ctx.mkLe(qVar,
							// ctx.mkInt(Integer.toString(cvc.getNoOfOutputTuples(tableName))));
							// added by rambabu for temporary fix
							BoolExpr ac2;
							try {
								ac2 = ctx.mkLe(qVar, ctx.mkInt(Integer.toString(cvc.getNoOfOutputTuples(tableName))));
							} catch (Exception e) {
								ac2 = ctx.mkLe(qVar,
										ctx.mkInt(Integer.toString(cvc.getNoOfOutputTuples(tableName.toUpperCase()))));
							}
							// added by rambabu ended here
							antecedant = ctx.mkAnd(ac1, ac2);

						}

						FuncDecl getFuncDecl = ctxFuncDecls.get("check" + col);
						FuncDecl isNullFuncDecl = ctxFuncDecls.get("ISNULL_" + col);

						


						Expr selectExpr = ConstraintGenerator.smtMap(table.getColumn(col), qVar);

						BoolExpr tempExpr = ctx.mkTrue();
						if(table.getColumn(col).limitDefined() || table.getColumn(col).getCheckValues().size()!=0)
							tempExpr = (BoolExpr) getFuncDecl.apply(selectExpr) ;
						
						BoolExpr con1 = ctx.mkOr(tempExpr,
								(BoolExpr) isNullFuncDecl.apply(selectExpr));


						BoolExpr consequent = ctx.mkAnd(con1); // if count is false

						// if(Configuration.getProperty("cntFlag").equalsIgnoreCase("true")) {
						// Column cntCol =
						// cvc.getTableMap().getTable(tableName.toUpperCase()).getColumn(cvc.getTableMap().getTable(tableName.toUpperCase()).getNoOfColumn()-1);
						// // added by sunanda for count
						// BoolExpr andWithCount =
						// ConstraintGenerator.ctx.mkGt((ArithExpr)ConstraintGenerator.smtMap(cntCol,
						// qVar), ConstraintGenerator.ctx.mkInt(0)); // added by sunanda for count
						// consequent = ctx.mkAnd(con1, andWithCount); // added by sunanda for count
						//
						// }
						Expr body = ctx.mkImplies(antecedant, consequent);

						qVarArray[0] = qVar;
						Quantifier funcQuantifier = ctx.mkForall(qVarArray, body, 1, null, null, null, null);
						domainConstraints.add(funcQuantifier);
					// }
				}
			}
		}

		return domainConstraints;
	}

	public static Expr putTableInCtx(GenerateCVC1 cvc, String[] attrNames, Sort[] attrTypes, String table_name) {
		String tupleTypeName = table_name + "_TupleType";
		Constructor[] cons = new Constructor[] {
				ctx.mkConstructor(tupleTypeName, "is_" + tupleTypeName, attrNames, attrTypes, null) };
		// fatal error due to below statement
		DatatypeSort tupleType = ctx.mkDatatypeSort(tupleTypeName, cons);
		ctxSorts.put(tupleTypeName, tupleType);
		ArraySort asort = ctx.mkArraySort(ctx.getIntSort(), tupleType);
		if (Configuration.isEnumInt.equalsIgnoreCase("true") && ctxSorts.containsKey(cvc.enumArrayIndex)) {
			asort = ctx.mkArraySort(getColumnSort(cvc.enumArrayIndex), tupleType);
		}
		String arrName = "O_" + table_name;
		Expr aex = ctx.mkConst(arrName, asort);
		ctxSorts.put(arrName, asort);
		ctxConsts.put(arrName, aex);
		// ctxConsts.remove(arrName,aex);
		return aex;
	}

	public static void putEnumSortInContext(String enumArrayIndex, Vector<String> enumArrayIndexData) {
		// TODO Auto-generated method stub
		if (ctxConsts.containsKey(enumArrayIndex)) {
			ctxConsts.remove(enumArrayIndex);
		}
		EnumSort colSort = ctx.mkEnumSort(enumArrayIndex,
				enumArrayIndexData.toArray(new String[enumArrayIndexData.size()]));
		ctxSorts.put(enumArrayIndex, colSort);

	}

	public static String GetDistinctClauseConstraintsForSubqueryTableWithCount(GenerateCVC1 cvc, QueryBlockDetails qbt,
			String subqueryType, String SQtableName) {
		File testFile = null;
		String indexType = "Int";
		if (Configuration.getProperty("isEnumInt").equalsIgnoreCase("true"))
			indexType = cvc.enumArrayIndex;
		// write api code here to generate string
		testFile = new File(Configuration.homeDir + "/temp_smt/groupbyconstraints" + ".smt");
		if (!testFile.exists()) {

			testFile.delete();
		}
		String ConstraintsStr = "";

		try {
			testFile.createNewFile();
			// number of tuples
			int numberOfTuples = 0; // get the number of tuples in base table from cvc
			int numberOfGroups = 0;

			boolean isMutant = false;
			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() == 5) {
				isMutant = true;
			}
			AggregateDataStructure aggregateDS = new AggregateDataStructure(
					SQtableName == "" ? "GSQ" : SQtableName + qbt.getLevel());
			ProjectedNodesData projectedNodeDs = new ProjectedNodesData();

			// ArrayList<Node> groupByNodes; // comment
			ArrayList<Node> projectedNodes = new ArrayList<>();

			int i, j;

			// groupByNodes = qbt.getGroupByNodes(); // comment

			// groupByNodes = GenConstraints.HandleExtraGroupByMutations(groupByNodes,
			// cvc.getCurrentMutant()); // comment

			for (i = 0; i < qbt.getProjectedCols().size(); i++) {
				if (qbt.getProjectedCols().get(i).getAgg() != null) {
					projectedNodes.add(qbt.getProjectedCols().get(i));
				}
			}
			GroupByNodesData groupByNodeDS = new GroupByNodesData(qbt.getGroupByNodes());
			groupByNodeDS.noOfGroupByColumns = qbt.getGroupByNodes().size();

			projectedNodeDs.putProjectedNodes(projectedNodes);
			projectedNodeDs.noOfProjectedColumns = projectedNodes.size();

			// to handle extra group by column mutations
			GenConstraints.HandleExtraGroupByMutations(groupByNodeDS.getGroupByNodes(), cvc.getCurrentMutant());

			// checking if join subquery table created
			boolean isJoin = true;
			// uncomment later

			Vector<String> primaryKeySQ = new Vector<String>(); // comment

			String cntColumnName = aggregateDS.subqueryTableName + "_XDATA_CNT";

			Solver dummySol = ctx.mkSolver(); // for getting string form of z3 context declarations

			// int[] indexOfProjectedColmnsInSQ = new int[projectedNodes.size()]; // comment

			HashMap<String, Object> aggTableDetails = createTempTableColumnsForAggregates(aggregateDS, groupByNodeDS,
					projectedNodeDs, subqueryType);

			cvc.getTableMap().putSQTables((Map<String, Table>) aggTableDetails.get("TableMap"));

			Expr aex = putTableInCtx(cvc, aggregateDS.attrColumnsOfSQ,
					aggregateDS.attrTypesOfSQ, aggregateDS.subqueryTableName); // uncomment

			ArrayList attrNamesBQArrayList = cvc.getDatatypeColumns();

			


			String agg_table = "";
			if (isJoin) {
				if (subqueryType.equalsIgnoreCase(subqueryType)) {
					aggregateDS.baseTableName = cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet()
							.iterator().next();
					agg_table = cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
					numberOfTuples = cvc.getNoOfTuples()
							.get(cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next());
					attrNamesBQArrayList = cvc.subqueryConstraintsMap.get(subqueryType).SQColumns.get(agg_table);
					Table baseTable = cvc.getTableMap().getSQTableByName(agg_table);
					for (i = 0; i < groupByNodeDS.groupByColumnsFromRefTables.size(); i++) {
						for (j = 0; j < attrNamesBQArrayList.size(); j++) {

							if (attrNamesBQArrayList.get(j).toString()
									.contains(groupByNodeDS.groupByColumnsFromRefTables.get(i).getColumnName())
									&& attrNamesBQArrayList.get(j).toString().contains(
											groupByNodeDS.groupByColumnsFromRefTables.get(i).getTableName())) {
								groupByNodeDS.groupByColumnsFromJSQ.add(i,
										baseTable.getColumn(attrNamesBQArrayList.get(j).toString()));
								break;
							}
						}

					}
					for (i = 0; i < projectedNodes.size(); i++) {
						for (j = 0; j < attrNamesBQArrayList.size(); j++) {

							if (attrNamesBQArrayList.get(j).toString()
									.contains(projectedNodes.get(i).getColumn().getColumnName())
									&& attrNamesBQArrayList.get(j).toString()
											.contains(projectedNodes.get(i).getColumn().getTableName())) {
								projectedNodeDs.aggregateColumnsFromJSQ.add(i,
										baseTable.getColumn(attrNamesBQArrayList.get(j).toString()));
								break;
							}
						}

					}
				}
			}

			Table sqi = cvc.getTableMap().getSQTableByName(aggregateDS.subqueryTableName);
			boolean isExits = cvc.getTableMap().getSQTableByName(agg_table).getIsExist();
			sqi.setIsExist(isExits);

			Vector<String> attrNamesBQ = new Vector<String>();
			Vector<Sort> attrTypesBQ = new Vector<Sort>();

			// creating context for base_table_name

			// String arrNameBQ = "O_" + agg_table; // comment
			aggregateDS.arrayNameOfBQInCtx = "O_" + agg_table;
			if (isJoin) {
				// attrNamesBQ = new String[attrNamesBQArrayList.size()];
				// attrTypesBQ = new Sort[attrNamesBQArrayList.size()];
				for (int k = 0; k < attrNamesBQArrayList.size(); k++) {
					// Column column = cvc.getResultsetColumns().get(k);

					if (subqueryType.equalsIgnoreCase(subqueryType)) {

						aggregateDS.arrayNameOfBQInCtx = "O_"
								+ cvc.subqueryConstraintsMap.get(subqueryType).SQTableName.keySet().iterator().next();
						aggregateDS.attrNamesBQ.add("" + attrNamesBQArrayList.get(k));
						aggregateDS.attrTypesBQ.add(getColumnSort(
								cvc.subqueryConstraintsMap.get(subqueryType).SQColumnsDataTypes.get(agg_table).get(k)));

					}

				}
			}

			// putting base table in context
			// Expr aexBQ = putTableInCtx(cvc, attrNamesBQ, attrTypesBQ, agg_table); //
			// comment
			Expr aexBQ = putTableInCtx(cvc, aggregateDS.attrNamesBQ,
					aggregateDS.attrTypesBQ, aggregateDS.baseTableName); // uncomment

			// end putting base table in context

			cvc.getNoOfTuples().put(aggregateDS.subqueryTableName, numberOfTuples);
			cvc.putNoOfOutputTuples(aggregateDS.subqueryTableName, numberOfTuples);

			// cvc.getNoOfTuples().put(subquery_table_name, numberOfTuples);
			// cvc.putNoOfOutputTuples(subquery_table_name, numberOfTuples);
			// adding dummy asserts so that solver has relevant declarations in string
			// returned by toString()
			BoolExpr dummyAssert = ctx.mkDistinct(aex);

			dummySol.add(dummyAssert);

			String[] constraints = dummySol.toString().split("\n");

			String tempStr = "";

			Vector<String> includedStatements = new Vector<String>();

			for (String statement : constraints) {
				if (statement.contains("_TupleType ") || statement.contains("declare-fun O_")) {
					includedStatements.add(statement);
				}
				if (statement.contains("(assert")) {
					break;
				}
			}
			tempStr += String.join("\n\n", includedStatements);

			// Tuple type declaration constraints of Subquery table

			// ***************************************************************************************************
			// primary key constraints for SQ table using COUNT

			Expr cntOfSQColumnSQi1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
					aggregateDS.attrColumnsOfSQ.indexOf(aggregateDS.countColOfSQ.getColumnName()), "i1");
			Expr cntOfSQColumnSQj1 = null;
			if (isMutant)
				cntOfSQColumnSQj1 = ConstraintGenerator.genSelectTest2(cvc, aggregateDS.subqueryTableName,
						projectedNodeDs.indexOfProjectedColmnsInSQ.get(0), "i1");

			// ***************************************************************************************************

			// }
			String primaryKeyConstraintsOfSQ;

			if (isMutant) {
				primaryKeyConstraintsOfSQ = "\n\t(assert (forall ((i1 Int))"
						+ ctx.mkEq(cntOfSQColumnSQi1, cntOfSQColumnSQj1).toString() + "))\n";

				Expr[] eqExprForward = new Expr[numberOfTuples];

				for (i = 1; i <= numberOfTuples; i++) {
					eqExprForward[i - 1] = (BoolExpr) ctx.mkGe(
							smtMap(aggregateDS.arrayNameOfSQInCtx, "j1", i, aggregateDS.attrColumnsOfSQ.size() - 1),
							ctx.mkInt(2));
				}

				primaryKeyConstraintsOfSQ += "\n\t(assert " + ctx.mkOr(eqExprForward).toString() + ")\n";

				// for (i = 1; i <= numberOfTuples; i++) {
				// eqExprForward[i - 1] = smtMap(aggregateDS.arrayNameOfSQInCtx, "j1", i,
				// aggregateDS.attrColumnsOfSQ.size() - 1);

				// }

				// primaryKeyConstraintsOfSQ += "\n\t(assert " +
				// ctx.mkGe(ctx.mkAdd(eqExprForward), ctx.mkInt(2)) + ")\n";

			} else {
				primaryKeyConstraintsOfSQ = getPrimaryKeyConstraintsForGSQTable(cvc,
						aggregateDS, indexType, numberOfTuples);

			}
			// Aggregate functions

			Expr[] arrForIndexI = new Expr[] { ctx.mkIntConst("i") };

			int cntColumnBQIndex = aggregateDS.attrNamesOfBQ.size() - 1;
			Expr[] arrForIntIndex = new Expr[numberOfTuples + 1];
			// Boolean ifSumPresent = false;
			// Boolean ifCountPresent = false;
			int p;
			for (p = 0; p < numberOfTuples; p++) {
				arrForIntIndex[p + 1] = (IntExpr) ctx.mkInt(p + 1);
			}
			arrForIntIndex[0] = (IntExpr) ctx.mkInt(0);

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);
				arrForIndexI = new Expr[] { ctx.mkConst("i", currentSort) };
				arrForIntIndex[0] = ctx.mkConst(cvc.enumIndexVar + 0, currentSort);
				for (p = 0; p < numberOfTuples; p++) {
					arrForIntIndex[p] = ctx.mkConst(cvc.enumIndexVar + (p + 1), currentSort);
				}
			}
			if (isJoin) {
				cntColumnBQIndex = attrNamesBQArrayList.size() - 1;
				// selectCntBQ = smtMap(arrNameBQ, "i1", p, cntColumnBQIndex);

			}

			HashMap<String, Object> aggCalls = generateAggregateConstraints(cvc, qbt, aggregateDS, projectedNodeDs,
					groupByNodeDS, arrForIndexI, numberOfTuples, cntColumnBQIndex);

			Vector<Expr> assertAggValDef = (Vector<Expr>) aggCalls.get("assertAggValDef");
			Vector<Expr> assertAggValCall = (Vector<Expr>) aggCalls.get("assertAggValCall");
			Vector<String> functionName = (Vector<String>) aggCalls.get("functionName");
			Vector<FuncDecl> DeclAssertAggVal = (Vector<FuncDecl>) aggCalls.get("DeclAssertAggVal"); // Function
																										// declaration
																										// for asserting
																										// agg val
			Vector<String> DefineAssertAggVal = (Vector<String>) aggCalls.get("DefineAssertAggVal"); // Function
																										// defination
																										// for asserting
																										// agg val fun

			Expr selectCntBQ;
			selectCntBQ = genSelectTest2(cvc, aggregateDS.arrayNameOfBQInCtx, cntColumnBQIndex, "i1");
			Expr checkIfBQTupleIsValid = ctx.mkGt((ArithExpr) selectCntBQ, ctx.mkInt(0));

			// Function 2 SQ Forward function
			EnumSort currentSort = (EnumSort) ConstraintGenerator.ctxSorts.get(cvc.enumArrayIndex);

			aggregateDS.attrNamesOfBQ = attrNamesBQArrayList;
			HashMap<String, Object> forwardPassData = generateForwardPassConstraints(cvc, qbt, aggregateDS,
					groupByNodeDS, projectedNodeDs, cntColumnBQIndex,
					numberOfTuples, checkIfBQTupleIsValid, currentSort, indexType, isMutant ? 2 : 1);

			FuncDecl sqTableIForward = (FuncDecl) forwardPassData.get("sqTableIForward");
			String DefinesqTableIForward = (String) forwardPassData.get("DefinesqTableIForward");
			Expr exitsExpr1 = (Expr) forwardPassData.get("exitsExpr1");

			// Function 3 SQ Backward function

			HashMap<String, Object> backwardPassData = generateBackwardPassConstraints(cvc, qbt, aggregateDS,
					groupByNodeDS, projectedNodeDs, cntColumnBQIndex,
					numberOfTuples, checkIfBQTupleIsValid, currentSort, indexType);

			FuncDecl sqTableIBackward = (FuncDecl) backwardPassData.get("sqTableIBackward");
			String DefinesqTableIBackward = (String) backwardPassData.get("DefinesqTableIBackward");
			Expr impliesExprBackward = (Expr) backwardPassData.get("impliesExprBackward");
			Expr[] eqExprBackward = (Expr[]) backwardPassData.get("eqExprBackward");
			Expr[] colValueBQForBackward = (Expr[]) backwardPassData.get("colValueBQForBackward");
			Expr[] colValueSQForBackward = (Expr[]) backwardPassData.get("colValueSQForBackward");

			// // end of backward function

			ConstraintsStr += "\n\n" + tempStr;
			ConstraintsStr += primaryKeyConstraintsOfSQ;
			ConstraintsStr += "\n\n" + DefinesqTableIForward.toString() + "\n\n " + "(and \n\t (=> \n\t\t"
					+ checkIfBQTupleIsValid + "\n\n\t" + exitsExpr1.toString() + ")))" + "\n\n";
			ConstraintsStr += "\n\n" + DefinesqTableIBackward.toString() + "\n\n " + impliesExprBackward.toString()
					+ ")" + "\n\n";

			BoolExpr[] assertSqTableIForward1 = new BoolExpr[numberOfTuples];
			Vector<BoolExpr> assertAggValFunsCall = new Vector<BoolExpr>();

			for (int s = 0; s < numberOfTuples; s++) {
				assertSqTableIForward1[s] = (BoolExpr) sqTableIForward.apply(arrForIntIndex[s + 1]);
			}

			BoolExpr andAssertsForward = ctx.mkAnd(assertSqTableIForward1);

			Expr assertSqTableIBackward1;

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkConst("i1", currentSort));
			} else
				assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkIntConst("i1"));

			// function asserts for number of tuples
			Expr backwardExprAnd = ctx.mkAnd((BoolExpr) assertSqTableIBackward1);
			i = 0;

			for (i = 0; i < assertAggValDef.size(); i++) {
				if (assertAggValDef.get(i) != null) {
					ConstraintsStr += "\n\n" + DefineAssertAggVal.get(i) + "\n\n " + assertAggValDef.get(i).toString()
							+ ")" + "\n\n";
					if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
						assertAggValFunsCall
								.add((BoolExpr) DeclAssertAggVal.get(i).apply(ctx.mkConst("i1", currentSort)));
					} else
						// assertSqTableIBackward1 = sqTableIBackward.apply(ctx.mkIntConst("i1"));
						assertAggValFunsCall.add((BoolExpr) DeclAssertAggVal.get(i).apply(ctx.mkIntConst("i1")));
				} else {
					assertAggValFunsCall.add(ctx.mkTrue());
					System.out.println(
							"************ Not supported for this Aggregate Function Name: " + functionName.get(i));
				}
			}

			assertAggValFunsCall.add((BoolExpr) assertSqTableIBackward1);

			Object[] temp = assertAggValFunsCall.toArray();
			Expr[] array = Arrays.copyOf(
					temp, temp.length, Expr[].class);

			BoolExpr andAssertBackwardAndAggValFunsCall = ctx.mkAnd(array); // work from here
			Expr[] iColArray = new IntExpr[] { ctx.mkIntConst("i1") };

			if (Configuration.isEnumInt.equalsIgnoreCase("true")) {
				iColArray = new Expr[] { ctx.mkConst("i1", currentSort) };
			}

			Expr backwardAsssertsFinal = ctx.mkForall((Expr[]) iColArray, (Expr) andAssertBackwardAndAggValFunsCall, 1,
					null, null, null, null);

			ConstraintsStr += "\n\n (assert " + andAssertsForward.toString() + ")" + "\n\n";
			ConstraintsStr += "\n\n (assert " + backwardAsssertsFinal.toString() + ")" + "\n\n";

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ConstraintsStr;
	}

	public String getUserDefinedComparisionOperator(String operator, Column col, String lC, String rC) {
		String constr = "";
		// if(operator.equalsIgnoreCase("/="))
		// 	operator = "!="; //FIXME
		if (lC.equalsIgnoreCase("") && rC.equalsIgnoreCase(""))
			return "";
		if (col.getCvcDatatype().equalsIgnoreCase("INT") || col.getCvcDatatype().equalsIgnoreCase("REAL")) {
			if(operator.equalsIgnoreCase("/="))
				constr += "(not (= " + lC + " " + rC + "))";
			else
				constr += "(" + operator + " " + lC + " " + rC + " " + ")";
			return constr;
		}
		if (operator.equalsIgnoreCase(">")) {
			constr += "(gt" + col.getCvcDatatype() + " " + lC + " " + rC + " " + ")";
			return constr;
		} else if (operator.equalsIgnoreCase("<")) {
			constr += "(lt" + col.getCvcDatatype() + " " + lC + " " + rC + " " + ")";
			return constr;
		}
		if (operator.equalsIgnoreCase("=") && !lC.equalsIgnoreCase("") && !rC.equalsIgnoreCase("")) {
			constr += "(= " + lC + " " + rC + ")";
			return constr;
		}
		if (operator.equalsIgnoreCase("/=") && !lC.equalsIgnoreCase("") && !rC.equalsIgnoreCase("")) {
			constr += "(not (= " + lC + " " + rC + "))";
			return constr;
		}

		return "";
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getUserDefinedComparisionOperator'");
	}

	public String getAssertNotNullCondition(GenerateCVC1 cvc, Node n, String index) {
		// TODO Auto-generated method stub

		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			String dataType = n.getColumn().getCvcDatatype();
			Expr isNullExprs;

			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real")) {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (dataType.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(smtMap(n.getColumn(), ctx.mkInt(index))));
			} else if (dataType.equalsIgnoreCase("date") || dataType.equalsIgnoreCase("time")||dataType.equalsIgnoreCase("timestamp")) {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + n.getColumn().getColumnName())
								.apply(smtMap(n.getColumn(), ctx.mkInt(index))));
			} 
			else {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + dataType)
								.apply(smtMap(n.getColumn(), ctx.mkInt(index))));
			}
			return isNullExprs.toString();

		}
		return "";
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getAssertNotNullCondition'");
	}

	public String getAssertNotNullConditionForSQTable(GenerateCVC1 cvc, Node n, String index, Table sqTable, int colIndex) {
		// TODO Auto-generated method stub

		if (n.getType().equalsIgnoreCase(Node.getColRefType())) {
			String dataType = n.getColumn().getCvcDatatype();
			Expr isNullExprs;
			Expr column = genSelectTest2(cvc, sqTable.getTableName(), colIndex, "i1");

			if (dataType.equalsIgnoreCase("int") || dataType.equalsIgnoreCase("real")) {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("CHECKALL_NULL" + (dataType.equalsIgnoreCase("Int") ? "Int" : "Real"))
								.apply(column));
			} 
			else if (dataType.equalsIgnoreCase("date") || dataType.equalsIgnoreCase("time")||dataType.equalsIgnoreCase("timestamp")) {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + n.getColumn().getColumnName())
								.apply(column));
			} 
			else {
				isNullExprs = ctx.mkNot(
						ctxFuncDecls
								.get("ISNULL_" + dataType)
								.apply(column));
			}
			return isNullExprs.toString();

		}
		return "";
		// throw new UnsupportedOperationException("Unimplemented method
		// 'getAssertNotNullCondition'");
	}
}
