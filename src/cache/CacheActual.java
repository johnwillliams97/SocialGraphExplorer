package cache;

public interface CacheActual<K, V extends CacheTrait> {
	public enum WebReadPolicy { AUTO, NEVER, ALWAYS };
	public V       get(K key, WebReadPolicy policy, double timeBoundSec); 
	public void    put(K key, V value);
	String         identify();
}
