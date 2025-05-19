import java.io.InputStream;
import minijava.MiniJavaParser;
import minijava.syntaxtree.Goal;
import utils.CustomStruct;
import utils.Pair;
import visitors.*;

public class J2S {
    public static void main(String[] args) throws Exception{
        InputStream in = System.in;
        new MiniJavaParser(in);
        Goal root = MiniJavaParser.Goal();
        ClassHierarchyVisitor chv = new ClassHierarchyVisitor();
        chv.visit(root);
        ClassFMVisitor cfmv = new ClassFMVisitor(chv.inheritanceTree);
        cfmv.visit(root, null);
        FinalVisitor fv = new FinalVisitor(cfmv.inheritanceTree, cfmv.fieldMap, cfmv.methodMap, cfmv.registers);
        CustomStruct res = fv.visit(root, new Pair(null, null));
        for (String instr : res.instructions) {
            System.out.println(instr);
        } 
    } 
}
