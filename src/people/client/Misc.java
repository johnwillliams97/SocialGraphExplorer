
package people.client;

import com.google.gwt.user.client.Window;


/**
 * Odds and ends
 

 */
public class Misc {
	
	public static void myAssert(boolean condition) {
		if (!condition) {
			assert(condition);  // Add breakpoint here
			Window.alert("assertion failed!");
		}
	}
	
	public static void reportException (Exception e) {
		if (!OurConfiguration.SUPRESS_ERRORS) {
			e.printStackTrace();
		}
	}
	
}
