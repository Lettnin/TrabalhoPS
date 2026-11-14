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

### Interface gráfica (pacote `sicxe.gui`)

Feita em Swing. Nenhuma outra classe depende dela: a interface só lê o estado
da `Memoria`, dos `Registradores` e da `CPU` e manda a CPU dar passos.

**JanelaPrincipal** — monta a tela: barra de botões no topo, memória à
esquerda, registradores e histórico à direita, e no rodapé a última instrução
executada e a barra de status.

| Método | O que faz |
|---|---|
| `JanelaPrincipal(mem, regs, cpu)` | Recebe as peças já criadas pelo `Main` e monta os painéis. |
| `carregarArquivo(arquivo)` | Usa o `Carregador` para pôr o programa na memória e prepara a CPU no endereço de execução. |
| `executarPasso()` | Um passo da CPU, usado pelo botão **Passo** e pelo `Timer` do **Rodar**. Registra a instrução no histórico. |
| `reset()` | Recarrega o último arquivo, voltando memória e registradores ao estado inicial. |
| `atualizarTela()` | Redesenha os painéis, o rodapé e habilita ou desabilita os botões conforme a CPU está parada. |

**PainelMemoria** — `JTable` com 16 bytes por linha, coluna de endereço e
coluna ASCII.

| Método | O que faz |
|---|---|
| `atualizar()` | Redesenha a tabela e rola até a linha do PC. |
| `irPara(endereco)` | Rola a tabela para deixar o endereço no meio da tela. |

**PainelRegistradores** — `JTable` com cada registrador em hexadecimal e em
decimal com sinal; no SW mostra também o CC (`=`, `<`, `>`).

| Método | O que faz |
|---|---|
| `guardarEstado()` | Tira um retrato dos valores atuais; o que mudar depois disso fica verde. |
| `atualizar()` | Redesenha a tabela. |

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

### Interface gráfica

- **Swing e não JavaFX.** O Swing já vem no Java; o JavaFX precisaria ser instalado e configurado à parte em cada máquina.
- **Execução animada com `javax.swing.Timer`, não com `while`.** O Swing desenha a tela na mesma thread que trata os cliques; um `while` congelaria a janela até o fim do programa. O `Timer` dá um passo a cada X ms (ajustável no controle deslizante) e a tela se redesenha entre um passo e outro.
- **A tabela lê direto da `Memoria`.** O modelo da `JTable` não copia os dados para um vetor à parte, então a tela nunca mostra informação velha.
- **Destaques.** Amarelo no byte apontado pelo PC; verde nos bytes escritos pela última instrução (`Memoria.foiEscritoRecentemente`) e nos registradores que ela alterou. Assim cada passo mostra exatamente o que mudou.
- **Rolagem automática até o PC**, para não perder a execução de vista nos 32 KB de memória. Só rola quando o PC sai da área visível, para a tabela não ficar pulando a cada passo.
- **Carga segura.** O arquivo é carregado antes numa memória de rascunho: se tiver erro, a mensagem aparece e a memória de verdade não fica com o programa pela metade.
- **Erros não fecham o simulador.** A mensagem da CPU aparece em vermelho na barra de status e no histórico, e os botões **Passo** e **Rodar** ficam desabilitados até carregar ou dar reset.

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

## 5. Etapa 2 — Tabela de símbolos e expressões (Pedro, parte D)

Esta parte segue os slides de Montagem do professor (tabela de símbolos e passos
do montador) e o capítulo 2 do Beck (seção 2.1, tabela de símbolos, e seção
2.3.3, expressões). A implementação parte de uma versão de referência do grupo,
montada a partir desse material; a única diferença funcional está em "Extensão
autorizada — multiplicação e divisão".

### Estruturas de dados

**TabelaSimbolos** guarda um `Map<String, Simbolo>` inicializado com
`LinkedHashMap<>`. A consulta é feita pelo nome, e a iteração preserva a
ordem de definição dos símbolos, usada pela listagem e pela interface.
Não há validação de nomes nem conversão de maiúsculas/minúsculas aqui:
essa preparação pertence ao analisador.

**TabelaSimbolos.Simbolo** é uma classe interna pública e estática. Seus
quatro campos são públicos e finais: `String nome`, `int valor`,
`boolean relativo` e `int linha` (linha da definição no fonte).

**Expressao.Valor** é uma classe interna pública e estática com os campos
públicos e finais `int valor` e `boolean relativo`. O valor absoluto
independe do endereço de carga; o relativo representa um endereço que
acompanha o programa na relocação. `Expressao` é uma classe final, com
construtor privado e avaliação por método estático.

### Funções desenvolvidas

| Classe | Método ou construtor | Responsabilidade |
|---|---|---|
| `TabelaSimbolos.Simbolo` | `Simbolo(String nome, int valor, boolean relativo, int linha)` | Guarda os quatro dados da definição. |
| `TabelaSimbolos` | `definir(String nome, int valor, boolean relativo, int linha)` | Insere um símbolo; rejeita duplicados sem substituir a definição anterior. |
| `TabelaSimbolos` | `contem(String nome)` | Consulta a existência do nome. |
| `TabelaSimbolos` | `buscar(String nome)` | Retorna o símbolo ou lança erro de símbolo indefinido. |
| `TabelaSimbolos` | `todos()` | Retorna uma nova `ArrayList<Simbolo>` na ordem de definição; alterar essa lista não altera a tabela. |
| `Expressao.Valor` | `Valor(int valor, boolean relativo)` | Guarda o resultado numérico e seu tipo. |
| `Expressao` | `avaliar(String texto, TabelaSimbolos simbolos, int lc)` | Avalia a expressão e retorna seu valor e tipo; lança `ErroMontagem` em caso de erro. |
| `Expressao` | `produto(...)` (privado) | Resolve multiplicações e divisões de cada trecho, da esquerda para a direita. |
| `Expressao` | `termo(...)` (privado) | Interpreta um número decimal, consulta um símbolo ou retorna o LC para `*`. |

### Estratégias adotadas

**D1 — Tabela de símbolos.** A passagem 1 define os rótulos; a passagem 2
consulta a tabela completa para resolver referências futuras de instruções.
Uma nova definição de um nome existente lança
`simbolo duplicado: NOME (ja definido na linha N)`, mantendo o símbolo
original. Uma consulta sem definição lança `simbolo indefinido: NOME`.
Esse segundo erro também pode ocorrer na passagem 1: `EQU`, reservas e
`ORG` precisam de valores naquele momento e só podem usar símbolos já
definidos. Não há resolução adiada nessas diretivas.

**D2 — Avaliação de expressões.** São aceitos números decimais, símbolos,
soma, subtração, sinal inicial opcional e `*` como LC recebido pelo
avaliador. Por exemplo, `FIMORIG EQU *` define o endereço logo depois do
texto no teste 5. Não são acrescentados parênteses, números hexadecimais,
frações, literais, tratamento de espaços ou normalização. `START` em
hexadecimal pertence à passagem 1; o analisador separa `#`, `@` e `,X`.

**D3 — Tipo absoluto ou relativo.** Cada termo relativo somado contribui
com +1, e cada termo relativo subtraído contribui com −1. Somente o saldo
final é validado: 0 indica absoluto; 1, relativo; outro valor lança
`expressao sem sentido: ... (so da para somar um endereco do programa)`.
Assim, `FIM+INICIO-INICIO` permanece válido mesmo com saldo intermediário
igual a 2. `FIMORIG-ORIGEM` é absoluto porque os dois endereços se deslocam
juntos quando o programa muda de lugar, mantendo a distância entre eles.
Com valores 45 e 30, essa distância é 15.

**Extensão autorizada — multiplicação e divisão.** Esta é a única diferença
funcional autorizada em relação à versão de referência do grupo. Os
contratos públicos e os comportamentos originais são preservados; o
avaliador não é idêntico ao da versão de referência. As operações `*` e `/` são resolvidas antes de
`+` e `-`, e operadores de mesma precedência são avaliados da esquerda
para a direita: `2+3*4 = 14`, `20/5*2 = 8` e `8/3*3 = 6`.
A divisão usa inteiros Java e trunca em direção a zero:
`15/2 = 7` e `-15/2 = -7`. O sinal opcional continua restrito ao início;
sequências como `2++3`, `2*/3` e `2*-3` são rejeitadas.

Multiplicação e divisão exigem dois operandos absolutos e produzem um
resultado absoluto. Isso mantém o modelo de tipo por saldo relativo,
sem introduzir regras adicionais de relocação. Um símbolo absoluto como
`TAM` pode participar de `TAM*2` ou `TAM/2`. Um símbolo relativo não pode,
nem em `TABELA*0` ou `TABELA/1`. As mensagens são
`multiplicacao com valor relativo na expressao: ...` e
`divisao com valor relativo na expressao: ...`.
Divisão de absolutos por zero lança
`divisao por zero na expressao: ...`, sem deixar escapar `ArithmeticException`.

