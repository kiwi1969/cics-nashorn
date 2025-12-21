package com.kiwi1969.nashorn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    /* load file Using Java, compile, cache, and then evaluate */
    @GET
    @Path("/evalFile")
    @Produces(MediaType.TEXT_PLAIN)
    public static Response load(
        @QueryParam("jsFile") @DefaultValue("scripts/myscript.js") String jsFile,
        @QueryParam("info") @DefaultValue("no") String info,
        @QueryParam("debug") @DefaultValue("no") String debug ) {  
        boolean bInfo = info.toLowerCase().startsWith("y");
        boolean bDebug = debug.toLowerCase().startsWith("y");
        String js = "";
        try {
            js = new String(Files.readAllBytes(Paths.get(jsFile)));
        } catch (IOException e) {
            String msg = e.toString();
            return Response.ok().entity(msg).build();
        }       
        return Response.ok()
            .entity(JavaScript.eval(jsFile, js, bInfo, bDebug))
            .build();
    }

    /* load and evaluate file Using JavaScript itself - this method doesn't use the cache 
     * but allows for URL or filename
    */
    @GET
    @Path("/jsload")
    @Produces(MediaType.TEXT_PLAIN)
    public static Response jsload(
        @QueryParam("jsPath") @DefaultValue("scripts/myscript.js") String jsPath,
        @QueryParam("info") @DefaultValue("no") String info,
        @QueryParam("debug") @DefaultValue("no") String debug ) { 
        boolean bInfo = info.toLowerCase().startsWith("y");
        boolean bDebug = debug.toLowerCase().startsWith("y");
        
        return Response.ok()
            .entity(JavaScript.jsLoad(jsPath, bInfo, bDebug ))
            .build();
    }

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

    /* show what is in the cache */
    @GET
    @Path("/listCache")
    @Produces(MediaType.TEXT_PLAIN)
            
    public static Response listCache() {
        
        return Response.ok()
            .entity(JavaScript.listCache())
            .build(); 
    }

    /* discard a file to the cache */
    @GET
    @Path("/discardCachedFile")
    @Produces(MediaType.TEXT_PLAIN)
        
    public static Response discardCachedFile(@QueryParam("jsPath") @DefaultValue("scripts/myscript.js") String jsPath) {
    
        return Response.ok()
            .entity(JavaScript.discardFile(jsPath))
            .build(); 
    }

    /* discard a file to the cache */
    @GET
    @Path("/about")
    @Produces(MediaType.TEXT_PLAIN)
            
    public static Response about() {
        
        return Response.ok()
            .entity(JavaScript.versionString())
            .build(); 
    }

}
