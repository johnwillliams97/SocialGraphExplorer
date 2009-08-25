package misc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;



/*
 * Profiling
 */
public class Statistics {
	private static final Logger logger = Logger.getLogger(Statistics.class.getName());
	
	private static Statistics _instance = null;
	private static int        _instanceCount = 0;
	
	
	public static Statistics getInstance() {
	    assert(_instance != null);
	    return _instance;
	}
	public static Statistics createInstance(String name) {
		++_instanceCount;
		_instance = new Statistics();
		_instance.recordEvent("Creation: <" + name + ", " +_instanceCount + ">");
	    return _instance;
	}
	
	public class Event {
		public long   _when;
		public String _name;
		public int _sequence;
		public Event(String name) {
			this._name = name;
			this._sequence = ++_sequenceNumber;
			this._when =  Calendar.getInstance().getTimeInMillis() - _callStartTime;
		}
		public String describe() {
			String description = "Event[" + this._sequence + "] " + this._when/1000L + " sec: '" + this._name + "'";
			return description;
		}
	}
	
	private long _callStartTime = 0L;
	private int  _sequenceNumber = 0;
	
	//Need to synchronize access to _events since this list is shared across servlets
	private List<Event> _events = null;
	
	private Statistics() {
		this._callStartTime = Calendar.getInstance().getTimeInMillis();
		this._sequenceNumber = 0;
		this._events = new ArrayList<Event>();
	}
	public long getLastTime() {
		long lastTime = 0L;
		synchronized(this) {
			int numEvents = this._events.size();
			if (numEvents > 0)
				lastTime = this._events.get(numEvents -1)._when;
		}
		return lastTime;
	}
	
	public  void recordEvent(String name) {
		Event event = new Event(name);
		synchronized(this) {
			this._events.add(event);
		}
		logger.warning(event.describe());
	}
	
	public void showAllEvents() {
		logger.warning("=============== All events ============");
		synchronized(this) {
			for (Event event: this._events) {
				logger.warning(event.describe());
			}
		}
		logger.warning("=============== ---------- ============");
	}
	
}
