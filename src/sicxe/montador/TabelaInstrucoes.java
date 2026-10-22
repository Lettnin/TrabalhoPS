package sicxe.montador;

import sicxe.cpu.TabelaOpcodes;


public final class TabelaInstrucoes {

    private TabelaInstrucoes() { }

    public static boolean existe(String mnemonico) {
        return TabelaOpcodes.POR_NOME.containsKey(mnemonico);
    }

    public static int opcode(String mnemonico) {
        Integer opcode = TabelaOpcodes.POR_NOME.get(mnemonico);
        if (opcode == null) {
            throw new ErroMontagem("operacao desconhecida: " + mnemonico);
        }
        return opcode;
    }

    public static int formato(String mnemonico) {
        return TabelaOpcodes.ehFormato2(opcode(mnemonico)) ? 2 : 3;
    }

    public static boolean semOperando(String mnemonico) {
        return mnemonico.equals("RSUB");
    }
}