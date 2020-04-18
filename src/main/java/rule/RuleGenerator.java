package rule;

import dataProcess.DataInitializer;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RuleGenerator extends Generator{
    private Map<String, Integer>    indexes = new HashMap<>();
    public Map<String, String>     original2convertMap = new HashMap<>();//原始到转换字符的映射
    public Map<String, String>     convert2originalMap = new HashMap<>();//转换名称到原始的映射

    public  static Map<String, Integer>    nameLiterals = new HashMap<>();
    public  static Map<String, Integer>    numberLiterals = new HashMap<>();


    private int stringCount     = 0;
    private int numberCount     = 0;
    private int characterCount  = 0;
    private int simpleNameCount = 0;
    private int qualifiedNameCount = 0;
    private int qualifiedTypeCount = 0;

    public static boolean isCoded    = true;

    // 获取Token
    // 然后才能获取rule list
    // 然后再

    public void clear() {
        addedRules= new ArrayList<>();
        indexes = new HashMap<>();
    }


    /**
     * 获取token有三种情况
     * @param node
     * @return
     */
    public String getToken(ASTNode node, String originalToken) {
        String converString =  "";
        if (node instanceof CharacterLiteral) {
            converString += "CharacterLiteral";
            if (isCoded) converString += characterCount;

            characterCount ++;
        } else if (node instanceof NumberLiteral) {
            if (DataInitializer.parseType == 1) {
                converString = ((NumberLiteral) node).getToken();
            } else if (!DataInitializer.Numbers.contains(originalToken)) {
                converString = "NumberLiteral";
                if (isCoded) converString += numberCount;

                numberCount ++;
            } else {
                converString = originalToken;
            }

            numberLiterals.put(converString, numberLiterals.getOrDefault(converString, 0) + 1);
        } else if (node instanceof StringLiteral) {
            converString += "StringLiteral";
            if (isCoded) converString += stringCount;

            stringCount ++;
        } else if (node instanceof SimpleName) {
            if (DataInitializer.parseType == 1 || DataInitializer.Names.contains(originalToken)) {
                converString = originalToken;
            } else {
                converString = "SimpleName";
                if (isCoded) converString += simpleNameCount;

                simpleNameCount ++;
            }

            nameLiterals.put(converString, nameLiterals.getOrDefault(converString, 0) + 1);
        } else if (node instanceof QualifiedName) {
            if (DataInitializer.parseType == 1 || DataInitializer.Names.contains(originalToken)) {
                converString = originalToken;
            } else {
                converString = "QualifiedName";
                if (isCoded) converString += qualifiedNameCount;

                qualifiedNameCount++;
            }

            nameLiterals.put(converString, nameLiterals.getOrDefault(converString, 0) + 1);
        } else if (node instanceof QualifiedType) {
            if (DataInitializer.parseType == 1 || DataInitializer.Names.contains(originalToken)) {
                converString = originalToken;
            } else {
                converString = "QualifiedType";
                if (isCoded) converString += qualifiedTypeCount;

                qualifiedTypeCount++;
            }

            nameLiterals.put(converString, nameLiterals.getOrDefault(converString, 0) + 1);
        }

        if (converString.equals("SimpleName170")) {
            int a= 2;
        }
        return converString;
    }


    void generateCopyRule(ASTNode node, String originalLiteral) {
        String convertLiteral = original2convertMap.containsKey(originalLiteral) ?
                original2convertMap.get(originalLiteral) : getToken(node, originalLiteral);
        int index = indexes.getOrDefault(convertLiteral, -1);
        if (index != -1) {
            addRule(new Rule(node).addChild(Rule.Copy));
            addRule(new Rule(Rule.Copy).addChild(index + ""));
        } else  {
            addRule(new Rule(node).addChild(convertLiteral));

            indexes.put(convertLiteral, addedRules.size()-1);
            convert2originalMap.put(convertLiteral, originalLiteral);
            original2convertMap.put(originalLiteral, convertLiteral);
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

    public boolean visit(QualifiedType node) {
        String originalLiteral = node.toString();
        generateCopyRule(node, originalLiteral);
        return false;
    }

    public boolean visit(QualifiedName node) {
        String originalLiteral = node.toString();
        generateCopyRule(node, originalLiteral);
        return false;
    }

}
