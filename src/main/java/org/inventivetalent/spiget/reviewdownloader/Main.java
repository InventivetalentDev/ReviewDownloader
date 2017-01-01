package org.inventivetalent.spiget.reviewdownloader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class Main {

	static File out = new File("reviews.json");

	public static void main(String[] args) throws IOException {
		if (out.exists()) {
			out.delete();
		}
		out.createNewFile();

		int page = 0;
		if (args.length > 0) {
			try {
				page = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Failed to parse page argument");
			}
		}
		downloadPage(page);
	}

	static boolean downloadPage(int page) throws IOException {
		System.out.println("Downloading page #" + page);

		URL url = new URL("http://api.spiget.org/v2/reviews?fields=message&pretty=false&size=1000&page=" + page);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.addRequestProperty("User-Agent", "SpigetReviewSimulator");

		String string = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null)
				string += line;
		}

		JsonArray reviewArray = new JsonParser().parse(string).getAsJsonArray();

		try (FileWriter fileWriter = new FileWriter(out, true)) {
			JsonWriter writer = new JsonWriter(fileWriter);

			writer.beginArray();

			for (JsonElement element : reviewArray) {
				String message = ((JsonObject) element).get("message").getAsString();
				message = new String(Base64.getDecoder().decode(message));
				message = Jsoup.parse(message).text();//Strip html tags

				writer.value(message);
			}

			writer.endArray();
		}

		System.out.println("There are a total of " + connection.getHeaderFieldInt("X-Page-Count", 1) + " pages available.");

		// Return true if there are more pages left to download
		return connection.getHeaderFieldInt("X-Page-Index", page) < connection.getHeaderFieldInt("X-Page-Count", 1);
	}

}
