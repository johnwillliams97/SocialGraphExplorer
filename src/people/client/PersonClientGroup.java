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
	 
	// Timings in seconds
	public double responseDuration;		// Total
	public double responseDuration1;		// Milestone 1
	public double responseDuration2;
	public double responseDuration3;
	
	//Cache statistics
	public int  numCacheFetches;
	public int  numMemCacheFetches;
	public int  numDBCacheFetches;
	
	// Request tracking
	public long timeSignatureMillis;        // Used to distinguish servlets sending responses
	public long clientSequenceNumber = -1L;	// Number of high-level requests
	public int  numCallsForThisClientSequenceNumber = -1;  
	public long sequenceNumber = -1L;		// Number of requests sent to server
	public boolean hadDeadlineExceededException = false; // Timed out while serving response
	public double callTime = 0.0;			// Time when client called server
	
}
