package people.client;

import java.util.ArrayList;
import java.util.List;

/*
 * Miscellaneous collection functions
 * loadsoffun.nocache.js
 */
public class MiscCollections {

	static public<T> List<T> arrayToList(T[] array) {
		List<T> list = null;
		if (array != null) {
			list = new ArrayList<T>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	
	static public List<Long> arrayToListLong(long[] array) {
		List<Long> list = null;
		if (array != null) {
			list = new ArrayList<Long>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	static public long[] listToArrayLong(List<Long> list) {
		long[] array = null;
		if (list != null) {
			array = new long[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				array[i] = list.get(i);
			}
		}
		return array;
	}

	static public List<Integer> arrayToListInt(int[] array) {
		List<Integer> list = null;
		if (array != null) {
			list = new ArrayList<Integer>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	
	static public  int[] listToArrayInt(List<Integer> list) {
		int[] array = null;
		if (list != null) {
			array = new int[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				array[i] = list.get(i);
			}
		}
		return array;
	}
	static int sizeOfArray(long[][] array) {
		int size = 0;
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				size += array[i].length;
			}
		}
		return size;
	}
	
	static long[] flattenArray(long[][] array) {
		long[] flattened = new long[sizeOfArray(array)];
		int  index = 0;
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				for (int j = 0; j < array[i].length; ++j) {
					flattened[index++] += array[i][j];
				}
			}
		}
		return flattened;
	}
	
	static public String arrayToString(long[] ids) {
 		int numIDs = (ids != null) ? ids.length : 0;
 		String dbgIDs = "" + numIDs + ": [";
		for (int i = 0; i < numIDs; ++i)
	     	dbgIDs += "" + ids[i] + ", ";
	  	dbgIDs += "]";
	  	return dbgIDs;
	}
	
}