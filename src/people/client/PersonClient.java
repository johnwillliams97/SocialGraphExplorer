
package people.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Hold relevant data for Person. This class is meant to be serialised in RPC
 * calls. 
 */
public class PersonClient implements IsSerializable, PersonTrait {
	
	private long   	_uniqueID = UNIQUE_ID_NOT_FOUND;
	private long    _requestedID = UNIQUE_ID_NOT_FOUND;
	private String 	_nameFull;
	private String 	_description;
	private String 	_location;
	private String 	_employer;
	private ReadState _readState;
	private List<Long> _connectionIDs;
	private String    _htmlPage;
	
	// Token for cases when no person exists
	private static final String MAGIC_STRING = "...  ";
	// uniqueID pseudo-value for when no uniqueID is found
	public static final long UNIQUE_ID_NOT_FOUND = -1L;
	
	// Marker for the default person to fetch from the DB. Needed for bootstrapping
	public static final long  MAGIC_PERSON_CLIENT_1_UNIQUE_ID = PersonTrait.GET_DEFAULT_PERSON_UNIQUEID;
	public static final PersonClient MAGIC_PERSON_CLIENT_1 = new PersonClient("Initialising server", MAGIC_PERSON_CLIENT_1_UNIQUE_ID);

	// Token for cases when person not in cache
	public static final PersonClient PERSON_NOT_IN_CACHE = new PersonClient("Fetching from server", UNIQUE_ID_NOT_FOUND);
	
			
	public PersonClient(String nameFull, String description, String location) {
		_nameFull = nameFull;
		_description = description;
		_location = location;
	}
	// Magic object constructor
	public PersonClient(String name, long uniqueID) {
		this(MAGIC_STRING, name, MAGIC_STRING);
		_uniqueID = uniqueID;
	}
	public PersonClient() {
		this(null, null, null);
	}
	// Simple test to distinguish magic persons from real persons
	public boolean isMagicPerson() {
		return (_location != null && _location.equals(MAGIC_STRING));
	}
	
	private boolean _isRealData = true;  // assume the worst
	public void setIsRealData(boolean isRealData) {
		_isRealData = isRealData;
	}
	public boolean getIsRealData() {
		return _isRealData;
	}
	
	/*
	 * getters & setters
	 */
	@Override
	public void  setNameFull(String nameFull) {
		_nameFull = nameFull;
	}
	@Override
	public String getNameFull() {
		return _nameFull;
	}
	@Override
	public void   setUniqueID(long uniqueID) {
		_uniqueID = uniqueID;
	}
	@Override
	public long  getUniqueID() {
		return _uniqueID;
	}
	@Override
	public List<Long> getConnectionIDs() {
		return _connectionIDs;
	}
	
	@Override
	public void  setConnectionIDs(List<Long> connectionIDs) {
		if (connectionIDs != null) {
			_connectionIDs = new ArrayList<Long>();
			_connectionIDs.addAll(connectionIDs);
		}
		else {
			_connectionIDs = null;
		}
	}
	@Override
	public void setDescription(String description) {
		_description = description;	
	}
	@Override
	public String getDescription() {
		return _description;
	}
	@Override
	public void setEmployer(String employer) {
		_employer = employer;
	}
	@Override
	public String getEmployer() {
		return _employer;
	}
	@Override
	public void setLocation(String location) {
		_location = location;	
	}
	@Override
	public String getLocation() {
		return _location;
	}
	
	
	@Override
	public void setReadState(ReadState readState) {
		_readState = readState;	
	}
	@Override
	public ReadState getReadState() {
		return _readState;
	}
	@Override
	public void  setHtmlPage(String htmlPage) {
		_htmlPage = htmlPage;
	}
	@Override
	public String getHtmlPage() {
		return _htmlPage;
	}
	
	public void setRequestedID(long requestedID) {
		_requestedID = requestedID;
	}
	public long getRequestedID() {
		return _requestedID;
	}
	
	// !@#$ Hack for checking cache performance
	String _whence = "nowhere";
	@Override
	public void setWhence(String whence) {
		_whence = whence;
	}
	@Override
	public String getWhence() {
		return _whence;
	}
	
	// !@#$ Hack for checking cache performance
	// This shows how long it took to fetch the person
	double _fetchDuration = 0.0;
	@Override
	public void setFetchDuration(double fetchDuration) {
		_fetchDuration = fetchDuration;
	}
	@Override
	public double getFetchDuration() {
		return _fetchDuration;
	}
	// Full client server round trip duration 
	static double _fetchDurationFull = 0.0;
	public void setFetchDurationFull(double fetchDuration) {
		_fetchDurationFull = fetchDuration;
	}
	public double getFetchDurationFull() {
		return _fetchDurationFull;
	}
	
		
	/*
	 * Track person's client cache level  
	 */
	private int cacheLevel = -1;
	public void setCacheLevel(int cacheLevel) {
		this.cacheLevel = cacheLevel;
	}
	public int getCacheLevel() {
		return this.cacheLevel;
	}
	
	public static void debugValidate(PersonTrait person) {
		 if (OurConfiguration.VALIDATION_MODE && person != null) {
			 List<Long> connectionIDs = person.getConnectionIDs();
			 long uniqueID = person.getUniqueID();
			 if (connectionIDs != null)  {
				 int numConnections = connectionIDs.size();
				 long idI, idJ;
				 for (int i = 0; i < numConnections; ++i) {
					 idI = connectionIDs.get(i) ;
					 Misc.myAssert(idI != uniqueID, "PersonClient.debugValidate - duplicate uniqueID" + i + "," + uniqueID);
					 for (int j = i+1; j < numConnections; ++j) { 
						 idJ = connectionIDs.get(j) ;
						 Misc.myAssert(idI != idJ,  "PersonClient.debugValidate - duplicate connection" + i + "," + j + "," + idJ);
					 }
				 }
			 }
		 }
	}
	
	
	
}
