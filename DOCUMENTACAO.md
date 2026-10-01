# Documentação — Simulador SIC/XE

Estruturas de dados, funções e estratégias do simulador. Cada integrante descreve a sua parte.

## 1. Estruturas de dados

### Pacote `sicxe.maquina`

**Memoria** — vetor de `byte` com 32768 posições (32 KB). Cada posição é um
endereço e guarda 8 bits. Uma palavra ocupa 3 posições seguidas, com o byte
mais significativo primeiro (big-endian), como no livro do Beck. A classe
também guarda um `HashSet<Integer>` com os endereços escritos pela última
instrução, usado pela interface para destacar o que mudou.

**Registradores** — vetor de `int` com 10 posições, indexado pelo número do
registrador definido no enunciado:


| Registrador | A   | X   | L   | B   | S   | T   | F   | PC  | SW  |
| ----------- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Número      | 0   | 1   | 2   | 3   | 4   | 5   | 6   | 8   | 9   |


A posição 7 existe no vetor só para o índice bater com o número, mas não é
registrador: acessar o 7 gera erro. O código de condição fica nos 2 bits
menos significativos do SW: 0 para `=`, 1 para `<` e 2 para `>`.

**ErroExecucao** — exceção (`RuntimeException`) lançada para qualquer erro da
máquina: endereço fora da memória, registrador inexistente, etc.

_A completar: `Instrucao`, `TabelaOpcodes`, `Resultado`._

## 2. Funções desenvolvidas

### `Palavra`, `Memoria` e `Registradores` (pacote `sicxe.maquina`)

**Palavra** (métodos estáticos)


| Método              | O que faz                                                                         |
| ------------------- | --------------------------------------------------------------------------------- |
| `mascarar(v)`       | Descarta os bits acima de 24 (`v & 0xFFFFFF`).                                    |
| `paraSinalizado(v)` | Converte 24 bits em complemento de 2 para inteiro com sinal. Ex.: `FFFFF1` → −15. |
| `sinal12(d)`        | O mesmo para o deslocamento de 12 bits do formato 3.                              |
| `hex(v, n)`         | Formata em hexadecimal com `n` dígitos e zeros à esquerda.                        |


**Memoria**


| Método                                             | O que faz                                                        |
| -------------------------------------------------- | ---------------------------------------------------------------- |
| `lerByte` / `escreverByte`                         | Lê ou grava um byte (0 a 255). Valida o endereço.                |
| `lerPalavra` / `escreverPalavra`                   | Lê ou grava os 3 bytes de uma palavra a partir do endereço dado. |
| `getTamanho`, `limpar`                             | Tamanho da memória; zera tudo.                                   |
| `limparEscritasRecentes`, `foiEscritoRecentemente` | Controlam o destaque dos bytes alterados na interface.           |


**Registradores**


| Método                   | O que faz                                                                                                |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| `get` / `set`            | Lê ou grava um registrador pelo número. O `set` aplica a máscara de 24 bits.                             |
| `getPC` / `setPC`        | Atalhos para o PC.                                                                                       |
| `getCC` / `setCC`        | Lê ou grava o CC sem alterar os outros bits do SW.                                                       |
| `comparar(v1, v2)`       | Compara dois valores **com sinal** e grava o resultado no CC. Usado por `COMP`, `COMPR`, `TIX` e `TIXR`. |
| `getCCTexto`             | Devolve `=`, `<` ou `>` para exibição.                                                                   |
| `nome` / `numeroPorNome` | Converte entre número e nome do registrador (ex.: 4 ↔ `S`).                                              |

### `Enderecamento` (pacote `sicxe.cpu`)

Descobre de onde vem o operando das instruções de formato 3 e 4, usando os bits `n i x b p e` e o deslocamento (`disp`) que o `Decodificador` já separou.

| Método | O que faz | Usado por |
|---|---|---|
| `calcularAlvo(ins)` | Calcula o endereço alvo (TA): modo SIC, formato 4, relativo ao PC, relativo à base ou direto. Soma X se a instrução for indexada e resolve a indireção. | os três métodos abaixo |
| `valorPalavra(ins)` | Devolve o operando de 24 bits. No modo imediato, é o próprio TA. | `LDA`, `ADD`, `COMP`, `TIX`, ... |
| `valorByte(ins)` | Devolve o operando de 1 byte. | `LDCH` |
| `enderecoDestino(ins)` | Devolve o endereço onde escrever ou para onde saltar. Recusa o modo imediato. | `STA`, `STCH`, `J`, `JSUB`, ... |
| `ehImediato(ins)`, `ehIndireto(ins)` | Identificam o modo pelos bits `n` e `i`. | uso interno |

### `InstrucoesMemoria` (pacote `sicxe.cpu`)

