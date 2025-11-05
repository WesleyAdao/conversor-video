package conversorvideo;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import conversorvideo.ConversaoItem;
import java.nio.file.Paths;

public class Conversor {
    private static volatile boolean cancelar = false;
    private static String ffmpegPath = null;

    /**
     * Retorna o caminho do ffmpeg local (ffmpeg/bin/ffmpeg.exe) se existir, senão retorna "ffmpeg" (PATH).
     * Para distribuição, coloque o ffmpeg.exe em 'ffmpeg/bin/' dentro da pasta do projeto.
     */
    public static String getFfmpegPath() {
        if (ffmpegPath != null) return ffmpegPath;
        String localPath = Paths.get(System.getProperty("user.dir"), "ffmpeg", "bin", "ffmpeg.exe").toString();
        if (new File(localPath).exists()) {
            ffmpegPath = localPath;
        } else {
            ffmpegPath = "ffmpeg";
        }
        return ffmpegPath;
    }

    public static void solicitarCancelamento() {
        cancelar = true;
    }

    public static void resetarCancelamento() {
        cancelar = false;
    }

    /**
     * Verifica se o ffmpeg está disponível localmente ou no PATH. Exibe mensagem detalhada se não encontrar.
     */
    public static boolean ffmpegDisponivel() {
        try {
            ProcessBuilder pb = new ProcessBuilder(getFfmpegPath(), "-version");
            Process p = pb.start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            System.err.println("FFmpeg não encontrado em 'ffmpeg/bin/ffmpeg.exe' nem no PATH do sistema.");
            System.err.println("Coloque o ffmpeg.exe em 'ffmpeg/bin/' ou adicione ao PATH.");
            return false;
        }
    }

    public interface ConversaoListener {
        void onProgresso(String arquivo, int progresso, long tempoMedioMs, long tempoRestanteMs);
        void onFinalizado(String arquivo);
        void onErro(String arquivo, String mensagem);
    }

    public interface ProgressoListener {
        void onProgresso(double progresso, String tempoEstimado);
    }

    public static class LogConversao {
        public final String arquivo;
        public final boolean sucesso;
        public final String mensagem;
        public LogConversao(String arquivo, boolean sucesso, String mensagem) {
            this.arquivo = arquivo;
            this.sucesso = sucesso;
            this.mensagem = mensagem;
        }
        @Override
        public String toString() {
            return (sucesso ? "[OK] " : "[ERRO] ") + arquivo + (mensagem != null ? ": " + mensagem : "");
        }
    }

    public static List<File> listarArquivosMKV(File pastaOrigem) {
        List<File> arquivos = new ArrayList<>();
        File[] files = pastaOrigem.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase().endsWith(".mkv")) {
                    arquivos.add(f);
                }
            }
        }
        return arquivos;
    }

    public static List<LogConversao> converterArquivos(List<File> arquivos, File pastaDestino, ConversaoListener listener) {
        resetarCancelamento();
        List<LogConversao> logs = new ArrayList<>();
        long tempoTotal = 0;
        int arquivosConvertidos = 0;
        for (File arquivo : arquivos) {
            if (cancelar) {
                logs.add(new LogConversao(arquivo.getName(), false, "Conversão cancelada pelo usuário."));
                break;
            }
            String nomeBase = arquivo.getName().replaceFirst("\\.mkv$", "");
            File destino = new File(pastaDestino, nomeBase + ".mp4");
            long inicio = System.currentTimeMillis();
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    getFfmpegPath(),
                    "-i", arquivo.getAbsolutePath(),
                    "-c:v", "copy",
                    "-c:a", "copy",
                    destino.getAbsolutePath()
                );
                pb.redirectErrorStream(true);
                Process processo = pb.start();
                processo.waitFor();
                long fim = System.currentTimeMillis();
                tempoTotal += (fim - inicio);
                long tempoMedio = arquivosConvertidos > 0 ? tempoTotal / arquivosConvertidos : 0;
                long tempoRestante = tempoMedio * (arquivos.size() - arquivosConvertidos);
                listener.onProgresso(destino.getName(), (int) (((arquivosConvertidos + 1) * 100) / arquivos.size()), tempoMedio, tempoRestante);
                listener.onFinalizado(destino.getName());
                arquivosConvertidos++;
                logs.add(new LogConversao(destino.getName(), true, null));
            } catch (Exception e) {
                listener.onErro(arquivo.getName(), e.getMessage());
                logs.add(new LogConversao(arquivo.getName(), false, e.getMessage()));
            }
        }
        return logs;
    }

    public static boolean converterArquivoComProgresso(ConversaoItem item, File destino, ProgressoListener listener) {
        resetarCancelamento();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                getFfmpegPath(),
                "-i", item.getCaminhoOrigem(),
                "-c:v", "copy",
                "-c:a", "copy",
                destino.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process processo = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(processo.getInputStream()));
            String linha;
            double duracaoTotal = 0;
            Pattern duracaoPattern = Pattern.compile("Duration: (\\d+):(\\d+):(\\d+).(\\d+)");
            Pattern timePattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+).(\\d+)");
            while ((linha = reader.readLine()) != null) {
                if (duracaoTotal == 0) {
                    Matcher m = duracaoPattern.matcher(linha);
                    if (m.find()) {
                        int h = Integer.parseInt(m.group(1));
                        int mnt = Integer.parseInt(m.group(2));
                        int s = Integer.parseInt(m.group(3));
                        int ms = Integer.parseInt(m.group(4));
                        duracaoTotal = h * 3600 + mnt * 60 + s + ms / 100.0;
                    }
                }
                Matcher m = timePattern.matcher(linha);
                if (m.find() && duracaoTotal > 0) {
                    int h = Integer.parseInt(m.group(1));
                    int mnt = Integer.parseInt(m.group(2));
                    int s = Integer.parseInt(m.group(3));
                    int ms = Integer.parseInt(m.group(4));
                    double tempoAtual = h * 3600 + mnt * 60 + s + ms / 100.0;
                    double progresso = Math.min(tempoAtual / duracaoTotal, 1.0);
                    String tempoEstimado = formatarTempo((int)(duracaoTotal - tempoAtual));
                    listener.onProgresso(progresso, tempoEstimado);
                }
                if (cancelar) {
                    processo.destroy();
                    return false;
                }
            }
            int exit = processo.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String formatarTempo(int segRestante) {
        int min = segRestante / 60;
        int hor = min / 60;
        int seg = segRestante % 60;
        min = min % 60;
        if (hor > 0) {
            return String.format("%dh %02dm %02ds", hor, min, seg);
        } else if (min > 0) {
            return String.format("%dm %02ds", min, seg);
        } else {
            return String.format("%ds", seg);
        }
    }
}
