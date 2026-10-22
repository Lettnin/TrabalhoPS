. Teste 4 do montador - deslocamentos, negativos, divisao e bytes
. (testes/04-bytes-e-sinal.hex).
SINAL    START   0
         LDA     #7          . A = 7
         SHIFTL  A,4         . A = 112 (o montador grava 4 - 1 = 3)
         SHIFTR  A,2         . A = 28
         LDA     #10
         SUB     #25         . A = -15 (FFFFF1)
         STA     NEG
         COMP    #0          . compara -15 com 0: CC fica "<"
         JLT     PULA
         J       FIM         . nao deve ser executado
PULA     LDCH    TEXTO       . A recebe so o byte 'A' (41)
         STCH    SAIDA       . grava esse byte em SAIDA
         LDA     #100
         DIV     #7          . A = 14
         STA     DIVRES
FIM      CLEAR   L
         RSUB
NEG      RESW    1
TEXTO    BYTE    C'ABC'
SAIDA    RESW    1
DIVRES   RESW    1
         END     SINAL
