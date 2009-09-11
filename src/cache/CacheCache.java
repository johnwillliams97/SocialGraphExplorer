package cache;
/*
 * Wrapper for javax.cache.Cache
 */
import java.util.Collections;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import people.client.Misc;
import datatypes.PersonDummy;


public class CacheCache implements CacheActual<Long, PersonDummy> {
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
	public PersonDummy get(Long key, double timeBoundSec) {
		PersonDummy person = (PersonDummy)theCache.get(key);
		if (person != null)
			person.setWhence("CacheCache");
		return person;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void put(Long key, PersonDummy value) {
		theCache.put(key, value);
	}

	@Override
	public String identify() {
		return CacheCache.class.getSimpleName();
	}

	

}

