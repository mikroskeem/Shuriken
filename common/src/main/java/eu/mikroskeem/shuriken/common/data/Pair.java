package eu.mikroskeem.shuriken.common.data;

/**
 * Pair class to represent key-value pairs
 * @author Mark Vainomaa
 */
public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Construct a new pair
     *
     * @param key Pair key
     * @param value Pair value
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Gets pair key
     *
     * @return Pair key
     */
    public K getKey() {
        return this.key;
    }

    /**
     * Gets pair value
     *
     * @return Pair value
     */
    public V getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Pair)) return false;
        Pair other = (Pair) o;
        return key == null ? other.getKey() == null : key.equals(other.getKey()) &&
                value == null ? other.getValue() == null : value.equals(other.getValue());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + (key == null ? 43 : key.hashCode());
        result = result * 59 + (value == null ? 43 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "eu.mikroskeem.shuriken.common.data.Pair(key=" + this.getKey() + ", value=" + this.getValue() + ")";
    }
}
