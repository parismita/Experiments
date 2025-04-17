package GenConstraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import generateConstraints.ConstraintGenerator;
import generateConstraints.GenerateCVCConstraintForNode;
import generateConstraints.GenerateCommonConstraintsForQuery;
import generateConstraints.GenerateConstraintForUnintendedJoins;
import generateConstraints.GenerateConstraintsForConjunct;
import generateConstraints.GenerateConstraintsForHavingClause;
import generateConstraints.GenerateConstraintsForWhereClauseSubQueryBlock;
import generateConstraints.GenerateConstraintsToKillExtraGroupByMutations;
import generateConstraints.GenerateGroupByConstraints;
import generateConstraints.GenerateJoinPredicateConstraints;
import generateConstraints.RelatedToEquivalenceClassMutations;
import generateConstraints.UtilsRelatedToNode;
import killMutations.GenerateDataForOriginalQuery;
import killMutations.outerQueryBlock.ColumnReplacementMutations;
import oracle.net.aso.f;
import parsing.Column;
import parsing.AggregateFunction;
import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.DisjunctQueryStructure;
import parsing.MutationStructure;
import parsing.Node;
import parsing.Table;
import parsing.correlationStructure;
import testDataGen.CountEstimationRelated;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import testDataGen.RelatedToParameters;
import util.Configuration;
import util.ConstraintObject;
import util.TagDatasets;

public class GenConstraints {
	/*
	 * @author Sunanda
	 * Driver function for nonempty GenConstraintsdatasets
	 */
	public static void generateDatasetForNonEmptyDataset(GenerateCVC1 cvc) throws Exception {
		// TODO Auto-generated method stub
		Node targetNode = null;
		String mutationType = TagDatasets.MutationType.ORIGINAL.getMutationType()
				+ TagDatasets.QueryBlock.NONE.getQueryBlock();
		Integer mutationTypeNumber = TagDatasets.mutationTypeNumber.ORIGINAL.getMutationType();
		MutationStructure mutStruct = new MutationStructure(mutationType, mutationTypeNumber, targetNode);

		// traverse query blocks and find target locations
		generateDatasetForSpecificMutation(cvc, cvc.getOuterBlock(), mutStruct);

		// throw new UnsupportedOperationException("Unimplemented method
		// 'generateDatasetForNonEmptyDataset'");
	}

	/*
	 * @author Sunanda
	 * Driver function for mutations
	 */
	public static void generateConstraintsToKillMutations(GenerateCVC1 cvc, String mutationType,
			Integer mutationTypeNumber) throws Exception {
		// TODO Auto-generated method stub
		Node targetNode = null;
		// mutationType = TagDatasets.MutationType.EQUIVALENCE.getMutationType() +
		// TagDatasets.QueryBlock.NONE.getQueryBlock();
		// mutationTypeNumber =
		// TagDatasets.mutationTypeNumber.EQUIVALENCE.getMutationType() ;
		MutationStructure mutStruct = new MutationStructure(mutationType, mutationTypeNumber, targetNode);

		Vector<MutationStructure> targetMutants = traverseQueryBlocksToCollectTargetMutantNodes(cvc,
				cvc.getOuterBlock(), mutStruct);
		traverseQueryBlocksToGenerateDatasetsForTargetMutantNodes(cvc, cvc.getOuterBlock(), targetMutants);
	}

	public static void generateConstraintsToKillMutationsForSet(GenerateCVC1 cvc, String mutationType,
		Integer mutationTypeNumber) throws Exception {


	MutationStructure targetMutants = new MutationStructure(mutationType, mutationTypeNumber);

	generateDatasetForSpecificMutation(cvc, cvc.getOuterBlock(), targetMutants);
	}

	private static void traverseQueryBlocksToGenerateDatasetsForTargetMutantNodes(GenerateCVC1 cvc,
			QueryBlockDetails qb, Vector<MutationStructure> targetMutants) throws Exception {
		// TODO Auto-generated method stub
		QueryBlockDetails s = qb;
		for (int i = 0; i < s.getWhereClauseSubQueries().size(); i++) {
			QueryBlockDetails n = s.getWhereClauseSubQueries().get(i);
			if (n != null) {
				traverseQueryBlocksToGenerateDatasetsForTargetMutantNodes(cvc, n, targetMutants);
			}
		}
		for (int i = 0; i < s.getFromClauseSubQueries().size(); i++) {
			QueryBlockDetails n = s.getFromClauseSubQueries().get(i);
			if (n != null) {
				traverseQueryBlocksToGenerateDatasetsForTargetMutantNodes(cvc, n, targetMutants);
			}
		}
		for (MutationStructure mutant : targetMutants) {
			if (mutant.getQueryBlock().getLevel() == s.getLevel())
				generateDatasetForSpecificMutation(cvc, s, mutant);
		}

		// throw new UnsupportedOperationException("Unimplemented method
		// 'traverseQueryBlocksToGenerateDatasetsForTargetMutantNodes'");
	}

	// this function can return final target nodes
	// mutant structure will have query block
	// use generateDatasetForSpecificMutation outside of this function after
	// collecting everything required
	private static Vector<MutationStructure> traverseQueryBlocksToCollectTargetMutantNodes(GenerateCVC1 cvc,
			QueryBlockDetails qb, MutationStructure mutStruct) throws Exception {
		// Traversal of queryblocks
		QueryBlockDetails s = qb;
		Vector<MutationStructure> targetMutants = new Vector<MutationStructure>();
		for (int i = 0; i < s.getWhereClauseSubQueries().size(); i++) {
			QueryBlockDetails n = s.getWhereClauseSubQueries().get(i);
			if (n != null) {
				targetMutants.addAll(traverseQueryBlocksToCollectTargetMutantNodes(cvc, n, mutStruct));
			}
		}
		for (int i = 0; i < s.getFromClauseSubQueries().size(); i++) {
			QueryBlockDetails n = s.getFromClauseSubQueries().get(i);
			if (n != null) {
				targetMutants.addAll(traverseQueryBlocksToCollectTargetMutantNodes(cvc, n, mutStruct));
			}
		}
		// Generate dataset for killing each equivalence class mutation
		targetMutants.addAll(returnTargetNodesForSpecificMutation(cvc, s, mutStruct));

		return targetMutants;

		// throw new UnsupportedOperationException("Unimplemented method
		// 'traverseQueryBlocksToTargetMutantNodes'");
	}

