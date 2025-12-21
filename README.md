# cics-nashorn

This is a demonstration of how Javascript can be executed from CICS Liberty Server, using Open Nashorn

It can accept traffic either via JAX-RS, or via CICS program via the "Link-to-liberty" feature.

`<feature>cicsts:link-1.0</feature>`

In my example I use maven to build the cics bundle, which is to be manually copied to Mainframe USS folder.
A CICS Bundle definition needs to be defined in the CSD, which when installed, dynamically adds the app to CICS

When used via JAX-RS, it has a few methods.
This is mainly as I was playingaround with different options
* /api/eval is used when you want to pass in a tsring that contains Javascript to execute
* /api/evalFile is used when you want to programatically read a file from USS, and then execute
* /api/jsload is used when you want Nashorn itself to read a file from USS, and then execute


**A useful tip is to define liberty feature MpOpenAPI in your server.xml, which automatically build a OpenAPI document and UI**

`<feature>mpOpenAPI-1.0</feature>`

Also, only one JAX-RS app is registered at a time, so if you have 2 Apps, only the first one started will appear in OpenApi UI
Note - depending on other features, Liberty may conflict/complain, and then you may have to pick a specific version of mpOpenAPI to not clash
This happened for me, so I stayed with a lower version of either 1.0 or 1.1.

When used via Link-to-Liberty, it passes traffic to via containers
ie the standard sort of pattern CICS programmers are used to
```
EXEC CICS PUT CHANNEL("MYCHANNEL") CONTAINER("JAVASCRIPT") FROM(MYJS) FLENGTH(MYJSLEN) CHAR
EXEC CICS LINK PROGRAM("NASHORN") CHANNEL("MYCHANNEL")
EXEC CICS GET CHANNEL("MYCHANNEL") CONTAINER("NASHORN_OUT") INTO(OUTPUT) FLENGTH(OUTLEN)
EXEC CICS GET CHANNEL("MYCHANNEL") CONTAINER("ERROR") INTO(OUTPUT) FLENGTH(OUTLEN)
```

I actually found Open Nashorn to be actually pretty fast - The only issue is if you want a newer ECMA standard.

One of the best features of Nashorn, is that you can get it to call Java classes.
So this means that if you have a java class, then you can get Nashorn to call it from within Javascripy
eg JCICS, JCICSX, JZOS, etc. 
