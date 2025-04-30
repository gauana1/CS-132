import utils.*;
import java.io.InputStream;
import minijava.MiniJavaParser;
import minijava.syntaxtree.Goal;
import visitors.CheckVisitor;
import visitors.ClassFieldsVisitor;
import visitors.ClassHierarchyVisitor;
import visitors.ClassMethodVisitor;
import visitors.SymbolTableVisitor;

public class Typecheck {
        public static void main(String[] args) throws Exception {
            InputStream in = System.in;
            new MiniJavaParser(in);
            Goal root = MiniJavaParser.Goal();
            ClassHierarchyVisitor h= new ClassHierarchyVisitor();
            h.visit(root);
            if(h.checkCycles()){
               System.out.println("Type error");
               return;
            }
            ClassMethodVisitor methods = new ClassMethodVisitor(h.childMap);
            methods.visit(root, new Pair(null, null));
            if(!methods.checkMethods() || !methods.check){
               System.out.println("Type error");
               return;
            }
            ClassFieldsVisitor fields = new ClassFieldsVisitor();
            fields.visit(root, null);
            SymbolTableVisitor stv = new SymbolTableVisitor(h.childMap, fields.classFields, methods.classMap); //now we should have the symbol tables, ow we can just use visitors to get the methods
            stv.visit(root, null);    
            if(!stv.check){
               System.out.println("Type error");
               return;
            }
            CheckVisitor cv = new CheckVisitor(stv.finalMap , h.childMap, stv.methodMap);
            cv.visit(root, null);
            if(!cv.typeChecks){
               System.out.println("Type error");
               return;
            }
            System.out.println("Program type checked successfully");
        }

}