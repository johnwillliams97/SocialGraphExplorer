package datatypes;

import java.io.Serializable;


import people.client.PersonTrait;


/*
 * Public interface to a PersonTrait record that can be persisted on the server
 */
public interface PersistentPersonTrait extends Serializable, PersonTrait {
	
	
	// uniqueID for default record in the database
	public static final long DEFAULT_PERSON_RECORD_UNIQUEID = -1;
	
	/*
	 * Real data must be protected
	 */
	public boolean 		isRealData();
	
}