O contexto distingue os dois usos de `*`: ao esperar um termo, ele é o LC
relativo; após um termo completo, é multiplicação. Portanto, `*+3` e
`FIM-*` usam o LC, `3*4` multiplica, e `*/2` é rejeitado por envolver um
valor relativo. `EQU *` mantém seu significado original.

**Integração e erros.** As classes da parte D lançam `ErroMontagem` sem
imprimir, alterar `Linha.erro` ou acrescentar `linha N:`. As passagens
capturam o erro e o associam à linha. O valor e o tipo são entregues aos
consumidores pela mesma API; a parte D não calcula bits `nixbpe` nem gera
registros M. Foram mantidas as mensagens originais de operando ausente,
expressão incompleta, número inválido e símbolo indefinido.

### Demonstração da parte D

No teste 2, mostrar a tabela na ordem de definição e explicar como
`STA SOMA` pode usar um rótulo definido mais adiante: a consulta ocorre
com a tabela pronta na passagem 2. No teste 5, mostrar
`FIMORIG EQU *`, `TAM EQU FIMORIG-ORIGEM` e o resultado absoluto 15.
Para a extensão, demonstrar `2+3*4`, `15/2` e a rejeição de um endereço
relativo em multiplicação ou divisão. Essas demonstrações dependem da
conclusão das demais peças do montador.

### Validação da parte D (21/10/2026)

Os testes foram criados e executados em diretório temporário fora do
repositório, sem dependências externas e sem alterar testes oficiais.
Foram aprovadas 5.131 verificações: contratos públicos, campos, ordem e
independência da lista de símbolos, erros de duplicação e de consulta,
casos originais e novos operadores. Dessas verificações, 5.000 comparam
valor, tipo ou mensagem de erro com o avaliador da versão de referência, usando
expressões de soma e subtração geradas com semente fixa.

A extensão foi verificada quanto à precedência, associatividade,
divisão inteira positiva e negativa, divisão por zero, uso de símbolos
absolutos, rejeição de relativos, distinção do LC e rejeição de sequências
incompletas ou inválidas. Também foram conferidos saldo relativo somente
ao final, ausência de normalização e mensagens originais.

A compilação integral dos 31 fontes atuais passou com JDK 25.0.2 e
`javac --release 17 -encoding UTF-8`, com os `.class` fora do repositório.
Isso verifica a compilação do estado existente, mas não certifica a
integração do montador: os testes 2, 5, os erros oficiais de símbolos e o
fonte temporário com `TAM EQU 2+3*4` não puderam ser executados pelo fluxo
real. Faltam os testes oficiais e implementações dos consumidores,
incluindo as passagens, o analisador, o codificador e a montagem; também
falta `MontadorConsole`. A validação de integração permanece pendente.

## 6. Etapa 2 — Diretivas, constantes e listagem (Samuel, parte A)

Esta parte segue o capítulo 2 do Beck e os slides de Montagem do professor. A
implementação parte de uma versão de referência do grupo, montada a partir
desse material; as diferenças em relação a ela estão indicadas nas estratégias
abaixo.

### Estruturas de dados

**ErroMontagem** é uma exceção (`RuntimeException`) lançada por quem detecta
um erro de montagem: símbolo indefinido, constante mal escrita, deslocamento
que não cabe. Quem lança não sabe em que linha está. As passagens capturam o
erro, guardam a mensagem em `Linha.erro` e seguem para a linha seguinte, de
modo que o montador mostra todos os erros de uma vez.

**Diretivas** guarda a tabela de pseudo-instruções (a POT dos slides):
`NOMES` é um `HashSet<String>` com as dez diretivas reconhecidas. Duas
constantes completam a classe: `TAMANHO_WORD` (3 bytes) e `MEMORIA_MAXIMA`
(`0x100000`, 1 MB). `Diretivas` é uma classe final, com construtor privado e
métodos estáticos.

| Diretiva | Para que serve |
|---|---|
| `START n` | endereço inicial do programa, em hexadecimal |
| `END r` | fim do fonte; `r` é onde a execução começa |
| `BYTE c` | constante de bytes: `C'ABC'` ou `X'F1'` |
| `WORD n` | constante de uma palavra (3 bytes), com sinal |
| `RESB n` / `RESW n` | reserva `n` bytes / `n` palavras |
| `EQU e` | dá ao rótulo o valor de uma expressão |
| `ORG e` | muda o contador de posição; sem operando, volta ao anterior |
| `BASE e` / `NOBASE` | liga e desliga o endereçamento relativo à base |

**Listagem** não guarda dados: monta o texto a partir da lista de `Linha` e da
`TabelaSimbolos` que recebe. É uma classe final, com construtor privado e um
método estático.

### Funções desenvolvidas

| Classe | Método ou construtor | Responsabilidade |
|---|---|---|
| `ErroMontagem` | `ErroMontagem(String mensagem)` | Cria o erro com a mensagem que vai para a linha do fonte e para a lista de erros. |
| `Diretivas` | `ehDiretiva(String operacao)` | Diz se a operação é uma das dez diretivas. Diferencia maiúsculas de minúsculas: a `Linha` guarda a operação em maiúsculas. |
| `Diretivas` | `tamanhoByte(String operando)` | Quantos bytes a constante de um `BYTE` ocupa; usado pela passagem 1. |
| `Diretivas` | `bytesDeByte(String operando)` | `C'EOF'` vira `45 4F 46` (código ASCII de cada caractere); `X'F1'` vira `F1` (um byte a cada dois dígitos). Lança `ErroMontagem` se a constante for inválida. |
| `Diretivas` | `bytesDeWord(int valor)` | Três bytes, o mais significativo primeiro; negativo vira complemento de 2 (`-15` vira `FF FF F1`). Aceita de −8388608 a 16777215. |
| `Diretivas` | `tamanhoReserva(String operacao, int quantidade)` | `RESB n` ocupa `n` bytes e `RESW n` ocupa `3n`. Recusa quantidade negativa e reserva acima de 1 MB. |
| `Listagem` | `gerar(List<Linha> linhas, TabelaSimbolos simbolos)` | Devolve o texto da listagem: o fonte com endereço e código objeto, os erros e a tabela de símbolos. |

### Estratégias adotadas

**A1 — Diretivas.** Uma diretiva não vira instrução de máquina: é uma ordem ao
montador. Na passagem 1 ela só mexe no contador de posição (soma o tamanho;
`START` e `ORG` trocam o valor); na passagem 2, só `BYTE` e `WORD` geram bytes.
O operando do `START` é hexadecimal e os demais números são decimais, como no
Beck: `START 1000` é o endereço 1000 em hexadecimal, e `RESB 2048` reserva
2048 bytes.

**A2 — Constantes `BYTE`.** `C'...'` vira o código ASCII de cada caractere, e só
são aceitos caracteres ASCII, sem acento. `X'...'` vira um byte a cada dois
dígitos hexadecimais; número ímpar de dígitos ou dígito inválido é erro. As
mensagens são `constante invalida no BYTE: use C'texto' ou X'hexadecimal'`,
`use so caracteres sem acento em C'...'`,
`X'...' precisa de um numero par de digitos hexadecimais` e
`digito hexadecimal invalido no BYTE: ...`. `bytesDeByte` espera o tipo (`C` ou
`X`) em maiúscula; aceitar o tipo em minúscula é responsabilidade do analisador
de linha.

**A3 — `WORD` e reservas.** `bytesDeWord` reaproveita a `Palavra.mascarar` da
Etapa 1, que descarta os bits acima de 24 e produz o complemento de 2: `WORD -1`
vira `FF FF FF`. Valores fora de −8388608 a 16777215 não cabem em 24 bits e
lançam `valor nao cabe em 24 bits: ...`. `RESB` e `RESW` não geram bytes no
programa objeto: o contador de posição pula, e um novo registro T começa depois
da reserva.

**Diferença em relação à versão de referência do grupo — limite nas reservas.**
Esta é a única diferença funcional da parte A em relação à versão de referência.
`tamanhoReserva` faz a conta de `RESW n` em `long` e recusa reservas acima de
1 MB (`0x100000`), a memória máxima do SIC/XE (Beck, seção 1.3.2). Na versão de
referência, `3 × n` em `int` estoura para `n` grande: `RESW 715827883` dava
tamanho negativo, e o montador gerava um programa objeto errado sem avisar. A
interface pública não mudou. A mensagem nova é
`reserva grande demais no RESW: ... (o SIC/XE enderece no maximo 1 MB)`.

**A4 — Listagem.** É a "linguagem de máquina anotada" dos slides: uma linha de
texto para cada linha do fonte, com o número, o endereço (seis dígitos
hexadecimais, em branco quando a linha não tem endereço, como comentário e linha
vazia), o código objeto e o texto original. No `EQU`, a coluna de endereço mostra
o valor do símbolo, como no Beck, e não o endereço da linha. O erro aparece logo
abaixo da linha que o causou, com `*** ERRO:`. No fim vem a tabela de símbolos,
com nome, valor e tipo (relativo ou absoluto), na ordem de definição.

