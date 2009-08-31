package people.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.google.gwt.user.client.Timer;
import people.client.PersonClientCacheEntry.CacheEntryState;
import people.client.RPCWrapper.IDsAtLevel;
import people.client.RPCWrapper.PersonsAcceptor;


/*
 * PersonClient client side cache and server connection
 * 
 * Main function: updateCacheAndGetVisible
 * 
 * Cache levels (for person record)

(Could divide person into summary that is visible in list and full info that is visible in body. Not in first version)
    Visible
    1 click away
        Next, Previous, First, Last screen = 4 x N
        Previous Anchor = N
        1st screen of connections of connections in list NxN

Client cache 

    Pre-fetch <=1-click away items. Kick off server request
    Get visible items. It not in client cache then block
    Update client cache as server request returns.

    No get without hinting first

    Cache levels
        Anchor
        Visible
        1 click
        2 clicks
        Recently used

    Cache entry states
        Empty
        Marked
        Requested
        Fetched

    Program flow
        UI change
        Hint 
        Update cache levels
        From highest to lowest level
            Search for missing entries in lower levels
            Evict old entries to fetched level
            Request all entries @ level from server asynchronously  (Should we block here until previous level returned?)
        Get visible entries from server.
            While any missing from cache start timer to check again   <= UI blocks here
        * At this point UI is synchronised 
 */
public class PersonClientCache {
		 
	public static final int NUM_VISIBLE = OurConfiguration.VISIBLE_PERSONS_COUNT;
	public static final int CACHE_LEVEL_ANCHOR = 0;
	public static final int CACHE_LEVEL_VISIBLE = 1;
	public static final int CACHE_LEVEL_CLICK1 = 2;
	public static final int CACHE_LEVEL_CLICK2 = 3;
	public static final int CACHE_LEVEL_SETTABLE_LEVELS = 4; 
	
	public static final int CACHE_LEVEL_RECENT = 4;
	public static final int CACHE_LEVEL_NUMBER_LEVELS = 5;

	
	PersonClientCacheEntry[][] _theClientCache = null;
	RPCWrapper _rpcWrapper = null;
	
	// Track server calls
	private long _clientSequenceNumber = 0L;
	
	// All calls from before this number are discarded.
	private long _clientSequenceNumberCutoff = -1L;
	// The visible persons currently requested !@#$ Probably should not return until these are fetched
	private long[] _currentVisibleRequest = null;
		  
	public PersonClientCache(int[] cacheLevelSizeIn) {
	 	
	  	/*
	  	 * Set up the cache defaults
	  	 */
		/*
		cacheLevelSize = new int[CACHE_LEVEL_NUMBER_LEVELS];
		cacheLevelSize[CACHE_LEVEL_ANCHOR] = 10;
		cacheLevelSize[CACHE_LEVEL_VISIBLE] = 10;
		cacheLevelSize[CACHE_LEVEL_CLICK1] = (4 + NUM_VISIBLE - 1) * NUM_VISIBLE;
		cacheLevelSize[CACHE_LEVEL_CLICK2] = (2) * NUM_VISIBLE;
		cacheLevelSize[CACHE_LEVEL_RECENT] = 100;
		*/
		int[] cacheLevelSize = null; // !@#$ redundant - remove
		if (cacheLevelSizeIn != null) {
			assert(cacheLevelSizeIn.length == CACHE_LEVEL_NUMBER_LEVELS);
			cacheLevelSize = new int[CACHE_LEVEL_NUMBER_LEVELS];
			for (int i = 0; i < CACHE_LEVEL_NUMBER_LEVELS; ++i) {
				assert(cacheLevelSizeIn[i] > 0);
				cacheLevelSize[i] = cacheLevelSizeIn[i];
			}
		}
		_theClientCache = new PersonClientCacheEntry[CACHE_LEVEL_NUMBER_LEVELS][];
		for (int i = 0; i < CACHE_LEVEL_NUMBER_LEVELS; ++i) {
			_theClientCache[i] = new PersonClientCacheEntry[cacheLevelSize[i]];
		}
		
		for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			  for (int j = 0; j < cacheLevelSize[level]; ++j) {
				  _theClientCache[level][j] = new PersonClientCacheEntry();
			  }
		}
			
