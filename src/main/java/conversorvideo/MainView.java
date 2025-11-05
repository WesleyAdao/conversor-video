package conversorvideo;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.ListCell;
import javafx.util.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainView {
    private final List<ConversaoItem> filaConversao = new CopyOnWriteArrayList<>();
    private File pastaDestino;
    private File ultimaPastaOrigem = null;
    private File ultimaPastaDestino = null;
    private final ListView<ConversaoItem> listaArquivos = new ListView<>();
    private final Button btnConverter = new Button("Converter");
    private final Button btnAbrirDestino = new Button("Abrir Pasta de Destino");
    private final Button btnCancelar = new Button("Cancelar Conversão");
    private final Label lblStatus = new Label();
    private final TextField txtDestino = new TextField();
    private final TextField txtArquivos = new TextField();
    private static final String CONFIG_FILE = "config.properties";

    public void start(Stage stage) {
        carregarPastasSalvas();
        Button btnSelecionarArquivos = new Button("Selecionar Arquivos");
        Button btnSelecionarPasta = new Button("Selecionar Pasta de Destino");
        btnSelecionarArquivos.setOnAction(e -> selecionarArquivos(stage));
        btnSelecionarPasta.setOnAction(e -> selecionarPasta(stage));
        btnConverter.setOnAction(e -> iniciarConversao());
        btnCancelar.setOnAction(e -> cancelarConversao());
        btnAbrirDestino.setOnAction(e -> abrirPastaDestino());
        txtDestino.setEditable(false);
        txtDestino.setPromptText("Selecione a pasta de destino...");
        txtDestino.setPrefWidth(450);
        txtArquivos.setEditable(false);
        txtArquivos.setPromptText("Selecione os arquivos...");
        txtArquivos.setPrefWidth(450);
        if (ultimaPastaDestino != null) {
            pastaDestino = ultimaPastaDestino;
            txtDestino.setText(pastaDestino.getAbsolutePath());
        }
        listaArquivos.setCellFactory(param -> new ListCell<>() {
            private final ProgressBar barra = new ProgressBar(0);
            private final Label lblNome = new Label();
            private final Label lblPorcentagem = new Label();
            private final Label lblTempo = new Label();
            private final HBox hBox = new HBox(10, lblNome, barra, lblPorcentagem, lblTempo);
            @Override
            protected void updateItem(ConversaoItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    lblNome.setText(item.getNomeArquivo());
                    barra.setProgress(item.getProgresso());
                    lblPorcentagem.setText(String.format("%.0f%%", item.getProgresso() * 100));
                    lblTempo.setText(item.getTempoEstimado());
                    setGraphic(hBox);
                }
            }
        });
        if (ultimaPastaOrigem != null) {
            filaConversao.clear();
            for (File f : Conversor.listarArquivosMKV(ultimaPastaOrigem)) {
                filaConversao.add(new ConversaoItem(f.getName(), f.getAbsolutePath(), null));
            }
            listaArquivos.getItems().setAll(filaConversao);
            txtArquivos.setText(ultimaPastaOrigem.getAbsolutePath());
        }
        // Ativa o botão Converter se já houver arquivos e pasta de destino
        btnConverter.setDisable(pastaDestino == null || filaConversao.isEmpty());
        btnConverter.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        btnCancelar.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        btnCancelar.setDisable(true);
        btnAbrirDestino.setDisable(true);
        VBox root = new VBox(10,
            listaArquivos,
            new HBox(10, btnSelecionarArquivos, txtArquivos),
            new HBox(10, btnSelecionarPasta, txtDestino),
            new HBox(10, btnConverter, btnCancelar),
            btnAbrirDestino,
            lblStatus
        );
        root.setPadding(new Insets(15));
        Scene scene = new Scene(root, 700, 500);
        stage.setTitle("Conversor de Vídeo MKV para MP4");
        stage.setScene(scene);
        stage.show();
    }

    private void carregarPastasSalvas() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
            String origem = props.getProperty("pastaOrigem");
            String destino = props.getProperty("pastaDestino");
            if (origem != null && new File(origem).exists()) {
                ultimaPastaOrigem = new File(origem);
            }
            if (destino != null && new File(destino).exists()) {
                ultimaPastaDestino = new File(destino);
            }
        } catch (IOException ignored) {}
    }

    private void salvarPastas() {
        Properties props = new Properties();
        if (ultimaPastaOrigem != null) props.setProperty("pastaOrigem", ultimaPastaOrigem.getAbsolutePath());
        if (pastaDestino != null) props.setProperty("pastaDestino", pastaDestino.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Pastas usadas no Conversor de Vídeo");
        } catch (IOException ignored) {}
    }

    private void atualizarStatusFfmpeg() {
        Platform.runLater(() -> {
            boolean disponivel = Conversor.ffmpegDisponivel();
            btnConverter.setDisable(!disponivel || pastaDestino == null || filaConversao.isEmpty());
        });
    }

    private void selecionarPastaOrigem(Stage stage) {
    }

    private void selecionarArquivos(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        if (ultimaPastaOrigem != null) fileChooser.setInitialDirectory(ultimaPastaOrigem);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos MKV", "*.mkv"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null && !files.isEmpty()) {
            ultimaPastaOrigem = files.get(0).getParentFile();
            salvarPastas();
            filaConversao.clear();
            for (File f : files) {
                filaConversao.add(new ConversaoItem(f.getName(), f.getAbsolutePath(), null));
            }
            listaArquivos.getItems().setAll(filaConversao);
            txtArquivos.setText(files.size() == 1 ? files.get(0).getAbsolutePath() : ultimaPastaOrigem.getAbsolutePath());
            btnConverter.setDisable(pastaDestino == null || filaConversao.isEmpty());
            atualizarStatusFfmpeg();
        }
    }

    private void selecionarPasta(Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        if (pastaDestino != null) directoryChooser.setInitialDirectory(pastaDestino);
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            pastaDestino = dir;
            txtDestino.setText(pastaDestino.getAbsolutePath());
            salvarPastas();
            btnConverter.setDisable(pastaDestino == null || filaConversao.isEmpty());
            atualizarStatusFfmpeg();
        }
    }

    private void iniciarConversao() {
        if (!Conversor.ffmpegDisponivel()) {
            atualizarStatusFfmpeg();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("FFmpeg não encontrado");
            alert.setHeaderText(null);
            alert.setContentText("FFmpeg não está disponível. Coloque o ffmpeg.exe em 'ffmpeg/bin/' ou adicione ao PATH.");
            alert.showAndWait();
            return;
        }
        btnConverter.setDisable(true);
        btnCancelar.setDisable(false);
        lblStatus.setText("Convertendo...");
        new Thread(() -> {
            for (ConversaoItem item : filaConversao) {
                if (item.getStatus() == StatusConversao.CONCLUIDO) continue;
                item.setStatus(StatusConversao.CONVERTENDO);
                Platform.runLater(listaArquivos::refresh);
                String nomeBase = item.getNomeArquivo().replaceFirst("\\.mkv$", "");
                File destino = new File(pastaDestino, nomeBase + ".mp4");
                item.setCaminhoDestino(destino.getAbsolutePath());
                boolean sucesso = Conversor.converterArquivoComProgresso(item, destino, (progresso, tempoEstimado) -> {
                    item.setProgresso(progresso);
                    item.setTempoEstimado(tempoEstimado);
                    Platform.runLater(listaArquivos::refresh);
                });
                if (sucesso) {
                    item.setStatus(StatusConversao.CONCLUIDO);
                    item.setProgresso(1.0);
                    item.setTempoEstimado("Concluído");
                } else {
                    item.setStatus(StatusConversao.FALHA);
                    item.setTempoEstimado("Falha");
                }
                Platform.runLater(listaArquivos::refresh);
            }
            Platform.runLater(() -> {
                lblStatus.setText("");
                btnConverter.setDisable(false);
                btnAbrirDestino.setDisable(false);
                btnCancelar.setDisable(true);
            });
        }).start();
    }

    private void cancelarConversao() {
        Conversor.solicitarCancelamento();
        lblStatus.setText("Cancelando conversão...");
        btnCancelar.setDisable(true);
    }

    private void abrirPastaDestino() {
        if (pastaDestino != null) {
            try {
                String cmd = "explorer.exe \"" + pastaDestino.getAbsolutePath() + "\"";
                Runtime.getRuntime().exec(cmd);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erro ao abrir pasta");
                alert.setHeaderText(null);
                alert.setContentText("Não foi possível abrir a pasta de destino.");
                alert.showAndWait();
            }
        }
    }
}
