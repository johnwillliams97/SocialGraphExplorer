package cache;

/*
 * Classes that inherit this interface will work with cache pipeline
 */

public interface CacheTrait {
	
	// Identifies the cache stage this object was fetched from
	public void    setWhence(String whence);
	public String  getWhence();
	
}
