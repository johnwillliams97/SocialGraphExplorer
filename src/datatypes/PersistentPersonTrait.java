package datatypes;

import java.io.Serializable;


import people.client.PersonTrait;


/*
 * Public interface to a PersonTrait record that can be persisted on the server
 */
public interface PersistentPersonTrait extends Serializable, PersonTrait {
	
	
}
