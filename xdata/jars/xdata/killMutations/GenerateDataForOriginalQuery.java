package killMutations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import GenConstraints.GenConstraints;
import generateConstraints.AddDataBaseConstraints;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GetSolverHeaderAndFooter;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.MutationStructure;
import parsing.Node;
import parsing.correlationStructure;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.Configuration;
import util.Utilities;

/**
 * This class generates data sets for the original query. This data set is
 * intended tom give non empty result for the original query
 * 
 * @author mahesh
 *
 */
public class GenerateDataForOriginalQuery {

	private static Logger logger = Logger.getLogger(GenerateDataForOriginalQuery.class.getName());

	/**
	 * Generates data set for the original query
	 * 
	 * @param cvc
	 */
	public static boolean generateDataForOriginalQuery(GenerateCVC1 cvc, String mutationType) throws Exception {

		logger.log(Level.INFO, "\n----------------------------------");
		logger.log(Level.INFO, "GENERATE DATA FOR ORIGINAL QUERY: ");
		logger.log(Level.INFO, "---------------------------------\n");
		try {
			/**
			 * Initialize the data structures for generating the data to kill this mutation
			 */
			cvc.inititalizeForDatasetQs();

			/**
			 * get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation
			 */
			if (GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;

			/** set the type of mutation we are trying to kill */
			cvc.setTypeOfMutation(mutationType);

			GetSolverHeaderAndFooter.generateSolver_Header(cvc, false); // testcode by Sunanda for IS_NULL

			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc, false);

			if (Configuration.tempJoins.equalsIgnoreCase("true")) {
				// step 1 and step 2 : Sunanda
				// Build Correlation Map
				QueryBlockDetails.ProcessProjectedColumnsFromFClauseSQ(cvc, cvc.getOuterBlock());
				QueryBlockDetails.TraverseNestedQueryStrcuture(cvc, cvc.getqStructure());
				// Calculates the level at which correlation conditions to be processed
				// considering Exists and Not Exists types
				QueryBlockDetails.setLevelForProcessingCorrelationCondition(cvc);
			}

			if(Configuration.getProperty("printSQL").equalsIgnoreCase("true")){
				String str = cvc.printSQL(cvc.outerBlock, null);
				System.out.println(str);
				String filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/printSQL.txt";
				Utilities.writeFile(filePath, str, true);
				filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/diff.txt";
				String diff = GenConstraints.PrintDiffForSpecificMutation(null);
				Utilities.writeFile(filePath, diff, true);
				return true;
			}

			/**Get the constraints for all the blocks of the query  */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

			/** Call the method for the data generation */
			return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc, false);
		} catch (TimeoutException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}

	
	public static boolean generateDataForQueryMutantsForSet(GenerateCVC1 cvc, MutationStructure mutation) throws Exception {
		logger.log(Level.INFO, "\n----------------------------------");
		logger.log(Level.INFO, "GENERATE DATA FOR SET OPERATOR MUTANT: ");
		logger.log(Level.INFO, "---------------------------------\n");
		try {
			
			cvc.inititalizeForDatasetQs();

			if (GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;

			cvc.setTypeOfMutation(mutation.getMutationType());

			GetSolverHeaderAndFooter.generateSolver_Header(cvc, false); 
			
			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc, false);

			if (Configuration.tempJoins.equalsIgnoreCase("true")) {
		
				QueryBlockDetails.ProcessProjectedColumnsFromFClauseSQ(cvc, cvc.getOuterBlock());
				QueryBlockDetails.TraverseNestedQueryStrcuture(cvc, cvc.getqStructure());
				QueryBlockDetails.setLevelForProcessingCorrelationCondition(cvc);
			}

			if(Configuration.getProperty("printSQL").equalsIgnoreCase("true")){
				String str = cvc.printSQL(cvc.outerBlock, null);
				System.out.println(str);
				String filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/printSQL.txt";
				Utilities.writeFile(filePath, str, true);
				filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/diff.txt";
				String diff = GenConstraints.PrintDiffForSpecificMutation(null);
				Utilities.writeFile(filePath, diff, true);
				return true;
			}

			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

			return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc, false);
		} catch (TimeoutException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}

