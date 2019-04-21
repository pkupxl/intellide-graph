package cn.edu.pku.sei.intellide.graph.qa.code_trace;

import cn.edu.pku.sei.intellide.graph.extraction.java.NameResolver;
import org.eclipse.jdt.core.dom.*;
import java.util.ArrayList;
import java.util.List;

public class CodeAnalyzer {
    private String code;
    private String type;

    public CodeAnalyzer(String code,String type){
        this.code=code;
        this.type=type;
    }

    public String getCode(){
        return code;
    }

    public String getType(){
        return type;
    }

    public String getFullNameFromCode(){
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(this.code.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        ASTVisitor codeVisitor=new CodeVisitor();
        CompilationUnit unit=(CompilationUnit)parser.createAST(null);
        unit.accept(codeVisitor);
        return ((CodeVisitor) codeVisitor).getClassName();
    }

    public String getMethodNameFromCode(){
        ASTParser parser = ASTParser.newParser(AST.JLS10);
        parser.setSource(this.code.toCharArray());
        parser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
        ASTVisitor methodVisitor=new MethodVisitor();
        TypeDeclaration  unit=(TypeDeclaration)(parser.createAST(null));
        unit.accept(methodVisitor);
        return ((MethodVisitor) methodVisitor).getMethodName();
    }

    class CodeVisitor extends ASTVisitor{
        private List<String> className=null;

        public String getClassName(){
            if(this.className!=null)
                return className.get(0);
            else return null;
        }

        public boolean visit(TypeDeclaration node){
            if(this.className==null){
                this.className=new ArrayList<String>();
                this.className.add(NameResolver.getFullName(node));
            }else{
                this.className.add(NameResolver.getFullName(node));
            }
            return false;
        }
    }

    class MethodVisitor extends ASTVisitor{
        private List<String> MethodName=null;

        public String getMethodName(){
            if(this.MethodName!=null){
                return MethodName.get(0);
            }else return null;
        }

        public boolean visit(MethodDeclaration node){
            if(this.MethodName==null){
                this.MethodName=new ArrayList<String>();
                this.MethodName.add(node.getName().toString());
            }else{
                this.MethodName.add(node.getName().toString());
            }
            return false;
        }
    }

    public static void main(String args[]){
    }
}
