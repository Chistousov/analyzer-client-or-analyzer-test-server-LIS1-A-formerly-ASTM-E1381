package com.github.chistousov.lib.tcp;


import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * ASCII characters used in ASTM1381 (Символов ASCII используемые в ASTM1381)
 * </p>
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 */
public enum CommonCommandASTM1381 {
    NULL((byte) 0x00, "NULL"),
    ENQ((byte) 0x05, "ENQ"),
    ACK((byte) 0x06, "ACK"),
    NAK((byte) 0x15, "NAK"),
    STX((byte) 0x02, "STX"),
    ETB((byte) 0x17, "ETB"),
    ETX((byte) 0x03, "ETX"),
    CR((byte) 0x0D, "CR"),
    LF((byte) 0x0A, "LF"),
    EOT((byte) 0x04, "EOT");

    private byte number;
    private String name;

    CommonCommandASTM1381(byte number, String name){
            this.number = number;
            this.name = name;
    }

    public byte getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + "(" + number + ")";
    }

    private static final Map<Byte, CommonCommandASTM1381> commonCommands = new HashMap<>();

    static {
        commonCommands.put(NULL.getNumber(), NULL);
        commonCommands.put(ENQ.getNumber(), ENQ);
        commonCommands.put(ACK.getNumber(), ACK);
        commonCommands.put(NAK.getNumber(), NAK);
        commonCommands.put(STX.getNumber(), STX);
        commonCommands.put(ETB.getNumber(), ETB);
        commonCommands.put(ETX.getNumber(), ETX);
        commonCommands.put(CR.getNumber(), CR);
        commonCommands.put(LF.getNumber(), LF);
        commonCommands.put(EOT.getNumber(), EOT);

    }

    public static CommonCommandASTM1381 getCommonCommandByNumber(byte number) {
        CommonCommandASTM1381 commonCommand = commonCommands.get(number);
        if (commonCommand == null) {
            commonCommand = NULL;
        }
        return commonCommand;
    }
    
    /**
     * <p>
     * Convenient output of ASCII characters (Удобный вывод символов ASCII)
     * </p>
     *
     * @param str - source string without display ASCII characters (исходная строка без отображения символов ASCII)
     * @return string with display ASCII characters (строка с отображаемыми символами ASCII)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     */
    public static String displayCommandByte(String str){
        for (Entry<Byte, CommonCommandASTM1381> entry : commonCommands.entrySet()) {
            str = str.replaceAll(new String(new byte[] { entry.getValue().getNumber() }, StandardCharsets.UTF_8), "<" + entry.getValue().getName() + ">");
        }
        return str;
    }

}
