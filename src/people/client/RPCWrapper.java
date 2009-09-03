package people.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.gwt.user.client.rpc.ServiceDefTarget;


/*
 * !@#$ Implement time-outs 
 * 		http://code.google.com/webtoolkit/doc/1.6/DevGuideCodingBasics.html#DevGuideDeferredCommand
 */
public class RPCWrapper {

	// The RPC service !
 	private final PersonServiceAsync personService;
	
 	// Various statistics
 	private long sequenceNumber = 0L;  // Identifies server requests
	private long baseServletLoadTime = -1L;  // Used to shorten long times. 
	private int  totalFetches = 0; // Total # person fetched from server
		  	
	// Keep a list of servlet instance called. 
	// Each servlet instance is identified by its start time, which should be somewhat unique.
  	private static final int maxServletInstances = 200;
  	private List<Long> servletLoadTimeInstances = new ArrayList<Long> ();
  	private int getNumServlets() {
  		return servletLoadTimeInstances.size();
  	}
  	private int getServletIndex(long servletLoadTime) {
  		int index = -1;
  		for (int i = 0; i < servletLoadTimeInstances.size(); ++i) {
  			if (servletLoadTimeInstances.get(i) == servletLoadTime) {
  				index = i;
  				break;
  			}
  		}
  		// No match so add the new time to the end of the list, if space
  		if (index < 0 && servletLoadTimeInstances.size() < maxServletInstances) {
  			servletLoadTimeInstances.add(servletLoadTime);
  			index = servletLoadTimeInstances.size() -1;
  		}
  		// No space?
  		if (index < 0) {
  			index = -3;  // Means no match!
  		}
  		return index;	
  	}
   
  	/*
  	 * Public interfaces
  	 */
    public RPCWrapper() {
    	// Initialise the service.
    	personService = (PersonServiceAsync) GWT.create(PersonService.class);

      // By default, we assume we'll make RPCs to a servlet,
    	ServiceDefTarget target = (ServiceDefTarget) personService;

      // Use a module-relative URL to ensure that this client code can find
      // its way home, even when the URL changes (as might happen when you
      // deploy this as a webapp under an external servlet container). 
    	String moduleRelativeURL = GWT.getModuleBaseURL() + "persons";
    	target.setServiceEntryPoint(moduleRelativeURL);
    }
	/*
	 * Callback triggered on server responses	    
	 */
	public interface PersonsAcceptor {
		public void accept(List<Integer> uniqueRequestedLevels, PersonFetch[] fetches, long clientSequenceNumber, double callTime);
	};
	
	/*
	 * Map L[N], R[N][] => L'[M]
	 * There is one uniqueLevel for each  requestedUniqueIDs[] array
	 * This function returns one level for each requestedUniqueIDs[][] element
	 * Inverse of getUniqueLevels
	 */
	/*
	private int[] getAllLevels(List<Integer> uniqueLevels, long[][] requestedUniqueIDs) {
		int[] allLevels = new int[MiscCollections.sizeOfArray(requestedUniqueIDs)];
		int   index = 0;
		for (int i = 0; i < uniqueLevels.size(); ++i) {
			int level = uniqueLevels.get(i);
			long[] ids = requestedUniqueIDs[i];
			for (int j = 0; j < ids.length; ++j) {
				allLevels[index++] = level;
			}
		}
		return allLevels;
	}
	*/
	/*
	 * Returns unique values in allLevels
	 * Inverse of getAllLevels
	 */
	private List<Integer>  getUniqueLevels(int[] allLevels) {
		List<Integer> uniqueLevels = new ArrayList<Integer>();
		if (allLevels != null) {
			for (int i = 0; i < allLevels.length; ++i) {
				if (!uniqueLevels.contains(allLevels[i]))
					uniqueLevels.add(allLevels[i]);
			}
		}
		return uniqueLevels;
	}
	
	public static class IDsAtLevel {
		  int       level;
		  Set<Long> ids;
		  public IDsAtLevel(int level, Set<Long> ids) {
			  this.level = level;
			  this.ids = ids;
		  }
	}
	private static final Set<Long> EMPTY_LONG_SET = new LinkedHashSet<Long>();
	private static Set<Long> getIdsForLevel(List<IDsAtLevel> idsAtLevelList, int level) {
		Set<Long> ids = EMPTY_LONG_SET;
		for (IDsAtLevel idsAtLevel: idsAtLevelList) {
			if (idsAtLevel.level == level) {
				ids = idsAtLevel.ids;
				break;
			}
		}
		return ids;
	}
	public static List<IDsAtLevel> meldIDsAtLevelLists(List<IDsAtLevel> list1, List<IDsAtLevel> list2) {
		Set<Integer> allLevels = new LinkedHashSet<Integer>();
		for (IDsAtLevel idsAtLevel: list1) {
			allLevels.add(idsAtLevel.level);
		}
		for (IDsAtLevel idsAtLevel: list2) {
			allLevels.add(idsAtLevel.level);
		}
		List<IDsAtLevel> meld = new ArrayList<IDsAtLevel>();
		for (Integer level: allLevels) {
			Set<Long> ids = new LinkedHashSet<Long>();
			ids.addAll(getIdsForLevel(list1, level));
			ids.addAll(getIdsForLevel(list2, level));
			meld.add(new IDsAtLevel(level, ids));
		}
		return meld;
	}
	
