package uk.co.inet.lexer;

import java.util.*;
import java.io.*;

public class References
{
  private static final Map<String, Set<String>> references = new HashMap<>();

  public static final boolean isReference(String name, String key)
  {
    if (! references.containsKey(name))
      loadReferences(name);

    return references.get(name).contains(key);
  }

  /* TODO
  public static final boolean isPrefix(String name, String key)
  {
    if (! references.containsKey(name))
      loadReferences(name);

    // A trie would be appropriate here
    return references.get(name).contains(key);
  }
  */

  public static final void loadReferences(String name)
  {
    Set<String> refs = new HashSet<>();

    try (BufferedReader r = new BufferedReader(new InputStreamReader(References.class.getClassLoader().getResourceAsStream(name + ".prp"))))
    {
      refs.add(r.readLine());
    }
    catch (Exception e)
    {
      System.err.println(e);
    }

    references.put(name, refs);
  }
}
