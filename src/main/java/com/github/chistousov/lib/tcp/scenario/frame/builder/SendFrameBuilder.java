package com.github.chistousov.lib.tcp.scenario.frame.builder;

import com.github.chistousov.lib.tcp.scenario.frame.AbstractFrame;
import com.github.chistousov.lib.tcp.scenario.frame.impl.SendFrame;
import com.github.chistousov.lib.tcp.scenario.type.CrushingType;

/**
 * <p>
 * builder to build frame to send (builder для сборки кадра для отправки)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class SendFrameBuilder {
    // message frame
    private String frame;
    
    // how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
    private byte errors;

    // The message is transmitted in whole or in parts (for a test server-analyzer) (Передается сообщение полностью или частями (для тестового сервер-анализатора))
    private CrushingType crushingType;

    /**
     * <p>
     * Start of construction of frame to send (Начало строительства сообщения для отправки)
     * </p>
     *
     * @param frame message frame
     * @return SendFrameBuilder
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public static SendFrameBuilder builder(String frame){
        return new SendFrameBuilder(frame);
    }

    /**
     * <p>
     * Start of construction of frame to send (Начало строительства сообщения для отправки)
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    private SendFrameBuilder(String frame){
        this.frame = frame;
    }

    /**
     * <p>
     * The message is transmitted in whole or in parts (for a test server-analyzer) (Передается сообщение полностью или частями (для тестового сервер-анализатора))
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public CrushingType getCrushingType() {
        return crushingType;
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
     * The message is transmitted in whole or in parts (for a test server-analyzer) (Передается сообщение полностью или частями (для тестового сервер-анализатора))
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public SendFrameBuilder setCrushingType(CrushingType crushingType) {
        this.crushingType = crushingType;
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
    public SendFrameBuilder setErrors(byte errors) {
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
    public SendFrameBuilder setFrame(String frame) {
        this.frame = frame;
        return this;
    }


    /**
     * <p>
     * End of construction of frame to send (Конец строительства сообщения для отправки)
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public AbstractFrame build(){
        return new SendFrame(this);
    }
}

