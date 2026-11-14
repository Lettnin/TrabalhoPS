. Erro esperado: macros, linha 8 (na expansao de LACO): a macro LACO chama
. a si mesma, e a expansao nunca terminaria
ERRO4    START   0
LACO     MACRO   &V
         LDA     &V
         LACO    &V
         MEND
         LACO    VALOR
         RSUB
VALOR    WORD    1
         END     ERRO4
