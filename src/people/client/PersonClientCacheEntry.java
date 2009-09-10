package people.client;


/**
 * Client side cache entry for a PersonClient
 */
public class PersonClientCacheEntry {
	
	public enum CacheEntryState {
		EMPTY,				// Nothing there
		NEED_TO_FETCH,		// Has been requested of this cache
		PENDING, 			// This cache is fetching it from the server
		FILLED,				// Got it!
	};
	private CacheEntryState	state = CacheEntryState.EMPTY;
	
	private PersonClient    person = null;		// Actual value returned from server
	private long 			requestedUniqueID = PersonClient.UNIQUE_ID_NOT_FOUND;  // uniqueID of person to be requested from server
	private long            lastReference = 0L;  // For LRU
	static private long 	globalLastReference = 0L;	
	
	public CacheEntryState getState() {
		return this.state;
	}
	public void setState(CacheEntryState state) {
		this.state = state;
		if (this.state == CacheEntryState.EMPTY) { 
			// Clear all remnants for functions that act on requestedUniqueID etc
			this.person = null;
			this.requestedUniqueID = PersonClient.UNIQUE_ID_NOT_FOUND;
		}
	}
	
	public PersonClient getPerson() {
		return this.person;
	}
	public void setPerson(PersonClient person) {
		this.person = person;
	}
	
	public long getRequestedUniqueID() {
		return this.requestedUniqueID;
	}
	public void setRequestedUniqueID(long requestedUniqueID) {
		this.requestedUniqueID = requestedUniqueID;
	}
	
	public void touchLastReference() {
		this.lastReference = globalLastReference;
		++globalLastReference;
	}
	public long getLastReference() {
		return this.lastReference;
	}
	public long getUniqueID() {
		long uniqueID = PersonClient.UNIQUE_ID_NOT_FOUND;
		if (this.person != null) {
			uniqueID = this.person.getUniqueID();
		}
		return uniqueID;
	}	
}
