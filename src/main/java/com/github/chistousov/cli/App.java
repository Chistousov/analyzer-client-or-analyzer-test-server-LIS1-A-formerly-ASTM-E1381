package com.github.chistousov.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.github.chistousov.lib.TypeSideCommunication;
import com.github.chistousov.lib.exceptions.CreateInstanseException;
import com.github.chistousov.lib.tcp.TCP;
import com.github.chistousov.lib.tcp.TCPBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Entry point for launching the test server-analyzer(Точка входа для запуска тестового сервера-анализатора).
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class App {

    private static Logger log = LoggerFactory.getLogger(App.class);

    private static Object obj = new Object();

    public static void main(String[] args) throws InterruptedException, CreateInstanseException {

        Path scenario;

        // requires -Dscenario=/path/tp/file
        if(System.getProperty("scenario") != null){
            scenario = Paths.get(System.getProperty("scenario"));
        } else {
            throw new CreateInstanseException("File scenario not found");
        }

        TCP serverTCPanalyzer = TCPBuilder
                                    .builder(TypeSideCommunication.SERVER, 8888)
                                    .setScenario(scenario)
                                    .build();

        serverTCPanalyzer.start();

        log.info("Server is started (port 8888)");

        synchronized(obj){
            obj.wait();
        }       
    }
} 
