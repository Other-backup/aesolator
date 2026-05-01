package com.winlator.cmod.contents;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;

public class Downloader {
    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS = 120000;
    private static final int MAX_RETRIES = 4;
    private static final int BUFFER_SIZE = 64 * 1024;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Ae.solator/ContentsDownloader";

    public static final class StringResponse {
        public final String url;
        public final int statusCode;
        public final String body;
        public final String errorBody;
        public final Exception exception;

        StringResponse(String url, int statusCode, String body, String errorBody, Exception exception) {
            this.url = url == null ? "" : url;
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
            this.errorBody = errorBody == null ? "" : errorBody;
            this.exception = exception;
        }

        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300 && !body.trim().isEmpty();
        }

        public boolean isGitHubRateLimited() {
            String normalizedUrl = url.trim().toLowerCase(Locale.US);
            String normalizedError = errorBody.trim().toLowerCase(Locale.US);
            return statusCode == HttpURLConnection.HTTP_FORBIDDEN
                    && normalizedUrl.contains("api.github.com")
                    && (normalizedError.isEmpty()
                    || normalizedError.contains("rate limit")
                    || normalizedError.contains("secondary rate limit"));
        }

        public String classifyFailure() {
            if (isGitHubRateLimited()) return "github_rate_limited";
            if (statusCode > 0) return "http_" + statusCode;
            if (exception != null) return "io_exception";
            if (body.trim().isEmpty()) return "empty_body";
            return "";
        }
    }

    public static boolean downloadFile(String address, File file) {
        if (address == null || address.trim().isEmpty() || file == null) return false;
        String normalizedAddress = address.trim();
        File partFile = new File(file.getAbsolutePath() + ".part");
        File metaFile = new File(file.getAbsolutePath() + ".part.meta");
        DownloadMeta meta = readMeta(metaFile);
        if (meta == null || !normalizedAddress.equals(meta.url)) {
            safeDelete(partFile);
            safeDelete(metaFile);
            meta = new DownloadMeta(normalizedAddress, "", "");
        }

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            long resumeFrom = partFile.isFile() ? partFile.length() : 0L;
            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;
            try {
                connection = openConnection(normalizedAddress);
                if (resumeFrom > 0L) {
                    connection.setRequestProperty("Range", "bytes=" + resumeFrom + "-");
                    String ifRange = !meta.etag.isEmpty() ? meta.etag : meta.lastModified;
                    if (!ifRange.isEmpty()) connection.setRequestProperty("If-Range", ifRange);
                }
                int responseCode = connection.getResponseCode();
                boolean partialResponse = responseCode == HttpURLConnection.HTTP_PARTIAL;
                boolean okResponse = responseCode == HttpURLConnection.HTTP_OK;
                if (!partialResponse && !okResponse) {
                    if (responseCode == HttpURLConnection.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE
                            && resumeFrom > 0L
                            && finalizeDownload(partFile, file)) {
                        safeDelete(metaFile);
                        return true;
                    }
                    continue;
                }

                if (resumeFrom > 0L && !partialResponse) {
                    // Server ignored range or source changed; restart clean.
                    safeDelete(partFile);
                    safeDelete(metaFile);
                    meta = new DownloadMeta(normalizedAddress, "", "");
                    continue;
                }

                String responseEtag = valueOrEmpty(connection.getHeaderField("ETag"));
                String responseLastModified = valueOrEmpty(connection.getHeaderField("Last-Modified"));
                if (!responseEtag.isEmpty() || !responseLastModified.isEmpty()) {
                    meta = new DownloadMeta(normalizedAddress, responseEtag, responseLastModified);
                    writeMeta(metaFile, meta);
                }

                input = connection.getInputStream();
                output = new FileOutputStream(partFile, partialResponse && resumeFrom > 0L);
                byte[] data = new byte[BUFFER_SIZE];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }
                output.flush();

                long expectedTotalLength = resolveExpectedTotalLength(connection, partialResponse, resumeFrom);
                if (expectedTotalLength > 0L && partFile.length() < expectedTotalLength) {
                    // Interrupted/short read, retry and continue from part.
                    continue;
                }

                if (!finalizeDownload(partFile, file)) {
                    continue;
                }
                safeDelete(metaFile);
                return true;
            } catch (Exception ignored) {
            } finally {
                closeQuietly(output);
                closeQuietly(input);
                if (connection != null) connection.disconnect();
            }
        }
        if (partFile.isFile() && partFile.length() > 0L && finalizeDownload(partFile, file)) {
            safeDelete(metaFile);
            return true;
        }
        return false;
    }

    public static String downloadString(String address) {
        StringResponse response = downloadStringResponse(address);
        return response.isSuccessful() ? response.body : null;
    }

    public static StringResponse downloadStringResponse(String address) {
        HttpURLConnection connection = null;
        InputStream input = null;
        InputStream error = null;
        BufferedReader reader = null;
        try {
            connection = openConnection(address);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                error = connection.getErrorStream();
                String errorBody = readFully(error);
                System.err.println("HTTP " + responseCode + " for " + address
                        + (errorBody.isEmpty() ? "" : " :: " + errorBody));
                return new StringResponse(address, responseCode, "", errorBody, null);
            }

            input = connection.getInputStream();
            return new StringResponse(address, responseCode, readFully(input), "", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new StringResponse(address, 0, "", "", e);
        } finally {
            closeQuietly(reader);
            closeQuietly(error);
            closeQuietly(input);
            if (connection != null) connection.disconnect();
        }
    }

    public static String sha256Hex(File file) {
        if (file == null || !file.isFile()) return "";
        InputStream input = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            input = new FileInputStream(file);
            byte[] data = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(data)) != -1) {
                digest.update(data, 0, count);
            }
            byte[] out = digest.digest();
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format(Locale.US, "%02x", b));
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        } finally {
            closeQuietly(input);
        }
    }

    private static HttpURLConnection openConnection(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(address)).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept-Encoding", "identity");
        connection.setRequestProperty("Connection", "close");
        applySourceHeaders(connection, address);
        return connection;
    }

    private static void applySourceHeaders(HttpURLConnection connection, String address) {
        String host = "";
        try {
            URI uri = new URI(address);
            host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.US);
        } catch (Exception ignored) {
        }

        if (host.contains("api.github.com")) {
            connection.setRequestProperty("Accept", "application/vnd.github+json, application/json;q=0.9, */*;q=0.8");
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            connection.setRequestProperty("Referer", "https://github.com/");
            return;
        }

        if (host.contains("raw.githubusercontent.com")
                || host.contains("githubusercontent.com")
                || host.contains("github.com")) {
            connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        }
    }

    private static long resolveExpectedTotalLength(HttpURLConnection connection, boolean partialResponse, long resumeFrom) {
        if (partialResponse) {
            String contentRange = connection.getHeaderField("Content-Range");
            long totalFromRange = parseTotalFromContentRange(contentRange);
            if (totalFromRange > 0L) return totalFromRange;
            long chunkLength = connection.getContentLengthLong();
            if (chunkLength > 0L) return resumeFrom + chunkLength;
            return -1L;
        }
        return connection.getContentLengthLong();
    }

    private static long parseTotalFromContentRange(String contentRange) {
        if (contentRange == null || contentRange.trim().isEmpty()) return -1L;
        String normalized = contentRange.trim().toLowerCase(Locale.US);
        int slash = normalized.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= normalized.length()) return -1L;
        String total = normalized.substring(slash + 1).trim();
        if (total.equals("*")) return -1L;
        try {
            return Long.parseLong(total);
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private static boolean finalizeDownload(File partFile, File outputFile) {
        if (partFile == null || !partFile.isFile()) return false;
        if (outputFile.exists() && !outputFile.delete()) return false;
        if (partFile.renameTo(outputFile)) return true;
        if (!copyFile(partFile, outputFile)) return false;
        safeDelete(partFile);
        return true;
    }

    private static boolean copyFile(File source, File destination) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(destination);
            byte[] data = new byte[BUFFER_SIZE];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            return true;
        } catch (IOException ignored) {
            return false;
        } finally {
            closeQuietly(output);
            closeQuietly(input);
        }
    }

    private static String readFully(InputStream input) throws IOException {
        if (input == null) return "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            closeQuietly(reader);
        }
    }

    private static DownloadMeta readMeta(File metaFile) {
        if (metaFile == null || !metaFile.isFile()) return null;
        InputStream input = null;
        try {
            input = new FileInputStream(metaFile);
            Properties properties = new Properties();
            properties.load(input);
            String url = valueOrEmpty(properties.getProperty("url"));
            String etag = valueOrEmpty(properties.getProperty("etag"));
            String lastModified = valueOrEmpty(properties.getProperty("lastModified"));
            if (url.isEmpty()) return null;
            return new DownloadMeta(url, etag, lastModified);
        } catch (IOException ignored) {
            return null;
        } finally {
            closeQuietly(input);
        }
    }

    private static void writeMeta(File metaFile, DownloadMeta meta) {
        if (metaFile == null || meta == null || meta.url.isEmpty()) return;
        OutputStream output = null;
        try {
            output = new FileOutputStream(metaFile);
            Properties properties = new Properties();
            properties.setProperty("url", meta.url);
            if (!meta.etag.isEmpty()) properties.setProperty("etag", meta.etag);
            if (!meta.lastModified.isEmpty()) properties.setProperty("lastModified", meta.lastModified);
            properties.store(output, "Ae.solator contents partial download metadata");
        } catch (IOException ignored) {
        } finally {
            closeQuietly(output);
        }
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static void safeDelete(File file) {
        if (file != null && file.exists()) file.delete();
    }

    private static void closeQuietly(InputStream input) {
        if (input == null) return;
        try {
            input.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(BufferedReader reader) {
        if (reader == null) return;
        try {
            reader.close();
        } catch (IOException ignored) {
        }
    }

    private static void closeQuietly(OutputStream output) {
        if (output == null) return;
        try {
            output.close();
        } catch (IOException ignored) {
        }
    }

    private static final class DownloadMeta {
        final String url;
        final String etag;
        final String lastModified;

        DownloadMeta(String url, String etag, String lastModified) {
            this.url = valueOrEmpty(url);
            this.etag = valueOrEmpty(etag);
            this.lastModified = valueOrEmpty(lastModified);
        }
    }
}
