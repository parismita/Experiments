package util;

import java.util.HashMap;

public class MutationStat {
    private HashMap<String, SingleMutationStat> mutationstats;

    public MutationStat() {
        this.mutationstats = new HashMap<>();
    }

    public HashMap<String, SingleMutationStat> getMutationStats() {
        return mutationstats;
    }

    public SingleMutationStat getMutationCount(String mutationType) {
        return mutationstats.get(mutationType);
    }

    public void setMutationStats(HashMap<String, SingleMutationStat> mutationstats) {
        this.mutationstats = mutationstats;
    }

    public void putMutationStats(String mutationType, SingleMutationStat count) {
        this.mutationstats.put(mutationType, count);
    }

    public void addMutationStats(String mutationType, SingleMutationStat count) {
        if(this.mutationstats.containsKey(mutationType)){
            SingleMutationStat mut = this.mutationstats.get(mutationType);
            mut.addItem(
                count.getTupleCount(), 
                count.getTimeTaken(), 
                count.getDatasetCount(), 
                count.getFailedCount(), 
                count.getTotalCount(), 
                count.getQueriesCount(), 
                count.getBasicFailedCount(), 
                count.getUnsupported());
        }
        else{
            this.mutationstats.put(mutationType, count);
        }
    }
    @Override
    public String toString() {
        String res = "";
        for(String s: mutationstats.keySet()){
            res+="|"+s+"|: "+mutationstats.get(s).toString();
        }
        return res;
    }
    
}
