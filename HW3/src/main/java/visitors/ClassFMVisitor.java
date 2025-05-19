package visitors;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;
import minijava.visitor.GJDepthFirst;
import utils.Pair;
import utils.Triple;
import utils.FieldInfo;
import utils.Method;


public class ClassFMVisitor extends GJDepthFirst<String,Pair>{
    public Map<String, ArrayList<String>> inheritanceTree= new HashMap<>();
    public Map<String, Map<String, FieldInfo>> fieldMap = new HashMap<>(); //mapping classes to mehtod names and methods
    public Map<String, Map<String, Method>> methodMap = new HashMap<>(); 
    public Set<String> registers = new HashSet<>();
    public Set<String> temp = new HashSet<>();
    public int fieldOffset = 0;
    public int methodOffset = 0;
    public Boolean check = true;

    public int getNextMethodOffset(String className) {
        int maxOffset = -1;
        if (!methodMap.containsKey(className)) {
            return 0; // No methods yet, so start from offset 0
        }
        for (Method method : methodMap.get(className).values()) {
            if (method.methodOffset > maxOffset) {
                maxOffset = method.methodOffset;
            }
        }
        return maxOffset + 1; // Return the next available offset
    }
    public Pair getFieldByOffset(String className, int targetOffset) {
        Map<String, FieldInfo> fields = fieldMap.get(className);
        if (fields == null) return null;

        for (FieldInfo fieldInfo : fields.values()) {
            for (Pair p : fieldInfo.fieldTypes) {
                if (p.offset == targetOffset) {
                    return p;
                }
            }
        }
        return null; // not found
    }
    public int getNextAvailableFieldOffset(String className) {
        Map<String, FieldInfo> fields = fieldMap.get(className);
        if (fields == null) return 0;

        int maxOffset = -1;

        for (FieldInfo fieldInfo : fields.values()) {
            for (Pair p : fieldInfo.fieldTypes) {
                if (p.offset > maxOffset) {
                    maxOffset = p.offset;
                }
            }
        }

        return maxOffset + 1;
    }
    public ClassFMVisitor(Map<String, ArrayList<String>> inheritanceTree){
        registers.add("a2");
        registers.add("a3");
        registers.add("a4");
        registers.add("a5");
        registers.add("a6");
        registers.add("a7");

        // Manually adding s1 to s11
        registers.add("s1");
        registers.add("s2");
        registers.add("s3");
        registers.add("s4");
        registers.add("s5");
        registers.add("s6");
        registers.add("s7");
        registers.add("s8");
        registers.add("s9");
        registers.add("s10");
        registers.add("s11");

        // Manually adding t0 to t5
        registers.add("t0");
        registers.add("t1");
        registers.add("t2");
        registers.add("t3");
        registers.add("t4");
        registers.add("t5");
        this.inheritanceTree = inheritanceTree;
    }
    public String visit(Goal n, Pair argu) {
        n.f0.accept(this, new Pair("Main", "main"));
        n.f1.accept(this, argu);
        prependArgsToAllMethods();
        checkMethods();
        prependAllFieldsToMethodVars();
        mergeAllFieldsWithOffsets();
        return null;
    }
    public String visit(TypeDeclaration n, Pair argu){
        n.f0.accept(this, argu); 
        return null;
    }
    public String visit(Type n, Pair argu) {
        return n.f0.accept(this, argu);
    }
    public String visit(MainClass n, Pair argu){
        String className = argu.first;
        String methodName = argu.second; //we get the method name before we do dfs on the
        if(!fieldMap.containsKey(className)){
            fieldMap.put(className, new HashMap<String, FieldInfo>());
        }
        if(!methodMap.containsKey(className)){
            methodMap.put(className, new HashMap<String, Method>());
        }
        String returnType = null;
        String methodLabel = "@" + argu.first + "_" + methodName;
        methodMap.get(argu.first).put(methodName, new Method(methodName, returnType, methodLabel, 0, argu.first));
        n.f14.accept(this, argu);//inits
        n.f15.accept(this, argu);//statements
        fieldOffset = 0;
        methodOffset = 0;
        return null;
    }
    public String visit(ClassDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!fieldMap.containsKey(className)){
            fieldMap.put(className, new HashMap<String, FieldInfo>());
        }
        if(!methodMap.containsKey(className)){
            methodMap.put(className, new HashMap<String, Method>());
        }
        n.f3.accept(this, new Pair(className, null)); //this corresponds to fields
        n.f4.accept(this, new Pair(className, null)); //this corresponds to methods
        fieldOffset = 0;
        methodOffset = 0;
        return null;

    }
    public String visit(ClassExtendsDeclaration n, Pair argu){
        String className = n.f1.f0.tokenImage; 
        if(!fieldMap.containsKey(className)){
            fieldMap.put(className, new HashMap<String, FieldInfo>());
        }
        if(!methodMap.containsKey(className)){
            methodMap.put(className, new HashMap<String, Method>());
        } 
        n.f5.accept(this, new Pair(className, null)); //this corresponds to fields
        n.f6.accept(this, new Pair(className, null)); //methods
        fieldOffset = 0;
        methodOffset = 0;
        return null;
    }
    public String visit(MethodDeclaration n, Pair argu){
        //will get the classname as first elem in pair and methodname second
        String methodName = n.f2.f0.tokenImage; //we get the method name before we do dfs on the
        String returnType = n.f1.accept(this, argu);
        argu.second = methodName;
        String methodLabel = "@" + argu.first + "_" + methodName;
        methodMap.get(argu.first).put(methodName, new Method(methodName, returnType, methodLabel, methodOffset, argu.first));
        methodOffset ++;
        n.f4.accept(this, argu); //this corresponds to parameter list
        n.f7.accept(this, argu); //this is for the variables
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
        if(registers.contains(id)){
            id = "z" + id;
        }
        Method m = methodMap.get(argu.first).get(argu.second);
        m.args.add(new Pair(type, id));
        return null;
    }
    public String visit(VarDeclaration n, Pair argu) {
        if(argu.second == null){
            String type = n.f0.accept(this, argu);
            String id = n.f1.f0.tokenImage;
            FieldInfo temp = new FieldInfo();
            Pair p = new Pair(type, id, argu.first);
            p.offset = fieldOffset;
            temp.fieldTypes.add(p);
            fieldOffset ++;
            fieldMap.get(argu.first).put(id,temp);
        } else{ //this means that we are in a method
            String type = n.f0.accept(this, argu);
            String id = n.f1.f0.tokenImage;
            Method m = methodMap.get(argu.first).get(argu.second);
            if (!m.vars.containsKey(id)) {
                m.vars.put(id, new ArrayList<>());
            }
            m.vars.get(id).add(new Triple(type, id, false));
        }
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
    public void mergeClasses(String parent, String child){
        Map<String, FieldInfo> parentFields = fieldMap.get(parent);
        Map<String, FieldInfo> childFields = fieldMap.get(child);
        Map<String, Method> parentMethods = methodMap.get(parent);
        Map<String, Method> childMethods = methodMap.get(child);

        for(String entry : parentFields.keySet()){
            FieldInfo parentArr = parentFields.get(entry);//merge
            FieldInfo childArr = childFields.get(entry);
            if(childArr == null){
                childArr = new FieldInfo();
                for (Pair p : parentArr.fieldTypes) {
                    Pair n = new Pair(p.first, p.second, p.originalClass);
                    int originalOffset = p.offset;

                    Pair conflict = getFieldByOffset(child, originalOffset);
                    if(conflict != null){
                        n.offset = originalOffset;
                        int newHighest = getNextAvailableFieldOffset(child);
                        conflict.offset = newHighest;
                    } else{
                        n.offset = originalOffset;
                    }
                    childArr.fieldTypes.add(n);
                }
            }else {
                for (int i = parentArr.fieldTypes.size() - 1; i >= 0; i--) {
                    Pair p = parentArr.fieldTypes.get(i);
                    Pair tp = new Pair(p.first, p.second, p.originalClass);
                    int originalOffset = p.offset;

                    // Check if child's fields already have a field with this offset
                    Pair conflict = getFieldByOffset(child, originalOffset);

                    if (conflict != null) {
                        // If there's a conflict, assign new offset
                        tp.offset = originalOffset;
                        // Also reassign a new offset to the conflicting field
                        int newOffsetForConflict = getNextAvailableFieldOffset(child);
                        conflict.offset = newOffsetForConflict;
                    } else {
                        tp.offset = originalOffset;
                    }

                    childArr.fieldTypes.add(0, tp);
                }
            } 
            fieldMap.get(child).put(entry, childArr);
            for (Method method : childMethods.values()) {
                for (Pair p : childArr.fieldTypes) { //just (type, ID) pairs for a specific ID(entry)
                    String varName = p.second;
                    ArrayList<Triple> existingList = method.vars.get(varName);
                    if (existingList == null) {
                        existingList = new ArrayList<>();
                        method.vars.put(varName, existingList);
                    }
                    Triple newTriple = new Triple(p.first, p.second, true, p.originalClass, -1);
                    int insertIdx = existingList.size(); // default to end
                    for (int i = 0; i < existingList.size(); i++) {
                        if (!existingList.get(i).third) {
                            insertIdx = i;//isField
                            break;
                        }
                    }
                    existingList.add(insertIdx, newTriple);                    
                }
            }
        }
        for (Map.Entry<String, Method> entry : parentMethods.entrySet()) { //merge parent, do it after cuz child method doesn't get the fields of inherited if its not overriden
            if (!childMethods.containsKey(entry.getKey())) {
                int highestOffset = getNextMethodOffset(child);
                Method inherited = new Method(entry.getValue());
                inherited.methodOffset = highestOffset;
                childMethods.put(entry.getKey(), inherited); 
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
        fieldOffset = 0;
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
    public void prependArgsToAllMethods() {
        for (Map<String, Method> classMethods : methodMap.values()) {
            for (Method m : classMethods.values()) {
                m.prependArgsToVars();
            }
        }
    }
    public void prependAllFieldsToMethodVars() {
        for (String className : fieldMap.keySet()) {
            Map<String, FieldInfo> fields = fieldMap.get(className);
            Map<String, Method> methods = methodMap.get(className);
            for (Method method : methods.values()) {
                for (Map.Entry<String, FieldInfo> entry : fields.entrySet()) {
                    String varName = entry.getKey();
                    FieldInfo info = entry.getValue();
                    for (int i = 0; i < info.fieldTypes.size(); i++) {
                        Pair p = info.fieldTypes.get(i);
                        ArrayList<Triple> existing = method.vars.get(varName);
                        if (existing == null) {
                            existing = new ArrayList<>();
                            method.vars.put(varName, existing);
                        }
                        Triple newTriple = new Triple((String) p.first, (String) p.second, true, p.originalClass,-1);
                        int insertIdx = existing.size(); // default to end
                        for (int j = 0; j < existing.size(); j++) {
                            if (!existing.get(j).third) {
                                insertIdx = j;
                                break;
                            }
                        }
                        existing.add(insertIdx, newTriple);
                    }
                }
            }
        }
    }
    public void mergeAllFieldsWithOffsets() {
        Set<String> visited = new HashSet<>();
        for (String className : inheritanceTree.keySet()) {
            if (!visited.contains(className)) {
                dfsFieldMerge(className, visited);
            }
        }
    }

    // DFS traversal to ensure parent fields are merged first
    private void dfsFieldMerge(String className, Set<String> visited) {
        if (visited.contains(className)) return;
        visited.add(className);

        List<String> children = inheritanceTree.getOrDefault(className, new ArrayList<>());
        if(children != null){
            for (String child : children) {
                dfsFieldMerge(child, visited);
            }
        }
        // Merge parent fields into this class
        for (String parent : inheritanceTree.keySet()) {
            if (inheritanceTree.get(parent) != null && inheritanceTree.get(parent).contains(className)) {
                mergeParentFieldsIntoChild(parent, className);
            }
        }
        assignFieldOffsets(className); // Assign offset per Pair
    }

    // Merge all parent FieldInfos into the child's FieldInfos
    private void mergeParentFieldsIntoChild(String parent, String child) {
        Map<String, FieldInfo> parentFields = fieldMap.getOrDefault(parent, new HashMap<>());
        Map<String, FieldInfo> childFields = fieldMap.getOrDefault(child, new HashMap<>());

        for (Map.Entry<String, FieldInfo> entry : parentFields.entrySet()) {
            String fieldName = entry.getKey();
            FieldInfo parentInfo = childFields.get(fieldName);
            FieldInfo childInfo = new FieldInfo();
            if (parentInfo != null) {
                for (Pair p : parentInfo.fieldTypes) {
                    childInfo.fieldTypes.add(new Pair(p.first, p.second, p.originalClass));
                }
            } 
            for (Pair p : parentInfo.fieldTypes) {
                boolean alreadyExists = childInfo.fieldTypes.stream().anyMatch(
                    existing -> existing.originalClass.equals(p.originalClass) &&
                                existing.second.equals(p.second));
                if (!alreadyExists) {
                    childInfo.fieldTypes.add(new Pair(p.first, p.second, p.originalClass));
                }
            }

            childFields.put(fieldName, childInfo);
        }

        fieldMap.put(child, childFields);
    }

    // Assign a unique offset to each Pair in the fieldTypes list
    private void assignFieldOffsets(String className) {
        Map<String, FieldInfo> fields = fieldMap.getOrDefault(className, new HashMap<>());
        Set<String> seen = new HashSet<>();
        int offset = 0;

        for (Map.Entry<String, FieldInfo> entry : fields.entrySet()) {
            FieldInfo info = entry.getValue();
            for (Pair   p : info.fieldTypes) {
                // Include the field name, its declaring class, AND the current class
                String key = className + "::" + p.second + "::" + p.originalClass;
                if (!seen.contains(key)) {
                    p.offset = offset++;
                    seen.add(key);
                }
            }
        }
    }
    
    
        
}
 
 
