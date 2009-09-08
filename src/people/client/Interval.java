package people.client;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
 * This class is used to setup the client cache for a specified CanonicalState
 * 
 * 	getAnchorAndConnectionsIDs() is the function that does this
 * 
 * An Interval is a contiguous set of indexes into an array  n0 <= index < n1
 */
class Interval {
	  private int n0;
	  private int n1;
	  
	  private Interval(int n0, int n1,  int max) {
		  assert(n1 >= n0);
		  assert(max > 0);
		  if (n0 < 0) 	 n0 = 0;
		  if (n1 > max)	 n1 = max;
		  if (n1 < n0)	 n1 = n0;
		  this.n0 = n0;
		  this.n1 = n1;
		  assert(this.n1 >= this.n0);
	  }
	  private int length() {
		  return (n1 - n0);
	  }
  	
	  /*
	   * Remove one interval from another
	   * @param removee - interval being removed from
	   * @param remover - interval to be removed from removee
	   * @return - resulting interval
	   */
	  static private Interval remove(Interval removee, Interval remover) {
		  // Fully enclosed => empty
		  if (remover.n0 <= removee.n0 && removee.n1 <= remover.n1)
			  removee.n1 = removee.n0;
		  // left end is intersected => move right
		  else if (remover.n0 <= removee.n0 && removee.n0 <= remover.n1)
			  removee.n0 = remover.n1;
		  // right end is intersected move left
		  else if (remover.n0 <= removee.n1 && removee.n1 <= remover.n1)
			  removee.n1 = remover.n0;
		  assert(removee.n1 >= removee.n0);
		  if (removee.n1 < removee.n0)
			  removee.n1 = removee.n0;
		  assert(removee.n1 >= removee.n0);
		  return removee;
	  }
  	
  	static private int[] intervalsToArray(Interval[] intervals) {
  		//System.err.println("intervalsToArray(" + intervals.length + " intervals)");
  		int len = 0;
  		for (int i = 0; i < intervals.length; i++) {
  			len += intervals[i].length();
  		}
  		//System.err.println("  array will have " + len + " elements)");
  		int[] out = null;
  		if (len > 0) {
		  out = new int[len];
	 	  int count = 0;
		  for (int i = 0; i < intervals.length; i++) {
			  for (int j = intervals[i].n0; j < intervals[i].n1; j++) {
				  out[count++] = j;
			  }
		  }
  		}
  		return out;
  	}
  	
  	static private long[] indexesToUniqueIDs(List<Long> connections, int[] indexes) {
	  //System.err.println("indexesToUniqueIDs(" + (indexes != null ? indexes.length : 0) + " indexes)");
	  long[] uniqueIDs = null;
	  if (indexes != null) {
		  assert(indexes.length <= connections.size());
		  uniqueIDs = new long[indexes.length];
		  for (int i = 0; i < indexes.length; ++i) {
			  uniqueIDs[i] = connections.get(indexes[i]);
		  }
		  System.err.println("  " + uniqueIDs.length + " IDs");
	  }
	  return uniqueIDs;
  	}
  	