		_rpcWrapper = new RPCWrapper();
	  }
	  
	  
	
	  
	  /*
	   *   Call sequence
	   *	hintPersonsInCache(long[] uniqueIDs, int level) sets up the client cache state but does not call the server
	   *    fetchPersonsFromServer(int level) fetches the missing data from the server. This is an async call
	   *    getPersonsFromCache(long[] uniqueIDs, int level) gets persons synchronously from cache. If
	   *    	fetchPersonsFromServer(level) has not played out then this call will block until it does.
	   *    @param array of arrays of ids to fetch
	   *    
	   *   Sequence 1  (Fewer wasted server fetches) DO THIS
	   *    key press
	   *     kill previous timers
	   *     hint all
	   *     fetch visible
	   *     get visible
	   *     set timer T secs to fetch invisible
	   *    update UI
	   *    
	   *   Sequence 2 (Faster look ahead)
	   *    key press
	   *     kill previous timers
	   *     hint all
	   *     fetch all
	   *     get visible
	   *    update UI
	   */
	  
	  // !@#$ Loose the timers? They slow UI response. 
	  private static int INVISIBLE_FETCH_DELAY_CLICK1 = OurConfiguration.INVISIBLE_FETCH_DELAY_CLICK1;
	  private static int INVISIBLE_FETCH_DELAY_CLICK2 = OurConfiguration.INVISIBLE_FETCH_DELAY_CLICK2;