Executa as 22 instruções de carga, armazenamento, aritmética, lógica, `COMP`
e `TIX`, delegando os modos de endereçamento a `Enderecamento`. O método
`executar(ins)` retorna `Resultado.CONTINUA` para essas instruções e
`Resultado.NAO_TRATADA` para os demais opcodes, permitindo que a CPU tente
`InstrucoesRegistrador`.

A aritmética usa complemento de 2 com `Palavra.paraSinalizado`, e
`Registradores.set` mantém os resultados em 24 bits. `LDCH` preserva os
16 bits superiores de A; `STCH` grava apenas seu byte inferior. `COMP` e
`TIX` usam `Registradores.comparar`, com o incremento de X antes da
comparação em `TIX`. Divisão por zero gera `ErroExecucao` com o endereço
da instrução.

## 3. Estratégias adotadas

### Memória e registradores

**Endereçamento por byte.** O enunciado diz que a unidade de endereçamento é a
palavra, mas o resto do próprio enunciado só funciona com endereço de byte:

- a tabela de instruções descreve uma palavra como `(m..m+2)`, ou seja, três
endereços seguidos para uma palavra;
- `LDCH` e `STCH` leem e gravam um único byte, `(m)`;
- existem instruções de 1, 2, 3 e 4 bytes, que não caberiam em endereços de
palavra;
- é assim no livro do Beck, que é a base do trabalho.

**Valores de 24 bits em `int`.** O Java não tem inteiro de 24 bits. Tudo é
guardado em `int` e passa pela máscara de 24 bits antes de ir para um
registrador ou para a memória. Números negativos seguem o complemento de 2, e
toda operação com sinal (soma, comparação) converte antes com `paraSinalizado`.

**Tamanho da memória.** O enunciado pede no mínimo 1 KB. Usamos 32 KB, o que
sobra para os programas de teste; a classe recusa tamanhos menores que 1 KB.

**Registrador F.** No enunciado o F tem 48 bits, mas todas as instruções de
ponto flutuante estão entre as que não devem ser implementadas. Por isso o F é
guardado como os outros, em 24 bits, e não aparece na interface.

**Erros sem travar o programa.** Todo erro da máquina é uma `ErroExecucao`. A
CPU captura, para a execução e mostra a mensagem, em vez de fechar com erro do
Java.

### Modos de endereçamento

- **Ordem do cálculo.** Primeiro `e`, `p` ou `b` definem a conta do endereço; depois `x` soma o registrador X; por fim `n` e `i` dizem se o resultado é o operando (imediato), o endereço do operando (direto) ou o endereço de um endereço (indireto).
- **Relativo ao PC.** Usa `endereço da instrução + tamanho`, que é o PC da próxima instrução, e lê o deslocamento com sinal (12 bits em complemento de 2), o que permite saltar para trás. No relativo à base, o deslocamento é sem sinal.
- **Indireção resolvida em um só lugar.** Ela acontece no fim de `calcularAlvo`, então quem chama sempre recebe o endereço final.
- **Três métodos de acesso em vez de um**, porque as instruções precisam de coisas diferentes: o valor (`LDA`), um único byte (`LDCH`) ou o endereço (`STA`, `J`).
- **Modo imediato em instrução que escreve ou salta** (`STA #5`, por exemplo) para a execução com mensagem, porque não existe endereço onde escrever.

## 4. Programas de teste e resultados esperados

Os programas ficam em `testes/`. Para rodar sem interface gráfica, passando os endereços de memória que se quer conferir:

```bash
java -cp bin sicxe.io.TesteConsole testes/<arquivo>.hex <enderecos em hexadecimal>
```

| Teste | O que exercita | Endereços a conferir | Resultado esperado | Instruções |
|---|---|---|---|---|
| `01-soma.hex` | imediato, direto, parada por `RSUB` | `0100` | A = `000008`; mem[`000100`] = `000008` | 4 |
| `02-laco.hex` | laço, indexação, formato 2, deslocamento negativo relativo ao PC | `002B` | A = X = `00000F`; mem[`00002B`] = `00000F` (1+2+3+4+5) | 26 |
| `03-enderecamento.hex` | formato 4, relativo à base, relativo ao PC, indireto, sub-rotina | `1027 102A 1033` | `00000E` (14), `000063` (99), `000007` | 13 |
| `04-bytes-e-sinal.hex` | deslocamento de bits, número negativo, comparação com sinal, byte, divisão | `0030 0036 0039` | `FFFFF1` (−15), byte `41` em `000036`, `00000E` (14) | 15 |

### Testes de erro (`testes/erros/`)

Em todos, o simulador deve parar com a mensagem e continuar aberto.

| Arquivo | Mensagem esperada |
|---|---|
| `opcode-invalido.hex` | `ERRO: Opcode desconhecido: FF no endereco 000000` |
| `divisao-por-zero.hex` | `ERRO: Divisao por zero no endereco 000000.` |
| `fora-da-memoria.hex` | `ERRO: Endereco fora da memoria: 008000` |
