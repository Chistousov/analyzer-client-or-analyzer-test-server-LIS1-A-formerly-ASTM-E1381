package com.github.chistousov.lib.exceptions;

/**
 * <p>
 * Error creating interaction participant (Ошибка при создании участника взаимодействия)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public class CreateInstanseException extends RuntimeException {
    /**
     * <p>
     * Error creating interaction participant (Ошибка при создании участника взаимодействия)
     * </p>
     *
     * @param message - error message
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public CreateInstanseException(String message) {
        super(message);
    }
}
