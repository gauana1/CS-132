package visitors;
import utils.Method;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;

public class ClassMethodVisitor extends NoOpGJDFSVisitor<Void,Pair>{
    //STILL NEED TO MAKE SURE NO OVERLOADING WITHIN CLASS OCCURS
    public Map<String, ArrayList<String>> classHierarchies = new HashMap<>();
    public Map<String, Map<String, Method>> classMap = new HashMap<>(); //mapping classes to mehtod names and methods
    public Boolean check = true;
    public ClassMethodVisitor(Map<String, ArrayList<String>>  classHierarchies){
        this.classHierarchies = classHierarchies;
    }
    public Void visit(Goal n, Pair argu) {
        n.f1.accept(this, argu);
        return null;
    }
    public Void visit(TypeDeclaration n, Pair argu){
        n.f0.accept(this, argu); 
        return null;
    }
    public Void visit(Type n, Pair argu) {
        n.f0.accept(this, argu);
        return null;
    }
    public Void visit(ClassDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!classMap.containsKey(className)){
            classMap.put(className, new HashMap<String, Method>());
        }
        n.f4.accept(this, new Pair(className, null)); //this corresponds to methods
        return null;
    }
    public Void visit(ClassExtendsDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!classMap.containsKey(className)){
            classMap.put(className, new HashMap<String, Method>());
        }
        n.f6.accept(this, new Pair(className, null)); 
        return null;
    }
    public Void visit(MethodDeclaration n, Pair argu){
        //will get the classname as first elem in pair and methodname second
        String methodName = n.f2.f0.tokenImage; //we get the method name before we do dfs on the
        argu.second = methodName;
        if(!classMap.get(argu.first).containsKey(methodName)){
            classMap.get(argu.first).put(methodName, new Method(methodName, null));
        } else{
            //it already has the method which is a problem
            check = false;
            return null;
        }
        n.f1.accept(this, argu); //type
        //f4 is the list of parameters
        n.f4.accept(this, argu);
        return null;
    }
    public Void visit(FormalParameterList n, Pair argu) {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }
    public Void visit(FormalParameter n, Pair argu) {
        n.f0.accept(this, argu); // f0 is the type, f1 is the identifier
        n.f1.accept(this, argu);
        return null;
    }
    public Void visit(Identifier n, Pair argu){
        //should only reach here for parameter ids
        Method m = classMap.get(argu.first).get(argu.second);
        Method.Argument last = null;
        if(m.args.size() > 0){
            last = m.args.get(m.args.size() - 1);
        }

        if(last != null && last.name == null){ //identifier is the name
            last.name = n.f0.tokenImage; 
            m.args.set(m.args.size() - 1, last);
        }
        else if(m.returnType == null){
            m.returnType = n.f0.tokenImage;
        }
        else { // identifier is the type
            Method.Argument arg = new Method.Argument(n.f0.tokenImage, null);
            m.args.add(arg);
        }
        classMap.get(argu.first).put(argu.second, m);
        return null;
    }
    public Void visit(ArrayType n, Pair argu){
        //have to create a new entry 
        Method m = classMap.get(argu.first).get(argu.second);
        if(m.returnType == null){
            m.returnType = "Array";
            return null;
        }
        Method.Argument arg = new Method.Argument("Array", null);
        m.args.add(arg);
        m.args.set(m.args.size() - 1, arg);
        classMap.get(argu.first).put(argu.second, m);
        return null;
    }
    public Void visit(BooleanType n, Pair argu){
        //have to create a new entry 
        Method m = classMap.get(argu.first).get(argu.second);
        if(m.returnType == null){
            m.returnType = "Boolean";
            return null;
        }
        Method.Argument arg = new Method.Argument("Boolean", null);
        m.args.add(arg);
        m.args.set(m.args.size() - 1, arg);
        classMap.get(argu.first).put(argu.second, m);
        return null;
    }
    public Void visit(IntegerType n, Pair argu){
        //have to create a new entry 
        Method m = classMap.get(argu.first).get(argu.second);
        if(m.returnType == null){
            m.returnType = "Integer";
            return null;
        }
        Method.Argument arg = new Method.Argument("Integer", null);
        m.args.add(arg);
        m.args.set(m.args.size() - 1, arg);
        classMap.get(argu.first).put(argu.second, m);
        return null;
    }
    //now we need methods so for all classes that extends, so like another dfs and compare method objects
    //for the parent class need to make sure htat 
    public boolean compareClasses(String parent, String child){
        //a is first class, b is second
        Map<String, Method> parentMap = classMap.get(parent);
        Map<String, Method> childMap = classMap.get(child);
        //want to check that the child class isn't overloading anything in the parent class
        for(String methodName : childMap.keySet()){
            if(parentMap.containsKey(methodName)){
                //means that the parent has the same method, then we can do a comparison
                Method parentMethod= parentMap.get(methodName);
                Method childMethod = childMap.get(methodName);
                if(!parentMethod.equals(childMethod)) return false;
            }
        }
        //if you haven't returned false then you have to merge parentmap into childmap cuz you knkow everything is basicaly equal
        childMap.putAll(parentMap);
        classMap.put(child, childMap);
        return true;
    }
    public boolean checkMethods(){
      Set<String> visitedSet = new HashSet<>();
      for (String className : classHierarchies.keySet()){
         if(!visitedSet.contains(className)){ //start from parents and work your way down
            if(!dfs(className, visitedSet)){
               return false;
            }
         }
      }
      return true;
    }
    private boolean dfs(String start,Set<String> visited){
        if(start == null || classHierarchies.get(start) == null) return true;
        for (String child : classHierarchies.get(start)){
            if(child != null){
                visited.add(start);
                if(!compareClasses(start, child) || !dfs(child, visited)) return false;
            }
        }
        return true;
    }
}
