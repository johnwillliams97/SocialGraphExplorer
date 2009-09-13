
package people.client;

import com.google.gwt.user.client.rpc.RemoteService;



/**
 * The interface for the RPC server end-point to get people
 * information.
 */
public interface PersonService extends RemoteService {
	/*
	 * @param requestedUniqueIDs - IDs of persons to fetch
	 * @param levels - client cache levels of the requestedUniqueIDs
	 * @param clientSequenceNumber - Number of high-level requests from this client
	 * @param numCallsForThisClientSequenceNumber - Number of calls for this clientSequenceNumber
	 * @param sequenceNumber - tracks client requests
	 * @param callTime - Time function was called in seconds
	 * @param urlArgs - pass some of the URL args along to the server
	 * @param payloadBytes - tells server to attach a payload of this size to each person record when running in ADD_FAKE_HTML mode
	 * @return list of persons fetched from the data store
	 */
	PersonClientGroup getPeople(long[] requestedUniqueIDs, int[] levels, 
				long clientSequenceNumber,	
				int  numCallsForThisClientSequenceNumber,
				long sequenceNumber,
				double callTime,
				int   payloadBytes);		
}
