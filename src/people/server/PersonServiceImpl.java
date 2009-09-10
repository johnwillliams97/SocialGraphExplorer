package people.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
//import com.google.apphosting.api.DeadlineExceededException; !@#$ handled in general get() catch(Exception e)
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cache.CacheActual;
import cache.CacheCache;
import cache.CacheDB;
import cache.CachePipeline;
import cache.CacheActual.WebReadPolicy;
import datatypes.PersistentPersonTrait;
import datatypes.PersonLI;
import people.client.Misc;
import people.client.MiscCollections;
import people.client.OurConfiguration;
import people.client.PersonFetch;
import people.client.PersonClient;
import people.client.PersonClientGroup;
import people.client.PersonTrait;
import people.client.Statistics;
import people.client.PersonService;


/*
 * The implementation of the RPC service which runs on the server.
 */
@SuppressWarnings("serial")
public class PersonServiceImpl extends RemoteServiceServlet implements PersonService {
	private static final Logger logger = Logger.getLogger(PersonServiceImpl.class.getName());
	private static final WebReadPolicy _webReadPolicy = OurConfiguration.ALLOW_LINKEDIN_READS ? WebReadPolicy.AUTO : WebReadPolicy.NEVER;
	private static final double _maxTime = OurConfiguration.MAX_TIME_FOR_SERVLET_RESPONSE; 
	private final long _servletLoadTimeMillis = System.currentTimeMillis();
	private long _firstCallTimeMillis = -1L;

	
	/*
	 * Tiny class for capturing cache pipeline stats
	 */
	class CachePipelineInstance {
	//	private static final Logger logger = Logger.getLogger(CachePipelineInstance.class.getName());
		
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
		public PersonLI get(long idIn, WebReadPolicy policy, double timeBoundSec) {
			double start = Statistics.getCurrentTime();
			
			long id = mapUnknownID(idIn);
			PersonLI person = this.cachePipeline.get(id, policy, timeBoundSec);
			cleanPersonConnections(person);
			++this.numFetches;
			String whence = "none";
						
			if (person != null) {
				double end = Statistics.getCurrentTime();
				person.setFetchDuration(Statistics.round3(end-start));
				whence = person.getWhence();
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
			
		//	String nameFull = person != null ? person.getNameFull() : "not found";
		//	logger.warning(id + ":" + nameFull + " - " + whence);
			
			return person;
		}
		// !@#$ Generalise this for PersistentPersonTrait.
		private long mapUnknownID(long id) {
			if (id == PersonTrait.GET_DEFAULT_PERSON_UNIQUEID)
				id = PersonLI.DEFAULT_PERSON_RECORD_UNIQUEID;
			return id;
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
		logger.warning("Hi there!");
	}
	
	
	
		
	private static PersonClient personServerToPersonClient(PersistentPersonTrait ps, long requestedID) {
		PersonClient pc = null;
		if (ps != null) {
			PersonClient.debugValidate(ps);
			pc = new PersonClient();
			pc.setIsRealData(ps.isRealData());
			pc.setUniqueID(ps.getUniqueID());
			pc.setRequestedID(requestedID);
			pc.setNameFull(ps.getNameFull());
			pc.setDescription(ps.getDescription());
			pc.setLocation(ps.getLocation());
			pc.setEmployer(ps.getEmployer());
			pc.setConnectionIDs(ps.getConnectionIDs());
			pc.setIsChildConnectionInProgress(ps.getIsChildConnectionInProgress());
			pc.setWhence(ps.getWhence());
			pc.setFetchDuration(ps.getFetchDuration());
			String htmlPage = ps.getHtmlPage();
			if (htmlPage != null && OurConfiguration.HTML_DATA_MAX_SIZE > 0) {
				int maxSize = Math.min(OurConfiguration.HTML_DATA_MAX_SIZE, htmlPage.length());
				if (maxSize > 1)
					htmlPage = htmlPage.substring(0, maxSize-1);
			}
			pc.setHtmlPage(htmlPage);
			PersonClient.debugValidate(pc);
		}
		return pc;
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


  /*
   * (non-Javadoc)
   * @see people.client.RPCWrapper#getPeople(long[], int[], long, long)
   * 
   *
    * @param requestedUniqueIDs - IDs of persons to fetch
	 * @param levels - client cache levels of the requestedUniqueIDs
	 * @param clientSequenceNumber - Number of high-level requests from this client
	 * @param sequenceNumber - tracks client requests
	 * @param currentTime - Time of call in seconds
	 * @return list of persons fetched from the data store
   */
	@Override
	public PersonClientGroup getPeople(long[] requestedUniqueIDs, int[] requestedLevels, 
			long clientSequenceNumber, int numCallsForThisClientSequenceNumber, long sequenceNumber,
			double callTime) {
		
		logger.info(MiscCollections.arrayToString(requestedUniqueIDs));
		Statistics.createInstance("getPeople");
		
		PersonClientGroup resultsMain = new PersonClientGroup();
		resultsMain.hadDeadlineExceededException = true;
		resultsMain.requestedUniqueIDs = requestedUniqueIDs;
		resultsMain.requestedLevels = requestedLevels;
		resultsMain.sequenceNumber = sequenceNumber;
		resultsMain.callTime = callTime;
		
		resultsMain = getPeople_(resultsMain, requestedUniqueIDs, requestedLevels, clientSequenceNumber, numCallsForThisClientSequenceNumber, sequenceNumber);
		
		// Some debug info  
		if (resultsMain.fetches == null) {
			logger.warning("resultsMain.fetches is null!!");
			//Statistics.getInstance().showAllEvents();
		}
		else {
			for (int i = 0; i < resultsMain.fetches.length; ++i) {
				List<Long> connectionIDs = resultsMain.fetches[i].person.getConnectionIDs();
				int numConnections = (connectionIDs != null) ? connectionIDs.size() : 0;
				logger.info("\t" + i + ": " 
						+ resultsMain.fetches[i].person.getNameFull() +  " - " 
						+ resultsMain.fetches[i].person.getUniqueID()  +  " - " 
						+ numConnections);
			}
		}
		logger.info("=========================*");
		
		return resultsMain;
	}
	/*
	 * @param	requestedUniqueIDs - IDs of persons to fetch
	 * @param	levels - client cache levels of the requestedUniqueIDs
	 * @param sequenceNumber - tracks client requests
	 * @return list of persons fetched from the data store
	 */
	private PersonClientGroup getPeople_(
						PersonClientGroup resultsMain,
						long[] requestedUniqueIDs, 
						int[]  requestedLevels,
						long   clientSequenceNumber, 
						int    numCallsForThisClientSequenceNumber,
						long   sequenceNumber) {
	
		CachePipelineInstance cachePipelineInstance = new CachePipelineInstance(this.cachePipeline);
		
		List<PersonFetch> fetchList = new ArrayList<PersonFetch>();
		
		final double maxTime1 = _maxTime / 2;
		double start = Statistics.getCurrentTime(); // !@#$ Change all times to double sec Statistics.getCurrentTime()
		double end = 0.0;
		
		if (_firstCallTimeMillis < 0L)
			_firstCallTimeMillis = System.currentTimeMillis();
	
		//First look at the people requested
		for (int i = 0; i < requestedUniqueIDs.length; ++i) {
			end =  Statistics.getCurrentTime();
			if (end - start > maxTime1)
				break;
			long uniqueID = requestedUniqueIDs[i];
			PersonLI person = null;
			try {
				 person = cachePipelineInstance.get(uniqueID, _webReadPolicy, start + maxTime1);
			}
			catch (Exception e) {
				// Best effort response to an exception
				logger.warning("Exception for person " + uniqueID +  ", " + i+ ": " + e.getMessage() + "," + e.toString()); 
				Misc.reportException(e);
				break;
			}
			if (person != null) {
				PersonFetch fetch = new PersonFetch();
				PersonClient rawPerson = personServerToPersonClient(person, requestedUniqueIDs[i]);
				fetch.person = MangleNames.manglePerson(rawPerson);
				fetch.requestedUniqueID = requestedUniqueIDs[i];
				fetch.level = requestedLevels[i];
				fetchList.add(fetch);
			}
			logger.info(uniqueID + ":" + (person != null ? person.getNameFull() : "not found") + ", i = " + i + ". fetchList size = " + fetchList.size());
			
		}
		logger.info("outta there!");
		
		Statistics.getInstance().recordEvent("responseDuration1");
		resultsMain.responseDuration1 = Statistics.round3(Statistics.getCurrentTime() - start);

		Statistics.getInstance().recordEvent("responseDuration2");
		resultsMain.responseDuration2 = Statistics.round3(Statistics.getCurrentTime() - start);
		
		logger.info("* fetchList size = " + fetchList.size());
		// Convert running list to output result
		if (fetchList.size() > 0) { 
			showPersonList(fetchList, "start");
			Statistics.getInstance().recordEvent("responseDuration3");
			resultsMain.responseDuration3 = Statistics.round3(Statistics.getCurrentTime() - start);
			PersonFetch[] fetches = new PersonFetch[fetchList.size()];
			for (int i = 0; i < fetches.length; ++i) {
				fetches[i] = fetchList.get(i);;										
			}
			resultsMain.fetches = fetches; //getUniqueNonNullEntries(results, 1000);
			logger.info("num fetches = " + fetches.length);
		}
			
		resultsMain.timeSignatureMillis = this._servletLoadTimeMillis + this._firstCallTimeMillis; // This is the most unique signature I can synthesize
		resultsMain.clientSequenceNumber = clientSequenceNumber;
		resultsMain.numCallsForThisClientSequenceNumber = numCallsForThisClientSequenceNumber;
		resultsMain.sequenceNumber = sequenceNumber;
		
		Statistics.getInstance().recordEvent("responseDuration");
		resultsMain.responseDuration = Statistics.round3(Statistics.getCurrentTime() - start);
		
		//Statistics.getInstance().showAllEvents();	
		
		resultsMain.numCacheFetches = cachePipelineInstance.getNumFetches();
		resultsMain.numMemCacheFetches = cachePipelineInstance.getNumMemCacheFetches();
		//resultsMain.numDBCacheFetches = cachePipelineInstance.getNumDBCacheFetches();
		//logger.warning("Total  cache fetches =     " + resultsMain.numCacheFetches);
		//logger.warning("Number mem cache fetches = " + resultsMain.numMemCacheFetches);
		//logger.warning("Number DB cache fetches =  " + resultsMain.numDBCacheFetches);
		
		return resultsMain;
	}

	private static void showPersonList(List<PersonFetch> fetchList, String name) {
		logger.info("------ " + name + " : " + fetchList.size());
		for (int i = 0; i < fetchList.size(); ++i) {
			PersonClient person = fetchList.get(i).person;
			List<Long> connectionIDs = person.getConnectionIDs();
			int numConnections = connectionIDs != null ? connectionIDs.size() : 0;
			logger.info("  " + i + ": " 
					+ person.getNameFull() + ", " 
					+ person.getLocation() + ", " 
					+ numConnections + ", " 
					+ person.getUniqueID()  );
		}
	}



  
}