**Fora do escopo desta entrega.** A tabela de referências cruzadas citada no
slide de listagem e os literais (`=C'EOF'`, `LTORG`) não foram implementados: a
listagem traz só a tabela de símbolos, e as constantes são definidas com `BYTE`
ou `WORD` e um rótulo, o que gera o mesmo código objeto (Beck, p. 69). Se os
literais forem acrescentados, a `Diretivas` ganharia o `LTORG`, e a tradução da
constante reaproveitaria o `bytesDeByte`.

### Demonstração da parte A

No teste 5, mostrar como as constantes viram bytes: `C'SIC/XE NA UFPEL'` vira os
15 códigos ASCII, `X'F0F0F0'` vira os próprios bytes e `WORD -1` vira `FFFFFF`.
Mostrar também `DESTINO RESB TAM` e a listagem, em que o `EQU` exibe o valor do
símbolo (`TAM` vale 15). Para os erros, mostrar `X'F1F'` (número ímpar de
dígitos) e um `RESW` grande demais. Essas demonstrações dependem da conclusão
das demais peças do montador.

### Validação da parte A (21/10/2026)

Os testes foram criados e executados em diretório temporário fora do
repositório, sem alterar testes oficiais. Foram aprovadas 58 verificações
diretas: as dez diretivas; `bytesDeByte` com constantes em caracteres e em
hexadecimal e com os erros de dígitos ímpares, dígito inválido, acento,
constante vazia ou sem aspas e tipo desconhecido; `bytesDeWord` nos limites de
24 bits (`7FFFFF`, `800000`, `FFFFFF`, `FFFFF1` para −15, erro em 16777216 e em
−8388609); `tamanhoReserva` nos limites de 1 MB e no estouro de `int`; e a
`Listagem` com linhas montadas à mão (comentário sem endereço, `EQU` com o valor
do símbolo, erro abaixo da linha e tabela de símbolos).

Com o restante do código de referência do grupo, que ainda não está no
repositório, a `Diretivas` com o limite de 1 MB gerou programas objeto idênticos
aos de referência nos cinco testes oficiais, e a `Listagem` reproduziu as cinco
listagens de referência. Em 46 programas de borda, o resultado foi o mesmo da
`Diretivas` de referência em todos, exceto o `RESW` gigante, que agora dá erro.

A compilação integral dos 31 fontes atuais passou com
`javac --release 17 -encoding UTF-8` (JDK 25.0.4.1), com os `.class` fora do
repositório. Isso verifica a compilação do estado existente, mas não certifica a
integração do montador, porque as passagens, o analisador, o codificador e a
montagem ainda são esqueleto no repositório. A validação de integração
permanece pendente.

## 7. Etapa 2 — Codificador e programas de teste (Eduardo, parte C)

O `Codificador` faz o caminho inverso do `Enderecamento` da Etapa 1: lá, os bits `n i x b p e` viravam endereço; aqui, o endereço vira bits. A passagem 2 chama o `Codificador` para cada instrução, com o opcode já consultado na tabela de instruções e o valor do operando já calculado pela `Expressao`. O código é o da versão de referência do grupo, sem mudanças.

### Estruturas de dados

O `Codificador` não guarda dados: é uma classe final, com construtor privado e métodos estáticos. Ele lê os campos da `Linha` preenchidos pelo analisador (`operando`, `imediato`, `indireto`, `indexado`, `formato4`) e pela passagem 1 (`endereco`), devolve os bytes da instrução e, no formato 4 com rótulo, preenche o pedido de registro M (`enderecoModificacao` e `meiosBytesModificacao`).

Os bytes seguem os formatos do Beck:

| Formato | Campos (bits) | Bytes |
|---|---|---|
| 2 | opcode (8) · r1 (4) · r2 (4) | 2 |
| 3 | opcode (6) · n i x b p e · disp (12) | 3 |
| 4 | opcode (6) · n i x b p e · endereço (20) | 4 |

### Funções desenvolvidas

| Método | O que faz |
|---|---|
| `formato2(linha, opcode)` | Separa o operando na vírgula, converte cada registrador com `Registradores.numeroPorNome` e junta o opcode com `r1` e `r2`. `CLEAR` e `TIXR` têm um registrador só; `SHIFTL` e `SHIFTR` gravam n − 1. Ex.: `ADDR S,X` → `90 41`, `CLEAR L` → `B4 20`, `SHIFTL A,4` → `A4 03`. |
| `formato34(linha, opcode, alvo, base)` | Escolhe os bits `n i x b p e`, calcula o campo de 12 ou 20 bits e devolve 3 ou 4 bytes. `alvo` é o valor e o tipo do operando (`null` no `RSUB`); `base` é o valor do `BASE` em vigor (`null` se não houver). |
| `exigir(...)` (privado) | Confere a quantidade de operandos do formato 2. |
| `registrador(...)` (privado) | Converte o nome do registrador e troca o erro da Etapa 1 por `registrador desconhecido: ...`. |
| `numero(...)` (privado) | Lê o número de bits do `SHIFTL` e do `SHIFTR`. |

### Estratégias adotadas

- **n, i, x e e saem do texto.** Sem sinal, n i = 1 1; com `#`, 0 1; com `@`, 1 0. O `,X` liga o x e o `+` liga o e. O montador sempre gera instruções SIC/XE (n e i nunca são 0 0), e `,X` junto com `#` ou `@` é erro.
- **b e p são a decisão de verdade** (Beck, seção 2.2.1), nesta ordem:
  1. formato 4: o valor inteiro vai nos 20 bits, com b = p = 0;
  2. operando absoluto (um número, ou um símbolo de `EQU` com número): vai direto no disp, com b = p = 0, de 0 a 4095;
  3. operando relativo (um rótulo do programa): primeiro o relativo ao PC, `disp = alvo − (endereço + 3)`, de −2048 a 2047, com o negativo em complemento de 2 em 12 bits; senão, o relativo à base, `disp = alvo − BASE`, de 0 a 4095; senão, erro pedindo o formato 4.
- **O PC vem antes da base** porque só depende do próprio programa. A base depende de o programa ter carregado em B o valor prometido no `BASE`. Além disso, o relativo ao PC não precisa de registro M: a distância entre a instrução e o rótulo não muda se o programa mudar de lugar.
- **O +3** é o tamanho da instrução de formato 3: quando ela executa, o PC já aponta para a próxima. É a mesma regra do `Enderecamento` da Etapa 1 (`ins.endereco + ins.tamanho`).
- **Registro M só no formato 4 com rótulo.** `+LDB #TABELA` pede um M (no `endereço + 1`, com 5 meios-bytes); `+LDT #4096` não pede, porque 4096 é só um número.
- **Juntar os bits.** Os pedaços são unidos com `<<` e `|`, do mesmo jeito que o `Decodificador` da Etapa 1 os separava com `>>` e `&`.

### Programas de teste e resultados esperados

Os programas ficam em `testes/montador/`. Os quatro primeiros são os da Etapa 1, agora em assembly; o quinto usa as diretivas e expressões que os outros não usam. Os `.lst` e `.obj` ao lado de cada fonte foram gerados pelo montador: se uma mudança no montador alterar algum deles, o `git diff` mostra.

```bash
java -cp bin sicxe.montador.MontadorConsole testes/montador/<arquivo>.asm       # monta
java -cp bin sicxe.io.TesteConsole testes/montador/<arquivo>.obj <enderecos>    # roda
```

| Teste | O que exercita | Endereços a conferir | Resultado esperado | Instruções |
|---|---|---|---|---|
| `01-soma.asm` | imediato e endereço fixo; os mesmos 12 bytes do `01-soma.hex` | `0100` | mem[`000100`] = `000008` | 4 |
| `02-laco.asm` | referência futura, indexado, formato 2, salto para trás (`JLT LOOP` → `3B2FF6`) | `002B` | A = X = `00000F`; mem[`00002B`] = `00000F` (15) | 26 |
| `03-enderecamento.asm` | `BASE`, formato 4, indireto, sub-rotina, registros M | `1027 102A 1833` | B = `001833`; `00000E` (14), `000063` (99), `000007` | 13 |
| `04-bytes-e-sinal.asm` | `SHIFTL` e `SHIFTR` (gravam n − 1), negativo, `BYTE C'ABC'`, divisão | `002D 0033 0036` | CC = `<`; `FFFFF1` (−15), `410000` (o 41 é o `A`), `00000E` (14) | 15 |
| `05-copia-texto.asm` | `EQU *`, `EQU` com expressão, `RESB TAM`, `BYTE X'...'`, `WORD -1`, `TIX`, formato 4 com número | `003C` | X = `00000F`; T = `001000`; mem[`00003C`] = `00000F`; de `002D` a `003B`, o texto `SIC/XE NA UFPEL` | 66 |

Os bytes do montador nem sempre são os dos `.hex` escritos à mão. No teste 2, `ADD VETOR,X` virou `1B A0 0D` (relativo ao PC) em vez de `1B 80 1C` (direto): os dois chegam ao mesmo endereço e dão o mesmo resultado, e o montador prefere o relativo ao PC. No teste 3, a TABELA foi afastada com `ESPACO RESB 2048` para obrigar o uso da base. No teste 4, os dados começam em `002D` em vez de `0030`, porque o fonte não tem o buraco de 3 bytes do `.hex`.

