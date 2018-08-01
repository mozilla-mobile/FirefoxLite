package org.mozilla.httprequest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class HttpRequest {

    public static String get(URL url, final String userAgent) {

        String line = "";
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", userAgent);

            line = readLines(urlConnection);
        } catch (IOException ignored) {

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return line;
    }

    private static String readLines(URLConnection connection) throws IOException {
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IndexOutOfBoundsException ignored) {
            // IndexOutOfBoundsException sometimes is thrown by the okHttp library
            // bundled within the android framework, we can only catch the exception here,
            // or use the latest okHttp3.
            return "";
        }

        StringBuilder total = new StringBuilder();
        try (BufferedReader bufferedReader = createReader(inputStream)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                total.append(line).append('\n');
            }
        }

        return total.toString();
    }

    private static BufferedReader createReader(InputStream stream) throws IOException {
        InputStreamReader reader = new InputStreamReader(new BufferedInputStream(stream), "utf-8");
        return new BufferedReader(reader);
    }
}
