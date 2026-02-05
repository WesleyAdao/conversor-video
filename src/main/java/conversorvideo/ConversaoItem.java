package conversorvideo;

public class ConversaoItem {
    private String nomeArquivo;
    private String caminhoOrigem;
    private String caminhoDestino;
    private double progresso; // 0.0 a 1.0
    private String tempoEstimado;
    private StatusConversao status;

    public ConversaoItem(String nomeArquivo, String caminhoOrigem, String caminhoDestino) {
        this.nomeArquivo = nomeArquivo;
        this.caminhoOrigem = caminhoOrigem;
        this.caminhoDestino = caminhoDestino;
        this.progresso = 0.0;
        this.tempoEstimado = "--";
        this.status = StatusConversao.DISPONIVEL;
    }

    public String getNomeArquivo() { return nomeArquivo; }
    public String getCaminhoOrigem() { return caminhoOrigem; }
    public String getCaminhoDestino() { return caminhoDestino; }
    public void setCaminhoDestino(String caminhoDestino) { this.caminhoDestino = caminhoDestino; }
    public double getProgresso() { return progresso; }
    public void setProgresso(double progresso) { this.progresso = progresso; }
    public String getTempoEstimado() { return tempoEstimado; }
    public void setTempoEstimado(String tempoEstimado) { this.tempoEstimado = tempoEstimado; }
    public StatusConversao getStatus() { return status; }
    public void setStatus(StatusConversao status) { this.status = status; }
}
