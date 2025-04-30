package visitors;
import minijava.visitor.*;
import minijava.syntaxtree.*;
import java.util.Enumeration;

/**
 * Provides default methods which visit each node in the tree in depth-first
 * order.  Your visitors may extend this class.
 */
public class NoOpDFSVisitor extends DepthFirstVisitor {
   //
   // Auto class visitors--probably don't need to be overridden.
   //
   public void visit(NodeList n) {
      for (Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
         e.nextElement().accept(this);
   }

   public void visit(NodeListOptional n) {
      if ( n.present() )
         for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
            e.nextElement().accept(this);
   }

   public void visit(NodeOptional n) {
      if ( n.present() )
         n.node.accept(this);
   }

   public void visit(NodeSequence n) {
      for ( Enumeration<Node> e = n.elements(); e.hasMoreElements(); )
         e.nextElement().accept(this);
   }

   public void visit(NodeToken n) { }
   public void visit(Goal n) {}
   public void visit(MainClass n) {}
   public void visit(TypeDeclaration n) {}
   public void visit(ClassDeclaration n) {}
   public void visit(ClassExtendsDeclaration n) {}
   public void visit(VarDeclaration n) {}
   public void visit(MethodDeclaration n) {}
   public void visit(FormalParameterList n) {}
   public void visit(FormalParameter n) {}
   public void visit(FormalParameterRest n) {}
   public void visit(Type n) {}
   public void visit(ArrayType n) {}
   public void visit(BooleanType n) {}
   public void visit(IntegerType n) {}
   public void visit(Statement n) {}
   public void visit(Block n) {}
   public void visit(AssignmentStatement n) {}
   public void visit(ArrayAssignmentStatement n) {}
   public void visit(IfStatement n) {}
   public void visit(WhileStatement n) {}
   public void visit(PrintStatement n) {}
   public void visit(Expression n) {}
   public void visit(AndExpression n) {}
   public void visit(CompareExpression n) {}
   public void visit(PlusExpression n) {}
   public void visit(MinusExpression n) {}
   public void visit(TimesExpression n) {}
   public void visit(ArrayLookup n) {}
   public void visit(ArrayLength n) {}
   public void visit(MessageSend n) {}
   public void visit(ExpressionList n) {}
   public void visit(ExpressionRest n) {}
   public void visit(PrimaryExpression n) {}
   public void visit(IntegerLiteral n) {}
   public void visit(TrueLiteral n) {}
   public void visit(FalseLiteral n) {}
   public void visit(Identifier n) {}
   public void visit(ThisExpression n) {}
   public void visit(ArrayAllocationExpression n) {}
   public void visit(AllocationExpression n) {}
   public void visit(NotExpression n) {}
   public void visit(BracketExpression n) {}
}
