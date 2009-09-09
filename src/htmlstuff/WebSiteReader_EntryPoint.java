package htmlstuff;

import people.client.OurConfiguration;
import people.client.Statistics;
import datatypes.PersonLI;

public class WebSiteReader_EntryPoint {
	/*
	 * Error handling
	 */
	String _errorMsg = null;
	
	/* Read information from http://www.linkedin.com
	 * This function does a single web page fetch as part of a sequence of page fetches.
	 * - partPerson has the part state from the previous page fetch
	 */

	public PersonLI getPersonFromLI(
				long     key,
				PersonLI partPerson,    // whatever we got in previous fetches, null if no prev fetches
				boolean  forceLiRead,	// forces a LI read
				double timeBoundSec) {
		PersonLI person = null;
		if (OurConfiguration.ALLOW_LINKED_READS)
			person = getPersonFromLI_(key, partPerson, forceLiRead, timeBoundSec);
		return person;
	}
	
	@SuppressWarnings("unused")
	private boolean _debug_scrape = false;
	
	private PersonLI getPersonFromLI_(
				long     key,
				PersonLI partPerson,    // whatever we got in previous fetches, null if no prev fetches
				boolean  forceLiRead,	// forces a LI read
				double timeBoundSec)	// request must complete before this time
	{
		WebSiteReader_Common.startNewHttpRequest();

		String  htmlPage = null;
		
		PersonLI person = partPerson;
		ConnectionsReaderState  connectionsReaderState = null;

		// First fetch of this person: Get summary
		if (person == null || // Need to read
		   (forceLiRead && person != null && !person.getIsChildConnectionInProgress()) // Forced to read
		   ) {
			
			person = WebSiteReader_UserProfile.doGetLiUserProfile(key, timeBoundSec);
			Statistics.getInstance().recordEvent("getPersonFromLI: Got summary");
		}
		// Second fetch of this person: Get details
		else if (person != null && person.getHtmlPage() == null) {
			htmlPage = WebSiteReader_UserProfile.doGetLiUserProfilePage(key, timeBoundSec);
			Statistics.getInstance().recordEvent("getPersonFromLI: Got details");
			person.setHtmlPage(htmlPage);
		}
		// Third or later fetch of this person: Get connections
		else if (person != null && person.getIsChildConnectionInProgress()) {
			for (int connType = 0; connType < person.getNumChildConnectionProgresses(); connType++) {
				if (person.getChildConnectionProgress(connType) != PersonLI.PROGRESS_COMPLETED) {
					if (connType == PersonLI.CHILDREN_CONNECTIONS) {// !@#$ Generalise this to a LUT
						connectionsReaderState = 
							WebSiteReader_Connections.doGetLiConnections(
									person.getUniqueID(), 
									person.getChildConnectionProgress(connType),
									timeBoundSec); 
					}
					if (connectionsReaderState != null) {
						person.addConnectionIDs(connectionsReaderState.connectionKeys);
						person.setChildConnectionProgress(connType, connectionsReaderState.lastPageNumRead);
						Statistics.getInstance().recordEvent("getPersonFromLI: Got connections");
						break;
					}
				}
			}
		}
		return person;
	}
	
	
}
