package brainwine.gameserver.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.stream.Collectors;

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
            List<Token> words = splitIntoTokens(text);
            for(int i = 0; i < words.size(); i++) {
                int longest = root.findLongestMatch(words, i);
                int start = -1;
                int end = -1;
                for(int j = i; j < i + longest; j++) {
                    if(words.get(j).getType() == TokenType.WORD) {
                        if(start == -1) start = j;
                        end = j;
                    }
                }
                if(start == -1) continue;
                for(int j = start; j <= end; j++) {
                    StringBuilder obscuredWord = new StringBuilder(words.get(j).getValue().substring(0, 1));
                    int lastChoice = (int) (Math.random() * obscuredCharacters.length);
                    for(int k = 1; k < words.get(j).getValue().length(); k++) {
                        int choice = (int) (Math.random() * obscuredCharacters.length);
                        obscuredWord.append(obscuredCharacters[choice == lastChoice ? (choice + 1) % obscuredCharacters.length : choice]);
                        lastChoice = choice;
                    }
                    words.get(j).setValue(obscuredWord.toString());
                }
                if(longest > 1) i += longest - 1;
            }

            return words.stream().map(Token::getValue).collect(Collectors.joining(""));
        } catch(Exception e) {
            logger.error("Couldn't filter phrase {}", text, e);
            return text;
        }
    }

    private List<Token> splitIntoTokens(String text) {
        StringBuilder sb = null;
        String[] chars = text.split("");
        List<Token> tokens = new ArrayList<>();

        TokenType lastTokenType = TokenType.NONE;
        for(String aChar : chars) {
            TokenType tokenType = aChar.matches("\\s") ? TokenType.WHITESPACE : aChar.matches("[!\"#%&'()*+,\\n\\-./:;<=>?@\\[\\\\\\]^_`{|}~]") ? TokenType.PUNCTUATION : TokenType.WORD;
            if(lastTokenType != tokenType) {
                if(sb != null) {
                    tokens.add(new Token(sb.toString(), lastTokenType));
                }
                sb = new StringBuilder();
            }
            sb.append(aChar);
            lastTokenType = tokenType;
        }

        if(sb != null && sb.length() > 0) {
            tokens.add(new Token(sb.toString(), lastTokenType));
        }

        return tokens;
    }

    public boolean filterAll(Map<String, String> strings) {
        try {
            StringJoiner sj = new StringJoiner(" ");
            for(Map.Entry<String, String> entry : strings.entrySet()) {
                sj.add(entry.getValue());
            }
            String initial = sj.toString();
            String result = filter(initial);
            if(initial.equals(result)) {
                return false;
            }

            if(initial.length() != result.length()) {
                logger.warn("initial and result are {} and {} long respectively.", initial.length(), result.length());
            }

            int used = 0;
            for(Map.Entry<String, String> entry : strings.entrySet()) {
                int start = used;
                int end = Math.min(start + entry.getValue().length(), result.length());
                entry.setValue(start < end ? result.substring(start, end) : "");
                used = end + 1;
            }

            return true;
        } catch(Exception e) {
            logger.error("Failed to filter all!", e);
            return false;
        }
    }
}
