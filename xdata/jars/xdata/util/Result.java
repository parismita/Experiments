package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * This is for single query
 */
public class Result {
    private List<String> datasets;
    private int tuplecount;
    private long timetaken;
    private int datasetcount;
    private int failedcount;//no of mutations failed
    private int totalcount;//no of mutations
    private int successcount;//no of mutations pass

    // Constructor
    public Result(List<String> strings, int tuplecount) {
        this.datasets = strings;
        this.tuplecount = tuplecount;
        this.datasetcount = datasets.size();
    }

    public Result() {
        this.datasets= new ArrayList<String>();
    }

    // Getters
    public List<String> getDatasets() {
        return datasets;
    }

    public int getTupleCount() {
        return tuplecount;
    }

    public long getTimeTaken() {
        return timetaken;
    }

    public int getDatasetCount() {
        return datasetcount;
    }

    public int getFailedCount() {
        return failedcount;
    }

    public int getTotalCount() {//mutant count
        return totalcount;
    }

    public int getSuccessCount() {//mutant count
        successcount=totalcount-failedcount;
        return successcount;
    }

    // Setters
    public void setDatasets(List<String> strings) {
        this.datasets = strings;
    }

    public void putDataset(String strings) {
        this.datasets.add(strings);
    }

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
        this.totalcount = total;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return "Result{" +
                "datasets=" + datasetcount +
                ", tuplecount=" + tuplecount +
                ", timetaken=" + timetaken +
                ", failed_mutantcount=" + failedcount +
                ", total_mutantcount=" + totalcount +
                ", mutantskilled=" + successcount +
                '}';
    }
}