### Testes de erro (`testes/montador/erros/`)

Nenhum deles gera `.obj`. A primeira linha de cada arquivo traz a mensagem esperada.

| Arquivo | Pego na | Mensagem esperada |
|---|---|---|
| `simbolo-indefinido.asm` | passagem 2 | `linha 4: simbolo indefinido: ROTINA` |
| `simbolo-duplicado.asm` | passagem 1 | `linha 6: simbolo duplicado: VALOR (ja definido na linha 5)` |
| `operacao-invalida.asm` | passagem 1 | `linha 3: operacao desconhecida: LDZ` |
| `fora-do-alcance.asm` | passagem 2 | `linha 4: o endereco 000BBE esta longe demais para o PC (e nao ha BASE); use o formato 4 (+)` |

No `fora-do-alcance.asm`, trocar `LDA LONGE` por `+LDA LONGE` faz a montagem passar, porque o formato 4 alcança a memória toda.

### Validação da parte C (22/10/2026)

O analisador, a passagem 1, a tabela de instruções e o `MontadorConsole` ainda não estão no repositório. Por isso, a validação usou uma cópia fora do repositório: o código de referência do grupo por baixo e, por cima, todos os arquivos já enviados, inclusive o limite das reservas (parte A) e a multiplicação e divisão (parte D). Os cinco programas objeto e as cinco listagens saíram idênticos aos de referência; no `TesteConsole`, os cinco programas deram os valores da tabela acima; e os quatro testes de erro deram as mensagens esperadas, sem gerar `.obj`. A validação pelo fluxo real fica para quando as peças que faltam forem enviadas.

## 8. Etapa 2 — Janela do montador (Arthur Gausmann, parte G)

A `JanelaMontador` é a parte que o professor vê: abre um `.asm`, monta, mostra
o resultado e manda o programa para o simulador da Etapa 1. O código é o da
versão de referência do grupo, sem mudanças.

### Estruturas de dados

A janela não tem estrutura própria: só guarda o que precisa para a tela.

| Campo | Tipo | Para que serve |
|---|---|---|
| `simulador` | `JanelaPrincipal` | a janela da Etapa 1, que recebe o programa montado |
| `montador` | `Montador` | o montador da parte F, chamado a cada clique em **Montar** |
| `editor`, `numerosDasLinhas` | `JTextArea` | o fonte, editável, e a coluna com o número de cada linha |
| `listagem`, `programaObjeto`, `mensagens` | `JTextArea` | só leitura: o resultado da montagem e os erros ou o resumo |
| `modeloSimbolos` | `DefaultTableModel` | as linhas da tabela de símbolos: nome, valor, tipo e linha |
| `arquivoFonte` | `File` | o `.asm` aberto ou salvo; `null` enquanto não há nenhum |
| `resultado` | `Montador.Resultado` | a última montagem, usada pelo **Carregar no simulador** |

Na `JanelaPrincipal`, o único campo novo é `janelaMontador`, criado no primeiro
clique em **Montador**.

### Funções desenvolvidas

**JanelaMontador**

| Método | O que faz |
|---|---|
| `JanelaMontador(simulador)` | Recebe a janela do simulador e monta a tela: botões em cima, fonte à esquerda, abas Listagem / Tabela de símbolos / Programa objeto à direita e mensagens embaixo. |
| `abrir()` | Escolhe um `.asm` (começa em `testes/montador`) e põe o texto no editor com `Montador.lerLinhas`. |
| `salvar()` | Grava o editor no arquivo aberto com `Montador.gravar`; se nenhum foi aberto, pergunta onde salvar. |
| `montar()` | Monta o texto do editor, mesmo sem salvar, e preenche as três abas. Com erro, mostra a lista em vermelho; sem erro, o resumo em verde (nome, início, tamanho e endereço de execução). |
| `carregarNoSimulador()` | Grava o `.obj` na pasta temporária do sistema e chama `simulador.carregarArquivo`. |
| `fonteMudou()` | Chamado a cada mudança no editor: refaz os números das linhas e desabilita o **Carregar no simulador**. |
| `mostrarMensagem(texto, cor)` | Escreve na área de mensagens com a cor dada. |

**JanelaPrincipal** (trecho novo)

| Método | O que faz |
|---|---|
| `abrirMontador()` | Ação do botão **Montador**: cria a `JanelaMontador` no primeiro clique e nos seguintes só a mostra de novo. |

### Estratégias adotadas

- **A ponte entre as etapas é o `.obj`.** A janela não mexe na memória nem na
  CPU: grava o programa objeto e chama o mesmo `carregarArquivo` do botão
  **Carregar programa**. O simulador não sabe que o programa veio do montador,
  e o montador continua sem saber que o simulador existe. Com isso, a carga
  segura da Etapa 1 (memória de rascunho primeiro) vale também aqui, e o
  **Reset** recarrega o programa montado.
- **O `.obj` vai para a pasta temporária**, e não para o lado do `.asm`, para
  não sobrescrever os `.obj` e `.lst` de `testes/montador/` quando alguém
  edita o fonte na tela. Os arquivos oficiais são gerados pelo terminal.
- **Monta o que está no editor, não o arquivo.** Dá para criar um erro e
  corrigir na hora, sem salvar, que é o que o vídeo mostra.
- **"Carregar no simulador" só depois de montar sem erros.** O botão começa
  desabilitado, liga quando a montagem não tem erros e desliga de novo a cada
  mudança no fonte, porque a montagem anterior deixa de valer. Assim nunca vai
  para o simulador um programa que não corresponde ao texto da tela.
- **Números de linha numa segunda `JTextArea`**, posta como `setRowHeaderView`
  da rolagem do editor: rola junto com o fonte, e os erros dizem "linha N", então
  é ali que se acha a linha.
- **Uma janela só, escondida ao fechar** (`HIDE_ON_CLOSE`). Fechar o montador
  não fecha o simulador, e reabrir pelo botão volta com o fonte e o resultado
  como estavam.
- **Abas em vez de uma tela única.** A listagem e o programa objeto são textos
  longos; em abas, cada um ocupa a área toda, e a aba da listagem é selecionada
  quando há erro, porque é nela que aparece o `*** ERRO:` abaixo da linha.

### Demonstração da parte G

No começo do vídeo, clicar em **Montador** na janela do simulador e apresentar a
tela. No teste 5, abrir `05-copia-texto.asm`, montar, mostrar as três abas e
clicar em **Carregar no simulador**: a janela da Etapa 1 vem para a frente com o
programa na memória, e o **Rodar** termina com `00000F` em `00003C`. Para os
erros, trocar uma operação por uma inexistente, montar (mensagem em vermelho,
Carregar desabilitado) e corrigir na hora.

### Validação da parte G (22/10/2026)

Compilação de todos os fontes com `javac -encoding UTF-8`. Teste da janela
fora do repositório, criando a `JanelaPrincipal` e a `JanelaMontador` e
chamando os métodos dos botões:

- o botão **Montador** cria a janela uma vez e reaproveita a mesma no segundo
  clique; fechar só esconde;
- com o `05-copia-texto.asm` no editor, a montagem dá
  `Montado sem erros: programa COPIA, comeca em 000000, tamanho 000045 (69 bytes), execucao em 000000.`;
  a aba da listagem e a do programa objeto ficam iguais ao `.lst` e ao `.obj`
  de `testes/montador/`, e a tabela de símbolos tem os 9 símbolos;
- **Carregar no simulador** começa desabilitado, liga depois da montagem e,
  depois da carga, o simulador roda as 66 instruções e termina com
  mem[`00003C`] = `00000F` e X = `00000F`;
- editar o fonte desabilita o **Carregar**; trocar `LDX` por `LDZ` dá
  `linha 6: operacao desconhecida: LDZ` em vermelho, com o Carregar desabilitado;
- nenhum arquivo de `testes/` foi alterado.

## 9. Etapa 3 — A macro e a tabela de macros (Pedro, parte D)

Esta parte segue os slides "03 - Macros" do professor (tabela de nomes de
macro, tabela de definição de macros, pré-tratamento dos parâmetros formais e
o parâmetro `SER`) e a seção 4.1 do Beck (tabela de definição com `?1`, `?2`).
São duas classes do pacote `sicxe.macro`: `Macro`, que guarda uma definição, e
`TabelaMacros`, que guarda todas as definições pelo nome.

### Estruturas de dados

**Macro** guarda uma definição em cinco campos públicos e finais:

| Campo | Tipo | Conteúdo |
|---|---|---|
| `nome` | `String` | o nome da macro, que vem no rótulo do protótipo |
| `linhaDefinicao` | `int` | a linha do fonte onde a definição foi lida |
| `formais` | `List<String>` | os parâmetros formais em maiúsculas, como escritos: `&A`, `&B` |
| `padroes` | `List<String>` | o valor padrão de cada formal, na mesma posição; `""` quando não há |
| `esqueleto` | `List<String>` | as linhas do corpo, já com `?n` no lugar de cada formal |