//	  private static int INVISIBLE_FETCH_DELAY_EXTRA = 5000;
	  private static List<Integer> VISIBLE_CACHE_LEVEL_LIST = Arrays.asList(new Integer[] {CACHE_LEVEL_ANCHOR, CACHE_LEVEL_VISIBLE});
	  private static List<Integer> INVISIBLE_CACHE_LEVEL_LIST = Arrays.asList(new Integer[] {INVISIBLE_FETCH_DELAY_CLICK1, INVISIBLE_FETCH_DELAY_CLICK2});
	  private Timer fetcherTimerClick1 = new Timer() {
		  public void run() {
			  //If there are unfetched visible persons then restart timer, else fetch
			 /*  long[][] idsToFetch = getIdLists(VISIBLE_CACHE_LEVEL_LIST, CacheEntryState.PENDING);
			   if (MiscCollections.sizeOfArray(idsToFetch) > 0) 
				   fetcherTimerClick1.schedule(INVISIBLE_FETCH_DELAY_EXTRA);
			   else
			   */
				   fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK1}), false);
		  }
 	  };
 	  private Timer fetcherTimerClick2 = new Timer() {
		  public void run() {
			  //If there are unfectched visible persons then restart timer, else fetch
			/*   long[][] idsToFetch = getIdLists(VISIBLE_CACHE_LEVEL_LIST, CacheEntryState.PENDING);
			   if (MiscCollections.sizeOfArray(idsToFetch) > 0) 
				   fetcherTimerClick2.schedule(INVISIBLE_FETCH_DELAY_EXTRA);
			   else
			   */
			   	fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK2}), false);
		  }
	  };
	  
	  /*
  	   * Ugly way of returning PersonClientCacheEntry[] 
  	   */
 	interface GetPersonsFromCacheCallback {
  		public void handleReturn(PersonClientCacheEntry[][] entries,  String description);
 	}
    
	private GetPersonsFromCacheCallback cb_params_callback;
	
	
	/*
	 * Main function
	 * @param uniqueIDsList - list of hints for all levels
	 * @param remainingIDs - list of hints for all levels on follow-up call. 
	 *     Needed for 2 part calls where some IDs depend on the persons fetched for other IDs
	 *     So far the only case of this is the anchorID
	 * Should return visible cache entries but has to use a callback to do this
	 * 
	 * !@#$ Need to optimise number of calls
	 *    Max 2 RPC calls in progress at once
	 *    http://code.google.com/webtoolkit/doc/1.6/DevGuideServerCommunication.html#DevGuideGettingUsedToAsyncCalls
	 
	 */
	public void updateCacheAndGetVisible(long[][] uniqueIDsList, 
 			  long[][] remainingIDs, // !@#$ vestigal
			  GetPersonsFromCacheCallback callback) {
		 
 		SocialGraphExplorer.get().showInstantStatus("updateCacheAndGetVisible(" 
 				+ (uniqueIDsList != null) + ", " 
 				+ (remainingIDs != null) +  ")" );
 		if (uniqueIDsList[CACHE_LEVEL_VISIBLE] != null)
 			SocialGraphExplorer.get().log("updateCacheAndGetVisible", 
 					"[" + CACHE_LEVEL_VISIBLE + "]: " +
 					+ this._clientSequenceNumber + " - "
 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_VISIBLE]) + " "
 					+ getCacheLevels(uniqueIDsList[CACHE_LEVEL_VISIBLE]));
 		
 		// Record the IDs that MUST be returned !@#$
 		this._currentVisibleRequest = uniqueIDsList[CACHE_LEVEL_VISIBLE] ;
 		
 		/* 
 		 * !@#$ Naively discard all old fetches in progress to guarantee cache coherency
 		 * 		(Smarter to filter out PENDING fetches currently in progress from new request list
 		 * 		Make this change later)
 		 * Clear out all pending requests to ensure cache coherency Bad result from server
 		 */
 		this._clientSequenceNumberCutoff = this._clientSequenceNumber;
 		fetcherTimerClick1.cancel();
 		fetcherTimerClick2.cancel();
 		clearPendingCacheEntries();
		  
		  // fetchPersonsFromServer() calls back through cb_params_callback.handleReturn();
		  // The timer'd functions call this function as well
		  cb_params_callback = callback;
		  
		  // Configure cache
		  for (int level = CACHE_LEVEL_ANCHOR; level <= CACHE_LEVEL_CLICK2; ++level) {
			  hintPersonsInCache(uniqueIDsList[level], level);
		  }
		  if (uniqueIDsList[CACHE_LEVEL_VISIBLE] != null)
	 			SocialGraphExplorer.get().log("hinted PersonsInCache", 
	 					"[" + CACHE_LEVEL_VISIBLE + "]: " +
	 					+ this._clientSequenceNumber + " - "
	 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_VISIBLE]) + " "
	 					+ getCacheLevels(uniqueIDsList[CACHE_LEVEL_VISIBLE]));
		  
		  dumpCache("hintPersonsInCache");
		  // Kick off timers to do low priority requests. Logically, this follows
		  // fetchPersonsFromServer(), but we do it first to avoid weird race conditions
		  fetcherTimerClick1.schedule(INVISIBLE_FETCH_DELAY_CLICK1);
		  fetcherTimerClick2.schedule(INVISIBLE_FETCH_DELAY_CLICK2);
		  
		  // Async fetch from server of visible cache entries
		  // Returns clunkily through cb_params_callback.handleReturn();
		  fetchPersonsFromServer(VISIBLE_CACHE_LEVEL_LIST, true); 
	}
	/*
	   * Get a list of persons from the cache
	   * If persons have not been fetched then wait for them to be fetched
	   * 
	   * Result is returned via the callback function. Can't help this since the function contains a 
	   * timer callback.
	   * 
	   * @param uniqueIDs - list of ids to hint (for consistency only since hintPersonsInCache()
	   * 					determines which ids are availabe.
	   * @param level   - level to hint the ids at
	   */

	interface WaitForCacheToFillCallback {
		// @param visibleCacheEntries - visible cache entries
		public void handleReturn(PersonClientCacheEntry[][] visibleCacheEntries);
  	}
  	
	  /*
	   * Fetch NEED_TO_FETCH (requested but not fetched persons at specified level from server
	   * This function makes an asynchronous call to the server and returns immediately
	   * 
	   * All the NEED_TO_FETCH persons at the specified levels will be fetched 
	   * from the server through a series of best-effort calls
	   * 
	   * Returns clunkily through cb_params_callback.handleReturn();
	   * 
	   * This call changes cache states from NEED_TO_FETCH => PENDING
	   * 
	   */
	  	private void fetchPersonsFromServer(List<Integer> requestedLevels, boolean needsReturn) {
	  		//SocialGraphExplorer.get().showInstantStatus("fetchPersonsFromServer(" + requestedLevels + ", " + needsReturn + ")");
	  		List<IDsAtLevel> idsToFetch = getIdLists(requestedLevels, CacheEntryState.NEED_TO_FETCH);
	  		if (idsToFetch.size() > 0) {
			  ++this._clientSequenceNumber;
			  requestsInProgress.startRequest(this._clientSequenceNumber);
			//  SocialGraphExplorer.get().log("fetchPersonsFromServer", requestedLevels + ", " + RPCWrapper.getTotalNumberOfIDs(idsToFetch));
			  dumpCache("fetchPersonsFromServer");
			  // Actual call hidden among all the instrumentation
			  callServerToFetchPersons(idsToFetch, this._clientSequenceNumber, requestsInProgress.getNumCalls(this._clientSequenceNumber));
			  changeCacheStates(requestedLevels, CacheEntryState.NEED_TO_FETCH, CacheEntryState.PENDING);
			  
			  requestsInProgress.increment(this._clientSequenceNumber);
	  		}
	  		else if (needsReturn) {
			  // This path requires a return to the caller
			  cb_params_callback.handleReturn(getVisibleCacheEntries(), "fetchPersonsFromServer");
	  		}
	  	}
	  
	  	  	
	  /*
	   * Filter out IDs from 'uniqueIDs' that are already in the 'cache'
	   */
	  private List<Long> pruneExistingEntries(List<Long> uniqueIDs, PersonClientCacheEntry[] cache) {
		  List<Long> prunedUniqueIDs = new ArrayList<Long>();
		  for (int i = 0; i < uniqueIDs.size(); ++i) {
			  long id = uniqueIDs.get(i);
			  boolean inCache = false;
			  for (int j = 0; j < cache.length; ++j) {
				  if (id == cache[j].getRequestedUniqueID() || id == cache[j].getUniqueID()) {
					  inCache = true;
					  break;
				  }
			  }
			  if (!inCache) {
				  prunedUniqueIDs.add(id);
			  }
		  }
		  return prunedUniqueIDs;
	  }
	  /*
	   * Clear the cache of entries of a given state, typically PENDING
	   */
	  private void clearPendingCacheEntries() {
		  for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			  for (int j = 0; j <_theClientCache[level].length; ++j) {
				  CacheEntryState state = _theClientCache[level][j].getState();
				  if (state == CacheEntryState.NEED_TO_FETCH || state == CacheEntryState.PENDING) {
					  _theClientCache[level][j].setState(CacheEntryState.EMPTY);
				  }
			  }
		  }  
	  }
	  
	  private List<Integer> getEmptyEntryIndexes(PersonClientCacheEntry[] cache) {
		  List<Integer> emptyIndexes = new ArrayList<Integer>();
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == CacheEntryState.EMPTY)
				  emptyIndexes.add(i);
		  }
		  return emptyIndexes;
	  }
	  /*
	   * Move all found uniqueIDs from other levels to 'level'
	   */
	  private void moveUniqueIDsToLevel(List<Long> neededIDs, int level) { 
		  for (int i = 0; i < neededIDs.size(); ++i) {
			  CacheIndex resident = findInCacheAtOtherLevels(neededIDs.get(i), level);  // Not at level
			  if (resident != null) {
				  _theClientCache[level][i] = _theClientCache[resident.level][resident.index];
				  _theClientCache[resident.level][resident.index] = new PersonClientCacheEntry();
			  }
		  }
	  }
		
	  
	  /*
	   * Hint list of persons to  be fetched
	   * 
	   * NOTE: If this function is called in an iteration, then it must be iterated in
	   *       increasing level because  findInCacheAtOtherLevels() calls in this order.
	   *       ALWAYS push to higher numeric (==lower priority levels)
	   * This function sets up the cache to reflect this, including moving objects
	   * to other levels and ejecting victims
	   * 
	   * Strategy: Look for empty slots first then for victims
	   * 
	   * @param uniqueIDs - list of ids to hint
	   * @param level   - level to hint the ids at
	   */
	  private void hintPersonsInCache(long[] uniqueIDs, int level) {
		  assert(CACHE_LEVEL_ANCHOR <= level && level < CACHE_LEVEL_RECENT);
		  PersonClientCacheEntry[] cache = this._theClientCache[level];
		  
		  if (uniqueIDs != null) {
			  assert(uniqueIDs.length <= cache.length);
			  List<Long> neededIDs = MiscCollections.arrayToListLong(uniqueIDs);
			  
			  // Prune the  entries that are already in the cache
			  neededIDs = pruneExistingEntries(neededIDs, cache);
			  // Try to find replacements from within the other cache levels
			  moveUniqueIDsToLevel(neededIDs, level); 
			  // Re-Prune the  entries that are already in the cache
			  neededIDs = pruneExistingEntries(neededIDs, cache);
			  
			  // Do we need to add any IDs to cache?
			  if (neededIDs.size() > 0) {
			  	  //  Find empty slots 
				  List<Integer> emptyIndexes = getEmptyEntryIndexes(cache);
				  // Number of evictions needed
				  int numVictims = Math.max(0, neededIDs.size() - emptyIndexes.size());
		 		  if (numVictims > 0) {
		 			  // Move victims to the "recently used" list
		 			  List<Integer> recycledIndexes = recycleNEntries(numVictims, level);
		 			  emptyIndexes.addAll(recycledIndexes);
		 		  }
			
				  // Finally there are sufficient EMPTY entries
		 		  // !@#$ This assumes there are no PENDING entries. How do we guarantee that?
		 		  if (!(emptyIndexes.size() >= neededIDs.size()))
		 			  assert(emptyIndexes.size() >= neededIDs.size());
				  // Fill them
				  for (int i = 0; i < neededIDs.size(); ++i) {
					  int index = emptyIndexes.get(i);
					  assert(0 <= index && index < cache.length);
					  cache[index].setRequestedUniqueID(neededIDs.get(i));
					  cache[index].setState(CacheEntryState.NEED_TO_FETCH);
				  }
			  }
		  }
	  }
	  /*
	   * Return a set of ids of persons in cache at level 'cacheLevel' and state 'cacheEntryState'
	   * Does not call server
	   * Does not change cache
	   * 
	   * @param cacheLevel - cache level
	   * @param cacheEntryState - cache state
	   * @return list of ids or null if there are no ids
	   */
	  private Set<Long> getIdList(int level, CacheEntryState cacheEntryState) {
		  Set<Long> ids = new LinkedHashSet<Long>();
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == cacheEntryState) 
				  ids.add(cache[i].getRequestedUniqueID());
		  }
		  return ids;
	  }
	  private List<IDsAtLevel> getIncompletePersonsIDs(PersonFetch[] fetches, 
			  					boolean noDetail, boolean connectionsInProgress,
			  					List<Integer> targetLevels) {
		  List<IDsAtLevel> list = new ArrayList<IDsAtLevel>();
		  int numFetches = fetches != null ? fetches.length : 0;
		  for (int i = 0; i < numFetches; ++i) {
			  if (fetches[i].person.isIncomplete(noDetail, connectionsInProgress)) {
				  int level = fetches[i].level;
				  if (targetLevels.contains((Integer)level)) {
					  long id = fetches[i].person.getLiUniqueID();
					  Set<Long> ids = null;
					  for (IDsAtLevel idsAtLevel: list) {
						  if (idsAtLevel.level == level) {
							  ids = idsAtLevel.ids;
							  ids.add(id);
							  break;
						  }
					  }
					  if (ids == null) {
						  ids = new LinkedHashSet<Long>();
						  ids.add(id);
						  list.add(new IDsAtLevel(level, ids));
					  }
				  }
			  }
		  }
		  return list;
	  }
	  
	  private List<IDsAtLevel> getIdLists(List<Integer> levels, CacheEntryState cacheEntryState) {
		  List<IDsAtLevel>  idsAtLevel = new ArrayList<IDsAtLevel> ();
		  for (int i = 0; i < levels.size(); ++i) {
			  int level = levels.get(i);
			  Set<Long> idList = getIdList(level, cacheEntryState);
			  if (idList.size() > 0) {
				  idsAtLevel.add(new IDsAtLevel(level, idList) );
			  }
		  }
		  return idsAtLevel;
	  }
	  private void changeCacheStatesAtLevel(int level, CacheEntryState oldState, CacheEntryState newState) {
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == oldState) 
				  cache[i].setState(newState);
		  }
	  }
	  private void changeCacheStates(List<Integer> levels, CacheEntryState oldState, CacheEntryState newState) {
		   for (int i = 0; i < levels.size(); ++i) {
			  changeCacheStatesAtLevel(levels.get(i), oldState, newState);
		  }
	  }
	 
	  /*
	   * Request a best-effort (time limited) person fetch from server
	   * 
	   * If returned data set is incomplete then the onSuccess() callback will keep fetching 
	   * until all requested persons are returned. This in turn will call this function
	   * with the unfetched id list and so on until all ids are fetched.
	   * 
	   * This function returns clunkily through AcceptorUpdateCache.accept() which 
	   *  calls cb_params_callback.handleReturn();
	   * The continued fetching is implemented here.
	   */
