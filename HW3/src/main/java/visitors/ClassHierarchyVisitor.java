package visitors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minijava.syntaxtree.*;
import minijava.visitor.DepthFirstVisitor;

public class ClassHierarchyVisitor extends DepthFirstVisitor {
   public Map<String, ArrayList<String>> inheritanceTree= new HashMap<>();
   public void visit(Goal n) {
      n.f1.accept(this);
   }
   //now that we have the root i need to traverse the tree
   public void visit(TypeDeclaration n) {
      n.f0.accept(this);
   }
   public void visit(ClassDeclaration n){
      String className = n.f1.f0.tokenImage; //this is the class name
      if(!inheritanceTree.containsKey(className)){
         inheritanceTree.put(className, null); 
      }
   }
   public void visit(ClassExtendsDeclaration n){
      String child = n.f1.f0.tokenImage; 
      String parent = n.f3.f0.tokenImage;
      ArrayList<String> descendantList = inheritanceTree.get(parent); //get the parent entry
      if (descendantList == null) {
         descendantList = new ArrayList<>();
     }
      descendantList.add(child);
      inheritanceTree.put(parent, descendantList); 
   }
   public boolean isSubtype(String ancestor, String descendant){
      return dfs(ancestor, descendant, new HashSet<>());
   }
   private boolean dfs(String start, String end, Set<String> visited){
      if (start == null || visited.contains(start)) return false;
      if(start.equals(end)) return true;
      visited.add(start);
      if(inheritanceTree.get(start) != null){
         for (String nm : inheritanceTree.get(start)){
            if(dfs(nm, end, visited)){
               return true;
            }
         }
      }
      return false;
   }
}