package sicxe.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import sicxe.maquina.ErroExecucao;
import sicxe.maquina.Memoria;


public class Carregador {

    public static class ResultadoCarga {
        public String nomePrograma = "";
        public int enderecoInicial = 0;   // onde o programa foi colocado
        public int enderecoExecucao = 0;  // onde a execucao comeca
        public int bytesCarregados = 0;
    }
    public ResultadoCarga carregar(File arquivo, Memoria mem) {
        try (BufferedReader leitor = new BufferedReader(new FileReader(arquivo))) {
            ResultadoCarga res = new ResultadoCarga();
            boolean achouRegistro = false;
            boolean achouExecucao = false;
            String linha;
            int numeroLinha = 0;

            while ((linha = leitor.readLine()) != null) {
                numeroLinha++;
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith(";")) {
                    continue;
                }
                char tipo = Character.toUpperCase(linha.charAt(0));

                if (tipo == 'H' && linha.length() > 1) {
                    String corpo = linha.substring(1).replace("^", "").trim();
                    if (corpo.length() >= 18) {
                        res.nomePrograma = corpo.substring(0, 6).trim();
                        res.enderecoInicial = Integer.parseInt(corpo.substring(6, 12), 16);
                    }
                    achouRegistro = true;
                } else if (tipo == 'T') {
                    String corpo = linha.substring(1).replace("^", "").trim();
                    int endereco = Integer.parseInt(corpo.substring(0, 6), 16);
                    int quantos = Integer.parseInt(corpo.substring(6, 8), 16);
                    String codigo = corpo.substring(8);
                    for (int k = 0; k < quantos; k++) {
                        int valor = Integer.parseInt(codigo.substring(k * 2, k * 2 + 2), 16);
                        mem.escreverByte(endereco + k, valor);
                        res.bytesCarregados++;
                    }
                    achouRegistro = true;
                } else if (tipo == 'E') {
                    String corpo = linha.substring(1).replace("^", "").trim();
                    if (!corpo.isEmpty()) {
                        res.enderecoExecucao = Integer.parseInt(corpo, 16);
                        achouExecucao = true;
                    }
                    achouRegistro = true;
                } else if (linha.toUpperCase().startsWith("START")) {
                    res.enderecoExecucao = Integer.parseInt(linha.substring(5).trim(), 16);
                    achouExecucao = true;
                } else if (linha.contains(":")) {
                    String[] partes = linha.split(":", 2);
                    int endereco = Integer.parseInt(partes[0].trim(), 16);
                    String[] bytes = partes[1].trim().split("\\s+");
                    for (String by : bytes) {
                        if (by.isEmpty()) {
                            continue;
                        }
                        mem.escreverByte(endereco, Integer.parseInt(by, 16));
                        endereco++;
                        res.bytesCarregados++;
                    }
                    achouRegistro = true;
                } else {
                    throw new ErroExecucao("Linha " + numeroLinha + " nao reconhecida: " + linha);
                }
            }

            if (!achouRegistro) {
                throw new ErroExecucao("O arquivo nao contem nenhum dado carregavel.");
            }
            if (!achouExecucao) {
                res.enderecoExecucao = res.enderecoInicial;
            }
            return res;

        } catch (IOException e) {
            throw new ErroExecucao("Nao foi possivel ler o arquivo: " + e.getMessage());
        } catch (NumberFormatException e) {
            throw new ErroExecucao("Valor hexadecimal invalido no arquivo: " + e.getMessage());
        } catch (StringIndexOutOfBoundsException e) {
            throw new ErroExecucao("Registro incompleto no arquivo objeto.");
        }
    }
}