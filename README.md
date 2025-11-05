# Conversor de Vídeo MKV para MP4

Este projeto é um conversor de vídeos MKV para MP4 com interface gráfica em Java (JavaFX). Permite selecionar arquivos ou pastas, exibe uma fila de conversão com barra de progresso, porcentagem e tempo estimado para cada vídeo, e utiliza o FFmpeg para realizar as conversões.

## Funcionalidades
- Seleção de arquivos MKV individuais ou por pasta
- Fila de conversão com barra de progresso, porcentagem e tempo estimado
- Conversão sequencial: cada vídeo é convertido e liberado na pasta de destino antes do próximo
- Armazenamento automático das últimas pastas de origem e destino usadas
- Detecção automática do FFmpeg (local ou via PATH)
- Botão para abrir a pasta de destino diretamente
- Cancelamento da conversão em andamento

## Requisitos
- Java 17 ou superior
- FFmpeg (executável ffmpeg.exe)
  - Recomenda-se colocar o ffmpeg em `ffmpeg/bin/ffmpeg.exe` dentro do projeto
  - Alternativamente, pode estar disponível no PATH do sistema

## Como usar
1. **Instale o Java 17+**
2. **Coloque o ffmpeg.exe em `ffmpeg/bin/`** (ou configure o PATH)
3. **Compile e execute o projeto**:
   - Via IntelliJ: Abra o projeto e execute `Main.java`
   - Via terminal:
     ```
     mvn clean install
     mvn javafx:run
     ```
4. **Selecione arquivos ou pasta de origem**
5. **Selecione a pasta de destino**
6. **Clique em "Converter"**
7. Acompanhe o progresso na interface. Ao final, abra a pasta de destino pelo botão dedicado.

## Configuração automática de pastas
O software salva as últimas pastas de origem e destino usadas em `config.properties` na raiz do projeto. Ao abrir novamente, essas pastas serão sugeridas automaticamente.

## Estrutura do projeto
```
conversor-video/
├── src/main/java/conversorvideo/
│   ├── Main.java
│   ├── MainView.java
│   ├── Conversor.java
│   ├── ConversaoItem.java
│   └── StatusConversao.java
├── ffmpeg/bin/ffmpeg.exe
├── config.properties
├── pom.xml
└── README.md
```

## Observações
- O FFmpeg é necessário para conversão. Baixe em: https://ffmpeg.org/download.html
- O JavaFX não está incluído no JDK 17+, mas é gerenciado automaticamente pelo Maven (veja o `pom.xml`).
- O software é multiplataforma, mas o caminho do ffmpeg pode variar em sistemas não Windows.

## Licença
Este projeto é distribuído sob a licença MIT.