	private static Vector<MutationStructure> returnTargetNodesForSpecificMutation(GenerateCVC1 cvc, QueryBlockDetails s,
			MutationStructure mutStruct) throws Exception {
		// TODO Auto-generated method stub
		Vector<MutationStructure> targetNodes = new Vector<MutationStructure>();

		switch (mutStruct.getMutationTypeNumber()) {
			case 1:
				// generate non-empty dataset
				break;
			case 2:
				// generate agg
				if (s.getHavingClause() != null) {
					QueryBlockDetails.pushHavingClauseAttrInProjectedColumns(s.getHavingClause(), s.getProjectedCols());
				}
				for (int i = 0; i < s.getProjectedCols().size(); i++) {
					if (s.getProjectedCols().get(i).getAgg() != null
							&& s.getProjectedCols().get(i).fromBelowLevelFClauseSQ == false) {
						MutationStructure tempMutantStructure = new MutationStructure(mutStruct.getMutationType(),
								mutStruct.getMutationTypeNumber(), s.getProjectedCols().get(i), s);
						tempMutantStructure.setmutationNode(s.getProjectedCols().get(i).getColumn());
						targetNodes.add(tempMutantStructure);
					}
				}
				break;
			case 3:
				// generate count
				break;
			case 4:
				// generate having
				if (s.getHavingClause() != null && s.getHavingClause().getType() != null
						&& s.getAggConstraints() != null) {
					ArrayList<Node> aggConstraints = s.getAggConstraints();

					for (Node havingNode : aggConstraints) {
						ArrayList<Node> havingMutants = UtilsRelatedToNode.getHavingMutations(havingNode);
						for (Node hvMut : havingMutants) {
							if (!havingNode.getOperator().equalsIgnoreCase(hvMut.getOperator())
									|| hvMut.getIsMutant()) {
								MutationStructure tempMutantStructure = new MutationStructure(
										mutStruct.getMutationType(),
										mutStruct.getMutationTypeNumber(), havingNode, s);
								tempMutantStructure.setmutationNode(hvMut);
								targetNodes.add(tempMutantStructure);
							}
						}
					}
				}

				return targetNodes;
			case 5:
				// generate distinct

				if (s.isDistinct()) {
					MutationStructure tempMutantStructure = new MutationStructure(
							mutStruct.getMutationType(),
							mutStruct.getMutationTypeNumber(), s.getProjectedCols(), s);

					targetNodes.add(tempMutantStructure);
				}

				break;
			case 6:
				// generate equivalence

				targetNodes.addAll(getMutatationsFromConjunctsRecursively(s.getConjunctsQs().get(0), s, mutStruct));

				// for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
				// // for (Vector<Node> equiClasses : conjunct.getEquivalenceClasses()) {
				// if(conjunct.getDisjuncts()!=null && conjunct.getDisjuncts().size()==0){
				// for(DisjunctQueryStructure disjunct: conjunct.getDisjuncts()){
				// MutationStructure tempMutantStructure = new
				// MutationStructure(mutStruct.getMutationType(),
				// mutStruct.getMutationTypeNumber(), disjunct, s);
				// targetNodes.add(tempMutantStructure);
				// }
				// }
				// MutationStructure tempMutantStructure = new
				// MutationStructure(mutStruct.getMutationType(),
				// mutStruct.getMutationTypeNumber(), conjunct, s);
				// targetNodes.add(tempMutantStructure);
				// // }
				// }
				return targetNodes;

			case 7:
				Map<String, String> tableOccurrence = new HashMap<String, String>();
				ArrayList<Column> extraColumn = GenerateConstraintsToKillExtraGroupByMutations.getExtraColumns(cvc, s,
						tableOccurrence);
				int i = 0;
				for (Column c : extraColumn) {
					if (i > 5)
						break;
					MutationStructure tempMutantStructure = new MutationStructure(
							mutStruct.getMutationType(),
							mutStruct.getMutationTypeNumber(), s.getGroupByNodes(), s);
					tempMutantStructure.setmutationNode(c);
					targetNodes.add(tempMutantStructure);
					i++;

				}
				return targetNodes;
			case 8:
				// PARTIALGROUPBY1
				if (s.getGroupByNodes().size() > 1) {
					for (Node n : s.getGroupByNodes()) {
						MutationStructure tempMutantStructure = new MutationStructure(
								mutStruct.getMutationType(),
								mutStruct.getMutationTypeNumber(), n, s);
						tempMutantStructure.setmutationNode(n.getColumn());
						targetNodes.add(tempMutantStructure);
					}
					return targetNodes;
				}
				break;
			case 10:
				// generate non-equi joins
				Vector<MutationStructure> tempmutants = new Vector<MutationStructure>();
				tempmutants.addAll(getMutatationsFromConjunctsRecursively(s.getConjunctsQs().get(0), s, mutStruct));

				for (MutationStructure mut : tempmutants) {
					if (mut.getMutationLoc() instanceof ConjunctQueryStructure) {
						ConjunctQueryStructure conjunct = (ConjunctQueryStructure) mut.getMutationLoc();
						Vector<Node> nequiJoinNodes = new Vector<Node>();
						nequiJoinNodes.addAll(conjunct.getJoinCondsAllOther());
						nequiJoinNodes.addAll(conjunct.getJoinCondsForEquivalenceClasses());
						for (Node neNode : nequiJoinNodes) {
							Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(neNode);
							for (Node scMut : scMutants) {
								if (!neNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), neNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
					if (mut.getMutationLoc() instanceof DisjunctQueryStructure) {
						DisjunctQueryStructure conjunct = (DisjunctQueryStructure) mut.getMutationLoc();
						Vector<Node> nequiJoinNodes = new Vector<Node>();
						nequiJoinNodes.addAll(conjunct.getJoinCondsAllOther());
						nequiJoinNodes.addAll(conjunct.getJoinCondsForEquivalenceClasses());
						for (Node neNode : nequiJoinNodes) {
							Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(neNode);
							for (Node scMut : scMutants) {
								if (!neNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), neNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
				}

				// for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
				// 	// Vector<Node> nequiJoinNodes = conjunct.getAllConds();
				// 	Vector<Node> nequiJoinNodes = new Vector<Node>();
				// 	nequiJoinNodes.addAll(conjunct.getJoinCondsAllOther());
				// 	nequiJoinNodes.addAll(conjunct.getJoinCondsForEquivalenceClasses());
				// 	for (Node neNode : nequiJoinNodes) {
				// 		Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(neNode);
				// 		for (Node scMut : scMutants) {
				// 			if (!neNode.getOperator().equalsIgnoreCase(scMut.getOperator()) || scMut.getIsMutant()) {
				// 				MutationStructure tempMutantStructure = new MutationStructure(
				// 						mutStruct.getMutationType(),
				// 						mutStruct.getMutationTypeNumber(), neNode, s);
				// 				tempMutantStructure.setmutationNode(scMut);
				// 				targetNodes.add(tempMutantStructure);
				// 			}
				// 		}
				// 		// MutationStructure tempMutantStructure = new MutationStructure(
				// 		// mutStruct.getMutationType(),
				// 		// mutStruct.getMutationTypeNumber(), neNode, s);
				// 		// // tempMutantStructure.setmutationNode(neNode);
				// 		// targetNodes.add(tempMutantStructure);
				// 	}
				// }
				return targetNodes;
			case 12:
				tempmutants = new Vector<MutationStructure>();
				tempmutants.addAll(getMutatationsFromConjunctsRecursively(s.getConjunctsQs().get(0), s, mutStruct));

				// selection mutations

				for (MutationStructure mut : tempmutants) {
					if (mut.getMutationLoc() instanceof ConjunctQueryStructure) {
						ConjunctQueryStructure conjunct = (ConjunctQueryStructure) mut.getMutationLoc();
						for (Node selecNode : conjunct.getSelectionConds()) {
							Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(selecNode);
							for (Node scMut : scMutants) {
								if (!selecNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), selecNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
					if (mut.getMutationLoc() instanceof DisjunctQueryStructure) {
						DisjunctQueryStructure conjunct = (DisjunctQueryStructure) mut.getMutationLoc();
						for (Node selecNode : conjunct.getSelectionConds()) {
							Vector<Node> scMutants = UtilsRelatedToNode.getSelectionCondMutations(selecNode);
							for (Node scMut : scMutants) {
								if (!selecNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), selecNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
				}

				// for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
				// for (Node selecNode : conjunct.getSelectionConds()) {
				// Vector<Node> scMutants =
				// UtilsRelatedToNode.getSelectionCondMutations(selecNode);
				// for (Node scMut : scMutants) {
				// if (!selecNode.getOperator().equalsIgnoreCase(scMut.getOperator()) ||
				// scMut.getIsMutant()) {
				// MutationStructure tempMutantStructure = new MutationStructure(
				// mutStruct.getMutationType(),
				// mutStruct.getMutationTypeNumber(), selecNode, s);
				// tempMutantStructure.setmutationNode(scMut);
				// targetNodes.add(tempMutantStructure);
				// }
				// }
				// }
				// }
				return targetNodes;
			case 13:
				tempmutants = new Vector<MutationStructure>();
				tempmutants.addAll(getMutatationsFromConjunctsRecursively(s.getConjunctsQs().get(0), s, mutStruct));

				// selection mutations

				for (MutationStructure mut : tempmutants) {
					if (mut.getMutationLoc() instanceof ConjunctQueryStructure) {
						ConjunctQueryStructure conjunct = (ConjunctQueryStructure) mut.getMutationLoc();
						for (Node stringNode : conjunct.getStringSelectionConds()) {
							Vector<Node> scMutants = UtilsRelatedToNode.getStringSelectionCondMutations(stringNode);
							for (Node scMut : scMutants) {
								if (!stringNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), stringNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
					if (mut.getMutationLoc() instanceof DisjunctQueryStructure) {
						DisjunctQueryStructure conjunct = (DisjunctQueryStructure) mut.getMutationLoc();
						for (Node stringNode : conjunct.getStringSelectionConds()) {
							Vector<Node> scMutants = UtilsRelatedToNode.getStringSelectionCondMutations(stringNode);
							for (Node scMut : scMutants) {
								if (!stringNode.getOperator().equalsIgnoreCase(scMut.getOperator())
										|| scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), stringNode, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
						}
					}
				}
				// for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
				// for (Node stringNode : conjunct.getStringSelectionConds()) {
				// Vector<Node> scMutants =
				// UtilsRelatedToNode.getStringSelectionCondMutations(stringNode);
				// for (Node scMut : scMutants) {
				// if (!stringNode.getOperator().equalsIgnoreCase(scMut.getOperator())
				// || scMut.getIsMutant()) {
				// MutationStructure tempMutantStructure = new MutationStructure(
				// mutStruct.getMutationType(),
				// mutStruct.getMutationTypeNumber(), stringNode, s);
				// tempMutantStructure.setmutationNode(scMut);
				// targetNodes.add(tempMutantStructure);
				// }
				// }
				// }
				// }
				return targetNodes;
			case 14:
				i = 0;
				tempmutants = new Vector<MutationStructure>();
				tempmutants.addAll(getMutatationsFromConjunctsRecursively(s.getConjunctsQs().get(0), s, mutStruct));

				for (MutationStructure mut : tempmutants) {
					if (mut.getMutationLoc() instanceof ConjunctQueryStructure) {
						ConjunctQueryStructure conjunct = (ConjunctQueryStructure) mut.getMutationLoc();
						HashMap<String, Table> tables = QueryBlockDetails.getListOfTablesInQueryBlock(cvc, s);
						ArrayList<ArrayList<Node>> extraCommonCols = GenerateConstraintForUnintendedJoins
								.getExtraColumnsWithCommonName(cvc, tables, s, conjunct);
						for (ArrayList<Node> extraCol : extraCommonCols) {
							if (i > 5)
								break;
							Vector<Node> scMutants = UtilsRelatedToNode.getUnintendedMutations(extraCol, conjunct);
							for (Node scMut : scMutants) {
								if (scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), conjunct, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
							i++;
						}
					}
					if (mut.getMutationLoc() instanceof DisjunctQueryStructure) {
						DisjunctQueryStructure conjunct = (DisjunctQueryStructure) mut.getMutationLoc();
						HashMap<String, Table> tables = QueryBlockDetails.getListOfTablesInQueryBlock(cvc, s);
						ArrayList<ArrayList<Node>> extraCommonCols = GenerateConstraintForUnintendedJoins
								.getExtraColumnsWithCommonName(cvc, tables, s, conjunct);
						for (ArrayList<Node> extraCol : extraCommonCols) {
							if (i > 5)
								break;
							Vector<Node> scMutants = UtilsRelatedToNode.getUnintendedMutations(extraCol, conjunct);
							for (Node scMut : scMutants) {
								if (scMut.getIsMutant()) {
									MutationStructure tempMutantStructure = new MutationStructure(
											mutStruct.getMutationType(),
											mutStruct.getMutationTypeNumber(), conjunct, s);
									tempMutantStructure.setmutationNode(scMut);
									targetNodes.add(tempMutantStructure);
								}
							}
							i++;
						}
					}
				}

				// for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
				// HashMap<String, Table> tables =
				// QueryBlockDetails.getListOfTablesInQueryBlock(cvc, s);
				// ArrayList<ArrayList<Node>> extraCommonCols =
				// GenerateConstraintForUnintendedJoins
				// .getExtraColumnsWithCommonName(cvc, tables, s, conjunct);
				// for (ArrayList<Node> extraCol : extraCommonCols) {
				// if (i > 5)
				// break;
				// Vector<Node> scMutants = UtilsRelatedToNode.getUnintendedMutations(extraCol,
				// conjunct);
				// for (Node scMut : scMutants) {
				// if (scMut.getIsMutant()) {
				// MutationStructure tempMutantStructure = new MutationStructure(
				// mutStruct.getMutationType(),
				// mutStruct.getMutationTypeNumber(), conjunct, s);
				// tempMutantStructure.setmutationNode(scMut);
				// targetNodes.add(tempMutantStructure);
				// }
				// }
				// i++;
				// }
				// }
				return targetNodes;
			case 15:
				for (ConjunctQueryStructure conjunct : s.getConjunctsQs()) {
					for (Node node : conjunct.getAllSubQueryConds()) {
						Vector<Node> scMutants = UtilsRelatedToNode.getWhereConectiveMutations(node);
						for (Node scMut : scMutants) {
							if (!node.getType().equalsIgnoreCase(scMut.getType()) || scMut.getIsMutant()) {
								MutationStructure tempMutantStructure = new MutationStructure(
										mutStruct.getMutationType(),
										mutStruct.getMutationTypeNumber(), node, s);
								tempMutantStructure.setmutationNode(scMut);
								targetNodes.add(tempMutantStructure);
							}
						}
					}
				}
				return targetNodes;
			case 20:
				if (s.getLevel() == 0) {
					ConjunctQueryStructure con = new ConjunctQueryStructure(new Vector<Node>());
					if (s.getConjunctsQs() != null && s.getConjunctsQs().size() != 0) {
						con = s.getConjunctsQs().get(0);
					}
					for (Node n : s.getProjectedCols()) {
						// Only non-string fields are considered here. The string fields are handled
						// automatically by ensuring that enumeration has different values for
						Vector<Node> scMutants = UtilsRelatedToNode.getColumnReplacement(cvc, n);
						for (Node scMut : scMutants) {
							if (n.getColumn().getColumnName().equalsIgnoreCase("xdata_cnt"))
								continue;
							if (Configuration.getProperty("printSQL").equalsIgnoreCase("true")) {
								MutationStructure tempMutantStructure = new MutationStructure(
										mutStruct.getMutationType(),
										mutStruct.getMutationTypeNumber(), n, s);
								tempMutantStructure.setmutationNode(scMut);
								targetNodes.add(tempMutantStructure);
							}
							if (n != null && n.getType() != null &&
									(!n.getType().equals(Node.getAggrNodeType()) && n.getColumn() != null
											&& n.getColumn().getDataType() != 12)
									|| n.getType().equals(Node.getBaoNodeType())) {
								MutationStructure tempMutantStructure = new MutationStructure(
										mutStruct.getMutationType(),
										mutStruct.getMutationTypeNumber(), con, s);
								tempMutantStructure.setmutationNode(scMut);
								targetNodes.add(tempMutantStructure);
							}
						}
					}
				}
				return targetNodes;
			// add more as you implement
			case 24:
				if(s.isDistinct() == false){
					s.setDistinct(true);
					MutationStructure tempMutantStructure = new MutationStructure(
							mutStruct.getMutationType(),
							mutStruct.getMutationTypeNumber(), s.getProjectedCols(), s);

					targetNodes.add(tempMutantStructure);
					// s.setDistinct(false);
				}
			default:
				break;
		}

		return targetNodes;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'returnTargetNodesForSpecificMutation'");
	}

	private static Vector<MutationStructure> getMutatationsFromConjunctsRecursively(
			ConjunctQueryStructure conjunct, QueryBlockDetails s, MutationStructure mutStruct) {
		// TODO Auto-generated method stub
		Vector<MutationStructure> targetMutants = new Vector<MutationStructure>();

		// for (Vector<Node> equiClasses : conjunct.getEquivalenceClasses()) {
		if (conjunct.getDisjuncts() != null && conjunct.getDisjuncts().size() != 0) {
			for (DisjunctQueryStructure disjunct : conjunct.getDisjuncts()) {
				MutationStructure tempMutantStructure = new MutationStructure(mutStruct.getMutationType(),
						mutStruct.getMutationTypeNumber(), disjunct, s);
				targetMutants.add(tempMutantStructure);
				if (disjunct.conjuncts != null && disjunct.conjuncts.size() != 0) {
					for (ConjunctQueryStructure con : disjunct.conjuncts)
						targetMutants.addAll(getMutatationsFromConjunctsRecursively(conjunct, s, mutStruct));
				}
			}
		}
		MutationStructure tempMutantStructure = new MutationStructure(mutStruct.getMutationType(),
				mutStruct.getMutationTypeNumber(), conjunct, s);
		targetMutants.add(tempMutantStructure);
		// }

		return targetMutants;

		// throw new UnsupportedOperationException("Unimplemented method
		// 'getMutatationsFromConjunctsRecursively'");
	}

	/*
	 * @author Sunanda
	 * Main function for generating dataset for specific mutation
	 */
	private static void generateDatasetForSpecificMutation(GenerateCVC1 cvc, QueryBlockDetails qbd,
			MutationStructure mutStruct) throws Exception {

		switch (mutStruct.getMutationTypeNumber()) {
			case 1:
				// generate non-empty dataset
				GenerateDataForOriginalQuery.generateDataForOriginalQuery(cvc, mutStruct.getMutationType());
				break;
			case 2:
				// generate agg
				cvc.inititalizeForDatasetQs();
				cvc.setTypeOfMutation(TagDatasets.MutationType.AGG, TagDatasets.QueryBlock.OUTER_BLOCK);
				cvc.setCurrentMutant(mutStruct);
				Node aggFun = (Node) mutStruct.getMutationLoc();
				/** Get aggregate function to kill in this iteration */
				AggregateFunction af = aggFun.getAgg();
				Column c = aggFun.getColumn();
				if (af.getAggExp() == null) { // projected attribute is not an aggregate
					break;
				}

				/** assign the number of tuples for the this outer query block */
				if (QueryBlockDetails.getTupleAssignment(cvc, qbd, null) == false)
					break;

				/** get the tuple assignment for all other query blocks */
				if (CountEstimationRelated.getTupleAssignmentExceptQueryBlock(cvc, qbd) == false)
					break;

				Node aggNode = Node.createNode(c, c.getTable());
				AggregateFunction ag = new AggregateFunction();
				Node columnNode = new Node(aggNode);
				columnNode.setTable(aggFun.getTable());
				columnNode.setTableAlias(aggFun.getTableAlias());
				columnNode.setTableNameNo(aggFun.getTableNameNo());

				ag.setAggExp(columnNode);
				ag.setFunc(AggregateFunction.getAggCOUNT());

				aggNode.setAgg(ag);
				aggNode.setType(Node.getAggrNodeType());
				aggNode.setTableAlias(columnNode.getTableAlias());
				aggNode.setTableNameNo(columnNode.getTableNameNo());
				aggNode.setIsMutant(true);
				qbd.getProjectedCols().add(aggNode);

				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);

				int index = mutStruct.getQueryBlock().getGroupByNodes().size();

				qbd.getProjectedCols().remove(aggNode);

				// qbd.setConstrainedAggregation(true);

				/** get the count needed */

				break;
			case 3:
				// generate count
				break;
			case 4:
				// generate having
				Node mutNode = (Node) mutStruct.getMutationNode();
				Node mutLoc = (Node) mutStruct.getMutationLoc();
				System.out.print("Aggregate Constraints Mutation: " + mutNode + " killed\n");
				Node havingClause = qbd.getAggConstraints().get(qbd.getAggConstraints().indexOf(mutLoc));
				String op = mutLoc.getOperator();
				havingClause.setOperator(mutNode.getOperator());
				cvc.setCurrentMutant(mutStruct);
				// cvc.inititalizeForDatasetQs();
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				havingClause.setOperator(op);
				break;
			case 5:
				// generate distinct
				Node prNode = ((ArrayList<Node>) mutStruct.getMutationLoc()).get(0);
				c = prNode.getColumn();
				// System.out.print("Extra Attribute Mutation in Group By Clause Killed: " +
				// c.getTableName() + " "
				// + c.getColumnName() + "\n");

				aggNode = prNode.clone();
				ag = new AggregateFunction();
				columnNode = new Node(aggNode);
				ag.setAggExp(columnNode);
				ag.setFunc(AggregateFunction.getAggCOUNT());
				aggNode.setOrgiColumn(c);
				aggNode.setAgg(ag);
				aggNode.setType(Node.getAggrNodeType());
				aggNode.setTableNameNo(columnNode.getTableNameNo());
				aggNode.setIsMutant(true);
				qbd.getProjectedCols().add(aggNode);

				cvc.setCurrentMutant(mutStruct);
				// cvc.inititalizeForDatasetQs();
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);

				qbd.getProjectedCols().remove(aggNode);

				break;
			case 6:
				// generate data to kill equivalence class mutations
				Object conjunct = null;
				Vector<Vector<Node>> equivalenceClassesOrig = new Vector<Vector<Node>>();
				if (mutStruct.getMutationLoc() instanceof ConjunctQueryStructure) {
					conjunct = (ConjunctQueryStructure) mutStruct.getMutationLoc();
					System.out.print("Killing equivalence class mutations: " + qbd.getLevel() + " "
							+ ((ConjunctQueryStructure) conjunct).getEquivalenceClasses() + "\n");
					equivalenceClassesOrig
							.addAll((Vector<Vector<Node>>) ((ConjunctQueryStructure) conjunct).getEquivalenceClasses()
									.clone());
				}
				if (mutStruct.getMutationLoc() instanceof DisjunctQueryStructure) {
					conjunct = (DisjunctQueryStructure) mutStruct.getMutationLoc();
					System.out.print("Killing equivalence class mutations: " + qbd.getLevel() + " "
							+ ((DisjunctQueryStructure) conjunct).getEquivalenceClasses() + "\n");
					equivalenceClassesOrig
							.addAll((Vector<Vector<Node>>) ((DisjunctQueryStructure) conjunct).getEquivalenceClasses()
									.clone());
				}

				// logger.log(Level.INFO,"\n----------------------------------");
				// logger.log(Level.INFO,"KILLING EC IN WHERE CLAUSE SUBQUERY BLOCK: " +
				// equivalenceClassesOrig);
				// logger.log(Level.INFO,"\n----------------------------------\n");

				/** For each equivalence class of this sub query */
				// for(int i=0; i< equivalenceClassesOrig.size(); i++){
				for (int i = (equivalenceClassesOrig.size() - 1); i >= 0; i--) {

					/** Get the equivalence class that is to be killed */
					Vector<Node> ec = (Vector<Node>) equivalenceClassesOrig.get(i).clone();

					/** Update the equivalence classes...these are used during tuple assignment */
					if (mutStruct.getMutationLoc() instanceof ConjunctQueryStructure) {
						((ConjunctQueryStructure) conjunct)
								.setEquivalenceClasses((Vector<Vector<Node>>) equivalenceClassesOrig.clone());
						((ConjunctQueryStructure) conjunct).getEquivalenceClasses().remove(ec);
					}
					if (mutStruct.getMutationLoc() instanceof DisjunctQueryStructure) {
						((DisjunctQueryStructure) conjunct)
								.setEquivalenceClasses((Vector<Vector<Node>>) equivalenceClassesOrig.clone());
						((DisjunctQueryStructure) conjunct).getEquivalenceClasses().remove(ec);
					}

					// HashMap<String, Object> exdetails = new HashMap<String, Object>();
					Node ecNodeTemp = null;
					/** Update the equivalence classes...these are used during tuple assignment */
					if(!(mutStruct.getMutationLoc() instanceof ConjunctQueryStructure) || !(mutStruct.getMutationLoc() instanceof DisjunctQueryStructure)){
						mutStruct.setMutationLoc(conjunct);
					}
					if (mutStruct.getMutationLoc() instanceof ConjunctQueryStructure) {
						((ConjunctQueryStructure) conjunct)
								.setEquivalenceClasses((Vector<Vector<Node>>) equivalenceClassesOrig.clone());
						((ConjunctQueryStructure) conjunct).getEquivalenceClasses().remove(ec);
						ConjunctQueryStructure cj = (ConjunctQueryStructure) conjunct;
						Vector<Node> jcforEq =  cj.getJoinCondsForEquivalenceClasses() ;
						Vector<Node> jcforEqUp = new Vector<Node>();
						for(Node n: jcforEq){
							Node r = n.getRight();
							Node l = n.getLeft() ;
							boolean flag = false;
							// for(Vector<Node> e: equivalenceClassesOrig){
								if(ec.contains(r) && ec.contains(l)) {
									// jcforEqUp.add(n);
									n.isNegated = true;
									flag = true;
									ecNodeTemp = n;
								} 		
							// }
							if(flag == false) jcforEqUp.add(n);
							
						}
						// exdetails.put("conjunct", jcforEqUp);
					}
					if (mutStruct.getMutationLoc() instanceof DisjunctQueryStructure) {
						((DisjunctQueryStructure) conjunct)
								.setEquivalenceClasses((Vector<Vector<Node>>) equivalenceClassesOrig.clone());
						((DisjunctQueryStructure) conjunct).getEquivalenceClasses().remove(ec);
						DisjunctQueryStructure cj = (DisjunctQueryStructure) conjunct;
						Vector<Node> jcforEq =  cj.getJoinCondsForEquivalenceClasses() ;
						Vector<Node> jcforEqUp = new Vector<Node>();
						for(Node n: jcforEq){
							Node r = n.getRight();
							Node l = n.getLeft() ;
							boolean flag = false;
							// for(Vector<Node> e: equivalenceClassesOrig){

								if(ec.contains(r) && ec.contains(l)) {
									// jcforEqUp.add(n);
									flag = true;
									n.isNegated = true;
									ecNodeTemp = n;
								} 		
							// }
							if(flag == false) jcforEqUp.add(n);
							
						}

						// exdetails.put("conjunct", jcforEqUp);
					}
					/** In this iteration we are killing equivalence class 'ec' */
					qbd.setEquivalenceClassesKilled(ec);

					/**
					 * Initialize the data structures for generating the data to kill this mutation
					 */
					cvc.inititalizeForDatasetQs();

					/** set the type of mutation we are trying to kill */
					cvc.setTypeOfMutation(TagDatasets.MutationType.EQUIVALENCE, TagDatasets.QueryBlock.WHERE_SUBQUERY);

					/**
					 * get the tuple assignment for this query
					 * If no possible assignment then not possible to kill this mutation
					 */
					if (GenerateCVC1.tupleAssignmentForQuery(cvc) == false)
						continue;

					/** keep a copy of this tuple assignment values */
					HashMap<String, Integer> noOfOutputTuplesOrig1 = (HashMap<String, Integer>) cvc
							.cloneNoOfOutputTuples();
					HashMap<String, Integer> noOfTuplesOrig1 = (HashMap<String, Integer>) cvc.getNoOfTuples().clone();
					HashMap<String, Integer[]> repeatedRelNextTuplePosOrig1 = (HashMap<String, Integer[]>) cvc
							.getRepeatedRelNextTuplePos().clone();

					// for (int j = 0; j < ec.size(); j++) {
					// 	cvc.setCurrentMutant(mutStruct);
					// 	// logger.log(Level.INFO,"\n----------------------------------\n");
					// 	cvc.setConstraints(new ArrayList<String>());
					// 	cvc.setStringConstraints(new ArrayList<String>());
					// 	cvc.setCVCStr("");
					// 	/** Add constraints related to parameters */
					// 	cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters(cvc, qbd));
					// 	cvc.setResultsetTableColumns1(new HashMap<Table, Vector<String>>());
					// 	cvc.setNoOfTuples((HashMap<String, Integer>) noOfTuplesOrig1.clone());
					// 	cvc.setNoOfOutputTuples((HashMap<String, Integer>) noOfOutputTuplesOrig1.clone());
					// 	cvc.setRepeatedRelNextTuplePos(
					// 			(HashMap<String, Integer[]>) repeatedRelNextTuplePosOrig1.clone());

					// 	String CVCStr = "";
					// 	Node eceNulled = ec.get(j);
					// 	CVCStr += ConstraintGenerator
					// 			.addCommentLine("DataSet Generated By Nulling: " + eceNulled.toString() + "\n");

					// 	// if (RelatedToEquivalenceClassMutations.getConstraintsForNulledColumns(cvc, qbd, ec,
					// 	// 		eceNulled) == false)
					// 	// 	continue;
					// 	/** Call the method for the data generation */
					// 	// GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
					// 	// // print not added - parismita ??
					// 	// if(ecNodeTemp != null)
					// 	// 	ecNodeTemp.isNegated = false;
					// }
					GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
						// print not added - parismita ??
					if(ecNodeTemp != null)
						ecNodeTemp.isNegated = false;
				}

				if (mutStruct.getMutationLoc() instanceof ConjunctQueryStructure) {
					conjunct = (ConjunctQueryStructure) mutStruct.getMutationLoc();
					System.out.print("Killing equivalence class mutations: " + qbd.getLevel() + " "
							+ ((ConjunctQueryStructure) conjunct).getEquivalenceClasses() + "\n");
							((ConjunctQueryStructure) conjunct).getEquivalenceClasses()
							.addAll(equivalenceClassesOrig);
				}
				if (mutStruct.getMutationLoc() instanceof DisjunctQueryStructure) {
					conjunct = (DisjunctQueryStructure) mutStruct.getMutationLoc();
					System.out.print("Killing equivalence class mutations: " + qbd.getLevel() + " "
							+ ((DisjunctQueryStructure) conjunct).getEquivalenceClasses() + "\n");
							((DisjunctQueryStructure) conjunct).getEquivalenceClasses()
							.addAll(equivalenceClassesOrig);
				}
				System.out.println();
				break;
			case 7:
				if (qbd.getGroupByNodes().size() != 0) {
					cvc.setCurrentMutant(mutStruct);
					// Node temp = (Node)((ArrayList) mutStruct.getMutationLoc()).get(0);
					c = ((Column) mutStruct.getMutationNode());
					System.out.print("Extra Attribute Mutation in Group By Clause Killed: " + c.getTableName() + " "
							+ c.getColumnName() + "\n");

					aggNode = Node.createNode(c, c.getTable());
					ag = new AggregateFunction();
					columnNode = new Node(aggNode);
					ag.setAggExp(columnNode);
					ag.setFunc(AggregateFunction.getAggCOUNT());

					aggNode.setAgg(ag);
					aggNode.setType(Node.getAggrNodeType());
					aggNode.setTableNameNo(columnNode.getTableNameNo());
					aggNode.setIsMutant(true);
					qbd.getProjectedCols().add(aggNode);

					GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
					index = mutStruct.getQueryBlock().getGroupByNodes().size();
					mutStruct.getQueryBlock().getGroupByNodes().remove(index - 1);

					qbd.getProjectedCols().remove(aggNode);
				}
				break;

			case 8:
				if (qbd.getGroupByNodes().size() != 0) {
					cvc.setCurrentMutant(mutStruct);
					Node temp = (Node) mutStruct.getMutationLoc();

					Column n = ((Column) mutStruct.getMutationNode());
					System.out.print("Missing Attribute Mutation in Group By Clause Killed: " + " " + n + "\n");

					aggNode = Node.createNode(n, n.getTable());
					ag = new AggregateFunction();
					columnNode = new Node(aggNode);

					ag.setAggExp(columnNode);
					ag.setFunc(AggregateFunction.getAggCOUNT());

					aggNode.setAgg(ag);
					aggNode.setType(Node.getAggrNodeType());
					aggNode.setTableNameNo(columnNode.getTableNameNo());
					aggNode.setIsMutant(true);
					qbd.getProjectedCols().add(aggNode);

					qbd.getGroupByNodes().remove(temp);

					GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
					index = mutStruct.getQueryBlock().getGroupByNodes().size();
					// mutStruct.getQueryBlock().getGroupByNodes().remove(index-1);

					qbd.getGroupByNodes().add(temp);
					qbd.getProjectedCols().remove(aggNode);
				}
				break;
			case 10:
				Node pred = (Node) mutStruct.getMutationLoc();
				Node predMut = (Node) mutStruct.getMutationNode();
				String oldOperator = pred.getOperator();
				// alternate way is to set isNegated true when this mutant is activated
				cvc.setCurrentMutant(mutStruct);
				System.out.println("#############################");
				System.out.print("Level " + qbd.getLevel() + " " + pred + " changed to (" + pred.getLeft()
						+ predMut.getOperator()
						+ pred.getRight() + ")\n");
				System.out.println("#############################");
				HashMap<String, Table> rels = UtilsRelatedToNode.getListOfRelationsFromNode(cvc,
						mutStruct.getQueryBlock(), pred);

				// Iterator rel = rels.keySet().iterator();
				// while(rel.hasNext()){

				String CVCStr = "";
				cvc.setConstraints(new ArrayList<String>());
				cvc.setStringConstraints(new ArrayList<String>());
				cvc.setTypeOfMutation("");
				cvc.setCVCStr("");

				/** set the type of mutation we are trying to kill */
				cvc.setTypeOfMutation(TagDatasets.MutationType.NONEQUIJOIN, TagDatasets.QueryBlock.OUTER_BLOCK);

				/** Add constraints related to parameters */
				cvc.getConstraints().add(RelatedToParameters.addDatatypeForParameters(cvc, qbd));

				// String aliasName = (String)rel.next();
				// String tableName = rels.get(aliasName).getTableName();

				/**
				 * FIXME: This function is generating constraints of form ASSERT NOT EXISTS (i:
				 * O_SECTION_INDEX_INT): ((O_SECTION[1].0>O_TAKES[1].1));
				 * These are causing problem. Example query19
				 */
				/**
				 * FIXME: Also the repeated relations are not correctly handled in the below
				 * method
				 */
				// cvc.getConstraints().add(
				// GenerateCVCConstraintForNode.genNegativeCondsForPredAgg(cvc, qbd, pred,
				// aliasName, tableName) );

				/**
				 * get positive constraints for all conditions except all conditions of the
				 * conjunct
				 */
				// cvc.getConstraints().add(
				// GenerateConstraintsForConjunct.getConstraintsForConjuctExceptNonEquiJoins(cvc,
				// qbd, conjunct) );

				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				pred.setOperator(oldOperator);
				// }
				break;

			case 12:
				Node n = (Node) mutStruct.getMutationLoc();
				oldOperator = n.getOperator();

				System.out.println("#############################");
				System.out.print("Mutant from level " + qbd.getLevel() + " " + n + "changed to"
						+ (Node) mutStruct.getMutationNode() + "\n");
				System.out.println("#############################");
				cvc.setCurrentMutant(mutStruct);
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				n.setOperator(oldOperator);
				mutStruct.setIsExpired(true);
				break;

			case 13:
				n = (Node) mutStruct.getMutationLoc();
				oldOperator = n.getOperator();
				Node oldRight = n.getRight();

				System.out.println("#############################");
				System.out.print("Mutant from level " + qbd.getLevel() + " " + (Node) mutStruct.getMutationLoc()
						+ "changed to" + (Node) mutStruct.getMutationNode() + "\n");
				System.out.println("#############################");
				cvc.setCurrentMutant(mutStruct);
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				n.setOperator(oldOperator);
				n.setRight(oldRight);
				mutStruct.setIsExpired(true);
				break;
			case 14:

			if(mutStruct.getMutationLoc() instanceof ConjunctQueryStructure){
				ConjunctQueryStructure con = (ConjunctQueryStructure) mutStruct.getMutationLoc();
				con.getJoinCondsForEquivalenceClasses().add((Node) mutStruct.getMutationNode());
				System.out.println("#############################");
				System.out.print("Unintended Join Mutant from level " + qbd.getLevel()
						+ " added constraints for " + (Node) mutStruct.getMutationNode() + "\n");
				System.out.println("#############################");
				cvc.setCurrentMutant(mutStruct);
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				con.getJoinCondsForEquivalenceClasses().remove((Node) mutStruct.getMutationNode());
				mutStruct.setIsExpired(true);
				break;
			}
			if(mutStruct.getMutationLoc() instanceof DisjunctQueryStructure){
				DisjunctQueryStructure con = (DisjunctQueryStructure) mutStruct.getMutationLoc();
				con.getJoinCondsForEquivalenceClasses().add((Node) mutStruct.getMutationNode());
				System.out.println("#############################");
				System.out.print("Unintended Join Mutant from level " + qbd.getLevel()
						+ " added constraints for " + (Node) mutStruct.getMutationNode() + "\n");
				System.out.println("#############################");
				cvc.setCurrentMutant(mutStruct);
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				con.getJoinCondsForEquivalenceClasses().remove((Node) mutStruct.getMutationNode());
				mutStruct.setIsExpired(true);
				break;
			}
				
			case 15:
				n = (Node) mutStruct.getMutationLoc();
				oldOperator = n.getType();
				System.out.println("#############################");
				System.out.print("Mutant from level " + qbd.getLevel() + " " + (Node) mutStruct.getMutationLoc()
						+ "changed to" + (Node) mutStruct.getMutationNode() + "\n");
				System.out.println("#############################");
				cvc.setCurrentMutant(mutStruct);
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
				n.setType(oldOperator);
				mutStruct.setIsExpired(true);
				break;
			case 20:
				if (qbd.getLevel() == 0) {
					mutNode = (Node) mutStruct.getMutationNode();
					if (Configuration.getProperty("printSQL").equalsIgnoreCase("true")) {
						System.out.println("#############################");
						System.out.print("Column Replacement Mutant from level " + qbd.getLevel() +
								" condition " + mutNode + "\n");
						System.out.println("#############################");
						cvc.setCurrentMutant(mutStruct);
						GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
						mutStruct.setIsExpired(true);
					} else {
						ConjunctQueryStructure con = (ConjunctQueryStructure) mutStruct.getMutationLoc();

						con.getSelectionConds().add(mutNode);
						System.out.println("#############################");
						System.out.print("Column Replacement Mutant from level " + qbd.getLevel() +
								" condition " + mutNode + "\n");
						System.out.println("#############################");
						cvc.setCurrentMutant(mutStruct);
						GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);
						con.getSelectionConds().remove(mutNode);
						mutStruct.setIsExpired(true);
					}
				}

				break;
				case 21:
					
					int count=cvc.getCount();

					if(cvc.getqStructure().setOperator=="UNION")
					{

						//union vs union all
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//union vs Intersect
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//union vs intersect all
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//union vs EXCEPT
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//union vs EXCEPT all
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}

					else if(cvc.getqStructure().setOperator=="INTERSECT" && cvc.isAll==0)
					{

						//intersect vs union
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect vs union all
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect vs intersect all
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect vs EXCEPT
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect vs EXCEPT all
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}
					else if(cvc.getqStructure().setOperator=="EXCEPT" && cvc.isAll==0)
					{

						//except vs union
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except vs intersect
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except vs union all
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except vs intersect all
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except vs EXCEPT all
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}
					else if(cvc.getqStructure().setOperator=="UNION ALL" )
					{

						//UNION ALL vs union
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//UNION ALL vs INTERSECT
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//UNION ALL vs EXCEPT
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//UNION ALL vs INTERSECT ALL
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//UNION ALL vs EXCEPT all
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}
					else if(cvc.getqStructure().setOperator=="INTERSECT" && cvc.isAll==1)
					{

						//intersect ALL vs union
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect ALL vs INTERSECT
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect ALL vs EXCEPT
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect ALL vs union ALL
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//intersect ALL vs EXCEPT all
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}

					else if(cvc.getqStructure().setOperator=="EXCEPT" && cvc.isAll==1)
					{

						//except ALL vs union
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except ALL vs INTERSECT
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except ALL vs EXCEPT
						cvc.setCount(count++);
						mutStruct.setmutationNode("EXCEPT");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except ALL vs union ALL
						cvc.setCount(count++);
						mutStruct.setmutationNode("UNION ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);

						//except ALL vs intersect all
						cvc.setCount(count++);
						mutStruct.setmutationNode("INTERSECT ALL");
						mutStruct.setIsExpired(false);
						cvc.setCurrentMutant(mutStruct);

						GenerateDataForOriginalQuery.generateDataForQueryMutantsForSet(cvc, mutStruct);

						mutStruct.setIsExpired(true);
						cvc.setCurrentMutant(mutStruct);


					}


					
				break;
			case 24:
				prNode = ((ArrayList<Node>) mutStruct.getMutationLoc()).get(0);
				c = prNode.getColumn();
				// System.out.print("Extra Attribute Mutation in Group By Clause Killed: " +
				// c.getTableName() + " "
				// + c.getColumnName() + "\n");

				aggNode = prNode.clone();
				ag = new AggregateFunction();
				columnNode = new Node(aggNode);
				ag.setAggExp(columnNode);
				ag.setFunc(AggregateFunction.getAggCOUNT());
				aggNode.setOrgiColumn(c);
				aggNode.setAgg(ag);
				aggNode.setType(Node.getAggrNodeType());
				aggNode.setTableNameNo(columnNode.getTableNameNo());
				aggNode.setIsMutant(true);
				qbd.getProjectedCols().add(aggNode);

				cvc.setCurrentMutant(mutStruct);
				// cvc.inititalizeForDatasetQs();
				GenerateDataForOriginalQuery.generateDataForQueryMutants(cvc, mutStruct);

				qbd.getProjectedCols().remove(aggNode);
				qbd.setDistinct(false);
			default:
				break;
		}

		// throw new UnsupportedOperationException("Unimplemented method
		// 'generateDatasetForSpecificMutation'");
	}

	

	/**
	 * @author parismita
	 *         Main function for generating dataset for specific mutation
	 */
	public static String PrintDiffForSpecificMutation(MutationStructure mutStruct) throws Exception {
		if (mutStruct == null)
			return "{\"mutation type\":\"no change\", \"from\": \"\", \"changed to\": \"\"}";
		switch (mutStruct.getMutationTypeNumber()) {
			case 1:
				return "{\"mutation type\":\"no change\", \"from\": \"\", \"changed to\": \"\"}";
			case 2:
				// generate agg
				Node aggFun = (Node) mutStruct.getMutationLoc();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\" : \""
						+ mutStruct.getMutationLoc().toString()
						+ "\", \"changed to\": \"" + aggFun.toString() + "\"}";

			case 3:
				// generate count
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType()
						+ "\", \"from\": \"\", \"changed to\": \"\"}";
			case 4:
				// generate having
				Node mutNode = (Node) mutStruct.getMutationNode();
				Node mutLoc = (Node) mutStruct.getMutationLoc();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", from : " + mutLoc
						+ "\", \"changed to\": " + mutNode + "\"}";
			case 5:
				// generate distinct
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType()
						+ "\",\"from\": \"\", \"changed to\": \"\"}";
			case 6:
				// generate data to kill equivalence class mutations
				ConjunctQueryStructure conjunct = (ConjunctQueryStructure) mutStruct.getMutationLoc();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"additional info\": "
						+ conjunct.getJoinCondsForEquivalenceClasses().get(0)
						+ "\", \"from\": \"\", \"changed to\": \"\"}";
			case 7:
				Column c = ((Column) mutStruct.getMutationNode());
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\":\"\""
						+ ", \"additional info\": \"Extra Attribute Mutation in Group By Clause at " + c.getTableName()
						+ " " + c.getColumnName() + "\", \"changed to\":\"" + c.getTableName() + " " + c.getColumnName()
						+ "\"}";
			case 8:
				Column n = ((Column) mutStruct.getMutationNode());
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\":\"\""
						+ ", \"changed to\":\"" + n
						+ "\", \"additional info\": \"Missing Attribute Mutation in Group By Clause at " + n + "\"}";

			case 10:
				Node pred = (Node) mutStruct.getMutationLoc();
				Node predMut = (Node) mutStruct.getMutationNode();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\" : \"" + pred
						+ "\", \"changed to\": \"" + predMut + "\"}";

			case 12:
				pred = (Node) mutStruct.getMutationLoc();
				predMut = (Node) mutStruct.getMutationNode();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\" : \"" + pred
						+ "\", \"changed to\": \"" + predMut + "\"}";

			case 13:
				pred = (Node) mutStruct.getMutationLoc();
				predMut = (Node) mutStruct.getMutationNode();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"from\" : \"" + pred
						+ "\", \"changed to\": \"" + predMut + "\"}";

			case 14:
				predMut = (Node) mutStruct.getMutationNode();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType()
						+ "\", \"from\": \"\", \"changed to\": \"" + (Node) mutStruct.getMutationNode() + "\"}";

			case 15:
				pred = (Node) mutStruct.getMutationLoc();
				predMut = (Node) mutStruct.getMutationNode();
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType() + "\", \"subquery\" : \"" + pred
						+ "\", \"from\" : \"" + pred.getType() + "\", \"changed to\": \"" + predMut.getType() + "\"}";
			case 20:
				return "{ \"mutation type\" : \"" + mutStruct.getMutationType()
						+ "\", \"from\" : \"" + ((Node) mutStruct.getMutationNode()).getLeft()
						+ "\", \"changed to\" : \"" + ((Node) mutStruct.getMutationNode()).getRight() + "\"}";

			default:
				if (mutStruct.getMutationType() != null)
					return "{ \"mutation type\" : \"" + mutStruct.getMutationType()
							+ "\", \"from\": \"\", \"changed to\": \"\"}";
				else
					return "{\"mutation type\":\"no change\", \"from\": \"\", \"changed to\": \"\"}";
		}
	}

	public static Vector<Node> HandleSelectionMutations(Vector<Node> selConds, MutationStructure mutation) {
		// TODO Auto-generated method stub
		if (!mutation.getIsExpired() && mutation.getMutationTypeNumber() == 12) {
			for (Node condition : selConds) {
				Node n = (Node) mutation.getMutationLoc();
				if (n.equals(condition)) {
					condition.setOperator(((Node) mutation.getMutationNode()).getOperator());
					n.setOperator(((Node) mutation.getMutationNode()).getOperator());
				}
			}
		}
		return selConds;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'HandleSelectionMutations'");
	}

	public static Vector<Node> HandleStringSelectionMutations(Vector<Node> strConds, MutationStructure mutation) {
		// TODO Auto-generated method stub
		if (!mutation.getIsExpired() && mutation.getMutationTypeNumber() == 13) {
			for (Node condition : strConds) {
				Node n = (Node) mutation.getMutationLoc();
				if (n.equals(condition)) {
					n.setOperator(((Node) mutation.getMutationNode()).getOperator());
					condition.setOperator(((Node) mutation.getMutationNode()).getOperator());
					if (n.getOperator().contains("~")) {
						n.setRight(((Node) mutation.getMutationNode()).getRight());
						condition.setRight(((Node) mutation.getMutationNode()).getRight());
					} // n and condition are not same node - they are same valued clones
				}
			}
		}
		return strConds;
	}

	public static void HandleWhereConnectiveMutations(Node condition, MutationStructure mutation) {
		if (!mutation.getIsExpired() && mutation.getMutationTypeNumber() == 15) {
			Node n = (Node) mutation.getMutationLoc();
			if (n.equals(condition)) {
				if (n.getType().equals(Node.getBroNodeSubQType())) {
					n.setOperator(((Node) mutation.getMutationNode()).getOperator());
					condition.setOperator(((Node) mutation.getMutationNode()).getOperator());
				} else
					n.setType(((Node) mutation.getMutationNode()).getType());
			}
		}
		// return condition;
	}

	public static Vector<Node> HandleNonEquiJoinMutations(Vector<Node> selConds, MutationStructure mutation) {
		// TODO Auto-generated method stub
		if (mutation != null && !mutation.getIsExpired() && mutation.getMutationTypeNumber() == 10) {
			for (Node condition : selConds) {
				Node n = (Node) mutation.getMutationLoc();
				if (n.equals(condition)) {
					condition.setOperator(((Node) mutation.getMutationNode()).getOperator());
					n.setOperator(((Node) mutation.getMutationNode()).getOperator());
				}
			}
		}
		return selConds;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'HandleNonEquiJoinMutations'");
	}

	public static ArrayList<Node> HandleExtraGroupByMutations(ArrayList<Node> selConds, MutationStructure mutation) {
		// TODO Auto-generated method stub
		if (mutation != null && !mutation.getIsExpired() && mutation.getMutationTypeNumber() == 7) {
			// selConds.get(0);
			Node n = new Node(selConds.get(0));
			Column newC = (Column) (mutation.getMutationNode());

			n.setColumn(newC);
			n.setTable(newC.getTable());
			n.setTableAlias(newC.getAliasName());
			// n.setTableNameNo(newC.getTable().ge);
			n.setIsMutant(true);
			selConds.add(n);
		}
		return selConds;
		// throw new UnsupportedOperationException("Unimplemented method
		// 'HandleNonEquiJoinMutations'");
	}

}
