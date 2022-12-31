package com.github.chistousov.lib.tcp;

/**
 * <p>
 * The phase the client or server is in (фаза, в которой находится клиент или сервер)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public enum State {
    NEUTRAL, ESTABLISHMENT_MYSELF, ESTABLISHMENT_REMOTE_SIDE, TRANSFER_MYSELF, TRANSFER_REMOTE_SIDE
}
