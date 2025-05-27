import java.io.InputStream;

import IR.SparrowParser;
import IR.visitor.SparrowConstructor;
import other.LinearScan;
import utils.SparrowVStruct;
import utils.Triple;
import visitors.LivenessVisitor;
import visitors.S2SVVisitor;
import IR.syntaxtree.Node;

public class S2SV {
    public static void main(String [] args) throws Exception {
        InputStream in = System.in;
        new SparrowParser(in);
        Node root = SparrowParser.Program();
        SparrowConstructor constructor = new SparrowConstructor();
        root.accept(constructor);
        sparrow.Program program = constructor.getProgram();
        S2SVVisitor sv = new S2SVVisitor();
        SparrowVStruct res = sv.visit(program, null);
        for(String line : res.instructions){
            System.out.println(line);
        }
    }
}