package uk.co.inet.lexer;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.atn.PredictionMode;

import org.jsoup.*;
import org.jsoup.nodes.*;

import org.apache.commons.collections4.trie.PatriciaTrie;

//import org.apache.pdfbox.pdmodel.PDDocument;
//import org.apache.pdfbox.text.PDFTextStripper;

import java.net.*;
import java.util.*;
import java.util.stream.*;
import java.io.*;

public class Text
{
  public Map<String,Map<String,Short>> sets;
  public List<Word> wordList;
  public Map<String,Word> mapOfWords;
  public int wordCnt = 0;
  public int sentenceCnt = 0;

  public String toString()
  {
    return "{" + sets.toString() + " : " + wordList.toString() + " # " + wordCnt + " : " + sentenceCnt + "}";
  }

  public Text(Map<String,Map<String,Short>> sets, List<Word> wordList, Map<String,Word> mapOfWords, int wordCnt, int sentenceCnt)
  {
    this.sets = sets;
    this.wordList = wordList;
    this.mapOfWords = mapOfWords;
    this.wordCnt = wordCnt;
    this.sentenceCnt = sentenceCnt;
  }

  public static final Text normalise(String nm, String stream, Map<String,String[]> wordMap, Map<String,String[]> csWordMap, PatriciaTrie<Collection<String>> star, Map<String,Object> env)
  {
    if (! stream.contains(":"))
      stream = "file:" + stream;

    try
    {
      URL url = new URL(stream);
      char[] chars = null;

      boolean isHtml = url.getFile().endsWith(".html") || (url.getFile().endsWith("/") && url.getProtocol().startsWith("http"));
      //boolean isPDF = url.getFile().endsWith(".pdf");

      if (isHtml)
      {
        Document doc = null;
          
        if (url.getProtocol().startsWith("http"))
          doc = Jsoup.connect(stream).get();
        else
        {
          if (stream.startsWith("file:"))
            stream = stream.substring(5);

          doc = Jsoup.parse(new File(stream), "UTF-8");
        }

        StringBuilder sb = new StringBuilder(doc.text());

        for (Element link : doc.select("a[href]"))
        {
          String emUrl = link.attr("abs:href");

          if (! emUrl.isEmpty())
          {
            sb.append(" #@#");
            sb.append(emUrl);
          }
        }

        sb.append(" ");

        chars = sb.toString().toCharArray();

        sb = null;
      }
      /*
      else if (isPDF)
      {
        //try (PDDocument doc = PDDocument.load(stream.getBytes("UTF8")))
        try (PDDocument doc = PDDocument.load(stream.getBytes()))
        {
          PDFTextStripper ts = new PDFTextStripper();

          stream = ts.getText(doc).replaceAll("[ \\t \\n\\r]", " ");
        }
        catch (Exception e)
        {
          // Ignore - cannot convert
        }
      }
      */

      ANTLRInputStream ais = null;

      try
      {
        if (chars != null)
          ais = new ANTLRInputStream(chars, chars.length);
        else
          ais = new ANTLRInputStream(new BufferedReader(new InputStreamReader(url.openStream())));

        return normalise(nm, ais, wordMap, csWordMap, star, env);
      }
      catch (IOException e)
      {
        System.err.println(e);

        /* ANTLRInputStream self closing?
        if (ais != null)
        {
          try
          {
            ais.close();
          }
          catch (Exception ee)
          {
            System.err.println(ee);
          }
        }
        */
      }
    }
    catch (MalformedURLException mue)
    {
      System.err.println(mue);
    }
    catch (IOException ioe)
    {
      System.err.println(ioe);
    }

    return null;
  }

  private static final Text normalise(String nm, ANTLRInputStream input, Map<String,String[]> wordMap, Map<String,String[]> csWordMap, PatriciaTrie<Collection<String>> star, Map<String,Object> env)
  {
    Text text = null;

    try
    {
      TextLexer lexer = new TextLexer(input);

      lexer.setWordMap(wordMap, csWordMap);
      lexer.setTokenFactory(new CommonTokenFactory(true));

      TextParser parser = new TextParser(new UnbufferedTokenStream<CommonToken>(lexer));

      parser.setStar(star);
      parser.setWordMap(wordMap, csWordMap);
      parser.setEnv(env);
      parser.setBuildParseTree(false); // tell ANTLR to not build a parse tree
      parser.getInterpreter().setPredictionMode(PredictionMode.SLL); 
      parser.removeErrorListeners();

      parser.re();

      text = new Text(parser.getResults(), parser.wordList, parser.mapOfWords, parser.wordCnt, parser.sentenceCnt);
      //System.err.println(parser.wordList);
    }
    catch (Exception ex)
    {
      // Do something
    }

    return text;
  }
}
