package dataProcess;


import com.sun.org.apache.bcel.internal.classfile.Code;
import javafx.util.Pair;
import org.eclipse.jdt.core.dom.ASTParser;
import rule.Generator;
import rule.Rule;
import rule.RuleGenerator;

import javax.xml.stream.events.Namespace;
import java.io.*;
import java.util.*;
import java.util.stream.Collector;

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

    public static String RuleCodesPath  = "/Users/caosheng/PycharmProjects/TreeGen/Files/RuleCodes";
    public static Map<Rule, Integer> RuleCodes;

    public static String TokenCodesPath = "/Users/caosheng/PycharmProjects/TreeGen/Files/TokenCodes";
    public static Map<String, Integer> TokenCodes;

    public static String NamesPath      = "/Users/caosheng/PycharmProjects/TreeGen/Files/Names";
    public static Set<String> Names;

    public static String NumbersPath     = "/Users/caosheng/PycharmProjects/TreeGen/Files/Numbers";
    public static Set<String> Numbers;

    public static String TrainPath = "/Users/caosheng/PycharmProjects/TreeGen/Files/data_train";
    public static String DevPath   = "/Users/caosheng/PycharmProjects/TreeGen/Files/data_dev";
    public static String TestPath  = "/Users/caosheng/PycharmProjects/TreeGen/Files/data_test";

    public static int nameTimeBound     = 100;

    public static int numberTimeBound   = 100;



