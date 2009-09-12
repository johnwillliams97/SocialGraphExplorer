
package people.client;


/**
 * Global configuration
 * 
 */
public class OurConfiguration {

	// Debugging. All flags == false => release mode.
	public static final boolean DEBUG_MODE = false;
	public static final boolean SHORT_LIST = true;
	public static final boolean VALIDATION_MODE = true;
	public static final boolean NO_SERVER_TIME_LIMIT = false;
	public static final boolean SUPRESS_ERRORS = false;
	public static final boolean SHORT_SERVER_RESPONSE = false;
	public static final int     HTML_DATA_MAX_SIZE = 2*1024;
	public static final boolean ADD_FAKE_HTML = true;
	
	// Determines the system's starting state. null => default
	// Handy for debugging
	public static final String INITIAL_UI_STATE = "key=100idx=0";	// "key=8050idx=110";
	// Params with bugs
	// key=5009idx=110, key=8050idx=110
	
	// All unique IDs below this are reserved.
	public static final long MINIMUM_UNIQUEID = 100L;
	
	// Control extent of uniqueIDs to shape data behaviour
	public static final long MAXIMUM_UNIQUEID = 9999L;
	
	// For RequestsInProgress
	public static final  int MAX_REQUESTS_IN_PROGRESS = 20;
	public static final  int MAX_SERVER_CALLS_PER_REQUEST = SHORT_SERVER_RESPONSE ? 3 : 9;
	
	// Set to max # async calls allowed by browser
	public static final  int MAX_SERVER_CALLS_IN_PROGRESS = 2;
	
	// For Server PersonServiceImpl
	public static final double MAX_TIME_FOR_SERVLET_RESPONSE = NO_SERVER_TIME_LIMIT ? 1000.0 : 10.0; // seconds

	// For PersonList
	public static final  int VISIBLE_PERSONS_COUNT = SHORT_LIST ?  3 : 11; 
	public static final  int CACHE_SIZE_LRU        = SHORT_LIST ?  5 : 200;
	
 


}
