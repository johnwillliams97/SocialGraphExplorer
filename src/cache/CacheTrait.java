package cache;

/*
 * Classes that inherit this interface will work with cache pipeline
 */

public interface CacheTrait {
	
	// Incomplete record - needs to be re-fetched from a deeper cache level
	public boolean isIncomplete();
	
	// Identifies the cache stage this object was fetched from
	public void    setWhence(String whence);
	public String  getWhence();
	
}
