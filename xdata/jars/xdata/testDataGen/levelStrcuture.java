package testDataGen;

public class levelStrcuture {
    public Integer queryLevel ;
    public Integer subqueryLevel ;
    public String repSQTable = "";
    public Boolean isAliasingSQTable ;

    levelStrcuture(int level, Boolean flag){
        this.queryLevel = level;
        this.isAliasingSQTable = flag;
    }
    void setSubqueryLevel(int level){
        if(this.isAliasingSQTable)
            this.subqueryLevel = level;
    }
    int getSubqueryLevel(){
        if(this.isAliasingSQTable)
            return this.subqueryLevel ;
        else return -1; // aliasing is not for subquery level
    }
    void setQueryLevel(int level){
        this.queryLevel = level;
    }
    int getQueryLevel(){
        return this.queryLevel ;
    }
    void setRepSQTable(String tablename){
        this.repSQTable = tablename;
    }
    String getRepSQTable() {
        return this.repSQTable;
    }
    void setIfSQTableAlias(Boolean flag){
        this.isAliasingSQTable = flag;
    }
    Boolean getIfSQTableAlias(){
        return this.isAliasingSQTable ;
    }
}
