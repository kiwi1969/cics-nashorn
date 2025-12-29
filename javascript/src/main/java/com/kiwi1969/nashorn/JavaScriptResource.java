package com.kiwi1969.nashorn;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/* Define JAX-RS APIs */
@Path("/api")

public class JavaScriptResource {

    /* This executes the given Javascript (supplied via body text) */
    @POST
    @Path("/eval")
    @Consumes("text/javascript")
    @Produces(MediaType.TEXT_PLAIN)
        
    public static Response eval(@QueryParam("info") @DefaultValue("no") String info, String js) {
        boolean bInfo = info.toLowerCase().startsWith("y");
    
        return Response.ok()
            .entity(JavaScript.eval("", js, bInfo, false))
            .build(); 
    }

    /* load file Using Java, compile, cache, and then evaluate */
    @GET
    @Path("/evalFile")
    @Produces(MediaType.TEXT_PLAIN)
    public static Response evalFile(
        @QueryParam("jsFile") @DefaultValue("scripts/myscript.js") String jsFile,
        @QueryParam("info") @DefaultValue("no") String info,
        @QueryParam("debug") @DefaultValue("no") String debug ) {  

        boolean bInfo = info.toLowerCase().startsWith("y");
        boolean bDebug = debug.toLowerCase().startsWith("y");

        if (jsFile.isEmpty())
            return Response.ok()
                .entity("No javascript supplied!")
                .build();
             
        return Response.ok()
            .entity(JavaScript.eval(jsFile, "", bInfo, bDebug))
            .build();
    }

    /* let Nashorn load, cache, and then evaluate */
    @GET
    @Path("/evalURL")
    @Produces(MediaType.TEXT_PLAIN)
    public static Response evalURL(
        @QueryParam("jsPath") @DefaultValue("scripts/myscript.js") String url,
        @QueryParam("info") @DefaultValue("no") String info,
        @QueryParam("debug") @DefaultValue("no") String debug ) {

        boolean bInfo = info.toLowerCase().startsWith("y");
        boolean bDebug = debug.toLowerCase().startsWith("y");
        
        return Response.ok()
            .entity(JavaScript.evalURL(url, bInfo, bDebug ))
            .build();
    }

    /* Program info */
    @GET
    @Path("/about")
    @Produces(MediaType.TEXT_PLAIN)
            
    public static Response about() {
        
        return Response.ok()
            .entity(JavaScript.versionString())
            .build(); 
    }

}