As listas são `ArrayList` criadas junto com o objeto. Elas são finais (a
referência não muda), mas o conteúdo é preenchido depois: os formais e os
padrões pela leitura do protótipo, e o esqueleto linha a linha durante a
definição. A posição de um formal na lista mais 1 é o número que ele recebe no
esqueleto: o primeiro formal vira `?1`, o segundo `?2`. O `?0` fica reservado
para o número de série da expansão (`.SER`).

Exemplo, para a macro do teste 1:

```
Fonte                              Guardado
SOMA     MACRO   &A,&B,&DEST       nome = SOMA, formais = [&A, &B, &DEST], padroes = ["", "", ""]
         LDA     &A                esqueleto[0] =          LDA     ?1
         ADD     &B                esqueleto[1] =          ADD     ?2
         STA     &DEST             esqueleto[2] =          STA     ?3
         MEND
```

**TabelaMacros** guarda um `Map<String, Macro>` inicializado com
`LinkedHashMap<>`, com o nome da macro como chave. A busca pelo nome é direta,
e a iteração preserva a ordem em que cada nome foi definido pela primeira vez,
usada no texto da tabela. Um mapa só faz o papel das duas tabelas dos slides: a
tabela de nomes (para reconhecer uma chamada pela operação da linha) e a tabela
de definição (o nome leva à `Macro`, que tem o esqueleto).

### Funções desenvolvidas

| Classe | Método ou construtor | Responsabilidade |
|---|---|---|
| `Macro` | `Macro(String nome, int linhaDefinicao)` | Cria a definição com o nome e a linha, e as três listas vazias. |
| `Macro` | `adicionarFormal(String nome, String padrao)` | Acrescenta um formal e o seu padrão; recusa nome repetido com `ErroMacro("parametro formal repetido: &A")`, sem alterar as listas. |
| `Macro` | `indiceFormal(String nome)` | Devolve a posição do formal, a partir de 1, ou -1 se ele não existe. Aceita o nome com ou sem `&` e em qualquer caixa. |
| `Macro` | `formaisComSerie()` | Devolve uma nova lista com `.SER` na posição 0 seguido dos formais: `[.SER, &A, &B, &DEST]`. |
| `Macro` | `prototipo()` | Monta o protótipo em texto, com os padrões: `INCR &VAR,&PASSO=1,&OP=ADD`. |
| `TabelaMacros` | `definir(Macro macro)` | Guarda a macro pelo nome; se o nome já existe, a nova definição substitui a antiga. |
| `TabelaMacros` | `existe(String nome)` | Diz se há uma macro com esse nome, sem diferenciar maiúsculas de minúsculas; `null` dá `false`. |
| `TabelaMacros` | `buscar(String nome)` | Devolve a macro com esse nome, ou `null` se não existe. |
| `TabelaMacros` | `todas()` | Devolve as macros na ordem de definição. |
| `TabelaMacros` | `tamanho()` | Devolve quantas macros estão definidas. |
| `TabelaMacros` | `comoTexto()` | Monta a tabela inteira em texto, para a aba **Tabela de macros** e para o terminal. |

### Estratégias adotadas

**D1 — A macro.** A `Macro` guarda só dados e algumas consultas sobre eles;
quem lê o protótipo e o corpo e quem troca os parâmetros são outras classes do
pacote. Três pontos do contrato:

- **Formal repetido é erro.** Em `M MACRO &A,&A`, os dois `&A` teriam a mesma
  posição de procura, e o segundo nunca seria usado. O `adicionarFormal`
  consulta o `indiceFormal` antes de acrescentar e lança `ErroMacro` sem mexer
  nas listas.
- **O `indiceFormal` aceita o nome sem `&`.** Na chamada por palavra-chave, o
  nome vai sem o `&` (`PASSO=5` para o formal `&PASSO`), como no Beck e nos
  slides. A comparação é feita com o nome em maiúsculas, com e sem o `&` na
  frente, então `&PASSO`, `PASSO` e `passo` dão a mesma posição (2, na macro
  `INCR` do teste 5).
- **O `.SER` é o formal número 0.** O `formaisComSerie` devolve a lista com
  `.SER` na frente. Com isso, a posição de cada nome nessa lista já é o número
  do `?n` do esqueleto: `.SER` → `?0`, `&A` → `?1`. É o "parâmetro formal
  definido pelo sistema" dos slides, tratado como um parâmetro qualquer. A lista
  é nova a cada chamada, então mudar a lista devolvida não muda a macro.

O esqueleto usa `?1`, `?2`, como o Beck, e não `#1`, `#2`, como os slides,
porque no SIC/XE o `#` já indica endereçamento imediato: `LDA #&A` fica
`LDA #?1`, sem confusão com `LDA #1`.

**D2 — Redefinição.** Na tabela de símbolos da Etapa 2, definir um nome que já
existe é erro (símbolo duplicado). Na tabela de macros não é: o `definir` só
faz `put` no mapa, e a definição nova substitui a antiga. É isso que a
definição aninhada precisa, porque cada expansão da macro de fora define a de
dentro de novo. No teste 3, `ESCALA 2` define `MULESC` com `SHIFTR A,2`, e
`ESCALA 3` define `MULESC` outra vez com `SHIFTR A,3`. A chamada seguinte usa a
última definição.

Na redefinição, o `LinkedHashMap` troca o valor e mantém a chave na posição
original: se `SOMA` foi definida antes de `INCR` e depois redefinida, ela
continua antes de `INCR` em `todas()` e no `comoTexto()`.

**Maiúsculas e `null`.** O `existe` e o `buscar` convertem o nome para
maiúsculas antes de consultar, então `soma` e `Soma` acham a macro `SOMA`. O
nome guardado já vem em maiúsculas da leitura do protótipo. O `existe` aceita
`null` e devolve `false`, porque o processador consulta a operação de toda
linha, e uma linha vazia ou só de comentário não tem operação.

**D3 — A tabela em texto.** O `comoTexto` mostra, para cada macro, o
protótipo, a linha onde ela foi definida e o esqueleto guardado, com 4 espaços
na frente de cada linha e uma linha em branco entre as macros. Sem macros, o
texto é `Nenhuma macro definida.`. Como o esqueleto aparece com os `?n`, a
tabela mostra o pré-tratamento dos parâmetros feito na definição. Para o teste 1:

```
SOMA &A,&B,&DEST      (definida na linha 6)
             LDA     ?1
             ADD     ?2
             STA     ?3
```

A linha mostrada é a da definição que está valendo. Por isso, no teste 3,
`MULESC` e `DIVESC` aparecem como definidas na linha 26: a última definição
delas veio da expansão de `ESCALA 3`, que está nessa linha do fonte.

**Integração e erros.** As duas classes não imprimem nada nem sabem em que
linha está a leitura: o `adicionarFormal` lança `ErroMacro` só com a mensagem,
e o processador de macros junta o número da linha. A `Macro` também não
interpreta o esqueleto; ela só guarda as linhas que recebe, na ordem.

### Demonstração da parte D

No teste 1, abrir a aba **Tabela de macros** e mostrar a macro `SOMA` com o
protótipo, a linha da definição e o esqueleto com `?1`, `?2` e `?3`. No teste
3, mostrar que o esqueleto de `ESCALA` guarda `&A`, `&B` e `&C` ainda pelo nome
e `&N` já como `?1`, e que `MULESC` e `DIVESC` aparecem com `3` e "definida na
linha 26", o resultado da redefinição feita pelo `ESCALA 3`. No
teste 5, mostrar o protótipo `INCR &VAR,&PASSO=1,&OP=ADD` com os padrões.

### Validação da parte D (13/11/2026)

Os testes foram criados e executados em diretório temporário fora do
repositório, sem dependências externas, compilados com JDK 25.0.2 e
`javac --release 17 -encoding UTF-8`. Foram aprovadas 27 verificações:

- **Macro (15):** protótipo com e sem padrões; `indiceFormal` com `&`, sem `&`,
  em minúsculas e com nome inexistente (-1); `formaisComSerie` com e sem
  formais; conteúdo de `formais` e `padroes`; formal repetido lançando
  `parametro formal repetido: &VAR` sem alterar as listas; `nome` e
  `linhaDefinicao`.
- **TabelaMacros (10):** tabela vazia com tamanho 0; `existe(null)` dando
  `false`; busca sem diferenciar maiúsculas, devolvendo o mesmo objeto; nome
  inexistente dando `false` e `null`; redefinição sem erro, com o tamanho
  mantido, a definição nova no lugar da antiga e a ordem original em `todas()`.
- **comoTexto (2):** o texto da tabela vazia e o texto exato da macro `SOMA`,
  com os 6 espaços antes de `(definida`, os 4 espaços antes de cada linha do
  esqueleto e a linha em branco no fim.

## 10. Etapa 3 — O algoritmo de uma passagem (João, parte E)

