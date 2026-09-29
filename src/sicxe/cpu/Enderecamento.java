package sicxe.cpu;

import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;
import sicxe.maquina.Palavra;
import sicxe.maquina.Registradores;

// Modos de enderecamento do SIC/XE: descobre DE ONDE vem o operando.
//
// Toda instrucao de formato 3/4 diz "faca algo com m". A instrucao traz um
// numero pequeno (disp) e os 6 bits n i x b p e, que dizem como chegar em m:
//
//   Passo 1 - montar o endereco alvo (TA), usando e, p e b:
//     e = 1      -> TA = endereco de 20 bits (formato 4), sem conta nenhuma
//     p = 1      -> TA = PC da PROXIMA instrucao + disp (disp COM sinal)
//     b = 1      -> TA = registrador B + disp (disp SEM sinal)
//     b = p = 0  -> TA = disp (so alcanca 0..4095)
//
//   Passo 2 - se x = 1, soma o registrador X (e assim que se percorre vetor).
//
//   Passo 3 - decidir o que fazer com o TA, usando n e i:
//     n=1 i=1  DIRETO    o operando esta na memoria, no endereco TA
//     n=1 i=0  INDIRETO  no endereco TA esta guardado o endereco do operando
//     n=0 i=1  IMEDIATO  o proprio TA ja e o operando
//     n=0 i=0  modo SIC  instrucao do SIC antigo: endereco direto de 15 bits
public class Enderecamento {

    private final Memoria mem;
    private final Registradores regs;

    public Enderecamento(Memoria mem, Registradores regs) {
        this.mem = mem;
        this.regs = regs;
    }

    // n=0 i=1: o valor calculado ja e o operando (ex.: LDA #5).
    // O modo SIC (n=0 i=0) NAO e imediato: e direto.
    public boolean ehImediato(Instrucao ins) {
        return !ins.modoSIC && !ins.n && ins.i;
    }

    // n=1 i=0: o valor calculado e o endereco onde esta guardado o endereco final.
    public boolean ehIndireto(Instrucao ins) {
        return !ins.modoSIC && ins.n && !ins.i;
    }

    // Calcula o endereco alvo (TA). No modo imediato, o "alvo" e o proprio valor.
    // A indirecao acontece aqui dentro, no fim: quem chama ja recebe o endereco final.
    public int calcularAlvo(Instrucao ins) {
        validarCombinacao(ins);

        int alvo;
        if (ins.modoSIC) {
            alvo = ins.disp;                                // 15 bits, direto
        } else if (ins.e) {
            alvo = ins.disp;                                // formato 4: 20 bits, direto
        } else if (ins.p) {
            // O PC usado e o da PROXIMA instrucao (endereco + tamanho) e o disp tem
            // sinal, por isso um laco consegue saltar para tras.
            alvo = ins.endereco + ins.tamanho + Palavra.sinal12(ins.disp);
        } else if (ins.b) {
            alvo = regs.get(Registradores.B) + ins.disp;    // disp sem sinal
        } else {
            alvo = ins.disp;                                // direto, 12 bits
        }

        if (ins.x) {
            alvo += regs.get(Registradores.X);
        }
        alvo = Palavra.mascarar(alvo);

        if (ehIndireto(ins)) {
            alvo = mem.lerPalavra(alvo);
        }
        return alvo;
    }

    // Valor de 24 bits do operando (LDA, ADD, COMP, ...).
    public int valorPalavra(Instrucao ins) {
        int alvo = calcularAlvo(ins);
        if (ehImediato(ins)) {
            return alvo;
        }
        return mem.lerPalavra(alvo);
    }

    // Valor de 1 byte do operando (LDCH).
    public int valorByte(Instrucao ins) {
        int alvo = calcularAlvo(ins);
        if (ehImediato(ins)) {
            return alvo & 0xFF;
        }
        return mem.lerByte(alvo);
    }

    // Endereco onde escrever ou para onde saltar (STA, STCH, J, JSUB, ...).
    // Imediato nao faz sentido aqui: nao da para guardar algo "dentro" de uma constante.
    public int enderecoDestino(Instrucao ins) {
        if (ehImediato(ins)) {
            throw new ErroExecucao(ins.mnemonico + " nao aceita modo imediato (endereco "
                    + Palavra.hex(ins.endereco, 6) + ").");
        }
        return calcularAlvo(ins);
    }

    // Combinacoes de bits que o livro do Beck nao permite.
    private void validarCombinacao(Instrucao ins) {
        if (ins.modoSIC) {
            return;
        }
        if (ins.b && ins.p) {
            throw new ErroExecucao("Bits b e p ligados ao mesmo tempo no endereco "
                    + Palavra.hex(ins.endereco, 6) + ".");
        }
        if (ins.x && !(ins.n && ins.i)) {
            throw new ErroExecucao("Indexacao (x) so pode ser usada no modo direto (endereco "
                    + Palavra.hex(ins.endereco, 6) + ").");
        }
    }
}
