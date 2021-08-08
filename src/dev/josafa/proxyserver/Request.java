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
	 * Aqui nos chamos uma thread na pr�pria instanciacao do objeto isso ap�s a url
	 * e algumas outras coisas serem tratadas, essa thread recebe o pr�prio objeto e
	 * se mant�m viva at� o fim da requisi��o e resposta.
	 * 
	 * @param url    endere�o do site
	 * @param client � o socket do client
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
	 * Esse m�todo � uma implementa��o da Interface Runnable que acontecer� toda a
	 * "m�gica" envolvida nos processos de intermedia��o do servidor pois o server
	 * ira utilizar para fazer a requisi��o na web se somente se n�o existir no
	 * cache, caso contr�rio ele busca no diretorio cache.
	 * </p>
	 * <p>
	 * Se n�o existir -> ele salva essa p�gina e todo seu conteudo em bytes na pasta
	 * cache, o nome do arquivo salvo sera um hash md5
	 * </p>
	 * <p>
	 * Se existir -> ele apenas recupera o conted�do apartir do diret�rio cache e
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
			// se o hash da url n�o tiver no cache ele vai buscar na web
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

	// Envia a p�gina para o cliente.
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