Esta parte segue o processador de macros de uma passagem dos slides "03 -
Macros" do professor (os três modos do diagrama de estados e o contador de
nível de definição) e a seção 4.1 do Beck. É uma classe só do pacote
`sicxe.macro`, a `ProcessadorMacros`. Ela lê o fonte, guarda as definições na
`TabelaMacros` (Pedro) e devolve o fonte expandido, que vira o `MASMAPRG.ASM`.
As peças de baixo são usadas pelo contrato da Seção 3.2 do guia: a
`LinhaMacro` (Victor) quebra a linha, o `Argumentos` (Samuel) lê os formais e
associa os parâmetros reais e a `Substituicao` (Eduardo) faz as duas trocas de
texto.

### Estruturas de dados

**Resultado** (classe interna pública) é o que o processador devolve:

| Campo | Tipo | Conteúdo |
|---|---|---|
| `expandido` | `List<String>` | o fonte expandido, linha por linha (o `MASMAPRG.ASM`) |
| `macros` | `TabelaMacros` | as macros que estavam definidas quando o fonte acabou |
| `erros` | `List<String>` | as mensagens de erro, já com o número da linha |
| `expansoes` | `int` | quantas chamadas foram expandidas; é também o contador do `.SER` |

O método `temErros()` diz se a lista de erros não está vazia. O
`MacroMontador` (Arthur Trettin) só grava o `MASMAPRG.ASM` e chama o montador
quando ela está vazia.

**Expansao** (classe interna privada) é uma chamada em andamento:

| Campo | Tipo | Conteúdo |
|---|---|---|
| `macro` | `Macro` | a macro que está sendo expandida |
| `valores` | `String[]` | os parâmetros reais: `valores[0]` é o número de série, `valores[1..n]` vêm do `Argumentos.associar` |
| `proxima` | `int` | o índice da próxima linha do esqueleto a ser entregue |

**As duas pilhas.** O processador tem duas pilhas, cada uma com um papel:

- **pilha de expansões** (`Deque<Expansao> expansoes`, um `ArrayDeque`): diz
  **de onde vêm as linhas**. Cada chamada em andamento é um andar; o topo é a
  chamada mais interna. É ela que faz a chamada aninhada funcionar.
- **pilha de formais** (`List<List<String>> formais`, local ao `definir`): diz
  **de quem é cada parâmetro** enquanto uma definição é guardada. Cada nível é
  a lista `formaisComSerie()` de uma macro aberta (`[.SER, &A, &B...]`): o
  nível 0 é a macro que está sendo guardada, e cada `MACRO` encontrado no corpo
  acrescenta um nível. O tamanho da pilha é o **contador de nível de
  definição** dos slides. É ela que faz a definição aninhada funcionar.

Os outros campos guardam a posição da leitura: `fonte` (as linhas do arquivo),
`indiceFonte` (a próxima linha do arquivo a ler) e `linhaAtual` (o número, a
partir de 1, da última linha lida do arquivo, usado nas mensagens de erro).

### Funções desenvolvidas

| Método | Responsabilidade |
|---|---|
| `processar(List<String> fonte)` | O laço principal. Pede linhas ao `proximaLinha` até acabarem e trata cada uma em um dos três modos: `MACRO`/`MCDEFN` → `definir`; `MEND`/`MCEND` solto → erro; operação que é nome de macro → `expandir`; o resto → copia para a saída. Cada linha é tratada dentro de um `try`, e o erro é guardado com o `onde()` sem parar o processamento. |
| `proximaLinha()` | Devolve a próxima linha a tratar, venha de onde vier: se há expansão em andamento, a próxima linha do esqueleto do topo, já passada pelo `Substituicao.expandir`; quando o esqueleto acaba, desempilha e tenta de novo; sem expansões, a próxima linha do arquivo. `null` quando o fonte acaba. |
| `definir(LinhaMacro prototipo)` | Modo de definição. Lê o protótipo, depois o corpo pelo `proximaLinha`, até o `MEND` que esvazia a pilha de formais. Cada linha do corpo passa pelo `Substituicao.preparar` e vai para o esqueleto; comentários de linha inteira são descartados. No fim, guarda a macro na tabela (substituindo a anterior, se o nome já existia). |
| `lerPrototipo(LinhaMacro prototipo)` | Confere o nome da macro (tem que existir e ser só letras e dígitos, começando por letra), cria a `Macro` com a linha da definição e pede os formais ao `Argumentos.lerFormais`. |
| `formaisDaMacroDeDentro(LinhaMacro linha)` | Devolve os formais (com `.SER`) de uma macro definida dentro da que está sendo guardada, para o novo nível da pilha de formais. Se o protótipo dela tiver erro, devolve só `[.SER]`: o erro aparece quando ela for definida de verdade. |
| `expandir(LinhaMacro chamada)` | Modo de expansão. Recusa a macro que já está na pilha (chama a si mesma), pede os valores ao `Argumentos.associar`, soma 1 no contador e põe o número de série em `valores[0]`, escreve a chamada como comentário (e o `ROTULO EQU *`, se tiver rótulo) e empilha a expansão. |
| `onde()` | O começo da mensagem de erro: `linha N: ` ou, durante uma expansão, `linha N (na expansao de NOME): `. |

### Estratégias adotadas

**E1 — Uma passagem, três modos.** Os slides mostram uma implementação de
duas passagens (a primeira coleta todas as definições, a segunda expande) e
uma de uma passagem. Ficamos com a de uma passagem por causa da **definição
aninhada**: a macro de dentro só passa a existir quando a de fora é expandida.
Numa primeira passagem que só coleta definições, ninguém expande nada, então
a de dentro nunca seria definida. O laço do `processar` decide o modo pela
própria linha:

| Modo | Quando | O que faz com a linha |
|---|---|---|
| **Normal** (cópia) | linha comum, comentário ou linha vazia | copia para a saída |
| **Definição** | a operação é `MACRO` ou `MCDEFN` | o `definir` lê as linhas seguintes até o `MEND` desta definição |
| **Expansão** | a operação é o nome de uma macro da tabela | o `expandir` empilha a chamada; as linhas seguintes vêm do esqueleto |

O preço é a restrição dos slides: **não há referência avante**. A macro tem
que estar definida antes da primeira chamada. Uma chamada antes da definição
é copiada como linha comum, e o montador acusa operação desconhecida.

**E2 — O `proximaLinha`.** É o que permite aninhar. Como ele entrega a
próxima linha venha do arquivo ou do esqueleto de uma expansão, uma linha que
saiu de uma expansão passa pelo mesmo laço que uma linha do arquivo:

- se a linha expandida **chama outra macro**, o `expandir` empilha outra
  expansão: é a **chamada aninhada**. Quando o esqueleto da de dentro acaba,
  o `proximaLinha` desempilha e continua de onde a de fora parou;
- se a linha expandida **é um `MACRO`**, o `definir` começa uma definição e lê
  o corpo pelo mesmo `proximaLinha`, ou seja, do esqueleto da macro de fora, já
  com os parâmetros dela trocados: é a **definição aninhada**.

A pilha de expansões no teste 4, na primeira chamada de `DELTA` (que chama
`MULT` três vezes):

| Linha que sai do `proximaLinha` | Veio de | O que acontece | Pilha depois (topo à direita) |
|---|---|---|---|
| `DELTA A1,B1,C1,D1` | arquivo | é macro: empilha | DELTA |
| `MULT A1,C1,TEMP1` | DELTA | é macro: empilha | DELTA, MULT |
| `LDA A1` / `MUL C1` | MULT | copia | DELTA, MULT |
| `STA TEMP1` | MULT | copia; MULT acabou: desempilha | DELTA |
| `MULT TEMP1,#4,TEMP1` | DELTA | é macro: empilha | DELTA, MULT |
| ... | | | |
| `STA D1` | DELTA | copia; DELTA acabou: desempilha | (vazia) |

`MULT` é definida **depois** de `DELTA` no fonte, e funciona: na definição, as
linhas de `DELTA` só são guardadas; elas só são examinadas quando `DELTA` é
chamada, e aí `MULT` já existe.

**E3 — O contador de nível e a pilha de formais.** Dentro do `definir`, um
`MACRO` sobe um nível (acrescenta os formais da macro de dentro na pilha) e um
`MEND` desce um nível. O `MEND` que esvazia a pilha é o desta definição: ele
fecha a definição e não entra no esqueleto. Os `MACRO` e `MEND` internos
entram, porque fazem parte do corpo. No teste 3 (`ESCALA`, o exemplo `SCALE`
dos slides), o contador passa por 1, 2, 1, 2, 1, 0, a mesma sequência dos
slides.

A mesma pilha é passada para o `Substituicao.preparar`, que procura cada
parâmetro de dentro para fora: só o que é da macro do nível 0 vira `?n` agora;
os parâmetros das macros de dentro ficam com o nome e viram `?n` quando elas
forem definidas de verdade, na expansão da de fora. Por isso o esqueleto de
`ESCALA` guarda `SHIFTR A,?1` (o `&N` é de `ESCALA`) e `LDA &A` (o `&A` é de
`MULESC`).

