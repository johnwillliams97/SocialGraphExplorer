package people.client;

import com.google.gwt.user.client.rpc.AsyncCallback;


/**
 * The interface for the RPC server endpoint that provides people
 * information for clients that will be calling asynchronously. 
 */
public interface PersonServiceAsync {
	/*
	 * @param requestedUniqueIDs - IDs of persons to fetch
	 * @param levels - client cache levels of the requestedUniqueIDs
	 * @param clientSequenceNumber - Number of high-level requests from this client
	 * @param numCallsForThisClientSequenceNumber - Number of calls for this clientSequenceNumber
	 * @param sequenceNumber - Number of requests sent to server from this client
	 * @param callTime - Time function was called in seconds 
	 * @param payloadBytes - tells server to attach a payload of this size to each person record when running in ADD_FAKE_HTML mode
	 * @param callback - returns list of persons fetched from the data store
	 */
	void getPeople(long[] requestedUniqueIDs, int[] levels, 
			long clientSequenceNumber,	
			int numCallsForThisClientSequenceNumber,
			long sequenceNumber,		
			double callTime,
			int    payloadBytes,
		  AsyncCallback<PersonClientGroup> callback);
	
}
