
package people.client;


/**
 * Global configuration
 * 
 * Should be able to auto-configure test vs. production off "user.dir	 /base/data/home/apps/peterwilliams9797/4.334125800870706693"
 *   see http://peterwilliams9797.appspot.com/jsp_util_list_properties.jsp
 *   user.dir contains " /base/data/home/apps" => implies production
 */
public class OurConfiguration {

	// Debugging. All flags == false => release mode.
	public static final boolean DEBUG_MODE = true;
	public static final boolean SHORT_LIST = false;
	public static final boolean VALIDATION_MODE = true;
	public static final boolean NO_SERVER_TIME_LIMIT = false;
	public static final boolean SUPRESS_ERRORS = true;
	public static final boolean SHORT_SERVER_RESPONSE = false;
	
	// Determines the system's starting state. null => default
	// Handy for debugging
	public static final String INITIAL_UI_STATE = "1814285,10";
	
	// For RequestsInProgress
	public static final  int MAX_REQUESTS_IN_PROGRESS = 20;
	public static final  int MAX_SERVER_CALLS_PER_REQUEST = SHORT_SERVER_RESPONSE ? 3 : 9;
	
	// For Server PersonServiceImpl
	public static final double MAX_TIME_FOR_SERVLET_RESPONSE = NO_SERVER_TIME_LIMIT ? 1000.0 : 10.0; // seconds

	// For PersonList
	public static final  int VISIBLE_PERSONS_COUNT = SHORT_LIST ?  3 : 11; 
	public static final  int CACHE_SIZE_LRU        = SHORT_LIST ?  5 : 200;
 
	// For ClientCache
	public static final int INVISIBLE_FETCH_DELAY_CLICK1 = 100;  	// msec
	public static final int INVISIBLE_FETCH_DELAY_CLICK2 = 200;		// msec
	
	 
	

}
