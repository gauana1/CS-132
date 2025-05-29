package visitors;

import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import other.LinearScan;
import sparrow.*;
import sparrow.visitor.ArgRetVisitor;


public class S2SVVisitor implements ArgRetVisitor<Triple, SparrowVStruct> {
    public Map<String, RegisterStruct> rMap = new HashMap<>();
    public ArrayList<ArrayList<String>> calleeRegs = new ArrayList<>();
    public S2SVVisitor(Map<String, LinearScan> rMap){
        for (String method : rMap.keySet()) {
            LinearScan scan = rMap.get(method);
            RegisterStruct rs = new RegisterStruct();

            // Copy register map
            rs.registerMap.putAll(scan.registerMap);

            // Build intervalMap from intervals
            for (Interval interval : scan.intervals) {
                rs.intervalMap.put(interval.id, new Pair(interval.start, interval.end));
            }

            this.rMap.put(method, rs); 
        }
    }
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
        String funcName = n.functionName.toString();
        RegisterStruct r = rMap.get(funcName);
        SparrowVStruct temp = new SparrowVStruct();
        String funcHeader = "func " + n.functionName.toString() + "(";
        boolean first = true;
        for (IR.token.Identifier p : n.formalParameters) {
            if (!first) {
                funcHeader += " ";
            } else {
                first = false;
            }
            String arg = p.toString();
            if(r.registerMap.containsKey(arg)){
                String reg = r.registerMap.get(arg);
                temp.instructions.add(reg + " = " + arg);
            }
            funcHeader +=  arg;
            
        }
        funcHeader += ")";
        result.instructions.add(funcHeader);
        result.instructions.addAll(temp.instructions);
        SparrowVStruct body = n.block.accept(this, new Triple(n.functionName.toString(), null, 0));
        result.instructions.addAll(body.instructions);

        
        return result;
    }
    public SparrowVStruct visit(Block n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        // for (int i = 1; i <= 11; i++) {
        //     String reg = "s" + i;
        //     result.instructions.add("stack_" + reg + " = " + reg);
        //     // result.instructions.add("print(" + reg + ")");
        // }
        // String[] calleeSaved = {"s1","s2","s3","s4","s5","s6","s7","s8","s9","s10","s11"};
        
        // for (String reg : calleeSaved) {
        //     result.instructions.add("stack_" + reg + " = " + reg);
        // }

        for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first == 's'){
                String saveReg = "stack_" + reg + " = " + reg;
                result.instructions.add(saveReg);
            } 
        } 


        int counter = 1;
        for (Instruction i : n.instructions){
            SparrowVStruct instr = i.accept(this, new Triple(t.methodName, null, counter));
            result.instructions.addAll(instr.instructions);
            counter ++;
        }
       

        //return
        String retID = n.return_id.toString();
        String retReg = "";
        if(r.registerMap.containsKey(retID)){
            //have to reassign
            String reg = r.registerMap.get(retID);
            retReg = reg;
            String reassignRet = retID + " = " + reg;
            result.instructions.add(reassignRet);
        }

        // Restore callee-saved s
        for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first == 's'){
                String restoreReg = reg + " = stack_" + reg;
                result.instructions.add(restoreReg);
            } 
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
        RegisterStruct r = rMap.get(t.methodName);
        String id = n.lhs.toString();
        String num = Integer.toString(n.rhs);
        if(r.registerMap.containsKey(id)){
            //has a register
            String reg = r.registerMap.get(id);
            String assign = reg + " = " + num;
            result.instructions.add(assign);
        }else {
            //spilled
            String temp = "t0 = " + num;
            result.instructions.add(temp);
            String reassign = id + " = t0";
            result.instructions.add(reassign);
        }
        return result;
    } 
    public SparrowVStruct visit(Move_Id_FuncName n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String id = n.lhs.toString();
        String rhs = n.rhs.toString();
        if(r.registerMap.containsKey(id)){
            String reg = r.registerMap.get(id);
            String assign = reg + " = @" + rhs;
            result.instructions.add(assign);
        } else{
            String temp = "t0 = @" + rhs;
            result.instructions.add(temp);
            String reassign = id + " = t0";
            result.instructions.add(reassign);
        }
        return result;
    }
    public SparrowVStruct visit(Add n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String arg1Reg;
        String arg2Reg;
        if(!r.registerMap.containsKey(arg1)){
            String reassignArg1 = "t0 = " + arg1;
            result.instructions.add(reassignArg1);
            arg1Reg = "t0";
        } else{
            arg1Reg = r.registerMap.get(arg1); 
            // result.instructions.add("print(" + arg1Reg + ")");
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            arg2Reg = "t1";
        } else{
            arg2Reg = r.registerMap.get(arg2);
            // result.instructions.add("print(" + arg2Reg + ")");

        }
        if(!r.registerMap.containsKey(lhs)){
            String temp = "t0 =" + arg1Reg + " + " + arg2Reg; 
            result.instructions.add(temp);
            String reassignLhs = lhs + " = t0";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String temp = lhsReg + " = " + arg1Reg + " + " + arg2Reg; 
            result.instructions.add(temp);
        }
        return result;
    }
    public SparrowVStruct visit(Subtract n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String arg1Reg;
        String arg2Reg;
        if(!r.registerMap.containsKey(arg1)){
            String reassignArg1 = "t0 = " + arg1;
            result.instructions.add(reassignArg1);
            arg1Reg = "t0";
        } else{
            arg1Reg = r.registerMap.get(arg1);
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            arg2Reg = "t1";
        } else{
            arg2Reg = r.registerMap.get(arg2);
        }
        if(!r.registerMap.containsKey(lhs)){
            String temp = "t0 =" + arg1Reg + " - " + arg2Reg; 
            result.instructions.add(temp);
            String reassignLhs = lhs + " = t0";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String temp = lhsReg + " = " + arg1Reg + " - " + arg2Reg; 
            result.instructions.add(temp);
        }
        return result;
    } 
    public SparrowVStruct visit(Multiply n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String arg1Reg;
        String arg2Reg;
        if(!r.registerMap.containsKey(arg1)){
            String reassignArg1 = "t0 = " + arg1;
            result.instructions.add(reassignArg1);
            arg1Reg = "t0";
        } else{
            arg1Reg = r.registerMap.get(arg1);
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            arg2Reg = "t1";
        } else{
            arg2Reg = r.registerMap.get(arg2);
        }
        if(!r.registerMap.containsKey(lhs)){
            String temp = "t0 =" + arg1Reg + " * " + arg2Reg; 
            result.instructions.add(temp);
            String reassignLhs = lhs + " = t0";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String temp = lhsReg + " = " + arg1Reg + " * " + arg2Reg; 
            result.instructions.add(temp);
        }
        return result;
    }
    public SparrowVStruct visit(LessThan n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String arg1 = n.arg1.toString();
        String arg2 = n.arg2.toString();
        String arg1Reg;
        String arg2Reg;
        if(!r.registerMap.containsKey(arg1)){
            String reassignArg1 = "t0 = " + arg1;
            result.instructions.add(reassignArg1);
            arg1Reg = "t0";
        } else{
            arg1Reg = r.registerMap.get(arg1);
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            arg2Reg = "t1";
        } else{
            arg2Reg = r.registerMap.get(arg2);
        }
        if(!r.registerMap.containsKey(lhs)){
            String temp = "t0 =" + arg1Reg + " < " + arg2Reg; 
            result.instructions.add(temp);
            String reassignLhs = lhs + " = t0";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String temp = lhsReg + " = " + arg1Reg + " < " + arg2Reg; 
            result.instructions.add(temp);
        }
        return result;
    }
    public SparrowVStruct visit(Load n, Triple t){
        SparrowVStruct result =  new SparrowVStruct();    
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String base = n.base.toString();
        String offset = Integer.toString(n.offset); 
        String baseReg;
        String lhsReg;
        if(!r.registerMap.containsKey(base)){
            String reassignBase = "t0 = " + base;
            result.instructions.add(reassignBase);
            baseReg = "t0";
        } else{
            baseReg = r.registerMap.get(base);
            // result.instructions.add("print(" + baseReg + ")");
        }
        if(!r.registerMap.containsKey(lhs)){
            String instr = "t0 =" + "[" + baseReg + " + " + offset + "]";
            result.instructions.add(instr);
            String reassignLhs = lhs + " = t0";
            result.instructions.add(reassignLhs);
            
        } else{
            lhsReg = r.registerMap.get(lhs);
            String instr = lhsReg + " = [" + baseReg + " + " + offset + "]";
            result.instructions.add(instr);
        }
        return result;
    } 
    public SparrowVStruct visit(Store n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        String base = n.base.toString();
        String offset = Integer.toString(n.offset); 
        String rhs = n.rhs.toString();
        String baseReg;
        String rhsReg;
        if(!r.registerMap.containsKey(base)){
            String reassignBase = "t0 = " + base;
            result.instructions.add(reassignBase);
            baseReg = "t0";
        } else{
            baseReg = r.registerMap.get(base);
        }
        if(!r.registerMap.containsKey(rhs)){
            String reassignRhs = "t1 = " + rhs;
            result.instructions.add(reassignRhs);
            rhsReg = "t1";
        } else{
            rhsReg = r.registerMap.get(rhs);
        }
        String instr = "[" + baseReg + " + " + offset + "] =" + rhsReg;
        result.instructions.add(instr);
        return result;
    }
    public SparrowVStruct visit(Move_Id_Id n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String rhs = n.rhs.toString();
        String rhsReg;
        if(!r.registerMap.containsKey(rhs)){
            String reassignRhs = "t0 = " + rhs;
            result.instructions.add(reassignRhs);
            rhsReg = "t0"; 
        }else{
            rhsReg = r.registerMap.get(rhs);
        }
        if(!r.registerMap.containsKey(lhs)){
            String instr = "t1 = " + rhsReg;
            result.instructions.add(instr);
            String reassignLhs = lhs + "= t1";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String instr = lhsReg + " = " + rhsReg;
            result.instructions.add(instr);
        }
        return result;
    }
    public SparrowVStruct visit(Alloc n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String size = n.size.toString();
        String sizeReg; 
        if(!r.registerMap.containsKey(size)){
            String reassignSize = "t0 = " + size;
            result.instructions.add(reassignSize);
            sizeReg = "t0";
        } else{
            sizeReg = r.registerMap.get(size);
        }
        if(!r.registerMap.containsKey(lhs)){
            String instr = "t1 = alloc(" + sizeReg + ")";
            result.instructions.add(instr);
            String reassignLhs = lhs + " = t1";
            result.instructions.add(reassignLhs);
        } else{
            String lhsReg = r.registerMap.get(lhs);
            String instr = lhsReg + " = alloc(" + sizeReg + ")";
            result.instructions.add(instr);
        }
        return result;
    }
    public SparrowVStruct visit(Print n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        RegisterStruct r = rMap.get(t.methodName);
        String content = n.content.toString();
        if(!r.registerMap.containsKey(content)){
            String reassignContent = "t0 = " + content;
            result.instructions.add(reassignContent);
            String instr = "print (t0)";
            result.instructions.add(instr);
        }else{
            String reg = r.registerMap.get(content);
            String instr = "print (" + reg + ")";
            result.instructions.add(instr);
        }
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
        RegisterStruct r = rMap.get(t.methodName);
        String condition = n.condition.toString();
        if(!r.registerMap.containsKey(condition)){
            String reassignCond = "t0 = " + condition;
            result.instructions.add(reassignCond);
            String instr = "if0 t0 goto " + n.label.toString();
            result.instructions.add(instr);
        } else{
            String reg = r.registerMap.get(condition);
            String instr = "if0 " + reg + " goto " + n.label.toString();
            result.instructions.add(instr);
        }
        return result;
    }
    public SparrowVStruct visit(Call n, Triple t){
        SparrowVStruct result = new SparrowVStruct(); 
        RegisterStruct r = rMap.get(t.methodName);
        String lhs = n.lhs.toString();
        String func = n.callee.toString();
        String funcReg;
        //save caller register(t0 - t5)
        RegisterStruct regStruct = rMap.get(t.methodName);

        String [] aRegs =  {"a2","a3","a4","a5","a6","a7"};

        for (String reg : aRegs) {
            result.instructions.add("stack_" + reg + " = " + reg);
        }

        int curLine = t.line;
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            String temp = "";
            if(r.registerMap.containsKey(lhs)){
                temp = r.registerMap.get(lhs);
            }
            if(first == 't' && !reg.equals(temp)){
                Pair p = regStruct.intervalMap.get(id);
                if(p.start < curLine && curLine < p.end){
                    String saveReg = "stacks_" + reg + " = " + reg;
                    result.instructions.add(saveReg);
                }
            } 
        } 
        //load from reg if exists alr

        if(!r.registerMap.containsKey(func)){
            String reassignFunc = "t0 = " + func;
            result.instructions.add(reassignFunc);
            funcReg = "t0";
        } else{
            funcReg = r.registerMap.get(func);
        }
        String instr;
        String resReg = "";
        if(!r.registerMap.containsKey(lhs)){
            instr = "t1 = call " + funcReg + "(";
        } else{
            String reg = r.registerMap.get(lhs);
            resReg = reg;
            instr = reg + " = call " + funcReg + "(";
        }
        boolean flag = false;
        for(IR.token.Identifier a : n.args){
            String arg = a.toString();
            if(!flag){
                instr +=  arg;
                flag = true;
            } else{
                instr += " " + arg;
            }
            if(r.registerMap.containsKey(arg)){
                String reg= r.registerMap.get(arg);
                result.instructions.add(arg + " = " + reg);
            }
        }
        instr += ")";
        result.instructions.add(instr);
        if(!r.registerMap.containsKey(lhs)){
            String reassignLhs = lhs + " = t1";
            result.instructions.add(reassignLhs);
        } 

        for (String reg : aRegs) {
            if(!reg.equals(resReg)){
                result.instructions.add(reg + " = stack_" + reg);
            } 
        }
        // for (String reg : tRegs) {
        //     if(!reg.equals(resReg)){
        //         result.instructions.add(reg + " = stack_" + reg);
        //     } 
        // }
        

        //reassign caller regs
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first != 't'){continue;}
            Pair p = regStruct.intervalMap.get(id);
            if(p.start < curLine && curLine < p.end &&!reg.equals(resReg)){
                String reassignReg = reg + " = stacks_" + reg;
                result.instructions.add(reassignReg);
            }
        } 
        return result;
   } 
}