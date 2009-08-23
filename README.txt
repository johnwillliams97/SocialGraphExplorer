Add fields
	Employer
	Industry
	
Algorithm
	Person 
		id
		basic
		full
			list(s) of connections
			
	Increments
		Person 
			basic => full
		List
			add connections
		Query
			Run query to build list
			Person's connections are a query

Scaling
	Sharding
		http://assets.en.oreilly.com/1/event/27/What%20Every%20Developer%20Should%20Know%20About%20Database%20Scalability%20Presentation.pdf

TODO
	Connections list
		entry = (id, completeness, order)
		completeness = id, basic info, # connections
		order changes with completeness
			id, # connections, ....
		re-sort connections list when size changes or completeness of any entry changges
			keep a checksum of completensses to detect changes. Does java have an md5?
	Add time-outs to server calls
	Optimise number of simultaneous server calls in progress
	Remove anchor level from server side
	Add m-click pre-fetch
	Split PersonClient into summary and detail
	Gears support
		http://code.google.com/docreader/#p=gwt-google-apis&s=gwt-google-apis&t=GearsGettingStartedDatabase
	UI fluff
		http://gwt-ext.com/
			
REFERENCES
	http://anyall.org/blog/2009/04/performance-comparison-keyvalue-stores-for-language-model-counts/
	http://www.zackgrossbart.com/hackito/antiptrn-gwt/ ** <= Important for high-level design
	http://www.youtube.com/watch?v=P-jRS4LvsDQ&feature=channel  Lots of useful info on GWT core
	http://www.ibm.com/developerworks/opensource/library/j-gaej2/index.html  <= Intro to writing GWT apps
	http://www.youtube.com/watch?v=sz6txhPT7vQ&feature=channel <= Using Google Apps with GWT
		Can create RPC proxies for apps
		Gadgets are usually servlets
		Gears (in Dalvik?)
		