package com.github.chistousov.lib;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.chistousov.lib.exceptions.CreateInstanseException;
import com.github.chistousov.lib.tcp.TCP;
import com.github.chistousov.lib.tcp.TCPBuilder;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

class TCPTest {

    private static final int port = 9999;
    private static final String host = "localhost";

    private static int messageAmount;

    private static Logger log = LoggerFactory.getLogger(TCPTest.class);

    @Test
    @DisplayName("Test TCP")
    void tcp() throws InterruptedException, IOException{
        // given (instead of when)
        Path scenario = Paths.get("src", "test", "resources", "scenario");

        //for send messages to analyzer
        Sinks.Many<byte[]> flowMessagesForAnalyzer = Sinks
                                                        .many()
                                                        .multicast()
                                                        .onBackpressureBuffer();

        TCP serverTCPanalyzer = TCPBuilder
                                    .builder(TypeSideCommunication.SERVER, port)
                                    .setScenario(scenario)
                                    .build();
        
        TCP clientTCPanalyzer = TCPBuilder
                                    .builder(TypeSideCommunication.CLIENT, port)
                                    .setHost(host)
                                    .setMessageFlowForNetwork(flowMessagesForAnalyzer.asFlux())
                                    .build();

        Flux<byte[]> flowMessagesFromAnalyzer = clientTCPanalyzer.getMessageFlowFromNetwork();


        clientTCPanalyzer
            .getStdIn()
            .subscribe(el -> {
                log.info("STDIN: {}", el);
            });


        clientTCPanalyzer
            .getStdOut()
            .subscribe(el -> {
                log.info("STDOUT: {}", el);
            });

        clientTCPanalyzer
            .getStdErr()
            .subscribe(el -> {
                log.info("STDERR: {}", el);
            });


        List<String> linesScenario = Files.readAllLines(scenario, Charset.forName("UTF-8"));

        String typeFrame = null;
        String charEndLine = null;

        StringBuilder stringBuilder = null;

        List<String> sendMessages = new ArrayList<>();
        List<String> receiveMessages = new ArrayList<>();


        for (int i = 0; i < linesScenario.size(); i++) {
            String line = linesScenario.get(i);

            if (line.charAt(0) == '@') {

                if (stringBuilder != null) {
                    String message = stringBuilder.toString();

                    if (typeFrame.equals("@send")) {

                        sendMessages.add(message);

                    } else if (typeFrame.equals("@receive")) {

                        receiveMessages.add(message);

                    }
                    stringBuilder = null;
                }

                String[] metaDataFrame = line.split(" ");

                typeFrame = metaDataFrame[0];

                String errorFlag = metaDataFrame[2];

                if (errorFlag.equals("witherror")) {

                    List<Byte> bytesEndLineList =  Arrays.stream(metaDataFrame[4]
                                    .split("(?<=\\G.{2})"))
                                    .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                                    .collect(Collectors.toList());

                    byte[] bytesEndLineArray= new byte[bytesEndLineList.size()];
                    for(int k = 0; k < bytesEndLineList.size(); k++){
                        bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                    }
                    try{
                        charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                    } catch(UnsupportedEncodingException ex){
                        throw new CreateInstanseException("Encoding no support");
                    }


                } else {

                    List<Byte> bytesEndLineList =  Arrays.stream(metaDataFrame[3]
                    .split("(?<=\\G.{2})"))
                    .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                    .collect(Collectors.toList());

                    byte[] bytesEndLineArray= new byte[bytesEndLineList.size()];
                    for(int k = 0; k < bytesEndLineList.size(); k++){
                        bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                    }
                    try{
                        charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                    } catch(UnsupportedEncodingException ex){
                        throw new CreateInstanseException("Encoding no support");
                    }
                }

            } else {
                if (typeFrame == null || charEndLine == null) {
                    throw new CreateInstanseException(String.format("Error in line %d scenario %s", i + 1,
                            scenario.toAbsolutePath().toString()));
                }

                if (stringBuilder == null) {
                    stringBuilder = new StringBuilder();
                }
                stringBuilder.append(line.replaceAll("\r\n|\r|\n", "") + charEndLine);
            }
        }

        if (stringBuilder != null) {
            String message = stringBuilder.toString();

            if (typeFrame.equals("@send")) {

                sendMessages.add(message);

            } else if (typeFrame.equals("@receive")) {

                receiveMessages.add(message);

            }
            stringBuilder = null;
        }

        messageAmount = 0;

        flowMessagesFromAnalyzer
            .subscribe(mes -> {

                messageAmount++;

                if(messageAmount == 1){
                    try {
                        flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(0).getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if(messageAmount == 2) {
                    try {
                        flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(1).getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                } else if (messageAmount == 3){
                    try {
                        flowMessagesForAnalyzer.tryEmitNext(receiveMessages.get(2).getBytes("UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            });
        // when

        serverTCPanalyzer.start();
        
        Awaitility.await()
                .until(serverTCPanalyzer::isRunning);

        clientTCPanalyzer.start();

        Awaitility.await()
                .until(clientTCPanalyzer::isRunning);


        // then (instead of verify)

        assertTrue(serverTCPanalyzer.isRunning());
        assertTrue(clientTCPanalyzer.isRunning());

        StepVerifier
            .create(flowMessagesFromAnalyzer.map(el -> {
                try {
                    return new String(el, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return "";
                }
            }).log(), 6)
            .expectNext(sendMessages.get(0))
            .expectNext(sendMessages.get(1))
            .expectNext(sendMessages.get(2))

            .expectNext(sendMessages.get(3))
            .expectNext(sendMessages.get(4))
            .expectNext(sendMessages.get(5))
            
            .thenCancel()
            .verify();

        serverTCPanalyzer.stop();

        clientTCPanalyzer.stop();
    }
}
