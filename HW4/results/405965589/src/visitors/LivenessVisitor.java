package visitors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sound.sampled.Line;

import utils.*;
import sparrow.*;
import sparrow.visitor.ArgRetVisitor;
import IR.token.Identifier;
import other.LinearScan;

public class LivenessVisitor implements ArgRetVisitor<Triple, Temp> {
    public Map<String, Map<String, Pair>> intervalMap = new HashMap<>(); //maps metehodName to IDname to (start, end) of interval
    public Map<String, Map<String, Integer>> labelMap = new HashMap<>(); //maps methodName to LabelName to line num
    public Map<String, LinearScan> lsMap = new HashMap<>();
    public void insertHelper(String id, Triple t){
        if(intervalMap.get(t.methodName).containsKey(id)){
            //then we can just edit the end
            Pair p = intervalMap.get(t.methodName).get(id);
            intervalMap.get(t.methodName).put(id, new Pair(p.start, t.line));
        } else{
            //otherwise this is the start
            intervalMap.get(t.methodName).put(id, new Pair(t.line, t.line));
        }
    }
    public void editStart(String id, Triple t, int start, int end){
        intervalMap.get(t.methodName).put(id, new Pair(start, end));
    }
    public Temp visit(Program n, Triple t){
        for (FunctionDecl fd: n.funDecls) {
            fd.accept(this, t);
        }
        return null;
    }
    public Temp visit(FunctionDecl n, Triple t){
        //here you load up the lines of the function
        String funcName = n.functionName.name;
        intervalMap.put(funcName, new HashMap<>());
        labelMap.put(funcName, new HashMap<>());
        t.methodName = funcName;
        for(Identifier i: n.formalParameters){
            String id = i.toString(); 
            // if(id.equals("this")){continue;}
            insertHelper(id, new Triple(t.methodName, null, 0));
        }
        n.block.accept(this, t);
        LinearScan ls = new LinearScan(intervalMap.get(funcName));
        ls.LinearScanRegisterAllocation(15);
        lsMap.put(funcName, ls);
        return null;
    }
    public Temp visit(Block n, Triple t){
        int counter = 1;
        for (Instruction i : n.instructions){
            Temp instrResult = i.accept(this, new Triple(t.methodName, t.labelName, counter));
            t.labelName = instrResult.label;
            counter += 1;
        }
        insertHelper(n.return_id.toString(), new Triple(t.methodName, null, counter));
        return null;
    }
    public Temp visit(LabelInstr n, Triple t){
        //since its a label instr we put it into the hashmap
        String label = n.label.toString();
        String methodName = t.methodName;

        labelMap.get(methodName).put(label, t.line);
        
        return new Temp(label);
    }
    public Temp visit(Move_Id_Integer n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        return new Temp(null);
    }
    public Temp visit(Move_Id_FuncName n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        return new Temp(null); 
    }
    public Temp visit(Add n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String arg1 = n.arg1.toString();
        insertHelper(arg1, t);
        String arg2 = n.arg2.toString();
        insertHelper(arg2, t);
        return new Temp(null);  
    }
    public Temp visit(Subtract n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String arg1 = n.arg1.toString();
        insertHelper(arg1, t);
        String arg2 = n.arg2.toString();
        insertHelper(arg2, t);
        return new Temp(null);   
    } 
    public Temp visit(Multiply n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String arg1 = n.arg1.toString();
        insertHelper(arg1, t);
        String arg2 = n.arg2.toString();
        insertHelper(arg2, t);
        return new Temp(null);  
    }
    public Temp visit(LessThan n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String arg1 = n.arg1.toString();
        insertHelper(arg1, t);
        String arg2 = n.arg2.toString();
        insertHelper(arg2, t);
        return new Temp(null);  
    }
    public Temp visit(Load n, Triple t){
        String lhs = n.lhs.toString();
        String base = n.base.toString();
        insertHelper(lhs, t);
        insertHelper(base, t);
        return new Temp(null);  
    }
    public Temp visit(Store n, Triple t){
        String base = n.base.toString();
        String rhs = n.rhs.toString();
        insertHelper(base, t);
        insertHelper(rhs, t);
        return new Temp(null);  
    } 
    public Temp visit(Move_Id_Id n, Triple t){
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();
        insertHelper(lhs, t);
        insertHelper(rhs, t);
        return new Temp(null);  
    }
    public Temp visit(Alloc n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String size = n.size.toString();
        insertHelper(size, t);
        return new Temp(null);  
    }
    public Temp visit(Print n, Triple t){
        String var = n.content.toString();
        insertHelper(var, t);
        return new Temp(null);  
    }
    public Temp visit(ErrorMessage n, Triple t){
        return new Temp(null);   
    }
    public Temp visit(Goto n, Triple t){
        String gotoLabel = n.label.toString();
        int labelLine;
        if(labelMap.get(t.methodName).containsKey(gotoLabel)){
            labelLine = labelMap.get(t.methodName).get(gotoLabel);
        } else{
            return new Temp(null);
        }
        for(String id:intervalMap.get(t.methodName).keySet()){
            Pair p = intervalMap.get(t.methodName).get(id);
            if(p != null && p.start < labelLine && p.end >= labelLine){
                insertHelper(id, t);
            }
            // if(p != null && p.start > labelLine && p.end < t.line){
            //     editStart(id, t, labelLine + 1, t.line);
            // }
        }
        return new Temp(null);   
    }
    public Temp visit(IfGoto n, Triple t){
        String var = n.condition.toString();
        insertHelper(var, t);
        String gotoLabel = n.label.toString();
        int labelLine;
        if(labelMap.get(t.methodName).containsKey(gotoLabel)){
            labelLine = labelMap.get(t.methodName).get(gotoLabel);
        } else{
            return new Temp(null);
        }
        for(String id:intervalMap.get(t.methodName).keySet()){
            Pair p = intervalMap.get(t.methodName).get(id);
            if(p != null && p.start < labelLine && p.end >= labelLine){
                insertHelper(id, t);
            }
            // if(p != null && p.start > labelLine && p.end < t.line){
            //     editStart(id, t, labelLine + 1, t.line);
            // }
        } 
        return new Temp(null); 
    }
    public Temp visit(Call n, Triple t){
        String lhs = n.lhs.toString();
        insertHelper(lhs, t);
        String func = n.callee.toString();
        insertHelper(func, t);
        for(IR.token.Identifier a : n.args){
            String arg = a.toString();
            insertHelper(arg, t);
        }
        return new Temp(null); 
    }
}
