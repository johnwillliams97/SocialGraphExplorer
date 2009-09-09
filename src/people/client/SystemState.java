
package people.client;


/**
 * Global state
 */
public class SystemState {
	
	public static final String KEY_UNIQUEID   = "key";
	public static final String KEY_INDEX = "idx";
	private long _uniqueID = PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID;
	private int  _index = 0;
	
	public SystemState(String stateString) {
		parseStateString(stateString);
	}
	public SystemState(long uniqueID, int index) {
		_uniqueID = uniqueID;
		_index = index;
	}

	private static final String SEP_INTER = "&";
	private static final String SEP_INTRA = "=";
	
	public String getAsString() {
		String termUniqueID = KEY_UNIQUEID + SEP_INTRA + _uniqueID;
		String termIndex = KEY_INDEX + SEP_INTRA + _index;
		String stateString = termUniqueID + SEP_INTER + termIndex;
		return stateString;
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

}
