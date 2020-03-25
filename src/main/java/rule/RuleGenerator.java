package rule;

import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.Map;

public class RuleGenerator extends Generator{
    private Map<String, Integer>    indexes = new HashMap<>();
    private Map<String, String>     nameMap = new HashMap<>();

    private int stringCount     = 0;
    private int numberCount     = 0;
    private int characterCount  = 0;
    private int nameCount       = 0;

    private boolean isCoded    = false;

    void generatorCopyRule(ASTNode node, String literal) {
        int index = indexes.getOrDefault(literal, -1);
        if (index != -1) {
            addRule(new Rule(node).addChild(Rule.Copy));
            addRule(new Rule(Rule.Copy).addChild(index + ""));
        } else  {
            String converString =  "";
            if (node instanceof CharacterLiteral) {
                converString += "CharacterLiteral";
                if (isCoded) converString += characterCount;

                characterCount ++;
            } else if (node instanceof NumberLiteral) {
                converString += "NumberLiteral";
                if (isCoded) converString += numberCount;

                numberCount ++;
            } else if (node instanceof StringLiteral) {
                converString += "StringLiteral";
                if (isCoded) converString += stringCount;

                stringCount ++;
            } else if (node instanceof SimpleName) {
                converString += "SimpleName";
                if (isCoded) converString += nameCount;

                nameCount ++;
            }

            addRule(new Rule(node).addChild(converString));

            indexes.put(literal, addedRules.size()-1);
            nameMap.put(literal, converString);
        }
    }

    @Override
    public boolean visit(StringLiteral node) {
        String literal = node.getEscapedValue();
        generatorCopyRule(node, literal);
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        String literal = node.getToken();
        generatorCopyRule(node, literal);
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        String literal = node.getEscapedValue();
        generatorCopyRule(node, literal);
        return false;
    }

    public boolean visit(SimpleName node) {
        String literal = node.getIdentifier();
        generatorCopyRule(node, literal);
        return false;
    }
}
