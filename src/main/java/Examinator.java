import dataProcess.CodeProcessor;
import javafx.util.Pair;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import rule.Rule;
import rule.Generator;
import rule.RuleGenerator;
import rule.ToString;

import java.io.*;
import java.util.*;

public class Examinator {

    public boolean log = false;

    private Set<String> getHeads(List<Rule> rules) {
        Set<String> heads = new HashSet<>();
        for (Rule rule: rules) {
            heads.add(rule.head);
        }
        return heads;
    }

    public boolean examine(String target, String source) {
        return target.replaceAll(" ", "").equals(source.replaceAll(" ", ""));
    }

    public static List<File> files = new ArrayList<>();
    public static int count = 0;

    static void read(File file) {
        //if (files.size() > 300) return;

        for (File f: file.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".java")) {
                count ++;
                System.out.println(count + ": " + file.getAbsoluteFile());
                files.add(f);
            }
            else if (f.isDirectory()) read(f);
        }
    }

    public static void main(String[] args) throws IOException {


        System.out.println(new File("").getAbsolutePath());

        //files.add(new File("code.txt"));
        //files.add(new File("/Users/caosheng/Desktop/jdk/jdk/test/jdk/java/lang/annotation/typeAnnotations/TestExecutableGetAnnotatedType.java"));
        read(new File("/Users/caosheng/Desktop/jdk"));

        long ruleCount = 0, methodCount = 0;
        for (int i = 0; i < files.size(); i++){
            if (i % 100 == 0) System.out.println(i);
            File file = files.get(i);
            String code = "";
            try {
                //BufferedReader reader = new BufferedReader(new FileReader(new File("/Users/caosheng/Desktop/jdk/jdk/test/micro/org/openjdk/bench/java/net/DatagramSocketSendReceive.java")));
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null)
                    code += line + "\n";



                ASTParser parser = ASTParser.newParser(8);
                parser.setSource(code.toCharArray());
                parser.setKind(ASTParser.K_COMPILATION_UNIT);

                CompilationUnit unit = (CompilationUnit) parser.createAST(null);

                List<MethodDeclaration> methods = new ArrayList<>();
                unit.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        methods.add(node);
                        return false;
                    }
                });

                Examinator examinator = new Examinator();
                for (MethodDeclaration method : methods) {
                    methodCount ++;
                    RuleGenerator generator = new RuleGenerator();
                    method.accept(generator);

                    String target = new ToString().toString(method).replaceAll("\\s", "");
                    String source = CodeProcessor.recover(generator.addedRules, false).replaceAll("\\s", "");


                    //CodeProcessor.prettifyPrint(CodeProcessor.getSequence(generator.addedRules));

                    ruleCount += generator.addedRules.size();

                    if (generator.addedRules.size() > 5000) {
                        System.out.println(target);
                    }


                    if (!examinator.examine(target, source)) {
                        System.out.println(file.getAbsolutePath());
                        examinator.log = true;

                        CodeProcessor.recover(generator.addedRules, true).replaceAll("\\s", "");
                        System.out.println(method.toString().replaceAll("\n", ""));
                        System.out.println(target);
                        System.out.println(source);
                        System.out.println();

                        RuleGenerator generator1 = new RuleGenerator();
                        method.accept(generator1);
                        examinator.log = false;
                    } else {
                        //System.out.println("true");
                        //System.out.println(target);
                        //System.out.println(source);
                    }
                }
            } catch (Exception e) {
                System.out.println(file.getAbsoluteFile());
                e.printStackTrace();
            }
        }

        System.out.println(Generator.rules.size());
        List<Pair<Integer, Rule>> rules = new ArrayList<>();
        for (Rule rule: Generator.rules.keySet()) {
            rules.add(new Pair<>(Generator.rules.get(rule), rule));
        }

        Collections.sort(rules, (a, b)-> b.getKey() - a.getKey());

        BufferedWriter writer = new BufferedWriter(new FileWriter(new File("rule.txt")));


        for (int i = 0; i < rules.size(); i++) {
            writer.write(i + ": " + rules.get(i).getKey() + " " + rules.get(i).getValue().toString() + "\n");
        }

        System.out.println(ruleCount / methodCount);
    }

}
