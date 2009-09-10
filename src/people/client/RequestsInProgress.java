 package people.client;

import java.util.ArrayList;
import java.util.List;


public class RequestsInProgress {

	private static int _maxRequestsInProgress = OurConfiguration.MAX_REQUESTS_IN_PROGRESS;
	
	public RequestsInProgress(int maxRequestsInProgress) {
		if (maxRequestsInProgress > 0)
			_maxRequestsInProgress = maxRequestsInProgress;
	}
	
	private class Request {
		long clientSequenceNumber;  // unique ID for a request
		int  numCalls;
		Request(long clientSequenceNumber) {
			this.clientSequenceNumber = clientSequenceNumber;
			this.numCalls = 0;
		}
	}
	
	private List<Request> requestsInProgress = new ArrayList<Request>();
	
	public void startRequest(long clientSequenceNumber) {
		Request request = find(clientSequenceNumber);
		Misc.myAssert(request == null, "startRequest null");
		request = new Request(clientSequenceNumber);
		if (requestsInProgress.size() >= _maxRequestsInProgress)
			requestsInProgress.remove(0);;
		requestsInProgress.add(request);
		++request.numCalls;
	}
	public int getNumCalls(long clientSequenceNumber) {
		int numCalls = Integer.MAX_VALUE;  // For when clientSequenceNumber is not matched
		Request request = find(clientSequenceNumber);
		if (request != null)
			numCalls = request.numCalls;
		return numCalls;
	}
	public void increment(long clientSequenceNumber) {
		Request request = find(clientSequenceNumber);
		if (request != null)
			++request.numCalls;
	}
	private Request find(long clientSequenceNumber) {
		Request request = null;
		for (Request r: requestsInProgress) {
			if (r.clientSequenceNumber == clientSequenceNumber) {
				request = r;
				break;
			}
		}
		return request;
	}
	

}
