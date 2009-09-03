package cache;
/*
 * Wrapper for javax.cache.Cache
 */
import java.util.Collections;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import people.client.Misc;
import datatypes.PersonLI;


public class CacheCache implements CacheActual<Long, PersonLI> {
	//private static final Logger logger = Logger.getLogger(CacheCache.class.getName());

	private Cache theCache = null;
	
	public CacheCache() {
		theCache = null;
	    try {
	    	theCache = CacheManager.getInstance().getCacheFactory().createCache( Collections.emptyMap());
	    } catch (CacheException e) {
	    	Misc.reportException(e);
	    }
	}
	
	
	@Override
	public PersonLI get(Long key, WebReadPolicy policy, double timeBoundSec) {
		PersonLI person = (PersonLI)theCache.get(key);
		return person;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void put(Long key, PersonLI value) {
		theCache.put(key, value);
	}

	@Override
	public String identify() {
		return CacheCache.class.getSimpleName();
	}

	

}

