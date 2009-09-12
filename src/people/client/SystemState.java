
package people.client;


/**
 * State read from URL
 * 
 * This will be a superset of the CanonicalState
 */
public class SystemState {
	
	private static final String SEP_INTER = "&";
	private static final String SEP_INTRA = "=";
	
	public static final String KEY_VERBOSE  = "verbose";
	public static final String KEY_UNIQUEID = "key";
	public static final String KEY_INDEX    = "idx";
	public static final String KEY_MAX_SERVER_CALLS_PER_REQUEST  = "scr";
	public static final String KEY_MAX_REQUESTS_IN_PROGRESS      = "rip";
	public static final String KEY_MAX_SERVER_CALLS_IN_PROGRESS  = "cip";
	
	private boolean _verbose = false; // Verbosity of URL only so a private variable
	private long _uniqueID = PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID;
	private int  _index = 0;
	private int  _maxServerCallsPerRequest = OurConfiguration.MAX_SERVER_CALLS_PER_REQUEST;
	private int  _maxRequestsInProgress    = OurConfiguration.MAX_REQUESTS_IN_PROGRESS;
	private int  _maxServerCallsInProgress = OurConfiguration.MAX_SERVER_CALLS_IN_PROGRESS;
	
	public SystemState(long uniqueID, int index) {
		_uniqueID = uniqueID;
		_index = index;
	}

	public String getAsString() {
		String stateString = makeTerm(KEY_UNIQUEID, _uniqueID);
		stateString += addTerm(KEY_INDEX, _index);
		if (_verbose) {
			stateString += addTerm(KEY_MAX_SERVER_CALLS_PER_REQUEST, _maxServerCallsPerRequest);
			stateString += addTerm(KEY_MAX_REQUESTS_IN_PROGRESS,     _maxRequestsInProgress);
			stateString += addTerm(KEY_MAX_SERVER_CALLS_IN_PROGRESS, _maxServerCallsInProgress);
			stateString += addTerm(KEY_VERBOSE, _verbose);
		}
		return stateString;
	}
	
	private static String addTerm(String key, Object val)  {
		return SEP_INTER + makeTerm(key, val) ;
	}
	private static String makeTerm(String key, Object val)  {
		return key + SEP_INTRA + val;
	}
	
	private void parseStateString(String stateString) {
		if (stateString != null) {
			String[] terms = stateString.split(SEP_INTER);
			if (terms != null) {
				for (String term: terms) {
					String[] kv = term.split(SEP_INTRA);
					if (kv != null && kv.length >= 2) {
						String key = kv[0];
						String val = kv[1];
						if (key.equalsIgnoreCase(KEY_UNIQUEID))
							_uniqueID = Long.parseLong(val);
						else if (key.equalsIgnoreCase(KEY_INDEX))
							_index = Integer.parseInt(val);
					}
				}
			}
		}
	}
	
	public long getUniqueID() {
		return _uniqueID;
	}
	public int getIndex() {
		return _index;
	}
	public int getMaxServerCallsPerRequest() {
		return _maxServerCallsPerRequest;
	}
	public int getMaxRequestsInProgress() {
		return _maxRequestsInProgress;
	}
	public int getMaxServerCallsInProgress() {
		return _maxServerCallsInProgress;
	}
	
	public SystemState(String stateString) {
		parseStateString(stateString);
	}

}
