package dev.josafa.proxyserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Service implements Runnable {

	// variaveis estáticas da classe
	public static Thread TASK;
	private static ServerSocket SERVER;
	private static final int PORT;
	private static boolean RUNNING;

	// na inicialização são setados alguns valores padrões para as variaves globais
	// estáticas
	static {
		PORT = 8888;
		TASK = null;
		SERVER = null;
		RUNNING = true;
	}

	// esse método sera chamado ao chamar o start();
	@Override
	public void run() {
		if (SERVER != null) {
			System.out.println("Servidor já inicializado.");
			return;
		}
		try {
			SERVER = new ServerSocket(Service.PORT);
			System.out.println();
			System.out.println(" [Diogenes] Servidor proxy iniciado na porta " + PORT);
			System.out.println(" [Diogenes] Versão: 1.0.0 ");
			System.out.println();
			while (RUNNING) {
				Socket client = SERVER.accept();
				client.setKeepAlive(true);
				String[] clientHead = getHead(client);
				if (clientHead == null || !isHTTPClient(clientHead)) {
					client.close();
				} else {
					System.out.println(getProtocol(clientHead) + " => " + client);
					String url = getUrl(clientHead);
					new Request(url, client);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Erro na inicialização do servidor");
		}

	}

	// Pega as informações do header do client que se conectou com server proxy
	private static String[] getHead(Socket client) {
		String[] firstLine = null;
		try {
			String c = new BufferedReader(new InputStreamReader(client.getInputStream())).readLine();
			if (c != null) {
				firstLine = c.split(" ");
			}
		} catch (IOException e) {
			throw new RuntimeException("Erro no buffer de dados.");
		}
		return firstLine;
	}

	// verifica se o header é valido
	private static boolean isValid(String[] head) {
		if (head.length != 3) {
			return false;
		}
		return true;
	}

	// retorna o protocolo de conexão do cliente
	private static String getProtocol(String[] head) {
		if (isValid(head))
			return head[2];
		return null;
	}

	// pega a url que o cliente digitou
	private static String getUrl(String[] head) {
		if (isValid(head))
			return head[1];
		return null;
	}

	// verifica se o cliente é esta acessando sobre protocolo HTTP
	private static boolean isHTTPClient(String[] head) {
		if (!isValid(head))
			return false;
		String protocol = head[2];
		if (protocol.equalsIgnoreCase("HTTP/1.1")) {
			return true;
		}
		return false;
	}

	// incia um ciclo de vida de uma thread para o servidor.
	// juntamente com uma task para verificar o cache a cada 120 minutos e excluir
	// arquivos expirados.
	public synchronized void start(long time) {
		TASK = new Thread(this);
		TASK.start();
		new Thread(new Runnable() {
			public void run() {
				while (Service.RUNNING) {
					try {
						System.out.println("Atualizando cache!");
						Utils.removeExpiredCache(time);
						Thread.sleep(time * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

}
