package visitors;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sparrow.*;
import sparrow.visitor.ArgRetVisitor;


public class S2SVVisitor implements ArgRetVisitor<Triple, SparrowVStruct> {
    public Map<String, RegisterStruct> rMap = new HashMap<>();
    public ArrayList<ArrayList<String>> calleeRegs = new ArrayList<>();
    public SparrowVStruct visit(Program n, Triple t){
        SparrowVStruct result = new SparrowVStruct();    
        for (FunctionDecl fd : n.funDecls){
            SparrowVStruct func = fd.accept(this, t);
            result.instructions.addAll(func.instructions);
        }
        return result;
    }
    public SparrowVStruct visit(FunctionDecl n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        //save calleeregs in temp values on the stack
        if(calleeRegs.size() > 0){
            for(String reg : calleeRegs.get(calleeRegs.size() - 1)){
                String saveReg = "stack_" + reg + " = " + reg;
                result.instructions.add(saveReg);
            }
        }

        String funcHeader = "func " + n.functionName.toString() + "(";
        boolean first = true;
        for (IR.token.Identifier p : n.formalParameters) {
            if (!first) {
                funcHeader += " ";
            } else {
                first = false;
            }
            funcHeader += p.toString();
        }
        funcHeader += ")";
        result.instructions.add(funcHeader);
        SparrowVStruct body = n.block.accept(this, t);
        result.instructions.addAll(body.instructions);

        //reassign calleeregs to og values
        if(calleeRegs.size() > 0){
            for(String reg : calleeRegs.get(calleeRegs.size() - 1)){
                String reassignReg = reg + " = stack_" + reg;
                result.instructions.add(reassignReg);
            }
        }
        
        return result;
    }
    public SparrowVStruct visit(Block n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        for (Instruction i : n.instructions){
            SparrowVStruct instr = i.accept(this, t);
            result.instructions.addAll(instr.instructions);
        }
        String retString = "return " + n.return_id.toString();
        result.instructions.add(retString);
        return result;
    }
    public SparrowVStruct visit(LabelInstr n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        String label = n.toString();
        result.instructions.add(label);
        return result;
    }
    public SparrowVStruct visit(Move_Id_Integer n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        String id = n.lhs.toString();
        String num = Integer.toString(n.rhs);
        String temp = "t0 = " + num;
        result.instructions.add(temp);
        String reassign = id + " = t0";
        result.instructions.add(reassign);
        return result;
    }
    public SparrowVStruct visit(Move_Id_FuncName n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        String id = n.lhs.toString();
        String rhs = n.rhs.toString();
        String temp = "t0 = @" + rhs;
        result.instructions.add(temp);
        String reassign = id + " = t0";
        result.instructions.add(reassign);
        return result;
    }
    public SparrowVStruct visit(Add n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String reassignArg1 = "t0 = " + arg1;
        result.instructions.add(reassignArg1);
        String reassignArg2 = "t1 = " + arg2;
        result.instructions.add(reassignArg2);
        String temp = "t0 = t0 + t1";
        result.instructions.add(temp);
        String reassignLhs = lhs + " = t0";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(Subtract n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();    
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String reassignArg1 = "t0 = " + arg1;
        result.instructions.add(reassignArg1);
        String reassignArg2 = "t1 = " + arg2;           
        result.instructions.add(reassignArg2);
        String temp = "t0 = t0 - t1";               
        result.instructions.add(temp);
        String reassignLhs = lhs + " = t0";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(Multiply n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();    
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String reassignArg1 = "t0 = " + arg1;
        result.instructions.add(reassignArg1);
        String reassignArg2 = "t1 = " + arg2;           
        result.instructions.add(reassignArg2);
        String temp = "t0 = t0 * t1";               
        result.instructions.add(temp);
        String reassignLhs = lhs + " = t0";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(LessThan n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();    
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String reassignArg1 = "t0 = " + arg1;
        result.instructions.add(reassignArg1);
        String reassignArg2 = "t1 = " + arg2;           
        result.instructions.add(reassignArg2);
        String temp = "t0 = t0 < t1";               
        result.instructions.add(temp);
        String reassignLhs = lhs + " = t0";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(Load n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();    
        String lhs = n.lhs.toString();
        String base = n.base.toString();
        String offset = Integer.toString(n.offset); 
        String reassignBase = "t0 = " + base;
        result.instructions.add(reassignBase);
        String instr = "t0 = [t0 + " + offset + "]";
        result.instructions.add(instr);
        String reassignLhs = lhs + " = t0";
        result.instructions.add(reassignLhs);
        return result;
    } 
    public SparrowVStruct visit(Store n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        String base = n.base.toString();
        String offset = Integer.toString(n.offset); 
        String rhs = n.rhs.toString();
        String reassignBase = "t0 = " + base;
        result.instructions.add(reassignBase);
        String reassignRhs = "t1 = " + rhs;
        result.instructions.add(reassignRhs);
        String instr = "[t0 + " + offset + "] = t1";
        result.instructions.add(instr);
        return result;
    }
    public SparrowVStruct visit(Move_Id_Id n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();
        String reassignRhs = "t0 = " + rhs;
        result.instructions.add(reassignRhs);
        String instr = "t1 = t0";
        result.instructions.add(instr);
        String reassignLhs = lhs + "= t1";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(Alloc n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String lhs = n.lhs.toString();
        String size = n.size.toString();
        String reassignSize = "t0 = " + size;
        result.instructions.add(reassignSize);
        String instr = "t1 = alloc(t0)";
        result.instructions.add(instr);
        String reassignLhs = lhs + "= t1";
        result.instructions.add(reassignLhs);
        return result;
    }
    public SparrowVStruct visit(Print n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String content = n.content.toString();
        String reassignContent = "t0 = " + content;
        result.instructions.add(reassignContent);
        String instr = "print (t0)";
        result.instructions.add(instr);
        return result;
    }
    public SparrowVStruct visit(ErrorMessage n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String msg= n.msg.toString();
        String instr = "error (" + msg + ")";
        result.instructions.add(instr);
        return result; 
    }
    public SparrowVStruct visit(Goto n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String instr = n.toString();
        result.instructions.add(instr);
        return result;
    }
    public SparrowVStruct visit(IfGoto n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String condition = n.condition.toString();
        String reassignCond = "t0 = " + condition;
        result.instructions.add(reassignCond);
        String instr = "if0 t0 goto " + n.label.toString();
        result.instructions.add(instr);
        return result;
    }
    public SparrowVStruct visit(Call n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        String lhs = n.lhs.toString();
        String func = n.callee.toString();
        //save caller register(t0 - t5)
        RegisterStruct regStruct = rMap.get(t.methodName);
        ArrayList<String> calleeSaved = new ArrayList<>();
        int curLine = t.line;
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first == 't'){
                Pair p = regStruct.intervalMap.get(id);
                if(p.start <= curLine && curLine <= p.end){
                    String saveReg = "stack_" + reg + " = " + reg;
                    result.instructions.add(saveReg);
                }
            } else if(first == 's'){
                Pair p = regStruct.intervalMap.get(id);
                if(p.start <= curLine && curLine <= p.end){
                    calleeSaved.add(reg);
                }
            }
        } 
        if(calleeSaved.size() > 0){
           calleeRegs.add(calleeSaved); 
        }

        String reassignFunc = "t0 = " + func;
        result.instructions.add(reassignFunc);
        String instr = "t1 = call t0(";
        for(IR.token.Identifier a : n.args){
            String arg = a.toString();
            instr += " " + arg;
        }
        instr += ")";
        result.instructions.add(instr);
        String reassignLhs = lhs + " = t1";
        result.instructions.add(reassignLhs);

        
        if(calleeSaved.size() > 0){
            calleeRegs.remove(calleeRegs.size() - 1);
        }
        //reassign caller regs
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first != 't'){continue;}
            Pair p = regStruct.intervalMap.get(id);
            if(p.start <= curLine && curLine <= p.end){
                String reassignReg = reg + " = stack_" + reg; 
                result.instructions.add(reassignReg);
            }
        } 
        return result;
   }
}