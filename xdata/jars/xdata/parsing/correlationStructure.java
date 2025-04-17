package parsing;

import java.io.Serializable;

/**
 * @author Sunanda
 */

public class correlationStructure {
    public Node condition ;
    public String queryType;
    public Boolean isEquiJoin; 
    public Integer processLevel = 0 ; // Level at which the correlation is written
    public Integer pushTableDownLevel = 0 ; // Level at
    public Integer originalLevelRight = 0;
    public Integer originalLevelLeft = 0;
    public Boolean isPushedDown = false;
    public Boolean isAggr = false;

    public correlationStructure(Node condition){
        this.condition = condition;
    }
    public correlationStructure(Node condition, String queryType){
        this.condition = condition;
        this.isEquiJoin = condition.getOperator()=="=";
        this.queryType = queryType;
    }
    public void setCondition(Node condition){
        this.condition = condition;
    }
    public void setProcessLevel(Integer level){
        this.processLevel = level;
    }
    public int getProcessLevel(){
        return this.processLevel;
    }
    public void setIsEquiJoin(boolean joinType){
        this.isEquiJoin = joinType;
    }
    public boolean getIsEquiJoin(){
        return this.isEquiJoin;
    }
    public void setPushTableDownLevel(Integer level){
        this.pushTableDownLevel = level;
    }
    public int getPushTableDownLevel(){
        return this.pushTableDownLevel;
    }
    public void setQueryType(String queryType){
        this.queryType = queryType;
    }
    public String getQueryType(){
        return this.queryType;
    }
    public Node getCondition(){
        return this.condition;
    }
    public void setOriginalLevelLeft(int level){
        this.originalLevelLeft = level;
    }
     public int getOriginalLevelLeft(){
        return this.originalLevelLeft ;
    }
    public void setOriginalLevelRight(int level){
        this.originalLevelRight = level;
    }
    public int getOriginalLevelRight(){
        return this.originalLevelRight ;
    }
    public void setIsPushedDown(Boolean value){
        this.isPushedDown = value;
    }
    public void setIsAggr(Boolean value){
        this.isAggr = value;
    }
}
