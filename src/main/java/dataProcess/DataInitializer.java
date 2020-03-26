package dataProcess;


import com.sun.org.apache.bcel.internal.classfile.Code;
import javafx.util.Pair;
import org.eclipse.jdt.core.dom.ASTParser;
import rule.Rule;
import rule.RuleGenerator;

import javax.xml.stream.events.Namespace;
import java.io.*;
import java.util.*;

/**
 * DataInitializer 有两方面的作用：
 * 1，从源代码中抽取预期的rule list
 * 2，从源代码中抽取去相应的token。
 */
public class DataInitializer {

    public interface Compare <T extends Object>{
        public boolean isValid(T item, int times);
    }


    // 1 表示 获取常见的name 和 number
    // 2 表示 获取所有的rule list
    // 3 表示 生成数据
    public static int parseType = 1;

    public static String RuleCodesPath  = "RuleCodes";
    public static Map<Rule, Integer> RuleCodes;

    public static String TokenCodesPath = "TokenCodes";
    public static Map<String, Integer> TokenCodes;

    public static String NamesPath      = "Names";
    public static Set<String> Names;

    public static String NumbersPath     = "Numbers";
    public static Set<String> Numbers;




    public static int nameTimeBound     = 100;

    public static int numberTimeBound   = 100;

    public static int ruleTimeBound     = 100;


    static {
    }

    public static void initialize() {
        if (DataInitializer.parseType == 1) {
            RuleCodes            = new HashMap<>();
            TokenCodes           = new HashMap<>();
            Names                = new HashSet<>();
            Numbers              = new HashSet<>();
        }

        String line = "";
        BufferedReader reader;

        if (DataInitializer.parseType >= 2) {
            try {
                reader = new BufferedReader(new FileReader(new File(NamesPath)));
                while ((line = reader.readLine()) != null) {
                    Names.add(line);
                }
                reader.close();

                reader = new BufferedReader(new FileReader(new File(NumbersPath)));
                while ((line = reader.readLine()) != null) {
                    Numbers.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        if (DataInitializer.parseType == 3) {
            try {
                int index = 1;
                reader = new BufferedReader(new FileReader(new File(RuleCodesPath)));
                while ((line = reader.readLine()) != null) {
                    RuleCodes.put(new Rule().init(line), index++);
                }
                reader.close();

                index = 1;
                reader = new BufferedReader(new FileReader(new File(TokenCodesPath)));
                while ((line = reader.readLine()) != null) {
                    TokenCodes.put(line, index++);
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }


    public static <T extends Object> List<T> store(Map<T, Integer> itemMap, int timeBound, String storePath) {
        List<T> res = new ArrayList<>();

        List<Pair<T, Integer>> itemList = new ArrayList<>();
        for (Map.Entry<T, Integer> entry: itemMap.entrySet()) {
            itemList.add(new Pair<T, Integer>(entry.getKey(), entry.getValue()));
        }

        Collections.sort(itemList, (a, b) -> {
            return b.getValue() - a.getValue();
        });


        StringBuilder all     = new StringBuilder();
        StringBuilder builder = new StringBuilder();
        for (Pair<T, Integer> p: itemList) {
            T item = p.getKey();
            int times = p.getValue();
            all.append(times).append(": ").append(item.toString()).append("\n");

            if (times >= timeBound) {
                res.add(item);
                builder.append(item.toString()).append("\n");
            }
        }

        write(new File(storePath + "_all"), all.toString());
        write(new File(storePath), builder.toString());

        return res;
    }

    public static void parse() {
        initialize();

        String baseDir = "/Users/caosheng/Desktop/code change/ambari";

        for (File file: new File(baseDir).listFiles()) {
            parseDir(file.getAbsolutePath());
        }

        if (parseType == 1) {
            store(RuleGenerator.nameLiterals, nameTimeBound, NamesPath);
            store(RuleGenerator.numberLiterals, numberTimeBound, NumbersPath);
        } else if (parseType == 2) {
            Map<Rule, Integer> normalRules = new HashMap<>();
            for (Map.Entry<Rule, Integer> entry: RuleGenerator.rules.entrySet()) {
                if (entry.getKey().head.equals(Rule.Copy)) continue;
                normalRules.put(entry.getKey(), entry.getValue());
            }

            normalRules.put(new Rule(Rule.Copy), ruleTimeBound);

            List<Rule> rules = store(normalRules, ruleTimeBound, RuleCodesPath);
            Set<String> tokens = new HashSet<>();

            for (Rule rule: rules) {
                tokens.add(rule.head);
                tokens.addAll(rule.children);
            }

            StringBuilder builder = new StringBuilder();
            for (String token: tokens)
                builder.append(token).append("\n");
            write(new File(TokenCodesPath), builder.toString());

        } else if (parseType == 3) {

        }
    }

    public static void parseDir(String dir) {
        System.out.println(dir);

        String newDir = dir + "/new";
        String oldDir = dir + "/old";

        if (DataInitializer.parseType == 3) {

            int count = 0;
            StringBuilder builder = new StringBuilder();
            for (File file: new File(oldDir).listFiles()) {
                String fileName = file.getName();
                String originalStr = read(file);
                String modifiedStr = read(new File(newDir + "/" + fileName));

                CodeRepresentation representation =
                        new CodeRepresentation(originalStr, modifiedStr, ASTParser.K_CLASS_BODY_DECLARATIONS);

                String originalSeq = CodeRepresentation.toString(representation.originalCodedSequences);
                String modifiedSeq = CodeRepresentation.toString(representation.modifiedCodedSequences);
                String targetSeq   = CodeRepresentation.toString(representation.targetSequence);

                builder.append(originalSeq).append("\n")
                        .append(modifiedSeq).append("\n")
                        .append(targetSeq).append("\n")
                        .append("\n");
                break;
            }

            write(new File(dir + "/data" ), builder.toString());

        } else {
            for (File file : new File(oldDir).listFiles()) {
                String fileName = file.getName();

                String originalStr = read(file);
                String modifiedStr = read(new File(newDir + "/" + fileName));

                CodeProcessor processor = new CodeProcessor().setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
                processor.setSource(originalStr.toCharArray());
                processor.generateRules();


                processor.setSource(modifiedStr.toCharArray());
                processor.generateRules();

                break;
            }


        }
    }

    public static String read(File file) {
        StringBuilder builder = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static void write(File file, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void test() {
        String fileName = "code.txt";
        String content = read(new File(fileName));

        CodeRepresentation code = new CodeRepresentation(content, content, ASTParser.K_CLASS_BODY_DECLARATIONS);

        int a = 2;
    }

    public static void main(String[] args) {
        DataInitializer.parseType = 1;
        parse();

        DataInitializer.parseType = 2;
        parse();

        DataInitializer.parseType = 3;
        parse();
    }
}
