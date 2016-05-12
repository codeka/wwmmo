package au.com.codeka.warworlds.testing;

import java.util.ArrayList;
import java.util.Random;

import au.com.codeka.warworlds.server.ctrl.NameGenerator;

public class NameGeneratorTest {
    public static void main(String[] args) {
        ArrayList<String> files = new ArrayList<String>();
        for (String arg : args) {
            files.add(arg);
        }
        NameGenerator.loadVocabularies(files);

        NameGenerator generator = new NameGenerator();
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            System.out.println(generator.generate(rand));
        }
    }
}
