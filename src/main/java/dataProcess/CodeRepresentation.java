package dataProcess;

import org.eclipse.jdt.core.dom.ASTParser;
import rule.Rule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

public class CodeRepresentation {
    public static Map<String, Integer> tokenCodes   = new HashMap<>();
    public static Map<Rule  , Integer> ruleCodes    = new HashMap<>();

    static {
        tokenCodes = new HashMap<>();
        try {
//            ResourceBundle bundle   = ResourceBundle.getBundle("properties");
//            String tokenCodesDir    = bundle.getString("TokenCodes");
//            BufferedReader reader   = new BufferedReader(new FileReader(new File(tokenCodesDir)));
//
//            String line = null;
//            int index = 1; // 0 for UNK
//            while ((line = reader.readLine()) != null) {
//                line = line.trim();
//                tokenCodes.put(line, index++);
//            }
//
//            String ruleCodesDir     = bundle.getString("RuleCodes");
//            reader                  = new BufferedReader(new FileReader(new File(tokenCodesDir)));
//
//            index = 1;  // 0 for extend
//            while ((line = reader.readLine()) != null) {
//                Rule rule = new Rule().init(line);
//                ruleCodes.put(rule, index ++);
//            }

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
        List<Integer> res = new ArrayList<>();
        for (Rule rule: sequence) {
            int code = ruleCodes.getOrDefault(rule, 0);
            if (code == 0) {
                new Exception("Error: rule code is 0!").printStackTrace();
                return null;
            } else {
                res.add(code);
            }
        }
        return res;
    }

    public boolean isValid() {
        return originalCodedSequences != null && originalCodedSequences.size() > 0
                && modifiedCodedSequences != null && modifiedCodedSequences.size() > 0
                && targetSequence != null && targetSequence.size() > 0 ;
    }




}
