package util;

/** 
 * This is for single mutation type
 */
public class SingleMutationStat {
    private int tuplecount;
    private long timetaken;
    private int datasetcount;
    private int avgtuplecount;
    private long avgtimetaken;
    private int avgdatasetcount;
    private int failedcount;
    private int noofmutations;
    private int successcount;
    private int noofqueries;
    private int basicfail;
    private int unsupported;


    // Constructor
    public SingleMutationStat(int tuplecount, long timetaken, int datasetcount, int failedcount, int noofmutations, int noofqueries, int basicfail, int unsupported) {
        this.tuplecount = tuplecount;
        this.timetaken = timetaken;
        this.datasetcount = datasetcount;
        this.failedcount=failedcount;
        this.successcount=noofmutations-failedcount;
        this.noofmutations=noofmutations;
        this.noofqueries=noofqueries;
        this.basicfail=basicfail;
        this.unsupported=unsupported;
        this.avgdatasetcount=(int) Math.ceil((double) this.datasetcount/(this.noofqueries==0?1:this.noofqueries));
        this.avgtimetaken=(int) Math.ceil((double) this.timetaken/(this.noofqueries==0?1:this.noofqueries));
        this.avgtuplecount=(int) Math.ceil((double) this.tuplecount/(this.noofqueries==0?1:this.noofqueries));
    }

    public SingleMutationStat() {
    }

    // Getters
    public int getTupleCount() {
        return tuplecount;
    }

    public long getTimeTaken() {
        return timetaken;
    }

    public int getDatasetCount() {
        return datasetcount;
    }

    public int getAvgTupleCount() {
        return avgtuplecount;
    }

    public long getAvgTimeTaken() {
        return avgtimetaken;
    }

    public int getAvgDatasetCount() {
        return avgdatasetcount;
    }

    public int getFailedCount() {
        return failedcount;
    }

    public int getBasicFailedCount() {
        return basicfail;
    }
    public int getUnsupported() {
        return unsupported;
    }

    public int getSuccessCount() {
        successcount=noofmutations-failedcount;
        return successcount;
    }

    public int getTotalCount() {//mutant count
        return noofmutations;
    }

    public int getQueriesCount() {//mutant count
        return noofqueries;
    }

    // Setters
    public void setTupleCount(int tuplecount) {
        this.tuplecount = tuplecount;
    }

    public void setTimeTaken(long timetaken) {
        this.timetaken = timetaken;
    }

    public void setFailedCount(int fail) {
        this.failedcount = fail;
    }

    public void setTotalCount(int total) {
        this.noofmutations = total;
    }

    public void setDatasetCount(int datasetcount) {
        this.datasetcount=datasetcount;
    }

    public void setQueiresCount(int total) {
        this.noofqueries = total;
    }

    public void setBasicFailedCount(int fail) {
        this.basicfail = fail;
    }

    public void setUnsupported(int fail) {
        this.unsupported = fail;
    }

    public void addItem(int tuplecount, long timetaken, int datasetcount, int failedcount, int noofmutations, int noofqueries, int basicfail, int unsupported) {
        this.tuplecount += tuplecount;
        this.timetaken += timetaken;
        this.datasetcount += datasetcount;
        this.failedcount += failedcount;
        this.noofmutations += noofmutations;
        this.noofqueries += noofqueries;
        this.basicfail += basicfail;
        this.unsupported+=unsupported;
        this.avgdatasetcount=(int) Math.ceil((double) this.datasetcount/(this.noofqueries==0?1:this.noofqueries));
        this.avgtimetaken=(int) Math.ceil((double) this.timetaken/(this.noofqueries==0?1:this.noofqueries));
        this.avgtuplecount=(int) Math.ceil((double) this.tuplecount/(this.datasetcount==0?1:this.datasetcount));
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return  "tuplecount: "+tuplecount +
                ", " + "timetaken: "+timetaken +
                ", " + "failedcount: "+failedcount +
                ", " + "mutations killed: "+(noofmutations-failedcount) +
                ", " + "noofmutations: "+noofmutations +
                ", " + "datasetcount: "+datasetcount +
                ", " + "noofqueries: "+noofqueries +
                ", " + "basicfail: "+basicfail +
                ", " + "avgdatasetcount: "+avgdatasetcount +
                ", " + "avgtimetaken: "+avgtimetaken +
                ", " + "avgtuplecount: "+avgtuplecount +
                ", " + "unsupported: "+unsupported +
                '\n';
    }
}
