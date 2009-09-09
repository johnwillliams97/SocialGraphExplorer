package people.client;


/*
 * This UI is a list. First item in the list is the 'anchor'.
 * The following items are indexes in anchor's connections list
 * anchorUniqueID, startIndex and selectedRow completely describe the state of this class; 
 */
public class CanonicalState {
	public long      anchorUniqueID;   // The ID of the anchor person
	public int       startIndex;
	public boolean   anchorFetched;
	public boolean   visibleFetched; 

	public CanonicalState() {
		init();
	}
	public CanonicalState(long anchorUniqueID, int startIndex) {
		init();
		this.anchorUniqueID = anchorUniqueID;
		this.startIndex = startIndex;
	}
	/*
	CanonicalState(String stringRep) {
		init();
		String[] parts = stringRep.split(SEPARATOR);
		if (parts.length > 0)
			this.anchorUniqueID = Long.parseLong(parts[0]);
		if (parts.length > 1)
			this.startIndex = Integer.parseInt(parts[1]);
	}
	*/
	public CanonicalState(CanonicalState s1) {
		this.anchorUniqueID = s1.anchorUniqueID;
		this.startIndex = s1.startIndex;
		this.anchorFetched = s1.anchorFetched;
		this.visibleFetched = s1.visibleFetched;
	}
	private void init() {
		this.anchorUniqueID = PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID;   // The ID of the anchor person
		this.startIndex = 0;
		this.anchorFetched = false;
		this.visibleFetched = false;
	}
	/*
	private static final String SEPARATOR = ",";
	public String getAsString() {
		return "" + this.anchorUniqueID + SEPARATOR + this.startIndex;
	}
	*/
	
}