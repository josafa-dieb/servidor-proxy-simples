package dev.josafa.proxyserver;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/*****************
 * @author josaf
 *****************/
public class Request implements Runnable {
	
	private String url;
	private String path = "/";
	private Socket socket, client;

	/**
	 * 
	 * Aqui nos chamos uma thread na própria instanciacao do objeto isso após a url
	 * e algumas outras coisas serem tratadas, essa thread recebe o próprio objeto e
	 * se mantém viva até o fim da requisição e resposta.
	 * 
	 * @param url    endereço do site
	 * @param client é o socket do client
	 */
	public Request(String url, Socket client) {
		this.url = url.replace("https://", "").replace("http://", "").replace("ftp://", "").replace("udp://", "");
		this.url = url.substring(1, url.length());
		String[] urlDir = this.url.split("/");
		if (urlDir.length > 0) {
			for (int i = 1; i < urlDir.length; i++) {
				path = path + "/" + urlDir[i];
			}
			this.path.replace("//", "/");
			this.url = urlDir[0];
		}
		this.client = client;
		new Thread(this).start();
	}

	/**
	 * <p>
	 * Esse método é uma implementação da Interface Runnable que acontecerá toda a
	 * "mágica" envolvida nos processos de intermediação do servidor pois o server
	 * ira utilizar para fazer a requisição na web se somente se não existir no
	 * cache, caso contrário ele busca no diretorio cache.
	 * </p>
	 * <p>
	 * Se não existir -> ele salva essa página e todo seu conteudo em bytes na pasta
	 * cache, o nome do arquivo salvo sera um hash md5
	 * </p>
	 * <p>
	 * Se existir -> ele apenas recupera o contedúdo apartir do diretório cache e
	 * envia para o client como resposta
	 * </p>
	 * </p>
	 * 
	 * @see Runnable
	 **/
	@Override
	public void run() {
		try {
			this.socket = new Socket(this.url, 80);
			String urlEncoded = Utils.md5(this.url + this.path) + ".cache";
			// se o hash da url não tiver no cache ele vai buscar na web
			// armazernar e depois enviar o arquivo armazenado para o cliente.
			if (!Utils.hasInCache(urlEncoded)) {
				String head = "GET " + this.path + " HTTP/1.1" + "\r\n" + "Host: " + this.url + "\r\n"
						+ "Cache-Control: no-cache" + "\r\n" + "Accept-Encoding: none" + "\r\n"
						+ "Connection: close\r\n" + "\r\n";
				this.socket.getOutputStream().write(head.getBytes());
				this.socket.getOutputStream().flush();
				InputStream socketInputStream = this.socket.getInputStream();
				Files.write(Paths.get("cache/" + urlEncoded), socketInputStream.readAllBytes(),
						StandardOpenOption.CREATE_NEW);
				Optional<String> isHtmlfile = Files.readAllLines(Paths.get("cache/" + urlEncoded)).stream()
						.filter(f -> f.contains("text/html")).findFirst();
				if (isHtmlfile.isPresent()) {
					Utils.addInfoBox(urlEncoded);
				}
			}
			if (this.client != null && this.socket != null && this.client.isConnected() & this.socket.isConnected()) {
				this.sendPage();
				this.client.close();
				this.socket.close();
			}
		} catch (IOException e) {
		}
	}

	// Envia a página para o cliente.
	// recuperando os bytes dos arquivos de cache
	// tratando e enviando para o client HTTP
	private void sendPage() {
		if (this.client != null && this.client.isConnected()) {
			String urlEncoded = Utils.md5(this.url + this.path) + ".cache";
			try {
				client.getOutputStream().write(Files.readAllBytes(Paths.get("cache/" + urlEncoded)));
				client.getOutputStream().flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
