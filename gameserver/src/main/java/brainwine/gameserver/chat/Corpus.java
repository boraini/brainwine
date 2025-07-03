package brainwine.gameserver.chat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Corpus {
    Map<String, Corpus> next = new HashMap<>();

    public void addPhrase(String item) throws IOException {
        try(Scanner sc = new Scanner(item)) {
            sc.useDelimiter("\\s+");
            addPhrase(sc);
        }
    }

    public void addPhrase(Scanner sc) throws IOException {
        if(sc.hasNext()) {
            String word = sc.next();
            Corpus nextCorpus = next.computeIfAbsent(word, s -> new Corpus());
            nextCorpus.addPhrase(sc);
        } else {
            next.put("", null);
        }
    }

    public int findLongestMatch(String[] words, int i) {
        return findLongestMatch(words, i, 0);
    }

    public int findLongestMatch(String[] words, int i, int currentCount) {
        while(i < words.length && words[i].isBlank()) {
            i++;
            currentCount++;
        }

        int myCount = next.containsKey("") ? currentCount : 0;

        if(i >= words.length) {
            return myCount;
        }

        String lower = words[i].toLowerCase();
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
