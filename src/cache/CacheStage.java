package cache;

import java.util.logging.Logger;

import people.client.Misc;

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
	CacheStats cacheStats = new CacheStats();
	private CacheActual<K,V> cacheActual = null;
	private CacheStage<K,V>  nextStage = null;
	
	private int sequence = -1;
	
	public CacheStage(CacheActual<K,V> cacheActual, int sequence) {
		this.cacheActual = cacheActual;
		this.sequence = sequence;
	}
	
	public V get(K key, WebReadPolicy policy, double timeBoundSec) {
		++cacheStats.numGets;
	
		V value = null; 
		try {
			value = cacheActual.get(key, policy, timeBoundSec);
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
			++cacheStats.numMisses;
			if (nextStage != null) {
				if (incomplete) {
					incomplete = true;
				}
				value = nextStage.get(key, policy,  timeBoundSec);
				if (value != null) {
					cacheActual.put(key, value); // Don't call this.put() !
				}
			}
		}
		
		// Debug code !@#$
		String cacheIdentity = cacheActual.identify();
		if (cacheIdentity.contains("CacheDB"))
			assert(nextStage == null);
		else
			assert(nextStage != null);
		//- Debug code
		
		return value;
	}
	
	public void put(K key, V value, double timeBoundSec) {
		++cacheStats.numPuts;
		V val = cacheActual.get(key, WebReadPolicy.AUTO,  timeBoundSec);
		if (!(val != null && val.equals(value))) {
			cacheActual.put(key, value);
			if (nextStage != null) {
				nextStage.put(key, value,  timeBoundSec);
			}
		}
	}
	public void setNextStage(CacheStage<K,V> nextStage) {
		this.nextStage = nextStage;
	}
	public CacheStage<K, V> getNextStage() {
		return nextStage;
	}
	public String identify() {
		String identity = "sequence=" + sequence + ", id=" + cacheActual.identify(); 
		return identity;
	}
	public String getStatus() {
		return cacheStats.getSummary();
	}
	
}
