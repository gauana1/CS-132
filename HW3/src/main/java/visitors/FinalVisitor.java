package visitors;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;
import minijava.visitor.GJDepthFirst;
import utils.Method;
import utils.Pair;
import utils.Triple;
import utils.CustomStruct;
import utils.FieldInfo;

public class FinalVisitor extends GJDepthFirst<CustomStruct, Pair>{
    public int variable_count = 0;
    public Map<String, ArrayList<String>> inheritanceTree= new HashMap<>(); 
    public Map<String, Map<String, FieldInfo>> fieldMap = new HashMap<>(); //mapping classes to fieldnames
    public Map<String, Map<String, Method>> methodMap = new HashMap<>(); 
    public Set<String> registers = new HashSet<>();
    public FinalVisitor(Map<String, ArrayList<String>> inheritanceTree,Map<String, Map<String, FieldInfo>> fieldMap, 
    Map<String, Map<String, Method>> methodMap, Set<String> registers){
        this.inheritanceTree = inheritanceTree;
        this.fieldMap = fieldMap;
        this.methodMap = methodMap;
        this.registers = registers;
    } 

    public String generateLabel(){
        String x =  "v" + Integer.toString(variable_count);
        variable_count ++;
        return x;
    }

    public CustomStruct visit(NodeList n, Pair argu) {
        CustomStruct res = new CustomStruct();
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            (e.nextElement()).accept(this, argu);
        }
        return res;
    } 

    public CustomStruct visit(NodeListOptional n, Pair argu) {
        CustomStruct result = new CustomStruct();
        for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); ) {
            CustomStruct child = ((Node) e.nextElement()).accept(this, argu);
            if (child != null) {
                result.instructions.addAll(child.instructions);
                result.resultVar = child.resultVar;
                result.type = child.type;
            }
        }
        return result;
    }

    public CustomStruct visit(NodeOptional n, Pair argu) {
        return n.present() ? n.node.accept(this, argu) : new CustomStruct();
    }
    public CustomStruct visit(Goal n, Pair argu){
        CustomStruct mainResult = n.f0.accept(this, argu);
        CustomStruct classResult = n.f1.accept(this, argu);
        CustomStruct result = new CustomStruct();
        result.instructions.addAll(mainResult.instructions);
        result.instructions.addAll(classResult.instructions);
        return result; 
    }
    public CustomStruct visit(MainClass n, Pair argu){
        CustomStruct result = new CustomStruct();
        String funcName = "func main()";
        result.instructions.add(funcName);
        String className = "Main";
        String methodName = "main";
        CustomStruct x= n.f15.accept(this, new Pair(className, methodName));
        result.instructions.addAll(x.instructions); 
        String retVal = generateLabel();
        result.instructions.add(retVal + " = 0");
        result.instructions.add("return " + retVal);
        return result;
    }
    public CustomStruct visit(TypeDeclaration n, Pair argu){
        CustomStruct result = new CustomStruct();
        CustomStruct methodResult = n.f0.accept(this, argu);
        result.instructions.addAll(methodResult.instructions); 
        return result;
    }
    public CustomStruct visit(ClassDeclaration n, Pair argu){
        CustomStruct result = new CustomStruct();
        String className = n.f1.f0.tokenImage;
        CustomStruct methodResults = n.f4.accept(this, new Pair(className, null)); //methods
        result.instructions.addAll(methodResults.instructions); 
        return result;
    }
    public CustomStruct visit(ClassExtendsDeclaration n, Pair argu){
        CustomStruct result = new CustomStruct();
        String className = n.f1.f0.tokenImage;
        CustomStruct methodResults = n.f6.accept(this, new Pair(className, null));//methods
        result.instructions.addAll(methodResults.instructions); 
        return result;
    }
    public CustomStruct visit(MethodDeclaration n, Pair argu){
        CustomStruct result = new CustomStruct();
        String methodName = n.f2.f0.tokenImage;
        String heading = "func " + argu.first + "_" + methodName;

        //do smth here like pass in the proper scope when you get the method struct
        Method m = methodMap.get(argu.first).get(methodName);
        CustomStruct methodStruct;
        if(!m.originalClass.equals(argu.first)){
            methodStruct = n.f8.accept(this, new Pair(m.originalClass, methodName));
        } else{
            methodStruct = n.f8.accept(this, new Pair(argu.first, methodName));
        }


        String argsString = "(this";
        for(Pair arg:m.args){
            argsString += " " + arg.second;

        }
        argsString += ")";
        result.instructions.add(heading + argsString);

        result.instructions.addAll(methodStruct.instructions);
            
        CustomStruct returnStruct = n.f10.accept(this, new Pair(argu.first, methodName));
        result.instructions.addAll(returnStruct.instructions);
        result.instructions.add("return " + returnStruct.resultVar);
        return result;
    }
    public CustomStruct visit(Statement n, Pair argu){
        CustomStruct res = n.f0.accept(this, argu);
        return res;
    }
    public CustomStruct visit(Block n, Pair argu){
        return n.f1.accept(this, argu); //maybe change this later
    }
    public CustomStruct visit(AssignmentStatement n, Pair argu){
        CustomStruct result = new CustomStruct();
        CustomStruct rhsStruct = n.f2.accept(this, argu);//rhs
        result.instructions.addAll(rhsStruct.instructions);
        CustomStruct lhsStruct = n.f0.accept(this, argu);
        String lhsName = n.f0.f0.tokenImage;
        Method m = methodMap.get(argu.first).get(argu.second);
        ArrayList<Triple> temp = m.vars.get(lhsName);
        if(temp.get(temp.size() - 1).third == true && argu.second != null){
            //that means we are in a method and have a field 
            FieldInfo f = fieldMap.get(argu.first).get(lhsName);
            ArrayList<Pair> aL = f.fieldTypes;
            Pair last = aL.get(aL.size() - 1);
            Pair fin = new Pair(last);
            int offset = (fin.offset + 1) * 4;
            String lhsInstr = "[this + " + Integer.toString(offset) + "]";
            String rhsInstr = rhsStruct.resultVar;
            String instr = lhsInstr + "=" + rhsInstr;
            result.instructions.add(instr);
        }else{
            String t = lhsStruct.resultVar;
            String instr;
            if(m.vars.containsKey(t)){
                //then like its an identifier
                String t2 = generateLabel();
                result.instructions.add(t2 + " = " + rhsStruct.resultVar);
                instr = lhsStruct.resultVar + " = " + t2; 
            } else{
                instr = lhsStruct.resultVar + " = " + rhsStruct.resultVar;
            }
            result.instructions.add(instr);
        }
        //if the lhs is a class, then we m ight have to store the type of the lhs updated in maybe the method table
        String lhsType = temp.get(temp.size() -1).first;
        if(lhsType != "Array" && lhsType != "Boolean" && lhsType != "Integer"){
            //update lhstype with rhs type
            Triple prev = temp.get(temp.size() -1);
            Triple newLast = new Triple(rhsStruct.type, lhsName, prev.third);
            temp.add(newLast);
        }
        return result;
    }
    public CustomStruct visit(ArrayAssignmentStatement n, Pair argu){
        CustomStruct result = new CustomStruct();

        CustomStruct arrayStruct = n.f0.accept(this, argu);
        CustomStruct indexStruct = n.f2.accept(this, argu);
        
        result.instructions.addAll(arrayStruct.instructions);
        result.instructions.addAll(indexStruct.instructions);

        CustomStruct rhsExpr = n.f5.accept(this, argu); 
        result.instructions.addAll(rhsExpr.instructions);

        String zero = generateLabel();
        result.instructions.add(zero + " = 0");
        String one = generateLabel();
        result.instructions.add(one + " = 1");
        String negOne = generateLabel();
        result.instructions.add(negOne + " = " + zero + " - " + one);

        String lowBoundCheck = generateLabel();
        result.instructions.add(lowBoundCheck + " = " + negOne + " < " + indexStruct.resultVar);

        String length = generateLabel();
        result.instructions.add(length + " = [" + arrayStruct.resultVar + " + 0]");

        String highBoundCheck = generateLabel();
        result.instructions.add(highBoundCheck + " = " + indexStruct.resultVar + " < " + length);

        String inBounds = generateLabel();
        result.instructions.add(inBounds + " = " + lowBoundCheck + " * " + highBoundCheck);

        result.instructions.add("if0 " + inBounds + " goto " + lowBoundCheck + "Error");
        result.instructions.add("goto " + lowBoundCheck + "End");
        result.instructions.add(lowBoundCheck + "Error:");
        result.instructions.add("error(\"array index out of bounds\")");
        result.instructions.add(lowBoundCheck + "End:"); 

        String four = generateLabel();
        result.instructions.add(four + " = " + " 4 ");
        String actualOffset = generateLabel();
        String indPlusOne = generateLabel();
        result.instructions.add(indPlusOne + " = " + indexStruct.resultVar + " + " + one);
        result.instructions.add(actualOffset + " = " + indPlusOne + " * " + four);

        String indexAddress = generateLabel();
        result.instructions.add(indexAddress + " = " + arrayStruct.resultVar + " + " + actualOffset);


        result.instructions.add("[ " + indexAddress + "+ 0 ]" + " = " + rhsExpr.resultVar);
        return result;

    }
    public CustomStruct visit(IfStatement n, Pair argu){
        CustomStruct result = new CustomStruct();
        CustomStruct expr = n.f2.accept(this, argu); //expr
        result.instructions.addAll(expr.instructions);
        CustomStruct s1 = n.f4.accept(this, argu); //statement1
        CustomStruct s2 = n.f6.accept(this, argu); //statement2
        String header = "if0 " + expr.resultVar + " goto " + expr.resultVar + "Else";
        result.instructions.add(header);

        result.instructions.addAll(s1.instructions);
        String gotoEnd = "goto " + expr.resultVar + "End";
        result.instructions.add(gotoEnd);
        String elseInstr = expr.resultVar + "Else:";
        result.instructions.add(elseInstr);
        result.instructions.addAll(s2.instructions);
        
        result.instructions.add(expr.resultVar + "End:");
        return result;
    }
    public CustomStruct visit(WhileStatement n, Pair argu){

        CustomStruct result = new CustomStruct();
        String loopVar = "loop" + generateLabel();
        result.instructions.add(loopVar + ":");

        CustomStruct expr = n.f2.accept(this, argu); //expr
        result.instructions.addAll(expr.instructions);

        CustomStruct s1 = n.f4.accept(this, argu); //statement1
        String jumpString = "if0 " + expr.resultVar + " goto " + loopVar + "End";
        result.instructions.add(jumpString);
        result.instructions.addAll(s1.instructions);

        result.instructions.add("goto " + loopVar);
        result.instructions.add(loopVar + "End:");
        return result;

    }
    public CustomStruct visit(PrintStatement n, Pair argu){
        CustomStruct result = new CustomStruct();
        CustomStruct expr = n.f2.accept(this, argu); //expr
        result.instructions.addAll(expr.instructions);
        result.instructions.add("print(" + expr.resultVar + ")");
        return result;
    }
    public CustomStruct visit(Expression n, Pair argu){
        return n.f0.accept(this, argu); //expr
    }
    public CustomStruct visit(AndExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();

        // Evaluate LHS first
        CustomStruct lhs = n.f0.accept(this, argu);
        result.instructions.addAll(lhs.instructions);

        // Short-circuit labels
        String shortCircuitFalse = "AndExprFalse";
        String shortCircuitEnd = "AndExprEnd";

        // If lhs == 0, jump to false label (short circuit)
        result.instructions.add("if0 " + lhs.resultVar + " goto " + shortCircuitFalse);
        
        // Evaluate RHS only if lhs != 0
        CustomStruct rhs = n.f2.accept(this, argu);
        result.instructions.addAll(rhs.instructions);

        // Assign result = rhs.resultVar
        result.resultVar = "andResult";
        result.instructions.add(result.resultVar + " = " + rhs.resultVar);

        // Jump to end
        result.instructions.add("goto " + shortCircuitEnd);

        // False label: lhs was false, so result = 0
        result.instructions.add(shortCircuitFalse + ":");
        result.instructions.add(result.resultVar + " = 0");

        // End label
        result.instructions.add(shortCircuitEnd + ":");

        return result;
    }
 

    public CustomStruct visit(CompareExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();
        CustomStruct lhs = n.f0.accept(this, argu); //first expr
        CustomStruct rhs = n.f2.accept(this, argu); //second expr
        result.instructions.addAll(lhs.instructions);
        result.instructions.addAll(rhs.instructions);
        result.resultVar = generateLabel();
        String andInstr = result.resultVar + "=" + lhs.resultVar + " < " + rhs.resultVar;
        result.instructions.add(andInstr);
        return result;
    }

    public CustomStruct visit(PlusExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();
        CustomStruct lhs = n.f0.accept(this, argu); //first expr
        CustomStruct rhs = n.f2.accept(this, argu); //second expr
        result.instructions.addAll(lhs.instructions);
        result.instructions.addAll(rhs.instructions);
        result.resultVar = generateLabel();
        String andInstr = result.resultVar + "=" + lhs.resultVar + " + " + rhs.resultVar;
        result.instructions.add(andInstr);
        return result;
    }

    public CustomStruct visit(MinusExpression n, Pair argu) {
       CustomStruct result = new CustomStruct();
        CustomStruct lhs = n.f0.accept(this, argu); //first expr
        CustomStruct rhs = n.f2.accept(this, argu); //second expr
        result.instructions.addAll(lhs.instructions);
        result.instructions.addAll(rhs.instructions);
        result.resultVar = generateLabel();
        String andInstr = result.resultVar + "=" + lhs.resultVar + " - " + rhs.resultVar;
        result.instructions.add(andInstr);
        return result;
    }

    public CustomStruct visit(TimesExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();
        CustomStruct lhs = n.f0.accept(this, argu); //first expr
        CustomStruct rhs = n.f2.accept(this, argu); //second expr
        result.instructions.addAll(lhs.instructions);
        result.instructions.addAll(rhs.instructions);
        result.resultVar = generateLabel();
        String andInstr = result.resultVar + "=" + lhs.resultVar + " * " + rhs.resultVar;
        result.instructions.add(andInstr);
        return result;
    }

    public CustomStruct visit(ArrayLookup n, Pair argu) {
        CustomStruct result = new CustomStruct();

        CustomStruct arrayStruct = n.f0.accept(this, argu);
        CustomStruct indexStruct = n.f2.accept(this, argu);

        result.instructions.addAll(arrayStruct.instructions);
        result.instructions.addAll(indexStruct.instructions);

        String zero = generateLabel();
        result.instructions.add(zero + " = 0");
        String one = generateLabel();
        result.instructions.add(one + " = 1");
        String negOne = generateLabel();
        result.instructions.add(negOne + " = " + zero + " - " + one); 

        String lowBoundCheck = generateLabel();
        result.instructions.add(lowBoundCheck + " = " + negOne + " < " + indexStruct.resultVar);

        String length = generateLabel();
        result.instructions.add(length + " = [" + arrayStruct.resultVar + " + 0]");

        String highBoundCheck = generateLabel();
        result.instructions.add(highBoundCheck + " = " + indexStruct.resultVar + " < " + length);

        String inBounds = generateLabel();
        result.instructions.add(inBounds + " = " + lowBoundCheck + " * " + highBoundCheck);
        

        
        result.instructions.add("if0 " + inBounds + " goto " + lowBoundCheck + "Error");
        result.instructions.add("goto " + lowBoundCheck + "End");
        result.instructions.add(lowBoundCheck + "Error:");
        result.instructions.add("error(\"array index out of bounds\")");
        result.instructions.add(lowBoundCheck + "End:");

        String four = generateLabel();
        result.instructions.add(four + " = 4");
        String actualOffset = generateLabel();
        String indPlusOne = generateLabel();
        result.instructions.add(indPlusOne + " = " + indexStruct.resultVar + " + " + one);
        result.instructions.add(actualOffset + " = " + indPlusOne + " * " + four);

        String elemAddress = generateLabel();
        result.instructions.add(elemAddress + " = " + arrayStruct.resultVar + " + " + actualOffset);
        result.instructions.add(elemAddress+" = ["+elemAddress+" + 0]");

        result.resultVar = elemAddress;

        return result;
    }
    public CustomStruct visit(ArrayLength n, Pair argu) {
        CustomStruct result = new CustomStruct();
        CustomStruct expr =  n.f0.accept(this, argu); //variable array is stored in
        result.instructions.addAll(expr.instructions);
        String fin = generateLabel();
        String lengthInstr = fin + " = [" + expr.resultVar + " + 0]";
        result.instructions.add(lengthInstr);
        result.resultVar = fin;
        return result;
    }

    public CustomStruct visit(MessageSend n, Pair argu) {
        CustomStruct res = new CustomStruct();

        // Step 1: Evaluate object on which method is invoked (e.g., obj in obj.foo())
        CustomStruct obj = n.f0.accept(this, argu);
        res.instructions = new ArrayList<>(obj.instructions);
        String objTemp = obj.resultVar;

        // Step 2: Runtime null pointer check
        String labelBase = generateLabel(); // base label for jumps
        String errorLabel = labelBase + "Error";
        String continueLabel = labelBase + "Continue";

        // if objTemp == 0 (null), jump to error
        res.instructions.add("if0 " + objTemp + " goto " + errorLabel);
        // else jump to continue
        res.instructions.add("goto " + continueLabel);

        // error block
        res.instructions.add(errorLabel + ":");
        res.instructions.add("error(\"null pointer\")");

        // continue execution here
        res.instructions.add(continueLabel + ":");

        // Step 3: Get method details
        String methodId = n.f2.f0.tokenImage;
        String classOfObj = obj.type;

        Method methodInfo = methodMap.get(classOfObj).get(methodId);
        String orignalClass = methodInfo.originalClass;
        int byteOffset = methodInfo.methodOffset * 4;

        // Step 4: Load method from vtable
        String vtableTemp = generateLabel();
        String methodAddrTemp = generateLabel();

        res.instructions.add(vtableTemp + " = [" + objTemp + " + 0]");
        res.instructions.add(methodAddrTemp + " = [" + vtableTemp + " + " + byteOffset + "]");

        // Step 5: Evaluate method arguments
        List<String> allArgs = new ArrayList<>();
        allArgs.add(objTemp); // 'this' pointer

        if (n.f4.present()) {
            CustomStruct paramStruct = n.f4.accept(this, argu);
            res.instructions.addAll(paramStruct.instructions);
            allArgs.addAll(paramStruct.args);
        }

        // Step 6: Call method
        String callResult = generateLabel();
        res.instructions.add(callResult + " = call " + methodAddrTemp + "(" + String.join(" ", allArgs) + ")");
        res.resultVar = callResult;
        res.type = methodInfo.returnType;
        return res;
    }

 

    public CustomStruct visit(ExpressionList n, Pair argu) {
        CustomStruct result = new CustomStruct();
        ArrayList<String> args = new ArrayList<>();

        CustomStruct firstExpr = n.f0.accept(this, argu);
        result.instructions.addAll(firstExpr.instructions);
        args.add(firstExpr.resultVar);

        for (Node node : n.f1.nodes) {
            CustomStruct expr = ((ExpressionRest) node).f1.accept(this, argu);
            result.instructions.addAll(expr.instructions);
            args.add(expr.resultVar);
        }

        result.args = args;
        return result;
    }

    public CustomStruct visit(ExpressionRest n, Pair argu) {
        return n.f1.accept(this, argu); //expression
    }

    public CustomStruct visit(PrimaryExpression n, Pair argu) {
        CustomStruct res = n.f0.accept(this, argu);
        return res;
    }

    public CustomStruct visit(IntegerLiteral n, Pair argu) {
        CustomStruct result = new CustomStruct();
        String num = n.f0.tokenImage;
        result.resultVar = generateLabel();
        String intString = result.resultVar + " = " + num;
        result.instructions.add(intString);
        result.isInt = true;
        return result;
    }

    public CustomStruct visit(TrueLiteral n, Pair argu) {
        CustomStruct result = new CustomStruct();
        String bl = "1";
        result.resultVar = generateLabel();
        String intString = result.resultVar + " = " + bl;
        result.instructions.add(intString);
        return result; 
    }

    public CustomStruct visit(FalseLiteral n, Pair argu) {
        CustomStruct result = new CustomStruct();
        String bl = "0";
        result.resultVar = generateLabel();
        String intString = result.resultVar + " = " + bl;
        result.instructions.add(intString);
        return result;  
    }

    public CustomStruct visit(Identifier n, Pair argu) {
        CustomStruct result = new CustomStruct();

        String idName = n.f0.tokenImage;
        if(registers.contains(idName)){
            idName = "z" + idName;
        }
        String className = argu.first;
        String methodName = argu.second;
        Method m = methodMap.get(className).get(methodName);
        if (m.vars.get(idName).get(m.vars.get(idName).size() - 1).third == true) {
            //this means its a field
            FieldInfo f = fieldMap.get(argu.first).get(idName);
            ArrayList<Pair> aL = f.fieldTypes;
            Pair last = aL.get(aL.size() - 1);
            Pair fin = new Pair(last);
            int offset = (fin.offset + 1) * 4;
            String temp = generateLabel();
            String instr = temp + " = [this + " + offset + "]";
            String type = fieldMap.get(className).get(idName).fieldTypes.get(0).first;
            if (!type.equals("Integer") && !type.equals("Boolean") && !type.equals("Array")) {
                result.type = type;
            }
            result.instructions.add(instr);
            result.resultVar = temp;

        } else{
            //method param or method var
            result.resultVar = idName;
            String type = m.vars.get(idName).get(m.vars.get(idName).size() - 1).first;
            if (!type.equals("Integer") && !type.equals("Boolean") && !type.equals("Array")) {
                result.type = type;
            }

        }
        return result;
    }

    public CustomStruct visit(ThisExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();
        result.resultVar = "this";
        result.type = argu.first;
        return result;
    }

    public CustomStruct visit(ArrayAllocationExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();

        CustomStruct sizeStruct = n.f3.accept(this, argu);
        result.instructions.addAll(sizeStruct.instructions);

        String zero = generateLabel();
        result.instructions.add(zero + " = 0");

        String sizeOK = generateLabel();
        result.instructions.add(sizeOK + " = " + zero + " < " + sizeStruct.resultVar);

        result.instructions.add("if0 " + sizeOK + " goto " + sizeOK + "Error");
        result.instructions.add("goto " + sizeOK + "End");
        result.instructions.add(sizeOK + "Error:");
        result.instructions.add("error(\"array index out of bounds\")");

        result.instructions.add(sizeOK+"End:");
        
        // Add 1 to the size for the length field
        String one = generateLabel();
        result.instructions.add(one + " = 1");

        String allocWords = generateLabel();
        result.instructions.add(allocWords + " = " + sizeStruct.resultVar + " + " + one);

        // Multiply by 4 to get size in bytes
        String four = generateLabel();
        result.instructions.add(four + " = 4");

        String allocBytes = generateLabel();
        result.instructions.add(allocBytes + " = " + allocWords + " * " + four);

        //  Allocate memory
        String arrayPtr = generateLabel();
        result.instructions.add(arrayPtr + " = alloc(" + allocBytes + ")");

        //  Store size at [arrayPtr + 0]
        result.instructions.add("[ " + arrayPtr + " + 0 ] = " + sizeStruct.resultVar);

        // Return the array pointer
        result.resultVar = arrayPtr;
        return result;
    }
 

    public CustomStruct visit(AllocationExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();

        String className = n.f1.f0.tokenImage;
        // Number of fields + 1 for vtable pointer
        Map<String, FieldInfo> fields = fieldMap.get(className);
        int count = 0;
        for (FieldInfo fieldInfo : fields.values()) {
            if (fieldInfo.fieldTypes != null) {
                count += fieldInfo.fieldTypes.size();
            }
        }
        int numFields = count + 1;

        // Allocate object memory
        String sizeVar = generateLabel();
        String fourVar = generateLabel();
        String totalBytesVar = generateLabel();
        String objPtrVar = generateLabel();

        result.instructions.add(sizeVar + " = " + numFields);
        result.instructions.add(fourVar + " = 4");
        result.instructions.add(totalBytesVar + " = " + sizeVar + " * " + fourVar);
        result.instructions.add(objPtrVar + " = alloc(" + totalBytesVar + ")");

        // Allocate vtable memory with explicit vmt_<className> name
        int numMethods = methodMap.get(className).size();
        if(numMethods == 0){
            result.resultVar = objPtrVar;
            result.type = className;
            return result;
        }
        String vmtSizeVar = generateLabel();
        String vmtPtrVar = "vmt_" + className;
        result.instructions.add(vmtSizeVar + " = " + (numMethods * 4));
        result.instructions.add(vmtPtrVar + " = alloc(" + vmtSizeVar + ")");

        // Set vtable pointer at offset 0 in the object
        result.instructions.add("[" + objPtrVar + " + 0] = " + vmtPtrVar);

        // Fill the vtable entries
        for (String methodName : methodMap.get(className).keySet()) {
            Method m = methodMap.get(className).get(methodName);
            String methodLabelVar = generateLabel();
            int offset = 4 * m.methodOffset; 
            result.instructions.add(methodLabelVar + " = @" + m.originalClass + "_" + methodName);
            result.instructions.add("[" + vmtPtrVar + " + " + offset + "] = " + methodLabelVar);
        }
    
        result.instructions.add("if0 "+objPtrVar+" goto "+objPtrVar+"Error");
        result.instructions.add("goto "+objPtrVar+"End");
        result.instructions.add(objPtrVar+"Error:");
        result.instructions.add("error(\"null pointer\")");
        result.instructions.add(objPtrVar+"End:");
        result.resultVar = objPtrVar;
        result.type = className;
        return result;
    } 
 
    

    public CustomStruct visit(NotExpression n, Pair argu) {
        CustomStruct result = new CustomStruct();
        CustomStruct expr =  n.f1.accept(this, argu);
        result.instructions.addAll(expr.instructions);
        result.resultVar = generateLabel();
        String one = generateLabel();
        result.instructions.add(one + " = 1");
        String instr = result.resultVar + " = " + one + " - " + expr.resultVar;
        result.instructions.add(instr);
        return result;
    }

    public CustomStruct visit(BracketExpression n, Pair argu) {
        CustomStruct result = n.f1.accept(this, argu); //expr
        return result;
    }
    
}
