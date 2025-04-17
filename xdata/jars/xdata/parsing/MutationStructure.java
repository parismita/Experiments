package parsing;

import testDataGen.QueryBlockDetails;
import util.TagDatasets;
import util.TagDatasets.MutationType;

/**
 * @author Sunanda
 */

public class MutationStructure {
    String mutationType ;
    Integer mutationTypeNumber;
    Object mutationLoc ;
    Object mutationNode ; // mutationTo
    QueryBlockDetails queryBlock ;
    Boolean isExpired;


    public MutationStructure(String mutationType,Integer mutationTypeNumber, Object mutationLoc, QueryBlockDetails qb){
        this.mutationType = mutationType;
        this.mutationTypeNumber = mutationTypeNumber;
        this.mutationLoc = mutationLoc;
        this.queryBlock = qb;
        this.isExpired = false;
    }
    public MutationStructure(String mutationType,Integer mutationTypeNumber, Object mutationLoc){
        this.mutationType = mutationType;
        this.mutationTypeNumber = mutationTypeNumber;
        this.mutationLoc = mutationLoc;
    }
    public MutationStructure(String mutationType,Integer mutationTypeNumber){
        this.mutationType = mutationType;
        this.mutationTypeNumber = mutationTypeNumber;
    }
    public String getMutationType(){
        return this.mutationType;
    }
    public Integer getMutationTypeNumber(){
        return this.mutationTypeNumber;
    }
    public Object getMutationLoc(){
        return this.mutationLoc;
    }
    public void setMutationLoc(Object mutationLoc){
        this.mutationLoc = mutationLoc;
    }
    public void setQueryBlock(QueryBlockDetails qb){
        this.queryBlock = qb;
    }
    public QueryBlockDetails getQueryBlock(){
        return this.queryBlock;
    }
    public void setmutationNode(Object mObject){
        this.mutationNode = mObject;
    }
    public Object getMutationNode(){
        return this.mutationNode ;
    }
    public void setIsExpired(Boolean flag){
        this.isExpired = flag;
    }
    public Boolean getIsExpired(){
        return this.isExpired;
    }
}


