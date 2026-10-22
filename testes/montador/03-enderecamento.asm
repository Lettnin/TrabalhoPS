. Teste 3 do montador - modos de enderecamento, BASE, formato 4 e sub-rotina.
. E o programa de testes/03-enderecamento.hex com uma diferenca: a TABELA
. foi empurrada para longe (ESPACO RESB 2048). Perto, o montador usaria o
. modo relativo ao PC, e o modo base nao apareceria.
TESTE3   START   1000
         +LDB    #TABELA     . formato 4 + imediato: B = endereco da TABELA
         BASE    TABELA      . avisa o montador que B vale TABELA daqui em diante
         LDA     #7          . A = 7
         STA     TABELA      . longe demais para o PC: usa a base
         LDA     #0          . A = 0
         LDA     TABELA      . base de novo: A = 7
         JSUB    DOBRA       . relativo ao PC: L = 1013
         STA     RESULT      . RESULT = 14
         LDA     @PONTEIRO   . indireto: PONTEIRO guarda o endereco de VALOR
         +STA    FINAL       . formato 4 com rotulo: gera registro M
         CLEAR   L           . L = 0
         RSUB                . L = 0 -> fim do programa
DOBRA    ADDR    A,A         . A = A + A
         RSUB                . volta para o endereco guardado em L
RESULT   RESW    1
FINAL    RESW    1
PONTEIRO WORD    VALOR       . guarda um endereco: tambem gera registro M
VALOR    WORD    99
ESPACO   RESB    2048
TABELA   RESW    1
         END     TESTE3
