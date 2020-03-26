package dataProcess;

import org.eclipse.jdt.core.dom.ASTParser;
import rule.Rule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class CodeRepresentation {
    public static Map<String, Integer> tokenCodes;
    public static Map<Rule  , Integer> ruleCodes;

    static {
        tokenCodes = new HashMap<>();
        try {
            tokenCodes = new HashMap<>();
            ruleCodes  = new HashMap<>();
            BufferedReader reader   = new BufferedReader(new FileReader(new File("TokenCodes")));

            String line;
            int index = 1; // 0 for UNK
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                tokenCodes.put(line, index++);
            }

            reader = new BufferedReader(new FileReader(new File("RuleCodes")));

            index = 1;  // 0 for extend
            while ((line = reader.readLine()) != null) {
                Rule rule = new Rule().init(line);
                ruleCodes.put(rule, index ++);
            }

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    CodeProcessor processor = new CodeProcessor();

    public List<Rule> originalRules;
    public List<Rule> modifiedRules;

    public List<Integer> targetSequence;


    public List<String> originalSequences;
    public List<String> modifiedSequences;


    public List<Integer> originalCodedSequences;
    public List<Integer> modifiedCodedSequences;



    public CodeRepresentation(String original, String modified, int kind) {
        processor.setKind(kind);

        originalRules = processor.setSource(original.toCharArray())
                .generateRules();
        originalSequences = processor.getSequence();
        originalCodedSequences = getCodedTokenSequence(originalSequences);

        modifiedRules = processor.setSource(modified.toCharArray())
                .generateRules();
        modifiedSequences = processor.getSequence();
        modifiedCodedSequences = getCodedTokenSequence(modifiedSequences);

        targetSequence = getCodedRuleSequence(modifiedRules);
    }


    public List<Integer> getCodedTokenSequence (List<String> sequence) {
        List<Integer> res = new ArrayList<>();
        for (String token: sequence) {
            res.add(tokenCodes.getOrDefault(token, 0));
        }

        return res;
    }

    public List<Integer> getCodedRuleSequence (List<Rule> sequence) {
        Rule temp = new Rule(Rule.Copy);

        List<Integer> res = new ArrayList<>();
        for (Rule rule: sequence) {
            int code = ruleCodes.getOrDefault(rule, 0);
            if (code != 0) {
                res.add(code);
            } else if (rule.head.equals(Rule.Copy)) {
                res.add(ruleCodes.get(temp));
            } else {
                res.add(0);
            }
        }
        return res;
    }

    public static String toString(List<Integer> sequence) {
        StringBuilder builder = new StringBuilder();
        for (Integer i: sequence) {
            builder.append(i).append(" ");
        }
        return builder.toString().trim();
    }

    public boolean isValid() {
        return originalCodedSequences != null && originalCodedSequences.size() > 0
                && modifiedCodedSequences != null && modifiedCodedSequences.size() > 0
                && targetSequence != null && targetSequence.size() > 0 ;
    }
}
