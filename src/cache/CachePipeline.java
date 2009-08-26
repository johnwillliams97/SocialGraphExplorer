package cache;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cache.CacheActual.WebReadPolicy;

public class CachePipeline<K,V> {
	
	private Set<CacheStage<K,V>> pipeline = null;
	private CacheStage<K,V> firstStage = null;
		
	public  CachePipeline(List<CacheActual<K, V>> stagesActual) {
		setPipeline(stagesActual);
	}
	public void setPipeline(List<CacheActual<K, V>> stagesActual) {
		if (stagesActual != null && stagesActual.size() > 0) {
			pipeline = new HashSet<CacheStage<K,V>>();
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
	
	public /*synchronized */ V get(K key, WebReadPolicy policy, long timeBoundMillis) {
		V value = firstStage.get(key, policy, timeBoundMillis);
		//log.info(this.identify()); !@#$ Debugging
		return value;
	}
	public synchronized void put(K key, V value, long timeBoundMillis) {
		firstStage.put(key, value,  timeBoundMillis);
	}
	
}
