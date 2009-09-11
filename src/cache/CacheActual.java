package cache;

public interface CacheActual<K, V extends CacheTrait> {
	public V       get(K key, double timeBoundSec); 
	public void    put(K key, V value);
	String         identify();
}
