package visitors;
import minijava.visitor.*;
import minijava.syntaxtree.*;

public class NoOpGJDFSVisitor<R,A> extends GJDepthFirst<R,A>{  
    public R visit(NodeToken n, A argu) {return null;}
    public R visit(Goal n, A argu) {return null;}
    public R visit(MainClass n, A argu) {return null;}
    public R visit(TypeDeclaration n, A argu) {return null;}
    public R visit(ClassDeclaration n, A argu) {return null;}
    public R visit(ClassExtendsDeclaration n, A argu) {return null;}
    public R visit(VarDeclaration n, A argu) {return null;}
    public R visit(MethodDeclaration n, A argu) {return null;}
    public R visit(FormalParameterList n, A argu) {return null;}
    public R visit(FormalParameter n, A argu) {return null;}
    public R visit(Type n, A argu) {return null;}
    public R visit(ArrayType n, A argu) {return null;}
    public R visit(IntegerType n, A argu) {return null;}
    public R visit(Statement n, A argu) {return null;}
    public R visit(Block n, A argu) {return null;}
    public R visit(AssignmentStatement n, A argu) {return null;}
    public R visit(ArrayAssignmentStatement n, A argu) {return null;}
    public R visit(IfStatement n, A argu) {return null;}
    public R visit(WhileStatement n, A argu) {return null;}
    public R visit(PrintStatement n, A argu) {return null;}
    public R visit(Expression n, A argu) {return null;}
    public R visit(AndExpression n, A argu) {return null;}
    public R visit(CompareExpression n, A argu) {return null;}
    public R visit(PlusExpression n, A argu) {return null;}
    public R visit(MinusExpression n, A argu) {return null;}
    public R visit(TimesExpression n, A argu) {return null;}
    public R visit(ArrayLookup n, A argu) {return null;}
    public R visit(ArrayLength n, A argu) {return null;}
    public R visit(MessageSend n, A argu) {return null;}
    public R visit(ExpressionList n, A argu) {return null;}
    public R visit(ExpressionRest n, A argu) {return null;}
    public R visit(PrimaryExpression n, A argu) {return null;}
    public R visit(IntegerLiteral n, A argu) {return null;}
    public R visit(TrueLiteral n, A argu) {return null;}
    public R visit(FalseLiteral n, A argu) {return null;}
    public R visit(Identifier n, A argu) {return null;}
    public R visit(ThisExpression n, A argu) {return null;}
    public R visit(ArrayAllocationExpression n, A argu) {return null;} 
    public R visit(AllocationExpression n, A argu) {return null;}
    public R visit(NotExpression n, A argu) {return null;}
    public R visit(BracketExpression n, A argu) {return null;}
}
