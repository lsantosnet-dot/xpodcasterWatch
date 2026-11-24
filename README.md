# XPodcaster Watch

App de Galaxy Watch / Wear OS que recebe as **notícias do Perplexity** do
[XPodcaster](https://github.com/lsantosnet-dot/XPodcaster) do celular e as guarda no relógio
para ler e ouvir sem o celular por perto.

## O que faz

- **Recebe do celular**: no XPodcaster, aba Perplexity, toque em **Enviar para o relógio**.
  A lista inteira vai para o relógio e substitui a que estava lá.
- **Lista de cards**: posição, data e título de cada notícia. Toque num card para abrir o
  **detalhe** com o texto completo.
- **Ouvir em voz alta**: **Ouvir tudo** lê as notícias uma depois da outra (TextToSpeech do relógio),
  anunciando "Notícia 2 de 5." antes de cada uma e trocando a voz para o idioma da notícia.
  No detalhe, **Ouvir a partir daqui** começa daquela notícia. Tela de reprodução com anterior,
  tocar/pausar, próxima e anel de progresso.
- **Continuar de onde parou**: o relógio guarda a última notícia lida ou ouvida (em
  `resume.json`, sobrevive ao app ser fechado). No topo da lista aparece **Continuar ouvindo**
  (volta na frase onde parou; depois de 2 minutos parado, volta uma frase) ou **Continuar lendo**
  (abre no parágrafo onde estava). Se a notícia foi até o fim, o chip aponta para a próxima.
  A posição é guardada pelo id da notícia: se uma lista nova do celular ainda tiver aquela notícia,
  o chip continua valendo. A pausa na tela de reprodução também retoma na mesma frase.
- **Lembrete da próxima notícia**: de tempos em tempos o relógio mostra uma notificação com a
  próxima notícia para ler (a mesma do "Continuar"). Tocar abre a notícia; **Lida** marca sem abrir;
  **Ouvir** toca em voz alta. Ao ler até o fim (ou ouvir até o fim), ela conta como lida, a
  notificação some e o marcador passa para a seguinte. Depois da última da lista, não avisa mais até
  chegarem notícias novas.
  O intervalo (15 min a 6 h) e o horário de silêncio (ex.: 22h às 8h) ficam em **Notificações**, no
  fim da lista, e valem na hora, sem precisar de outro build. **Testar agora** mostra a notificação
  na hora.
- **Limpar tudo**: no fim da lista, com confirmação. Para ter as notícias de novo, envie outra vez
  pelo celular.

Não há sincronização de lido/não lido com o celular nem exclusão de uma notícia por vez.

## Como o celular acha o relógio

Mesmo esquema do Offline Reader Watch, com insistência (veja `WatchBridge.kt` no XPodcaster):

1. capability `xpodcaster_watch`, declarada em `wear/src/main/res/values/wear.xml`;
2. relógio que já se anunciou antes (DataItem `/xpodcaster/watch` ou resposta a ping);
3. se nada disso responder, o celular manda um **ping** para todos os nós conectados e usa
   quem responder **pong**. Repete por até 15 segundos.

| Caminho | Sentido | Tipo | Conteúdo |
|---|---|---|---|
| `/xpodcaster/perplexity/news` | celular → relógio | ChannelClient | JSON com a lista completa |
| `/xpodcaster/perplexity/received` | relógio → celular | Message | quantidade recebida (confirmação) |
| `/xpodcaster/ping` / `/xpodcaster/pong` | ida e volta | Message | "você está aí?" / quantidade guardada |
| `/xpodcaster/watch` | relógio → celular | DataItem | anúncio do app instalado |

### Assinatura (importante)

A Data Layer só conecta apps com o **mesmo `applicationId` (`com.xpodcaster.xpodcaster`)** e a
**mesma assinatura**. Os dois repositórios têm a mesma chave de debug
(`keystore/xpodcaster-debug.keystore` aqui, `android/keystore/xpodcaster-debug.keystore` no
XPodcaster). Ela não é segredo: serve só para os dois builds baterem.

Se o XPodcaster já estava instalado no celular com outra chave, **desinstale-o uma vez** antes de
instalar o build novo (o Android recusa a atualização com assinatura diferente).

## Instalar

APK do relógio (build mais recente da `main`):
https://github.com/lsantosnet-dot/xpodcasterWatch/releases/latest/download/XPodcasterWatch-relogio.apk

Ou compile:

```bash
./gradlew :wear:assembleDebug
# ative a Depuração por Wi-Fi nas Opções do desenvolvedor do relógio
adb connect <ip-do-relogio>:<porta>
adb -s <ip-do-relogio>:<porta> install wear/build/outputs/apk/debug/wear-debug.apk
```

## Limitações

- O lembrete usa o WorkManager: o Android pode atrasar alguns minutos para economizar bateria.
  O modo Hora de dormir / Não perturbe do relógio também silencia a notificação.
- A leitura em voz alta para se o app do relógio for fechado.
- O relógio precisa ter uma voz de TTS instalada (Samsung TTS ou Google). Sem a voz do idioma da
  notícia, lê com a voz em português.
