package people.client;

import java.util.ArrayList;
import java.util.List;


/**
 * Stack with unique entries and loses entries that exceed stack max size. 
 * Not a classic stack.
 * Used for history lists.
 */
public class StackLikeContainer<T> {
	private int     _maxSize;
	private List<T> _data;
	
	public StackLikeContainer(int maxSize) {
		_data = new ArrayList<T>();
		_maxSize = maxSize;
	}
	public int size() {
		return _data.size();
	}
	public void push(T item) {
		if (_data.contains(item)) {
			_data.remove(item);
		}
		_data.add(item);
		if (_data.size() > _maxSize) {
			_data.remove(0);
		}
	}
	public T pop() {
		T result = null;
		if (_data.size() > 0) {
			result = _data.get(_data.size() - 1);
			_data.remove(_data.size() - 1);
		}
		return result;
	}
	public List<T> getData() {
		return this._data;
	}
	
	public String getState() {
		String state = "" + _data.size() + ": ";
		for (int i = 0; i < _data.size(); ++i)
			state += "" + _data.get(i) + ", ";
		return state;
	}
	/*	
	public static void test() {
		final int M = 4;
		final int N = 2*M;
		StackLikeContainer<Long> stack = new StackLikeContainer<Long>(M);
		for (long i = 0; i < M; ++i) {
			stack.push(i);
			System.out.println("push " + i + " - " + stack.getState());
		}
		for (long i = 0; i < N; ++i) {
			stack.push(i);
			System.out.println("push " + i + " - " + stack.getState());
		}
		for (long i = 0; i < N; ++i) {
			Long n = stack.pop();
			System.out.println("pop  " + n + " - " + stack.getState());
		}
	}
	public static void main(String[] args) {
		test();
	}
	*/
}
	