package people.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import cache.CachePipeline;
import cache.CacheActual.WebReadPolicy;
import datatypes.PersonLI;
import people.client.PersonClient;
import people.client.PersonTrait;
import people.client.Statistics;


class CachePipelineInstance {
	private static final Logger logger = Logger.getLogger(CachePipelineInstance.class.getName());
		
	private int _numFetches = 0;
	private int _numMemCacheFetches = 0;
	private int _numDBCacheFetches = 0;
	private int _numWTFCacheFetches = 0;
	private CachePipeline<Long, PersonLI> _cachePipeline;
	private int _numCacheMisses = 0;;
	public CachePipelineInstance(CachePipeline<Long, PersonLI> cachePipeline) {
		_numFetches = 0;
		_cachePipeline = cachePipeline;
	}
	public PersonLI get(long idIn, WebReadPolicy policy, double timeBoundSec) {
		double start = Statistics.getCurrentTime();
		
		long id = mapUnknownID(idIn);
		PersonLI person = _cachePipeline.get(id, policy, timeBoundSec);
		cleanPersonConnections(person);
		++_numFetches;
		String whence = "none";
					
		if (person != null) {
			double end = Statistics.getCurrentTime();
			person.setFetchDuration(Statistics.round3(end-start));
			whence = person.getWhence();
			if (whence != null) {
				if (whence.contains("CacheDB"))
					++_numDBCacheFetches;
				else if (whence.contains("CacheCache"))
					++_numMemCacheFetches;
				else {
					++_numWTFCacheFetches;
				}
			}
		}
		else {
			++_numCacheMisses ;
		}
		
		return person;
	}
	// !@#$ Generalise this for PersistentPersonTrait.
	private long mapUnknownID(long id) {
		if (id == PersonTrait.GET_DEFAULT_PERSON_UNIQUEID)
			id = PersonLI.DEFAULT_PERSON_RECORD_UNIQUEID;
		return id;
	}
	int getNumFetches() {
		return _numFetches;
	}
	int getNumMemCacheFetches() {
		return _numMemCacheFetches;
	}
	int getNumDBCacheFetches() {
		return _numDBCacheFetches;
	}
	
	static private void cleanPersonConnections(PersonLI person) {
		if (person != null) {
			List<Long> connectionIDs = person.getConnectionIDs();
			if (connectionIDs != null) {
				Long id = person.getUniqueID();
				boolean removed = false;
				int  numRemoved = 0;
				while (true) {
					removed = connectionIDs.remove(id);
					if (!removed)
						break;
					++numRemoved;
				}
				if (numRemoved > 0) {// !@#$ fix data in database
					//logger.warning(person.getNameFull() + " [" + person.getUniqueID() + "] " 	+ numRemoved + " connections are unique ID");
				}
				connectionIDs = removeDuplicates(connectionIDs); // !@#$ fix data in database
				person.setConnectionIDs(connectionIDs);
			}
			PersonClient.debugValidate(person);
		}
	}
	
	private static List<Long> removeDuplicates(List<Long> list) {
	    Set<Long>  noDupsSet = new LinkedHashSet<Long>(list);
	    List<Long> noDupsList = new ArrayList<Long>(noDupsSet);
	    if (list.size() != noDupsSet.size())
	    	logger.warning("Removed " + (list.size() - noDupsSet.size()) + " duplicates");
	    return noDupsList;
	}
}
