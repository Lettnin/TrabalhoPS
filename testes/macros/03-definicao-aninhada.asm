. Teste 3 das macros - definicao aninhada e redefinicao (exemplo SCALE dos
. slides). ESCALA nao gera codigo: ela DEFINE as macros MULESC e DIVESC,
. com o numero de bits do deslocamento ja trocado pelo parametro &N.
. Antes de ESCALA ser chamada, MULESC nao existe. Cada chamada de ESCALA
. define MULESC de novo, com outro &N (redefinicao).
.   MULESC A,B,C:  C = (A * B) deslocado &N bits para a direita
.   DIVESC A,B,C:  C = (A * B) deslocado &N bits para a esquerda
TESTE3   START   0
ESCALA   MACRO   &N
MULESC   MACRO   &A,&B,&C
         LDA     &A
         MUL     &B
         SHIFTR  A,&N
         STA     &C
         MEND
DIVESC   MACRO   &A,&B,&C
         LDA     &A
         MUL     &B
         SHIFTL  A,&N
         STA     &C
         MEND
         MEND
         ESCALA  2             . define MULESC e DIVESC com &N = 2
         MULESC  P,Q,R1        . R1 = 60 / 4 = 15
         DIVESC  P,Q,R2        . R2 = 60 * 4 = 240
         ESCALA  3             . redefine as duas com &N = 3
         MULESC  P,Q,R3        . R3 = 60 / 8 = 7
         RSUB
P        WORD    6
Q        WORD    10
R1       RESW    1
R2       RESW    1
R3       RESW    1
         END     TESTE3
