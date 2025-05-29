package utils;

import java.util.ArrayList;
import java.util.Collection;

public class LinesArray {
    //would have an array of livenessobject
    public ArrayList<LivenessObject> arr = new ArrayList<>();
    public void addAll(LinesArray other) {
        this.arr.addAll(other.arr);
    } 
    public void add(LivenessObject obj) {
        this.arr.add(obj);
    }
    
}
