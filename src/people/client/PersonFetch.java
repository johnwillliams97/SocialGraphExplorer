package people.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;


/**
 * Data for tracking a person fetch from server
 * calls.
 */
public class PersonFetch implements IsSerializable {
	public long 		requestedUniqueID;		// persons requested
	public PersonClient person;					// persons returned
	public int 			level;					// client cache level
	
	static List<Long> getFetchIDs(PersonFetch[] fetches) {
		List<Long> ids = new ArrayList<Long>();
		if (fetches != null)  {
			for (PersonFetch fetch: fetches) {
				ids.add(fetch.person.getLiUniqueID());
			}
		}
		return ids;
	}
}
	