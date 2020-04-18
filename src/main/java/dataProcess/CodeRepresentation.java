package dataProcess;

import org.eclipse.jdt.core.dom.ASTParser;
import rule.Rule;
import rule.RuleGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class CodeRepresentation {

    public List<Rule> originalRules;
    public List<Rule> modifiedRules;

    public List<Integer> targetSequence;
    public List<String> parentSequence;


    public List<String> originalSequences;
    public List<String> modifiedSequences;

    public List<String> diffSequence;


    public List<Integer> originalCodedSequences;
    public List<Integer> modifiedCodedSequences;

    public DiffGraph diffGraph;

    public CodeRepresentation(List<Rule> originalRules, List<Rule> modifiedRules) {
        this.originalRules = originalRules;
        originalSequences = CodeProcessor.getSequence(originalRules);
        originalCodedSequences = getCodedTokenSequence(originalSequences);

        targetSequence = getCodedRuleSequence(originalRules);
        generateParentSequence();

        diffGraph = new DiffGraph(originalRules, modifiedRules);
        diffGraph.generateSequence();

    }

    public CodeRepresentation(String original, String modified, int kind) {
        char[] originalArray = CodeProcessor.getCleanCode(original.toCharArray(), kind);

        char[] modifiedArray = CodeProcessor.getCleanCode(modified.toCharArray(), kind);


        RuleGenerator generator = new RuleGenerator();


        CodeProcessor originalProcessor = new CodeProcessor();

        originalRules = originalProcessor.setSource(originalArray, kind)
                .generateRules(generator);
        originalSequences = originalProcessor.getSequence();
        originalCodedSequences = getCodedTokenSequence(originalSequences);

        CodeProcessor modifiedProcessor = new CodeProcessor();
        modifiedRules = modifiedProcessor.setSource(modifiedArray, kind)
                .generateRules(generator);
        modifiedSequences = modifiedProcessor.getSequence();
        //modifiedCodedSequences = getCodedTokenSequence(modifiedSequences);

        targetSequence = getCodedRuleSequence(modifiedRules);
        generateParentSequence();

        diffGraph = new DiffGraph(originalProcessor.getRoot(), modifiedProcessor.getRoot(), generator.original2convertMap);
        diffGraph.generateSequence();
    }


    public List<Integer> getCodedTokenSequence (List<String> sequence) {
        List<Integer> res = new ArrayList<>();
        for (String token: sequence) {
            if (DataInitializer.TokenCodes.containsKey(token)) {
                res.add(DataInitializer.TokenCodes.get(token));
            } else {
                new Exception("error from CodeRepresentation.java: Token has not been coded ->" + token).printStackTrace();
            }
        }

        return res;
    }

    public List<Integer> getCodedRuleSequence(List<Rule> sequence) {
        Rule temp = new Rule(Rule.Copy);

        List<Integer> res = new ArrayList<>();
        for (Rule rule: sequence) {
            int code = DataInitializer.RuleCodes.getOrDefault(rule, -1);
            if (code != -1) {
                res.add(code);
            } else if (rule.head.equals(Rule.Copy)) {
                int index = Integer.parseInt(rule.children.get(0));
                res.add(100000 + index);
                //res.add(ruleCodes.get(temp));
            } else {
                new Exception().printStackTrace();
            }
        }
        return res;
    }

    private void generateParentSequence() {
        Stack<String> parents = new Stack<>();
        parents.push("<Start>");

        Set<String> heads = new HashSet<>();
        for (Rule rule: modifiedRules)
            heads.add(rule.head);

        parentSequence = new ArrayList<>();

        Stack<String> ops = new Stack<>();
        ops.push(modifiedRules.get(0).head);

        Iterator<Rule> it = modifiedRules.iterator();
        while (!ops.isEmpty()) {
            String top = ops.pop();

            if (!heads.contains(top)) continue;

            if (top.equals("<END>")) {
                parents.pop();
                continue;
            }

            parentSequence.add(parents.peek());

            Rule rule = it.next();
            for (int i = rule.children.size() - 1; i >= 0; i--)
                ops.push(rule.children.get(i));

            parents.push(rule.head);
        }
    }

    public  static <T extends Object> String toString(List<T> sequence) {
        StringBuilder builder = new StringBuilder();
        for (T i: sequence) {
            builder.append(i).append(" ");
        }
        return builder.toString().trim();
    }

    public String getString() {
        StringBuilder builder = new StringBuilder();
        builder.append(toString(originalCodedSequences)).append("\n");

        builder.append(toString(getCodedTokenSequence(diffGraph.sequences))).append("\n");
        builder.append(diffGraph.matrix2String()).append("\n");

        builder.append(toString(targetSequence)).append("\n");
        builder.append(toString(getCodedTokenSequence(parentSequence))).append("\n");
        builder.append(CodeProcessor.generateConnectMatrix(modifiedRules));

        return builder.toString();
    }

    public boolean isValid() {
        return originalCodedSequences != null && originalCodedSequences.size() > 0
                && targetSequence != null && targetSequence.size() > 0 ;
    }
}
