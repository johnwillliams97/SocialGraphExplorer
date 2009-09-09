
package people.client;


/**
 * Global state
 */
public class SystemState {
	
	public static final String KEY_UNIQUEID   = "key";
	public static final String KEY_INDEX = "idx";
	private long _uniqueID = 0L;
	private int  _index = 0;
	
	public SystemState(String stateString) {
		parseStateString(stateString);
	}
	public SystemState(long uniqueID, int index) {
		this._uniqueID = uniqueID;
		this._index = index;
	}

	private static final String SEP_INTER = "&";
	private static final String SEP_INTRA = "=";
	
	public String getAsString() {
		String termUniqueID = KEY_UNIQUEID + SEP_INTRA + this._uniqueID;
		String termIndex = KEY_INDEX + SEP_INTRA + this._index;
		String stateString = termUniqueID + SEP_INTER + termIndex;
		return stateString;
	}
	private void parseStateString(String stateString) {
		String[] terms = stateString.split(SEP_INTER);
		if (terms != null) {
			for (String term: terms) {
				String[] kv = term.split(SEP_INTRA);
				if (kv != null && kv.length >= 2) {
					String key = kv[0];
					String val = kv[1];
					if (key.equalsIgnoreCase(KEY_UNIQUEID))
						this._uniqueID = Long.parseLong(val);
					else if (key.equalsIgnoreCase(KEY_INDEX))
						this._index = Integer.parseInt(val);
				}
			}
		}
	}
	
	public long getUniqueID() {
		return this._uniqueID;
	}
	public int getIndex() {
		return this._index;
	}

}
