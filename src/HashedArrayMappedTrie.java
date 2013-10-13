import java.util.*;

public class HashedArrayMappedTrie<K, V>
{
    public static class HashCollisionException extends RuntimeException
    {
    }

    private final List<Entry<K, V>> rootEntries;

    public HashedArrayMappedTrie()
    {
        List<Entry<K, V>> empty = new ArrayList<Entry<K, V>>(32);
        for (int i = 0; i < 32; i++)
            empty.add(new ZerotonEntry());

        this.rootEntries = empty;
    }

    private HashedArrayMappedTrie(List<Entry<K, V>> rootEntries)
    {
        this.rootEntries = rootEntries;
    }

    public V get(K key)
    {
        HashChunker hashIterator = new HashChunker(key.hashCode());
        int firstHashChunk = hashIterator.next();
        return rootEntries.get(firstHashChunk)
                .lookupRecursively(hashIterator);
    }

    public HashedArrayMappedTrie<K, V> put(K key, V value)
    {
        HashChunker hashIterator = new HashChunker(key.hashCode());
        int firstHashChunk = hashIterator.next();

        Entry<K, V> oldEntry = rootEntries.get(firstHashChunk);
        Entry<K, V> newEntry = oldEntry.putRecursively(hashIterator, value);

        return new HashedArrayMappedTrie<K, V>(
                CopyOnWrite.set(rootEntries, firstHashChunk, newEntry));
    }

    public int size()
    {
        int result = 0;
        for (Entry<K, V> entry : rootEntries)
            result += entry.size();
        return result;
    }

    public String dump()
    {
        Indenter indenter = new Indenter();

        StringBuilder result = new StringBuilder().append(indenter.get()).append("[\n");

        indenter.increment();
        for (int i = 0; i < rootEntries.size(); i++)
            result.append(indenter.get())
                    .append(String.format("%2x: ", i))
                    .append(rootEntries.get(i).dump(indenter))
                    .append("\n");
        indenter.decrement();

        result.append("]");

        return result.toString();
    }

    static class MultitonEntry<K, V> extends Entry<K, V>
    {
        private final boolean[] map;
        private final List<Entry<K, V>> entries;

        public MultitonEntry()
        {
            this(new boolean[32], new ArrayList<Entry<K, V>>(0));
        }

        private MultitonEntry(boolean[] map, List<Entry<K, V>> entries)
        {
            this.map = map;
            this.entries = entries;
        }

        public V lookupRecursively(HashChunker keyHash)
        {
            int hashChunk = keyHash.next();
            int entryIdx = hashToEntry(hashChunk);
            if (entryIdx < 0)
                return null;
            else
                return entries.get(entryIdx).lookupRecursively(keyHash);
        }

        public Entry<K, V> putRecursively(HashChunker keyHash, V value)
        {
            if (!keyHash.hasNext())
                throw new HashCollisionException();

            int hashChunk = keyHash.next();
            int entryIdx = hashToEntry(hashChunk);
            if (entryIdx < 0)
            {
                Entry<K, V> newEntry = new SingletonEntry<K, V>(keyHash.rest(), value);
                return new MultitonEntry<K, V>(
                        CopyOnWrite.set(map, hashChunk, true),
                        CopyOnWrite.insert(entries, 0, newEntry));
            }
            else
            {
                Entry<K, V> oldEntry = entries.get(entryIdx);
                Entry<K, V> newEntry = oldEntry.putRecursively(keyHash, value);
                return new MultitonEntry<K, V>(
                        map,
                        CopyOnWrite.set(entries, entryIdx, newEntry));
            }
        }

        private int hashToEntry(int hashChunk)
        {
            return map[hashChunk] ? countFlagsBelow(hashChunk, map) : -1;
        }

        public int size()
        {
            int result = 0;
            for (Entry<K, V> entry : entries)
                result += entry.size();
            return result;
        }

        public String dump(Indenter indenter)
        {
            StringBuilder result = new StringBuilder().append("\n");
            indenter.increment();
            result.append(indenter.get()).append("[\n");
            indenter.increment();
            for (int i = 0; i < map.length; i++)
            {
                if (map[i])
                {
                    result.append(indenter.get())
                            .append(String.format("%2x: ", i))
                            .append(entries.get(hashToEntry(i)).dump(indenter))
                            .append("\n");
                }
            }
            indenter.decrement();
            result.append(indenter.get()).append("]");
            indenter.decrement();
            return result.toString();
        }
    }

    static class SingletonEntry<K, V> extends Entry<K, V>
    {
        private final int hashCodeSuffix;
        private final V value;

        private SingletonEntry(int hashCodeSuffix, V value)
        {
            this.hashCodeSuffix = hashCodeSuffix;
            this.value = value;
        }

        public V lookupRecursively(HashChunker keyHash)
        {
            return hashCodeSuffix == keyHash.rest() ? value : null;
        }

        public Entry<K, V> putRecursively(HashChunker keyHash, V value)
        {
            if (keyHash.rest() == hashCodeSuffix)
                throw new HashCollisionException();

            return upgradeToMultiton().putRecursively(keyHash, value);
        }

        private Entry<K, V> upgradeToMultiton()
        {
            return new MultitonEntry<K, V>().putRecursively(new HashChunker(hashCodeSuffix), value);
        }

        public int size()
        {
            return 1;
        }

        public String dump(Indenter indenter)
        {
            return hashCodeSuffix + " => " + value.toString();
        }
    }

    static class ZerotonEntry<K, V> extends Entry<K, V>
    {
        public V lookupRecursively(HashChunker keyHash)
        {
            return null;
        }

        public Entry<K, V> putRecursively(HashChunker keyHash, V value)
        {
            return new SingletonEntry<K, V>(keyHash.rest(), value);
        }

        public int size()
        {
            return 0;
        }

        public String dump(Indenter indenter)
        {
            return "-";
        }
    }

    static abstract class Entry<K, V>
    {
        public abstract V lookupRecursively(HashChunker keyHash);

        public abstract Entry<K, V> putRecursively(HashChunker keyHash, V value);

        public abstract String dump(Indenter indenter);

        public abstract int size();
    }

    static class HashChunker
    {
        private static final int CHUNK_SIZE = 5;

        private int hashCode;
        private int bitsLeft = 32;

        public HashChunker(int hashCode)
        {
            this.hashCode = hashCode;
        }

        public boolean hasNext()
        {
            return bitsLeft >= CHUNK_SIZE;
        }

        public int next()
        {
            int result = hashCode & 0x1f;
            hashCode >>= CHUNK_SIZE;
            bitsLeft -= CHUNK_SIZE;
            return result;
        }

        public int rest()
        {
            return hashCode;
        }
    }

    static int countFlagsBelow(int index, boolean[] bitmap)
    {
        assert index < bitmap.length;

        int nFlagsOn = 0;
        for (int i = 0; i < index; i++)
            if (bitmap[i])
                nFlagsOn++;
        return nFlagsOn;
    }

    static class Indenter
    {
        private String indent = "";

        public void increment()
        {
            indent += "  ";
        }

        public String get()
        {
            return indent;
        }

        public void decrement()
        {
            indent = indent.substring(2);
        }
    }
}