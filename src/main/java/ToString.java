import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;

import java.util.Iterator;

public class ToString extends NaiveASTFlattener {
    public boolean visit(CharacterLiteral node) {
        this.buffer.append(Rule.getCharLiteral(node));
        return false;
    }

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
}
