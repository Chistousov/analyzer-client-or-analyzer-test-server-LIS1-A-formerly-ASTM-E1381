package com.github.chistousov.lib.tcp.scenario.frame;

import com.github.chistousov.lib.tcp.scenario.CreateFrameException;

/**
 * <p>
 * Base frame (Базовый фрейм)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public abstract class AbstractFrame {
    // message frame
    private String frame;
    
    // how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
    private byte errors;

    /**
     * <p>
     * Base frame (Базовый фрейм)
     * </p>
     *
     * @param frame - message frame
     * @param errors - how many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    protected AbstractFrame(String frame, byte errors){
        if(frame == null || frame.isEmpty()){
            throw new CreateFrameException("Frame cannot be empty");
        }
        this.frame = frame;
        this.errors = errors;
    }


    /**
     * <p>
     * Message frame
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
     * How many times do you need to emulate the error in the last message? (сколько раз нужно эмулировать ошибку в последнем сообщении?)
     * </p>
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public byte getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return frame;
    }
}
