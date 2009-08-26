package people.server;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;
import com.google.apphosting.api.DeadlineExceededException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cache.CacheActual;
import cache.CacheCache;
import cache.CacheDB;
import cache.CachePipeline;
import cache.CacheActual.WebReadPolicy;
import datatypes.PersonLI;
import people.client.OurConfiguration;
import people.client.PersonFetch;
import people.client.PersonClient;
import people.client.PersonClientGroup;
import misc.Statistics;
import people.client.PersonService;


/*
 * The implementation of the RPC service which runs on the server.
 */
@SuppressWarnings("serial")
public class PersonServiceImpl extends RemoteServiceServlet implements PersonService {
	private static  final long maxTime = OurConfiguration.MAX_TIME_FOR_SERVLET_RESPONSE; 
	private static final Logger logger = Logger.getLogger(PersonServiceImpl.class.getName());
	private final long servletLoadTime = Calendar.getInstance().getTimeInMillis();
	private long firstCallTime = -1L;

	private class Fetch2 {
		public int level;					// client cache level requested
		public long requestedUniqueID;		// person requested
		public PersonLI person;				// persons returned
	}
	/*
	 * Tiny class for capturing cache pipeline stats
	 */
	class CachePipelineInstance {
		private int numFetches = 0;
		private int numMemCacheFetches = 0;
		private int numDBCacheFetches = 0;
		private int numWTFCacheFetches = 0;
		private CachePipeline<Long, PersonLI> cachePipeline;
		private int numCacheMisses = 0;;
		public CachePipelineInstance(CachePipeline<Long, PersonLI> cachePipeline) {
			numFetches = 0;
			this.cachePipeline = cachePipeline;
		}
		public PersonLI get(long id, WebReadPolicy policy, long timeBoundMillis) {
			PersonLI person = this.cachePipeline.get(id, policy, timeBoundMillis);
			++this.numFetches;
			if (person != null) {
				String whence = person.getWhence();
				if (whence != null) {
					if (whence.contains("CacheDB"))
						++this.numDBCacheFetches;
					else if (whence.contains("CacheCache"))
						++this.numMemCacheFetches;
					else {
						++this.numWTFCacheFetches;
					}
				}
			}
			else {
				++this.numCacheMisses ;
			}
			return person;
		}
		int getNumFetches() {
			return this.numFetches;
		}
		int getNumMemCacheFetches() {
			return this.numMemCacheFetches;
		}
		int getNumDBCacheFetches() {
			return this.numDBCacheFetches;
		}
	}
//	 private static final PersonLIClientGroup NO_PEOPLE = new PersonLIClientGroup();
	 private CachePipeline<Long, PersonLI> cachePipeline = null;
			 
	 public static CachePipeline<Long, PersonLI> makeCachePipeline() {
		List<CacheActual<Long,PersonLI>> stagesActual = new ArrayList<CacheActual<Long,PersonLI>>();
		CacheCache cacheCache = new CacheCache();
		stagesActual.add(cacheCache);
		CacheDB cacheDB = new CacheDB();
		stagesActual.add(cacheDB);
		CachePipeline<Long, PersonLI> cachePipeline = new CachePipeline<Long, PersonLI>(stagesActual);
		cachePipeline.identify();
		return cachePipeline;
	}
  
	public PersonServiceImpl() {
		assert(cachePipeline == null);
		cachePipeline = makeCachePipeline();
	}
	
	
	
		
	private static PersonClient personServerToPersonClient(PersonLI ps, long requestedID) {
		PersonClient pc = null;
		if (ps != null) {
			pc = new PersonClient();
			pc.setLiUniqueID(ps.getLiUniqueID());
			pc.setRequestedID(requestedID);
			pc.setNameFull(ps.getNameFull());
			pc.setDescription(ps.getDescription());
			pc.setLocation(ps.getLocation());
			pc.setEmployer(ps.getEmployer());
			pc.setConnectionIDs(ps.getConnectionIDs());
			pc.setIsChildConnectionInProgress(ps.getIsChildConnectionInProgress());
			pc.setWhence(ps.getWhence());
			pc.setHtmlPage(ps.getHtmlPage());
		}
		return pc;
	}
	
	
	private void cleanPersonConnections(PersonLI person) {
		List<Long> connectionIDs = person.getConnectionIDs();
		if (connectionIDs != null) {
			Long id = person.getLiUniqueID();
			connectionIDs.remove(id);
			person.setConnectionIDs(connectionIDs);
		}
	}

