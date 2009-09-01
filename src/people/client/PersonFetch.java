package people.client;

import com.google.gwt.user.client.rpc.IsSerializable;


/**
 * Data for tracking a person fetch from server
 * calls.
 */
public class PersonFetch implements IsSerializable {
	public long 		requestedUniqueID;		// persons requested
	public PersonClient person;					// persons returned
	public int 			level;					// client cache level
}
	