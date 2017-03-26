package uk.co.inet.lexer;

import java.net.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import org.apache.commons.collections4.trie.PatriciaTrie;

public class CommandLine
{
  public static void main(String... args)
  {
    String fn = System.getProperty("Lists", "global.prp");

    Map<String,Object> env = Utils.loadPrp(fn);
    PatriciaTrie<Collection<String>> trie = Utils.resolveRefs(env);

    for (String url : args)
    {
      Text t = Utils.getAll(url, env, trie);

      System.out.println(t);
    }
  }
}
