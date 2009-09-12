package cache;

import java.util.logging.Logger;
import people.client.Misc;


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
	
	public V get(K key, double timeBoundSec) {
		++_cacheStats.numGets;
	
		V value = null; 
		try {
			value = _cacheActual.get(key, timeBoundSec);
		}
		catch (Exception e) {
			// Best effort response to an exception
			logger.warning("+Exception for person "+ key + ", " + this.identify() + ": " + e.getMessage() + "," + e.toString()); 
			Misc.reportException(e);
		}
		
		if (value == null) {
			++_cacheStats.numMisses;
			if (_nextStage != null) {
				value = _nextStage.get(key,  timeBoundSec);
				if (value != null) {
					_cacheActual.put(key, value); // Don't call this.put() !
				}
			}
		}
		return value;
	}
	
	public void put(K key, V value, double timeBoundSec) {
		++_cacheStats.numPuts;
		V val = _cacheActual.get(key, timeBoundSec);
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
