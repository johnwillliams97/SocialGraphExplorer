package cache;
/*
 * Wrapper for DB access
 */
import java.util.Calendar;
import java.util.logging.Logger;

import misc.Statistics;

import htmlstuff.WebSiteReader_EntryPoint;
//import htmlstuff.WebSiteReader_Quota; indexesToUniqueIDs

import datatypes.PersonLI;

public class CacheDB implements CacheActual<Long, PersonLI> {
	private static final Logger logger = Logger.getLogger(CacheActual.class.getName());
	
	private static String 		LINKED_IN = "LinkedIn";
	private WebSiteReader_EntryPoint 	webSiteReaderEntry = new WebSiteReader_EntryPoint();
	private long 				longestLIReadDuration = 0L; // For profiling
	
	private static boolean isNullOrEmpty(String s) {
		return s == null || s.isEmpty();
	}
	private PersonLI getFromDBandLI(Long key, boolean doCheckLI, WebReadPolicy policy, long timeBoundMillis) {
		
		PersonLI 	person = PersonLI.findInDBbyUniqueId(key);
		boolean 	needsLIRead = false;
		
		Statistics.getInstance().recordEvent("CacheDB.getFromDBandLI()");
		if (doCheckLI && policy != WebReadPolicy.NEVER) {
			if 	(person == null) {
				needsLIRead = true;
			}
			// Check for mal-formed records. These come from early versions of the program
			else if (isNullOrEmpty(person.getNameFull()) ||
					isNullOrEmpty(person.getDescription()) ||
					isNullOrEmpty(person.getLocation())	 ) {
				needsLIRead = true;
				person = null;
			}
			// Partially read states
			else if (person.getIsChildConnectionInProgress() || person.getHtmlPage() == null) { 
				needsLIRead = true;
				logger.info("Re-read" + person.getNameFull() + " from LI");
			}
	
			if (needsLIRead) {
				long before = Calendar.getInstance().getTimeInMillis();
				Statistics.getInstance().recordEvent("getPersonFromLI");
				
				// Actual website read is hidden amongst the instrumentation
				PersonLI personRead = webSiteReaderEntry.getPersonFromLI(key, person, policy == WebReadPolicy.ALWAYS, timeBoundMillis);
				
				long after = Calendar.getInstance().getTimeInMillis();
				long duration = after - before;
				this.longestLIReadDuration = Math.max(duration, this.longestLIReadDuration);
				logger.info("LinkeIn read took " + duration/1000L + " (longest = " + this.longestLIReadDuration/1000L + ")");
				Statistics.getInstance().recordEvent("done getPersonFromLI");
				if (personRead != null) {
					personRead.setWhence(LINKED_IN);
					person = personRead;
					person.saveToDB();
					Statistics.getInstance().recordEvent("Saved " + person.getNameFull() + " to DB");
					// !@#$ check
					PersonLI 	person2 = PersonLI.findInDBbyUniqueId(key);
					boolean personHasHtml  = (person != null  && person.getHtmlPage() != null);
					boolean person2HasHtml = (person2 != null && person2.getHtmlPage() != null);
					assert(person2HasHtml == personHasHtml);
					String employer1 = person != null  ? person.getEmployer()  : null;;
					String employer2 = person2 != null ? person2.getEmployer() : null;
					assert((employer1 == null && employer2 == null) ||
						   (employer1 != null && employer2 != null && employer1.equals(employer2)));
				}
			}
		}
		
		logger.info(key + ": " + (person != null ? person.getNameFull() : "not found")); 
				
		return person;
	}
	
/*	@Override
	public boolean containsKey(Long key,long timeBoundMillis) {
		Statistics.getInstance().recordEvent("CacheDB.containsKey()");
		return (this.getFromDBandLI(key, false, WebReadPolicy.AUTO) != null);
	} 
*/
	@Override
	public PersonLI get(Long key, WebReadPolicy policy, long timeBoundMillis) {
		PersonLI person = this.getFromDBandLI(key, true, policy, timeBoundMillis);
		
		//String nameFull = person != null ? person.getNameFull() : "not found";
		//logger.warning(key + ":" + nameFull);
		
		return person;
	}
	
	@Override
	public void put(Long key, PersonLI value) {
		assert(key == value.getLiUniqueID());
		value.saveToDB();
	}

	@Override
	public String identify() {
		return CacheDB.class.getSimpleName();
	}

	@Override
	public boolean isIncomplete(PersonLI person) {
		boolean incomplete = false;
		if (person != null)
			incomplete = person.getIsChildConnectionInProgress() || person.getHtmlPage() == null;
		return incomplete;
	}
	
	@Override
	public PersonLI setWhence(PersonLI person) {
		logger.info("setWhence("+this.identify()+") - " + (person != null ? person.getNameFull() : "not found"));
		if (person != null) {
			String whence = person.getWhence();
			if (whence == null) {
				person.setWhence(this.identify());
			}
		}
		return person;
	}
}

