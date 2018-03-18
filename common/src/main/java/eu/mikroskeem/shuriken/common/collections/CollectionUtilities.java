package eu.mikroskeem.shuriken.common.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Collection utilities
 *
 * @author Mark Vainomaa
 */
public final class CollectionUtilities {
    /**
     * Gets first item from iterator
     *
     * @param iterator Iterator
     * @param <T> Iterable type
     * @return First or null
     */
    @Nullable
    public static <T> T firstOrNull(@NotNull Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Gets first item from iterable, using {@link CollectionUtilities#firstOrNull(Iterator)}
     *
     * @param iterable Iterable
     * @param <T> Iterable type
     * @return First or null
     */
    @Nullable
    public static <T> T firstOrNull(@NotNull Iterable<T> iterable) {
        return firstOrNull(iterable.iterator());
    }

    /**
     * Gets first item from list
     *
     * @param list List
     * @param <T> List type
     * @return First or null
     */
    @Nullable
    public static <T> T firstOrNull(@NotNull List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Gets first item from set
     *
     * @param set Set
     * @param <T> Set type
     * @return First or null
     */
    @Nullable
    public static <T> T firstOrNull(@NotNull Set<T> set) {
        return set.isEmpty() ? null : firstOrNull(set.iterator());
    }
}
