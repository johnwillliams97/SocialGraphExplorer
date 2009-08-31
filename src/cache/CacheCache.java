package cache;
/*
 * Wrapper for javax.cache.Cache
 */
import java.util.Collections;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;

import datatypes.PersonLI;


public class CacheCache implements CacheActual<Long, PersonLI> {
	private static final Logger logger = Logger.getLogger(CacheCache.class.getName());

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
			long uniqueID = person.getLiUniqueID();
			String nameFull = person.getNameFull();
			String compl = incomplete ? "INCOMPLETE" : "complete";
			if (incomplete)
				logger.warning(uniqueID + ":" + nameFull + " - " + compl);
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
	public PersonLI get(Long key, WebReadPolicy policy, double timeBoundSec) {
		PersonLI person = (PersonLI)theCache.get(key);
		//logger.info(key + ":" + (person != null ? person.getNameFull() : "not found"));
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

	@Override
	public PersonLI setWhence(PersonLI person) { 
		logger.info("setWhence("+this.identify()+") - " + (person != null ? person.getNameFull() : "not found"));
		if (person != null) {
			person.setWhence(this.identify());
		}
		return person;
	}

}

