package uk.co.inet.lexer;

import static java.util.stream.Collectors.toList;

import java.util.List;

public class Word
{
  public String word;
  public int wordPos;
  public int sentencePos;
  public Word next;

  public Word(String word, int wordPos, int sentencePos)
  {
    this.word = word;
    this.wordPos = wordPos;
    this.sentencePos = sentencePos;
  }

  public String toString()
  {
    return word + ":" + wordPos;
    //return word;
  }
  
  public static List<String> asStringList(final List<Word> words) {
     return words.stream().map(Word::toString).collect(toList());
  }
}
