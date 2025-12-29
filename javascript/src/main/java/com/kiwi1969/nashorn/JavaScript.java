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
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

@ApplicationPath("/nashorn")
public class JavaScript extends Application {

    final static String[] arguments = new String[] {"-strict --language=es6","-dump-on-error"};

    //Share one engine for all threads
    //If use just 1 engine, caching of compiled objects occurs!
    //Note : Engines are thread-safe, Bindings are not
    static ScriptEngine engine = null;

    /* one off process when class loads */
    static {
        System.out.println(versionString()); /* Note this runs init() in engine == null) */
    }

    public static void main(String[] args) throws ScriptException {
        
        final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        final ScriptEngine engine = factory.getScriptEngine(arguments);
        final StringWriter sw = new StringWriter();
        final ScriptContext context = engine.getContext();
        context.setWriter(sw);
        context.setErrorWriter(sw);
        System.out.println(versionString());
        engine.eval("print('hello world')");
        System.out.println(sw);
    }

    static void init() {
        if (engine == null) {
            final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
            engine = factory.getScriptEngine(arguments);
        }
    }

    /* Note there is this nice extension pack here : https://github.com/efwGrp/nashorn-ext-for-es6
     * Copy to home folder, then
     * Add this line to get a bunch of ECMA2015 functionality : load("nashorn-ext-for-es6.min.js");
     */

    public static String eval( String name, String js, boolean bInfo, boolean bDebug) {

        final StringWriter sw = new StringWriter();  
        final ScriptContext context = engine.getContext();
        context.setWriter(sw);
        context.setErrorWriter(sw);
        try {
            /* If no script supplied, then load it */
            if (js.isEmpty() && !name.isEmpty()) {
                if (bInfo)
                    sw.write("Loading " + name + " (using Java)\n");
                js = new String(Files.readAllBytes(Paths.get(name)));
            }

            if (bInfo) {
                sw.write("Received " + name + " : \n" + js + "\n========\n");

                sw.write("\nResponse : \n========\n");
                long start = Instant.now().toEpochMilli();
                engine.eval(js);
                long duration = Instant.now().toEpochMilli() - start;
                sw.write("\nJavascript engine Response Time = " + duration + "ms");
                
            }
            else
                engine.eval(js);
        } catch (IOException | ScriptException e) {
            sw.write(e.getMessage());
        }
    
        return sw.toString();
    } 

    public static String evalURL( String name, boolean bInfo, boolean optDebug) {
        StringWriter sw = new StringWriter();
        if (bInfo)
            sw.write("Nashorn is Loading URL = " + name + " (utilizing Nashorn's internal cache)\n\n");
        
        final ScriptContext context = engine.getContext();
        context.setWriter(sw);
        context.setErrorWriter(sw);

        try {
            if (bInfo) {
                sw.write("Response : \n========\n");
                long start = Instant.now().toEpochMilli();
                engine.eval(new FileReader(name));
                long duration = Instant.now().toEpochMilli() - start;
                sw.write("\nJavascript engine Response Time = " + duration + "ms");
            }
            else
                engine.eval(new FileReader(name));
        }
        catch (FileNotFoundException | ScriptException e) {
            sw.write(e.getMessage());
        } 

        return sw.toString();

    }

    public static String versionString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Nashorn for CICS v1.00 (evaluation/demonstration version - not for commerical use)\n");
        sb.append("Copyright (c) 2025 Russell Shaw Software. All Rights Reserved\n");

        if (engine == null)
            init();

        NashornScriptEngineFactory factory = (NashornScriptEngineFactory) engine.getFactory();     
        sb.append(factory.getEngineName() + " " + factory.getEngineVersion() + "\n");

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

        boolean gotInput = false;

        try {
            Container urlContainer = channel.getContainer("URL");
            outStr = eval(urlContainer.getString(), "", false, false);
            gotInput = true;
        } catch (ContainerErrorException | LengthErrorException | ChannelErrorException | CCSIDErrorException | CodePageErrorException e) {
            errStr = e.getMessage();
        }

        if (!gotInput)
            try {
                Container scriptContainer = channel.getContainer("JAVASCRIPT");
                outStr = eval("", scriptContainer.getString(), false, false);
                gotInput = true;
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