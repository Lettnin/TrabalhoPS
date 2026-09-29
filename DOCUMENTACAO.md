# Documentação — Simulador SIC/XE (Etapa 1)

Estruturas de dados, funções e estratégias do simulador. Cada integrante descreve a sua parte.

## 1. Estruturas de dados

_A completar: `Memoria`, `Registradores`, `Instrucao`, `TabelaOpcodes`, `Resultado`._

## 2. Funções desenvolvidas

### `Enderecamento` (pacote `sicxe.cpu`)

Descobre de onde vem o operando das instruções de formato 3 e 4, usando os bits `n i x b p e` e o deslocamento (`disp`) que o `Decodificador` já separou.

| Método | O que faz | Usado por |
|---|---|---|
| `calcularAlvo(ins)` | Calcula o endereço alvo (TA): modo SIC, formato 4, relativo ao PC, relativo à base ou direto. Soma X se a instrução for indexada e resolve a indireção. | os três métodos abaixo |
| `valorPalavra(ins)` | Devolve o operando de 24 bits. No modo imediato, é o próprio TA. | `LDA`, `ADD`, `COMP`, `TIX`, ... |
| `valorByte(ins)` | Devolve o operando de 1 byte. | `LDCH` |
| `enderecoDestino(ins)` | Devolve o endereço onde escrever ou para onde saltar. Recusa o modo imediato. | `STA`, `STCH`, `J`, `JSUB`, ... |
| `ehImediato(ins)`, `ehIndireto(ins)` | Identificam o modo pelos bits `n` e `i`. | uso interno |

## 3. Estratégias adotadas

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