//    static {
//        if (new File(TokenCodesPath).exists())
//            TokenCodes = DataInitializer.readIndex(TokenCodesPath);
//
//        if (new File(RuleCodesPath).exists()) {
//            DataInitializer.RuleCodes = new HashMap<>();
//            for (Map.Entry<String, Integer> item : DataInitializer.readIndex(DataInitializer.RuleCodesPath).entrySet()) {
//                DataInitializer.RuleCodes.put(new Rule().init(item.getKey()), item.getValue());
//            }
//        }
//    }

    public static void initRuleAndToken(String ruleDir, String tokenDir) {
        if (new File(tokenDir).exists())
            TokenCodes = DataInitializer.readIndex(tokenDir);

        if (new File(ruleDir).exists()) {
            DataInitializer.RuleCodes = new HashMap<>();
            for (Map.Entry<String, Integer> item : DataInitializer.readIndex(ruleDir).entrySet()) {
                DataInitializer.RuleCodes.put(new Rule().init(item.getKey()), item.getValue());
            }
        }
    }

    public static void initialize() {
        Generator.rules = new HashMap<>();

        if (DataInitializer.parseType == 1) {
            RuleCodes            = new HashMap<>();
            TokenCodes           = new HashMap<>();
            Names                = new HashSet<>();
            Numbers              = new HashSet<>();

        }

        if (DataInitializer.parseType >= 2) {
            try {
                Names = new HashSet<>();
                Numbers = new HashSet<>();
                String line = "";
                BufferedReader reader = new BufferedReader(new FileReader(new File(NamesPath)));
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
                RuleCodes = new HashMap<>();
                DataInitializer.readIndex(RuleCodesPath).forEach((a, b) -> {
                    RuleCodes.put(new Rule().init(a), b);
                });

                TokenCodes = DataInitializer.readIndex(TokenCodesPath);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static <T extends Object> void store(Collection<T> itemList, String storePath) {
        Iterator<T> it = itemList.iterator();
        StringBuilder builder = new StringBuilder();
        while (it.hasNext()) {
            builder.append(it.next().toString()).append("\n");
        }

        write(new File(storePath), builder.toString());
    }


    public static <T extends Object> List<T> store(Map<T, Integer> itemMap, String storePath, Compare<T> compare) {
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

            if (compare.isValid(item, times)) {
                res.add(item);

                builder.append(item.toString()).append("\n");
            }
        }

        write(new File(storePath + "_all"), all.toString());
        write(new File(storePath), builder.toString());

        return res;
    }

    // 这个函数用于读取带有编码的数据，
    // 文件格式是：第一行为编码，第二行为数据；然后重复
    public static Map<String, Integer> readIndex(String filePath) {
        Map<String, Integer> result = new HashMap<String, Integer>();
        try{
            String line = "";

            BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)));
            int index = 0;
            while ((line = reader.readLine()) != null) {
                result.put(line, index ++);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void parse() {
        initialize();

        String baseDir = "/Users/caosheng/Desktop/code change/ambari/";


        int count = 0;
        for (File file: new File(baseDir).listFiles()) {
            if (file.isFile()) continue;
            parseDir(file.getAbsolutePath());
            //if (count++ > 10) break;
        }

        if (parseType == 1) { // 确定常见的变量名
            store(RuleGenerator.nameLiterals, NamesPath, (a, time) ->  time > nameTimeBound);
            store(RuleGenerator.numberLiterals, NumbersPath, (a, time) -> time > numberTimeBound);
        } else if (parseType == 2) { // 确定常见的rule
            List<Rule> rules = store(RuleGenerator.rules, RuleCodesPath, (rule, time) -> !rule.head.equals(Rule.Copy));

            Set<String> tokens = new HashSet<>();
            tokens.add(CodeProcessor.LEFT); // 代码序列需要
            tokens.add(CodeProcessor.RIGHT); // 代码序列需要
            tokens.add(Rule.Start); // 代码开始节点
            tokens.add("{}"); // gumtree的结果中有这个符号，所以要加上
            tokens.add("<Start>");
            for (Rule rule: rules) {
                tokens.add(rule.head);
                tokens.addAll(rule.children);
            }
            store(tokens, TokenCodesPath);
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

                if (representation.isValid())
                    builder.append(representation. getString()).append("\n").append("\n");
                if (count ++ > 1000) break;
            }

            write(new File(TrainPath), builder.toString());
            write(new File(DevPath), builder.toString());
            write(new File(TestPath), builder.toString());
        } else {
            int count = 0;
            for (File file : new File(oldDir).listFiles()) {
                String fileName = file.getName();

                String originalStr = read(file);
                String modifiedStr = read(new File(newDir + "/" + fileName));

                RuleGenerator generator = new RuleGenerator();
                try {


                    CodeProcessor processor = new CodeProcessor().setSource(originalStr.toCharArray(), ASTParser.K_CLASS_BODY_DECLARATIONS);
                    processor.generateRules(generator);

                    processor.setSource(modifiedStr.toCharArray(), ASTParser.K_CLASS_BODY_DECLARATIONS);
                    processor.generateRules(generator);
                } catch (Exception e) {
                    System.out.println("error: " + (originalStr.contains("assert") || modifiedStr.contains("assert")));
                }
                if (count ++ > 1000) break;
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

    }

    public static void cleanCode() {
        for (File file: new File("/Users/caosheng/Desktop/code change/ambari/").listFiles()) {

            List<File> files = new ArrayList<>();
            for (File f: new File(file.getAbsolutePath() + "/new").listFiles())
                files.add(f);


            for (File f: files) {
                File oldFile = new File(f.getAbsolutePath().replaceAll("new", "old"));
                try {
                    String code = read(f);
                    write(f, new String(CodeProcessor.getCleanCode(code.toCharArray(), ASTParser.K_CLASS_BODY_DECLARATIONS)));

                    code = read(oldFile);
                    write(oldFile, new String(CodeProcessor.getCleanCode(code.toCharArray(), ASTParser.K_CLASS_BODY_DECLARATIONS)));
                } catch (Exception e) {
                    f.delete();
                    oldFile.delete();
                    System.out.println(f.getAbsolutePath());
                    continue;
                }
            }
        }
    }


    public static void main(String[] args) {
        //cleanCode();

        DataInitializer.parseType = 1;
        parse();

        DataInitializer.parseType = 2;
        parse();

        DataInitializer.parseType = 3;
        parse();
    }
}
