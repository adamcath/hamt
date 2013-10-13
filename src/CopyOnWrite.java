import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CopyOnWrite
{
    public static <T> List<T> set(List<? extends T> list, int index, T newValue)
    {
        List<T> newList = new ArrayList<T>(list);
        newList.set(index, newValue);
        return newList;
    }

    public static <T> List<T> insert(List<? extends T> list, int index, T newValue)
    {
        List<T> newList = new ArrayList<T>(list.size() + 1);
        for (int i = 0; i < index; i++)
            newList.add(list.get(i));
        newList.add(newValue);
        for (int i = index; i < list.size(); i++)
            newList.add(list.get(i));
        return newList;
    }

    public static boolean[] set(boolean[] bitmask, int index, boolean newValue)
    {
        boolean[] newBitmask = Arrays.copyOf(bitmask, bitmask.length);
        newBitmask[index] = newValue;
        return newBitmask;
    }
}
