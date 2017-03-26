package uk.co.inet.lexer;

import java.net.*;
import java.util.*;
import java.util.stream.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import org.apache.commons.collections4.trie.PatriciaTrie;

public class Utils
{
  private static class Parts
  {
    Map<String,String[]> globalTs = new HashMap<>();
    Map<String,String[]> globalFTs = new HashMap<>();
    Set<String> globalListSet = new HashSet<>();
    Map<String,Object> notLists = new HashMap<>();
    boolean globalTsEmpty = true;

    boolean getEmpty()
    {
      return globalTsEmpty;
    }

    void setEmpty(boolean empty)
    {
      this.globalTsEmpty = empty;
    }
  }

  private static final Map<String,Parts> globalLists = new ConcurrentHashMap<>();
  private final static void addToCombine(final Map<String,Object> preds, String k, String s, final Map<String,String[]> ts, final Map<String,String[]> fts, Map<String,Object> nl)
  {
    if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))
    {
      if (s.startsWith("\"") && s.endsWith("\""))
        System.err.println("String should not be quoted! - " + s);

      s = s.substring(1, s.length() - 1);
    }

    if (s.startsWith("~"))
    {
      s = s.substring(1);
      k = "Not" + k;

      Set<Object> nss = (Set<Object>) nl.get(k);

      if (nss == null)
      {
        nss = new HashSet<>();

        nl.put(k, nss);
      }

      nss.add(s);
    }

    String lcs = s.toLowerCase();

    if (ts.containsKey(lcs))
    {
      String[] kalc = ts.get(lcs);
      int l = kalc.length;

      kalc = Arrays.copyOf(kalc, l + 1);

      kalc[l] = k;

      ts.put(lcs, kalc);

      if (ts.containsKey(s))
      {
        String[] ka = ts.get(s);

        ka = Arrays.copyOf(ka, l + 1);

        ka[l] = k;

        ts.put(s, ka);
        fts.put(s, ka);
      }
      else
      {
        String[] ka = new String[] { k };

        ts.put(s, ka);
        fts.put(s, ka);
      }
    }
    else
    {
      String[] ka = new String[] { k };

      ts.put(s, ka);
      fts.put(s, ka);
      ts.put(lcs, ka);
    }
  }

  @SuppressWarnings("unchecked")
  private final static Parts combine(String nm, final Map<String,Object> preds)
  {
    synchronized (nm.intern())
    {
      Parts p = globalLists.get(nm);

      if (p != null && p.getEmpty())
      {
        Map<String,Object> newpreds = new HashMap<>();
        Map<String,List<String>> foreign = null;

        Map<String,String[]> ts = p.globalTs;
        Map<String,String[]> fts = p.globalFTs;
        Set<String> listSet = p.globalListSet;
        Map<String,Object> nl = p.notLists;
        Object trans = preds.get("Translation");

        // Lookup all foreign words and make an English to foreign map
        // for later lookup and to add to both individual maps and global map
        if (trans != null)
        {
          if (trans instanceof Collection)
          {
            Map<String,Object> newTrans = new TreeMap<>();

            for (Object e : (Collection<Object>) trans)
            {
              String l = e.toString();

              if ((l.startsWith("'") && l.startsWith("'")) || (l.startsWith("\"") && l.startsWith("\"")))
                l = l.substring(1, l.length() - 1);

              Object v = preds.get(l);

              if (v != null)
              {
                if (v instanceof Map)
                  newTrans.putAll((Map<String,Object>) v);
              }
            }

            trans = newTrans;
          }
        }
        else // Implicit lists of translations?
        {
          Map<String,Object> newTrans = new TreeMap<>();

          for (String k : preds.keySet())
          {
            if (k.contains("Translation"))
            {
              Object v = preds.get(k);

              if (v != null)
              {
                if (v instanceof Map)
                  newTrans.putAll((Map<String,Object>) v);
              }
            }
          }

          trans = newTrans;
        }

        if (trans != null)
        {
          foreign = new HashMap<>();

          for (Map.Entry<String,Object> e : ((Map<String,Object>) trans).entrySet())
          {
            String v = (String) e.getValue();
            List<String> val;

            if (foreign.containsKey(v))
              val = foreign.get(v);
            else
              foreign.put(v, val = new ArrayList<>());

            val.add(e.getKey());
          }
        }

        for (Map.Entry<String,Object> e : preds.entrySet())
        {
          String k = e.getKey();

          if (! listSet.contains(k) && Character.isUpperCase(k.charAt(0)))
          {
            Object v = e.getValue();

            if (v != null)
            {
              if (v instanceof Collection)
              {
                Collection<Object> lv = (Collection<Object>) v;
                Collection<Object> fc = null;

                for (Object vv : lv)
                {
                  String lk = vv.toString();

                  addToCombine(newpreds, k, lk, ts, fts, nl);

                  if (foreign != null && foreign.containsKey(lk))
                  {
                    if (fc == null)
                      fc = new ArrayList<>();

                    List<String> fkv = foreign.get(lk);

                    for (String fkvi : fkv)
                      addToCombine(newpreds, k, fkvi, ts, fts, nl);

                    fc.addAll(fkv);
                  }
                }

                if (fc != null)
                {
                  lv.addAll(fc);
                }
              }
              else if (v instanceof Object[])
              {
                Object[] lv = (Object[]) v;
                Collection<Object> fc = null;

                for (Object vv :  lv)
                {
                  String lk = vv.toString();

                  addToCombine(newpreds, k, lk, ts, fts, nl);

                  if (foreign != null && foreign.containsKey(lk))
                  {
                    if (fc == null)
                      fc = new ArrayList<>();

                    List<String> fkv = foreign.get(lk);

                    for (String fkvi : fkv)
                      addToCombine(newpreds, k, fkvi, ts, fts, nl);

                    fc.addAll(fkv);
                  }
                }

                if (fc != null)
                {
                  fc.addAll(Arrays.asList(lv));

                  preds.put(k, fc.toArray());
                }
              }
              else  // ???
              {
                addToCombine(newpreds, k, v.toString(), ts, fts, nl);
              }
            }

            listSet.add(k);
          }
        }

        p.setEmpty(false);

        preds.putAll(newpreds);
      }

      if (p.notLists != null)
        preds.putAll(p.notLists);

      return p;
    }
  }

  public final static Map<String,Object> loadPrp(String fn)
  {
    Properties prps = new Properties();

    try (Reader r = new BufferedReader(new InputStreamReader(Text.class.getClassLoader().getResourceAsStream(fn))))
    {
      prps.load(r);
    }
    catch (NullPointerException ex)
    {
      // File does not exist, this is okay
    }
    catch (FileNotFoundException ex)
    {
      System.err.println(fn + " not found");
    }
    catch (IOException ex)
    {
      System.err.println(fn + " will not load");
    }
    catch(Exception npe)
    {
    	npe.printStackTrace();
      System.err.println(fn + " was not found");
    }

    Map<String,Object> map = new TreeMap<>();

    for (Map.Entry<Object,Object> e : prps.entrySet())
    {
      String k = e.getKey().toString();
      String v = e.getValue().toString().trim();

      if (v.startsWith("'") || v.startsWith("\""))
        v = v.substring(1);
      if (v.endsWith("'") || v.endsWith("\""))
        v = v.substring(0, v.length() - 1);

      String[] vs = v.split("['\"]?\\s*,\\s*['\"]?");
      Object value = null;

      if (vs.length > 0)
      {
        if (vs[0].contains("="))
        {
          Map<String, String> al = new TreeMap<>();

          for (String i : vs)
          {
            String[] it = i.split("['\"]?\\s*=\\s*['\"]?");

            if (it.length == 2)
              al.put(it[0], it[1]);
          }

          value = al;
        }
        else
        {
          value = new ArrayList<String>(Arrays.asList(vs));
        }

        map.put(k, value);
      }
    }

    return map;
  }

  public static PatriciaTrie<Collection<String>> resolveRefs(Map<String,Object> pars)
  {
    PatriciaTrie<Collection<String>> trie = new PatriciaTrie<>();
    boolean allFound = true;

    do
    {
      try
      {
        for (Map.Entry<String, Object> k : pars.entrySet())
        {
          if (! resolveRefs(k.getKey(), k.getValue(), pars, trie))
            allFound = false;
        }
      }
      catch (ConcurrentModificationException chme)
      {
        // Nasty evil hack. Ignore and try again!
        // Though justifiable as should not happen if lists pop. once only
        continue;
      }
    }
    while (! allFound);

    return trie;
  }

  private static final boolean resolveRefs(String key, Object val, Map<String,Object> pars, PatriciaTrie<Collection<String>> trie)
  {
    boolean allFound = true;

    if (val instanceof Map)
      val = ((Map) val).keySet();

    if (val instanceof Collection)
    {
      for (Object o : new ArrayList((Collection) val))
      {
        if (o instanceof String)
        {
          String ref = (String) o;

          if (ref.startsWith("&"))
          {
            if (pars.containsKey(ref.substring(1)))
              resolveRefs(key, ref, val, pars, trie);
            else
              allFound = false;
          }
          else if (ref.endsWith("*"))
          {
            String v = ref.substring(0, ref.length() - 1);

            Collection<String> lists = trie.get(v);

            if (lists == null)
            {
              lists = new ArrayList<>();

              trie.put(v, lists);
            }

            if (! lists.add(key))
              lists.contains(key);
          }
        }
      }
    }

    return allFound;
  }

  private static void resolveRefs(String key, String ref, Object val, Map<String,Object> pars, PatriciaTrie<Collection<String>> trie)
  {
    String ref2 = ref.substring(1);

    if (pars.containsKey(ref2))
    {
      Collection cVal = (Collection) val;

      Object r = pars.get(ref2);

      if (r instanceof Collection)
        cVal.addAll((Collection) r);
      else if (r instanceof Map)
        cVal.addAll(((Map) r).keySet());
      else
        cVal.add(r);

      cVal.remove(ref);
    }
  }

  public static final Text getAll(String url, Map<String,Object> env, PatriciaTrie<Collection<String>> trie)
  {
    String nm = url;

    if (nm != null && ! globalLists.containsKey(nm))
      globalLists.put(nm, new Parts());

    Parts p = combine(nm, env);

    return Text.normalise(nm, url, p.globalTs, p.globalFTs, trie, env);
  }
}
