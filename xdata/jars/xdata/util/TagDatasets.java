package util;

/**
 * This class contains enums to denote the type of mutation we are trying to kill
 * These are used to tag data sets that we are generating
 * @author mahesh
 *
 */


public class TagDatasets {


	/** Indicates the type of mutation we are trying to kill*/
	public enum MutationType{

		ORIGINAL("DATASET FOR GENERATING NON EMPTY RESULT "),
		
		AGG("DATASET TO KILL AGGREGATION MUTATIONS "),
		
		COUNT("DATASET TO KILL COUNT MUTATIONS "),
		
		HAVING("DATASET TO KILL CONSTRAINED AGGREGATION MUTATIONS"),
		
		DISTINCT("DATASET TO KILL DISTINCT MUTATIONS "),
		
		EQUIVALENCE("DATASET TO KILL JOIN MUTATIONS "),
		
		EXTRAGROUPBY("DATASET TO KILL EXTRA GROUP BY ATTRIBUTES MUTATIONS "),
		
		//PARTIALGROUPBY1("DATASET TO KILL PARTIAL GROUP BY ATTRIBUTES MUTATIONS "),
		PARTIALGROUPBY1("DATASET TO KILL MISSING GROUP BY ATTRIBUTES MUTATIONS "),
		
		//PARTIALGROUPBY2("DATASET TO KILL PARTIAL GROUP BY ATTRIBUTES MUTATIONS "),
		PARTIALGROUPBY2("DATASET TO KILL MISSING GROUP BY ATTRIBUTES MUTATIONS "),
		
		NONEQUIJOIN("DATASET TO KILL NON EQUI JOIN MUTATIONS "),
		
		LIKE("DATASET TO KILL LIKE MUTATIONS "),
		
		SELCTION("DATASET TO KILL SELECTION MUTATIONS "),
		
		STRING("DATASET TO KILL STRING SELECTION MUTATIONS "),
		
		UNINTENDED("DATASET TO KILL UNINTENDED JOIN MUTATIONS DUE TO COMMON NAMES "),
		
		WHERECONNECTIVE("DATASET TO KILL WHERE CLAUSE CONNECTIVE (EXIST, NOTEXIST) MUTATIONS "),
		
		NULL("DATASET TO KILL IS NULL MUTATIONS "),
		
		MISSING_SUBQUERY("DATASET TO KILL MISSING SUBQUERY MUTATIONS " ),
		
		PATTERN("DATASET TO KILL LIKE PATTERN MUTATIONS "),
		
		NOTEXISTS("DATASET TO GENERATE NON-EMPTY RESULT FOR NOT EXISTS "),
		
		COLUMNREPLACEMENT("DATASET TO KILL COLUMN REPLACEMENT IN PROJECTION "),
		
		SETOP("DATASET TO KILL SET OPERATOR MUTATIONS "),
		
		MISSINGJOINS("DATASET TO KILL MISSING JOIN MUTATIONS "),
		
		CASECONDITION("DATASET TO GENERATE NON-EMPTY RESULT FOR CASE CONDITION"),//case

		ADDITIONALDISTINCT("DATASET TO KILL ADDITIONAL DISTINCT MUTATIONS"); // DISTINCT ADD
		
		private String mutationType;
		
		/**constructor*/
		MutationType( String mutationType) {
			
			this.mutationType = mutationType;
		}
		
		/** get method*/
		public String getMutationType() {

			return mutationType;
		}
		
	}

	
	/**Indicates in which query block we are killing the mutation*/
	public enum QueryBlock{
		
		OUTER_BLOCK( "" ),
		
		FROM_SUBQUERY("IN FROM CLAUSE NESTED SUB QUERY BLOCK"),
		
		WHERE_SUBQUERY("IN WHERE CLAUSE NESTED SUB QUERY BLOCK"),
		
		NONE("");
		
		private String queryBlock;

		/**constructor*/
		QueryBlock( String queryBlock){

			this.queryBlock = queryBlock;
		}

		/** get method*/
		public String getQueryBlock() {

			return queryBlock;
		}
	}

	public enum mutationTypeNumber{
		
		ORIGINAL(1),
		
		AGG(2),
		
		COUNT(3),
		
		HAVING(4),
		
		DISTINCT(5),
		
		EQUIVALENCE(6),
		
		EXTRAGROUPBY(7),
		
		//PARTIALGROUPBY1("DATASET TO KILL PARTIAL GROUP BY ATTRIBUTES MUTATIONS "),
		PARTIALGROUPBY1(8),
		
		//PARTIALGROUPBY2("DATASET TO KILL PARTIAL GROUP BY ATTRIBUTES MUTATIONS "),
		PARTIALGROUPBY2(9),
		
		NONEQUIJOIN(10),
		
		LIKE(11),
		
		SELCTION(12),
		
		STRING(13),
		
		UNINTENDED(14),
		
		WHERECONNECTIVE(15),
		
		NULL(16),
		
		MISSING_SUBQUERY(17),
		
		PATTERN(18),
		
		NOTEXISTS(19),
		
		COLUMNREPLACEMENT(20),
		
		SETOP(21),
		
		MISSINGJOINS(22),
		
		CASECONDITION(23),
		
		ADDITIONALDISTINCT(24);
		
		private Integer mutationType;
		
		/**constructor*/
		mutationTypeNumber( Integer mutationType) {
			
			this.mutationType = mutationType;
		}
		
		/** get method*/
		public Integer getMutationType() {

			return mutationType;
		}
	}
}