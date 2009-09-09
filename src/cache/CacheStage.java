package cache;

import java.util.logging.Logger;

import people.client.Misc;
import people.client.OurConfiguration;

import cache.CacheActual.WebReadPolicy;


public class CacheStage<K, V extends CacheTrait> {
	private static final Logger logger = Logger.getLogger(CacheStage.class.getName());
	
	public class CacheStats {
		int numPuts = 0;
		int numGets = 0;
		int numMisses = 0;
		int getNumHits() { return numGets - numMisses; }
		double getHitRate() { return numGets > 0 ? (double)getNumHits()/(double)numGets : 0.0; }
		String getSummary() {
			String summary = "";
			summary += "gets = " + numGets + ", ";
			summary += "(" + getNumHits() + " hits + " + numMisses + " misses), ";
			summary += "hit rate = " + (int)Math.round(getHitRate() *1000.0)/10.0 + "%, ";
			summary += "puts = " + numPuts + ". ";
			return summary;
		}
	}
	private CacheStats       _cacheStats = new CacheStats();
	private CacheActual<K,V> _cacheActual = null;
	private CacheStage<K,V>  _nextStage = null;
	
	private int _sequence = -1;
	
	public CacheStage(CacheActual<K,V> cacheActual, int sequence) {
		_cacheActual = cacheActual;
		_sequence = sequence;
	}
	
	public V get(K key, WebReadPolicy policy, double timeBoundSec) {
		++_cacheStats.numGets;
	
		V value = null; 
		try {
			value = _cacheActual.get(key, policy, timeBoundSec);
		//	if (value != null /*&& value.getWhence() == null*/)
		//		value.setWhence(cacheActual.identify());
		}
		catch (Exception e) {
			// Best effort response to an exception
			logger.warning("+Exception for person "+ key + ", " + this.identify() + ": " + e.getMessage() + "," + e.toString()); 
			Misc.reportException(e);
		}
		
		boolean incomplete = (value != null && value.isIncomplete());
		
		if (value == null || policy == WebReadPolicy.ALWAYS || incomplete) {
			++_cacheStats.numMisses;
			if (_nextStage != null) {
				if (incomplete) {
					incomplete = true;
				}
				value = _nextStage.get(key, policy,  timeBoundSec);
				if (value != null) {
					_cacheActual.put(key, value); // Don't call this.put() !
				}
			}
		}
		
		// Debug code !@#$
		String cacheIdentity = _cacheActual.identify();
		if (cacheIdentity.contains("CacheDB"))
			assert(_nextStage == null);
		else
			assert(_nextStage != null);
		//- Debug code
		
		return value;
	}
	
	public void put(K key, V value, double timeBoundSec) {
		++_cacheStats.numPuts;
		final WebReadPolicy webReadPolicy = OurConfiguration.ALLOW_LINKED_READS ? WebReadPolicy.AUTO : WebReadPolicy.NEVER;
		V val = _cacheActual.get(key, webReadPolicy, timeBoundSec);
		if (!(val != null && val.equals(value))) {
			_cacheActual.put(key, value);
			if (_nextStage != null) {
				_nextStage.put(key, value,  timeBoundSec);
			}
		}
	}
	public void setNextStage(CacheStage<K,V> nextStage) {
		_nextStage = nextStage;
	}
	public CacheStage<K, V> getNextStage() {
		return _nextStage;
	}
	public String identify() {
		String identity = "sequence=" + _sequence + ", id=" + _cacheActual.identify(); 
		return identity;
	}
	public String getStatus() {
		return _cacheStats.getSummary();
	}
	
}
