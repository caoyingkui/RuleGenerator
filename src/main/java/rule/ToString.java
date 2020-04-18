package rule;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;
import rule.Rule;

import java.util.Map;

public class ToString extends NaiveASTFlattener {

    public Map<String, String> nameMap;

    public ToString(Map<String, String> map) {
        this.nameMap = map;
    }

    public String toString(ASTNode node) {
        node.accept(this);
        return this.buffer.toString();
    }

    @Override
    public boolean visit(StringLiteral node) {
        String originalLiteral = node.getEscapedValue();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        String originalLiteral = node.getToken();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        String originalLiteral = node.getEscapedValue();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }

    public boolean visit(SimpleName node) {
        String originalLiteral = node.getIdentifier();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }

    public boolean visit(QualifiedType node) {
        String originalLiteral = node.toString();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }

    public boolean visit(QualifiedName node) {
        String originalLiteral = node.toString();
        this.buffer.append(nameMap.get(originalLiteral));
        return false;
    }
}
