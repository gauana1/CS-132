package visitors;

import utils.*;

import java.util.ArrayList;
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

        for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
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
        if(r.registerMap.containsKey(retID)){
            //have to reassign
            String reg = r.registerMap.get(retID);
            String reassignRet = retID + " = " + reg;
            result.instructions.add(reassignRet);
        }

        // Restore callee-saved s
        for(Map.Entry<String, String> entry : r.registerMap.entrySet()){
            String reg = entry.getValue();
            char first = reg.charAt(0);
            if(first == 's'){
                String restoreReg = reg + " = stack_" + reg;
                result.instructions.add(restoreReg);
            } 
        } 

        result.instructions.add("t0 = " + n.return_id.toString());
        // result.instructions.add("print(t0)");
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
            // result.instructions.add("print(" +arg1Reg + ")");
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
            result.instructions.add("print(" + arg2Reg + ")");
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
        SparrowVStruct argAssigns = topoSortAssignArgs(n.args, regStruct, resReg);
        result.instructions.addAll(argAssigns.instructions);
        String args = "(";
        for (int i = 0; i < n.args.size(); i++) {
            if(i >= 6){
                IR.token.Identifier a = n.args.get(i);
                String arg = a.toString();
                args += " " + arg;
            }
            
        } 
        args += ")";

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

    public SparrowVStruct topoSortAssignArgs(List<IR.token.Identifier> args, RegisterStruct rStruct, String resReg) {
        SparrowVStruct result = new SparrowVStruct();

        Map<String, List<String>> graph = new HashMap<String, List<String>>();
        Set<String> nodes = new HashSet<String>();
        Map<String, Integer> indegree = new HashMap<String, Integer>();

        int numArgs = args.size();

        // Step 1: Create nodes for each arg assignment
        for (int i = 0; i < numArgs; i++) {
            String nodeName;
            if (i < 6) {
                nodeName = "assign_a" + (i + 2);
            } else {
                nodeName = "assign_stack_a" + (i + 2);
            }
            nodes.add(nodeName);
            indegree.put(nodeName, 0);
        }

        // Step 2: Build edges if register overlap causes clobbering
        for (int i = 0; i < numArgs; i++) {
            String argI = args.get(i).toString();
            String regI = rStruct.registerMap.get(argI);
            if (regI == null) continue;

            for (int j = 0; j < numArgs; j++) {
                if (i == j) continue;
                String argJ = args.get(j).toString();
                String targetRegJ = j < 6 ? "a" + (j + 2) : rStruct.registerMap.get(argJ);
                if (regI.equals(targetRegJ)) {
                    String fromNode = i < 6 ? "assign_a" + (i + 2) : "assign_stack_a" + (i + 2);
                    String toNode   = j < 6 ? "assign_a" + (j + 2) : "assign_stack_a" + (j + 2);

                    if (!graph.containsKey(fromNode)) {
                        graph.put(fromNode, new ArrayList<>());
                    }
                    graph.get(fromNode).add(toNode);

                    indegree.put(toNode, indegree.get(toNode) + 1);
                }
            }
        }

            // Step 3: Topological sort (Kahn's algorithm)
        Queue<String> queue = new LinkedList<String>();
        for (String node : nodes) {
            if (indegree.get(node) == 0) {
                queue.add(node);
            }
        }

        List<String> sorted = new ArrayList<String>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);

            List<String> neighbors = graph.get(current);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    indegree.put(neighbor, indegree.get(neighbor) - 1);
                    if (indegree.get(neighbor) == 0) {
                        queue.add(neighbor);
                    }
                }
            }
        }
        if (sorted.size() != nodes.size()) {
            // Handle cycle using t0 temp register with register shuffle logic
            // Build a list of raw assignments that would have been made
            List<String[]> moves = new ArrayList<>();
            for (String node : nodes) {
                if (node.startsWith("assign_a")) {
                    int idx = Integer.parseInt(node.substring("assign_a".length())) - 2;
                    IR.token.Identifier arg = args.get(idx);
                    String argName = arg.toString();
                    String aReg = "a" + (idx + 2);
                    String fromReg = rStruct.registerMap.getOrDefault(argName, argName);
                    moves.add(new String[]{aReg, fromReg});
                }
            }

            // Apply register shuffling logic with t0 to handle cycles
            // Create a map: destReg -> srcReg
            Map<String, String> moveMap = new HashMap<>();
            for (String[] move : moves) {
                moveMap.put(move[0], move[1]);
            }

            Set<String> visited = new HashSet<>();
            for (String dest : moveMap.keySet()) {
                if (visited.contains(dest)) continue;

                String current = dest;
                List<String> cycle = new ArrayList<>();

                while (!visited.contains(current) && moveMap.containsKey(current)) {
                    visited.add(current);
                    cycle.add(current);
                    current = moveMap.get(current);
                }

                if (cycle.size() == 1 && moveMap.get(cycle.get(0)).equals(cycle.get(0))) {
                    // No-op move, skip
                    continue;
                }

                // If a cycle or chain is detected
                String first = cycle.get(0);
                result.instructions.add("t0 = " + moveMap.get(first)); // t0 = src of first move

                // Shift all values in reverse order
                for (int i = cycle.size() - 1; i > 0; i--) {
                    String to = cycle.get(i);
                    String from = cycle.get(i - 1);
                    result.instructions.add(to + " = " + moveMap.get(from));
                }

                // Restore t0 to final dest
                result.instructions.add(cycle.get(0) + " = t0");
            }
        } else {
            // Step 4: Generate instructions for assignments in topo order
            for (String node : sorted) {
                if (node.startsWith("assign_a")) {
                    // assign arg i to a-reg
                    int idx = Integer.parseInt(node.substring("assign_a".length())) - 2;
                    IR.token.Identifier arg = args.get(idx);
                    String argName = arg.toString();
                    String aReg = "a" + (idx + 2);
                    result.instructions.add("stack_" + aReg + " = " + aReg);
                    if (rStruct.registerMap.containsKey(argName)) {
                        String fromReg = rStruct.registerMap.get(argName);
                        result.instructions.add(aReg + " = " + fromReg);
                    } else {
                        result.instructions.add(aReg + " = " + argName);
                    }

                } else if (node.startsWith("assign_stack_a")) {
                    int idx = Integer.parseInt(node.substring("assign_stack_a".length())) - 2;
                    IR.token.Identifier arg = args.get(idx);
                    String argName = arg.toString();
                    String stackReg = "stack_a" + (idx + 2);
                    if (rStruct.registerMap.containsKey(argName)) {
                        String fromReg = rStruct.registerMap.get(argName);
                        if (!fromReg.equals(resReg)) {
                            result.instructions.add(stackReg + " = " + fromReg);
                        }
                    } else {
                        result.instructions.add("t0 = " + argName);
                        result.instructions.add(stackReg + " = t0");
                    }
                }
            }
        }

        return result;
    }


}