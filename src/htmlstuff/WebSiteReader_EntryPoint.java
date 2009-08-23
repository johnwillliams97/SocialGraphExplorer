package htmlstuff;

import misc.Statistics;
import datatypes.PersonLI;

public class WebSiteReaderEntry {
	/*
	 * Error handling
	 */
	String _errorMsg = null;
	/*
	private static final java.util.Comparator<PersonLI> NUM_CONN_ORDER =
        new java.util.Comparator<PersonLI>() {
			public int compare(PersonLI p1, PersonLI p2) {
				int n = 0;
				long numConn = -(p1.getNumConnections() - p2.getNumConnections()); // Descending
				if (numConn > 0)
					n = +1;
				else if (numConn < 0)
					n = -1;
				if (n == 0) {
					long id = +(p1.getLiUniqueID() - p2.getLiUniqueID()); // Ascending
					if (id > 0)
						n = +1;
					else if (id < 0)
						n = -1;
				}
				return n;
			}
		};
	
	public static Set<PersonLI> getFetchedPersons(PersonLI person, boolean cachedOnly, ActionType actionType) {
		
		Set<PersonLI>  connectionPersons = null;
		long start = Calendar.getInstance().getTimeInMillis();
		long collectionTime = 0L;
		/// TEST
		connectionPersons = PersonLI.getConnectionsForPerson(person);
		int np = connectionPersons != null ? connectionPersons.size() : 0;
		collectionTime = Calendar.getInstance().getTimeInMillis() - start;
		log.info("getConnectionsForPerson took " + collectionTime/1000L + " seconds for " + np + " connectionz");
		/// TEST
		if (person != null) {
			connectionPersons = new HashSet<PersonLI>();
			Set<Long> connIDs_ = person.getConnectionIDs();
			if (connIDs_ != null) {
				int numKeys = connIDs_.size();
				long[]     uniqueIDs = new long[numKeys];
				boolean[]  isFetched = new boolean[numKeys];
				int 		i = 0;
				for (Long key: connIDs_) {
					uniqueIDs[i] = key;
					isFetched[i] = false;
					++i;
				}
				int numPasses = cachedOnly ? 1 : 2;
				for (int pass = 0; pass < numPasses; pass++) {
					boolean doCachedOnly = (pass == 0);
					for ( i = 0; i < numKeys; i++ ) {
						if (!isFetched[i]) {
							if (!WebSiteReader_Quota.hasTimeLeft(actionType))
								break;
							PersonLI p = PersonCache.findInDbyUniqueId(uniqueIDs[i], doCachedOnly);
							if (p != null) {
								connectionPersons.add(p);
								isFetched[i] = true;
							}
							collectionTime = Calendar.getInstance().getTimeInMillis() - start;
						}
					}
				}
			}
			if (connectionPersons.size() == 0)
				connectionPersons = null;
		}
		collectionTime = Calendar.getInstance().getTimeInMillis() - start;
		if (connectionPersons != null) {
			Collections.sort(connectionPersons, NUM_CONN_ORDER);
		}
		long totalTime = Calendar.getInstance().getTimeInMillis() - start;;
		if (totalTime > 1000)
			log.info("Collection = " + collectionTime/1000L +  ", total = " + totalTime/1000L);
		return connectionPersons;
	}
	public static boolean isFetchListCompelete(PersonLI person, List<PersonLI> connectionPersons) {
		boolean complete = true;
		if (person != null && person.getConnectionIDs() != null) {
			complete = false;
			if (connectionPersons != null) {
				if (connectionPersons.size() == person.getConnectionIDs().size()) {
					complete = true;
				}
			}
		}
		return complete;
	}
*/
	/* Read information from http://www.linkedin.com
	 * This function does a single web page fetch as part of a sequence of page fetches.
	 * - partPerson has the part state from the previous page fetch
	 */
	@SuppressWarnings("unused")
	private boolean _debug_scrape = false;
	
	public PersonLI getPersonFromLI(
				long     key,
				PersonLI partPerson,    // whatever we got in previous fetches, null if no prev fetches
				boolean  forceLiRead)	// forces a LI read
	{
		WebSiteReader_Common.startNewHttpRequest();

		String  htmlPage = null;
		
		PersonLI person = partPerson;
		ConnectionsReaderState  connectionsReaderState = null;

		// First fetch of this person: Get summary
		if (person == null || // Need to read
		   (forceLiRead && person != null && !person.getIsChildConnectionInProgress()) // Forced to read
		   ) {
			
			person = WebSiteReader_UserProfile.doGetLiUserProfile(key, -777L);
			Statistics.getInstance().recordEvent("getPersonFromLI: Got summary");
			//return person;  // Have done a read so leave
		}
		// Second fetch of this person: Get details
		else if (person != null && person.getHtmlPage() == null) {
			htmlPage = WebSiteReader_UserProfile.doGetLiUserProfilePage(key, -777L);
			Statistics.getInstance().recordEvent("getPersonFromLI: Got details");
			person.setHtmlPage(htmlPage);
		
		//	return person;  // Have done a read so leave
		}
		// Third or later fetch of this person: Get connections
		else if (person != null && person.getIsChildConnectionInProgress()) {
			//!@#$ Fix database inconsistencies
			// !@#$ Kills performance
		//	if (person.getChildConnectionProgress(PersonLI.CHILDREN_CONNECTIONS) == PersonLI.PROGRESS_COMPLETED)
		//		person.setChildConnectionProgress(PersonLI.CHILDREN_CONNECTIONS, PersonLI.PROGRESS_NOT_STARTED);
			for (int connType = 0; connType < person.getNumChildConnectionProgresses(); connType++) {
				if (person.getChildConnectionProgress(connType) != PersonLI.PROGRESS_COMPLETED) {
					if (connType == PersonLI.CHILDREN_CONNECTIONS) {// !@#$ Generalise this to a LUT
						connectionsReaderState = WebSiteReader_Connections.doGetLiConnections(person.getLiUniqueID(), 
							person.getChildConnectionProgress(connType)); 
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
		//person.setEmployer("DOES THIS GET WRITTEN TO THE DB?");
		return person;
	}
	
	
}
