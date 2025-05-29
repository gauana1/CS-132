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
        LivenessVisitor lv = new LivenessVisitor();
        lv.visit(program, new Triple(null, null, 0));
        S2SVVisitor sv = new S2SVVisitor(lv.lsMap);
        SparrowVStruct res = sv.visit(program, new Triple(null, null, 0));
        for(String line : res.instructions){
            System.out.println(line);
        }
    }
}