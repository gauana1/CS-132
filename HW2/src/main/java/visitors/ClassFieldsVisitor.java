package visitors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import utils.Pair;
import minijava.syntaxtree.*;

public class ClassFieldsVisitor extends NoOpGJDFSVisitor<Void, String> {
    //what do we need here, honestly probably just the hashmap and traverse
    public Map<String, ArrayList<Pair>> classFields = new HashMap<>();
    
    public Void visit(Goal n, String argu){
        n.f1.accept(this, null);
        return null;
    }
    public Void visit(TypeDeclaration n, String argu){
        n.f0.accept(this, null);
        return null;
    }
    public Void visit(ClassDeclaration n, String argu){
        String className = n.f1.f0.tokenImage;
        classFields.put(className, new ArrayList<>());
        n.f3.accept(this, className);//this corresponds to the classFields
        return null;
    }
    public Void visit(ClassExtendsDeclaration n, String argu){
        String className = n.f1.f0.tokenImage;
        classFields.put(className, new ArrayList<>());
        n.f5.accept(this, className);//this corresponds to the classFields
        return null;
    }
    public Void visit(Type n, String argu) {
        n.f0.accept(this, argu);
        return null;
    }
    public Void visit(VarDeclaration n, String argu){
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return null;
    }
    public Void visit(Identifier n, String argu){
        ArrayList<Pair> fields = classFields.get(argu);
        Pair last = null;
        if(fields.size() != 0){
            last = fields.get(fields.size()-1);
        } 
        if(last != null && last.second == null){
            //this means that the identifier is an ID, not a type
            last.second = n.f0.tokenImage;
            fields.set(fields.size() - 1, last); // update the fields array
        } else{
            //this means that we need to add a new entry, as the id is the type
            Pair newLast = new Pair(n.f0.tokenImage, null);
            fields.add(newLast);
        }
        classFields.put(argu, fields);
        return null;
    }
    public Void visit(ArrayType n, String argu){
        //this has to be the type of the identifier
        ArrayList<Pair> fields = classFields.get(argu);
        Pair newLast = new Pair("Array", null);
        fields.add(newLast);
        classFields.put(argu, fields);
        return null;
    }   
    public Void visit(BooleanType n, String argu){
        //this has to be the type of the identifier
        ArrayList<Pair> fields = classFields.get(argu);
        Pair newLast = new Pair("Boolean", null);
        fields.add(newLast);
        classFields.put(argu, fields);
        return null;
    }
    public Void visit(IntegerType n, String argu){
        //this has to be the type of the identifier
        ArrayList<Pair> fields = classFields.get(argu);
        Pair newLast = new Pair("Integer", null);
        fields.add(newLast);
        classFields.put(argu, fields);
        return null;
    }

}
