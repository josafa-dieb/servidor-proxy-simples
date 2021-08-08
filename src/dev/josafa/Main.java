package dev.josafa;

import dev.josafa.proxyserver.Service;

/*****************
 * @author josaf
 *****************/
public class Main {
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("Use: java -jar diogenes.jar <cache-time-in-secounds>");
			return;
		}
		//por padrão caso aconteca algum problema vou deixar 120 segundos
		long time = 120;
		try {
			time = Long.parseLong(args[0]);
		} catch (Exception e) {
			System.out.println("Use números para o tempo de limpeza do cache.");
		}
		new Service().start(time);
	}
}
