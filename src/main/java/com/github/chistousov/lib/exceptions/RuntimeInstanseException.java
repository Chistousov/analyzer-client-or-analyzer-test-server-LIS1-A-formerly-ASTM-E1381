package com.github.chistousov.lib.exceptions;

/**
 * <p>
 * Error during execution of interaction (Ошибка во время выполнения взаимодействия)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class RuntimeInstanseException extends RuntimeException {
    
    /**
     * <p>
     * Error during execution of interaction (Ошибка во время выполнения взаимодействия)
     * </p>
     *
     * @param message - error message
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public RuntimeInstanseException(String message) {
        super(message);
    }
    
    /**
     * <p>
     * Error during execution of interaction (Ошибка во время выполнения взаимодействия)
     * </p>
     *
     * @param message - error message
     * @param cause - nested error
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public RuntimeInstanseException(String message, Throwable cause) {
        super(message, cause);
    }
}
