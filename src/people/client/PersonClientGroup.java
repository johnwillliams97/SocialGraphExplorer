package people.client;



import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Hold relevant data for Person. This class is meant to be serialized in RPC
 * calls.
 */
public class PersonClientGroup  implements IsSerializable {
	
	public PersonFetch[] fetches;			// persons returned
	public int[]  requestedLevels;          // For matching to client cache refresh requests
	public long[] requestedUniqueIDs;		// persons requested
	 
	// Timings
	public long responseDuration;		// Total
	public long responseDuration1;		// Milestone 1
	public long responseDuration2;
	public long responseDuration3;
	
	//Cache stats
	public int  numCacheFetches;
	public int  numMemCacheFetches;
	public int  numDBCacheFetches;
	
	// Request tracking
	public long servletLoadTime;        // Used to distinguish servlets sending responses
	public long clientSequenceNumber = -1L;	// Number of high-level requests
	public int  numCallsForThisClientSequenceNumber = -1;  
	public long sequenceNumber = -1L;		// Number of requests sent to server
	public boolean hadDeadlineExceededException = false; // Timed out while serving response
	
}
