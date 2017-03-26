package uk.co.inet.lexer;

public class CountMap<K,T> extends java.util.HashMap<K,T>
{
  public String toString()
  {
    //return keySet().toString();
    return entrySet().toString();
  }
}
