package visitors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;
import minijava.visitor.GJDepthFirst;
import utils.Pair;
import utils.Method;


public class ClassFMVisitor extends GJDepthFirst<String,Pair>{
    public Map<String, ArrayList<String>> inheritanceTree= new HashMap<>();
    public Map<String, Map<String, ArrayList<Pair>>> fieldMap = new HashMap<>(); //mapping classes to mehtod names and methods
    public Map<String, Map<String, Method>> methodMap = new HashMap<>(); 

    public Boolean check = true;
    public ClassFMVisitor(Map<String, ArrayList<String>> inheritanceTree){
        this.inheritanceTree = inheritanceTree;
    }
    public String visit(Goal n, Pair argu) {
        n.f1.accept(this, argu);
        return null;
    }
    public String visit(TypeDeclaration n, Pair argu){
        n.f0.accept(this, argu); 
        return null;
    }
    public String visit(Type n, Pair argu) {
        n.f0.accept(this, argu);
        return null;
    }
    public String visit(ClassDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!fieldMap.containsKey(className)){
            fieldMap.put(className, new HashMap<String, ArrayList<Pair>>());
        }
        if(!methodMap.containsKey(className)){
            methodMap.put(className, new HashMap<String, Method>());
        }
        n.f3.accept(this, new Pair(className, null)); //this corresponds to fields
        n.f4.accept(this, new Pair(className, null)); //this corresponds to methods
        return null;
    }
    public String visit(ClassExtendsDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!fieldMap.containsKey(className)){
            fieldMap.put(className, new HashMap<String, ArrayList<Pair>>());
        }
        if(!methodMap.containsKey(className)){
            methodMap.put(className, new HashMap<String, Method>());
        } 
        n.f5.accept(this, new Pair(className, null)); //this corresponds to fields
        n.f6.accept(this, new Pair(className, null)); //methods
        return null;
    }
    public String visit(MethodDeclaration n, Pair argu){
        //will get the classname as first elem in pair and methodname second
        String methodName = n.f2.f0.tokenImage; //we get the method name before we do dfs on the
        String returnType = n.f1.accept(this, argu);
        argu.second = methodName;
        methodMap.get(argu.first).put(methodName, new Method(methodName, returnType));

        n.f4.accept(this, argu); //this corresponds to parameter list
        return null;
    }
    public String visit(FormalParameterList n, Pair argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }
    public String visit(FormalParameter n, Pair argu) {
        String type = n.f0.accept(this, argu); // f0 is the type, f1 is the identifier
        String id = n.f1.f0.tokenImage;
        Method m = methodMap.get(argu.first).get(argu.second);
        m.args.add(new Pair(type, id));
        return null;
    }
    public String visit(VarDeclaration n, Pair argu) {
        String type = n.f0.accept(this, argu);
        String id = n.f1.f0.tokenImage;
        fieldMap.get(argu.first).put(id, new ArrayList<Pair>());
        ArrayList<Pair> temp = new ArrayList<Pair>();
        temp.add(new Pair(type, id));
        fieldMap.get(argu.first).put(id,temp);
        return null;
    }
    public String visit(Identifier n, Pair argu){
        //should only reach here for field types 
        String fieldType = n.f0.tokenImage;
        return fieldType;
    }
    public String visit(ArrayType n, Pair argu){
        //have to create a new entry 
        return "Array";
    }
    public String visit(BooleanType n, Pair argu){
        return "Boolean";
    }
    public String visit(IntegerType n, Pair argu){
        return "Integer";
    }
    //now we need methods so for all classes that extends, so like another dfs and compare method objects
    //for the parent class need to make sure htat 
    public void mergeClasses(String parent, String child){
        //a is first class, b is second
        Map<String, ArrayList<Pair>> parentFields = fieldMap.get(parent);
        Map<String, ArrayList<Pair>> childFields = fieldMap.get(child);
        Map<String, Method> parentMethods = methodMap.get(parent);
        Map<String, Method> childMethods = methodMap.get(child);
        for(String entry : parentFields.keySet()){
            //if entry is in childFields, then merge
            if(childFields.containsKey(entry)){
                //merge
                ArrayList<Pair> parentArr = parentFields.get(entry);
                ArrayList<Pair> childArr = childFields.get(entry);
                childArr.addAll(0, parentArr);
            }
        }
        //now we can merge parentMethods and childMethods
        for (Map.Entry<String, Method> entry : parentMethods.entrySet()) {
            if (!childMethods.containsKey(entry.getKey())) {
                // If the child doesn't already have this method, add it
                childMethods.put(entry.getKey(), entry.getValue());
            }
        } 
    }
    public void checkMethods(){
        Set<String> visitedSet = new HashSet<>();
        for (String className : inheritanceTree.keySet()){
            if(!visitedSet.contains(className)){ //start from parents and work your way down
                dfs(className, visitedSet);
            }
        }
    }
    private void dfs(String start,Set<String> visited){
        if(start == null || inheritanceTree.get(start) == null) return;
        for (String child : inheritanceTree.get(start)){
            if(child != null){
                visited.add(start);
                mergeClasses(start, child);
                dfs(child, visited);
            }
        }
        return;
    }
}