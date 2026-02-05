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
import java.util.concurrent.ConcurrentHashMap;

public class Conversor {
    private static volatile boolean cancelar = false;
    private static final ConcurrentHashMap<ConversaoItem, Process> processosPorItem = new ConcurrentHashMap<>();
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

    public static void solicitarCancelamentoItem(ConversaoItem item) {
        Process processo = processosPorItem.get(item);
        if (processo != null) {
            processo.destroy();
            processosPorItem.remove(item);
            // Exclui arquivo de destino incompleto
            if (item.getCaminhoDestino() != null) {
                File destino = new File(item.getCaminhoDestino());
                if (destino.exists()) destino.delete();
            }
            item.setStatus(StatusConversao.CANCELADO);
            item.setProgresso(0.0);
            item.setTempoEstimado("");
        }
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
            processosPorItem.put(item, processo);
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
                if (cancelar || Thread.currentThread().isInterrupted()) {
                    processo.destroy();
                    processosPorItem.remove(item);
                    if (destino.exists()) destino.delete();
                    return false;
                }
            }
            int exit = processo.waitFor();
            processosPorItem.remove(item);
            return exit == 0;
        } catch (Exception e) {
            processosPorItem.remove(item);
            if (destino.exists()) destino.delete();
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
}
