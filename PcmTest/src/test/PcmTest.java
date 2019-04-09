package test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public final class PcmTest extends JavaPlugin implements Listener {

	private final String path = Bukkit.getPluginManager().getPlugin("PcmEvent").getDataFolder().getPath();

	@Override
	public final void onEnable() {

		getServer().getPluginManager().registerEvents(this, this);

	}

	@EventHandler
	public final void onCommand(AsyncPlayerChatEvent event) {

		String[] args = event.getMessage().split(" ");
		Player player = event.getPlayer();

		if (args[0].equals("path") && player.getName().equals("CactusCata") && args.length >= 3) {

			try {
				player.sendMessage(method(args));
			} catch (NullPointerException e) {
				player.sendMessage("null");
			} catch (IOException e) {
				player.sendMessage("erreur");
			}

			event.setCancelled(true);

		}
	}

	private String method(String[] args) throws IOException, NullPointerException {
		File folder = new File(this.path, args[2]);

		switch (args[1]) {
		case "see":
			File[] files = folder.listFiles();

			StringBuilder build = new StringBuilder("--> " + folder.getName() + "\n");
			for (File file : files) {
				build.append((file.isFile() ? "FILE:" : "FOLDER:") + " " + file.getName() + "\n");
			}

			return "§e" + build.toString();

		case "delete":

			if (folder.isFile())
				folder.delete();
			else
				FileUtils.deleteDirectory(folder);
			return "ok";

		case "copy":

			if (folder.isFile())
				Files.copy(folder, new File(this.path, folder.getName()));
			else {
				File finalPath = new File(this.path + "/zip");
				Zip.compress(folder, finalPath);

			}

			return "ok";
		}
		return "what";

	}

	private static class Zip {

		// Remplace l'extension si le fichier cible ne fini pas par '.zip'
		private static File getZipTypeFile(final File source, final File target) throws IOException {
			if (target.getName().toLowerCase().endsWith(".zip"))
				return target;
			final String tName = target.isDirectory() ? source.getName() : target.getName();
			final int index = tName.lastIndexOf('.');
			return new File(new StringBuilder(
					target.isDirectory() ? target.getCanonicalPath() : target.getParentFile().getCanonicalPath())
							.append(File.separatorChar).append(index < 0 ? tName : tName.substring(0, index))
							.append(".zip").toString());
		}

		// Compresse un fichier
		private final static void compressFile(final ZipOutputStream out, final String parentFolder, final File file)
				throws IOException {
			final String zipName = new StringBuilder(parentFolder).append(file.getName())
					.append(file.isDirectory() ? '/' : "").toString();

			// Définition des attributs du fichier
			final ZipEntry entry = new ZipEntry(zipName);
			entry.setSize(file.length());
			entry.setTime(file.lastModified());
			out.putNextEntry(entry);

			// Traitement récursif s'il s'agit d'un répertoire
			if (file.isDirectory()) {
				for (final File f : file.listFiles())
					compressFile(out, zipName.toString(), f);
				return;
			}

			// Ecriture du fichier dans le zip
			final InputStream in = new BufferedInputStream(new FileInputStream(file));
			try {
				final byte[] buf = new byte[8192];
				int bytesRead;
				while (-1 != (bytesRead = in.read(buf)))
					out.write(buf, 0, bytesRead);
			} finally {
				in.close();
			}
		}

		// Compresse un fichier à l'adresse pointée par le fichier cible.
		// Remplace le fichier cible s'il existe déjà.
		public static void compress(final File file, final File target) throws IOException {
			final File source = file.getCanonicalFile();

			// Création du fichier zip
			final ZipOutputStream out = new ZipOutputStream(
					new FileOutputStream(getZipTypeFile(source, target.getCanonicalFile())));
			out.setMethod(ZipOutputStream.DEFLATED);
			out.setLevel(Deflater.BEST_COMPRESSION);

			// Ajout du(es) fichier(s) au zip
			compressFile(out, "", source);
			out.close();
		}
	}
}
