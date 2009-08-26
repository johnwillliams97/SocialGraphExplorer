package cache;
/*
 * Wrapper for javax.cache.Cache
 */
import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import datatypes.PersonLI;


public class CacheCache implements CacheActual<Long, PersonLI> {

	private Cache theCache = null;
	
	public CacheCache() {
		theCache = null;
	    try {
	    	theCache = CacheManager.getInstance().getCacheFactory().createCache( Collections.emptyMap());
	    } catch (CacheException e) {
	    	e.printStackTrace();
	    }
	}
	
	@Override
	public boolean isIncomplete(PersonLI person) {
		boolean incomplete = false;
		if (person != null) {
			incomplete = person.getIsChildConnectionInProgress() || person.getHtmlPage() == null;// || true;  //!@#$
		}
		return incomplete;
	}
/*	
	@Override
	public boolean containsKey(Long key) {
		return theCache.containsKey(key);
	}
*/
	@Override
	public PersonLI get(Long key, WebReadPolicy policy, long timeBoundMillis) {
		return (PersonLI)theCache.get(key);
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

	@Override
	public PersonLI setWhence(PersonLI person) {
		if (person != null) {
			person.setWhence(this.identify());
		}
		return person;
	}

}

