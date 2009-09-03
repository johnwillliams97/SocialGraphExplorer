
package people.client;


/**
 * Odds and ends
 

 */
public class Misc {
	
	public static void myAssert(boolean condition) {
		if (!condition) {
			assert(condition);  // Add breakpoint here
		}
	}
	
	public static void reportException (Exception e) {
		if (!OurConfiguration.SUPRESS_ERRORS) {
			e.printStackTrace();
		}
	}
	
}
