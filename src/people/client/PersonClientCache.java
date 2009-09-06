package people.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
//import com.google.gwt.user.client.Timer;
import people.client.PersonClientCacheEntry.CacheEntryState;
import people.client.RPCWrapper.IDsAtLevel;
import people.client.RPCWrapper.PersonsAcceptor;


/*
 * PersonClient client-side cache and server connection
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
        From highest to lowest level !@#$ see new flow in hintPersonsInCache
            Search for missing entries in lower levels
            Evict old entries to fetched level
            Request all entries @ level from server asynchronously  (Should we block here until previous level returned?)
        Get visible entries from server.
            While any missing from cache start timer to check again   <= UI blocks here
        * At this point UI is synchronised 
 */
public class PersonClientCache {
		 
	public static final int CACHE_LEVEL_ANCHOR = 0;
	public static final int CACHE_LEVEL_VISIBLE = 1;
	public static final int CACHE_LEVEL_VISIBLE_LEVELS = 2; 
	
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
	private long[] _requestedVisibleIDs = null;
		  
	public PersonClientCache(int[] cacheLevelSize) {
	 	
		Misc.myAssert(cacheLevelSize != null) ;
		Misc.myAssert(cacheLevelSize.length == CACHE_LEVEL_NUMBER_LEVELS);
		
		_theClientCache = new PersonClientCacheEntry[CACHE_LEVEL_NUMBER_LEVELS][];
		for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			_theClientCache[level] = new PersonClientCacheEntry[cacheLevelSize[level]];
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
	//  private static int INVISIBLE_FETCH_DELAY_CLICK1 = OurConfiguration.INVISIBLE_FETCH_DELAY_CLICK1;
	 // private static int INVISIBLE_FETCH_DELAY_CLICK2 = OurConfiguration.INVISIBLE_FETCH_DELAY_CLICK2;
//	  private static int INVISIBLE_FETCH_DELAY_EXTRA = 5000;
	  private static List<Integer> VISIBLE_CACHE_LEVEL_LIST   = Arrays.asList(new Integer[] {CACHE_LEVEL_ANCHOR, CACHE_LEVEL_VISIBLE});
	  private static List<Integer> INVISIBLE_CACHE_LEVEL_LIST = Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK1, CACHE_LEVEL_CLICK2});
	  /*
	  private static boolean do_three_simultaneous_fetches = false;
	  private Timer fetcherTimerClick1 = new Timer() {
		  public void run() {
			  if (do_three_simultaneous_fetches)
				  fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK1}), false);
			  else
				  fetchPersonsFromServer(INVISIBLE_CACHE_LEVEL_LIST, false);
		  }
 	  };
 	  private Timer fetcherTimerClick2 = new Timer() {
		  public void run() {
			  if (do_three_simultaneous_fetches)
				  fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK2}), false);
		  }
	  };
	  */
	  /*
  	   * Ugly way of returning PersonClientCacheEntry[] 
  	   */
 	public interface GetPersonsFromCacheCallback {
  		public void handleReturn(PersonClientCacheEntry[][] entries,  String description);
 	}
 	// This is really a global, hence all lower case
	private GetPersonsFromCacheCallback cb_params_callback;
	private int                         _numberOfCallbacksNeeded = 0;
	
	// Always call back through this function to keep track of calls in progress
	private void doCallback(String description) {
		Misc.myAssert(_numberOfCallbacksNeeded >= 0);
		if (_numberOfCallbacksNeeded > 0)
			--_numberOfCallbacksNeeded;
		SocialGraphExplorer.get().showInstantStatus2("doCallback(" + description +  ")",
					  this._clientSequenceNumber + ":"  + _requestsInProgress.getNumCalls(this._clientSequenceNumber) 
					  + ", " + _numServerCallsInProgress + ":" + _serverFetchArgsList.size()
					  + ", " + (this._clientSequenceNumber <= _clientSequenceNumberCutoff)
					  + ", " + _numberOfCallbacksNeeded, false);
		
		PersonClientCacheEntry[][] entries = getVisibleCacheEntries();
		
		// _requestedVisibleIDs are the IDs of the cache entries that must be filled before returning
		// visibleEntryIds should not contain any IDs that are not in _requestedVisibleIDs
		long[] visibleEntryIds = MiscCollections.listToArrayLong(getIdListForEntries(entries[CACHE_LEVEL_VISIBLE]));
		Misc.myAssert(MiscCollections.arrayContainsArray(_requestedVisibleIDs, visibleEntryIds));
		// This when combined with the previous assertion ensures visibleEntryIds==_requestedVisibleIDs
		Misc.myAssert(MiscCollections.arrayContainsArray(visibleEntryIds, _requestedVisibleIDs));
		
		cb_params_callback.handleReturn(entries, description);
	}
	
	
	/*
	 * Main function
	 * @param uniqueIDsList - list of hints for all levels
	 * @param callback - this function returns through this callback. The callback returns the 
	 * 					_visible_ cache entries
	 * 
	 * !@#$ Need to optimise number of calls
	 *    Max 2 RPC calls in progress at once
	 *    http://code.google.com/webtoolkit/doc/1.6/DevGuideServerCommunication.html#DevGuideGettingUsedToAsyncCalls
	 */
	public void updateCacheAndGetVisible(long[][] uniqueIDsList, GetPersonsFromCacheCallback callback) {
		Misc.myAssert(uniqueIDsList != null);
 		Misc.myAssert(uniqueIDsList[CACHE_LEVEL_ANCHOR] != null);
 	//	Misc.myAssert(uniqueIDsList[CACHE_LEVEL_VISIBLE] != null); Will be null when anchor fetched for first time
 		Misc.myAssert(!MiscCollections.arrayContains(uniqueIDsList[CACHE_LEVEL_VISIBLE], uniqueIDsList[CACHE_LEVEL_ANCHOR][0]));
 		
 		// These are the IDs of the cache entries that must be filled before returning
 		_requestedVisibleIDs = uniqueIDsList[CACHE_LEVEL_VISIBLE];
 		
 		// fetchPersonsFromServer() calls back through cb_params_callback.handleReturn();
		// The timer'd functions call this function as well
		cb_params_callback = callback;
		 			
 		SocialGraphExplorer.get().showInstantStatus("updateCacheAndGetVisible(" + (uniqueIDsList != null) 
 				+  ") calls in prog = " + _numberOfCallbacksNeeded, true);
 		SocialGraphExplorer.get().log("updateCacheAndGetVisible", 
 					"[" + CACHE_LEVEL_ANCHOR +  ", " + CACHE_LEVEL_VISIBLE + "]: " +
 					+ this._clientSequenceNumber + " - "
 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_ANCHOR]) + ", "
 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_VISIBLE]) + ", "
 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_CLICK1]) + ", "
 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_CLICK2]) + " - "
 					+ getCacheLevels(uniqueIDsList[CACHE_LEVEL_ANCHOR]) + ", "
 					+ getCacheLevels(uniqueIDsList[CACHE_LEVEL_VISIBLE]),
 					true);
 		
 		 // Record the IDs that MUST be returned !@#$
 		this._requestedVisibleIDs = uniqueIDsList[CACHE_LEVEL_VISIBLE] ;
 		
 		/* 
 		 * !@#$ Naively discard all old fetches in progress to guarantee cache coherency
 		 * 		(Smarter to filter out PENDING fetches currently in progress from new request list
 		 * 		Make this change later)
 		 * Clear out all pending requests to ensure cache coherency 
		 */
 		this._clientSequenceNumberCutoff = this._clientSequenceNumber;
 		clearPendingCacheEntries();
 		while (_serverFetchArgsList.size() > 0 ) {
 			@SuppressWarnings("unused")
			ServerFetchArgs workArgs = _serverFetchArgsList.remove();
 		}
 		// All pending calls are now cleared  
 		markCacheLevels();  // Instrumentation
 		
 		// Callback for discarded server calls from previous calls to updateAndGetVisible()
 		if (_numberOfCallbacksNeeded > 0) {
 			doCallback("discarding old server calls"); 
 		}
		// Now the cache is in clean
 		
 		
 		++_numberOfCallbacksNeeded;
 		
 		// Configure cache
 		for (int level = CACHE_LEVEL_ANCHOR; level <= CACHE_LEVEL_CLICK2; ++level) {
			  hintPersonsInCache(uniqueIDsList[level], level);
		}
		// Set LRUs for visible entries
		for (int level = CACHE_LEVEL_ANCHOR; level <= CACHE_LEVEL_VISIBLE; ++level) {
			  PersonClientCacheEntry[] cache = this._theClientCache[level];
			  for (PersonClientCacheEntry entry: cache)
				  entry.touchLastReference();
		}
		if (uniqueIDsList[CACHE_LEVEL_VISIBLE] != null)
	 			SocialGraphExplorer.get().log("hinted PersonsInCache", 
	 					"[" + CACHE_LEVEL_VISIBLE + "]: " +
	 					+ this._clientSequenceNumber + " - "
	 					+ MiscCollections.arrayToString(uniqueIDsList[CACHE_LEVEL_VISIBLE]) + " "
	 					+ getCacheLevels(uniqueIDsList[CACHE_LEVEL_VISIBLE]));
		dumpCache("after hintPersonsInCache");
		  
		  // Kick off timers to do low priority requests. Logically, this follows
		  // fetchPersonsFromServer(), but we do it first to avoid weird race conditions
		  /*
		  fetcherTimerClick1.schedule(INVISIBLE_FETCH_DELAY_CLICK1);
		  if (do_three_simultaneous_fetches)
			  fetcherTimerClick2.schedule(INVISIBLE_FETCH_DELAY_CLICK2);
		  */
		  // Async fetch from server of visible cache entries
		  // Returns clunkily through cb_params_callback.handleReturn();
		fetchPersonsFromServer(VISIBLE_CACHE_LEVEL_LIST, true); 
		fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK1}), false);
		fetchPersonsFromServer(Arrays.asList(new Integer[] {CACHE_LEVEL_CLICK2}), false);
	}
	
	/*
	   * Get a list of persons from the cache
	   * 
	   * Result is returned via the callback function. Can't help this since the function contains a 
	   * timer callback.
	   * 
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
	   * @param requestedLevels - cache levels to fetch persons for
	   * @param needsReturn - true if callback is required
	   */
	  	private void fetchPersonsFromServer(List<Integer> requestedLevels, boolean needsReturn) {
	  		List<IDsAtLevel> idsToFetch = getIdLists(requestedLevels, CacheEntryState.NEED_TO_FETCH);
	  		if (idsToFetch.size() > 0) {
			  ++this._clientSequenceNumber;
			  _requestsInProgress.startRequest(this._clientSequenceNumber);
			  
			  // Actual call hidden among all the instrumentation
			  callServerToFetchPersons(idsToFetch, this._clientSequenceNumber, _requestsInProgress.getNumCalls(this._clientSequenceNumber));
			  changeCacheStates(requestedLevels, CacheEntryState.NEED_TO_FETCH, CacheEntryState.PENDING);
			  
			  _requestsInProgress.increment(this._clientSequenceNumber);
	  		}
	  		else if (needsReturn) {
			  // This path requires a return to the caller
	  			doCallback("fetchPersonsFromServer");
	  		}
	  	}
	  
	  	  	
	  
	  /*
	   * Clear the cache of PENDING and NEED_TO_FETCH entries
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
	  /*
	   * Return the indexes of all the empty slots in a cache
	   * @param cache - the cache
	   * @return - list of empty slot indexes
	   */
	  static private List<Integer> getEmptyEntryIndexes(PersonClientCacheEntry[] cache) {
		  List<Integer> emptyIndexes = new ArrayList<Integer>();
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == CacheEntryState.EMPTY)
				  emptyIndexes.add(i);
		  }
		  return emptyIndexes;
	  }
	 
		
	  /*
	   * Find 'uniqueID' from any cache level except 'notThisLevel'
	   * @param uniqueID - id to match
	   * @param notThisLevel - level to avoid
	   * @return - (level,index) pair on match, null otherwise
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
	   * Hint list of persons to  be fetched
	   * 
	   * NOTE: If this function is called in an iteration, then it must be iterated in
	   *       increasing level because  findInCacheAtOtherLevels() calls in this order.
	   *       ALWAYS push to higher numeric (==lower priority levels)
	   * This function sets up the cache to reflect this, including moving objects
	   * to other levels and ejecting victims
	   * 
	   * Strategy: 
	   * 	Needed IDs are one of
	   * 		in cache at this level
	   * 		in cache at another level
	   * 		not in cache
	   * 	Therefore
	   * 		evict (recycle) unneeded IDs from this level of cache
	   * 		leave needed IDs that are already at this level
	   * 		look for missing needed IDs in other levels of cache
	   * 		fetch still missing needed IDs from server 
	   * 
	   * @param uniqueIDs - list of IDs to hint
	   * @param level   - level to hint the IDs at
	   */
	  private void hintPersonsInCache(long[] uniqueIDs, int level) {
		  Misc.myAssert(CACHE_LEVEL_ANCHOR <= level && level < CACHE_LEVEL_SETTABLE_LEVELS);
		  PersonClientCacheEntry[] cache = this._theClientCache[level];
		  
		  if (uniqueIDs != null) {
			  Misc.myAssert(uniqueIDs.length <= cache.length);
			 
			  // Evict the unneeded IDs
			  recycleUnneededIDs(uniqueIDs, level);
			 // dumpCache("!@#$ recycleUnneededIDs", false);
			  
			  // Now all unneeded slots are empty
			  // Compute the still needed IDs
			  long[] neededIDs = getNeededIDs(uniqueIDs, cache);
			  
			  // Try to find replacements from within the other cache levels
			  moveUniqueIDsToLevel(neededIDs, level); 
			//  dumpCache("!@#$ moveUniqueIDsToLevel");
			  
			  // Re-compute the still needed IDs
			  neededIDs = getNeededIDs(uniqueIDs, cache);
			  
			  // Mark all the still needed IDs as NEED_TO_FETCH
			  markAsNeeded(neededIDs, cache);
			//  dumpCache("!@#$ markAsNeeded");
		  }
	  }
	  
	  /*
	   * Return a set of IDs of persons in cache at level 'cacheLevel' and state 'cacheEntryState'
	   * Does not call server
	   * Does not change cache
	   * 
	   * @param level - cache level
	   * @param cacheEntryState - cache state
	   * @return - set of IDs or null if there are no IDs
	   */
	  private Set<Long> getIdList(int level, CacheEntryState cacheEntryState) {
		  Set<Long> ids = new LinkedHashSet<Long>();
		  Misc.myAssert(CACHE_LEVEL_ANCHOR <= level && level < CACHE_LEVEL_SETTABLE_LEVELS);
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == cacheEntryState) {
				  long uniqueID = cache[i].getUniqueID();
				  if (uniqueID == PersonClient.UNIQUE_ID_NOT_FOUND) // Handle case where person has not been fetched yet.
					  uniqueID = cache[i].getRequestedUniqueID();
				  ids.add(uniqueID);
			  }
		  }
		  return ids;
	  }
	  /*
	   * Get the IDs for an array of entries
	   * @param entries - an array of cache entries
	   * @return the IDs of the cache entries
	   */
	  public static List<Long> getIdListForEntries(PersonClientCacheEntry[] entries) {
		  List<Long> ids = new ArrayList<Long>();
		  if (entries != null) {
			  for (int i = 0; i < entries.length; ++i) {
				  ids.add(entries[i].getUniqueID());
			  }
		  }
		  return ids;
	  }
	  
	  
	  /*
	   * Extract a list of incompletely fetched person from the fetches array returned by a server call
	   * @param fetches - persons fetched from server
	   * @param noDetail - return persons missing detail (full html)
	   * @param connectionInProgress - return persons with partially complete connections list
	   * @param targetLevels - return only these levels
	   * @return - list of incompletely fetched persons
	   */
	  static private List<IDsAtLevel> getIncompletePersonsIDs(PersonFetch[] fetches, 
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
	  
	  /*
	   * get IDs of cache entries with the specified state from the specified cache levels
	   * @param levels - list of cache levels to search
	   * @param cacheEntryState - cache state to match
	   * @return - list of IDs at the specified levels
	   */
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
	  
	  /*
	   * Change cache entry states at the specified level and state
	   * @param level - cache level to search
	   * @param oldState - cache state to match
	   * @param newState - cache state to change to
	   */
	  private void changeCacheStatesAtLevel(int level, CacheEntryState oldState, CacheEntryState newState) {
		  PersonClientCacheEntry[] cache = _theClientCache[level];
		  for (int i = 0; i < cache.length; ++i) {
			  if (cache[i].getState() == oldState) 
				  cache[i].setState(newState);
		  }
	  }
	  
	  /*
	   * Change cache entry states at the specified levels and state
	   * @param levels - cache levels to search
	   * @param oldState - cache state to match
	   * @param newState - cache state to change to
	   */
	  private void changeCacheStates(List<Integer> levels, CacheEntryState oldState, CacheEntryState newState) {
		   for (int i = 0; i < levels.size(); ++i) {
			  changeCacheStatesAtLevel(levels.get(i), oldState, newState);
		  }
	  }
	  
	  /*
	   * Queue calls to callServerToFetchPersons()
	   * 
	   */
	  class ServerFetchArgs {
		  List<IDsAtLevel> idsAtLevel;
		  long clientSequenceNumber;
		  int numCallsForThisClientSequenceNumber;
		  
		  public ServerFetchArgs(List<IDsAtLevel> idsAtLevel,
				  long clientSequenceNumber, int numCallsForThisClientSequenceNumber) {
			  this.idsAtLevel = idsAtLevel;
			  this.clientSequenceNumber = clientSequenceNumber;
			  this.numCallsForThisClientSequenceNumber = numCallsForThisClientSequenceNumber;
		  }
	  };
	  private Queue<ServerFetchArgs> _serverFetchArgsList = new LinkedList<ServerFetchArgs>();
	  private int _numServerCallsInProgress = 0;
	  private static int MAX_CALLS_IN_PROGRESS = 2;
	  
	  private void callServerToFetchPersons(List<IDsAtLevel> idsAtLevel,
			  long clientSequenceNumber, int numCallsForThisClientSequenceNumber) {
		  
		  SocialGraphExplorer.get().showInstantStatus2("callServerToFetchPersons", 
				  clientSequenceNumber + ":" + numCallsForThisClientSequenceNumber 
				  + ", " + _numServerCallsInProgress
				  + ", " + _serverFetchArgsList.size()); 
		  
		  ServerFetchArgs newArgs = new ServerFetchArgs(idsAtLevel,
			   clientSequenceNumber,  numCallsForThisClientSequenceNumber);
		  _serverFetchArgsList.offer(newArgs);
	
		  dispatchPendingFetches();
	 }
	  
	  private void dispatchPendingFetches() {
		  SocialGraphExplorer.get().showInstantStatus2("dispatchPendingFetches", "in");
		  while (_numServerCallsInProgress < MAX_CALLS_IN_PROGRESS  &&
				  _serverFetchArgsList.size() > 0 ) {
			  ServerFetchArgs workArgs = _serverFetchArgsList.remove();
			  callServerToFetchPersons_(workArgs.idsAtLevel,
					  workArgs.clientSequenceNumber, 
					  workArgs.numCallsForThisClientSequenceNumber);
			  ++_numServerCallsInProgress;
		  }
		  // All fetches will not necessarily be dispatches since _numServerCallsInProgress is limite
		 // Misc.myAssert(_serverFetchArgsList.size()==0);
		  Misc.myAssert(0 <=_numServerCallsInProgress && _numServerCallsInProgress <= MAX_CALLS_IN_PROGRESS);
		  SocialGraphExplorer.get().showInstantStatus2("dispatchPendingFetches", "out");
	  }
	  
	  private RequestsInProgress _requestsInProgress = new RequestsInProgress();
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
	   * 
	   * @param idsAtLevel - list of IDs and their levels to fetch from the server
	   * @param clientSequenceNumber - number of calls to fetchPersonsFromServer()
	   * @param numCallsForThisClientSequenceNumber - number of server calls for this clientSequenceNumber
	   */
	  private void callServerToFetchPersons_(List<IDsAtLevel> idsAtLevel,
			  long clientSequenceNumber, int numCallsForThisClientSequenceNumber) {
		  
		  SocialGraphExplorer.get().showInstantStatus2("callServerToFetchPersons_", 
				  clientSequenceNumber + ":" + numCallsForThisClientSequenceNumber 
				  + ", " + _numServerCallsInProgress + ":" + _serverFetchArgsList.size());
		  SocialGraphExplorer.get().log("callServerToFetchPersons", 
				  "[" + clientSequenceNumber + ":" + numCallsForThisClientSequenceNumber + "]"
				  + "(" + idsAtLevel.get(0).level + "): " + idsAtLevel.get(0).ids);
		  
		  // The actual call
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
	  			  					
	  			--_numServerCallsInProgress;
	  			Misc.myAssert(0 <=_numServerCallsInProgress && _numServerCallsInProgress <MAX_CALLS_IN_PROGRESS);
	  				  			
	  			SocialGraphExplorer.get().showInstantStatus2("accept", 
						  clientSequenceNumber + ":"  + _requestsInProgress.getNumCalls(clientSequenceNumber) 
						  + ", " + _numServerCallsInProgress + ":" + _serverFetchArgsList.size()
						  + ", " + requestedLevels
						  + ", " + (clientSequenceNumber <= _clientSequenceNumberCutoff)
						  + ", " + _numberOfCallbacksNeeded, true);
	  			SocialGraphExplorer.get().log("accept", clientSequenceNumber + ": "
	  					+ _requestsInProgress.getNumCalls(clientSequenceNumber) + " - "
	  					+ (fetches != null ? fetches.length : 0) + " fetches, " 
	  					+ requestedLevels + " levels, "
	  					+ PersonFetch.getFetchIDs(fetches));
	  			
	  			// Throw away old requests 
	  			if (clientSequenceNumber <= _clientSequenceNumberCutoff) {
	  				SocialGraphExplorer.get().log("Discarding old request", 
	  						  "clientSequenceNumber = " + clientSequenceNumber
	  						+ ", _clientSequenceNumberCutoff = " + _clientSequenceNumberCutoff);
	  				// Update UI if visible changes
	  				// or (in order to prevent hanging) if there are any outstanding callbacks
		  			if (hasVisibleLevel(requestedLevels) || _numberOfCallbacksNeeded > 0) {  //!@#$ Need to update on invisible fetches?
		  				//doCallback("accept - discarding"); !@#$ move to updateCacheAndGetVisible()
		  			}
	  			}
	  			else {
		  			recordCallDurations(fetches, callTime);
		  			// Update the cache!
		  			List<Long> misses = addToCache(fetches);
		  			if (misses.size() > 0) {
		  				SocialGraphExplorer.get().showError("Could not add " + clientSequenceNumber + "(misses = "  + misses + ") to cache" );
		  			}
		  			//dumpCache("accept");
		  			
		  			// Update UI if visible changes
		  			if (hasVisibleLevel(requestedLevels)) {  //!@#$ Need to update on invisible fetches?
		  				doCallback("accept");
		  			}
		  			
		  			// Fetch more persons if not all are fetched or some are incompletely, but first check the number of RPC calls for this request
		  			List<IDsAtLevel> unfetchedIDs = getIdLists(requestedLevels, CacheEntryState.PENDING);
		  			List<IDsAtLevel> incompletePersonIDsVisible   = getIncompletePersonsIDs(fetches, true, true, VISIBLE_CACHE_LEVEL_LIST); // !@#$ only for visible levels
		  			List<IDsAtLevel> incompletePersonIDsInvisible = getIncompletePersonsIDs(fetches, true, true, INVISIBLE_CACHE_LEVEL_LIST);
		  			List<IDsAtLevel> incompletePersonIDs = RPCWrapper.meldIDsAtLevelLists(incompletePersonIDsVisible, incompletePersonIDsInvisible);
		  			List<IDsAtLevel> idsToFetch = RPCWrapper.meldIDsAtLevelLists(unfetchedIDs, incompletePersonIDs);
		  			int numCallsForThisClientSequenceNumber = _requestsInProgress.getNumCalls(clientSequenceNumber);
		  			if (idsToFetch.size() > 0 && numCallsForThisClientSequenceNumber <= RequestsInProgress.MAX_SERVER_CALLS_PER_REQUEST) {
		  				SocialGraphExplorer.get().log("Incomplete fetch ", clientSequenceNumber 
		  						+ ": " + RPCWrapper.getTotalNumberOfIDs(unfetchedIDs) + " unfetched "
		  						+ "+ " + RPCWrapper.getTotalNumberOfIDs(incompletePersonIDsVisible) + " visible incomplete "
		  						+ "+ " + RPCWrapper.getTotalNumberOfIDs(incompletePersonIDsInvisible) + " invisible incomplete "
		  						+ "= " + RPCWrapper.getTotalNumberOfIDs(idsToFetch) + " total " 
		  						+ ", ids = " + idsToFetch.get(0).ids 
		  					);
		  				
		  				// Call server again from the response callback
		  				// The actual call!! hidden amongst logging code
		  				callServerToFetchPersons(idsToFetch, clientSequenceNumber, numCallsForThisClientSequenceNumber);
		  				
						_requestsInProgress.increment(clientSequenceNumber);
					}
	  			}
	  			// Keep working through the queue of pending fetches
	  			dispatchPendingFetches();
			}
	  		
	  		
	  	}
	  
	  /*
	   * Mark the round-trip durations in persons
	   * @param fetches - list of persons fetched from server
	   * @param callTime - the time the call was made
	   */
	  static private void recordCallDurations(PersonFetch[] fetches, double callTime) {
		  double duration = Statistics.getCurrentTime() - callTime;
		  if (fetches != null) {
			  for (PersonFetch fetch: fetches) {
				  fetch.person.setFetchDurationFull(Statistics.round1(duration));
			  }
		  }
	  }
	  
	  /*
	   * Tells whether levels are visible in UI
	   * @param levels - list of cache levels
	   * @return - true if any level is visible in UO
	   */
	  static private boolean hasVisibleLevel(List<Integer> levels) {
	  		Integer anchor = CACHE_LEVEL_ANCHOR;
	  		Integer visible = CACHE_LEVEL_VISIBLE;
	  		return (levels.contains(anchor) || levels.contains(visible));
	  	}
	  
	  	/*
	  	 * @return - all visible FILLED cache entries
		 */
	  	private PersonClientCacheEntry[][] getVisibleCacheEntries() {
	  		PersonClientCacheEntry[][] visibleCacheEntries = new PersonClientCacheEntry[2][];
  			visibleCacheEntries[CACHE_LEVEL_ANCHOR] = getFilledCacheEntriesAtLevel(CACHE_LEVEL_ANCHOR);
  			visibleCacheEntries[CACHE_LEVEL_VISIBLE] = getFilledCacheEntriesAtLevel(CACHE_LEVEL_VISIBLE);
  			Misc.myAssert(visibleCacheEntries[CACHE_LEVEL_ANCHOR] != null);
  			Misc.myAssert(visibleCacheEntries[CACHE_LEVEL_ANCHOR].length > 0);
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
			  return entries; 
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
					person.setCacheLevel(level);
					
					PersonClientCacheEntry[] cache = _theClientCache[level];
					int index = getIndexInCache(person, cache);
					if (index >= 0) {
						cache[index].setPerson(person);
						cache[index].setState(CacheEntryState.FILLED);
					}
					else {
						Misc.myAssert("Can't happen because clearPendingCacheEntries()" == null);
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
	  	static private int getIndexInCache(PersonClient person, PersonClientCacheEntry[] cache)  {
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
	  	 * Add neededIDs to cache and mark them as needed
	  	 */
	  	static private void markAsNeeded(long[] neededIDs, PersonClientCacheEntry[] cache) {
	  		List<Integer> emptyIndexes = getEmptyEntryIndexes(cache);
	  		Misc.myAssert(emptyIndexes.size() >= neededIDs.length);
	  		
	  		for (int i = 0; i < neededIDs.length; ++i) {
	  			PersonClientCacheEntry entry = cache[emptyIndexes.get(i)];
	  			entry.setRequestedUniqueID(neededIDs[i]);
	  			entry.setState(CacheEntryState.NEED_TO_FETCH);
	  		}
	  	}
	  	static private long[] getNeededIDs(long[] uniqueIDs, PersonClientCacheEntry[] cache) {
	  		 List<Long> prunedUniqueIDs = new ArrayList<Long>();
			  for (int i = 0; i < uniqueIDs.length; ++i) {
				  long id = uniqueIDs[i];
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
			  long[] neededIDs = MiscCollections.listToArrayLong(prunedUniqueIDs);
			  return neededIDs;
	  	}
	  	 /* !@#$ findInCacheAtOtherLevels() could be for only lower levels since unneeeded IDs are evicted
	  	  *      to the LRU level before this function is called!
		   * Move cache entries with specified IDs from other cache levels to specified level
		   * @param  neededIDs - the IDs that are needed at this cache level
		   * @param  level - the cache level the IDs are needed at
		   */
		  private void moveUniqueIDsToLevel(long[] neededIDs, int level) { 
			  PersonClientCacheEntry[] cache = _theClientCache[level];
			  List<Integer> emptyIndexes = getEmptyEntryIndexes(cache);
			  Misc.myAssert(emptyIndexes.size() >= neededIDs.length);
			  for (int i = 0; i < neededIDs.length; ++i) {
				  CacheIndex resident = findInCacheAtOtherLevels(neededIDs[i], level);  // Not at level
				  if (resident != null) {
					  cache[emptyIndexes.get(i)] = _theClientCache[resident.level][resident.index];
					  _theClientCache[resident.level][resident.index] = new PersonClientCacheEntry();
				//	  dumpCache("!@#$ moveUniqueIDsToLevel("+ level + ":" + emptyIndexes.get(i)+ ") " + i);
				  }
			  }
		  }
	  	/*
	  	 * Recycle 'numVictims' FILLED entries at cache level 'level'
	  	 * Move the entry to the recently used level and mark the entry's original place as EMPTY
	  	 */ 
	  	private void recycleUnneededIDs(long[] neededIDs, int level) {
	  		Misc.myAssert(level < CACHE_LEVEL_SETTABLE_LEVELS);
	  		PersonClientCacheEntry[] cache = this._theClientCache[level];
	  		Misc.myAssert(neededIDs.length <= cache.length);
	  
			for (int index = 0; index < cache.length; ++index) {
				PersonClientCacheEntry entry = cache[index];
  				if (entry.getState() != CacheEntryState.EMPTY && !MiscCollections.arrayContains(neededIDs, entry.getUniqueID())) {
  					copyToRecent(entry);
  					entry.setState(CacheEntryState.EMPTY);
  				}
  			} 
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
			  Misc.myAssert(entry != null);
			  if (entry.getState() == CacheEntryState.EMPTY) {
				  lru = i;
				  break;
			  }
		  }
		  // If no slots empty then find LRU slot
		  if (lru < 0) {
			  for (int i = 0; i < cache.length; ++i) {
				  entry = cache[i];
				  Misc.myAssert(entry != null);
				  if (entry.getState() == CacheEntryState.FILLED &&
					  entry.getLastReference() < oldestRef) {
					  oldestRef = entry.getLastReference();
					  lru = i;
				  }
			  }
		  }
	
		  Misc.myAssert(0 <= lru && lru < cache.length); 
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
	
	 /*
	  * Debug code
	  */
	  public void dumpCache(String location) {
		  dumpCache(location, false); 
	  }
	  
	  public void dumpCache(String location, boolean validate) {
		  if (OurConfiguration.VALIDATION_MODE) {
			  if (validate) {
				 validateCache();
			  }
		  }  
		  if (OurConfiguration.DEBUG_MODE) {
			  int numOccupied = 0;
			  String msg = "" ;
			  for (int level = 0; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
		  			PersonClientCacheEntry[] cache = _theClientCache[level];
		  			msg += "Level " + level + " [" + cache.length + "]: ";
		  			int index = 0;
			  		for (PersonClientCacheEntry cacheEntry: cache) {
				    	if (cacheEntry.getRequestedUniqueID() != PersonClient.UNIQUE_ID_NOT_FOUND) {
				    		msg +=  "" + index
										+   "{" + cacheEntry.getRequestedUniqueID()
								        +   "," + cacheEntry.getState()
								        +   "}, ";
				    		++numOccupied;
				    		++index;
				    	}
			  		}
			  		msg += ";; ";
			  }
			  SocialGraphExplorer.get().log("dumpCache(" + location + ")", msg);
			  System.err.println("dumpCache(" + location + ")" + ": " + msg);
		  }
	  }
	  
	  static private boolean anchor_has_been_filled = false;
	  private void validateCache() {
		  // Once anchor has been filled it has to stay filled
		  boolean gotAnchor = (_theClientCache[CACHE_LEVEL_ANCHOR][0] != null && _theClientCache[CACHE_LEVEL_ANCHOR][0].getPerson() != null);
		  if (!anchor_has_been_filled) {
			  if (gotAnchor)
				  anchor_has_been_filled = true;
		  }
		  else {
			  Misc.myAssert(gotAnchor);
		  }
	  }
	 
	  
	  /*
	   * Instrumentation
	   * Mark current cache levels
	   */
	  void markCacheLevels() {
		  for (int level = CACHE_LEVEL_VISIBLE_LEVELS; level < CACHE_LEVEL_NUMBER_LEVELS; ++level) {
			  for (int j = 0; j <_theClientCache[level].length; ++j) {
				  CacheEntryState state = _theClientCache[level][j].getState();
				  if (state == CacheEntryState.FILLED) {
					  Misc.myAssert(_theClientCache[level][j].getPerson() != null);
					 _theClientCache[level][j].getPerson().setCacheLevel(level);
					 Misc.myAssert(_theClientCache[level][j].getPerson().getCacheLevel() == level);
				  }
			  }
		  } 
	  }
	  
}

