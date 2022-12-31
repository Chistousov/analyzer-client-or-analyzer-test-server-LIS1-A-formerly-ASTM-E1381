package com.github.chistousov.lib.tcp.scenario.frame.impl;

import com.github.chistousov.lib.tcp.scenario.frame.AbstractFrame;
import com.github.chistousov.lib.tcp.scenario.frame.builder.ReceiveFrameBuilder;
import com.github.chistousov.lib.tcp.scenario.type.NAKStartCommunication;

/**
 * <p>
 * Received frame (Полученный фрейм)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class ReceiveFrame extends AbstractFrame {
    // Do I need to send a NAK when interacting? (for the test analyzer server) (Нужно ли отправлять NAK при взаимодействии? (для тестового сервер-анализатора))
    private NAKStartCommunication nakStartCommunication;

    /**
     * <p>
     * Received frame (Полученный фрейм)
     * </p>
     * 
     * @param receiveFrameBuilder - builder to build received frame
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public ReceiveFrame(ReceiveFrameBuilder receiveFrameBuilder){
        super(receiveFrameBuilder.getFrame(), receiveFrameBuilder.getErrors());
        
        this.nakStartCommunication = receiveFrameBuilder.getNakStartCommunication();
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
}
