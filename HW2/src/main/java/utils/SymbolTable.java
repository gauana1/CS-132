package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SymbolTable {
   public Map<String, ArrayList<String>> map = new HashMap<>(); 
   public ArrayList<Pair> argList = new ArrayList<>();
   public String classString;

   // Constructor
   public SymbolTable(String classString) {
       this.classString = classString;
   }

   public SymbolTable(SymbolTable s, String classString) {
       this.classString = classString;
       for (String key : s.map.keySet()) {
           this.map.put(key, new ArrayList<>(s.map.get(key)));
       }
   }

   // Copy constructor
   public SymbolTable(SymbolTable s) {
       this.classString = s.classString;
       
       // Deep copy of map
       for (String key : s.map.keySet()) {
           this.map.put(key, new ArrayList<>(s.map.get(key)));
       }
       
       // Deep copy of argList
       for (Pair p : s.argList) {
           this.argList.add(new Pair(p.first, p.second));  // Assuming Pair has a copy constructor
       }
   }
}
