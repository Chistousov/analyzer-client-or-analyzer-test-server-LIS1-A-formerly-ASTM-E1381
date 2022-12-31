package com.github.chistousov.lib.tcp.scenario.frame.builder;

import com.github.chistousov.lib.tcp.scenario.frame.AbstractFrame;
import com.github.chistousov.lib.tcp.scenario.frame.impl.ReceiveFrame;
import com.github.chistousov.lib.tcp.scenario.type.NAKStartCommunication;

/**
 * <p>
 * builder to build received frame (builder для создания полученного сообщения)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class ReceiveFrameBuilder {
    // message frame
    private String frame;
    
    // how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
    private byte errors;
    
    // Do I need to send a NAK when interacting? (for the test analyzer server) (Нужно ли отправлять NAK при взаимодействии? (для тестового сервер-анализатора))
    private NAKStartCommunication nakStartCommunication;

    /**
     * Start of construction of received frame (Начало строительства полученного сообщения)
     * 
     * @param frame message frame
     * @return ReceiveFrameBuilder
     */
    public static ReceiveFrameBuilder builder(String frame){
        return new ReceiveFrameBuilder(frame);
    }
    /**
     * Start of construction of the received frame (Начало строительства полученного сообщения)
     * 
     * @param frame message frame
     */
    private ReceiveFrameBuilder(String frame){
        this.frame = frame;
    }
    /**
     * <p>
     * Do I need to send a NAK when interacting? (for the test analyzer server) (Нужно ли отправлять NAK при взаимодействии? (для тестового сервер-анализатора))
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public NAKStartCommunication getNakStartCommunication() {
        return nakStartCommunication;
    }
    /**
     * <p>
     * how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public byte getErrors() {
        return errors;
    }
    /**
     * <p>
     * message frame
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public String getFrame() {
        return frame;
    }

    /**
     * <p>
     * Do I need to send a NAK when interacting? (for the test analyzer server) (Нужно ли отправлять NAK при взаимодействии? (для тестового сервер-анализатора))
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public ReceiveFrameBuilder setNakStartCommunication(NAKStartCommunication nakStartCommunication) {
        this.nakStartCommunication = nakStartCommunication;
        return this;
    }

    /**
     * <p>
     * how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public ReceiveFrameBuilder setErrors(byte errors) {
        this.errors = errors;
        return this;
    }

    /**
     * <p>
     * message frame
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public ReceiveFrameBuilder setFrame(String frame) {
        this.frame = frame;
        return this;
    }

    /**
     * End of construction of received frame (Конец строительства полученного сообщения)
     * 
     * @return new TCP object
     */
    public AbstractFrame build(){
        return new ReceiveFrame(this);
    }
    
}