package sicxe.cpu;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TabelaOpcodes {

    public static final Map<Integer, String> FORMATO_2 = new LinkedHashMap<>();
    public static final Map<Integer, String> FORMATO_34 = new LinkedHashMap<>();
    public static final Map<String, Integer> POR_NOME = new LinkedHashMap<>();

    public static final int ADDR = 0x90, SUBR = 0x94, MULR = 0x98, DIVR = 0x9C;
    public static final int COMPR = 0xA0, SHIFTL = 0xA4, SHIFTR = 0xA8, RMO = 0xAC;
    public static final int CLEAR = 0xB4, TIXR = 0xB8;

    public static final int LDA = 0x00, LDX = 0x04, LDL = 0x08, STA = 0x0C;
    public static final int STX = 0x10, STL = 0x14, ADD = 0x18, SUB = 0x1C;
    public static final int MUL = 0x20, DIV = 0x24, COMP = 0x28, TIX = 0x2C;
    public static final int JEQ = 0x30, JGT = 0x34, JLT = 0x38, J = 0x3C;
    public static final int AND = 0x40, OR = 0x44, JSUB = 0x48, RSUB = 0x4C;
    public static final int LDCH = 0x50, STCH = 0x54, LDB = 0x68, LDS = 0x6C;
    public static final int LDT = 0x74, STB = 0x78, STS = 0x7C, STT = 0x84;

    static {
        f2(ADDR, "ADDR");  f2(SUBR, "SUBR");   f2(MULR, "MULR");   f2(DIVR, "DIVR");
        f2(COMPR, "COMPR"); f2(SHIFTL, "SHIFTL"); f2(SHIFTR, "SHIFTR"); f2(RMO, "RMO");
        f2(CLEAR, "CLEAR"); f2(TIXR, "TIXR");

        f34(LDA, "LDA");   f34(LDX, "LDX");   f34(LDL, "LDL");   f34(STA, "STA");
        f34(STX, "STX");   f34(STL, "STL");   f34(ADD, "ADD");   f34(SUB, "SUB");
        f34(MUL, "MUL");   f34(DIV, "DIV");   f34(COMP, "COMP");  f34(TIX, "TIX");
        f34(JEQ, "JEQ");   f34(JGT, "JGT");   f34(JLT, "JLT");   f34(J, "J");
        f34(AND, "AND");   f34(OR, "OR");     f34(JSUB, "JSUB");  f34(RSUB, "RSUB");
        f34(LDCH, "LDCH"); f34(STCH, "STCH"); f34(LDB, "LDB");   f34(LDS, "LDS");
        f34(LDT, "LDT");   f34(STB, "STB");   f34(STS, "STS");   f34(STT, "STT");
    }

    private TabelaOpcodes() { }

    private static void f2(int opcode, String nome) {
        FORMATO_2.put(opcode, nome);
        POR_NOME.put(nome, opcode);
    }

    private static void f34(int opcode, String nome) {
        FORMATO_34.put(opcode, nome);
        POR_NOME.put(nome, opcode);
    }

    public static boolean ehFormato2(int primeiroByte) {
        return FORMATO_2.containsKey(primeiroByte);
    }

    public static int total() {
        return FORMATO_2.size() + FORMATO_34.size();
    }
}