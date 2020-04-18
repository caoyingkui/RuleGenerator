package rule;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class Generator extends ASTVisitor {

    //rules 出现的次数
    public static Map<Rule, Integer> rules = new HashMap<>();

    public ArrayList<Rule> addedRules = new ArrayList<Rule>();



    protected void addRule(Rule rule) {
        if (rule.children.size() == 1 && rule.head.equals(rule.children.get(0))) {
            System.out.println("error from Generator");
        }

        int count = rules.getOrDefault(rule, 0);
        count ++;
        rules.put(rule, count);
        addedRules.add(rule);
    }

    private String getModifiers(int modifiers) {
        StringBuilder str = new StringBuilder();

        if (Modifier.isPublic(modifiers)) str.append("public ");

        if (Modifier.isProtected(modifiers)) str.append("protected ");

        if (Modifier.isPrivate(modifiers)) str.append("private ");

        if (Modifier.isStatic(modifiers)) str.append("static ");

        if (Modifier.isAbstract(modifiers)) str.append("abstract ");

        if (Modifier.isFinal(modifiers)) str.append("final ");

        if (Modifier.isSynchronized(modifiers)) str.append("synchronized ");

        if (Modifier.isVolatile(modifiers)) str.append("volatile ");

        if (Modifier.isNative(modifiers)) str.append("native ");

        if (Modifier.isStrictfp(modifiers)) str.append("strictfp ");

        if (Modifier.isTransient(modifiers)) str.append("transient ");

        return str.toString().trim();
    }

    private String getModifiers(List<ASTNode> modifiers) {
        //ToString temp = new ToString();
        StringBuilder str = new StringBuilder();
        for (ASTNode node: modifiers) {
            str.append(node.toString()).append(" ");
        }

        return str.toString().trim();

//        List<Annotation> annotations = new ArrayList<>();
//
//        StringBuilder str = new StringBuilder();
//        Iterator<ASTNode> it = modifiers.iterator();
//        while (it.hasNext()) {
//            ASTNode next = it.next();
//            if (next instanceof NormalAnnotation
//                    || next instanceof MarkerAnnotation
//                    || next instanceof SingleMemberAnnotation) {
//                annotations.add((Annotation) next);
//            } else if (next instanceof Modifier) {
//                str.append(((Modifier) next).getKeyword().toString()).append(" ");
//            }
//        }
//        return new Pair<>(annotations.size() > 0 ? annotations : null, str.toString().trim());
    }

    private <T extends ASTNode> void extendBasicType(List<T> nodeList, String extendType, String unitType) {
        Iterator it = nodeList.iterator();
        while (it.hasNext()) {
            T cur = (T) it.next();
            if (it.hasNext()) {
                addRule(new Rule(extendType).addChild(unitType).addChild(extendType));
            } else {
                addRule(new Rule(extendType).addChild(unitType));
            }
            addRule(new Rule(unitType).addChild(cur));

            cur.accept(this);
        }
    }

    private <T extends ASTNode> void extendBasicType(List<T> nodeList, String extendType, String unitType, String delimiter) {
        Iterator it = nodeList.iterator();
        while (it.hasNext()) {
            T cur = (T) it.next();
            if (it.hasNext()) {
                addRule(new Rule(extendType).addChild(unitType).addChild(delimiter).addChild(extendType));
            } else {
                addRule(new Rule(extendType).addChild(unitType));
            }
            addRule(new Rule(unitType).addChild(cur));

            cur.accept(this);
        }
    }

    private Type getSuperclass(TypeDeclaration node) {
        return node.getSuperclassType();
    }

    private List<Type> getSuperInterface(TypeDeclaration node) {
        return node.superInterfaceTypes().isEmpty() ? null : node.superInterfaceTypes();
    }

    protected StringBuffer buffer = new StringBuffer();
    private int indent = 0;

    private <T extends ASTNode> void visitUnitList(List<T> units) {
        Iterator it = units.iterator();
        while (it.hasNext()) {
            T next = (T) it.next();
            next.accept(this);
        }
    }

    private <T extends ASTNode> void visitUnitList(List<T> units, String type) {
        Iterator it = units.iterator();
        while (it.hasNext()) {
            T next = (T) it.next();
            addRule(new Rule(type).addChild(next));
            next.accept(this);
        }
    }

    public boolean visit(AnnotationTypeDeclaration node) {
        String modifiersStr = getModifiers(node.modifiers());

        List<Annotation> annotations = node.bodyDeclarations().isEmpty() ? node.bodyDeclarations() : null;

        Rule rule = new Rule(node);
        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild("@").addChild("interface").addChild(Rule.Name)
                .addChild("{").addChild(Rule.Annotations, annotations != null).addChild("}");
        addRule(rule);
        // The end

        if (node.getJavadoc() != null) node.getJavadoc().accept(this);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));


        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);


        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.BodyDeclaration, " ", annotations.size())
            ));

            visitUnitList(annotations, Rule.Annotation);
        }

        return false;
    }

    public boolean visit(AnnotationTypeMemberDeclaration node) {
        String modifiersStr = getModifiers(node.modifiers());

        boolean hasDefault = node.getDefault() != null;

        Rule rule = new Rule(node);


        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Type)
                .addChild(Rule.Name).addChild("(").addChild(")")
                .addChild("default", hasDefault).addChild(Rule.Expression).addChild(";");
        addRule(rule);



        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);

        if (node.getDefault() != null) {
            addRule(new Rule(Rule.Expressions).addChild(node.getDefault()));
            node.getDefault().accept(this);
        }

        return false;
    }

    public boolean visit(AnonymousClassDeclaration node) {
        boolean hasDeclaration = !node.bodyDeclarations().isEmpty();

        Rule rule = new Rule(node);
        rule.addChild("{").addChild(Rule.BodyDeclarations, hasDeclaration).addChild("}");
        addRule(rule);

        if (hasDeclaration) {
            extendBasicType(node.bodyDeclarations(), Rule.BodyDeclarations, Rule.BodyDeclaration);
        }

        return false;
    }

    public boolean visit(ArrayAccess node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression).addChild("[").addChild(Rule.Expression).addChild("]");
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getArray()));
        node.getArray().accept(this);

        addRule(new Rule(Rule.Expression).addChild(node.getIndex()));
        node.getIndex().accept(this);
        return false;
    }

    public boolean visit(ArrayCreation node) {
        // new int[4][3][][];
        // [4][3] fromerDimension;
        // [][] latterDimension

        ArrayType type = node.getType();

        int formerDimensionSize = node.dimensions().size();
        int latterDimensionSize = type.dimensions().size() - formerDimensionSize;
        String formerDimension = formerDimensionSize == 0 ?
                "" : Rule.getExtendStr("[ " + Rule.Expression + " ]", " ", formerDimensionSize);
        String latterDimension = latterDimensionSize == 0 ?
                "" : Rule.getExtendStr("[]", " ", latterDimensionSize);

        Rule rule = new Rule(node);

        rule.addChild("new").addChild(Rule.Type)
                .addChildren(formerDimension, formerDimensionSize > 0)
                .addChildren(latterDimension, latterDimensionSize > 0)
                .addChild(ArrayInitializer.class, node.getInitializer() != null);

        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(type.getElementType()));
        type.getElementType().accept(this);

        if (formerDimensionSize > 0)
            visitUnitList(node.dimensions(), Rule.Expression);

        if (node.getInitializer() != null) {
            node.getInitializer().accept(this);
        }

        return false;
    }

    public boolean visit(ArrayInitializer node) {
        Rule rule = new Rule(node);
        rule.addChild("{").addChild(Rule.Expressions, node.expressions().size() > 0).addChild("}");
        addRule(rule);

        if (node.expressions().size() > 0) {
            extendBasicType(node.expressions(), Rule.Expressions, Rule.Expression, ",");
        }
        return false;
    }

    public boolean visit(ArrayType node) {
        int dimensionSize = 0, apiLevel = node.getAST().apiLevel();
        if (apiLevel < 8) {
            Type temp = node;
            while (temp instanceof ArrayType) {
                dimensionSize ++;
                temp = ((ArrayType)temp).getComponentType();
            }
        } else {
            dimensionSize = node.dimensions().size();
        }

        Rule rule = new Rule(node);
        rule.addChild(Rule.Type)
                .addChild(Rule.Dimensions, dimensionSize > 0);
        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(node.getElementType()));
        node.getElementType().accept(this);

        if (apiLevel < 8) {
            addRule(new Rule(Rule.Dimensions).addChildren(
                    Rule.getExtendStr("[]", " ", dimensionSize)
            ));
        } else {
            addRule(new Rule(Rule.Dimensions).addChildren(
                    Rule.getExtendStr(Rule.Dimension, " ", dimensionSize)
            ));
            visitUnitList(node.dimensions());
        }

        return false;
    }

    public boolean visit(AssertStatement node) {
        Rule rule = new Rule(node);

        rule.addChild("assert");
        rule.addChild(node.getExpression());
        if (node.getMessage() != null) {
            rule.addChild(":").addChild(node.getMessage());
        }
        rule.addChild(";");
        addRule(rule);


        node.getExpression().accept(this);

        if (node.getMessage() != null) {
            node.getMessage().accept(this);
        }

        return false;
    }

    public boolean visit(Assignment node) {
        Rule rule = new Rule(node);

        rule.addChild(node.getLeftHandSide());
        rule.addChild(node.getOperator().toString());
        rule.addChild(node.getRightHandSide());
        addRule(rule);

        node.getLeftHandSide().accept(this);
        this.buffer.append(node.getOperator().toString());
        node.getRightHandSide().accept(this);
        return false;
    }


    public boolean visit(Block node) {
        List<Statement> statements = node.statements().isEmpty() ?
                null : node.statements();

        Rule rule = new Rule(node);
        rule.addChild("{")
                .addChild(Rule.Statements, statements != null)
                .addChild("}");
        addRule(rule);

        if (statements != null)
            extendBasicType(statements, Rule.Statements, Rule.Statement);

        return false;
    }

    public boolean visit(BlockComment node) {
        Rule rule = new Rule(node);
        rule.addChild("/* */");
        addRule(rule);

        return false;
    }

    public boolean visit(BooleanLiteral node) {
        Rule rule = new Rule(node);

        if (node.booleanValue()) {
            rule.addChild("true");
        } else {
            rule.addChild("false");
        }
        addRule(rule);

        return false;
    }

    public boolean visit(BreakStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("break");

        if (node.getLabel() != null) {
            rule.addChild(node.getLabel());
        }

        rule.addChild(";");
        addRule(rule);

        if (node.getLabel() != null) {
            node.getLabel().accept(this);
        }

        return false;
    }

    public boolean visit(CastExpression node) {
        Rule rule = new Rule(node);
        rule.addChild("(");
        rule.addChild(node.getType());
        rule.addChild(")");
        rule.addChild(node.getExpression());
        addRule(rule);

        node.getType().accept(this);

        node.getExpression().accept(this);

        return false;
    }

    public boolean visit(CatchClause node) {
        Rule rule = new Rule(node);
        rule.addChild("catch").addChild("(").addChild(Rule.SingleVariableDeclaration).addChild(")")
                .addChild(node.getBody()); //
        addRule(rule);

        node.getException().accept(this);
        node.getBody().accept(this);
        return false;
    }

    public boolean visit(CharacterLiteral node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.getCharLiteral(node));
        addRule(rule);

        //this.buffer.append(node.getEscapedValue());
        return false;
    }

    public boolean visit(ClassInstanceCreation node) {
        int apiLevel = node.getAST().apiLevel();

        Expression qualifier = node.getExpression();

        List<Type> typeArguments = new ArrayList<>();
        if (apiLevel >= 3)
            typeArguments = node.typeArguments();

        List<Type> arguments = node.arguments();

        AnonymousClassDeclaration anonymousClassDeclaration = node.getAnonymousClassDeclaration();

        Rule rule = new Rule(node);
        rule.addChildren(qualifier != null, Rule.Expression, ".")
                .addChild("new")
                .addChildren(typeArguments.size() > 0, "<", Rule.Types, ">")
                .addChild(Rule.Type)
                .addChild("(").addChild(Rule.Expressions, arguments.size() > 0).addChild(")")
                .addChild(AnonymousClassDeclaration.class, anonymousClassDeclaration != null);
        addRule(rule);

        if (qualifier != null) {
            addRule(new Rule(Rule.Expression).addChild(qualifier));
            qualifier.accept(this);
        }

        if (typeArguments.size() > 0) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (arguments.size() > 0) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }

        if (anonymousClassDeclaration != null)
            anonymousClassDeclaration.accept(this);

        return false;
    }

    public boolean visit(CompilationUnit node) {
        Rule rule = new Rule(node);

        if (node.getAST().apiLevel() >= 9 && node.getModule() != null) {
            rule.addChild(node.getModule()); // 类型不是虚类
        }

        if (node.getPackage() != null) {
            rule.addChild(node.getPackage()); // 类型不是虚类
        }


        int importsSize = node.imports().size();
        if (importsSize > 0) {
            String importRule = ImportDeclaration.class.toString();
            importRule = importRule.substring(importRule.lastIndexOf("."));

            String importsStr = Rule.getExtendStr(importRule, " ", importsSize);
            rule.addChild(importsStr);
        }

        int declarationSize = node.types().size();
        if (declarationSize > 0) {
            // 非虚类
            String compileUnitStr = Rule.getExtendStr(Rule.Declaration, " ", declarationSize);
            rule.addChild(compileUnitStr);
        }

        addRule(rule);

        if (node.getAST().apiLevel() >= 9 && node.getModule() != null) {
            node.getModule().accept(this);
        }

        if (node.getPackage() != null) {
            node.getPackage().accept(this);
        }

        Iterator it = node.imports().iterator();

        while(it.hasNext()) {
            ImportDeclaration d = (ImportDeclaration)it.next();
            d.accept(this);
        }

        it = node.types().iterator();

        while(it.hasNext()) {
            AbstractTypeDeclaration d = (AbstractTypeDeclaration)it.next();
            addRule(new Rule(Rule.Declaration).addChild(d));

            d.accept(this);
        }

        return false;
    }

    public boolean visit(ConditionalExpression node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression);
        rule.addChild("?");
        rule.addChild(Rule.Expression);
        rule.addChild(":");
        rule.addChild(Rule.Expression);
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);
        //this.buffer.append(" ? ");

        addRule(new Rule(Rule.Expression).addChild(node.getThenExpression()));
        node.getThenExpression().accept(this);

        //this.buffer.append(" : ");
        addRule(new Rule(Rule.Expression).addChild(node.getElseExpression()));
        node.getElseExpression().accept(this);
        return false;
    }

    public boolean visit(ConstructorInvocation node) {
        List<Type> typeArguments = node.typeArguments().isEmpty() ?
                null : node.typeArguments();

        List<Expression> arguments = node.arguments().isEmpty() ?
                null : node.arguments();

        Rule rule = new Rule(node);
        rule.addChildren(typeArguments != null, "<", Rule.Types, ">")
                .addChild("this").addChild("(")
                .addChild(Rule.Expressions, arguments != null)
                .addChild(")").addChild(";");
        addRule(rule);


        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        if (arguments != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }
        return false;
    }

    public boolean visit(ContinueStatement node) {
        SimpleName label = node.getLabel();
        Rule rule = new Rule(node);
        rule.addChild("continue")
                .addChild(SimpleName.class, label != null)
                .addChild(";");
        addRule(rule);

        if (label != null) {
            label.accept(this);
        }

        return false;
    }

    public boolean visit(CreationReference node) {
        List<Type> typeArguments = node.typeArguments().isEmpty() ?
                null : node.typeArguments();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Type).addChild("::")
                .addChildren(typeArguments != null, "<", Rule.Types, ">")
                .addChild("new");
        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        return false;
    }

    public boolean visit(Dimension node) {

        List<Annotation> annotations = node.annotations().isEmpty() ?
                null : node.annotations();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Annotations, annotations != null).addChild("[]");
        addRule(rule);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }
        return false;
    }

    public boolean visit(DoStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("do")
                .addChild(node.getBody())
                .addChild("while")
                .addChild("(")
                .addChild(Rule.Expression)
                .addChild(")")
                .addChild(";");
        addRule(rule);

        node.getBody().accept(this);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);
        return false;
    }

    public boolean visit(EmptyStatement node) {
        Rule rule = new Rule(node);
        rule.addChild(";");
        addRule(rule);

        return false;
    }

    public boolean visit(EnhancedForStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("for")
                .addChild("(")
                .addChild(node.getParameter())
                .addChild(":")
                .addChild(Rule.Expression);
        rule.addChild(")");

        rule.addChild(node.getBody()); // block 是 statement的实例子类
        addRule(rule);

        node.getParameter().accept(this);


        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);


        node.getBody().accept(this);
        return false;
    }

    public boolean visit(EnumConstantDeclaration node) {
        String modifiersStr = getModifiers(node.modifiers());

        List<Expression> arguments = node.arguments().isEmpty() ? node.arguments() : null;
        AnonymousClassDeclaration anonymousClassDeclaration = node.getAnonymousClassDeclaration();


        Rule rule = new Rule(node);
        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Name)
                .addChildren(arguments != null, "(", Rule.Expressions, ")")
                .addChild(AnonymousClassDeclaration.class, anonymousClassDeclaration != null);
        addRule(rule);



        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);


        if (arguments != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }

        if (anonymousClassDeclaration != null) {
            anonymousClassDeclaration.accept(this);
        }

        return false;
    }

    public boolean visit(EnumDeclaration node) {

        int apiLevel = node.getAST().apiLevel();
        String modifiersStr  = apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        String implementStr = Rule.getExtendStr(Rule.Type, ",", node.superInterfaceTypes().size());

        int enumConstantSize = node.enumConstants().size();


        List<BodyDeclaration> declarations = node.bodyDeclarations();

        Rule rule = new Rule(node);

        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChildren(implementStr.length() > 0, "implements", Rule.Types)
                .addChild("{")
                .addChild(Rule.EnumConstantDeclarations, enumConstantSize > 0)
                .addChild(Rule.BodyDeclarations, declarations.size() > 0)
                .addChild("}");

        addRule(rule);

        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);

        if (node.superInterfaceTypes().size() > 0) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", node.superInterfaceTypes().size())
            ));
            visitUnitList(node.superInterfaceTypes(), Rule.Type);
        }

        if (enumConstantSize > 0) {
            extendBasicType(node.enumConstants(), Rule.EnumConstantDeclarations, Rule.EnumConstantDeclaration, ",");
        }

        if (declarations.size() > 0) {
            extendBasicType(node.bodyDeclarations(), Rule.BodyDeclarations, Rule.BodyDeclaration);
        }

        return false;
    }

    public boolean visit(ExpressionMethodReference node) {

        int typeArgumentsSize = node.typeArguments().size();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression)
                .addChild("::")
                .addChildren(typeArgumentsSize > 0, "<", Rule.Types, ">")
                .addChild(SimpleName.class);
        addRule(rule);


        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);

        if (typeArgumentsSize > 0) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArgumentsSize)
            ));
            visitUnitList(node.typeArguments(), Rule.Type);
        }

        node.getName().accept(this);
        return false;
    }

    public boolean visit(ExpressionStatement node) {
        Rule rule = new Rule(node);
        rule.addChild(node.getExpression());
        rule.addChild(";");
        addRule(rule);

        node.getExpression().accept(this);

        return false;
    }

    public boolean visit(FieldAccess node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression)
                .addChild(".")
                .addChild(node.getName());
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);

        node.getName().accept(this);
        return false;
    }

    public boolean visit(FieldDeclaration node) {
        String modifiersStr = node.getAST().apiLevel() == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        List<VariableDeclarationFragment> fragments = node.fragments().isEmpty() ?
                null : node.fragments();

        Rule rule = new Rule(node);
        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Type)
                .addChild(Rule.VariableDeclarationFragments)
                .addChild(";");
        addRule(rule);


        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (fragments != null) {
            addRule(new Rule(Rule.VariableDeclarationFragments).addChildren(
                    Rule.getExtendStr(Rule.VariableDeclarationFragment, ",", fragments.size())
            ));

            visitUnitList(fragments);
        }

        return false;
    }

    public boolean visit(ForStatement node) {
        List<Expression> forInit = node.initializers().isEmpty() ?
                null : node.initializers();

        Expression expression = node.getExpression();

        List<Expression> forUpdate = node.updaters().isEmpty() ?
                null : node.updaters();

        Rule rule = new Rule(node);
        rule.addChild("for").addChild("(")
                .addChild(Rule.Expressions, forInit != null).addChild(";")
                .addChild(Rule.Expression , expression != null).addChild(";")
                .addChild(Rule.Expressions, forUpdate != null).addChild(")")
                .addChild(Rule.Statement);
        addRule(rule);

        if (forInit != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", forInit.size())
            ));
            visitUnitList(forInit, Rule.Expression);
        }

        if (expression != null) {
            addRule(new Rule(Rule.Expression).addChild(expression));
            expression.accept(this);
        }

        if (forUpdate != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", forUpdate.size())
            ));
            visitUnitList(forUpdate, Rule.Expression);
        }

        addRule(new Rule(Rule.Statement).addChild(node.getBody()));
        node.getBody().accept(this);
        return false;
    }

    public boolean visit(IfStatement node) {
        Expression condition = node.getExpression();

        Statement thenStatement = node.getThenStatement();

        Statement elseStatement = node.getElseStatement();

        Rule rule = new Rule(node);
        rule.addChild("if").addChild("(").addChild(Rule.Expression).addChild(")")
                .addChild(Rule.Statement)
                .addChildren(elseStatement != null, "else", Rule.Statement);
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(condition));
        condition.accept(this);

        addRule(new Rule(Rule.Statement).addChild(thenStatement));
        thenStatement.accept(this);

        if (elseStatement != null) {
            addRule(new Rule(Rule.Statement).addChild(elseStatement));
            elseStatement.accept(this);
        }

        return false;
    }

    public boolean visit(ImportDeclaration node) {
        Rule rule = new Rule(node);
        rule.addChild("import");
        if (node.getAST().apiLevel() >= 3 && node.isStatic()) rule.addChild("static");

        rule.addChild(Rule.Name);

        if (node.isStatic()) rule.addChild(".*");

        rule.addChild(";");
        addRule(rule);

        node.getName().accept(this);
        return false;
    }

    public boolean visit(InfixExpression node) {

        // 2 一个是左操作数，另一个是右操作数
        int expressionSize = 2 + node.extendedOperands().size();
        addRule(new Rule(node).addChildren(
                Rule.getExtendStr(Rule.Expression, node.getOperator().toString(), expressionSize)
        ));

        addRule(new Rule(Rule.Expression).addChild(node.getLeftOperand()));
        node.getLeftOperand().accept(this);

        addRule(new Rule(Rule.Expression).addChild(node.getRightOperand()));
        node.getRightOperand().accept(this);

        if (node.extendedOperands().size() != 0) {
            visitUnitList(node.extendedOperands(), Rule.Expression);
        }
        return false;
    }

    public boolean visit(Initializer node) {

        String modifiersStr = node.getAST().apiLevel() == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        Rule rule = new Rule(node);
        rule.addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(node.getBody());
        addRule(rule);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        node.getBody().accept(this);
        return false;
    }

    public boolean visit(InstanceofExpression node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression);
        rule.addChild("instanceof");
        rule.addChild(Rule.Type);
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getLeftOperand()));
        node.getLeftOperand().accept(this);

        //this.buffer.append(" instanceof ");

        addRule(new Rule(Rule.Type).addChild(node.getRightOperand()));
        node.getRightOperand().accept(this);
        return false;
    }

    public boolean visit(IntersectionType node) {
        // IntersectionType -> Type & Type { & Type }

        addRule(new Rule(node).addChildren(
                Rule.getExtendStr(Rule.Type, "&", node.types().size())
        ));

        visitUnitList(node.types(), Rule.Type);

        return false;
    }

    public boolean visit(Javadoc node) {

        addRule(new Rule(node).addChild(
                Rule.getJavadoc(node)
        ));
        return false;
    }

    public boolean visit(LabeledStatement node) {
        Rule rule = new Rule(node);
        rule.addChild(node.getLabel()).addChild(":").addChild(node.getBody());
        addRule(rule);

        node.getLabel().accept(this);

        node.getBody().accept(this);
        return false;
    }

    public boolean visit(LambdaExpression node) {
        boolean hasParentheses = node.hasParentheses();
        List<VariableDeclaration> variables = node.parameters().isEmpty() ?
                null : node.parameters();

        Rule rule = new Rule(node);
        rule.addChild("(", hasParentheses)
                .addChild(Rule.VariableDeclarations, variables != null)
                .addChild(")", hasParentheses)
                .addChild("->")
                .addChild(ASTNode.class);
        addRule(rule);

        if (variables != null) {
            addRule(new Rule(Rule.VariableDeclarations).addChildren(
                    Rule.getExtendStr(Rule.VariableDeclaration, ",", variables.size())
            ));
            visitUnitList(variables, Rule.VariableDeclaration);
        }

        addRule(new Rule(ASTNode.class).addChild(node.getBody()));
        node.getBody().accept(this);
        return false;
    }

    public boolean visit(LineComment node) {
        Rule rule = new Rule(node);
        rule.addChild("LineComment");
        addRule(rule);

        return false;
    }

    public boolean visit(MarkerAnnotation node) {
        System.out.println("不想遍历MarkerAnnotation节点，又该条提示消息，说明有问题！");
        Rule rule = new Rule(node);
        rule.addChild("@").addChild(Rule.Name);
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getTypeName()));
        node.getTypeName().accept(this);
        return false;
    }

    public boolean visit(MemberRef node) {
        Name qualifier = node.getQualifier();
        addRule(new Rule(node)
                .addChild(Rule.Name, qualifier != null).addChild("#").addChild(SimpleName.class)
        );


        if (qualifier != null) {
            addRule(new Rule(Rule.Name).addChild(qualifier));
            qualifier.accept(this);
        }

        node.getName().accept(this);
        return false;
    }

    public boolean visit(MemberValuePair node) {
        Rule rule = new Rule(node);
        rule.addChild(node.getName())
                .addChild("=")
                .addChild(Rule.Expression);
        addRule(rule);

        node.getName().accept(this);

        addRule(new Rule(Rule.Expression).addChild(node.getValue()));
        node.getValue().accept(this);
        return false;
    }

    public boolean visit(MethodDeclaration node) {
        int apiLevel = node.getAST().apiLevel();

        String modifiersStr =  apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        int typeParameterSize = apiLevel >= 3 ? node.typeParameters().size() : 0;


        Type returnType = node.isConstructor() ? null :
                (apiLevel == 2 ? node.getReturnType() : node.getReturnType2());


        boolean hasReceiverParameter = apiLevel >= 8 && node.getReceiverType() != null;

        // arguments
        List arguments = node.parameters().isEmpty() ? null : node.parameters();

        List<Dimension> dimensions = node.extraDimensions().isEmpty() ? null : node.extraDimensions();

        List<Type> exceptionTypes = node.thrownExceptionTypes().isEmpty() ? null : node.thrownExceptionTypes();


        Rule rule = new Rule(node);
        rule.addChild(Javadoc.class, node.getJavadoc() != null)
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChildren(typeParameterSize > 0, "<", Rule.TypeParameters, ">")
                .addChild(Rule.Type, returnType != null).addChild("void", !node.isConstructor() && returnType == null)
                .addChild(SimpleName.class).addChild("(")
                .addChild(Rule.ReceiverParameter, hasReceiverParameter).addChild(",", hasReceiverParameter && arguments != null)
                .addChild(Rule.Expressions, arguments != null)
                .addChild(")")
                .addChild(Rule.Dimensions, dimensions != null)
                .addChildren(exceptionTypes != null, "throws", Rule.Types)
                .addChild(node.getBody(), node.getBody()!=null)
                .addChild(";", node.getBody() == null);
        addRule(rule);

        if (rule.children.get(0).equals("->")) {
            int a = 2;
        }

        if (node.getJavadoc() != null) node.getJavadoc().accept(this);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        if (typeParameterSize > 0) {
            addRule(new Rule(Rule.TypeParameters).addChildren(
                    Rule.getExtendStr(Rule.TypeParameter, ",", typeParameterSize)
            ));

            visitUnitList(node.typeParameters());
        }

        if (returnType != null) {
            addRule(new Rule(Rule.Type).addChild(returnType));
            returnType.accept(this);
        }

        node.getName().accept(this);

        Type receiverType = node.getReceiverType();
        if (hasReceiverParameter) {
            SimpleName qualifier = node.getReceiverQualifier();

            Rule receiverRule = new Rule(Rule.ReceiverParameter);
            receiverRule.addChild(Rule.Type)
                    .addChild(qualifier, qualifier != null).addChild(".", qualifier != null)
                    .addChild("this");
            addRule(receiverRule);

            addRule(new Rule(Rule.Type).addChild(receiverType));
            receiverType.accept(this);

            if (qualifier != null) {
                qualifier.accept(this);
            }
        }

        if (arguments != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }

        if (dimensions != null) {
            addRule(new Rule(Rule.Dimensions).addChildren(
                    Rule.getExtendStr(Rule.Dimension, " ", dimensions.size())
            ));
            visitUnitList(dimensions);
        }

        if (exceptionTypes != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", exceptionTypes.size())
            ));
            visitUnitList(exceptionTypes, Rule.Type);
        }

        if (node.getBody() != null) node.getBody().accept(this);
        return false;
    }

    public boolean visit(MethodInvocation node) {
        Expression expression = node.getExpression();

        List<Type> typeArguments = node.getAST().apiLevel() >= 3 && !node.typeArguments().isEmpty()?
                node.typeArguments() : null ;

        List<Expression> arguments = !node.arguments().isEmpty() ? node.arguments() : null;

        Rule rule = new Rule(node);
        rule.addChildren(expression != null, Rule.Expression, ".")
                .addChildren(typeArguments != null , "<", Rule.Types, ">")
                .addChild(SimpleName.class).addChild("(")
                .addChild(Rule.Expressions, arguments != null)
                .addChild(")");
        addRule(rule);


        if (expression != null) {
            addRule(new Rule(Rule.Expression).addChild(expression));
            expression.accept(this);
        }

        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        node.getName().accept(this);

        if (arguments != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }

        return false;
    }

    public boolean visit(MethodRef node) {
        Rule rule = new Rule(node);

        if (node.getQualifier() != null) rule.addChild(Rule.Name);

        rule.addChild("#");
        rule.addChild(node.getName());
        rule.addChild("(");
        rule.addChildren(Rule.getExtendStr(MethodRefParameter.class, ",", node.parameters().size()));
        rule.addChild(")");
        addRule(rule);

        if (node.getQualifier() != null) {
            node.getQualifier().accept(this);
        }

        node.getName().accept(this);

        Iterator it = node.parameters().iterator();

        while(it.hasNext()) {
            MethodRefParameter e = (MethodRefParameter)it.next();
            e.accept(this);
        }

        return false;
    }

    public boolean visit(MethodRefParameter node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Type);
        if (node.getAST().apiLevel() >= 3 && node.isVarargs()) rule.addChild("...");

        if (node.getName() != null) rule.addChild(node.getName());

        addRule(rule);

        node.getType().accept(this);

        if (node.getName() != null) {
            node.getName().accept(this);
        }

        return false;
    }

    public boolean visit(Modifier node) {
        Rule rule = new Rule(Rule.Modifier);
        rule.addChild(node.getKeyword().toString());
        addRule(rule);

        return false;
    }

    public boolean visit(ModuleModifier node) {
        Rule rule = new Rule(node);
        rule.addChild(node.getKeyword().toString());
        addRule(rule);

        return false;
    }

    public boolean visit(NameQualifiedType node) {
        List<Annotation> annotations = node.annotations().isEmpty() ? null : node.annotations();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Name).addChild(".")
                .addChild(Rule.Annotations, annotations != null)
                .addChild(SimpleName.class);
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getQualifier()));
        node.getQualifier().accept(this);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));

            visitUnitList(annotations, Rule.Annotation);
        }

        node.getName().accept(this);
        return false;
    }

    public boolean visit(NormalAnnotation node) {
        List<MemberValuePair> pairs = node.values().isEmpty() ? null : node.values();
        String pairStr = pairs == null ?
                "" : Rule.getExtendStr(MemberValuePair.class, ",", node.values().size());

        Rule rule = new Rule(node);
        rule.addChild("@").addChild(Rule.Name)
                .addChild("(")
                .addChildren(pairStr, pairs != null)
                .addChild(")");
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getTypeName()));
        node.getTypeName().accept(this);

        if (pairs != null)
            visitUnitList(pairs);
        return false;
    }

    public boolean visit(NullLiteral node) {
        addRule(new Rule(node).addChild("null"));
        return false;
    }

    public boolean visit(NumberLiteral node) {
        addRule(new Rule(node).addChild(Rule.getNumberLiteral(node)));
        return false;
    }


    public boolean visit(PackageDeclaration node) {
        Rule rule = new Rule(node);
        if (node.getAST().apiLevel() >= 3) {
            if (node.getJavadoc() != null) rule.addChild(node.getJavadoc());

            if (node.annotations().size() > 0) rule.addChild(Rule.Annotations);

        }

        rule.addChild("package");
        rule.addChild(Rule.Name);
        addRule(rule);

        if (node.getAST().apiLevel() >= 3) {
            if (node.getJavadoc() != null) {
                node.getJavadoc().accept(this);
            }

            int size = node.annotations().size();
            if (size > 0)
                addRule(new Rule(Rule.Annotations)
                        .addChildren(Rule.getExtendStr(Rule.Annotation, " ", size)));

            Iterator it = node.annotations().iterator();

            while(it.hasNext()) {
                Annotation p = (Annotation)it.next();
                p.accept(this);
                //this.buffer.append(" ");
            }
        }

        node.getName().accept(this);
        return false;
    }

    public boolean visit(ParameterizedType node) {
        int typeSize = node.typeArguments().size();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Type)
                .addChild("<")
                .addChild(Rule.Types, typeSize > 0)
                .addChild(">");
        addRule(rule);


        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (typeSize > 0) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeSize)
            ));
            visitUnitList(node.typeArguments(), Rule.Type);
        }

        return false;
    }

    public boolean visit(ParenthesizedExpression node) {
        Rule rule = new Rule(node);
        rule.addChild("(").addChild(Rule.Expression).addChild(")");
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);
        return false;
    }

    public boolean visit(PostfixExpression node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Expression).addChild(node.getOperator().toString());
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getOperand()));
        node.getOperand().accept(this);

        return false;
    }

    public boolean visit(PrefixExpression node) {
        Rule rule = new Rule(node);
        rule.addChild(node.getOperator().toString()).addChild(Rule.Expression);
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getOperand()));
        node.getOperand().accept(this);
        return false;
    }

    public boolean visit(PrimitiveType node) {
        List<Annotation> annotations = node.annotations().isEmpty() ? null : node.annotations();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Annotations, annotations != null)
                .addChild(node.getPrimitiveTypeCode().toString());
        addRule(rule);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }

        return false;
    }

    public boolean visit(QualifiedName node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Name).addChild(".").addChild(node.getName());
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getQualifier()));
        node.getQualifier().accept(this);

        node.getName().accept(this);
        return false;
    }

    public boolean visit(QualifiedType node) {
        Type qualifier = node.getQualifier();

        List<Annotation> annotations = node.annotations().isEmpty() ? null : node.annotations();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Type).addChild(".")
                .addChild(Rule.Annotations, annotations != null)
                .addChild(SimpleName.class);
        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(qualifier));
        qualifier.accept(this);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }

        node.getName().accept(this);

        return false;
    }

    public boolean visit(ReturnStatement node) {
        Expression expression = node.getExpression();

        Rule rule = new Rule(node);
        rule.addChild("return")
                .addChild(Rule.Expression, expression != null)
                .addChild(";");
        addRule(rule);

        if (expression != null) {
            addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
            expression.accept(this);
        }

        return false;
    }

    public boolean visit(SimpleName node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.getSimpleName(node));
        addRule(rule);

        return false;
    }

    public boolean visit(SimpleType node) {
        List<Annotation> annotations = node.annotations().isEmpty() ?
                null : node.annotations();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Annotations, annotations != null)
                .addChild(Rule.Name);
        addRule(rule);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);

        return false;
    }

    public boolean visit(SingleMemberAnnotation node) {
        Rule rule = new Rule(node);
        rule.addChild("@");
        rule.addChild(Rule.Name);
        rule.addChild("(").addChild(Rule.Expression).addChild(")");
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getTypeName()));
        node.getTypeName().accept(this);

        addRule(new Rule(Rule.Expression).addChild(node.getValue()));
        node.getValue().accept(this);
        return false;
    }

    public boolean visit(SingleVariableDeclaration node) {
        int apiLevel = node.getAST().apiLevel();

        String modifiersStr = apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        boolean isVarargs = apiLevel >= 3 && node.isVarargs();
        List<Annotation> annotations = isVarargs && apiLevel >= 8 && node.varargsAnnotations().size() > 0 ?
                node.varargsAnnotations() : null;

        List<Dimension> dimensions = node.getExtraDimensions() > 0 ?
                node.extraDimensions() : null;

        Expression initlizer = node.getInitializer();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Type)
                .addChild(Rule.Annotations, annotations != null)
                .addChild("...", isVarargs)
                .addChild(SimpleName.class)
                .addChild(Rule.Dimensions, dimensions != null)
                .addChildren(initlizer != null, "=", Rule.Expression);
        addRule(rule);


        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }

        node.getName().accept(this);

        if (dimensions != null) {
            addRule(new Rule(Rule.Dimensions).addChildren(
                    Rule.getExtendStr(Rule.Dimension, " ", dimensions.size())
            ));
            visitUnitList(dimensions);
        }

        if (initlizer != null) initlizer.accept(this);

        return false;
    }

    public boolean visit(StringLiteral node) {
        Rule rule = new Rule(node);
        rule.addChildren(Rule.getStringLiteral(node));
        addRule(rule);

        //this.buffer.append(node.getEscapedValue());
        return false;
    }

    public boolean visit(SuperConstructorInvocation node) {
        Expression expression = node.getExpression();

        List<Type> typeArguments = node.typeArguments().isEmpty() ? null : node.typeArguments();

        List<Expression> arguments = node.arguments().isEmpty() ? null : node.arguments();

        Rule rule = new Rule(node);
        rule.addChildren(expression != null, Rule.Expression, ".")
                .addChildren(typeArguments != null, "<", Rule.Types, ">")
                .addChild("super").addChild("(")
                .addChild(Rule.Expressions, arguments != null).addChild(")").addChild(";");
        addRule(rule);

        if (expression != null) {
            addRule(new Rule(Rule.Expression).addChild(expression));
            expression.accept(this);
        }

        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        if (arguments != null) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", arguments.size())
            ));
            visitUnitList(arguments, Rule.Expression);
        }

        return false;
    }

    public boolean visit(SuperFieldAccess node) {
        Name qualifier = node.getQualifier();

        Rule rule = new Rule(node);
        rule.addChildren(qualifier != null, Rule.Name, ".")
                .addChild("super").addChild(".").addChild(Rule.Name);
        addRule(rule);

        if (qualifier != null) {
            addRule(new Rule(Rule.Name).addChild(qualifier));
            qualifier.accept(this);
        }

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);
        return false;
    }

    public boolean visit(SuperMethodInvocation node) {
        Rule rule = new Rule(node);
        if (node.getQualifier() != null) rule.addChild(Rule.Name).addChild(".");

        rule.addChild("super").addChild(".");

        if (node.getAST().apiLevel() >= 3 && !node.typeArguments().isEmpty())
            rule.addChild("<").addChild(Rule.Types).addChild(">");

        rule.addChild(Rule.Name).addChild("(");

        if (node.arguments().size() > 0)
            rule.addChild(Rule.Expressions);

        rule.addChild(")");

        addRule(rule);


        if (node.getQualifier() != null) {
            addRule(new Rule(Rule.Name).addChild(node.getQualifier()));
            node.getQualifier().accept(this);
        }

        Iterator it;
        if (node.getAST().apiLevel() >= 3 && !node.typeArguments().isEmpty()) {
            //this.buffer.append("<");
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", node.typeArguments().size())
            ));

            it = node.typeArguments().iterator();

            while(it.hasNext()) {
                Type t = (Type)it.next();
                addRule(new Rule(Rule.Type).addChild(t));
                t.accept(this);
            }
        }

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);

        //this.buffer.append("(");
        it = node.arguments().iterator();
        if (it.hasNext()) {
            addRule(new Rule(Rule.Expressions).addChildren(
                    Rule.getExtendStr(Rule.Expression, ",", node.arguments().size())
            ));
        }

        while(it.hasNext()) {
            Expression e = (Expression)it.next();
            addRule(new Rule(Rule.Expression).addChild(e));
            e.accept(this);
        }
        return false;
    }

    public boolean visit(SuperMethodReference node) {
        Name qualifier = node.getQualifier();
        List<Type> typeArguments = node.typeArguments().isEmpty() ?
                null : node.typeArguments();

        Rule rule = new Rule(node);
        rule.addChildren(qualifier != null, Rule.Name, ".")
                .addChild("super").addChild("::")
                .addChildren(typeArguments != null, "<", Rule.Types, ">")
                .addChild(SimpleName.class);
        addRule(rule);

        if (qualifier != null) {
            addRule(new Rule(Rule.Name).addChild(qualifier));
            qualifier.accept(this);
        }

        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        node.getName().accept(this);

        return false;
    }

    public boolean visit(SwitchCase node) {
        Rule rule = new Rule(node);
        if (node.isDefault()) {
            rule.addChild("default").addChild(":");
        } else {
            rule.addChild("case").addChild(Rule.Expression).addChild(":");
        }
        addRule(rule);


        if (!node.isDefault()) {
            addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
            node.getExpression().accept(this);
        }

        return false;
    }

    public boolean visit(SwitchStatement node) {
        List<Statement> statements = node.statements().isEmpty() ?
                null : node.statements();

        Rule rule = new Rule(node);
        rule.addChild("switch").addChild("(").addChild(Rule.Expression).addChild(")")
                .addChild("{")
                .addChild(Rule.Statements, statements != null)
                .addChild("}");
        addRule(rule);


        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);


        if (statements != null)
            extendBasicType(statements, Rule.Statements, Rule.Statement);
        return false;
    }

    public boolean visit(SynchronizedStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("synchronized")
                .addChild("(").addChild(Rule.Expression).addChild(")")
                .addChild(node.getBody());
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);

        node.getBody().accept(this);
        return false;
    }

    public boolean visit(TagElement node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.getTagName(node));
        addRule(rule);

        return false;
    }

    public boolean visit(TextElement node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.getTextElementString(node));
        addRule(rule);

        return false;
    }

    public boolean visit(ThisExpression node) {
        Rule rule = new Rule(node);
        if (node.getQualifier() != null) rule.addChild(Rule.Name).addChild(".");

        rule.addChild("this");
        addRule(rule);

        if (node.getQualifier() != null) {
            addRule(new Rule(Rule.Name).addChild(node.getQualifier()));
            node.getQualifier().accept(this);
        }

        return false;
    }

    public boolean visit(ThrowStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("throw").addChild(Rule.Expression).addChild(";");
        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);

        return false;
    }

    public boolean visit(TryStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("try");
        if (node.getAST().apiLevel() >= 4 && !node.resources().isEmpty()) {
            rule.addChild("(").addChild(Rule.Expressions).addChild(")");
        }
        rule.addChild(node.getBody());

        if (!node.catchClauses().isEmpty())
            rule.addChild(Rule.CatchClauses);

        if (node.getFinally() != null) {
            rule.addChild("finally");
            rule.addChild(node.getFinally());
        }
        addRule(rule);

        if (node.getAST().apiLevel() >= 4) {
            List resources = node.resources();
            if (!resources.isEmpty()) {
                String resourcesStr = Rule.getExtendStr(Rule.Expression, ";", resources.size());
                addRule(new Rule(Rule.Expressions).addChildren(resourcesStr));

                Iterator it = resources.iterator();

                while(it.hasNext()) {
                    Expression variable = (Expression)it.next();
                    addRule(new Rule(Rule.Expression).addChild(variable));
                    variable.accept(this);
                }
            }
        }

        node.getBody().accept(this);

        Iterator it = node.catchClauses().iterator();
        if (it.hasNext()) {
            String catchClausesStr = Rule.getExtendStr(CatchClause.class, " ", node.catchClauses().size());
            addRule(new Rule(Rule.CatchClauses).addChildren(catchClausesStr));
        }

        while(it.hasNext()) {
            CatchClause cc = (CatchClause)it.next();
            cc.accept(this);
        }

        if (node.getFinally() != null) {
            node.getFinally().accept(this);
        }

        return false;
    }

    public boolean visit(TypeDeclaration node) {
        int apiLevel = node.getAST().apiLevel();
        String modifiersStr = apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());


        List<TypeParameter> typeParameters = apiLevel >= 3 && node.typeParameters().size() > 0 ?
                node.typeParameters() : null;

        Type superclassType = getSuperclass(node);

        List<Type> superInterfaces = getSuperInterface(node);

        List<BodyDeclaration> bodyDeclarations = node.bodyDeclarations().isEmpty() ?
                null : node.bodyDeclarations();

        Rule rule = new Rule(node);
        rule.addChild(node.getJavadoc())
                .addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild("class", !node.isInterface()).addChild("interface", node.isInterface())
                .addChild(SimpleName.class)
                .addChildren(typeParameters != null, "<", Rule.TypeParameters, ">")
                .addChildren(superclassType != null, "extends", Rule.Type)
                .addChildren(!node.isInterface() && superInterfaces != null, "implements", Rule.Types)
                .addChildren(node.isInterface() && superInterfaces != null, "extends", Rule.Type)
                .addChild("{").addChild(Rule.BodyDeclarations, bodyDeclarations != null).addChild("}");
        addRule(rule);

        if (node.getJavadoc() != null) {
            node.getJavadoc().accept(this);
        }

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        node.getName().accept(this);

        if (typeParameters != null) {
            addRule(new Rule(Rule.TypeParameters).addChildren(
                    Rule.getExtendStr(Rule.TypeParameter, ",", typeParameters.size())
            ));
            visitUnitList(typeParameters);
        }

        if (superclassType != null) {
            addRule(new Rule(Rule.Type).addChild(superclassType));
            superclassType.accept(this);
        }

        if (superInterfaces != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", superInterfaces.size())
            ));
            visitUnitList(superInterfaces, Rule.Type);
        }

        if (bodyDeclarations != null) {
            extendBasicType(bodyDeclarations, Rule.BodyDeclarations, Rule.BodyDeclaration);
        }
        return false;
    }

    public boolean visit(TypeDeclarationStatement node) {
        if (node.getAST().apiLevel() == 2) {
            addRule(new Rule(node).addChild(node.getTypeDeclaration()));
            node.getTypeDeclaration().accept(this);
        }

        if (node.getAST().apiLevel() >= 3) {
            addRule(new Rule(node).addChild(node.getDeclaration()));
            node.getDeclaration().accept(this);
        }

        return false;
    }

    public boolean visit(TypeLiteral node) {
        Rule rule = new Rule(node);
        rule.addChild(Rule.Type).addChild(".").addChild("class");
        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);
        //this.buffer.append(".class");
        return false;
    }

    public boolean visit(TypeMethodReference node) {
        List<Type> typeArguments = node.typeArguments().isEmpty() ?
                null : node.typeArguments();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Type).addChild("::")
                .addChildren(typeArguments != null, "<", Rule.Types, ">")
                .addChild(SimpleName.class);
        addRule(rule);

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (typeArguments != null) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, ",", typeArguments.size())
            ));
            visitUnitList(typeArguments, Rule.Type);
        }

        node.getName().accept(this);
        return false;
    }

    public boolean visit(TypeParameter node) {
        String modifiersStr = node.getAST().apiLevel() >= 8 ?
            getModifiers(node.modifiers()) : "";

        int typeSize = node.typeBounds().size();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(SimpleName.class)
                .addChildren(typeSize > 0, "extends", Rule.Types);
        addRule(rule);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));


        node.getName().accept(this);

        if (typeSize > 0) {
            addRule(new Rule(Rule.Types).addChildren(
                    Rule.getExtendStr(Rule.Type, "&", typeSize)
            ));
            visitUnitList(node.typeBounds(), Rule.Type);
        }

        return false;
    }

    public boolean visit(UnionType node) {
        Rule rule = new Rule(node);
        rule.addChildren(Rule.getExtendStr(Rule.Type, "|", node.types().size()));
        addRule(rule);

        visitUnitList(node.types(), Rule.Type);

        return false;
    }

    public boolean visit(VariableDeclarationExpression node) {
        int apiLevel = node.getAST().apiLevel();

        String modifiersStr = apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        List<VariableDeclarationFragment> fragments = node.fragments().isEmpty() ?
                null : node.fragments();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Type)
                .addChild(Rule.VariableDeclarationFragments);
        addRule(rule);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));

        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (fragments != null) {
            addRule(new Rule(Rule.VariableDeclarationFragments).addChildren(
                    Rule.getExtendStr(Rule.VariableDeclarationFragment, ",", fragments.size())
            ));
            visitUnitList(fragments);
        }

        return false;
    }

    public boolean visit(VariableDeclarationFragment node) {

        int dimensionSize = node.getExtraDimensions();
        Expression initializer = node.getInitializer();

        Rule rule = new Rule(Rule.VariableDeclarationFragment);
        rule.addChild(Rule.Name)
                .addChild(Rule.Dimensions, dimensionSize > 0)
                .addChildren(initializer != null, "=", Rule.Expression);
        addRule(rule);

        addRule(new Rule(Rule.Name).addChild(node.getName()));
        node.getName().accept(this);

        if (dimensionSize > 0) {
            if (node.getAST().apiLevel() >= 8) {
                addRule(new Rule(Rule.Dimensions).addChildren(
                        Rule.getExtendStr(Rule.Dimension, " ", dimensionSize)
                ));

                visitUnitList(node.extraDimensions());
            } else {
                addRule(new Rule(Rule.Dimensions).addChildren(
                        Rule.getExtendStr("[]", " ", dimensionSize)
                ));
            }
        }

        if (initializer != null) {
            addRule(new Rule(Rule.Expression).addChild(initializer));
            initializer.accept(this);
        }

        return false;
    }

    public boolean visit(VariableDeclarationStatement node) {
        int apiLevel = node.getAST().apiLevel();

        String modifiersStr = apiLevel == 2 ?
                getModifiers(node.getModifiers()) : getModifiers(node.modifiers());

        List<VariableDeclarationFragment> fragments = node.fragments().isEmpty() ?
                null : node.fragments();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Modifier, modifiersStr.length() > 0)
                .addChild(Rule.Type)
                .addChild(Rule.VariableDeclarationFragments, fragments != null)
                .addChild(";");
        addRule(rule);

        if (modifiersStr.length() > 0)
            addRule(new Rule(Rule.Modifier).addChildren(modifiersStr));


        addRule(new Rule(Rule.Type).addChild(node.getType()));
        node.getType().accept(this);

        if (fragments != null) {
            addRule(new Rule(Rule.VariableDeclarationFragments).addChildren(
                    Rule.getExtendStr(Rule.VariableDeclarationFragment, ",", fragments.size())
            ));
            visitUnitList(fragments);
        }

        return false;
    }

    public boolean visit(WhileStatement node) {
        Rule rule = new Rule(node);
        rule.addChild("while").addChild("(").addChild(Rule.Expression).addChild(")")
                .addChild(Rule.Statement);

        addRule(rule);

        addRule(new Rule(Rule.Expression).addChild(node.getExpression()));
        node.getExpression().accept(this);

        addRule(new Rule(Rule.Statement).addChild(node.getBody()));
        node.getBody().accept(this);
        return false;
    }

    public boolean visit(WildcardType node) {

        List<Annotation> annotations = node.annotations().isEmpty() ?
                null : node.annotations();

        Type bound = node.getBound();

        Rule rule = new Rule(node);
        rule.addChild(Rule.Annotations, annotations != null)
                .addChild("?")
                .addChild("extends", bound != null && node.isUpperBound())
                .addChild("super"  , bound != null && !node.isUpperBound())
                .addChild(Rule.Type, bound != null);
        addRule(rule);

        if (annotations != null) {
            addRule(new Rule(Rule.Annotations).addChildren(
                    Rule.getExtendStr(Rule.Annotation, " ", annotations.size())
            ));
            visitUnitList(annotations, Rule.Annotation);
        }

        if (bound != null) {
            addRule(new Rule(Rule.Type).addChild(bound));
            bound.accept(this);
        }

        return false;
    }


    public static void main(String[] args) {
        System.out.println(Block.class.toString());
    }
}
