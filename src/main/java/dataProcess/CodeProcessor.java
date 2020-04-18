package dataProcess;

import org.eclipse.jdt.core.dom.*;
import rule.CleanVisitor;
import rule.Rule;
import rule.RuleGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class CodeProcessor {
    public static Set<String> tokens = null;
    public static final String LEFT = "{{";
    public static final String RIGHT = "}}";


    private char[] sourceCode = "".toCharArray();
    private int kind = ASTParser.K_COMPILATION_UNIT;
    public List<Rule> rules = null;
    private ASTNode root;


    public ASTNode getRoot() {
        if (root == null)
            root = getNode(sourceCode, kind);
        return root;
    }

    CodeProcessor setSource(char[] sourceCode, int kind) {
        this.kind = kind;
        this.sourceCode =getCleanCode(sourceCode, kind);
        return this;
    }

    public static char[] getCleanCode(char[] code, int kind) {
        CleanVisitor visitor = new CleanVisitor();
        ASTNode node = getNode(code, kind);
        node.accept(visitor);
        return visitor.getResult().toCharArray();
    }

    public static ASTNode getNode(char[] code, int kind) {
        try {
            ASTParser parser = ASTParser.newParser(9);
            parser.setKind(kind);
            parser.setSource(code);

            TypeDeclaration node  = (TypeDeclaration) (parser.createAST(null));
            MethodDeclaration method = (MethodDeclaration) (node.bodyDeclarations().get(0));
            return method;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String generateConnectMatrix(List<Rule> ruleList) {

        StringBuilder builder = new StringBuilder();

        Stack<String> ops = new Stack<>();
        ops.push("<END>");
        ops.push(ruleList.get(0).head);

        Stack<Integer> parents = new Stack<>();
        for (int index = 0; index < ruleList.size(); index++) {
            String top = ops.pop();
            while (top.equals("<END>")) {
                top = ops.pop();
                parents.pop();
            }

            Rule rule = ruleList.get(index);

            if (!top.equals(rule.head))
                new Exception("error from CodeProcessor").printStackTrace();

            if (!parents.isEmpty()) // 第一个节点会出现没有父亲节点的情况
                builder.append(parents.peek()).append(" ").append(index).append(" ");

            parents.push(index);
            ops.push("<END>");
            for (int i = rule.children.size() - 1; i >= 0; i --) {
                String child = rule.children.get(i);
                if (child.startsWith(Rule.HEAD)) {
                    ops.push(child);
                }
            }
        }

        return builder.toString().trim();
    }

    public List<Rule> generateRules(RuleGenerator generator) {
        generator.clear(); // 如果generator在重复使用，则需要把之前保留的添加的rule清除。


        root = getNode(sourceCode, kind);
        root.accept(generator);

        rules = generator.addedRules;
        return rules;
    }

    public String recover(boolean printTrace) {
        if (rules != null)
            return CodeProcessor.recover(rules, printTrace);
        else {
            new Exception("Rule list is empty!").printStackTrace();
            return "";
        }
    }

    public static String recover(List<Rule> ruleList, boolean printTrace) {
        Set<String> heads = new HashSet<>();
        for (Rule rule: ruleList)
            heads.add(rule.head);

        StringBuilder res = new StringBuilder();

        Stack<String> ops = new Stack<String>();

        ops.push(ruleList.get(0).head);
        Iterator<Rule> it = ruleList.iterator();

        int indent = 0;
        int count = 0;
        while (!ops.empty()) {
            String top = ops.pop();
            if (top.equals("$END$")) {
                indent --;
                continue;
            }

            if (!heads.contains(top)) {
                res.append(top).append(" ");
                continue;
            }

            if (!it.hasNext()) {
                break;
            }
            Rule rule = it.next();

            if (printTrace) {
                String indentString = "";
                for (int i = 0; i < indent; i ++) indentString += "\t";
                System.out.print(String.format("%-5s", count));
                System.out.println(indentString + top + " " + rule.toString());
                count ++;
            }

            if (!rule.head.equals(top)) break;

            //插入的时候 是倒着插入的
            indent ++;
            ops.push("$END$");

            if (rule.head.equals(Rule.Copy)){
                int index = Integer.parseInt(rule.children.get(0));
                ops.push(ruleList.get(index).children.get(0));
            } else {
                for (int i = rule.children.size() - 1; i >= 0; i --) {
                    ops.push(rule.children.get(i));
                }
            }
        }

        return res.toString();
    }

    public List<String> getSequence() {
        if (rules != null) {
            return getSequence(this.rules);
        } else {
            new Exception("Rule list is empty").printStackTrace();
            return null;
        }
    }

    public static List<String> getSequence(List<Rule> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            new Exception("Rule list is empty").printStackTrace();
            return null;
        }

        List<String> result = new ArrayList<>();

        Set<String> heads = new HashSet<>();
        for (Rule rule: ruleList) heads.add(rule.head);

        Stack<String> visitHeads = new Stack<>();
        Stack<String> ops = new Stack<>();
        ops.push(ruleList.get(0).head);

        Iterator<Rule> it = ruleList.iterator();

        while (!ops.isEmpty()) {
            String top = ops.pop();

            if (top.equals("$END$")) {
                result.add(RIGHT);
                result.add(visitHeads.pop());
                continue;
            } else if (!heads.contains(top)) {
                result.add(LEFT);
                result.add(top);
                result.add(RIGHT);
                result.add(top);
                continue;
            }

            Rule rule = it.next();
            ops.push("$END$");

            if (rule.head.equals(top) && !top.equals(Rule.Copy)){
                visitHeads.push(top);
                result.add(LEFT);
                result.add(top);


                for (int i = rule.children.size() - 1; i >= 0; i--) {
                    ops.push(rule.children.get(i));
                }
            } else if (rule.head.equals(top) && top.equals(Rule.Copy)) {
                visitHeads.push(top);
                int index = Integer.parseInt(rule.children.get(0));
                String copyName = ruleList.get(index).children.get(0);
                result.add(LEFT);
                result.add(Rule.Copy);
                result.add(LEFT);
                result.add(copyName);
                result.add(RIGHT);
                result.add(copyName);
            } else {
                new Exception("Rule list is invalid").printStackTrace();
            }
        }

        return result;
    }

    public static void prettifyPrint(List<String> sequence) {
        for (String s: sequence) System.out.print(s + " ");
        System.out.println();

        int indent = 0;
        for (int i = 0; i < sequence.size(); i++) {
            String s = sequence.get(i);
            if (s.equals(LEFT)) {

                System.out.println();
                for (int j = 0; j < indent; j++) System.out.print(" ");

                System.out.print(LEFT + " " + sequence.get(++i) + " ");
                if (sequence.get(i + 1).equals(RIGHT)) {
                    indent--;
                    i++;
                    System.out.print(RIGHT + " " + sequence.get(++ i) + " ");
                }
                indent ++;
            } else if (s.equals(RIGHT)){
                indent --;
                System.out.println();
                for (int j = 0; j < indent; j++) System.out.print(" ");

                System.out.print(RIGHT + " " + sequence.get(++i) + " ");
            } else {
                System.out.print(sequence.get(i) + " ");
            }

        }
        System.out.println();

    }

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File("code.txt")));
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            CodeProcessor processor = new CodeProcessor()
                    .setSource(builder.toString().toCharArray(), ASTParser.K_CLASS_BODY_DECLARATIONS);

            //processor.generateRules();

            processor.prettifyPrint(processor.getSequence());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
