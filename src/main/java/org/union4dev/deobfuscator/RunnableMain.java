package org.union4dev.deobfuscator;

import org.union4dev.deobfuscator.configuration.Configuration;
import org.union4dev.deobfuscator.transformer.implement.QProtectCrasherTransformer;
import org.union4dev.deobfuscator.transformer.implement.QProtectFlowTransformer;
import org.union4dev.deobfuscator.transformer.implement.QProtectNumberTransformer;
import org.union4dev.deobfuscator.util.SecurityChecker;

public class RunnableMain {

    public static void main(String[] args) {
        final Configuration configuration = new Configuration();
        configuration.setSecurityChecker(new SecurityChecker(true, true, true, true, true, true, false, true, true, true, true, true, true));
        configuration.setInput("D:\\deobf\\Test.jar");
        configuration.setOutput("D:\\deobf\\Test-deobf.jar");
        configuration.addClasspath("C:\\Program Files\\Java\\jre1.8.0_361\\lib\\rt.jar");
        configuration.addClasspath("C:\\Program Files\\Java\\jre1.8.0_361\\lib\\jce.jar");
        configuration.addTransformer(new QProtectCrasherTransformer(), new QProtectFlowTransformer(), new QProtectNumberTransformer());
        Deobfuscator.INSTANCE.run(configuration);
    }
}
