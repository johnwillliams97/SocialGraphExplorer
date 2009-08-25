package htmlstuff;

import java.util.List;

public class ConnectionsReaderState {
	public List<Long> 	connectionKeys     = null;
	public long             firstPageNumRead   = 0;
	public long             lastPageNumRead    = 0;
	public boolean		hasReadAllPages    = false;
	public boolean 		connected          = false;
}