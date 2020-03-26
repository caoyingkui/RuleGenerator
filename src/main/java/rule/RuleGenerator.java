package rule;

import dataProcess.DataInitializer;
import org.eclipse.jdt.core.dom.*;

import java.util.HashMap;
import java.util.Map;

public class RuleGenerator extends Generator{

    public Map<String, Integer> addTokens   = new HashMap<>();

    private Map<String, Integer>    indexes = new HashMap<>();
    private Map<String, String>     nameMap = new HashMap<>();

    public  static Map<String, Integer>    nameLiterals = new HashMap<>();
    public  static Map<String, Integer>    numberLiterals = new HashMap<>();


    private int stringCount     = 0;
    private int numberCount     = 0;
    private int characterCount  = 0;
    private int nameCount       = 0;

    public static boolean isCoded    = true;

    public static boolean needToCovert = true;

    // 获取Token
    // 然后才能获取rule list
    // 然后再

    /**
     * 获取token有三种情况
     * @param node
     * @return
     */
    public String getToken(ASTNode node) {
        String converString =  "";
        if (node instanceof CharacterLiteral) {
            converString += "CharacterLiteral";
            if (isCoded) converString += characterCount;

            characterCount ++;
        } else if (node instanceof NumberLiteral) {
            if (DataInitializer.parseType == 1) {
                converString = ((NumberLiteral) node).getToken();
            } else if (!DataInitializer.Numbers.contains(numberLiterals)) {
                converString += "NumberLiteral";
                if (isCoded) converString += numberCount;

                numberCount ++;
            }
            numberLiterals.put(converString, numberLiterals.getOrDefault(converString, 0) + 1);
        } else if (node instanceof StringLiteral) {
            converString += "StringLiteral";
            if (isCoded) converString += stringCount;

            stringCount ++;
        } else if (node instanceof SimpleName) {
            if (DataInitializer.parseType == 1) {
                converString = ((SimpleName) node).getIdentifier();
            } else if (!DataInitializer.Names.contains(converString)) {
                converString += "SimpleName";
                if (isCoded) converString += nameCount;

                nameCount ++;
            }

            nameLiterals.put(converString, nameLiterals.getOrDefault(converString, 0) + 1);
        }

        return converString;
    }


    void generateCopyRule(ASTNode node, String originalLiteral) {
        String convertLiteral = getToken(node);
        int index = indexes.getOrDefault(convertLiteral, -1);
        if (index != -1) {
            addRule(new Rule(node).addChild(Rule.Copy));
            addRule(new Rule(Rule.Copy).addChild(index + ""));
        } else  {
            addRule(new Rule(node).addChild(convertLiteral));

            indexes.put(convertLiteral, addedRules.size()-1);
            nameMap.put(convertLiteral, originalLiteral);
        }
    }

    @Override
    public boolean visit(StringLiteral node) {
        String originalLiteral = node.getEscapedValue();
        generateCopyRule(node, originalLiteral);
        return false;
    }

    @Override
    public boolean visit(NumberLiteral node) {
        String originalLiteral = node.getToken();
        generateCopyRule(node, originalLiteral);
        return false;
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        String originalLiteral = node.getEscapedValue();
        generateCopyRule(node, originalLiteral);
        return false;
    }

    public boolean visit(SimpleName node) {
        String originalLiteral = node.getIdentifier();
        generateCopyRule(node, originalLiteral);
        return false;
    }
}
