import org.junit.Test;
import sun.jvm.hotspot.utilities.Assert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class HashedArrayMappedTrieTest
{

    private static class MockString
    {
        private final String value;
        private final int hashCode;

        public MockString(String value, int hashCode)
        {
            this.value = value;
            this.hashCode = hashCode;
        }

        public String toString()
        {
            return value;
        }

        public int hashCode()
        {
            return hashCode;
        }
    }

    @Test
    public void happyCase() throws Exception
    {
        assertEquals(
                "bar",
                new HashedArrayMappedTrie<String, String>()
                        .put("foo", "bar")
                        .get("foo"));

        assertEquals(
                "bar",
                new HashedArrayMappedTrie<String, String>()
                        .put("foo", "bar")
                        .put("adam", "cath")
                        .get("foo"));

        assertEquals(
                "cath",
                new HashedArrayMappedTrie<String, String>()
                        .put("foo", "bar")
                        .put("adam", "cath")
                        .get("adam"));
    }

    @Test
    public void hashCodeCollision()
    {
        try
        {
            new HashedArrayMappedTrie<String, String>()
                    .put("foo", "bar")
                    .put("foo", "baz");
            fail("Should have thrown");
        } catch (HashedArrayMappedTrie.HashCollisionException e)
        {
            // Good, good
        }
    }

    @Test
    public void afterModificationOldVersionIsUnchanged()
    {
        HashedArrayMappedTrie<String, String> v1 = new HashedArrayMappedTrie<String, String>();
        String v1dump = v1.dump();

        HashedArrayMappedTrie<String, String> v2 = v1.put("adam", "cath");
        String v2dump = v2.dump();
        assertEquals(v1dump, v1.dump());
        assertEquals(0, v1.size());

        HashedArrayMappedTrie<String, String> v3 = v2.put("stephanie", "greer");
        assertEquals(v2dump, v2.dump());
        assertEquals(1, v2.size());
        assertEquals(v1dump, v1.dump());
        assertEquals(0, v1.size());
        assertEquals(2, v3.size());
    }

    @Test
    public void testCountFlagsBelow() throws Exception
    {

        class Fixture
        {
            void doTest(boolean[] bitmap, int index, int expected)
            {
                assertEquals(expected, HashedArrayMappedTrie.countFlagsBelow(index, bitmap));
            }
        }

        Fixture f = new Fixture();

        f.doTest(new boolean[]{true}, 0, 0);
        f.doTest(new boolean[]{false}, 0, 0);
        f.doTest(new boolean[]{true, true}, 1, 1);
        f.doTest(new boolean[]{false, true}, 1, 0);
        f.doTest(new boolean[]{false, false}, 1, 0);
        f.doTest(new boolean[]{true, true, false}, 2, 2);
    }
}
