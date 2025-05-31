package other;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import utils.Interval;
import utils.Pair;

public class LinearScan {
    public ArrayList<Interval> intervals = new ArrayList<>(); 
    public ArrayList<Interval> active = new ArrayList<>();
    public ArrayList<String> freeRegisters = new ArrayList<>(Arrays.asList(
    "t2", "t3", "t4", "t5",  // caller-saved
    "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11"  // callee-saved
));
    public Map<String, String> registerMap = new HashMap<>();
    public Map<String, Integer> spillMap = new HashMap<>();
    int spillInt = 0;
    public LinearScan(Map<String, Pair> intervalMap){
        for(String id : intervalMap.keySet()){
            Pair p = intervalMap.get(id);
            Interval i = new Interval(id, p.start, p.end);
            intervals.add(i);
        }
        intervals.sort(Comparator.comparingInt(i -> i.start));
    }

    public void LinearScanRegisterAllocation(int numRegisters){
        for(Interval i : intervals){
            ExpireOldIntervals(i);
            if(active.size() == numRegisters){
                SpillAtInterval(i);
            } else{
                String register = freeRegisters.get(0);
                freeRegisters.remove(0);
                registerMap.put(i.id, register);
                active.add(i);
                active.sort(Comparator.comparingInt(in -> in.end));
            }
        }
    }
    public void ExpireOldIntervals(Interval i){
        for (int idx = 0; idx < active.size(); ) {
            Interval j = active.get(idx);
            if (j.end >= i.start) {
                return;
            }
            active.remove(idx); 
            String register =  registerMap.get(j.id);
            freeRegisters.add(register);
        }
    }
    public void SpillAtInterval(Interval i){
        Interval spill = active.get(active.size() - 1);
        if(spill.end > i.end){
            String spillRegister = registerMap.get(spill.id);
            registerMap.remove(spill.id);
            registerMap.put(i.id, spillRegister);
            spillMap.put(spill.id, spillInt++);
            
            active.remove(active.size() - 1);
            active.add(i);
            active.sort(Comparator.comparingInt(in -> in.end));
        } else{
            spillMap.put(i.id, spillInt++);
        }
    }
}
