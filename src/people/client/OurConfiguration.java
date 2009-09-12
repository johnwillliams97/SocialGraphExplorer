
package people.client;


/**
 * Global configuration
 * 
 */
public class OurConfiguration {

	// Debugging. All flags == false => release mode.
	public static final boolean DEBUG_MODE = true;
	public static final boolean SHORT_LIST = true;
	public static final boolean VALIDATION_MODE = false;
	public static final boolean NO_SERVER_TIME_LIMIT = false;
	public static final boolean SUPRESS_ERRORS = false;
	public static final boolean SHORT_SERVER_RESPONSE = false;
	public static final int     HTML_DATA_MAX_SIZE = 4*1024;
	public static final boolean ADD_FAKE_HTML = true;
	
	// Controls the number of synthetic connections created. This is an approximate value 
	// due to the complexity of the of the synthesis code
	public static final long NUMBER_SYNTHECTIC_CONNECTIONS = 500L;
	// Determines the system's starting state. null => default
	// Handy for debugging
	// Default is "key=101idx=110" which is me
	public static final String INITIAL_UI_STATE = "key=101&idx=0";	
	// Params with bugs
	// key=5009&idx=110, key=8050&idx=110
	
	// All unique IDs below this are reserved.
	public static final long MINIMUM_UNIQUEID = 100L;
	
	// Control extent of uniqueIDs to shape data behaviour
	public static final long MAXIMUM_UNIQUEID = 99999L;
	
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
	
	// Configure server cache pipepline to use memcache
	public static final boolean USE_MEMCACHE = true;

	
 


}
