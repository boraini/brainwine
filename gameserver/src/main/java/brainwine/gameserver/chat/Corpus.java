package brainwine.gameserver.chat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Corpus {
    Map<String, Corpus> next = new HashMap<>();

    public void addPhrase(String item) {
        try(Scanner sc = new Scanner(item)) {
            sc.useDelimiter("\\s+");
            addPhrase(sc);
        }
    }

    public void addPhrase(Scanner sc) {
        if(sc.hasNext()) {
            String word = sc.next();
            Corpus nextCorpus = next.computeIfAbsent(word, s -> new Corpus());
            nextCorpus.addPhrase(sc);
        } else {
            next.put("", null);
        }
    }

    public int findLongestMatch(List<Token> words, int i) {
        return findLongestMatch(words, i, 0);
    }

    public int findLongestMatch(List<Token> words, int i, int currentCount) {
        while(i < words.size() && words.get(i).getType() != TokenType.WORD) {
            i++;
            currentCount++;
        }

        int myCount = next.containsKey("") ? currentCount : 0;

        if(i >= words.size()) {
            return myCount;
        }

        String lower = words.get(i).getValue().toLowerCase();
        if(next.containsKey(lower)) {
            Corpus nextCorpus = next.get(lower);

            int nextCount = nextCorpus == null ? 0 : nextCorpus.findLongestMatch(words, i + 1, currentCount + 1);

            return Math.max(myCount, nextCount);
        }

        return myCount;
    }

    public boolean isEmpty() {
        return next.isEmpty();
    }
}
