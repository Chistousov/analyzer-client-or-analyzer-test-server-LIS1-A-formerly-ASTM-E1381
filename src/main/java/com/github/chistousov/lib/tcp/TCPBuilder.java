package com.github.chistousov.lib.tcp;

import java.nio.file.Path;

import com.github.chistousov.lib.TypeSideCommunication;

import reactor.core.publisher.Flux;

/**
 * <p>
 * builder to build the interaction side(builder для создания стороны взаимодействия)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class TCPBuilder {
    // Scenario for server (сценарий для сервера)
    private Path scenario;

    // DNS server name (required for client) (DNS имя сервера (требуется для клиента) 
    private String host;

    // Server port (порт сервера)
    private int port;

    // Interaction side type (тип стороны взаимодействия)
    private TypeSideCommunication typeSideCommunication;

    // Message flow for network (for remote side) Поток сообщений для сети (для удаленной стороны)
    private Flux<byte[]> messageFlowForNetwork;

    /**
     * Start of construction of the object's TCP (Начало строительства TCP объекта)
     * 
     * @param typeSideCommunication Interaction side type (тип стороны взаимодействия)
     * @param port Server port (порт сервера)
     * @return TCPBuilder
     */
    public static TCPBuilder builder(TypeSideCommunication typeSideCommunication, int port){
        return new TCPBuilder(typeSideCommunication, port);
    }
    /**
     * Start of construction of the object's TCP (Начало строительства TCP объекта)
     * 
     * @param typeSideCommunication Interaction side type (тип стороны взаимодействия)
     * @param port Server port (порт сервера)
     */
    private TCPBuilder(TypeSideCommunication typeSideCommunication, int port){
        this.typeSideCommunication = typeSideCommunication;
        this.port = port;
    }

    /**
     * <p>
     * DNS server name (required for client) (DNS имя сервера (требуется для клиента)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public TCPBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * <p>
     * DNS server name (required for client) (DNS имя сервера (требуется для клиента)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>
     * Scenario for server (сценарий для сервера)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public TCPBuilder setScenario(Path scenario) {
        this.scenario = scenario;
        return this;
    }

    /**
     * <p>
     * Scenario for server (сценарий для сервера)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public Path getScenario() {
        return scenario;
    }

    /**
     * <p>
     * Server port (порт сервера)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>
     * Interaction side type (тип стороны взаимодействия)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public TypeSideCommunication getTypeSideCommunication() {
        return typeSideCommunication;
    }

    /**
     * <p>
     * Message flow for network (for remote side) Поток сообщений для сети (для удаленной стороны)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public TCPBuilder setMessageFlowForNetwork(Flux<byte[]> messageFlowForNetwork) {
        this.messageFlowForNetwork = messageFlowForNetwork;
        return this;
    }

    /**
     * <p>
     * Message flow for network (for remote side) Поток сообщений для сети (для удаленной стороны)
     * </p>
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public Flux<byte[]> getMessageFlowForNetwork() {
        return messageFlowForNetwork;
    }

    /**
     * End of construction of the object's TCP (Конец строительства TCP объекта)
     * 
     * @return new TCP object
     */
    public TCP build(){
        return new TCP(this);
    }
}
