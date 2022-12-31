package com.github.chistousov.lib.tcp.scenario;

/**
 * <p>
 * Frame creation error (Ошибка создания фрейма)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class CreateFrameException extends RuntimeException {
    
    /**
     * <p>
     * Frame creation error (Ошибка создания фрейма)
     * </p>
     *
     * @param message - error message
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public CreateFrameException(String message) {
        super(message);
    }
}