package cpf;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class CPFValidator {

    // Lista de opções válidas de threads
    private static final List<Integer> OPCOES_VALIDAS = Arrays.asList(1, 2, 3, 5, 6, 10, 15, 30);

    // Valida um CPF
    public static boolean validaCPF(String cpf) {
        cpf = cpf.replaceAll("\\D", "");
        if (cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        for (int i = 9; i <= 10; i++) {
            int soma = 0;
            for (int j = 0; j < i; j++) {
                soma += (cpf.charAt(j) - '0') * ((i + 1) - j);
            }
            int digito = (soma * 10) % 11;
            if (digito == 10) digito = 0;
            if (digito != (cpf.charAt(i) - '0')) {
                return false;
            }
        }
        return true;
    }

    // Lê todos os arquivos .txt na pasta "cpfs"
    public static List<File> obterArquivosTxt(String caminho) {
        File pasta = new File(caminho);
        File[] arquivos = pasta.listFiles((dir, nome) -> nome.endsWith(".txt"));
        return arquivos != null ? Arrays.asList(arquivos) : new ArrayList<>();
    }

    // Executa a validação com N threads
    public static void executarValidacao(List<File> arquivos, int numThreads) {
        int qtdArquivos = arquivos.size();
        int tamanhoLote = (int) Math.ceil((double) qtdArquivos / numThreads);

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<int[]>> resultados = new ArrayList<>();

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < qtdArquivos; i += tamanhoLote) {
            List<File> sublista = arquivos.subList(i, Math.min(i + tamanhoLote, qtdArquivos));
            Callable<int[]> tarefa = () -> processarArquivos(sublista);
            resultados.add(executor.submit(tarefa));
        }

        executor.shutdown();
        int totalValidos = 0;
        int totalInvalidos = 0;

        try {
            for (Future<int[]> futuro : resultados) {
                int[] resultado = futuro.get();
                totalValidos += resultado[0];
                totalInvalidos += resultado[1];
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        long fim = System.currentTimeMillis();
        long tempoTotal = fim - inicio;

        System.out.println("CPFs válidos: " + totalValidos);
        System.out.println("CPFs inválidos: " + totalInvalidos);
        System.out.println("Tempo total de execução com " + numThreads + " threads: " + tempoTotal + " ms");

        salvarTempoExecucao(tempoTotal, numThreads);
    }

    // Processa uma lista de arquivos e conta CPFs válidos e inválidos
    public static int[] processarArquivos(List<File> arquivos) {
        int validos = 0;
        int invalidos = 0;

        for (File arquivo : arquivos) {
            try (Scanner scanner = new Scanner(arquivo)) {
                while (scanner.hasNextLine()) {
                    String cpf = scanner.nextLine().trim();
                    if (!cpf.isEmpty()) {
                        if (validaCPF(cpf)) {
                            validos++;
                        } else {
                            invalidos++;
                        }
                    }
                }
            } catch (FileNotFoundException e) {
                System.err.println("Arquivo não encontrado: " + arquivo.getName());
            }
        }

        return new int[]{validos, invalidos};
    }

    // Salva o tempo de execução no diretório /resultados
    public static void salvarTempoExecucao(long tempo, int numThreads) {
        File dir = new File("resultados");  // <- caminho corrigido
        if (!dir.exists()) {
            boolean criada = dir.mkdirs();
            if (!criada) {
                System.err.println("Erro ao criar diretório: " + dir.getPath());
                return;
            }
        }

        File arquivo = new File(dir, "versao_" + numThreads + "_threads.txt");
        try (FileWriter writer = new FileWriter(arquivo)) {
            writer.write("Tempo total de execução: " + tempo + " ms");
            System.out.println("Tempo salvo em: " + arquivo.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar tempo: " + e.getMessage());
        }
    }

    // Programa principal com loop de escolha
    public static void main(String[] args) {
        List<File> arquivos = obterArquivosTxt("cpfs");
        if (arquivos.isEmpty()) {
            System.out.println("Nenhum arquivo .txt encontrado na pasta 'cpfs'.");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nSelecione o número de threads (ou digite 0 para sair):");
            System.out.println("Opções válidas: " + OPCOES_VALIDAS);
            System.out.print("Sua escolha: ");

            int escolha;
            try {
                escolha = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida. Tente novamente.");
                continue;
            }

            if (escolha == 0) {
                System.out.println("Encerrando o programa.");
                break;
            }

            if (!OPCOES_VALIDAS.contains(escolha)) {
                System.out.println("Opção inválida. Tente novamente.");
                continue;
            }

            executarValidacao(arquivos, escolha);
        }
        scanner.close();
    }
}
