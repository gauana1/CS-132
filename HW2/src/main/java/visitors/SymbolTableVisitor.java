package visitors;

import java.util.Map;
import java.util.Set;

import utils.*;
import utils.Method.Argument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import minijava.syntaxtree.*;

public class SymbolTableVisitor extends NoOpGJDFSVisitor<String, SymbolTable> {
    public OuterTable outerTable = new OuterTable();
    public Map<String, ArrayList<String>> classHierarchies = new HashMap<>();
    public Map<String, ArrayList<Pair>> classFields = new HashMap<>();
    public Map<String, Map<String, Method>> methodMap = new HashMap<>(); //mapping classes to mehtod names and methods
    public Map<String, Map<String, SymbolTable>> finalMap = new HashMap<>();
    public Boolean check = true;

    public SymbolTableVisitor(Map<String, ArrayList<String>> classHierarchies,
            Map<String, ArrayList<Pair>> classFields, Map<String, Map<String, Method>> methodMap) {
        // so we are given class hierarchies, and all the fields for particular class
        this.classHierarchies = classHierarchies; 
        this.classFields = classFields;
        this.methodMap = methodMap;
        Set<String> visitedSet = new HashSet<>();
        for (String className : classFields.keySet()) {
            if(!visitedSet.contains(className)){
                dfs(className, visitedSet);
            }
        }
    }

    private void dfs(String start, Set<String> visited) {
        // i am just going to dfs through all the classes here
        if (start == null)
            return;
        ArrayList<Pair> parentFields = classFields.get(start);
        prePopulate(start, parentFields); //need to prepopulate the outermap with the parent
        visited.add(start);
        if(!classHierarchies.containsKey(start) || classHierarchies.get(start) == null){return;}
        for (String child : classHierarchies.get(start)) {
            //now we have to merge parent fields into child fields
            SymbolTable parentTable = outerTable.outerMap.get(start);
            //child should get the same symbol table deepcopied and then you can call prepopulate on the child
            SymbolTable tempChildTable = new SymbolTable(parentTable, child);
            outerTable.outerMap.put(child, tempChildTable);
            dfs(child, visited);
        }
    }

    private void prePopulate(String name, ArrayList<Pair> fields) {
        SymbolTable temp = null;
        if(!outerTable.outerMap.containsKey(name)){
            temp = new SymbolTable(name);
        } else{
            temp = outerTable.outerMap.get(name);
        }
        for (Pair field : fields) {
            // now you have the field you can start adding to symbol table
            String type = field.first;
            String id = field.second;
            ArrayList<String> tempArr = null;
            if (!temp.map.containsKey(id)) {
                tempArr = new ArrayList<>();
            } else {
                tempArr = temp.map.get(id);
            }
            tempArr.add(type);
            temp.map.put(id, tempArr);
        }
        outerTable.outerMap.put(name, temp); //put the result into the outertable, maps class to a symbol table
    }

    public String visit(Goal n, SymbolTable sym){
        n.f1.accept(this, sym);
        return null;
    }
    public String visit(TypeDeclaration n, SymbolTable sym){
        n.f0.accept(this, sym);
        return null;
    }
    public String visit(ClassDeclaration n, SymbolTable sym){
        String className = n.f1.f0.tokenImage;
        //now we can get the symtable for the class
        SymbolTable classTable = outerTable.outerMap.get(className);
        n.f4.accept(this, classTable);
        return null;
    }
    public String visit(ClassExtendsDeclaration n, SymbolTable sym){
        String className = n.f1.f0.tokenImage;
        //now we can get the symtable for the class
        SymbolTable classTable = outerTable.outerMap.get(className);
        n.f6.accept(this, classTable);
        return null;
    }
    public String visit(MethodDeclaration n, SymbolTable sym){
        //now we can merge methods with symbol table passed in
        String methodName = n.f2.f0.tokenImage;
        //get the method map for that particular 
        String className = sym.classString;
        sym = new SymbolTable(sym);
        Method m = methodMap.get(className).get(methodName);
        ArrayList<Argument> argList = m.args;
        ArrayList<Pair> argPList = new ArrayList<>();
        //now merge existing symbol table for this method with the params
        for(Argument arg : argList){
            String id = arg.name;
            if(sym.map.containsKey(id)){
                ArrayList<String> temp = sym.map.get(id);
                temp.add(arg.type);
                sym.map.put(id, temp);
            } else{
                ArrayList<String> temp = new ArrayList<>();
                temp.add(arg.type);
                sym.map.put(id, temp);
            }
            argPList.add(new Pair(arg.type,id));
        }
        n.f7.accept(this, sym); // I THINK THI SCORRESPONDS TO THE METHOD VARS
        sym.argList = argPList;
        if(!finalMap.containsKey(className)){
            finalMap.put(className, new HashMap<>());
        }
        finalMap.get(className).put(methodName, sym);
        return null;
    }
    public String visit(VarDeclaration n, SymbolTable sym){
        String type = n.f0.accept(this, sym);
        String id = n.f1.accept(this, sym);
        if(!sym.map.containsKey(id)){
            //then add it
            sym.map.put(id, new ArrayList<>(Arrays.asList(type)));
        } else{
            //then append
            // ArrayList<String> temp = sym.map.get(id);
            // temp.add(type);
            // sym.map.put(id,temp);
            check = false;
        }
        return null;
    }
    public String visit(Type n, SymbolTable sym){
        String type = n.f0.accept(this, sym);
        return type;
    }
    public String visit(ArrayType n, SymbolTable sym){
        return "Array";
    }
    public String visit(BooleanType n, SymbolTable sym){
        return "Boolean";
    }
    public String visit(IntegerType n, SymbolTable sym){
        return "Integer";
    }
    public String visit(Identifier n, SymbolTable sym){
        return n.f0.tokenImage;
    }
}