 	/*
     * Get the set of visible and about to be visible IDs for the current UI state
     * 
     * Note. This depends on anchorUniqueID, startIndex from this UI state and 
     *       the anchor person having been fetched from the cache
     *       
     *  @param state - canonical state of UI     
     *  @param connectionIDs - list of IDs to predict caching for 
     *  @param rowsPerScreen - entries per screen of data
     *  @param cacheLevelSize - for checking that entries will fit
     *  @return - cache hints for this UI state
     */
  	public static long[][] getAnchorAndConnectionsIDs(CanonicalState state, 
	  				List<Long> connectionIDs,
	  				int rowsPerScreen,
		  			 int[] cacheLevelSize) {
  		 return  getAnchorAndConnectionsIDs2(state, connectionIDs, rowsPerScreen,  cacheLevelSize);
  	 }
  
 
    @SuppressWarnings("unused")
	static private long[][] getAnchorAndConnectionsIDs1(CanonicalState state, 
  		  		  				List<Long> connectionIDs,
  		  		  				int rowsPerScreen,
  		  		  			    int[] cacheLevelSize) {
  	  
  	  long[][] uniqueIDsList = new long[PersonClientCache.CACHE_LEVEL_SETTABLE_LEVELS][];
  	  uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR] = new long[1];
  	  uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR][0] = state.anchorUniqueID;
  	  
  	  int numConnections = 0;
  		  
  	  
  	  if (connectionIDs != null) {
  		  	numConnections = connectionIDs.size();
  		  
  			  /* Anchor = anchorUniqueID
  			   * Visible =  connections[<N>..<N>+VISIBLE_PERSONS_COUNT-2] N=startIndex
  			   * Visible+1= connections[<N>..<N>+VISIBLE_PERSONS_COUNT-1] 
  			   * 			N=0, N=numConnections-VISIBLE_PERSONS_COUNT,
  			   * 			N=startIndex+VISIBLE_PERSONS_COUNT
  			   * 			N=startIndex-VISIBLE_PERSONS_COUNT
  			   * Visible+2= connections[<N>..<N>+VISIBLE_PERSONS_COUNT-1] 
  			   * 			N=startIndex+2*VISIBLE_PERSONS_COUNT
  			   * 			N=startIndex-2*VISIBLE_PERSONS_COUNT
  			   */
  			
  			  
  			  /* 
  			   * Raw regions 
  			   * 	v=visible
  			   * 	c=start+end = 1 click
  			   * 	d=+-1 screen = 1 click
  			   * 	e=+-2 screens = 2 clicks
  			   */
  			  Interval v  = new Interval(state.startIndex, state.startIndex + rowsPerScreen, numConnections);
  			  Interval c1 = new Interval(0, rowsPerScreen, numConnections); 
  			  Interval c2 = new Interval(numConnections - rowsPerScreen, numConnections, numConnections); 
  			  Interval d1 = new Interval(state.startIndex - 2*rowsPerScreen, state.startIndex - 1*rowsPerScreen, numConnections); 
  			  Interval d2 = new Interval(state.startIndex + 1*rowsPerScreen, state.startIndex + 2*rowsPerScreen, numConnections); 
  			  Interval e1 = new Interval(state.startIndex - 3*rowsPerScreen, state.startIndex - 2*rowsPerScreen, numConnections); 
  			  Interval e2 = new Interval(state.startIndex + 2*rowsPerScreen, state.startIndex + 3*rowsPerScreen, numConnections); 
  			  
  			  // Remove intersections
  			  Interval[] intervals = { v, c1, c2, d1, d2, e1, e2 };
  			  for (int i = 1; i < intervals.length; ++i) {
  				  for (int j = 0; j < i; ++j) {
  					 intervals[i] = remove(intervals[i], intervals[j]) ;
  				  }
  			  }
  			//  System.err.println("<<< " + intervals.length + " intervals");
  			  Interval[] intervalVisible = {v};
  			  Interval[] intervalClick1  = { c1, c2, d1, d2 };
  			  Interval[] intervalClick2  = { e1, e2 };
  			  int[] idxVisible = intervalsToArray(intervalVisible);
  			  int[] idxClick1  = intervalsToArray(intervalClick1);
  			  int[] idxClick2  = intervalsToArray(intervalClick2);
  			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_VISIBLE]= indexesToUniqueIDs(connectionIDs, idxVisible);
  			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK1] = indexesToUniqueIDs(connectionIDs, idxClick1);
  			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK2] = indexesToUniqueIDs(connectionIDs, idxClick2);
  	  	}
  	  	return uniqueIDsList;
    }
   
    static public int getMaxIndex(List<Long> connectionIDs, int rowsPerScreen) {
    	int numConnections = connectionIDs != null ? connectionIDs.size() : 0;
    	return (numConnections > 0) ? ((numConnections-1)/rowsPerScreen)*rowsPerScreen : 0;
    }
   
    static private long[][] getAnchorAndConnectionsIDs2(CanonicalState state, 
	  				List<Long> connectionIDs,
	  				int rowsPerScreen,
		  			int[] cacheLevelSize) {
		int numConnections = connectionIDs != null ? connectionIDs.size() : 0;
		
		int i0  = state.startIndex;
		int i1b = Math.max(0, i0  - rowsPerScreen);
		int i2b = Math.max(0, i1b - rowsPerScreen);
		int i1f = Math.min(numConnections, i0  + rowsPerScreen);
		int i2f = Math.min(numConnections, i1f + rowsPerScreen);
		int i3f = Math.min(numConnections, i2f + rowsPerScreen);
		int j1  = Math.min(numConnections,       rowsPerScreen);
		int j2  = Math.min(numConnections, j1 +  rowsPerScreen);
		int k1  = getMaxIndex(connectionIDs, rowsPerScreen);
		int k2  = Math.max(0, k1 - rowsPerScreen);
		
		// conns<n> = n-click away list
		Set<Long> conns0 = new LinkedHashSet<Long>();
		Set<Long> conns1 = new LinkedHashSet<Long>();
		Set<Long> conns2 = new LinkedHashSet<Long>();
		
		// Populate lists
		if (connectionIDs != null) {
			conns0.addAll(MiscCollections.getSubList(connectionIDs, i0 , i1f-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			
			conns1.addAll(MiscCollections.getSubList(connectionIDs, i1b, i0-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns1.addAll(MiscCollections.getSubList(connectionIDs, i1f, i2f-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns1.addAll(MiscCollections.getSubList(connectionIDs,  0 , j1 -1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns1.addAll(MiscCollections.getSubList(connectionIDs, k1 , numConnections-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			
			conns2.addAll(MiscCollections.getSubList(connectionIDs, i2b, i1b-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns2.addAll(MiscCollections.getSubList(connectionIDs, i2f, i3f-1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns2.addAll(MiscCollections.getSubList(connectionIDs, j1 , j2 -1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
			conns2.addAll(MiscCollections.getSubList(connectionIDs, k2 , k1 -1));
			checkSizes(cacheLevelSize, conns0, conns1, conns2);
		}
		
		// conns0a = visible list + anchor
		Set<Long> conns0a = new LinkedHashSet<Long>(conns0);
		conns0a.add(state.anchorUniqueID);
		
		// Remove duplicates from lower priority lists
		MiscCollections.removeAll(conns1, conns0a);
		checkSizes(cacheLevelSize, conns0, conns1, conns2);
		MiscCollections.removeAll(conns2, conns0a);
		checkSizes(cacheLevelSize, conns0, conns1, conns2);
		MiscCollections.removeAll(conns2, conns1);
		checkSizes(cacheLevelSize, conns0, conns1, conns2);
		
		// Convert to required output format  
		long[][] uniqueIDsList = new long[PersonClientCache.CACHE_LEVEL_SETTABLE_LEVELS][];
	  	uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR] = new long[1];
	  	uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR][0] = state.anchorUniqueID;
	  	uniqueIDsList[PersonClientCache.CACHE_LEVEL_VISIBLE]= MiscCollections.listToArrayLong(conns0);
		uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK1] = MiscCollections.listToArrayLong(conns1);
		uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK2] = MiscCollections.listToArrayLong(conns2);
	  	
		return uniqueIDsList;
	}
	
	private static void checkSizes( int[] cacheLevelSize,
			Set<Long> conns0,
			Set<Long> conns1,
			Set<Long> conns2) {
		if (OurConfiguration.VALIDATION_MODE) {
			Misc.myAssert(conns0.size() <= cacheLevelSize[PersonClientCache.CACHE_LEVEL_VISIBLE], 
					"checkSizes(CACHE_LEVEL_VISIBLE) " + conns0.size() + ", " + cacheLevelSize[PersonClientCache.CACHE_LEVEL_VISIBLE]);
			Misc.myAssert(conns1.size() <= cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK1], 
					"checkSizes(CACHE_LEVEL_CLICK1) " + conns1.size() + ", " + cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK1]);
			Misc.myAssert(conns2.size() <= cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK2], 
					"checkSizes(CACHE_LEVEL_CLICK2) " + conns2.size() + ", " + cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK2]);
		}
	}
	
}
