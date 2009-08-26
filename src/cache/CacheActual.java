package cache;

public interface CacheActual<K,V> {
	public enum WebReadPolicy { AUTO, NEVER, ALWAYS };
//	public boolean containsKey(K key);
	public boolean isIncomplete(V value);
	public V       setWhence(V value);
	public V       get(K key, WebReadPolicy policy, long timeBoundMillis); 
	public void    put(K key, V value);
	String         identify();
}