	public static boolean generateDataForQueryMutants(GenerateCVC1 cvc, MutationStructure mutation) throws Exception {

		logger.log(Level.INFO, "\n----------------------------------");
		logger.log(Level.INFO, "GENERATE DATA FOR ORIGINAL QUERY: ");
		logger.log(Level.INFO, "---------------------------------\n");
		try {
			if(Configuration.getProperty("printSQL").equalsIgnoreCase("true")){
				String filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/diff.txt";
				String diff = GenConstraints.PrintDiffForSpecificMutation(mutation);
				Utilities.writeFile(filePath, diff, true);
			}
			/**
			 * Initialize the data structures for generating the data to kill this mutation
			 */
			if (mutation.getMutationTypeNumber() != 6)
				cvc.inititalizeForDatasetQs();

			/**
			 * get the tuple assignment for this query
			 * If no possible assignment then not possible to kill this mutation
			 */

			if (GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
				return false;

			if (cvc.getCurrentMutant() != null && (cvc.getCurrentMutant().getMutationTypeNumber() == 5 || cvc.getCurrentMutant().getMutationTypeNumber() == 24) ) {
				/** get table name */
				if(CountEstimationRelated.getCountNeededToKillDistinctMutation(cvc,
							cvc.currentMutant.getQueryBlock()) == false)
					return false;
				String tableNameNo = null;

				Node n1 = null;
				for (Node n : cvc.currentMutant.getQueryBlock().getProjectedCols())
					for (QueryBlockDetails qb : cvc.getOuterBlock().getFromClauseSubQueries())
						if (!qb.getProjectedCols().contains(n)) {
							tableNameNo = n.getTableNameNo();
							n1 = n;
						}

				/**
				 * If this relation is involved in equi-joins then we can ensure multiple tuples
				 * at the output, even if this relation contains a single tuple
				 * FIXME: But what if all the relations in this equivalence class contains a
				 * single tuple
				 */
				for (ConjunctQueryStructure con : cvc.currentMutant.getQueryBlock().getConjunctsQs())
					for (Vector<Node> ec : con.getEquivalenceClasses())
						for (Node n2 : ec)
							if (n2.getTableNameNo().equalsIgnoreCase(tableNameNo))
								tableNameNo = null;

				/** assign the number of tuples for the this query block */
				if (QueryBlockDetails.getTupleAssignment(cvc, cvc.currentMutant.getQueryBlock(), tableNameNo) == false)
					return false;

				/** get the tuple assignment for all other query blocks */
				if (CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, cvc.currentMutant.getQueryBlock()) == false)
					return false;

			}
			

			if (cvc.getCurrentMutant() != null
					&& (cvc.getCurrentMutant().getMutationTypeNumber() == 7
							|| cvc.getCurrentMutant().getMutationTypeNumber() == 8)
					&& CountEstimationRelated.getCountAndTupleAssignmentToKillExtraGroupByMutations(cvc,
							cvc.currentMutant.getQueryBlock()) == false)
				return false;

			if (cvc.getCurrentMutant() != null && (cvc.getCurrentMutant().getMutationTypeNumber() == 7
					|| cvc.getCurrentMutant().getMutationTypeNumber() == 8)) {
				Column c = ((Column) cvc.getCurrentMutant().getMutationNode());
				if (cvc.noOfOutputTuplesContains(c.getTableName().toUpperCase()))
					cvc.putNoOfOutputTuples(c.getTableName().toUpperCase(),
							cvc.getNoOfOutputTuples(c.getTableName().toUpperCase()) + 1);
			}
			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() == 2
					&& CountEstimationRelated.getCountNeededToKillAggregationMutation(cvc,
							cvc.getCurrentMutant().getQueryBlock(), 5) == false)
				return false;

			if (cvc.getCurrentMutant() != null && cvc.getCurrentMutant().getMutationTypeNumber() == 2) {
				Column c = ((Node) cvc.getCurrentMutant().getMutationLoc()).getColumn();
				if (cvc.noOfOutputTuplesContains(c.getTableName().toUpperCase()))
					cvc.putNoOfOutputTuples(c.getTableName().toUpperCase(),
							cvc.getNoOfOutputTuples(c.getTableName().toUpperCase()) + 2);
			}
			/** set the type of mutation we are trying to kill */
			cvc.setTypeOfMutation(mutation.getMutationType());

			GetSolverHeaderAndFooter.generateSolver_Header(cvc, false); // testcode by Sunanda for IS_NULL

			GenerateCommonConstraintsForQuery.generateNullandDBConstraints(cvc, false);

			if (Configuration.tempJoins.equalsIgnoreCase("true")) {
				// step 1 and step 2 : Sunanda
				// Build Correlation Map
				QueryBlockDetails.ProcessProjectedColumnsFromFClauseSQ(cvc, cvc.getOuterBlock());

				QueryBlockDetails.TraverseNestedQueryStrcuture(cvc, cvc.getqStructure());
				// Calculates the level at which correlation conditions to be processed
				// considering Exists and Not Exists types
				QueryBlockDetails.setLevelForProcessingCorrelationCondition(cvc);
			}

			/**Get the constraints for all the blocks of the query  */
			cvc.getConstraints().add( QueryBlockDetails.getConstraintsForQueryBlock(cvc) );

			if(Configuration.getProperty("printSQL").equalsIgnoreCase("true")){
				String str = cvc.printSQL(cvc.outerBlock, mutation);
				System.out.println(str);
				String filePath = Configuration.getProperty("printDir") +"/"+cvc.getQueryId()+ "/printSQL.txt";
				Utilities.writeFile(filePath, str, true);
				return true;
			}
			
			/** Call the method for the data generation*/
			return GenerateCommonConstraintsForQuery.generateDataSetForConstraints(cvc,false);
		}catch (TimeoutException e){
			logger.log(Level.SEVERE,e.getMessage(),e);		
			throw e;
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		}
	}
}