**E4 — O número de série (`.SER`).** O contador `expansoes` do `Resultado` é
global: soma 1 a cada chamada expandida, inclusive as aninhadas, e o
`expandir` grava o valor com 4 dígitos (`0001`, `0002`...) em `valores[0]`,
que é o `?0` do esqueleto. Assim, `POS.SER` vira `POS0001` numa expansão e
`POS0002` na outra, e o montador não acusa símbolo duplicado (compare o teste
2 com o `erros/sem-ser.asm`). O mesmo contador é o número de chamadas
expandidas que o terminal e a janela mostram.

**E5 — O que vai para o `MASMAPRG.ASM`.** O `processar` só escreve na saída
em dois lugares: no modo normal (a linha como veio) e no `expandir`:

- a definição (`MACRO` até `MEND`) não aparece: virou entrada da tabela de
  macros;
- cada chamada aparece como comentário, com um `.` na frente da linha
  original, antes das linhas que gerou, inclusive as chamadas de dentro;
- o rótulo de uma chamada vira uma linha `ROTULO EQU *` antes das linhas
  expandidas. O `*` é o endereço atual, então o rótulo marca a primeira
  instrução gerada, sem brigar com um rótulo que a primeira linha do esqueleto
  já tenha.

Para o teste 1:

```
Fonte                                   MASMAPRG.ASM
         SOMA    PRECO,FRETE,TOTAL      .         SOMA    PRECO,FRETE,TOTAL
                                                 LDA     PRECO
                                                 ADD     FRETE
                                                 STA     TOTAL
DOBRAR   SOMA    TOTAL,TOTAL,DOBRO      .DOBRAR   SOMA    TOTAL,TOTAL,DOBRO
                                        DOBRAR   EQU     *
                                                 LDA     TOTAL
                                                 ADD     TOTAL
                                                 STA     DOBRO
```

**E6 — Erros.** Como no montador, o processador **não para no primeiro
erro**: cada linha é tratada dentro de um `try`, e o `catch` guarda a mensagem
do `ErroMacro` com o `onde()` na frente e segue para a próxima linha. O
`onde()` é calculado antes de tratar a linha, então o erro aponta a linha onde
o problema começou (no `falta o MEND`, a linha do `MACRO`). Um erro dentro de
uma expansão mostra a linha da chamada no fonte e o nome da macro:
`linha 8 (na expansao de LACO): ...`. Os erros desta classe:

| Erro | Onde é detectado | Mensagem |
|---|---|---|
| Definição sem fim | `definir`, quando o `proximaLinha` devolve `null` | `falta o MEND da macro DOBRA` |
| Fim sem definição | `processar` | `MEND sem MACRO correspondente` |
| Macro sem nome | `lerPrototipo` | `falta o nome da macro, que vai no rotulo: NOME MACRO &A,&B` |
| Nome inválido | `lerPrototipo` | `nome de macro invalido: 1X (use so letras e digitos)` |
| Macro que chama a si mesma | `expandir` | `a macro LACO chama a si mesma, e a expansao nunca terminaria` |

Os erros de parâmetro (formal repetido ou mal escrito, parâmetros demais,
palavra-chave inexistente) são lançados pelo `Argumentos` e pela `Macro` e
chegam ao mesmo `catch`, com a linha na frente.

Dois cuidados:

- **Protótipo com erro.** Se o protótipo tem erro, o `definir` guarda o erro,
  lê o corpo assim mesmo até o `MEND` e só depois lança. Se lançasse logo, as
  linhas do corpo seriam tratadas como linhas normais, e o `MEND` dela daria
  um `MEND sem MACRO` falso, além de outros erros em cascata.
- **Macro que chama a si mesma.** O nosso processador não tem macros
  condicionais (`IF`), então uma macro que chama a si mesma nunca pararia de
  expandir. Antes de empilhar, o `expandir` percorre a pilha de expansões
  procurando a mesma macro; se ela já está lá (direta ou indiretamente, como
  `A` chama `B` que chama `A`), é erro.

**Reaproveitamento.** A classe não mexe no montador da Etapa 2: ela só
transforma texto em texto, e o montador recebe o `MASMAPRG.ASM` como recebia
qualquer fonte. A quebra da linha em rótulo, operação e operando é a da
`LinhaMacro`, que usa o mesmo `AnalisadorLinha.palavras` do montador; por isso
as regras de coluna e de comentário são as mesmas nas duas etapas.

### Demonstração da parte E

No teste 1, mostrar o `MASMAPRG.ASM`: a definição sumiu, cada chamada virou
comentário seguido de três linhas, e o `DOBRAR` virou `DOBRAR EQU *`. No teste
3, mostrar que a expansão de `ESCALA 2` não gera nenhuma linha no
`MASMAPRG.ASM`, só define `MULESC` e `DIVESC` (a definição aninhada), e que a
chamada seguinte de `MULESC` já sai com `SHIFTR A,2`. No teste 4, seguir a pilha de expansões com a tabela da estratégia
E2 e mostrar as chamadas de `MULT` como comentários dentro da expansão de
`DELTA`. No código, mostrar o `proximaLinha` e o laço do `processar`. Para os
erros, rodar o `erros/macro-recursiva.asm` e o `erros/falta-mend.asm`.

### Validação da parte E (13/11/2026)

Os testes foram executados em um diretório temporário fora do repositório,
compilados com OpenJDK 25.0.1 e `javac -encoding UTF-8`. Como o `Argumentos`
(parte A), a `Substituicao` (parte C) e o `MacroMontador` e o `MacroConsole`
(parte F) ainda não estavam no GitHub, essas quatro classes vieram do zip da
Etapa 3; o resto do código era o do repositório, com o `ProcessadorMacros`
desta parte. Foram aprovadas 10 verificações:

- **Testes 1 a 5 (5):** cada um passou pelo `MacroConsole` sem erros, com o
  número de chamadas expandidas esperado (2, 2, 5, 8 e 5), e o programa objeto
  rodou no `TesteConsole` com os valores da tabela da Seção 7.6 do guia:
  TOTAL = 12 e DOBRO = 24; D1 = 7 e D2 = 16; R1 = 15, R2 = 240 e R3 = 7;
  D1 = 25 e D2 = -16; CONTA = 14 e OUTRA = 97.
- **Testes de erro (5):** cada arquivo de `testes/macros/erros/` deu
  exatamente a mensagem da sua primeira linha: `falta o MEND da macro DOBRA`
  (linha 3), `MEND sem MACRO correspondente` (linha 4), `parametros demais na
  chamada de SOMA (ela tem 3)` (linha 8), `a macro LACO chama a si mesma, e a
  expansao nunca terminaria` (linha 8, na expansão de `LACO`) e, no
  `sem-ser.asm`, nenhum erro de macro e o `simbolo duplicado: POS` do
  montador na linha 20 do `MASMAPRG.ASM`.

## 11. Etapa 3 — Listas de parâmetros (Samuel, parte A)

Esta parte segue o capítulo 4 do Beck e os slides "03 - Macros" do professor (o
protótipo com os parâmetros formais e a lista de parâmetros reais da chamada). A
implementação parte de uma versão de referência do grupo, montada a partir desse
material; ela foi conferida e testada, e os cuidados que encontramos estão
descritos nas estratégias abaixo. São duas classes do pacote `sicxe.macro`:
`ErroMacro`, a exceção do processador de macros, e `Argumentos`, que lê os
parâmetros formais do protótipo e associa os parâmetros reais de cada chamada.

### Estruturas de dados

**ErroMacro** é uma exceção (`RuntimeException`) lançada por quem detecta um erro
de macro: parâmetros demais na chamada, palavra-chave inexistente, formal mal
escrito. Quem lança não sabe em que linha está. O `ProcessadorMacros` captura o
erro, junta o número da linha e segue para a linha seguinte, de modo que todos os
erros aparecem de uma vez, como o montador faz com o `ErroMontagem`.

**Argumentos** não guarda dados: é uma classe final, com construtor privado e
três métodos estáticos. O que ela produz é a **lista de parâmetros reais** dos
slides, um `String[] valores` com uma posição a mais que o número de formais:

| Posição | Conteúdo | Quem preenche |
|---|---|---|
| `valores[0]` | o número de série da expansão (`.SER`) | o `ProcessadorMacros`; o `associar` deixa `null` |
| `valores[1]` a `valores[n]` | o parâmetro real de cada formal, ou o padrão | `Argumentos.associar` |

A posição `k` do vetor é a mesma do `?k` no esqueleto da macro (Pedro):
`valores[1]` substitui o `?1`, `valores[2]` o `?2`, e assim por diante.

Exemplo, com `INCR  MACRO  &VAR,&PASSO=1,&OP=ADD` (formais `[&VAR, &PASSO, &OP]` e
padrões `["", "1", "ADD"]`):

| Chamada | `valores` devolvido |
|---|---|
| `INCR CONTA` | `[null, CONTA, 1, ADD]` |
| `INCR CONTA,5` | `[null, CONTA, 5, ADD]` |
| `INCR OP=SUB,VAR=CONTA` | `[null, CONTA, 1, SUB]` |
| `INCR CONTA,,SUB` | `[null, CONTA, 1, SUB]` |
| `INCR OUTRA,OP=SUB,PASSO=3` | `[null, OUTRA, 3, SUB]` |

