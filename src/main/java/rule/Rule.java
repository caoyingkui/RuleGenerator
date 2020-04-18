package rule;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;

public class Rule {
    public static final String HEAD = "$";
    public static final String LEAF = "&";

    public static final String Start        = HEAD + "Start";

    public static final String Annotation   = getClassName(Annotation.class);
    public static final String Annotations  = Annotation + "s";

    public static final String Copy         = HEAD + "Copy";

    public static final String Declaration  = HEAD + "Declaration";

    public static final String Dimension    = getClassName(Dimension.class);
    public static final String Dimensions   = Dimension + "s";

    public static final String Expression   = getClassName(Expression.class);
    public static final String Expressions  = Expression + "s";

    public static final String Name         = HEAD + "Name";

    public static final String Statement    = getClassName(Statement.class);
    public static final String Statements   = Statement + "s";


    public static final String Type         = getClassName(Type.class);
    public static final String Types        = Type + "s";


    public static final String BodyDeclaration  = getClassName(BodyDeclaration.class);
    public static final String BodyDeclarations = BodyDeclaration + "s";


    public static final String CatchClauses     = getClassName(CatchClause.class) + "s";



    public static final String TypeParameter    = getClassName(TypeParameter.class);
    public static final String TypeParameters   = TypeParameter + "s";

    public static final String Modifier             = getClassName(Modifier.class);


    public static final String ReceiverParameter    = HEAD + "ReceiverParameter";

    public static final String VariableDeclaration  = getClassName(VariableDeclaration.class);
    public static final String VariableDeclarations  = getClassName(VariableDeclaration.class) + "s";


    public static final String EnumConstantDeclaration  = getClassName(EnumConstantDeclaration.class);
    public static final String EnumConstantDeclarations = EnumConstantDeclaration + "s";


    public static final String InfixOperator            = HEAD + "InfixOperator";


    public static final String SingleVariableDeclaration    = getClassName(SingleVariableDeclaration.class);
    public static final String SingleVariableDeclarations   = SingleVariableDeclaration + "s";

    public static final String VariableDeclarationFragment  = getClassName(VariableDeclarationFragment.class);
    public static final String VariableDeclarationFragments = VariableDeclarationFragment + "s";


    public String head;
    public ArrayList<String> children = new ArrayList<>();

    public String ruleString = "";

    public static String getSimpleName(SimpleName node) {
        return "SimpleName";
        //return node.getIdentifier();
    }

    public static String getCharLiteral(CharacterLiteral node) {
        return "CharacterLiteral";
        //return node.getEscapedValue();
    }

    public static String getNumberLiteral(NumberLiteral node) {
        return "NumberLiteral";
        //return node.getToken();
    }

    public static String getStringLiteral(StringLiteral node) {
        return "StringLiteral";
        //return node.getEscapedValue();
    }

    public static String getJavadoc(Javadoc node) {

        return "Javadoc";
        //return node.toString();
        //return "/**Javadoc*/";
    }

    public static String getTagName(TagElement node) {
        return "TagName";
    }

    public static String getTextElementString(TextElement node) {
        return LEAF + "TextElement";
    }

    public static String getClassName(Class clazz) {
        String className = clazz.toString();
        return HEAD + className.substring(className.lastIndexOf(".") + 1);
    }

    public static String getClassName(String clazz) {
        return HEAD + clazz;
    }

    public Rule() {
        head = "";
        children = new ArrayList<>();
    }

    public Rule(Class clazz) {
        head = getClassName(clazz);
    }

    public Rule(String head) {
        this.head = head;
    }

    public Rule(ASTNode node) {
        head = getClassName(node.getClass());
    }

    public Rule addChild(String child) {
        if (child.split(" ").length != 1) {
           new Exception("add rule.Rule function error: " + child ).printStackTrace();
        }
        children.add(child);
        return this;
    }


    public Rule addChild(String child, boolean condition) {
        if (condition) return addChild(child);
        else return this;
    }

    public Rule addChild(Class child, boolean condition) {
        if (condition) addChild(child);

        return this;
    }

    public Rule addChild(ASTNode child, boolean condition) {
        if (condition) addChild(child);
        return this;
    }

    public Rule addChild(Class child) {
        String className = getClassName(child);
        children.add(className);
        return this;
    }

    public Rule addChild(ASTNode node) {
        if (node != null) {
            String className = getClassName(node.getClass());
            children.add(className);
        }
        return this;
    }

    public Rule addChildren(boolean condition, String... childrenString) {
        if (condition) {
            for (String child: childrenString)
                addChild(child);
        }
        return this;
    }

    public Rule addChildren(String childrenString) {
        int count = 0;
        for (String child: childrenString.trim().split(" ")) {
            if (child.length() == 0) continue;
            children.add(child);
            count ++;
        }
        if (count < 2) {
            //new Exception("add rule.Rule function error!").printStackTrace();
        }
        return this;
    }

    public Rule addChildren(String childreString, boolean condition) {
        if (condition) addChildren(childreString);
        return this;
    }

    public Rule init(String str) {
        String[] parts = str.split(" ");
        if (parts.length < 3) {
            new Exception("Rule string is not valid").printStackTrace();
        } else {
            head = parts[0];
            // 过滤箭头
            for (int i = 2; i < parts.length; i++) {
                children.add(parts[i]);
            }
        }
        return this;
    }

    public String toString() {
        initStr();
        return ruleString;
    }

    public void initStr() {
        StringBuilder builder = new StringBuilder();
        builder.append(head).append(" ->");

        for (String child: children) {
            builder.append(" ").append(child);
        }

        ruleString = builder.toString();
    }

    public static String getExtendStr(Class clazz, String delimiter, int times) {
        String unit = getClassName(clazz);
        return getExtendStr(unit, delimiter, times);
    }

    public static String getExtendStr(String unit, String delimiter, int times) {
        if (times == 0) return "";

        unit = unit.trim();
        delimiter = delimiter.trim();

        StringBuilder str = new StringBuilder();
        str.append(unit);

        // 次数要少一次
        for (int i = 1; i < times; i++)
            str.append(" ").append(delimiter).append(" ").append(unit);

        return str.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Rule)) return false;

        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