	public static int getTotalNumberOfIDs(List<IDsAtLevel> idsAtLevelList) {
		int total = 0;
		for (IDsAtLevel idsAtLevel: idsAtLevelList)
			total += idsAtLevel.ids.size();
		return total;
	}
	public static long[] getArrayOfIDs(List<IDsAtLevel> idsAtLevelList) {
		List<Long> idsList = new ArrayList<Long>();
		for (IDsAtLevel idsAtLevel: idsAtLevelList) {
			for (Long id: idsAtLevel.ids) {
				idsList.add(id);
			}
		}
		return MiscCollections.listToArrayLong(idsList);
	}
	public static int[] getArrayOfLevels(List<IDsAtLevel> idsAtLevelList) {
		List<Integer> levelsList = new ArrayList<Integer>();
		for (IDsAtLevel idsAtLevel: idsAtLevelList) {
			for (int i = 0; i < idsAtLevel.ids.size(); ++i) {
				levelsList.add(idsAtLevel.level);
			}
		}
		return MiscCollections.listToArrayInt(levelsList);
	}
	
	
	/*
	 * Fetch the requested uniqueIDs from the server
	 * 
	 * This is a best effort fetch. Caller should check results and call again
	 * until fetching is complete.
	 * 
	 * @param acceptor - Class that writes the updated data on the screen
	 * @param requestedUniqueIDs - IDs to fetch
	 * @param description - for debugging
	 */
	private PersonsAcceptor theAcceptor; // !@#$ crappy global variable
//	private List<Integer>   theLevels;   // !@#$ crappy global variable
	// !@#$ Replace  levels + requestedUniqueIDs with an {level+uniuqIDs[]}[]
	public void getPersonsFromServer(final PersonsAcceptor acceptor, 
	    							 List<IDsAtLevel> idsAtLevelList,
	    							 long clientSequenceNumber,
	    							 int numCallsForThisClientSequenceNumber,
	    							 String description) {
	    
    	this.theAcceptor = acceptor;
    //	this.theLevels = levels;
    	
    	if (idsAtLevelList.size() > 0) {
	       	System.err.println("getPersonsFromServer(" + description + ", " + getTotalNumberOfIDs(idsAtLevelList) + ") " + MiscCollections.arrayToString(getArrayOfIDs(idsAtLevelList)));
	       	SocialGraphExplorer.get().log("getPersonsFromServer", description + ", " + getTotalNumberOfIDs(idsAtLevelList) + ": " + MiscCollections.arrayToString(getArrayOfIDs(idsAtLevelList)));
	       	// Fetch the data remotely.
		    // This is an async call that returns immediately 
	    	// Do a best-effort (time limited) fetch. If partial list is returned then 
	       	// keep calling server and get more partial lists until whole list is returned
	        ++this.sequenceNumber;
	        personService.getPeople(
	        	getArrayOfIDs(idsAtLevelList),
	        	getArrayOfLevels(idsAtLevelList),
	        	clientSequenceNumber,
	        	numCallsForThisClientSequenceNumber,
	        	this.sequenceNumber,
	        	Statistics.getCurrentTime(),
	        	new AsyncCallback<PersonClientGroup>() {
	        		public void onFailure(Throwable caught) {
	        			onFailureCallback(caught);
			    	}
			    	public void onSuccess(PersonClientGroup result) {
			    		onSuccessCallback(result);
			    	}
			    }
		    );
	    }
    }
	
	// Longest call to server	
	private static double _maxDuration  = 0;
    /*
     * Callback that gets called when async call to server completes on server.
     *  theAcceptor.accept() keeps calling this function until the client cache is up to date
     */
    private void onSuccessCallback(PersonClientGroup result) {
	
		List<Integer> uniqueLevels = getUniqueLevels(result.requestedLevels);
		
		// Log a bunch of statistics
		onSuccessCallbackReportStatus(result, uniqueLevels) ;
		
		// Update the cache !!!! , call back to UI 
		// getPersonsFromServer() will get called again if data incomplete
		this.theAcceptor.accept(uniqueLevels, result.fetches, result.clientSequenceNumber, result.callTime);
 	}

    
    