### Funções desenvolvidas

| Classe | Método ou construtor | Responsabilidade |
|---|---|---|
| `ErroMacro` | `ErroMacro(String mensagem)` | Cria o erro com a mensagem que vai para a lista de erros, depois de o processador juntar o número da linha. |
| `Argumentos` | `separar(String operando)` | Quebra o operando nas vírgulas, sem quebrar dentro de apóstrofos: `A,C'X,Y',,B` dá quatro itens (`A`, `C'X,Y'`, vazio e `B`). `null` e `""` dão lista vazia. |
| `Argumentos` | `lerFormais(Macro macro, String operando)` | Lê o protótipo (`&A,&B,&C=TOTAL`) e guarda cada formal, em maiúsculas, e o seu padrão (ou `""`) na `Macro`, pelo `adicionarFormal`. Lança `ErroMacro` se o nome for inválido. |
| `Argumentos` | `associar(Macro macro, String operando)` | Associa os parâmetros reais da chamada aos formais, por posição ou por palavra-chave, e devolve o `String[] valores`. Quem ficou sem valor, ou com valor vazio, recebe o padrão. |
| `Argumentos` | `nomeValido(String nome)` (privado) | Confere `&NOME` ou `NOME`: uma letra no começo e depois só letras e dígitos. |

### Estratégias adotadas

- **Vírgula entre apóstrofos não separa.** O `separar` anda pelo texto caractere a
  caractere e inverte o `entreApostrofos` a cada `'`; só uma vírgula fora dos
  apóstrofos fecha um parâmetro. É o mesmo cuidado que o montador tem com a
  constante `C'...'`, de modo que `C'A,B'` é um parâmetro só. Um parâmetro vazio
  (`X,,Z`) é mantido como item vazio, porque a posição dele conta.
- **Posicional ou palavra-chave.** Um parâmetro real é palavra-chave quando tem um
  `=` e o que vem antes é um nome válido; a posição é buscada com o
  `Macro.indiceFormal`, que aceita o nome com ou sem `&` (na chamada se escreve
  `PASSO=5` para o formal `&PASSO`). Os demais são posicionais. Um `=` dentro de
  apóstrofos (`C'A=B'`) ou no começo (`=C'A'`) não faz do parâmetro uma
  palavra-chave.
- **O contador do posicional só conta posicionais.** A variável
  `proximaPosicao` só avança nos parâmetros posicionais e não pula posições já
  preenchidas por palavra-chave. Por isso a ordem importa: `OUTRA,OP=SUB,PASSO=3`
  funciona (o posicional vai para a posição 1 e as palavras-chave para as
  posições 3 e 2), mas `VAR=B,A` dá erro, porque o `A` iria para a posição 1, que
  a palavra-chave `VAR` já preencheu.
- **Omitido ou vazio vira o padrão.** Depois de associar tudo, cada posição sem
  valor, ou com valor vazio, recebe o padrão do protótipo. É assim que
  `CONTA,,SUB` dá `PASSO = 1`. Sem padrão, o valor fica vazio e, se a linha
  precisar dele, quem reclama é o montador. O valor é copiado como texto, sem ser
  avaliado, como o resto do processador.
- **Erros.** Todos são `ErroMacro`, sem número de linha; o processador junta a
  linha:

| Situação | Mensagem |
|---|---|
| Formal mal escrito | `parametro formal invalido: '&1X' (use &NOME, so letras e digitos)` |
| Formal repetido (lançado pela `Macro`) | `parametro formal repetido: &A` |
| Palavra-chave que não existe | `a macro INCR nao tem o parametro ZZ` |
| Parâmetros demais | `parametros demais na chamada de INCR (ela tem 3)` |
| Mesmo parâmetro duas vezes | `o parametro &VAR recebeu valor duas vezes` |

- **Formais sem `&`.** O `nomeValido` aceita o nome com ou sem `&`, porque também
  serve aos nomes de palavra-chave, que não levam `&`; por isso o `lerFormais`
  aceita `SOMA MACRO P,Q`. Isso funciona se o corpo escreve os nomes do mesmo
  jeito (`LDA P`), mas o `&` é recomendado: sem ele, um formal com o nome de um
  registrador ou de uma instrução (`A`, `X`, `ADD`) troca também essas palavras
  no corpo, porque a troca é por palavra inteira. E a mistura não funciona: com
  `SOMA MACRO A,B` e o corpo `LDA &A`, o `&A` nunca é trocado, e o montador
  reclama depois, no fonte expandido, com `simbolo indefinido: &A`. Todos os
  testes escrevem os formais com `&`.
- **Espaço depois da vírgula.** A chamada `SOMA X, Y` tem um parâmetro só: depois
  do operando, o resto da linha é comentário (a mesma regra do montador), então o
  `Y` não chega ao `Argumentos`. Os parâmetros se escrevem sem espaços:
  `SOMA X,Y`.
- **Reaproveitamento.** As duas classes não mexem no montador da Etapa 2 nem na
  `Macro` do Pedro: o `Argumentos` só usa o `adicionarFormal`, o `indiceFormal`,
  a lista `formais` e a lista `padroes` dela.

### Demonstração da parte A

No teste 5 (`05-palavra-chave.asm`), mostrar as cinco chamadas do `INCR` e o que
o `associar` devolve para cada uma (a tabela das estruturas de dados): os dois
padrões (`INCR CONTA`, `CONTA` vira 11), o posicional (`INCR CONTA,5`, 16), a
palavra-chave em qualquer ordem (`OP=SUB,VAR=CONTA`, 15), o parâmetro omitido
(`CONTA,,SUB`, 14) e o misturado (`OUTRA,OP=SUB,PASSO=3`, `OUTRA` vira 97). Mostrar
também o `C'X,Y'` como parâmetro único. Para o erro, rodar o
`erros/parametros-demais.asm`: o `associar` lança `parametros demais na chamada
de SOMA (ela tem 3)`, e o processador acrescenta `macros, linha 8`.

### Validação da parte A (13/11/2026)

Os testes foram executados em diretório temporário fora do repositório,
compilados com OpenJDK 25.0.5 e `javac -encoding UTF-8`, sem alterar testes
oficiais. Foram aprovadas 34 verificações diretas:

- **`separar` (9):** o exemplo `A,C'X,Y',,B` (quatro itens, o terceiro vazio),
  `null`, `""`, `X,,Z`, vírgula no fim, só uma vírgula e `X'F1,F2',Z`.
- **`lerFormais` (9):** formais, padrões e protótipo de `&VAR,&PASSO=1,&OP=ADD`,
  maiúsculas, padrão preservado como escrito, os erros de nome inválido (`&1X` e
  item vazio), de formal repetido e o operando `null`.
- **`associar` (12):** os dois exemplos do guia (`CONTA,,SUB` e
  `OP=SUB,VAR=X`), as cinco chamadas do teste 5, operando `null`, vírgula e `=`
  dentro de apóstrofos, palavra-chave com `&` e em minúsculas e o `=C'A'` como
  posicional.
- **Erros (4):** parâmetros demais, palavra-chave inexistente, valor duas vezes e
  palavra-chave seguida de posicional na mesma posição.

Na data desta validação, o `Substituicao` (parte C), o `MacroMontador` e o
`MacroConsole` (parte F) e o `MontadorConsole` (Etapa 2) ainda não estavam no
GitHub; por isso o repositório ainda não compilava por inteiro, e o `Argumentos`
foi testado com essas quatro classes vindas da versão de referência do grupo, e
com o resto do código do repositório, incluindo o `ProcessadorMacros` do João.
Nessa montagem, os cinco testes passaram pelo `MacroConsole` sem erros, com o
número de chamadas esperado (2, 2, 5, 8 e 5), os `.obj` e `.lst` idênticos aos de
referência e os valores da tabela de testes: TOTAL = 12 e DOBRO = 24; D1 = 7 e
D2 = 16; R1 = 15, R2 = 240 e R3 = 7; D1 = 25 e D2 = -16; CONTA = 14 e OUTRA = 97.
Os cinco arquivos de `testes/macros/erros/` deram a mensagem da primeira linha,
entre elas `parametros demais na chamada de SOMA (ela tem 3)`, que vem do
`Argumentos`.

Foram rodados também 14 programas de borda, fora dos testes oficiais, e todos
tiveram o resultado esperado: formais com prefixo igual (`&A` e `&AB`); fonte em
minúsculas, com tabulação e fim de linha CRLF; redefinição de macro; macro vazia
e macro sem parâmetros; rótulo na chamada; `+LDA #&V` e `LDA @&V`; macro que chama
outra definida depois dela; vírgula e valor hexadecimal entre apóstrofos; seis
erros de macro de uma vez, sem erro em cascata; recursão indireta
(`PING` chama `PONG`, que chama `PING`); e os casos de parâmetro faltando, espaço
depois da vírgula e formal sem `&`, que são descritos nas estratégias. O `ErroMacro` e o `Argumentos` compilam com o `Macro` (Pedro) e
com o `ProcessadorMacros` (João) do repositório, sem erro que os cite.