  /*
   * (non-Javadoc)
   * @see people.client.RPCWrapper#getPeople(long[], int[], long, long)
   * 
   *
    * @param requestedUniqueIDs - IDs of persons to fetch
	 * @param levels - client cache levels of the requestedUniqueIDs
	 * @param clientSequenceNumber - Number of high-level requests from this client
	 * @param sequenceNumber - tracks client requests
	 * @return list of persons fetched from the data store
   */
	@Override
	public PersonClientGroup getPeople(long[] requestedUniqueIDs, int[] levels, 
			long clientSequenceNumber, int numCallsForThisClientSequenceNumber, long sequenceNumber) {
		PersonClientGroup resultsMain = new PersonClientGroup();
		resultsMain.hadDeadlineExceededException = true;
		try {
			resultsMain = getPeople_(requestedUniqueIDs, levels, clientSequenceNumber, numCallsForThisClientSequenceNumber, sequenceNumber);
		}
		catch (DeadlineExceededException e) {
			logger.warning("DeadlineExceededException");
			resultsMain.hadDeadlineExceededException = true;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return resultsMain;
	}
	/*
	 * @param	requestedUniqueIDs - IDs of persons to fetch
	 * @param	levels - client cache levels of the requestedUniqueIDs
	 * @param sequenceNumber - tracks client requests
	 * @return list of persons fetched from the data store
	 */
	private PersonClientGroup getPeople_(long[] requestedUniqueIDs, 
										   int[]  requestedLevels,
										   long   clientSequenceNumber, 
										   int    numCallsForThisClientSequenceNumber,
										   long   sequenceNumber) {
	
		Statistics.createInstance("getPeople_");
		CachePipelineInstance cachePipelineInstance = new CachePipelineInstance(this.cachePipeline);
		
		PersonClientGroup resultsMain = new PersonClientGroup();
		resultsMain.requestedUniqueIDs = requestedUniqueIDs;
		resultsMain.requestedLevels = requestedLevels;
		List<Fetch2> fetchList = new ArrayList<Fetch2>();
		
		final long maxTime1 = maxTime / 2;
//		final long maxTime2 = (maxTime*3)/4;
		long start = Calendar.getInstance().getTimeInMillis();
		long end = 0L;
		
		if (this.firstCallTime < 0L)
			this.firstCallTime  = start;
	
		//First look at the people requested
		for (int i = 0; i < requestedUniqueIDs.length; ++i) {
			end = Calendar.getInstance().getTimeInMillis();
			if (end - start > maxTime1)
				break;
			long liUniqueID = requestedUniqueIDs[i];
			if (liUniqueID <= 0L)
				liUniqueID = PersonLI.DEFAULT_LI_UNIQUEID;
			PersonLI person = cachePipelineInstance.get(liUniqueID, WebReadPolicy.AUTO, start + maxTime1);
			if (person != null) {
				cleanPersonConnections(person);
				end = Calendar.getInstance().getTimeInMillis();
				Fetch2 fetch = new Fetch2();
				fetch.person = person;
				fetch.requestedUniqueID = requestedUniqueIDs[i];
				fetch.level = requestedLevels[i];
				fetchList.add(fetch);
			}
		}
		Statistics.getInstance().recordEvent("responseDuration1");
		resultsMain.responseDuration1 = Statistics.getInstance().getLastTime();

		Statistics.getInstance().recordEvent("responseDuration2");
		resultsMain.responseDuration2 = Statistics.getInstance().getLastTime();
		// Convert running list to output result
		if (fetchList.size() > 0) { 
			showPersonList(fetchList, "start");
			Statistics.getInstance().recordEvent("responseDuration3");
			resultsMain.responseDuration3 = Statistics.getInstance().getLastTime();
			PersonFetch[] fetches = new PersonFetch[fetchList.size()];
			for (int i = 0; i < fetches.length; ++i) {
				Fetch2 fetch2 = fetchList.get(i);
				PersonFetch fetch = new PersonFetch();
				fetch.level = fetch2.level;
				fetch.requestedUniqueID = fetch2.requestedUniqueID;
				fetch.person = personServerToPersonClient(fetch2.person, fetch2.requestedUniqueID);
				fetches[i] = fetch;										
			}
			resultsMain.fetches = fetches; //getUniqueNonNullEntries(results, 1000);
		}
		
		for (int i = 0; i < resultsMain.fetches.length; ++i) {
			List<Long> connectionIDs = resultsMain.fetches[i].person.getConnectionIDs();
			int numConnections = (connectionIDs != null) ? connectionIDs.size() : 0;
			logger.warning("\t" + i + ": " 
					+ resultsMain.fetches[i].person.getNameFull() +  " - " 
					+ resultsMain.fetches[i].person.getLiUniqueID()  +  " - " 
					+ numConnections);
		}
		resultsMain.servletLoadTime = this.servletLoadTime + this.firstCallTime; // This is the most unique signature I can synthesize
		resultsMain.clientSequenceNumber = clientSequenceNumber;
		resultsMain.numCallsForThisClientSequenceNumber = numCallsForThisClientSequenceNumber;
		resultsMain.sequenceNumber = sequenceNumber;
		
		Statistics.getInstance().recordEvent("responseDuration");
		resultsMain.responseDuration = Statistics.getInstance().getLastTime();
		logger.warning("Servlet load time = " + resultsMain.servletLoadTime);
		
		Statistics.getInstance().showAllEvents();	
		
		resultsMain.numCacheFetches = cachePipelineInstance.getNumFetches();
		resultsMain.numMemCacheFetches = cachePipelineInstance.getNumMemCacheFetches();
		resultsMain.numDBCacheFetches = cachePipelineInstance.getNumDBCacheFetches();
		logger.warning("Total  cache fetches =     " + resultsMain.numCacheFetches);
		logger.warning("Number mem cache fetches = " + resultsMain.numMemCacheFetches);
		logger.warning("Number DB cache fetches =  " + resultsMain.numDBCacheFetches);
		
		logger.warning("==========================");
		return resultsMain;
	}

	private static void showPersonList(List<Fetch2> fetchList, String name) {
		logger.warning("------ " + name + " : " + fetchList.size());
		for (int i = 0; i < fetchList.size(); ++i) {
			PersonLI person = fetchList.get(i).person;
			logger.warning("  " + i + ": " 
					+ person.getNameFull() + ", " 
					+ person.getLocation() + ", " 
					+ person.getNumConnections() + ", " 
					+ person.getLiUniqueID()  );
		}
	}



  
}
