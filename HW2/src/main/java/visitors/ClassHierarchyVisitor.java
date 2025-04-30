package visitors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;

public class ClassHierarchyVisitor extends NoOpDFSVisitor {
   public Map<String, ArrayList<String>> childMap = new HashMap<>();
   public Set<String> classMap = new HashSet<>();
   public boolean cycles; 
   //after you get the root, then you can sart the traveral of the tree
   public void visit(Goal n) {
      n.f0.accept(this);
      n.f1.accept(this);
   }
   //now that we have the root i need to traverse the tree
   public void visit(MainClass n){
      n.f1.accept(this); 
      String className = n.f1.f0.tokenImage; //this is the class name    
      classMap.add(className);
      childMap.put(className, null);                                     
   }
   public void visit(TypeDeclaration n) {
      n.f0.accept(this);
   }
   public void visit(ClassDeclaration n){
      String className = n.f1.f0.tokenImage; //this is the class name
      classMap.add(className);
      if(!childMap.containsKey(className)){
         childMap.put(className, null); 
      }
   }
   public void visit(ClassExtendsDeclaration n){
      String child = n.f1.f0.tokenImage; 
      String parent = n.f3.f0.tokenImage;
      classMap.add(child);
      ArrayList<String> descendantList = childMap.get(parent); //get the parent entry
      if (descendantList == null) {
         descendantList = new ArrayList<>();
     }
      descendantList.add(child);
      childMap.put(parent, descendantList); 
   }
   //now we need a method in order to check for cycles
   public boolean checkCycles(){
      Set<String> visitedSet = new HashSet<>();
      for (String className : childMap.keySet()){
         if(!classMap.contains(className)) return true;
         if(!visitedSet.contains(className)){
            if(!dfs(className, null, visitedSet)){
               cycles = true;
               return true;
            }
         }
      }
      return false;
   }
   private boolean dfs(String start, String end, Set<String> visited){
      if (start == null || visited.contains(start)) return false;
      if(start.equals(end)) return true;
      visited.add(start);
      if(childMap.get(start) != null){
         for (String nm : childMap.get(start)){
            if(!dfs(nm, end, visited)){
               return false;
            }
         }
      }
      return true;
   }
   public boolean isSubtype(String ancestor, String descendant){
      return dfs2(ancestor, descendant, new HashSet<>());
   }
   private boolean dfs2(String start, String end, Set<String> visited){
      if (start == null || visited.contains(start)) return false;
      if(start.equals(end)) return true;
      visited.add(start);
      if(childMap.get(start) != null){
         for (String nm : childMap.get(start)){
            if(dfs2(nm, end, visited)){
               return true;
            }
         }
      }
      return false;
   }
}
