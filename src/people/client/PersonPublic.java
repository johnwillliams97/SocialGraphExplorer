package people.client;

import java.util.List;


/*
 * Public interface to a person
 */
public interface PersonPublic {
	/*
	 * 	State of partial searches
	 */
	public enum ReadState {
		RECORD_READ_MINIMAL,
		RECORD_READ_BASIC,
		RECORD_READ_FULL
	}
	long NO_PERSON_UNIQUEID = 1L;
	
	/*
	 * Real data must be protected
	 */
	public boolean 		isRealData();
	 /*
 	  * getters & setters
 	  */
	  public void   	setNameFull(String nameFull); 
	  public String 	getNameFull();
	  public void   	setDescription(String description); 
	  public String 	getDescription();
	  public void   	setLocation(String location); 
	  public String 	getLocation();
	  public void   	setEmployer(String employer); 
	  public String 	getEmployer();
	  public void   	setLiUniqueID(long liUniqueID); 
	  public long   	getLiUniqueID() ;
	  public List<Long> getConnectionIDs(); 
	  public void   	setConnectionIDs(List<Long> connectionIDs);
	  public void 		setIsChildConnectionInProgress(boolean isChildConnectionInProgress);
	  public boolean 	getIsChildConnectionInProgress();
	  public void       setReadState(ReadState readState);
	  public ReadState  getReadState();
	  public void       setHtmlPage(String htmlPage);
	  public String     getHtmlPage();
	  
	  // Debug and tuning
	  public void 		setWhence(String whence);
	  public String 	getWhence();
	  public void 		setFetchDuration(double fetchDuration);
	  public double 	getFetchDuration();
	
}
