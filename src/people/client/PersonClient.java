
package people.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Hold relevant data for Person. This class is meant to be serialised in RPC
 * calls. 
 */
public class PersonClient implements IsSerializable, PersonPublic {
	
	private long   	liUniqueID = UNIQUE_ID_NOT_FOUND;
	private long    requestedID = UNIQUE_ID_NOT_FOUND;
	private String 	nameFull;
	private String 	description;
	private String 	location;
	private String 	employer;
	private boolean isChildConnectionInProgress;
	private ReadState readState;
	private List<Long> connectionIDs;
	private String    htmlPage;
	
	// Token for cases when no person exists
	private static final String MAGIC_STRING = "...  ";
	// uniqueID pseudo-value for when no uniqueID is found
	public static final long UNIQUE_ID_NOT_FOUND = -1L;
	
	// First magic person
	public static final long  MAGIC_PERSON_CLIENT_1_UNIQUE_ID = -101L;
	public static final PersonClient MAGIC_PERSON_CLIENT_1 = new PersonClient("Initialising server", MAGIC_PERSON_CLIENT_1_UNIQUE_ID);

	// Token for cases when person not in cache
	public static final PersonClient PERSON_NOT_IN_CACHE = new PersonClient("Fetching from server", UNIQUE_ID_NOT_FOUND);
	
			
	public PersonClient(String nameFull, String description, String location) {
		this.nameFull = nameFull;
		this.description = description;
		this.location = location;
	}
	// Magic object constructor
	public PersonClient(String name, long liUniqueID) {
		this(MAGIC_STRING, name, MAGIC_STRING);
		this.liUniqueID = liUniqueID;
	}
	public PersonClient() {
		this(null, null, null);
	}
	// Simple test to distinguish magic persons from real persons
	public boolean isMagicPerson() {
		return (this.location != null && this.location.equals(MAGIC_STRING));
	}
	/*
	 * getters & setters
	 */
	@Override
	public void  setNameFull(String nameFull) {
		this.nameFull = nameFull;
	}
	@Override
	public String getNameFull() {
		return this.nameFull;
	}
	@Override
	public void   setLiUniqueID(long liUniqueID) {
		this.liUniqueID = liUniqueID;
	}
	@Override
	public long   getLiUniqueID() {
		return this.liUniqueID;
	}
	@Override
	public List<Long> getConnectionIDs() {
		return this.connectionIDs;
	}
	
	@Override
	public void   setConnectionIDs(List<Long> connectionIDs) {
		if (connectionIDs != null) {
			this.connectionIDs = new ArrayList<Long>();
			this.connectionIDs.addAll(connectionIDs);
		}
		else {
			this.connectionIDs = null;
		}
	}
	@Override
	public void setDescription(String description) {
		this.description = description;	
	}
	@Override
	public String getDescription() {
		return this.description;
	}
	@Override
	public void setEmployer(String employer) {
		this.employer = employer;
	}
	@Override
	public String getEmployer() {
		return this.employer;
	}
	@Override
	public void setLocation(String location) {
		this.location = location;	
	}
	@Override
	public String getLocation() {
		return this.location;
	}
	@Override
	public void setIsChildConnectionInProgress(boolean isChildConnectionInProgress) {
		this.isChildConnectionInProgress = isChildConnectionInProgress;	
	}
	@Override
	public boolean getIsChildConnectionInProgress() {
		return this.isChildConnectionInProgress;
	}
	@Override
	public void setReadState(ReadState readState) {
		this.readState = readState;	
	}
	@Override
	public ReadState getReadState() {
		return this.readState;
	}
	@Override
	public void  setHtmlPage(String htmlPage) {
		this.htmlPage = htmlPage;
	}
	@Override
	public String getHtmlPage() {
		return this.htmlPage;
	}
	
	public void setRequestedID(long requestedID) {
		this.requestedID = requestedID;
	}
	public long getRequestedID() {
		return this.requestedID;
	}
	
	// !@#$ Hack for checking cache performance
	String whence = "nowhere";
	@Override
	public void setWhence(String whence) {
		this.whence = whence;
	}
	@Override
	public String getWhence() {
		return this.whence;
	}
	
	// !@#$ Hack for checking cache performance
	// This shows how long it took to fetch the person
	double fetchDuration = 0.0;
	@Override
	public void setFetchDuration(double fetchDuration) {
		this.fetchDuration = fetchDuration;
	}
	@Override
	public double getFetchDuration() {
		return this.fetchDuration;
	}
	// Full client server round trip duration 
	double fetchDurationFull = 0.0;
	public void setFetchDurationFull(double fetchDuration) {
		this.fetchDurationFull = fetchDuration;
	}
	public double getFetchDurationFull() {
		return this.fetchDurationFull;
	}
	
	public boolean isIncomplete(boolean noDetail, boolean connectionsInProgress)  {
		return 
			((noDetail && (this.htmlPage == null)) ||
			 (connectionsInProgress && this.isChildConnectionInProgress));
	}
	
	static public void debugValidate(PersonPublic person) {
		 if (OurConfiguration.DEBUG_MODE && person != null) {
			 List<Long> connectionIDs = person.getConnectionIDs();
			 long uniqueID = person.getLiUniqueID();
			 if (connectionIDs != null)  {
				 int numConnections = connectionIDs.size();
				 long idI, idJ;
				 for (int i = 0; i < numConnections; ++i) {
					 idI = connectionIDs.get(i) ;
					 myAssert(idI != uniqueID);
					 for (int j = i+1; j < numConnections; ++j) { 
						 idJ = connectionIDs.get(j) ;
						 myAssert(idI != idJ);
					 }
				 }
			 }
		 }
	}
	static private void myAssert(boolean condition) {
		if (!condition) {
			assert(condition);  // Add breakpoint here
		}
	}
	
}
