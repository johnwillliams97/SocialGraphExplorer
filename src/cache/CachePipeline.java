package cache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import cache.CacheActual.WebReadPolicy;


public class CachePipeline<K, V extends CacheTrait> {
	private static final Logger logger = Logger.getLogger(CachePipeline.class.getName());
	
	private Set<CacheStage<K,V>> pipeline = null;
	private CacheStage<K,V> firstStage = null;
		
	public  CachePipeline(List<CacheActual<K, V >> stagesActual) {
		setPipeline(stagesActual);
	}
	public void setPipeline(List<CacheActual<K, V>> stagesActual) {
		if (stagesActual != null && stagesActual.size() > 0) {
			pipeline = new LinkedHashSet<CacheStage<K,V>>();
			int sequence = 0;
			CacheStage<K,V> previous = null;
			for (CacheActual<K,V> cacheActual: stagesActual) {
				CacheStage<K,V> cacheStage = new CacheStage<K, V>(cacheActual, sequence);
				pipeline.add(cacheStage);
				if (previous != null) {
					previous.setNextStage(cacheStage);
				}
				else {
					firstStage = cacheStage;
				}
				previous = cacheStage;
				++sequence;
			}
		}
		if (pipeline != null) {
		//	firstStage = pipeline.iterator().next();
		}
		
	}
	public String identify() {
		String identity = "== CACHE PIPELINE ==\n";
		assert(pipeline != null);
		int stageNum = 0;
		CacheStage<K,V> stage = firstStage;
		while (stage != null) {
			identity += "stage " + stageNum + ": ";
			identity += stage.identify() + "\n";
			
			// Debug
			String cacheIdentity = stage.identify();
			if (stageNum == 0)
				assert(cacheIdentity.contains("CacheCache"));
			if (stageNum == 1)
				assert(cacheIdentity.contains("CacheDB"));
			//- Debug
			
			++stageNum;
			stage = stage.getNextStage();
		}
		return identity;
	}
	public String getStatus() {
		String status = "== CACHE STATUS ==\n";
		int stageNum = 0;
		CacheStage<K,V> stage = firstStage;
		while (stage != null) {
			status += "stage " + stageNum + ": ";
			status += stage.getStatus() + "\n";
			++stageNum;
			stage = stage.getNextStage();
		}
		return status;
	}
	
	public /*synchronized */ V get(K key, WebReadPolicy policy, double timeBoundSec) {
		V value = null;
		
		try {
			value = firstStage.get(key, policy, timeBoundSec);
		}
		catch (Exception e) {
			// Best effort response to an exception
			logger.warning("Exception for person " + key + ", cache pipeline: '" + e.getMessage() + "', " + e.toString()); 
			e.printStackTrace();
		}
		
		
	//	PersonLI person = (PersonLI)value;
	//	logger.info(key + ":" + (person != null ? person.getNameFull() : "not found"));
		//log.info(this.identify()); !@#$ Debugging
		return value;
	}
	public synchronized void put(K key, V value, double timeBoundSec) {
		firstStage.put(key, value,  timeBoundSec);
	}
	
}
