package dataProcess;


import javafx.util.Pair;
import org.eclipse.jdt.core.dom.ASTParser;
import rule.Rule;
import rule.RuleGenerator;

import java.io.*;
import java.util.*;

/**
 * DataInitializer 有两方面的作用：
 * 1，从源代码中抽取预期的rule list
 * 2，从源代码中抽取去相应的token。
 */
public class DataInitializer {

    public static String TokenCodes  = "";
    public static String RuleCodes   = "";

    static {
//        ResourceBundle bundle = ResourceBundle.getBundle("properties");
//        TokenCodes  = bundle.getString("TokenCodes");
//        RuleCodes   = bundle.getString("RuleCodes");
    }


    public static void parse() {
        String baseDir = "/Users/caosheng/Desktop/code change/ambari";

        for (File file: new File(baseDir).listFiles()) {
            parseDir(file.getAbsolutePath());
        }

        List<Pair<Rule, Integer>> rules = new ArrayList<>();
        for (Map.Entry<Rule, Integer> entry: RuleGenerator.rules.entrySet()) {
            rules.add(new Pair<>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(rules, (a, b) -> {
            int differences = b.getValue() - a.getValue();
            return differences != 0 ? differences : (a.getKey().head.compareTo(b.getKey().head));
        });

        StringBuilder builder = new StringBuilder();
        Set<String> tokens = new HashSet<>();
        for (Pair<Rule, Integer> rulePair: rules) {
            Rule rule = rulePair.getKey();
            builder.append(rulePair.getValue()).append(": ").append(rulePair.getKey().toString()).append("\n");
            tokens.add(rule.head);
            for (String c: rule.children)
                tokens.add(c);
        }

        TokenCodes = "TokenCodes";
        RuleCodes  = "RuleCodes";

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(TokenCodes)));
            writer.write(builder.toString());
            writer.close();

            writer = new BufferedWriter(new FileWriter(new File(RuleCodes)));

            for (String token: tokens)
                writer.write(token + "\n");
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    public static void parseDir(String dir) {
        System.out.println(dir);

        String newDir = dir + "/new";
        String oldDir = dir + "/old";

        try {
            for (File file : new File(oldDir).listFiles()) {
                String fileName = file.getName();

                String originalStr = read(file);
                String modifiedStr = read(new File(newDir + "/" + fileName));

                CodeRepresentation code = new CodeRepresentation(originalStr, modifiedStr, ASTParser.K_CLASS_BODY_DECLARATIONS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static String read(File file) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static void test() {
        String fileName = "code.txt";
        String content = read(new File(fileName));

        CodeRepresentation code = new CodeRepresentation(content, content, ASTParser.K_CLASS_BODY_DECLARATIONS);

        int a = 2;
    }

    public static void main(String[] args) {
        parse();
        //test();
    }
}
