package rule;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;
import rule.Rule;

public class ToString extends NaiveASTFlattener {

    public boolean visit(Javadoc node) {
        this.buffer.append(Rule.getJavadoc(node));
        return false;
    }

    public String toString(ASTNode node) {
        this.buffer = new StringBuffer();
        node.accept(this);
        return this.buffer.toString();
    }

    public boolean visit(StringLiteral node) {
        this.buffer.append(Rule.getStringLiteral(node));
        return false;
    }


    @Override
    public boolean visit(NumberLiteral node) {
        //String literal = node.getToken();
        this.buffer.append(Rule.getNumberLiteral(node));
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        //String literal = node.getEscapedValue();
        this.buffer.append(Rule.getCharLiteral(node));
        return false;
    }

    public boolean visit(SimpleName node) {
        this.buffer.append(Rule.getSimpleName(node));
        return false;
    }
}
