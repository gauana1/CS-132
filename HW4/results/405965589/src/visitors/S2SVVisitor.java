package visitors;

import utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
        SparrowVStruct tempStruct = new SparrowVStruct();
        RegisterStruct r = rMap.get(funcName);
        String funcHeader = "func " + n.functionName.toString() + "(";
        boolean flag = false;
        for (int i = 0; i < n.formalParameters.size(); i++) {
            IR.token.Identifier a = n.formalParameters.get(i);
            String arg = a.toString();
            String aReg;
            if(i < 6){
                aReg = "a" + Integer.toString(i + 2);
                // if(r.registerMap.containsKey(arg)){
                //     String reg = r.registerMap.get(arg);
                //     tempStruct.instructions.add(reg + " = " + aReg);
                // } else{
                //     tempStruct.instructions.add(arg + " = " + aReg);
                // }
                r.registerMap.put(arg, aReg);
                // tempStruct.instructions.add("print(" + aReg + ")");
            } else{
                if(!flag){
                    funcHeader += arg;
                    flag = true;
                } else{
                    funcHeader += " " + arg;
                }
                if(r.registerMap.containsKey(arg)){
                    String reg = r.registerMap.get(arg);
                    tempStruct.instructions.add(reg + " = " + arg);
                }
            }
        }
        funcHeader += ")";
        result.instructions.add(funcHeader);
        result.instructions.addAll(tempStruct.instructions);

        SparrowVStruct body = n.block.accept(this, new Triple(n.functionName.toString(), null, 0));
        result.instructions.addAll(body.instructions);

        return result;
    }
    public SparrowVStruct visit(Block n, Triple t){
        SparrowVStruct result = new SparrowVStruct();
        RegisterStruct r = rMap.get(t.methodName);
        if(!t.methodName.equals("main") || !t.methodName.equals("main")){
            for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
                String reg = entry.getValue();
                char first = reg.charAt(0);
                if(first == 's'){
                    String saveReg = "stack_" + reg + " = " + reg;
                    result.instructions.add(saveReg);
                } 
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
        if(r.registerMap.containsKey(retID)){
            //have to reassign
            String reg = r.registerMap.get(retID);
            String reassignRet = retID + " = " + reg;
            result.instructions.add(reassignRet);
        }

        // Restore callee-saved s
        if(!t.methodName.equals("Main") || !t.methodName.equals("main")){
            for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
                String reg = entry.getValue();
                char first = reg.charAt(0);
                if(first == 's'){
                    String restoreReg = reg + " = stack_" + reg;
                    result.instructions.add(restoreReg);
                } 
            } 
        }

        // result.instructions.add("t0 = " + n.return_id.toString());
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
            // result.instructions.add("print(t0)");
            arg1Reg = "t0";
        } else{
            arg1Reg = r.registerMap.get(arg1); 
            // result.instructions.add("print(" + arg1Reg + ")");
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            // result.instructions.add("print(t1)");
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
            // result.instructions.add("print(t0)");
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
            // result.instructions.add("print(t0)");
        } else{
            arg1Reg = r.registerMap.get(arg1);
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            // result.instructions.add("print(t1)");
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
            // result.instructions.add("print(t0)");
        } else{
            arg1Reg = r.registerMap.get(arg1);
            // result.instructions.add("print(" + arg1Reg + ")");
        }
        if(!r.registerMap.containsKey(arg2)){
            String reassignArg2 = "t1 = " + arg2;
            result.instructions.add(reassignArg2);
            arg2Reg = "t1";
            // result.instructions.add("print(t1)");
        } else{
            arg2Reg = r.registerMap.get(arg2);
            // result.instructions.add("print(" + arg2Reg + ")");
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
        String resReg = "";
        if(r.registerMap.containsKey(lhs)){
            resReg = r.registerMap.get(lhs);
        }
        RegisterStruct regStruct = rMap.get(t.methodName);

        //save caller regs(t2-t5)
        int curLine = t.line;
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first == 't' && !reg.equals(resReg)){
                Pair p = regStruct.intervalMap.get(id);
                if(p.start < curLine && curLine < p.end){
                    String saveReg = "stack_" + reg + " = " + reg;
                    result.instructions.add(saveReg);
                }
            } 
        } 

        //now assign arg regs(a2 - a7)
        // String args = "(";
        // for (int i = 0; i < n.args.size(); i++) {
        //    IR.token.Identifier a = n.args.get(i);
        //     String arg = a.toString();
        //     String aReg = "a" + Integer.toString(i + 2);
        //     if(i < 6){
        //         result.instructions.add("stack_" + aReg + " = " + aReg);
        //         if(r.registerMap.containsKey(arg)){
        //             String argReg = r.registerMap.get(arg);
        //             String assignAReg = aReg + " = " + argReg;
        //             result.instructions.add(assignAReg);
        //         } else{
        //             String assignAReg = aReg + " = " + arg;
        //             result.instructions.add(assignAReg);
        //         }
        //     } else{
        //         if(r.registerMap.containsKey(arg)){
        //             String argReg = r.registerMap.get(arg);
        //             if(!argReg.equals(resReg)){
        //                 result.instructions.add("stack_" + aReg + " = " + argReg); //id = reg
        //             } //look into like passing the extra args and see if like the names apppear out of nowhere, need to track themn properly
        //             String assignAReg = arg + " = " + argReg;
        //             result.instructions.add(assignAReg);
                    
        //         } else{
        //             //have to save like caller saved
        //             result.instructions.add("t0 = " + arg);
        //             result.instructions.add("stack_" + aReg + " = t0"); // id = reg
        //         }
        //         args += " " + arg;
        //     }
            
        // } 
        // args += ")";

         //now assign arg regs(a2 - a7)
        SparrowVStruct argAssigns = topoSortAssignArgs(n.args, regStruct, resReg,t.methodName);
        result.instructions.addAll(argAssigns.instructions);
        SparrowVStruct temp = new SparrowVStruct();
        String args = "(";
        for (int i = 0; i < n.args.size(); i++) {
            if(i >= 6){
                IR.token.Identifier a = n.args.get(i);
                String arg = a.toString();
                if(r.registerMap.containsKey(arg)){
                    String reg = r.registerMap.get(arg);
                    //then it has a register associated with it
                    if(reg.charAt(0) == 'a'){
                        String stackReg = "stack_a" + Integer.toString(i + 2);
                        args += " " + stackReg;
                    } else{
                        temp.instructions.add(arg + " = " + reg);
                        args += " " + arg;
                    }
                }else{
                    args += " " + arg;
                }
                
            }
            
        } 
        args += ")";
        result.instructions.addAll(temp.instructions);
        if(!r.registerMap.containsKey(func)){
            String reassignFunc = "t0 = " + func;
            result.instructions.add(reassignFunc);
            funcReg = "t0";
        } else{
            funcReg = r.registerMap.get(func);
        }
        String instr;
        if(!r.registerMap.containsKey(lhs)){
            instr = "t1 = call " + funcReg + args;
        } else{
            String reg = r.registerMap.get(lhs);
            instr = reg + " = call " + funcReg + args;
        }
        result.instructions.add(instr);
        if(!r.registerMap.containsKey(lhs)){
            String reassignLhs = lhs + " = t1";
            result.instructions.add(reassignLhs);
        } 

        //reassign a regs like caller regs
        if(!t.methodName.equals("Main") && !t.methodName.equals("main")){
            for (int i = 0; i < n.args.size(); i++) {
                IR.token.Identifier a = n.args.get(i);
                String arg = a.toString();
                String aReg = "a" + Integer.toString(i + 2);
                if(i < 6){
                    result.instructions.add(aReg + " = stack_" + aReg);
                // }
                } else{
                    if(r.registerMap.containsKey(arg)){
                        String argReg = r.registerMap.get(arg);
                        if(argReg.equals(resReg)){continue;}
                        result.instructions.add(argReg + " = stack_" + aReg); //id = reg
                    } else{
                        result.instructions.add("t0 = stack_" + aReg); // id = reg
                        result.instructions.add(arg + " = t0");
                    }
                }
                
            }
        }
        

        //reassign caller regs
        for(Map.Entry<String, String> entry : regStruct.registerMap.entrySet()){
            String id = entry.getKey(); 
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first != 't'){continue;}
            Pair p = regStruct.intervalMap.get(id);
            if(p.start < curLine && curLine < p.end &&!reg.equals(resReg)){
                String reassignReg = reg + " = stack_" + reg;
                result.instructions.add(reassignReg);
            }
        } 
        return result;
    }     
    public SparrowVStruct topoSortAssignArgs(List<IR.token.Identifier> args, RegisterStruct rStruct, String resReg, String funcName) {
        SparrowVStruct result = new SparrowVStruct();
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> nodes = new HashSet<>();
        Map<String, Integer> indegree = new HashMap<>();

        // Build graph
        for (int i = 0; i < args.size(); i++) {
            String node = i < 6 ? "assign_a" + (i + 2) : "assign_stack_a" + (i + 2);
            nodes.add(node);
            indegree.put(node, 0);
        }

        for (int i = 0; i < args.size(); i++) {
            String regI = rStruct.registerMap.get(args.get(i).toString());
            if (regI == null) continue;
            
            for (int j = 0; j < args.size(); j++) {
                if (i == j) continue;
                String targetJ = j < 6 ? "a" + (j + 2) : rStruct.registerMap.get(args.get(j).toString());
                if (regI.equals(targetJ)) {
                    String from = i < 6 ? "assign_a" + (i + 2) : "assign_stack_a" + (i + 2);
                    String to = j < 6 ? "assign_a" + (j + 2) : "assign_stack_a" + (j + 2);
                    graph.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
                    indegree.put(to, indegree.get(to) + 1);
                }
            }
        }

        // Process acyclic nodes first
        Queue<String> q = new LinkedList<>();
        for (String node : nodes) {
            if (indegree.get(node) == 0) q.add(node);
        }

        while (!q.isEmpty()) {
            String node = q.poll();
            generateInstruction(node, args, rStruct, resReg, result, funcName);
            
            for (String neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                indegree.put(neighbor, indegree.get(neighbor) - 1);
                if (indegree.get(neighbor) == 0) q.add(neighbor);
            }
        }

        // Handle cycles
        Set<String> remaining = new HashSet<>();
        for (String node : nodes) {
            if (indegree.get(node) > 0) remaining.add(node);
        }

        while (!remaining.isEmpty()) {
            String start = remaining.iterator().next();
            List<String> cycle = findCycle(start, graph, remaining);
            
            if (cycle.size() > 1) {
                // Break cycle: save first, do chain, restore last
                String temp = "t0";
                String lastNode = cycle.get(cycle.size() - 1);
                int lastIdx = Integer.parseInt(lastNode.replaceAll("[^0-9]", "")) - 2;
                String argReg = "a" + Integer.toString(lastIdx + 2);
                // String firstArg = args.get(firstIdx).toString();
                // String argReg = rStruct.registerMap.get(firstArg);
                result.instructions.add(temp + " = " + argReg);
                
                // Process cycle chain
                for (int i = 0; i < cycle.size(); i++) {
                    String node = cycle.get(i);
                    int idx = Integer.parseInt(node.replaceAll("[^0-9]", "")) - 2;
                    String sourceArg = args.get(idx).toString();
                    String sourceReg = node.startsWith("assign_a") ? "a" + (idx + 2) : "stack_a" + (idx + 2);
                    if (i == cycle.size() - 1) {
                        // Last node gets temp value
                        //need to assign first = last, last is already in temp
                        node = cycle.get(0);
                        idx = Integer.parseInt(node.replaceAll("[^0-9]", "")) - 2;
                        sourceReg = node.startsWith("assign_a") ? "a" + (idx + 2) : "stack_a" + (idx + 2); 
                        if (node.startsWith("assign_a") && !funcName.equals("main") && !funcName.equals("Main")) {
                            result.instructions.add("stack_" + sourceReg + " = " + sourceReg);
                        } 
                        result.instructions.add(sourceReg + " = " + temp);
                    } else {
                        // Get value from next node in cycle
                        int nextIdx = Integer.parseInt(cycle.get(i + 1).replaceAll("[^0-9]", "")) - 2;
                        // String nextArg = args.get(nextIdx).toString();
                        String targetReg= node.startsWith("assign_a") ? "a" + (nextIdx + 2) : "stack_a" + (nextIdx + 2);
                        if (node.startsWith("assign_a") && !funcName.equals("main") && !funcName.equals("Main")) {
                            result.instructions.add("stack_" + targetReg + " = " + targetReg);
                        }
                        result.instructions.add(targetReg + " = " + (sourceReg != null ? sourceReg : sourceArg));
                    }
                }
            }
            
            remaining.removeAll(cycle);
        }
        return result;
    }
    private void generateInstruction(String node, List<IR.token.Identifier> args, RegisterStruct rStruct, String resReg, SparrowVStruct result, String funcName) {
        int idx = Integer.parseInt(node.replaceAll("[^0-9]", "")) - 2;
        String arg = args.get(idx).toString();
        String sourceReg = rStruct.registerMap.get(arg);
        
        if (node.startsWith("assign_a")) {
            String aReg = "a" + (idx + 2);
            if(!funcName.equals("main") && !funcName.equals("Main")){
                result.instructions.add("stack_" + aReg + " = " + aReg);
            }
            String temp = sourceReg != null ? sourceReg : arg;
            if(!temp.equals(aReg)){
                result.instructions.add(aReg + " = " + temp);
            }
        } else {
            if(!funcName.equals("main") && !funcName.equals("Main")) {
                String stackReg = "stack_a" + (idx + 2);
                if (sourceReg != null && !sourceReg.equals(resReg)) {
                    result.instructions.add(stackReg + " = " + sourceReg);
                } else if (sourceReg == null) {
                    result.instructions.add("t0 = " + arg);
                    result.instructions.add(stackReg + " = t0");
                }
            }
        }
    }
    private List<String> findCycle(String start, Map<String, List<String>> graph, Set<String> remaining) {
        List<String> path = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        if (dfs(start, start, graph, remaining, visited, path, false)) {
            return path;
        }
        return Arrays.asList(start); // Single node
    }
    private boolean dfs(String current, String target, Map<String, List<String>> graph, Set<String> remaining, Set<String> visited, List<String> path, boolean started) {
        if (!remaining.contains(current)) return false;
        if (current.equals(target) && started) {
            return true;
        }
        if (visited.contains(current)) return false;
        
        visited.add(current);
        path.add(current);
        
        for (String neighbor : graph.getOrDefault(current, Collections.emptyList())) {
            if (dfs(neighbor, target, graph, remaining, visited, path, true)) {
                return true;
            }
        }
        
        path.remove(path.size() - 1);
        return false;
    }
}