//	  static List<Integer> theLevels = null;
	  private RequestsInProgress requestsInProgress = new RequestsInProgress();
	  
	  private void callServerToFetchPersons(List<IDsAtLevel> idsAtLevel,
			  long clientSequenceNumber, int numCallsForThisClientSequenceNumber) {
		  
		  SocialGraphExplorer.get().log("callServerToFetchPersons", 
				  "[" + idsAtLevel.get(0).level + "]: " + idsAtLevel.get(0).ids);
		  dumpCache("callServerToFetchPersons");
		  
		  _rpcWrapper.getPersonsFromServer(_acceptorUpdateCache, 
				  	 idsAtLevel, clientSequenceNumber, numCallsForThisClientSequenceNumber,
	  				"callServerToFetchPersons");
	  }	 
	 
	  private AcceptorUpdateCache _acceptorUpdateCache  = new AcceptorUpdateCache();
	  class AcceptorUpdateCache implements PersonsAcceptor {
		  /*
		   * (non-Javadoc)
		   * @see people.client.RPCWrapper.PersonsAcceptor#accept(java.util.List, people.client.PersonFetch[])
		   * 
		   * Gets called when async RPC calls complete.
		   * 	1) Always update cache
		   * 	2) Calls back to UI if visible persons have been fetched
		   * 	3) Calls RPC again as long as there are people to fetch
		   * !@#$ could call for unfetched persons through a cancelable timer 
		   */
	  		@Override
			public void accept(List<Integer> requestedLevels, PersonFetch[] fetches,
					          long clientSequenceNumber, double callTime) {
	  			SocialGraphExplorer.get().log("accept", clientSequenceNumber + ": "
	  					+ requestsInProgress.getNumCalls(clientSequenceNumber) + " - "
	  					+ (fetches != null ? fetches.length : 0) + " fetches, " 
	  					+ requestedLevels + " levels, "
	  					);
	  			// Throw away old requests ?
	  			if (clientSequenceNumber <= _clientSequenceNumberCutoff) {
	  				SocialGraphExplorer.get().log("Discarding old request", 
	  						  "clientSequenceNumber = " + clientSequenceNumber
	  						+ ", _clientSequenceNumberCutoff = " + _clientSequenceNumberCutoff);
	  			// Update UI if visible changes
		  			if (hasVisibleLevel(requestedLevels)) {  //!@#$ Need to update on invisible fetches?
		  				cb_params_callback.handleReturn(getVisibleCacheEntries(), "accept");
		  			}
	  			}
	  			else {
		  			recordCallDurations(fetches, callTime);
	  			   // Update the cache!
		  			List<Long> misses = addToCache(fetches);
		  			if (misses.size() > 0) {
		  				SocialGraphExplorer.get().showError("Could not add " + clientSequenceNumber + "(misses = "  + misses + ") to cache" );
		  			}
		  			
		  			// Update UI if visible changes
		  			if (hasVisibleLevel(requestedLevels)) {  //!@#$ Need to update on invisible fetches?
		  				cb_params_callback.handleReturn(getVisibleCacheEntries(), "accept");
		  			}
		  			
		  			// Fetch more persons if not all are fetched or some are incompletely, but first check the number of RPC calls for this request
		  			List<IDsAtLevel> unfetchedIDs = getIdLists(requestedLevels, CacheEntryState.PENDING);
		  			List<IDsAtLevel> incompletePersonIDsVisible   = getIncompletePersonsIDs(fetches, true, true, VISIBLE_CACHE_LEVEL_LIST); // !@#$ only for visible levels
		  			List<IDsAtLevel> incompletePersonIDsInvisible = getIncompletePersonsIDs(fetches, true, true, INVISIBLE_CACHE_LEVEL_LIST);
		  			List<IDsAtLevel> incompletePersonIDs = RPCWrapper.meldIDsAtLevelLists(incompletePersonIDsVisible, incompletePersonIDsInvisible);
		  			List<IDsAtLevel> idsToFetch = RPCWrapper.meldIDsAtLevelLists(unfetchedIDs, incompletePersonIDs);
		  			int numCalls = requestsInProgress.getNumCalls(clientSequenceNumber);
		  			if (idsToFetch.size() > 0 && numCalls <= RequestsInProgress.MAX_SERVER_CALLS_PER_REQUEST) {
		  				SocialGraphExplorer.get().log("Incomplete fetch ", clientSequenceNumber 
		  						+ ": " + RPCWrapper.getTotalNumberOfIDs(unfetchedIDs) + " unfetched "
		  						+ "+ " + RPCWrapper.getTotalNumberOfIDs(incompletePersonIDsVisible) + " visible incomplete "
		  						+ "+ " + RPCWrapper.getTotalNumberOfIDs(incompletePersonIDsInvisible) + " invisible incomplete "
		  						+ "= " + RPCWrapper.getTotalNumberOfIDs(idsToFetch) + " total " 
		  						+ ", ids = " + idsToFetch.get(0).ids 
		  					);
		  				
		  				// Call server again from the response callback
		  				// The actual call!! hidden amongst logging code
		  				callServerToFetchPersons(idsToFetch, clientSequenceNumber, numCalls);
		  				
						requestsInProgress.increment(clientSequenceNumber);
					}
	  			}
			}
	  	}
	  
	  private void recordCallDurations(PersonFetch[] fetches, double callTime) {
		  double duration = Statistics.getCurrentTime() - callTime;
		  if (fetches != null) {
			  for (PersonFetch fetch: fetches) {
				  fetch.person.setFetchDurationFull(Statistics.round1(duration));
			  }
		  }
	  }
	  
	  	private boolean hasVisibleLevel(List<Integer> requestedLevels) {
	  		Integer anchor = CACHE_LEVEL_ANCHOR;
	  		Integer visible = CACHE_LEVEL_VISIBLE;
	  		return (requestedLevels.contains(anchor) || requestedLevels.contains(visible));
	  	}
	  	/*
		  * @return - all visible FILLED cache entries
		   */
	  	private PersonClientCacheEntry[][] getVisibleCacheEntries() {
	  		PersonClientCacheEntry[][] visibleCacheEntries = new PersonClientCacheEntry[2][];
  			visibleCacheEntries[0] = getFilledCacheEntriesAtLevel(CACHE_LEVEL_ANCHOR);
  			visibleCacheEntries[1] = getFilledCacheEntriesAtLevel(CACHE_LEVEL_VISIBLE);
  			assert(visibleCacheEntries[0] != null);
  			return visibleCacheEntries;
	  	}
	  	
	  	  /*
		   * Get all FILLED cache entries at a specified level.
		   * @param level
		   * @return - all cache entries at this level
		   */
		  public PersonClientCacheEntry[] getFilledCacheEntriesAtLevel(int level) {
			  Set<Long> fetchedIDs = getIdList(level, CacheEntryState.FILLED);
			  PersonClientCacheEntry[] entries = new PersonClientCacheEntry[fetchedIDs.size()];
			  int i = 0;
			  for (Long id: fetchedIDs) {
				  entries[i++] = getPersonCacheEntryAtLevel(id, level);
			  }
			  return entries; // ??
		  }
		  
		  /*
		   * Get an entry from a cache level.
		   * It may, or may not, contain a person.
		   * @param uniqueID
		   * @param level
		   * @return - matching cache entry, or null if there is no match
		   */
		  private PersonClientCacheEntry getPersonCacheEntryAtLevel(long uniqueID, int level) {
			  PersonClientCacheEntry personCacheEntry = null;
			  boolean match = false;
			  if (uniqueID > 0 || uniqueID == PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID)  {
				  for (PersonClientCacheEntry cacheEntry: _theClientCache[level]) {
					  // Need this special test during the bootstrapping phase where cacheEntry person has not been filled
					  // !@#$ Can this be simplified
					  match = (cacheEntry.getRequestedUniqueID() == uniqueID); // For bootstrapping, uniqueID < 0 
					  if (cacheEntry.getPerson() != null)   {
						  if ((cacheEntry.getPerson().getRequestedID() == uniqueID && uniqueID < 0) ||   // For bootstrapping, uniqueID < 0 
							  (cacheEntry.getPerson().getLiUniqueID()  == uniqueID && uniqueID > 0)) {    // For matching on connections,  uniqueID > 0 
							  match = true;
						  } 
					  }
					  if (match) {
						  personCacheEntry = cacheEntry;
						  break;
					  }
				  }
			  }
			  return personCacheEntry;
		  }
		  
	  	/*
		 * Add some (actual) persons to the cache. This should only get called when the
		 * server returns
		 * @return - misses
		 */
	  	private List<Long> addToCache(PersonFetch[] fetches)  {
	  		List<Long> misses = new ArrayList<Long>();
	  		if (fetches != null) {
		  		for (int i = 0; i < fetches.length; ++i) {
					int level = fetches[i].level;
					PersonClient person = fetches[i].person;
					PersonClientCacheEntry[] cache = _theClientCache[level];
					int index = getIndexInCache(person, level);
					if (index >= 0) {
						cache[index].setPerson(person);
						cache[index].setState(CacheEntryState.FILLED);
					}
					else {
						misses.add(person.getRequestedID());
					}
				}
	  		}
	  		return misses;
		}
	  	
	  	/*
	     * Returns index into cache for a person based on requested ID.
	     * Should only be called when server returns data and person is in cache
	     * Will fail when PENDING entries are pushed out of cache
	     */
	  	private int getIndexInCache(PersonClient person, int level)  {
			  PersonClientCacheEntry[] cache = _theClientCache[level];
			  int index = -1;
			  for (int i = 0; i < cache.length; ++i) {
				  if (cache[i].getRequestedUniqueID() == person.getRequestedID()) {
					  index = i;
					  break;
				  }
			  }
			  return index;
	  	}
		  
	  	/*
	  	 * Recycle 'numVictims' FILLED entries at cache level 'level'
	  	 * Move the entry to the recently used level and mark the entry's original place as EMPTY
	  	 *
	  	 * Doubts:
	  	 *  Need to recycle all non-EMPTY slot to guarantee success
	  	 *		FILLED then PENDING then NEED_TO_FETCH.
	  	 *  !@#$ This is ugly, but I currently think this will give a better user experience than
	  	 *  blocking while the PENDING requests complete.
	  	 *  I think that NEED_TO_FETCH should never be used but I am uncertain about Javascript callback
	  	 *  ordering.
	  	*/ 
	  	List<Integer> recycleNEntries(int numVictims, int level) {
	  		assert(level < CACHE_LEVEL_SETTABLE_LEVELS);
	  		PersonClientCacheEntry[] cache = this._theClientCache[level];
	  		assert(numVictims <= cache.length);
	  		List<Integer> recycledIndexes = new ArrayList<Integer>();
	  		PersonClientCacheEntry entry = null;
	  		int victimNum = 0;
	  		CacheEntryState[] statesToCheck = 
	  			{ CacheEntryState.FILLED, CacheEntryState.PENDING, CacheEntryState.NEED_TO_FETCH };
out:	  	for (CacheEntryState state: statesToCheck) {
				assert(state !=  CacheEntryState.NEED_TO_FETCH); // !@#$ Wish I knew
		  		for (int index = 0; index < cache.length; ++index) {
	  				entry = cache[index];
	  				if (entry.getState() == state) {
	  					if (entry.getState() == CacheEntryState.FILLED) {
	  						copyToRecent(entry);
	  					}
	  		  			entry.setState(CacheEntryState.EMPTY);
	  		  			recycledIndexes.add(index);
	  		  			++victimNum;
	  		  			if (victimNum >= numVictims)
	  		  				break out;
	  				}
	  			} 
	  		}
	  		assert(recycledIndexes.size() == numVictims);
	  		return recycledIndexes;
	  	}
	 	  
	  /*
	   * Copy an entry into CACHE_LEVEL_RECENT, replacing the LRU entry there
	   */
	  private void copyToRecent(PersonClientCacheEntry entry) {
		  int lru = findLRUCacheIndex(CACHE_LEVEL_RECENT);
		  _theClientCache[CACHE_LEVEL_RECENT][lru] = entry;
	  }
	 
	  /*
	   * Find least recently used slot in a cache of a specified level
	   */
	  private int findLRUCacheIndex(int level) {
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  int lru = -1;
		  long oldestRef = Long.MAX_VALUE;
		  PersonClientCacheEntry entry;
		
		  // Check for empty slots first.
		  for (int i = 0; i < cache.length; ++i) {
			  entry = cache[i];
			  assert(entry != null);
			  if (entry.getState() == CacheEntryState.EMPTY) {
				  lru = i;
				  break;
			  }
		  }
		  // If no slots empty then find lru slot
		  if (lru < 0) {
			  for (int i = 0; i < cache.length; ++i) {
				  entry = cache[i];
				  assert(entry != null);
				  if (entry.getState() == CacheEntryState.FILLED &&
					  entry.getLastReference() < oldestRef) {
					  oldestRef = entry.getLastReference();
					  lru = i;
				  }
			  }
		  }
	//	  System.err.println("lru = " + lru);
		  assert(0 <= lru && lru < cache.length); 
		  return lru;
	  }
	  
	  /*
	   * Find a uniqueID in the cache. Look in every level except notThisLevel
	   * @param uniqueID - match this. This must be in the cache, not merely requested. See 
	   * 		PersonClientCacheEntry.getUniqueID()
	   * @param notThisLevel - look at all levels except this one
	   * @return [level, index] or null if no match
	   */
	  private class CacheIndex {
		  public int level;
		  public int index;
	  }
	  /*
	   * Get 'uniqueID' from any cache level except 'notThisLevel'
	   * @return level,index pair on match, null otherwise
	   */
	  private CacheIndex findInCacheAtOtherLevels(long uniqueID, int notThisLevel) {
		  CacheIndex cacheIndex = null;
		  int index = -1;
		  for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			  if (level != notThisLevel) {
				  index = findInCacheAtLevel(uniqueID, level);
				  if (index >= 0) {
					  cacheIndex = new CacheIndex();  
					  cacheIndex.level = level;
					  cacheIndex.index = index;
					  break;
				  }
			  }
		  }
		  return cacheIndex;
	  }
	  /*
	   * Find a uniqueID in the cache at specified level
	   * @param uniqueID - match this. This must be in the cache, not merely requested. See 
	   * 		PersonClientCacheEntry.getUniqueID()
	   * @param level - level to search in
	   * @return index into level, or -1 if no match
	   */
	  int findInCacheAtLevel(long uniqueID, int level) {
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  int index = -1;
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getUniqueID() == uniqueID) {
				  index = i;
				  break;
			  }
		  }
		  return index;
	  }
	  /*
	   * Find which cache level, if any, uniqueID is in.
	   * @param uniqueID - match this. This must be in the cache, not merely requested. See 
	   * 		PersonClientCacheEntry.getUniqueID()
	   * @return cache level, or -1 if no match
	   */
	  int getCacheLevel(long uniqueID) {
		  int cacheLevel = -1;
	 	  for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			int index = findInCacheAtLevel(uniqueID, level);
			if (index >= 0) {
				cacheLevel = level;
				break;
			}
	 	  }
	 	  return cacheLevel;
	  }
	  
	  List<Integer> getCacheLevels(long[] uniqueIDs) {
		  List<Integer> levels = new ArrayList<Integer>();
		  if (uniqueIDs != null) {
			  for (int i = 0; i < uniqueIDs.length; ++i) {
				  levels.add(getCacheLevel(uniqueIDs[i]));
			  }
		  }
		  return levels;
	  }
	
	  public void dumpCache(String location) {
		  int numOccupied = 0;
		  String msg = "" ;
		  for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
	  			PersonClientCacheEntry[] cache = _theClientCache[level];
	  			msg += "Level " + level + ": ";
		  		for (PersonClientCacheEntry cacheEntry: cache) {
			    	if (cacheEntry.getRequestedUniqueID() != PersonClient.UNIQUE_ID_NOT_FOUND) {
			    		msg +=  "" + numOccupied
									+   "{" + cacheEntry.getRequestedUniqueID()
							        +   "," + cacheEntry.getState()
							        +   "}, ";
			    		++numOccupied;
			    	}
		  		}
		  		msg += ";;; ";
		  }
		//  SocialGraphExplorer.get().log("dumpCache(" + location + ")", msg);
	  }
	  
	  
}

