package utils;
import java.util.ArrayList;
public class Method {
    public String methodName; 
    public String returnType;
    public ArrayList<Pair> args = new ArrayList<>();
    public Method(Method other) {
        this.methodName = other.methodName;
        this.returnType = other.returnType;
        for (Pair arg : other.args) {
            this.args.add(new Pair(arg.first, arg.second));
        }
    } 
    public Method(String methodName, String returnType){
        this.methodName = methodName;
        this.returnType = returnType;
    }
    public void addArgs(Pair arg){
        args.add(arg);
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