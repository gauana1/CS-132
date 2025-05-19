package utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
public class Method {
    public String methodName; 
    public String returnType;
    public String methodLabel;    
    public int methodOffset;
    public ArrayList<Pair> args = new ArrayList<>();
    public Map<String, ArrayList<Triple>> vars = new HashMap<>();
    public String originalClass;
    
    public Method(Method other) {
        this.methodName = other.methodName;
        this.returnType = other.returnType;
        this.methodLabel = other.methodLabel;
        this.methodOffset = other.methodOffset; 
        this.originalClass = other.originalClass;
        this.args = new ArrayList<>();
        for (Pair arg : other.args) {
            this.args.add(new Pair(arg.first, arg.second));
        }

        this.vars = new HashMap<>();
        for (Map.Entry<String, ArrayList<Triple>> entry : other.vars.entrySet()) {
            ArrayList<Triple> copiedList = new ArrayList<>();
            for (Triple p : entry.getValue()) {
                copiedList.add(new Triple(p.first, p.second, p.third, p.originalClass, -1));
            }
            this.vars.put(entry.getKey(), copiedList);
        } 
    }


    public Method(String methodName, String returnType, String methodLabel, int methodOffset, String originalClass){
        this.methodName = methodName;
        this.returnType = returnType;
        this.methodLabel = methodLabel;
        this.methodOffset = methodOffset;
        this.originalClass = originalClass;
    }
    public void addArgs(Pair arg){
        args.add(arg);
    }
    public void prependArgsToVars() {
        for (Pair arg : args) {
            // Ensure an entry exists for this arg id
            vars.computeIfAbsent(arg.second, k -> new ArrayList<>());

            // Prepend to the front of the list
            vars.get(arg.second).add(0, new Triple(arg.first, arg.second, false));
        }
    }
    public boolean equals(Object obj){
        if (this == obj) return true;                     
        if (obj == null || getClass() != obj.getClass()) return false;
        Method other = (Method) obj;
        if(!(args.size() == other.args.size()) || !this.returnType.equals(other.returnType)){
            return false;
        }
        int ptr = 0;
        while(ptr < args.size()){
            Pair thisArg = args.get(ptr);
            Pair otherArg = other.args.get(ptr);
            if(thisArg.first.equals(otherArg.first)){
                ptr +=1;
                continue;
            }
            else{
                return false;
            }

        }
        return true;
    }
}