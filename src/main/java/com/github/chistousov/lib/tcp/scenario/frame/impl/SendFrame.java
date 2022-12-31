package com.github.chistousov.lib.tcp.scenario.frame.impl;

import com.github.chistousov.lib.tcp.scenario.frame.AbstractFrame;
import com.github.chistousov.lib.tcp.scenario.frame.builder.SendFrameBuilder;
import com.github.chistousov.lib.tcp.scenario.type.CrushingType;

/**
 * <p>
 * Frame to send (Фрейм для отправки)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class SendFrame extends AbstractFrame {
    // The message is transmitted in whole or in parts (for a test server-analyzer) (Передается сообщение полностью или частями (для тестового сервер-анализатора))
    private CrushingType crushingType;

    /**
     * <p>
     * Frame to send (Фрейм для отправки)
     * </p>
     * 
     * @param sendFrameBuilder - builder to build frame to send
     *
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public SendFrame(SendFrameBuilder sendFrameBuilder){
        super(sendFrameBuilder.getFrame(), sendFrameBuilder.getErrors());
        
        this.crushingType = sendFrameBuilder.getCrushingType();
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
}