    /*
     * Report some status. Not critical
     */
    private void onSuccessCallbackReportStatus(PersonClientGroup result, List<Integer> uniqueLevels ) {
    	// Verify some assumptions
    	Misc.myAssert(result != null);
    	//myAssert(result.requestedLevels != null);
    	//myAssert(result.requestedUniqueIDs != null);
    	//myAssert(result.fetches != null);
    	
    	// House keeping
    	if (baseServletLoadTime < 0)	
			baseServletLoadTime = result.timeSignatureMillis;
		int numFetches = result.fetches != null ? result.fetches.length : 0;
		totalFetches += numFetches;
		int numRequestedIDs = (result.requestedUniqueIDs != null) ? result.requestedUniqueIDs.length : 0; //!@#$ use empty collections instead of null
		//SocialGraphExplorer.get().showInstantStatus2("onSuccessCallback", numFetches + " of " + numRequestedIDs);
		
		if (result.requestedUniqueIDs == null) {
			SocialGraphExplorer.get().showError("Bad result from server: result.requestedUniqueIDs == null");
		}
		else if (result.fetches == null) {
			SocialGraphExplorer.get().showError("Bad result from server: result.fetches == null");
		}
		else { // If this is a valid response
			if (numFetches < result.requestedUniqueIDs.length)
				SocialGraphExplorer.get().showInstantStatus2("Incomplete fetch", numFetches + " of " + numRequestedIDs);
			
			// Track longest server response 
			if (_maxDuration < result.responseDuration)
				_maxDuration = result.responseDuration;
			
			// Report status  - for debugging
			String uniqueLevelsString = uniqueLevels.toString();
			SocialGraphExplorer.get().showStatus("response" ,
					+ result.clientSequenceNumber + ":"
					+ result.numCallsForThisClientSequenceNumber 
					+ " (" + result.sequenceNumber + ")"
					+ " Levels: " + uniqueLevelsString
					+ " <" + result.requestedUniqueIDs.length + " requested, " + numFetches + " fetched> "
					+ "(" + totalFetches + " total) "
					+ " (servlet " + getServletIndex(result.timeSignatureMillis - baseServletLoadTime) 	+ " of " + getNumServlets() +")" 
				//	+ arrayToString(result.requestedUniqueIDs)
					);
			SocialGraphExplorer.get().showStatus("Fetches", 
					+ result.fetches.length + ", "
					+ result.numCacheFetches + " ("
					+ result.numMemCacheFetches + " mem, "
					+ result.numDBCacheFetches+ " DB) for "
					//+ getIDsAsString(result.persons)
					);
			SocialGraphExplorer.get().showStatus("Duration", 
					+ result.responseDuration1 + ", "
			 		+ result.responseDuration2 + ", "
			 		+ result.responseDuration3 + ", "
			 		+ result.responseDuration  + ": "
			 		+ _maxDuration );
			SocialGraphExplorer.get().showStatus("IDs", "" + result.getFetchedIDs() );
		}
		
		SocialGraphExplorer.get().statusFlush();
				
		//personCache.dumpCache();hinted PersonsInCache
    }
  	
	/*
	 * Utility functions
	 */
  /*  
	
	*/
 	/*
 	private static String getIDsAsString(PersonLIClient[] persons) {
 		long[] ids = null;
 		if (persons != null)  {
 			int numPersons = persons.length;
 			ids = new long[numPersons];
 			for (int i = 0; i < numPersons; ++i)
 				ids[i] = persons[i].getLiUniqueID();
 		}
 		return arrayToString(ids);
 	}
 	*/
 	
	/*
	 * Error handling fetchPersonsFromServer
	 */
	private void onFailureCallback(Throwable caught) {
    	String header = "Error";
		String body = "";
		if (caught instanceof InvocationException) {
			header = "An RPC server could not be reached";
    		body = NO_CONNECTION_MESSAGE;
    	} 
		else   	{
			header = "Unexcepted Error processing remote call";
			body = caught.getMessage();
    	}
		String msg = header + ":\n" + body;
		SocialGraphExplorer.get().log(header, body);
    	Window.alert(msg);
    }
  
		 		
  	private static final String NO_CONNECTION_MESSAGE = 
  			"This program uses a Remote Procedure Call "
  	      + "(RPC) to request data from the server.  In order for the RPC to "
  	      + "successfully return data, the server component must be available.";
}