package datatypes;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.Text;



public class PersonLI   {
	
	private static final Logger logger = Logger.getLogger(PersonLI.class.getName());
	
/*
 * The public interface. Keys I care about.	
 */
	
	private long rank;						// My ranking of the person
  
	
 	private String nameFull;				 
	
	
 	private String nameFirst;				//  First name, for searching
  
									
	private String nameLast;				//  Last name, for searching
  
	
	private String location;
	
	
	private String employer;
	
	
	private String description;
	
	private long   liUniqueID;				// Unique ID for LI
	
	private List<Long> connectionIDs;					// liUniqueIDs of connections
	
	
	private long		numConnectionIDs;				// Cache connectionIDs.size() here so we can search on it
	
	/****************************************************************
	 * Internals
	 * 	State of partial searches
	 * 	LinkedIn page contents
	 *********************************************************************/
	
	/*
	 *  childConnectionProgress[CHILDREN_CONNECTIONS]
	 *  For each type
	 *  	> 0 = progress e.g. #pages read
	 *  	-1 = not started
	 *      -2 = complete
	 */
	public static final int NUM_PROGRESS_TYPES = 40;
	
	public static final int CHILDREN_CONNECTIONS = 0;
	public static final int CHILDREN_RECOMMENDATIONS = 1;
	public static final int CHILDREN_RECOMMENDED_BY = 2;
	public static final int CHILDREN_ALSO_VIEWED = 3;
	public static final int CHILDREN_WVMP = 4;

	public static final int RECORD_READ_PROGRESS = NUM_PROGRESS_TYPES - 1;
	
	public static final long PROGRESS_NOT_STARTED = -1L;
	public static final long PROGRESS_COMPLETED = 0L; // -2L;
	
	
	private long[] 	childConnectionProgress = new long[NUM_PROGRESS_TYPES]; 
	
		  
									
	private Text htmlPage;				// Full html page. Keep for later analysis?
  
	
	private Date whenReadFirst;				// When first read from LI
	
	
	private Date whenReadLast;				// When last read from LI

	
	  public PersonLI(String nameFirst, 
			  		String nameLast, 
			  		long   liUniqueID,
			  		long   rank,
			  		String location,
			  		String description,
			  		List<Long> connectionIDs) {
		    this.nameFirst = nameFirst;
		    this.nameLast  = nameLast;
		    this.liUniqueID = liUniqueID;
		    this.rank = rank;
		    this.location = location;
		    this.description = description;
		    this.connectionIDs = connectionIDs;
		 //   MessageDigest.getInstance("MD5");
	  }
	  /*
	   * http://leepoint.net/notes-java/oop/constructors/constructor.html
	   */
	  public PersonLI() {
		  	this(
		  			(String)null, 
		  			(String)null, 
			  		0L, 0L,
			  		(String)null,
			  		(String)null,
			  		(List<Long>)null);
	  }
	  
  	 /*
  	  * getters & setters
  	  */
	  public void setNameFirst(String nameFirst) {
		   this.nameFirst = nameFirst;
	  }
	  public String getNameFirst() {
		  return this.nameFirst;
	  }
	  
	  public void setNameLast(String nameLast) {
		   this.nameLast = nameLast;
	  }
	  public String getNameLast() {
		  return this.nameLast;
	  }
	  
	  public void setNameFull(String nameFull) {
		   this.nameFull = nameFull;
	  }
	  public String getNameFull() {
		  return this.nameFull;
	  }
	  
	  public void setLiUniqueID(long liUniqueID) {
		  this.liUniqueID = liUniqueID;
	  }
	  public long getLiUniqueID() {
		  return this.liUniqueID;
	  }
	
	  public long getNumConnections() { 
		  // Break numConnectionIDs caching because it got out of synch somehow !@#$
		  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
		  
		  return this.numConnectionIDs;
	  }
	  
	  public List<Long> getConnectionIDs() {
		  return this.connectionIDs;
	  }
	
	  
	  public void setConnectionIDs(List<Long> connectionIDs) {
		  if (connectionIDs == null) {
			  this.connectionIDs = null;
		  }
		  else if (connectionIDs != this.connectionIDs) {
			  List<Long> tmp = new ArrayList<Long>(); // Shouldn't need a temp if connectionIDs != this.connectionIDs
			  tmp.addAll(connectionIDs);
			  this.connectionIDs = tmp;
		  }
		  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
	  }
	  public void addConnectionIDs(List<Long> connectionIDs) {
		  if (this.connectionIDs == null)
			  setConnectionIDs(connectionIDs);
		  else if (connectionIDs != null) {
			  this.connectionIDs.addAll(connectionIDs);
			  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
		  }
	  }
	  
	  public static final int MAX_DESCRIPTION = 400;

	  public static final long DEFAULT_LI_UNIQUEID = 1960788L; // Me
	
	  public void setDescription(String description) {
		  if (description!= null && description.length() > MAX_DESCRIPTION) {
			 logger.info("description (b)[" + description.length()+ "] " + description);
		  }
		  else {
			  this.description = description;
		  }
	  }
		
		public String getEmployer() {
			return this.employer;
		}
		
		public void setEmployer(String employer) {
			this.employer = employer;
		}
		
		
		public void setLocation(String location) {
			this.location = location;
		}
		
		public String getLocation() {
	  		return this.location;
	  	}
		
		
		public String getHtmlPage() {
			return (this.htmlPage != null ? this.htmlPage.getValue() : null);
		}
		
		public void setHtmlPage(String htmlPage) {
			if (htmlPage != null) {
				this.htmlPage = new Text(htmlPage);
			}
		}
		
	 
		 
  
  	public long getRank() {
  		return rank;
  	}
  	
  	public Date getReadFirst() {
  		return this.whenReadFirst;
  	}
  	public Date getReadLast() {
  		return this.whenReadLast;
  	}
  
  	static public String decodeUrl(String description) {
  		String descClean = null;
  		if (description != null) {
			try {
				descClean = URLDecoder.decode(description, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			while (descClean.contains("&amp;")) {
				descClean = descClean.replaceAll("&amp;", "&");
			}
			while (descClean.contains("&quot;")) {
				descClean = descClean.replaceAll("&quot;", "'");
			}
  		}
  		return descClean;
  	}
	public String getDescription() {
  		String descClean = decodeUrl(this.description);
  		return descClean;
  	}
  	  	
  	public boolean isSameAs(PersonLI person) {
  		return (this.getLiUniqueID() == person.getLiUniqueID());
  	}
  	
  	
  	public boolean getIsChildConnectionInProgress() {
	boolean connectionInProgress = false;
		for (int connType = 0; connType < this.childConnectionProgress.length; connType++) {
			if (this.childConnectionProgress[connType] != PersonLI.PROGRESS_COMPLETED) {
				connectionInProgress = true;
				break;
			}
		}
		return connectionInProgress;
	}
	
  	public long getNumChildConnectionProgresses(){
		  return childConnectionProgress.length;      
	 }
	 
  	public long getChildConnectionProgress(int index) {
		  assert(0 <= index && index < childConnectionProgress.length);
		  return childConnectionProgress[index];      
	}
	  
	 public void setChildConnectionProgress(int index, long progress) {
		  assert(0 <= index && index < getNumChildConnectionProgresses());
		  childConnectionProgress[index] = progress; 

	  }
}
