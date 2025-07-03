package brainwine.gameserver.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ProfanityManager {
    private static final char[] obscuredCharacters = "!@#$%&".toCharArray();

    private static final Logger logger = LogManager.getLogger();
    Corpus root = new Corpus();

    public ProfanityManager() {
        File file = new File("profanity.txt");
        if(!file.exists()) {
            logger.warn("No profanity filter has been enabled. Add profanity.txt to the working directory to enable it.");
            return;
        }

        try(Scanner sc = new Scanner(new FileInputStream(file))) {
            while(sc.hasNextLine()) {
                String line = sc.nextLine();
                if(!line.matches("\\s*") && !line.startsWith("##") && !line.startsWith("--")) {
                    String[] words = line.split(", ?");
                    for(String word : words) {
                        try {
                            root.addPhrase(word);
                        } catch(Exception e) {
                            logger.warn("Couldn't register profane phrase {}", word, e);
                        }
                    }
                }
            }
        } catch(IOException e) {
            logger.error("Couldn't load the profanity filter.", e);
        }
    }

    public String filter(String text) {
        if(root.isEmpty()) return text;

        try {
            String[] words = splitIntoTokens(text);
            for(int i = 0; i < words.length; i++) {
                int longest = root.findLongestMatch(words, i);
                for(int j = i; j < i + longest; j++) {
                    StringBuilder obscuredWord = new StringBuilder(words[j].substring(0, 1));
                    int lastChoice = (int) (Math.random() * obscuredCharacters.length);
                    for(int k = 1; k < words[j].length(); k++) {
                        int choice = (int) (Math.random() * obscuredCharacters.length);
                        obscuredWord.append(obscuredCharacters[choice == lastChoice ? (choice + 1) % obscuredCharacters.length : choice]);
                        lastChoice = choice;
                    }
                    words[j] = obscuredWord.toString();
                }
                if(longest > 1) i += longest - 1;
            }

            return String.join("", words);
        } catch(Exception e) {
            logger.error("Couldn't filter phrase {}", text, e);
            return text;
        }
    }

    private String[] splitIntoTokens(String text) {
        int lastMode = -1;
        StringBuilder sb = null;
        String[] chars = text.split("");
        List<String> tokens = new ArrayList<>();

        for(String aChar : chars) {
            int mode = aChar.matches("\\s") ? 0 : aChar.matches("[!\"#%&'()*+,\\n\\-./:;<=>?@\\[\\\\\\]^_`{|}~]") ? 1 : 2;
            if(lastMode != mode) {
                if(sb != null) tokens.add(sb.toString());
                sb = new StringBuilder();
            }
            sb.append(aChar);
            lastMode = mode;
        }

        if(sb != null && sb.length() > 0) {
            tokens.add(sb.toString());
        }

        return tokens.toArray(new String[0]);
    }
}
