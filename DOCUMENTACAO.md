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
