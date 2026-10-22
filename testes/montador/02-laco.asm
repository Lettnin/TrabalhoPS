. Teste 2 do montador - soma os 5 elementos de um vetor (testes/02-laco.hex).
. STA SOMA usa o SOMA antes de ele ser definido: referencia futura.
. JLT LOOP volta para tras: deslocamento negativo relativo ao PC.
LACO     START   0
         LDX     #0          . X = 0 (indice em bytes)
         LDA     #0          . A = 0
         LDS     #3          . S = 3 (tamanho de uma palavra)
         LDT     #15         . T = 15 (5 elementos x 3 bytes)
LOOP     ADD     VETOR,X     . A = A + vetor[X]
         ADDR    S,X         . X = X + 3
         COMPR   X,T         . compara X com T
         JLT     LOOP        . volta enquanto X < T
         STA     SOMA
         RSUB
VETOR    WORD    1
         WORD    2
         WORD    3
         WORD    4
         WORD    5
SOMA     RESW    1
         END     LACO
