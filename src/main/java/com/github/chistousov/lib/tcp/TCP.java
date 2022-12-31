package com.github.chistousov.lib.tcp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.chistousov.lib.TypeSideCommunication;
import com.github.chistousov.lib.exceptions.CreateInstanseException;
import com.github.chistousov.lib.exceptions.RuntimeInstanseException;
import com.github.chistousov.lib.tcp.scenario.frame.AbstractFrame;
import com.github.chistousov.lib.tcp.scenario.frame.builder.ReceiveFrameBuilder;
import com.github.chistousov.lib.tcp.scenario.frame.builder.SendFrameBuilder;
import com.github.chistousov.lib.tcp.scenario.frame.impl.ReceiveFrame;
import com.github.chistousov.lib.tcp.scenario.frame.impl.SendFrame;
import com.github.chistousov.lib.tcp.scenario.type.CrushingType;
import com.github.chistousov.lib.tcp.scenario.type.NAKStartCommunication;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitResult;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.NettyInbound;
import reactor.netty.NettyOutbound;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;

/**
 * <p>
 * TCP/IP interaction object (client or server)(Объект взаимодействия по TCP/IP
 * (клиент или сервер)).
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class TCP {
    private static Logger log = LoggerFactory.getLogger(TCP.class);

    private static final String FULLY = "fully";
    private static final String TRY_SEND_MESSAGE_TO_CLIENT = "Try send message {} to CLIENT";
    private static final String RECEIVED_NOISE = "Received noise";
    private static final String SERVER_TO_CLIENT = "SERVER -->> CLIENT";
    private static final String CLIENT_TO_SERVER = "CLIENT -->> SERVER";
    private static final int MAX_FRAME_SIZE = 64_000;

    // the phase the client or server is in (фаза, в которой находится клиент или
    // сервер)
    private State statePhase;

    // for sending frames
    private byte[][] currentFramesToSend;
    private int indexCurrentFrameToSend;
    // FN 0-7
    private byte currentFrameNumberSend;

    // for receiving frames
    private byte[] bufferReceiveData;
    private byte[] bufferReceiveFramesData;
    // FN 0-7
    private byte currentFrameNumberReceive;

    // client or server
    private TypeSideCommunication typeSideCommunication;

    // number of attempts to send a message (6.5.1.2)
    private int numberOfAttemptsToSendMessage;

    // server or client is running
    private boolean isRunning = false;

    // ---- SERVER ----

    //
    // scenario for server
    private Path scenario;

    // object to stop the server
    private DisposableServer disposableServer;

    // object server
    private TcpServer tcpServer;

    // to simulate sending in parts
    private static final int MAX_FRAME_SIZE_FOR_SIMULATE = 50;

    // current CrushingType for send
    private CrushingType currentCrushingType;

    // current frame to send
    private byte[] currentSendMessageBytesSimulate;

    // how much to simulate an erroneous situation
    private byte imitationErrorAmount;

    // check the message from the client
    private String shouldBeReceiveStr;

    // simulation messages
    private List<AbstractFrame> abstractMessages;

    // ---- ----

    // ---- CLIENT ----

    // object to stop the client
    private Connection connectionClient;

    // object client
    private TcpClient tcpClient;

    // message flow for network
    private Queue<byte[]> messageFlowForNetworkQueue;

    // message flow from network
    private Sinks.Many<byte[]> messageFlowFromNetwork;

    // ---- ----

    // Events related to sending something to the analyzer
    private Sinks.Many<String> sinksStdIn;

    // Events related to the arrival of something from the analyzer
    private Sinks.Many<String> sinksStdOut;

    // Events of interaction errors with the analyzer
    private Sinks.Many<String> sinksStdErr;

    /**
     * <p>
     * Events related to sending something to the analyzer
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public Flux<String> getStdIn() {
        return sinksStdIn
                .asFlux()
                .publishOn(Schedulers.newSingle("stdin thread", true));
    }

    /**
     * <p>
     * Events related to the arrival of something from the analyzer
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public Flux<String> getStdOut() {
        return sinksStdOut
                .asFlux()
                .publishOn(Schedulers.newSingle("stdout thread", true));
    }

    /**
     * <p>
     * Events of interaction errors with the analyzer
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public Flux<String> getStdErr() {
        return sinksStdErr
                .asFlux()
                .publishOn(Schedulers.newSingle("stderror thread", true));
    }

    public TCP(TCPBuilder tcpBuilder) {

        if (tcpBuilder.getPort() <= 0) {
            throw new CreateInstanseException("Port cannot be zero");
        }

        if (tcpBuilder.getTypeSideCommunication() == null) {
            throw new CreateInstanseException("TypeSideCommunication should not be null");
        }

        if (tcpBuilder.getTypeSideCommunication() == TypeSideCommunication.SERVER && tcpBuilder.getScenario() == null) {
            throw new CreateInstanseException("Scenario should not be null when TypeSideCommunication = SERVER");
        }

        if (tcpBuilder.getTypeSideCommunication() == TypeSideCommunication.CLIENT
                && tcpBuilder.getMessageFlowForNetwork() == null) {
            throw new CreateInstanseException(
                    "MessageFlowForNetwork should not be null when TypeSideCommunication = CLIENT");
        }

        if (tcpBuilder.getTypeSideCommunication() == TypeSideCommunication.CLIENT && tcpBuilder.getHost() == null) {
            throw new CreateInstanseException("Host should not be null when TypeSideCommunication = CLIENT");
        }

        this.typeSideCommunication = tcpBuilder.getTypeSideCommunication();

        this.statePhase = State.NEUTRAL;

        this.sinksStdIn = Sinks
                // may have multiple subscribers
                .many()
                .multicast()
                // if there are no subscribers messages are buffered
                .onBackpressureBuffer(10);

        this.sinksStdOut = Sinks
                // may have multiple subscribers
                .many()
                .multicast()
                // if there are no subscribers messages are buffered
                .onBackpressureBuffer(10);

        this.sinksStdErr = Sinks
                // may have multiple subscribers
                .many()
                .multicast()
                // if there are no subscribers messages are buffered
                .onBackpressureBuffer(10);

        if (this.typeSideCommunication == TypeSideCommunication.SERVER) {
            this.messageFlowFromNetwork = null;
            this.messageFlowForNetworkQueue = null;

            this.abstractMessages = new ArrayList<>();

            this.scenario = tcpBuilder.getScenario();

            List<String> linesScenario;
            try {
                linesScenario = Files.readAllLines(scenario, StandardCharsets.UTF_8);
            } catch (IOException e1) {
                throw new CreateInstanseException(String.format(
                        "I/O error occurs reading from the file or a malformed or unmappable byte sequence is read (scenario %s)",
                        scenario.toAbsolutePath().toString()));
            }

            String typeFrame = null;
            String typeCrushing = null;
            String typeNAK = null;
            byte amountErrors = 0;
            String charEndLine = null;

            StringBuilder stringBuilder = null;

            // reading scenario

            for (int i = 0; i < linesScenario.size(); i++) {
                String line = linesScenario.get(i);

                if (line.charAt(0) == '@') {

                    if (stringBuilder != null) {
                        String message = stringBuilder.toString();

                        if (typeFrame.equals("@send")) {

                            abstractMessages.add(
                                    SendFrameBuilder
                                            .builder(message)
                                            .setErrors(amountErrors)
                                            .setCrushingType(Objects.nonNull(typeCrushing) && typeCrushing.equals(FULLY)
                                                    ? CrushingType.FULLY
                                                    : CrushingType.PARTS)
                                            .build());

                        } else if (typeFrame.equals("@receive")) {

                            abstractMessages.add(
                                    ReceiveFrameBuilder
                                            .builder(message)
                                            .setErrors(amountErrors)
                                            .setNakStartCommunication(
                                                    Objects.nonNull(typeNAK) && typeNAK.equals("withNAK")
                                                            ? NAKStartCommunication.WITH_NAK
                                                            : NAKStartCommunication.WITHOUT_NAK)
                                            .build());

                        }
                        stringBuilder = null;
                    }

                    String[] metaDataFrame = line.split(" ");

                    typeFrame = metaDataFrame[0];

                    String flag1 = metaDataFrame[1];

                    if (flag1.equals(FULLY) || flag1.equals("parts")) {
                        typeCrushing = flag1;
                    } else {
                        typeNAK = flag1;
                    }

                    String errorFlag = metaDataFrame[2];

                    if (errorFlag.equals("witherror")) {
                        amountErrors = Byte.valueOf(metaDataFrame[3]);

                        List<Byte> bytesEndLineList = Arrays.stream(metaDataFrame[4]
                                .split("(?<=\\G.{2})"))
                                .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                                .collect(Collectors.toList());

                        byte[] bytesEndLineArray = new byte[bytesEndLineList.size()];
                        for (int k = 0; k < bytesEndLineList.size(); k++) {
                            bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                        }
                        try {
                            charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                        } catch (UnsupportedEncodingException ex) {
                            throw new CreateInstanseException("Encoding no support");
                        }

                    } else {
                        amountErrors = (byte) -1;

                        List<Byte> bytesEndLineList = Arrays.stream(metaDataFrame[3]
                                .split("(?<=\\G.{2})"))
                                .map(el -> Byte.valueOf(Integer.valueOf(el, 16).byteValue()))
                                .collect(Collectors.toList());

                        byte[] bytesEndLineArray = new byte[bytesEndLineList.size()];
                        for (int k = 0; k < bytesEndLineList.size(); k++) {
                            bytesEndLineArray[k] = bytesEndLineList.get(k).byteValue();
                        }
                        try {
                            charEndLine = new String(bytesEndLineArray, "UTF-8").intern();
                        } catch (UnsupportedEncodingException ex) {
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

                    abstractMessages.add(
                            SendFrameBuilder
                                    .builder(message)
                                    .setErrors(amountErrors)
                                    .setCrushingType(Objects.nonNull(typeCrushing) && typeCrushing.equals(FULLY)
                                            ? CrushingType.FULLY
                                            : CrushingType.PARTS)
                                    .build());

                } else if (typeFrame.equals("@receive")) {

                    abstractMessages.add(
                            ReceiveFrameBuilder
                                    .builder(message)
                                    .setErrors(amountErrors)
                                    .setNakStartCommunication(Objects.nonNull(typeNAK) && typeNAK.equals("withNAK")
                                            ? NAKStartCommunication.WITH_NAK
                                            : NAKStartCommunication.WITHOUT_NAK)
                                    .build());

                }
                stringBuilder = null;
            }

            this.tcpServer = TcpServer
                    .create()
                    .port(tcpBuilder.getPort())
                    .doOnConnection(connection -> {
                        log.info("Connected client {}", connection.address());

                        if (this.abstractMessages.isEmpty()) {
                            stop();
                        }

                        if (!this.abstractMessages.isEmpty() && (this.abstractMessages.get(0) instanceof SendFrame)) {

                            SendFrame sendFrame = (SendFrame) this.abstractMessages.get(0);
                            this.currentCrushingType = sendFrame.getCrushingType();
                            this.imitationErrorAmount = sendFrame.getErrors();
                            this.currentSendMessageBytesSimulate = sendFrame.getFrame()
                                    .getBytes(StandardCharsets.UTF_8);

                            log.info(TRY_SEND_MESSAGE_TO_CLIENT, sendFrame.getFrame());

                            try {
                                Thread.sleep(1_000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                Thread.currentThread().interrupt();
                            }

                            this.abstractMessages.remove(0);

                        }

                        sendToRemoteSide(connection.outbound(), new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

                        this.statePhase = State.ESTABLISHMENT_MYSELF;

                    })
                    .handle(this::handlerOnePeer)
                    .wiretap(true);

        } else {

            this.messageFlowFromNetwork = Sinks
                    // may have multiple subscribers
                    .many()
                    .multicast()
                    // if there are no subscribers messages are buffered
                    .onBackpressureBuffer();

            this.messageFlowForNetworkQueue = new ConcurrentLinkedQueue<>();
            tcpBuilder.getMessageFlowForNetwork()
                    .publishOn(Schedulers.newSingle("Thread inserting into queue", true))
                    .subscribe(
                            el -> this.messageFlowForNetworkQueue.offer(el),
                            ex -> log.error("Error inserting into queue", ex),
                            () -> log.error("Complete inserting into queue"));

            this.tcpClient = TcpClient
                    .create()
                    .host(tcpBuilder.getHost())
                    .port(tcpBuilder.getPort())
                    .handle(this::handlerOnePeer)
                    .wiretap(true);

        }
    }

    public synchronized void start() throws RuntimeInstanseException {

        try {
            if (this.typeSideCommunication == TypeSideCommunication.SERVER && this.disposableServer != null) {
                throw new RuntimeInstanseException("Server is running");
            }
            if (this.typeSideCommunication == TypeSideCommunication.CLIENT && this.connectionClient != null) {
                throw new RuntimeInstanseException("Client is running");
            }

        } catch (Exception ex) {

            EmitResult emitResult;

            for (int i = 0; i < 5; i++) {
                log.debug("Try {} pull frame to stderr", i + 1);

                emitResult = this.sinksStdErr
                        .tryEmitNext(String.format("error: %s phase: %s", ex.getMessage(), this.statePhase));

                if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                    break;
                } else {
                    log.warn("Failed try {} pull frame to stderr ({}) ", i + 1, emitResult);
                }
            }

            throw ex;
        }

        // starting a client or server blocks the main thread, so we run it in a
        // separate thread
        Thread startNettyThread = new Thread(() -> {

            try {
                if (this.typeSideCommunication == TypeSideCommunication.SERVER) {
                    this.disposableServer = this.tcpServer.bindNow();
                    this.isRunning = true;
                    this.disposableServer.onDispose().block();
                } else {
                    this.isRunning = true;

                    while (this.isRunning) {
                        this.connectionClient = this.tcpClient.connectNow();
                        this.connectionClient.onDispose().block();

                        // try 5 time
                        EmitResult emitResult;

                        for (int i = 0; i < 5; i++) {
                            log.debug("Try {} pull frame to stdin", i + 1);

                            emitResult = this.sinksStdIn
                                    .tryEmitNext("reconnecting to server");

                            if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                                break;
                            } else {
                                log.warn("Failed try {} pull frame to stdin ({}) ", i + 1, emitResult);
                            }
                        }

                        if (this.connectionClient != null) {
                            this.connectionClient.channel().close();
                        }
                    }

                }
            } catch (Exception ex) {

                isRunning = false;

                EmitResult emitResult;

                for (int i = 0; i < 5; i++) {
                    log.debug("Try {} pull frame to stderr", i + 1);

                    emitResult = this.sinksStdErr
                            .tryEmitNext(String.format("error: %s phase: %s", ex.getMessage(), this.statePhase));

                    if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                        break;
                    } else {
                        log.warn("Failed try {} pull frame to stderr ({}) ", i + 1, emitResult);
                    }
                }

                throw ex;
            }

        });
        startNettyThread.setName(String.format("Netty Start Thread TCP (%s)", this.typeSideCommunication));
        startNettyThread.setDaemon(true);
        startNettyThread.start();
    }

    public synchronized void stop() throws RuntimeInstanseException {
        try {
            if (this.typeSideCommunication == TypeSideCommunication.SERVER && this.disposableServer == null) {
                throw new RuntimeInstanseException("Server is not running");
            }
            if (this.typeSideCommunication == TypeSideCommunication.CLIENT && this.connectionClient == null) {
                throw new RuntimeInstanseException("Client is not running");
            }

            if (this.typeSideCommunication == TypeSideCommunication.SERVER) {
                this.disposableServer.channel().close();
                this.disposableServer = null;
                this.isRunning = false;
            } else {
                this.connectionClient.channel().close();
                this.connectionClient = null;
                this.isRunning = false;
            }

        } catch (Exception ex) {

            EmitResult emitResult;

            for (int i = 0; i < 5; i++) {
                log.debug("Try {} pull frame to stderr", i + 1);

                emitResult = this.sinksStdErr
                        .tryEmitNext(String.format("error: %s phase: %s", ex.getMessage(), this.statePhase));

                if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                    break;
                } else {
                    log.warn("Failed try {} pull frame to stderr ({}) ", i + 1, emitResult);
                }
            }

            throw ex;
        }
    }

    public synchronized boolean isRunning() {
        return this.isRunning;

    }

    public Flux<byte[]> getMessageFlowFromNetwork() {
        return messageFlowFromNetwork
                .asFlux()
                .publishOn(Schedulers.newSingle("Message flow from network thread", true));
    }

    private void finalizeReceiveData() {
        this.bufferReceiveData = null;

        this.currentFrameNumberReceive = -1;
    }

    private void initSendData() {

        this.currentFramesToSend = null;
        this.indexCurrentFrameToSend = 0;

        this.currentFrameNumberSend = 0;
    }

    private void finalizeSendData() {
        this.currentFramesToSend = null;
        this.indexCurrentFrameToSend = -1;

        this.currentFrameNumberSend = -1;
    }

    private Publisher<Void> handlerOnePeer(NettyInbound inbound, NettyOutbound outbound) {
        return inbound
                .receive()
                .timeout(Duration.ofSeconds(15))
                .doOnNext(inByteBuf -> {

                    try {
                        if (inByteBuf.readableBytes() == 1) {

                            byte commandByte = inByteBuf.getByte(0);

                            CommonCommandASTM1381 command = CommonCommandASTM1381.getCommonCommandByNumber(commandByte);
                            if (command == CommonCommandASTM1381.NULL) {
                                throw new RuntimeInstanseException(String.format("Unknown command received %s ",
                                        new String(new byte[] { commandByte }, StandardCharsets.UTF_8)));
                            }

                            log.info("Command\r\nRECEIVED {}\r\n{}\r\n({} state is {})",
                                    this.typeSideCommunication == TypeSideCommunication.SERVER ? CLIENT_TO_SERVER
                                            : SERVER_TO_CLIENT,
                                    command, this.typeSideCommunication, this.statePhase);

                            // try 5 time
                            EmitResult emitResult;

                            for (int i = 0; i < 5; i++) {
                                log.debug("Try {} pull frame to stdout", i + 1);

                                emitResult = this.sinksStdOut
                                        .tryEmitNext(String.format("command: %s phase: %s", command, this.statePhase));

                                if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                                    break;
                                } else {
                                    log.warn("Failed try {} pull frame to stdout ({}) ", i + 1, emitResult);
                                }
                            }

                            handlerCommand(outbound, command, this.typeSideCommunication);

                        } else if (inByteBuf.readableBytes() > 1 && this.statePhase == State.TRANSFER_REMOTE_SIDE) {

                            int readableBytes = inByteBuf.readableBytes();

                            if (this.bufferReceiveData == null) {

                                this.bufferReceiveData = new byte[readableBytes];
                                inByteBuf.readBytes(this.bufferReceiveData, 0, readableBytes);

                            } else {

                                byte[] payload = new byte[inByteBuf.readableBytes()];
                                inByteBuf.readBytes(payload, 0, readableBytes);

                                byte[] receivedData = new byte[this.bufferReceiveData.length];
                                System.arraycopy(this.bufferReceiveData, 0, receivedData, 0, receivedData.length);

                                this.bufferReceiveData = new byte[receivedData.length + payload.length];
                                System.arraycopy(receivedData, 0, this.bufferReceiveData, 0, receivedData.length);
                                System.arraycopy(payload, 0, this.bufferReceiveData, receivedData.length,
                                        payload.length);
                            }

                            String displayStr = CommonCommandASTM1381.displayCommandByte(
                                    new String(this.bufferReceiveData, StandardCharsets.UTF_8).intern());

                            log.info("Bytes\r\nRECEIVED {}\r\n{}\r\n({} state is {})",
                                    this.typeSideCommunication == TypeSideCommunication.SERVER ? CLIENT_TO_SERVER
                                            : SERVER_TO_CLIENT,
                                    displayStr,
                                    this.typeSideCommunication, this.statePhase);
                            // try 5 time
                            EmitResult emitResult;

                            for (int i = 0; i < 5; i++) {
                                log.debug("Try {} pull bytes to stdout", i + 1);

                                emitResult = this.sinksStdOut
                                        .tryEmitNext(String.format("bytes: %s phase: %s", displayStr, this.statePhase));

                                if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                                    break;
                                } else {
                                    log.warn("Failed try {} pull bytes to stdout ({}) ", i + 1, emitResult);
                                }
                            }


                            if(this.bufferReceiveData == null 
                                || 
                                CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[0]) != CommonCommandASTM1381.STX
                                ||
                                this.bufferReceiveData.length > MAX_FRAME_SIZE
                            ){
                                throw new RuntimeInstanseException(RECEIVED_NOISE);
                            }

                            if(this.bufferReceiveData.length > 8
                                &&
                                CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[0]) == CommonCommandASTM1381.STX
                                &&
                                CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[this.bufferReceiveData.length - 1]) == CommonCommandASTM1381.LF
                                &&
                                CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[this.bufferReceiveData.length - 2]) == CommonCommandASTM1381.CR
                                &&
                                (
                                    CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[this.bufferReceiveData.length - 5]) == CommonCommandASTM1381.ETB
                                    ||
                                    CommonCommandASTM1381.getCommonCommandByNumber(this.bufferReceiveData[this.bufferReceiveData.length - 5]) == CommonCommandASTM1381.ETX
                                )
                            ) {

                                // remove STX
                                this.bufferReceiveData[0] = 0;

                                // remove LF and CR
                                this.bufferReceiveData[this.bufferReceiveData.length - 1] = 0;
                                this.bufferReceiveData[this.bufferReceiveData.length - 2] = 0;

                                byte c1 = this.bufferReceiveData[this.bufferReceiveData.length - 4];
                                byte c2 = this.bufferReceiveData[this.bufferReceiveData.length - 3];

                                // remove C1 and C2
                                this.bufferReceiveData[this.bufferReceiveData.length - 4] = 0;
                                this.bufferReceiveData[this.bufferReceiveData.length - 3] = 0;

                                // remove ETB or ETX
                                this.bufferReceiveData[this.bufferReceiveData.length - 5] = 0;

                                byte[] checksum = calculateChecksun(this.bufferReceiveData);

                                // check frame number
                                byte frameNumber = nextFrameNumber(currentFrameNumberReceive);

                                if (typeSideCommunication == TypeSideCommunication.SERVER
                                        && this.imitationErrorAmount > 0) {
                                    log.info("Imitation of an erroneous frame ({} state is {})",
                                            this.typeSideCommunication, this.statePhase);

                                    this.imitationErrorAmount--;

                                    sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.NAK.getNumber() });

                                } else if (checksum[0] == c1 && checksum[1] == c2
                                        && (String.valueOf(frameNumber).getBytes()[0]) == this.bufferReceiveData[1]) {

                                    sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ACK.getNumber() });

                                    currentFrameNumberReceive = frameNumber;

                                    if (this.bufferReceiveFramesData == null) {

                                        this.bufferReceiveFramesData = new byte[this.bufferReceiveData.length - 7];
                                        System.arraycopy(this.bufferReceiveData, 2, this.bufferReceiveFramesData, 0,
                                                this.bufferReceiveFramesData.length);

                                    } else {

                                        byte[] payload = new byte[this.bufferReceiveData.length - 7];
                                        System.arraycopy(this.bufferReceiveData, 2, payload, 0, payload.length);

                                        byte[] receivedData = new byte[this.bufferReceiveFramesData.length];
                                        System.arraycopy(this.bufferReceiveFramesData, 0, receivedData, 0,
                                                receivedData.length);

                                        this.bufferReceiveFramesData = new byte[receivedData.length + payload.length];
                                        System.arraycopy(receivedData, 0, this.bufferReceiveFramesData, 0,
                                                receivedData.length);
                                        System.arraycopy(payload, 0, this.bufferReceiveFramesData, receivedData.length,
                                                payload.length);
                                    }

                                } else {
                                    log.info("The message has an invalid checksum ({} state is {})",
                                            this.typeSideCommunication, this.statePhase);

                                    sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.NAK.getNumber() });
                                }
                                this.bufferReceiveData = null;
                            }

                        } else {

                            byte[] invalidFrame = new byte[inByteBuf.readableBytes()];
                            inByteBuf.readBytes(invalidFrame);

                            throw new RuntimeInstanseException(
                                    String.format("It is not known what to do with the received frame%n%s%n",
                                            new String(invalidFrame, StandardCharsets.UTF_8)));
                        }

                    } catch (Exception ex) {
                        log.error(String.format("doOnError (%s state is %s)", this.typeSideCommunication,
                                this.statePhase), ex);

                        // try 5 time
                        EmitResult emitResult;

                        for (int i = 0; i < 5; i++) {
                            log.debug("Try {} pull frame to stderr", i + 1);

                            emitResult = this.sinksStdErr.tryEmitNext(
                                    String.format("error: %s phase: %s", ex.getMessage(), this.statePhase));

                            if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                                break;
                            } else {
                                log.warn("Failed try {} pull frame to stderr ({}) ", i + 1, emitResult);
                            }
                        }

                        try {
                            Thread.sleep(2_000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                            Thread.currentThread().interrupt();
                        }

                        this.statePhase = State.NEUTRAL;

                        sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.EOT.getNumber() });
                    }

                })
                .then();
    }

    private void handlerCommand(NettyOutbound outbound, CommonCommandASTM1381 command,
            TypeSideCommunication typeSideCommunication) {

        if (CommonCommandASTM1381.ENQ == command && this.statePhase == State.NEUTRAL
                && typeSideCommunication == TypeSideCommunication.SERVER) {

            if (!this.abstractMessages.isEmpty() && (this.abstractMessages.get(0) instanceof SendFrame)) {

                SendFrame sendFrame = (SendFrame) this.abstractMessages.get(0);
                this.currentCrushingType = sendFrame.getCrushingType();
                this.imitationErrorAmount = sendFrame.getErrors();
                this.currentSendMessageBytesSimulate = sendFrame.getFrame().getBytes(StandardCharsets.UTF_8);

                log.info(TRY_SEND_MESSAGE_TO_CLIENT, sendFrame.getFrame());

                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                this.statePhase = State.ESTABLISHMENT_MYSELF;

                sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

                this.abstractMessages.remove(0);

            } else if (!this.abstractMessages.isEmpty() && (this.abstractMessages.get(0) instanceof ReceiveFrame)) {

                ReceiveFrame receiveFrame = (ReceiveFrame) this.abstractMessages.get(0);

                CommonCommandASTM1381 ackOrNak;

                if (receiveFrame.getNakStartCommunication() == NAKStartCommunication.WITH_NAK) {
                    this.statePhase = State.ESTABLISHMENT_REMOTE_SIDE;
                    ackOrNak = CommonCommandASTM1381.NAK;
                } else {
                    this.statePhase = State.TRANSFER_REMOTE_SIDE;
                    this.currentFrameNumberReceive = 0;
                    ackOrNak = CommonCommandASTM1381.ACK;

                }

                sendToRemoteSide(outbound, new byte[] { ackOrNak.getNumber() });

                this.imitationErrorAmount = receiveFrame.getErrors();
                this.shouldBeReceiveStr = receiveFrame.getFrame();

                this.abstractMessages.remove(0);

            } else {
                sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ACK.getNumber() });
                this.statePhase = State.TRANSFER_REMOTE_SIDE;
                this.currentFrameNumberReceive = 0;

            }

        } else if (CommonCommandASTM1381.ENQ == command
                && ((this.statePhase == State.NEUTRAL && this.typeSideCommunication == TypeSideCommunication.CLIENT)
                        ||
                        (this.statePhase == State.ESTABLISHMENT_MYSELF
                                && this.typeSideCommunication == TypeSideCommunication.CLIENT)
                        ||
                        (this.statePhase == State.ESTABLISHMENT_REMOTE_SIDE
                                && this.typeSideCommunication == TypeSideCommunication.SERVER)
                        ||
                        (this.statePhase == State.ESTABLISHMENT_REMOTE_SIDE
                                && this.typeSideCommunication == TypeSideCommunication.CLIENT))) {

            this.statePhase = State.TRANSFER_REMOTE_SIDE;
            this.currentFrameNumberReceive = 0;

            sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ACK.getNumber() });

        } else if (CommonCommandASTM1381.ENQ == command && statePhase == State.ESTABLISHMENT_MYSELF
                && this.typeSideCommunication == TypeSideCommunication.SERVER) {

            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }

            sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

        } else if (CommonCommandASTM1381.ACK == command && statePhase == State.ESTABLISHMENT_MYSELF) {

            initSendData();

            this.statePhase = State.TRANSFER_MYSELF;

            // some bytes from external environment to send
            byte[] byteForSendRemoteSide = null;

            int maxFrameSize = 0;

            try {
                if (typeSideCommunication == TypeSideCommunication.SERVER) {
                    maxFrameSize = currentCrushingType == CrushingType.PARTS ? MAX_FRAME_SIZE_FOR_SIMULATE
                            : currentSendMessageBytesSimulate.length;

                    byteForSendRemoteSide = this.currentSendMessageBytesSimulate;
                } else {
                    maxFrameSize = MAX_FRAME_SIZE;

                    byteForSendRemoteSide = this.messageFlowForNetworkQueue.poll();
                }

                if (byteForSendRemoteSide == null) {
                    throw new RuntimeInstanseException("Bytes for send is null");
                }

            } catch (Exception ex) {
                throw new RuntimeInstanseException("Nothing to send", ex);
            }

            if (byteForSendRemoteSide.length > maxFrameSize) {
                int amountFrame = (byteForSendRemoteSide.length / maxFrameSize) + 1;
                this.currentFramesToSend = new byte[amountFrame][];

                log.debug("byteForSendRemoteSide.length = {} , maxFrameSize = {}, amountFrame = {}",
                        byteForSendRemoteSide.length, maxFrameSize, amountFrame);

                for (int i = 0; i < amountFrame; i++) {

                    int len = (maxFrameSize * (i + 1)) <= byteForSendRemoteSide.length ? maxFrameSize
                            : byteForSendRemoteSide.length % maxFrameSize;

                    boolean isEndFrame = byteForSendRemoteSide.length <= (maxFrameSize * (i + 1));

                    log.debug("i = {}, len = {}, isEndFrame = {}", i, len, isEndFrame);

                    byte[] frame = new byte[len];
                    System.arraycopy(byteForSendRemoteSide, maxFrameSize * i, frame, 0, len);

                    this.currentFramesToSend[i] = createFrame(frame, isEndFrame);

                }
            } else {
                this.currentFramesToSend = new byte[1][];
                this.currentFramesToSend[0] = createFrame(byteForSendRemoteSide, true);
            }

            this.sendCurrentFrameToRemoteSide(outbound);
            numberOfAttemptsToSendMessage = 1;

        } else if (CommonCommandASTM1381.ACK == command && this.statePhase == State.TRANSFER_MYSELF) {

            this.indexCurrentFrameToSend++;

            if (this.indexCurrentFrameToSend >= this.currentFramesToSend.length) {

                this.statePhase = State.NEUTRAL;

                sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.EOT.getNumber() });

            } else {
                this.sendCurrentFrameToRemoteSide(outbound);
                numberOfAttemptsToSendMessage = 1;
            }

        } else if (CommonCommandASTM1381.NAK == command && this.statePhase == State.ESTABLISHMENT_MYSELF) {

            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

        } else if (CommonCommandASTM1381.NAK == command && this.statePhase == State.TRANSFER_MYSELF) {

            /*
             * 6.5.1.2 Upon receiving a <NAK> or any character except an <ACK> or <EOT> (a
             * <NAK> condition), the
             * sender increments a retransmit counter and retransmits the frame. If this
             * counter shows a single frame was sent and
             * not accepted six times, the sender must abort this message by proceeding to
             * the termination phase. An abort should
             * be extremely rare, but it provides a mechanism to escape from a condition
             * where the transfer phase cannot continue.
             */

            if (numberOfAttemptsToSendMessage >= 6) {

                log.info("Too much retransmits the frame");

                this.statePhase = State.NEUTRAL;

                sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.EOT.getNumber() });

            } else {
                this.sendCurrentFrameToRemoteSide(outbound, true);
                numberOfAttemptsToSendMessage++;
            }

        } else if (CommonCommandASTM1381.EOT == command) {

            if (this.bufferReceiveFramesData != null) {

                String currentReceiveStr = new String(this.bufferReceiveFramesData, StandardCharsets.UTF_8).intern();

                log.info("Message\r\nRECEIVED {}\r\n{}\r\n({} state is {})",
                        this.typeSideCommunication == TypeSideCommunication.SERVER ? CLIENT_TO_SERVER
                                : SERVER_TO_CLIENT,
                        CommonCommandASTM1381.displayCommandByte(currentReceiveStr), this.typeSideCommunication,
                        this.statePhase);

                if (this.typeSideCommunication == TypeSideCommunication.SERVER && this.shouldBeReceiveStr != null) {

                    log.info("Received message on CLIENT is {} expected ",
                            this.shouldBeReceiveStr.equals(currentReceiveStr)
                                    ? "EQUAL"
                                    : "NOT the same as");
                } else if (this.typeSideCommunication == TypeSideCommunication.CLIENT) {

                    EmitResult emitResult = null;

                    // try 5 time
                    for (int i = 0; i < 5; i++) {
                        log.debug("Try {} pull received message", i + 1);

                        emitResult = this.messageFlowFromNetwork.tryEmitNext(this.bufferReceiveFramesData);

                        if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                            break;
                        } else {
                            log.warn("Failed try {} pull received message ({}) ", i + 1, emitResult);
                        }
                    }

                    if (emitResult != EmitResult.OK && emitResult.isFailure()) {
                        log.error("Failed pull received message",
                                new RuntimeInstanseException("Failed pull received message"));
                    }

                    log.debug("Pulled received message");
                }

                this.bufferReceiveFramesData = null;
            }

            finalizeReceiveData();
            finalizeSendData();

            if (this.typeSideCommunication == TypeSideCommunication.SERVER
                    && this.statePhase == State.TRANSFER_REMOTE_SIDE) {

                currentSendMessageBytesSimulate = null;
                imitationErrorAmount = -1;
                shouldBeReceiveStr = null;

                if (!this.abstractMessages.isEmpty() && this.abstractMessages.get(0) instanceof SendFrame) {

                    SendFrame sendFrame = (SendFrame) this.abstractMessages.get(0);
                    this.currentCrushingType = sendFrame.getCrushingType();
                    this.imitationErrorAmount = sendFrame.getErrors();
                    this.currentSendMessageBytesSimulate = sendFrame.getFrame().getBytes(StandardCharsets.UTF_8);

                    log.info(TRY_SEND_MESSAGE_TO_CLIENT, sendFrame.getFrame());

                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }

                    this.abstractMessages.remove(0);

                    sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

                    this.statePhase = State.ESTABLISHMENT_MYSELF;

                } else if (this.abstractMessages.isEmpty()) {

                    stop();

                } else {

                    sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.EOT.getNumber() });

                    this.statePhase = State.NEUTRAL;
                }

            } else {
                sendToRemoteSide(outbound, new byte[] { CommonCommandASTM1381.ENQ.getNumber() });

                this.statePhase = State.ESTABLISHMENT_MYSELF;
            }

        } else {
            throw new RuntimeInstanseException(String.format("Unable to execute received command %s", command));
        }
    }

    private byte nextFrameNumber(byte currentFrameNumber) {
        currentFrameNumber = (byte) ((currentFrameNumber + 1) % 8);
        return currentFrameNumber;
    }

    private byte[] createFrame(byte[] payload, boolean isEndFrame) {

        byte[] frame = new byte[payload.length + 7];
        System.arraycopy(payload, 0, frame, 2, payload.length);

        currentFrameNumberSend = nextFrameNumber(currentFrameNumberSend);

        frame[1] = String.valueOf(currentFrameNumberSend).getBytes()[0];
        frame[frame.length - 5] = isEndFrame ? CommonCommandASTM1381.ETX.getNumber()
                : CommonCommandASTM1381.ETB.getNumber();

        // checksum
        byte[] checksum = calculateChecksun(frame);
        // C1
        frame[frame.length - 4] = checksum[0];
        // C2
        frame[frame.length - 3] = checksum[1];

        frame[0] = CommonCommandASTM1381.STX.getNumber();
        frame[frame.length - 2] = CommonCommandASTM1381.CR.getNumber();
        frame[frame.length - 1] = CommonCommandASTM1381.LF.getNumber();

        return frame;
    }

    private byte[] calculateChecksun(byte[] data) {

        return String
                .format("%02X",
                        IntStream
                                .range(0, data.length)
                                .map(index -> data[index] & 255)
                                .reduce(0, Integer::sum)
                                % 256)
                .intern()
                .getBytes();
    }

    private void sendToRemoteSide(NettyOutbound outbound, byte[] sendData) {

        String displayStr;

        if (sendData.length == 1) {
            displayStr = CommonCommandASTM1381.getCommonCommandByNumber(sendData[0]).toString().intern();
        } else {
            displayStr = CommonCommandASTM1381
                    .displayCommandByte(new String(sendData, StandardCharsets.UTF_8).intern());
        }

        log.info("{}\r\n{} SENT\r\n{}\r\n({} state is {})", sendData.length == 1 ? "command" : "frame",
                this.typeSideCommunication == TypeSideCommunication.SERVER ? SERVER_TO_CLIENT : CLIENT_TO_SERVER,
                displayStr, this.typeSideCommunication, this.statePhase);

        // try 5 time
        EmitResult emitResult;

        for (int i = 0; i < 5; i++) {
            log.debug("Try {} pull frame to stdin", i + 1);

            emitResult = this.sinksStdIn.tryEmitNext(String.format("frame: %s phase: %s", displayStr, this.statePhase));

            if (emitResult == EmitResult.OK && emitResult.isSuccess()) {
                break;
            } else {
                log.warn("Failed try {} pull frame to stdin ({}) ", i + 1, emitResult);
            }
        }

        outbound
                .sendByteArray(Mono.just(sendData))
                .then()
                .subscribe(
                        vvoid -> {
                        },
                        ex -> {
                            throw new RuntimeInstanseException(String.format("Error while sending data %s", displayStr),
                                    ex);
                        });
    }

    private void sendCurrentFrameToRemoteSide(NettyOutbound outbound) {
        byte[] currentFrame = new byte[this.currentFramesToSend[this.indexCurrentFrameToSend].length];

        System.arraycopy(this.currentFramesToSend[this.indexCurrentFrameToSend], 0, currentFrame, 0,
                this.currentFramesToSend[this.indexCurrentFrameToSend].length);

        if (this.imitationErrorAmount > 0 && this.indexCurrentFrameToSend == this.currentFramesToSend.length - 1) {
            currentFrame[4] += 4;
            this.imitationErrorAmount--;
        }

        sendToRemoteSide(outbound, currentFrame);
    }

    private void sendCurrentFrameToRemoteSide(NettyOutbound outbound, boolean recalculateChecksum) {
        if (recalculateChecksum) {
            byte[] currentFrame = this.currentFramesToSend[this.indexCurrentFrameToSend];

            currentFrame[0] = 0;
            currentFrame[currentFrame.length - 4] = 0;
            currentFrame[currentFrame.length - 3] = 0;
            currentFrame[currentFrame.length - 2] = 0;
            currentFrame[currentFrame.length - 1] = 0;

            byte ETBOrETX = currentFrame[currentFrame.length - 5];

            currentFrame[currentFrame.length - 5] = 0;
            // checksum
            byte[] checksum = calculateChecksun(currentFrame);

            currentFrame[currentFrame.length - 5] = ETBOrETX;

            // C1
            currentFrame[currentFrame.length - 4] = checksum[0];
            // C2
            currentFrame[currentFrame.length - 3] = checksum[1];

            currentFrame[0] = CommonCommandASTM1381.STX.getNumber();
            currentFrame[currentFrame.length - 2] = CommonCommandASTM1381.CR.getNumber();
            currentFrame[currentFrame.length - 1] = CommonCommandASTM1381.LF.getNumber();
        }

        sendCurrentFrameToRemoteSide(outbound);
    }

}
