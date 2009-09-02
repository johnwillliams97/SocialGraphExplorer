
package people.client;


/**
 * Global configuration
 * 
 * Should be able to auto-configure test vs. production off "user.dir	 /base/data/home/apps/peterwilliams9797/4.334125800870706693"
 *   see http://peterwilliams9797.appspot.com/jsp_util_list_properties.jsp
 *   user.dir contains " /base/data/home/apps" => implies production
 */
public class OurConfiguration {

	// For RequestsInProgress
	public static final  int MAX_REQUESTS_IN_PROGRESS = 20;
	public static final  int MAX_SERVER_CALLS_PER_REQUEST = 9;
	
	// For Server PersonServiceImpl
	public static final double MAX_TIME_FOR_SERVLET_RESPONSE = 10.0; //  // 10,000 sec for debugging, 10 sec for production // %^&*

	// Debugging
	public static final boolean DEBUG_MODE = true;
	public static final boolean SHORT_LIST = true;
	public static final boolean VALIDATION_MODE = false;
	
	// For PersonList
	public static final  int VISIBLE_PERSONS_COUNT = SHORT_LIST ?  3 : 10; 
	public static final  int ANCHOR_HISTORY_COUNT  = SHORT_LIST ?  2 : 20;
	public static final  int CACHE_SIZE_LRU        = SHORT_LIST ? 5 : 200;
 
	// For ClientCache
	public static final int INVISIBLE_FETCH_DELAY_CLICK1 = 100;  	// msec
	public static final int INVISIBLE_FETCH_DELAY_CLICK2 = 200;		// msec

	

}
