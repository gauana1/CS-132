// File: Method.java
package utils;
import java.util.ArrayList;
public class Method {
    public String methodName; 
    public String returnType;
    public ArrayList<Argument> args = new ArrayList<>();
    public Method(Method other) {
        this.methodName = other.methodName;
        this.returnType = other.returnType;
        for (Argument arg : other.args) {
            this.args.add(new Argument(arg.type, arg.name));
        }
    } 
    public static class Argument {
        public String type;
        public String name;

        public Argument(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }
    public Method(String methodName, String returnType){
        this.methodName = methodName;
        this.returnType = returnType;
    }
    public void addArgs(Argument arg){
        args.add(arg);
    }
    public boolean equals(Object obj){
        if (this == obj) return true;                     // same object
        if (obj == null || getClass() != obj.getClass()) return false;
        Method other = (Method) obj;
        if(!(args.size() == other.args.size()) || !this.returnType.equals(other.returnType)){
            return false;
        }
        int ptr = 0;
        while(ptr < args.size()){
            Argument thisArg = args.get(ptr);
            Argument otherArg = other.args.get(ptr);
            if(thisArg.type.equals(otherArg.type)){
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
