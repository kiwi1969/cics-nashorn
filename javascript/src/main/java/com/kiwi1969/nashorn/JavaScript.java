package com.kiwi1969.nashorn;

import com.ibm.cics.server.CCSIDErrorException;
import com.ibm.cics.server.Channel;
import com.ibm.cics.server.ChannelErrorException;
import com.ibm.cics.server.CodePageErrorException;
import com.ibm.cics.server.Container;
import com.ibm.cics.server.ContainerErrorException;
import com.ibm.cics.server.InvalidRequestException;
import com.ibm.cics.server.LengthErrorException;
import com.ibm.cics.server.Task;
import com.ibm.cics.server.invocation.CICSProgram;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import javax.script.CompiledScript;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.openjdk.nashorn.api.scripting.NashornScriptEngine;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

@ApplicationPath("/nashorn")
public class JavaScript extends Application{
    
    protected static Map <String, CompiledScript> cache = new HashMap<>();

    public static String eval( String name, String js, boolean bInfo, boolean bDebug) {

        StringWriter sw = new StringWriter();
        if (bInfo)
            sw.write("Received " + name + " : \n" + js + "\n");
        
        NashornScriptEngine nse = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("Nashorn");       
        nse.getContext().setWriter(sw);

        CompiledScript compiled = null;

        if (!name.isEmpty() && cache.containsKey(name)) {
            compiled = cache.get(name);
            if (bInfo)
                sw.write("Executing cached Java bytecode...\n");
        }
        else {

            try {
                if (bInfo) {
                    sw.write("Compiling to Java bytecode...\n");
                    long start = Instant.now().toEpochMilli();
                    compiled = nse.compile(js);
                    long duration = Instant.now().toEpochMilli() - start;
                    sw.write("Compile Response Time = " + duration + "ms\n");
                    if (!name.isEmpty()) {
                        cache.put(name, compiled);
                        sw.write("Compiled Bytecode for " + name + " added to cache\n");
                    }
                }
                else {
                    compiled = nse.compile(js);
                    if (!name.isEmpty())
                        cache.put(name, compiled);
                }
            }
            catch (ScriptException e) {
                sw.write(e.getMessage());
                return sw.toString();
            }
        } 
        
        try {
            if (bInfo) {
                sw.write("\nResponse : \n========\n");
                long start = Instant.now().toEpochMilli();
                compiled.eval();
                long duration = Instant.now().toEpochMilli() - start;
                sw.write("\nJavascript Response Time = " + duration + "ms");
            }
            else
                compiled.eval();

        }
        catch (ScriptException e) {
            sw.write(e.getMessage());
        }

        return sw.toString();
    }

    public static String jsLoad( String filename, boolean bInfo, boolean optDebug) {
        StringWriter sw = new StringWriter();
        if (bInfo)
            sw.write("Loading " + filename + "\n\n");
        
        NashornScriptEngine nse = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("Nashorn");       
        nse.getContext().setWriter(sw);

        try {
            if (bInfo) {
                sw.write("Response : \n========\n");
                long start = Instant.now().toEpochMilli();
                nse.eval(new FileReader(filename));
                long duration = Instant.now().toEpochMilli() - start;
                sw.write("\nJavascript Response Time = " + duration + "ms");
            }
            else
                nse.eval(new FileReader(filename));
        }
        catch (FileNotFoundException|ScriptException e) {
            sw.write(e.getMessage());
        } 

        return sw.toString();

    }

    public static String listCache() {
        StringBuilder sb = new StringBuilder("JavaScript files with cached bytecode :");

        if (cache.isEmpty())
            sb.append("\nNo script currently in cache");
        else
            for (Map.Entry<String, CompiledScript> entry : cache.entrySet()) {
                sb.append("\n" + entry.getKey());
            }
        return sb.toString();
    }

    public static String discardFile(String key) {

        if (cache == null || key == null || key.isEmpty() )
            return "Invalid Request";

        if (cache.isEmpty() || !cache.containsKey(key))
            return "No such file in cache";

        cache.remove(key);
        return key + " was removed from cache";
    }

    public static String versionString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Nashorn for CICS v1.00 (evaluation/demonstration version - not for commerical use)\n");
        sb.append("Copyright © 2025 Russell Shaw Software. All Rights Reserved\n");

        NashornScriptEngine nse = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("Nashorn"); 
        NashornScriptEngineFactory factory = (NashornScriptEngineFactory) nse.getFactory();     
        sb.append(factory.getEngineName() + " " + factory.getEngineVersion());

        return sb.toString();
    }

    /* CICS entry point - payload is supplied as a text container called "JAVASCRIPT" */
    @CICSProgram("NASHORN")
    public static void nashorn() {

        String outStr = "";
        String errStr = "";
        Task t = Task.getTask();
        Channel channel = t.getCurrentChannel();
        if (channel == null) {
            System.err.print("No CICS Channel supplied to NASHORN");
            return;
        }

        try {
            Container scriptContainer = channel.getContainer("JAVASCRIPT");
            outStr = eval("", scriptContainer.getString(), false, false);
        } catch (ContainerErrorException | LengthErrorException | ChannelErrorException | CCSIDErrorException | CodePageErrorException e) {
            errStr = e.getMessage();
        }

        if (errStr.isEmpty() && !outStr.isEmpty())
            try {
                channel.createContainer("NASHORN_OUT").putString(outStr);
            } catch (InvalidRequestException | ContainerErrorException | ChannelErrorException | CCSIDErrorException | CodePageErrorException e) {
                errStr = e.getMessage();
            }

        if (!errStr.isEmpty())
            try {
                channel.createContainer("ERROR").putString(errStr);
            } catch (InvalidRequestException | ContainerErrorException | ChannelErrorException | CCSIDErrorException | CodePageErrorException e) {
                System.err.print(e.getMessage());
            }
    
    }

}