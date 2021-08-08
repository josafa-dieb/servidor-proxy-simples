package dev.josafa.proxyserver;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * <p>
 * Essa classe fica todas os métodos utilizáveis globalmente, todos os que
 * server de utilidades.
 * </p>
 * 
 * @author josaf
 **/
public class Utils {
	/**
	 * @param s um valor do tipo string
	 * @return hash md5 da string <b>s</b>
	 **/
	public static String md5(String s) {
		MessageDigest m;
		try {
			m = MessageDigest.getInstance("MD5");
			m.update(s.getBytes(), 0, s.length());
			String encoded = new BigInteger(1, m.digest()).toString(16);
			return encoded;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return s;
	}

	// O conteudo da caixa de informações
	// aqui eu tomei a liberdade de colocar o nome da minha aplicação
	public static String infoBox() {
		return "<p style=\"z-index:9999; position:fixed; top:20px; left:20px; width:200px;"
				+ "height:100px; background-color:yellow; padding:10px; font-weight:bold;\">Diógenes Web Server Cache: "
				+ Utils.getFormatedDate() + "</p>";
	}

	/**
	 * 
	 * Adiciona o box com informações da página em cache alterando o tamanho do
	 * conteúdo e adicionando a informação.
	 * 
	 **/
	public static void addInfoBox(String urlEncoded) throws IOException {
		Path path = Paths.get("cache/" + urlEncoded);
		List<String> lines = Files.readAllLines(path);
		int lineAddNewcontent = 0, endchk = 0, endchk2 = 0, lineAddInfoBox = 0;
		for (String line : lines) {
			if (!line.startsWith("Content-Length:") && endchk == 0) {
				lineAddNewcontent += 1;
			} else {
				endchk = 1;
			}
			if (!line.contains("<body") && endchk2 == 0) {
				lineAddInfoBox++;
			} else {
				endchk2 = 1;
			}
		}
		// tamanho atual do conteudo
		int sizeContent = Integer.parseInt(
				lines.get(lineAddNewcontent).split(":")[1].replace(" ", "").replace("\r", "").replace("\n", ""))
				+ Utils.infoBox().getBytes().length;
		// tamanho do conteudo acrescentando o tamanho do conteudo extra fixado com dada
		// do cache.
		lines.add(lineAddInfoBox + 1, Utils.infoBox());
		String newContent = "Content-Length: " + sizeContent;
		lines.remove(lineAddNewcontent);
		lines.add(lineAddNewcontent, newContent);
		Files.write(path, lines);
	}

	// criado com a função de facilitar o formatado de data para enviar para o
	// servidor e usar o InfoBox
	public static String getFormatedDate() {
		SimpleDateFormat simpleFormat = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss", Locale.ENGLISH);
		simpleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date data = new Date();
		return simpleFormat.format(data) + " GMT";
	}

	// verifica se o arquivo existe no cache
	// no caso o file name seria o hash md5 da url do site
	public static boolean hasInCache(String fileName) {
		try (Stream<Path> stream = Files.list(Paths.get("./cache"))) {
			return stream.filter(f -> f.getFileName().toString().equals(fileName)).findFirst().isPresent();
		} catch (IOException e) {
			return false;
		}
	}

	// remove o cache
	public static void removeExpiredCache(long secounds) {
		try {
			Stream<Path> files = Files.list(Paths.get("cache/"));
			files.forEach(f -> {
				try {
					BasicFileAttributes b = Files.readAttributes(f, BasicFileAttributes.class);
					FileTime ftime = b.creationTime();
					long lastTime = ftime.toMillis();
					ftime.to(TimeUnit.MINUTES);
					long currentTime = System.currentTimeMillis();
					long time = ((currentTime - lastTime) / 1000);
					if (time >= secounds) {
						Files.delete(f);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			files.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
