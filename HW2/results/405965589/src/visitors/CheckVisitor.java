package visitors;
import utils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import minijava.syntaxtree.*;
public class CheckVisitor extends NoOpGJDFSVisitor<String,Pair> {
    public CheckTable checkTable = new CheckTable();
    public ClassHierarchyVisitor vis = new ClassHierarchyVisitor(); 
    public Map<String, Map<String, Method>> methodMap = new HashMap<>(); //mapping classes to mehtod names and methods
    public Boolean typeChecks = true;

    public CheckVisitor(Map<String, Map<String, SymbolTable>> checkTable, Map<String, ArrayList<String>> childMap, Map<String, Map<String, Method>> methodMap){
        this.checkTable.symbolMap = checkTable;
        this.vis.childMap = childMap;
        this.methodMap = methodMap;
    }
    public String visit(Goal n, Pair p){
        n.f1.accept(this, null);
        return null;
    }
    public String visit(TypeDeclaration n, Pair p){
        n.f0.accept(this, null);
        return null;
    }
    public String visit(ClassDeclaration n, Pair p){
        String className = n.f1.f0.tokenImage;
        n.f4.accept(this, new Pair(className, null)); //this corresponds to methods
        return null; 
    }
    public String visit(ClassExtendsDeclaration n, Pair p){
        String className = n.f1.f0.tokenImage;
        n.f5.accept(this, new Pair(className, null));
        return null;
    }
    public String visit(MethodDeclaration n, Pair p){;
        String methodName = n.f2.f0.tokenImage;
        String returnType = methodMap.get(p.first).get(methodName).returnType;
        p.second = methodName;
        String returnExprType = n.f10.accept(this, p);
        if(returnExprType == null || returnType == null){
            typeChecks = false;
            return null;
        }
        if(!returnType.equals("Boolean") && !returnType.equals("Array") && !returnType.equals("Integer") &&
        !returnExprType.equals("Boolean") && !returnExprType.equals("Array") && !returnExprType.equals("Integer")){
            if(!vis.isSubtype(returnType, returnExprType)){
                typeChecks = false;
                return null;
            }
        } else if(!returnExprType.equals(returnType)){
            typeChecks = false;
            return null;
        }
        p.second = methodName;
        n.f8.accept(this, p); //this corrresponds to statements
        return null;
    }
    public String visit(ArrayType n, Pair p){
        return "Array";
    }
    public String visit(BooleanType n, Pair p){
        return "Boolean";
    }
    public String visit(IntegerType n, Pair p){
        return "Integer";
    }
    public String visit(Type n, Pair p){
        return n.f0.accept(this, p);
    }
    public String visit(Identifier n, Pair p){
        String className = p.first;
        String methodName = p.second;
        SymbolTable sym = checkTable.symbolMap.get(className).get(methodName);
        String id = n.f0.tokenImage;
        if(!sym.map.containsKey(id)){
            typeChecks = false;
            return null;
        } 
        return sym.map.get(id).get(sym.map.get(id).size() -1);
    }
    public String visit(Statement n, Pair p){
        n.f0.accept(this, p);
        return null;
    }
    public String visit(Block n, Pair p){
        n.f1.accept(this, p);
        return null;
    }
    public String visit(AssignmentStatement n, Pair p){
        //need to make sure the left is a subtype of the right
        String lhsType = n.f0.accept(this, p);
        String rhsType = n.f2.accept(this, p);
        if(lhsType == null || rhsType == null){
            typeChecks = false;
            return null;
        }
        if (!lhsType.equals("Array") && !lhsType.equals("Boolean") && !lhsType.equals("Integer") && !rhsType.equals("Array") && !rhsType.equals("Boolean") && !rhsType.equals("Integer")) {
            if(!vis.isSubtype(lhsType, rhsType)){
                typeChecks = false;
            } 
        } else if(!lhsType.equals(rhsType)){
            typeChecks = false;
        }
        return null;
    }
    public String visit(ArrayAssignmentStatement n, Pair p){
        String arrayType = n.f0.accept(this, p);
        String e1Type = n.f2.accept(this, p);
        String e2Type = n.f5.accept(this, p);
        if(e1Type == null || e2Type == null){
            typeChecks = false;
            return null;
        }
        if(!arrayType.equals("Array") || !e1Type.equals("Integer")
        || !e2Type.equals("Integer")){
            typeChecks = false;
        }
        return null;
    }
    public String visit(IfStatement n, Pair p){
        String eType = n.f2.accept(this, p); 
        if(eType == null || !eType.equals("Boolean")){
            typeChecks = false;
            return null;
        }
        n.f4.accept(this, p);
        n.f6.accept(this, p);
        return null;
    }
    public String visit(WhileStatement n, Pair p){
        String eType = n.f2.accept(this, p);
        if(eType == null || !eType.equals("Boolean")){
            typeChecks = false;
            return null;
        }
        n.f4.accept(this, p);
        return null;
    }
    public String visit(PrintStatement n, Pair p){
        String eType = n.f2.accept(this, p);
        if(eType == null || !eType.equals("Integer")){
            typeChecks = false;
        }
        return null;
    }
    public String visit(Expression n, Pair p){
        String eType = n.f0.accept(this, p);
        return eType;
    }
    public String visit(CompareExpression n, Pair p){
        String e1Type = n.f0.accept(this, p);
        String e2Type = n.f2.accept(this, p);
        if(e1Type == null || e2Type == null || !e1Type.equals("Integer")|| !e2Type.equals("Integer")){
            typeChecks = false;
            return null;
        }
        return "Boolean";
    }
    public String visit(AndExpression n, Pair p){
        String e1Type = n.f0.accept(this, p);
        String e2Type = n.f2.accept(this, p);
        if(e1Type == null || e2Type == null || !e1Type.equals("Boolean") || !e2Type.equals("Boolean")){
            typeChecks = false;
            return null;
        }
        return "Boolean";
    }
    public String visit(PlusExpression n, Pair p){
        String e1Type = n.f0.accept(this, p);
        String e2Type = n.f2.accept(this, p);
        if(e1Type == null || e2Type == null || !e1Type.equals("Integer") || !e2Type.equals("Integer")){
            typeChecks = false;
            return null;
        }
        return "Integer";
    }
    public String visit(MinusExpression n, Pair p){
        String e1Type = n.f0.accept(this, p);
        String e2Type = n.f2.accept(this, p);
        if(e1Type == null || e2Type == null || !e1Type.equals("Integer") || !e2Type.equals("Integer")){
            typeChecks = false;
            return null;
        }
        return "Integer";
    }
    public String visit(TimesExpression n, Pair p){
        String e1Type = n.f0.accept(this, p);
        String e2Type = n.f2.accept(this, p);
        if(e1Type == null || e2Type == null || !e1Type.equals("Integer") || !e2Type.equals("Integer")){
            typeChecks = false;
            return null;
        }
        return "Integer";
    }
    public String visit(ArrayLookup n, Pair p){
        String arrType = n.f0.accept(this, p);
        String eType = n.f2.accept(this, p); 
        if(arrType == null || eType == null || !arrType.equals("Array") || !eType.equals("Integer")){
            typeChecks = false;
            return null;
        }
        return "Integer";
    }
    public String visit(ArrayLength n, Pair p){
        String arrType = n.f0.accept(this, p);
        if(arrType == null || !arrType.equals("Array")){
            typeChecks = false;
            return null;
        }
        return "Integer";
    }
    public String visit(MessageSend n, Pair p){
        String objType = n.f0.accept(this, p);
        if(objType == null || objType.equals("Integer") || objType.equals("Array") || objType.equals("Boolean")){
            typeChecks = false;
            return null;
        }
        String methodName = n.f2.f0.tokenImage;
        //check if method is in methodmap
        if(methodMap.containsKey(objType)){
            if(methodMap.get(objType).containsKey(methodName)){
                Method m = methodMap.get(objType).get(methodName);
                String returnType =  m.returnType;
                n.f4.accept(this, new Pair(p.first, p.second, objType, methodName));
                if(typeChecks){
                    return returnType;
                }
            }
        }
        typeChecks = false;
        return null;
    }
    public String visit(ExpressionList n, Pair p){
        //assume that p.second is going to change to match the called method
        String exprType = n.f0.accept(this, new Pair(p.first, p.second));
        String className = p.third;
        String methodName = p.fourth;
        SymbolTable sym = checkTable.symbolMap.get(className).get(methodName);
        SymbolTable temp = new SymbolTable(sym);
        if(sym.argList.size() > 0){
            Pair firstArgRemaning = sym.argList.get(0);
            String firstArgTypeRemaining = firstArgRemaning.first;
            if(!exprType.equals("Array") && !exprType.equals("Boolean") && !exprType.equals("Integer") &&
            !firstArgTypeRemaining.equals("Array") && !firstArgTypeRemaining.equals("Boolean") && !firstArgTypeRemaining.equals("Integer")){
                if(!vis.isSubtype(firstArgTypeRemaining, exprType)){
                    typeChecks = false;
                    return null;
                }
            } else{
                if(!exprType.equals(firstArgTypeRemaining)){
                    typeChecks = false;
                    return null;
                }
            }
            sym.argList.remove(0);
        } else{
            if(exprType != null){
                typeChecks = false;
                return null;
            }
        }
        n.f1.accept(this, p);
        if(sym.argList.size() > 0){
            typeChecks = false;
            return null;
        }
        checkTable.symbolMap.get(className).put(methodName, temp);
        return null;
    }
    public String visit(ExpressionRest n, Pair p){
        String exprType = n.f1.accept(this, new Pair(p.first, p.second));
        String className = p.third;
        String methodName = p.fourth;
        SymbolTable sym = checkTable.symbolMap.get(className).get(methodName);
        if(sym.argList.size() > 0){
            Pair firstArgRemaning = sym.argList.get(0);
            String firstArgTypeRemaining = firstArgRemaning.first;
            if(!exprType.equals("Array") && !exprType.equals("Boolean") && !exprType.equals("Integer") &&
            !firstArgTypeRemaining.equals("Array") && !firstArgTypeRemaining.equals("Boolean") && !firstArgTypeRemaining.equals("Integer")){
                if(!vis.isSubtype(firstArgTypeRemaining, exprType)){
                    typeChecks = false;
                    return null;
                }
            } else{
                if(!exprType.equals(firstArgTypeRemaining)){
                    typeChecks = false;
                    return null;
                }
            }
            sym.argList.remove(0);
        } else{
            //arglist size is 0
            if(exprType != null){
                typeChecks = false;
                return null;
            }
        }
        return null;
    }
    public String visit(PrimaryExpression n, Pair p){
        String exprType = n.f0.accept(this, p);
        return exprType;
    }
    public String visit(IntegerLiteral n, Pair p){
        return "Integer";
    }
    public String visit(TrueLiteral n, Pair p){
        return "Boolean";
    }
    public String visit(FalseLiteral n, Pair p){
        return "Boolean";
    }
    public String visit(ThisExpression n, Pair p){
        return p.first; //want the classname
    }
    public String visit(ArrayAllocationExpression n, Pair p){
        String exprType = n.f3.accept(this, p);
        if(exprType == null || !exprType.equals("Integer")){
            typeChecks = false;
        }
        return "Array";
    }
    public String visit(AllocationExpression n, Pair p){
        String objType = n.f1.f0.tokenImage; //this is just the classname in 'new A()'
        return objType;
    }
    public String visit(NotExpression n, Pair p){
        String exprType = n.f1.accept(this, p);
        if(exprType == null || !exprType.equals("Boolean")){
            typeChecks = false;
        }
        return "Boolean";
    }
    public String visit(BracketExpression n, Pair p){
        String exprType = n.f1.accept(this, p);
        return exprType; 
    }
}
