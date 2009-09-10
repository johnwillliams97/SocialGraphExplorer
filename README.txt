Locations
---------
Source:  http://github.com/peterwilliams97/SocialGraphExplorer
Program: http://peterwilliams9798.appspot.com/
Admin:   http://appengine.google.com/dashboard?&app_id=peterwilliams9798
Status:  http://code.google.com/status/appengine

Product Definition
------------------
The application will be a (dummy) Social Network Navigator. It will navigate a person's list of social network 
connections show each connection's information allow the connection to become the person so their connections 
can be navigated. 

High Level Design - User Interface 
----------------------------------
UI Components
	Main person (one line of summary info: name, address, description).
	List of person's connections (one line of summary info)
	Panel with details of selected person or connection.
	Navigation buttons (next, previous, first, last screen of connections)
UI Actions
	Selecting a person or connection shows their details in the Details panel. 
	Clicking on a selected connection makes the connection the main person.
	Navigation buttons move between screens of connections

High Level Design - Client Cache 
--------------------------------
The server will be fetching data from the Google datastore and sending it back to the client. This is 
going to be slow compared to desired UI response time so we need to keep a client side cache with the data 
people will be likely to be requesting. The client cache needs to fetch data from the server before the user 
requests viewing them. One way to arrange the cache is in terms of the number of mouse clicks a person is from 
being visible on the main screen.

	Visible
	1 click from being visible
	2 clicks from being visible
	...
	
If there are N people per screen then
	N visible persons
	<= 4*N 1-click persons  (next, previous, first, last)
	<= 4*N 2-click persons (next-next, previous-previous, first-next, last-previous
	
Choosing a new main person will change the few clicks away persons a lot. These persons can be predicted but 
changing the main person is a big change so we will leave this prediction for a future iteration of the design.
The cache can be navigated forwards and backwards. Backwards navigation can be cached effectively with an LRU 
cache. Forwards navigation cached by enumerating the persons that can be accessed by 1 or more mouse clicks.
Therefore a sufficient cache hint is a set of lists of persons that can be reached in m mouse clicks for 
m=0..M where M is a parameter that is to specified and optimised.

High Level Design - UI and Client Cache Interaction
---------------------------------------------------
The previous section outlined a cache that depended on the UI state. This leads to a simple set of interactions 
between the UI and cache.
	User sets new UI state
	UI sends the m-click hints to client cache and disables user controls (effectively freezing the state of the UI)
	Client cache signals UI when visible persons are in cache.
	UI enables user controls.
	
This is simple interaction which is good for a small project like this.

Client Cache Effectiveness and Tuning
-------------------------------------

The effectiveness of the client cache will depend on how often it fetched the m-click-away persons from 
the server before the user makes those clicks.

There are some obvious trade-offs. 
	Fewer navigation controls in the UI makes caching easier but leads to a less flexibiltiy
	Bigger caches are faster but use up client side memory and may waste server fetches.

High Level Design - Client Cache and Server Interactions
--------------------------------------------------------
This is simple in principle. Fetch data from the server. Some things will need to be tuned 
	number of people fetched per server interaction
	timing of fetches for different client cache levels (all at once or m-click before m+1-click)

High Level Design - Server
--------------------------
Each person will have some possibly data and some searchable meta-data.

	Person = summary (name, address, ...) + detail + list of connections (other persons)

Since we want to access this rapidly for each person we will need to store it on our server in our format. Most 
likely we will have a multi-stage cache.
	Fetch data from datastore (and store it memcache)
	Fetch data from memcache.
We will need to run this a little to get some idea of the relative speeds of 1 and 2 to see how we will tune this.

High Level Design - Person Data Structure 
-----------------------------------------
The person data structure is mostly straightforward, a big chunk of data with some meta-data. The obvious wrinkle 
is the relationships between persons. Each person has connections to other persons. If there needs to be any work
 on connections, such as sorting the connections by the connections' meta-data then we have to decide how much 
 meta-data to store with each connection, or see how the datastore can assist in one to many relationships.

