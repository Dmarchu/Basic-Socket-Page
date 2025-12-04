import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Page {
    public static void main(String[] args) {
        String videoPath = "Your path here";

        // Crear un ThreadPool para manejar múltiples clientes simultáneamente
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket ss = new ServerSocket(8080)) {
            System.out.println("Servidor corriendo en: http://localhost:8080");

            while (true) {
                Socket s = ss.accept(); // Aceptar una nueva conexión
                System.out.println("- Cliente conectado");

                // Delegar el manejo de la conexión a un hilo separado
                threadPool.execute(() -> handleClient(s, videoPath));
            }
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleClient(Socket s, String videoPath) {
        try (Scanner sc = new Scanner(s.getInputStream());
             PrintWriter pw = new PrintWriter(s.getOutputStream());
             OutputStream os = s.getOutputStream()) {

            if (!sc.hasNextLine()) return; // Evitar manejar conexiones sin solicitudes

            String request = sc.nextLine();
            if (request.startsWith("GET")) {
                handleRequest(request, videoPath, pw, os, sc);
            }
        } catch (IOException e) {
            System.out.println("Error al procesar la solicitud: " + e.getMessage());
        } finally {
            try {
                s.close(); // Cerrar la conexión al cliente
            } catch (IOException e) {
                System.out.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    private static void handleRequest(String request, String videoPath, PrintWriter pw, OutputStream os, Scanner sc) throws IOException {
        String[] parts = request.split(" ");
        String route = parts[1];

        if (route.equals("/") || route.equals("/index.html")) {
            sendResponse(pw, generateMainPage(videoPath));
        } else if (route.endsWith(".mp4")) {
            serveVideo(videoPath, route, pw, os, sc);
        } else if (route.startsWith("/Temporada")) {
            String temporada = route.replace("/Temporada", "").trim();
            sendResponse(pw, generateSeasonPage(videoPath, temporada));
        } else {
            sendResponse(pw, "<h1>404 - Página no encontrada</h1>");
        }
    }

    private static void serveVideo(String videoPath, String route, PrintWriter pw, OutputStream os, Scanner sc) throws IOException {
        File videoFile = new File(videoPath + route.replace("TemporadaTemporada%20", "Temporada "));
        if (videoFile.exists()) {
            sendVideoFile(videoFile, pw, os, sc);
        } else {
            sendResponse(pw, "<h1>404 - Archivo no encontrado</h1>");
        }
    }

    private static void sendVideoFile(File videoFile, PrintWriter pw, OutputStream os, Scanner sc) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(videoFile, "r")) {
            long fileLength = videoFile.length();
            String rangeHeader = null;

            // Leer encabezados HTTP
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (line.startsWith("Range:")) {
                    rangeHeader = line;
                    break;
                }
                if (line.isEmpty()) {
                    break; // Fin de encabezados
                }
            }

            long start = 0;
            long end = fileLength - 1;

            if (rangeHeader != null) {
                String[] range = rangeHeader.replace("Range: bytes=", "").split("-");
                start = Long.parseLong(range[0]);
                if (range.length > 1 && !range[1].isEmpty()) {
                    end = Long.parseLong(range[1]);
                }
            }

            long contentLength = end - start + 1;
            raf.seek(start);

            // Enviar encabezados HTTP
            pw.println("HTTP/1.1 206 Partial Content");
            pw.println("Content-Type: video/mp4");
            pw.println("Content-Length: " + contentLength);
            pw.println("Accept-Ranges: bytes");
            pw.println("Content-Range: bytes " + start + "-" + end + "/" + fileLength);
            pw.println("Connection: close");
            pw.println();
            pw.flush();

            // Enviar contenido del archivo
            byte[] buffer = new byte[8192]; // 8 KB
            long bytesRemaining = contentLength;
            int bytesRead;
            while (bytesRemaining > 0 && (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) != -1) {
                os.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
            os.flush();
        }
    }

    private static void sendResponse(PrintWriter pw, String content) {
        pw.println("HTTP/1.1 200 OK");
        pw.println("Content-Type: text/html; charset=UTF-8");
        pw.println("Connection: close");
        pw.println();
        pw.println(content);
        pw.flush();
    }

    private static String generateMainPage(String videoPath) {
        File dir = new File(videoPath);
        StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang='es'><head>");
        html.append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<style>")
                .append("body { background-color: black; color: white; font-family: 'Arial', sans-serif; margin: 0; padding: 0; }")
                .append("h1, h2 { color: yellow; text-align: center; margin: 10px; }")
                .append("a { color: yellow; text-decoration: none; transition: color 0.3s ease; }")
                .append("a:hover { color: orange; }")
                .append(".container { width: 90%; max-width: 1200px; margin: 0 auto; padding: 20px; }")
                .append(".menu { background-color: #333; padding: 10px; display: flex; justify-content: space-between; align-items: center; }")
                .append(".menu a { color: white; margin: 0 10px; font-size: 18px; }")
                .append(".menu a:hover { color: orange; }")
                .append(".search-bar { display: flex; justify-content: center; margin: 20px 0; }")
                .append(".search-bar input { width: 80%; max-width: 500px; padding: 10px; border: none; border-radius: 5px; }")
                .append(".search-bar button { padding: 10px 20px; background-color: yellow; border: none; border-radius: 5px; font-size: 16px; cursor: pointer; margin-left: 10px; }")
                .append(".search-bar button:hover { background-color: orange; }")
                .append(".card { background-color: #444; border-radius: 10px; padding: 15px; margin: 10px; color: white; text-align: center; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); transition: transform 0.3s ease; }")
                .append(".card:hover { transform: scale(1.05); }")
                .append(".banner { width: 100%; height: auto; border-radius: 10px; }")
                .append(".button { display: block; width: 200px; margin: 20px auto; padding: 10px 20px; background-color: yellow; color: black; text-align: center; text-decoration: none; font-size: 18px; border-radius: 5px; transition: background-color 0.3s ease; }")
                .append(".button:hover { background-color: orange; }")
                .append(".footer { background-color: #333; text-align: center; padding: 10px; color: white; position: fixed; bottom: 0; width: 100%; }")
                .append("</style>")
                .append("<link rel='icon' type='image/png' href='https://pbs.twimg.com/profile_images/1660460780/Logo_200x200.gif'>")
                .append("<title>Comunidad de Montepinar</title>")
                .append("</head><body>");

        String bannerPath = "https://album.mediaset.es/eimg/2024/11/18/cartel-oficial-lqsa-15_9e2f.jpg?w=1200";

        // Menú de navegación
        html.append("<div class='menu'>")
                .append("<a href='/'>Inicio</a>")
                .append("<a href='#temporadas'>Temporadas</a>")
                .append("<a href='#random'>Capítulo aleatorio</a>")
                .append("</div>");

        // Banner y encabezado
        html.append("<div class='container'>")
                .append("<h1>Comunidad de Montepinar</h1>")
                .append("<img src='").append(bannerPath).append("' class='banner' alt='Banner'>");

        // Barra de búsqueda
        html.append("<div class='search-bar'>")
                .append("<input type='text' placeholder='Buscar capítulo o temporada...' id='searchInput'>")
                .append("<button onclick='search()'>Buscar</button>")
                .append("</div>");

        // Listado de temporadas
        html.append("<h2 id='temporadas'>Temporadas</h2><div class='season-list'>");

        File[] folders = Objects.requireNonNull(dir.listFiles(File::isDirectory));
        Arrays.sort(folders, Comparator.comparing(File::getName, Comparator.comparingInt(Page::extractNumber)));

        for (File folder : folders) {
            String temporada = folder.getName();
            html.append("<div class='card'><a href='/Temporada").append(temporada).append("'>")
                    .append("<h3>").append(temporada).append("</h3>")
                    .append("</a></div>");
        }

        html.append("</div>");

        // Botón de capítulo aleatorio
        html.append("<div id='random'>")
                .append("<a href='").append(getRandomEpisode(videoPath)).append("' class='button'>Reproducir capítulo aleatorio</a>")
                .append("</div>");

        // Pie de página
        html.append("</div><div class='footer'>")
                .append("&copy; 2024 Comunidad de Montepinar - Todos los derechos reservados.")
                .append("</div>");

        // Script para funcionalidad de búsqueda
        html.append("<script>")
                .append("function search() {")
                .append("let query = document.getElementById('searchInput').value.toLowerCase();")
                .append("let cards = document.querySelectorAll('.card');")
                .append("cards.forEach(card => {")
                .append("let text = card.textContent.toLowerCase();")
                .append("if (text.includes(query)) {")
                .append("card.style.display = 'block';")
                .append("} else {")
                .append("card.style.display = 'none';")
                .append("}")
                .append("});")
                .append("}")
                .append("</script>");

        html.append("</body></html>");
        return html.toString();
    }

    private static String getRandomEpisode(String videoPath) {
        File dir = new File(videoPath);
        File[] folders = dir.listFiles(File::isDirectory);

        if (folders == null || folders.length == 0) {
            return null; // No hay temporadas disponibles
        }

        // Seleccionar una temporada al azar
        File randomFolder = folders[new Random().nextInt(folders.length)];
        File[] episodes = randomFolder.listFiles((d, name) -> name.endsWith(".mp4"));

        if (episodes == null || episodes.length == 0) {
            return null; // No hay episodios en esta temporada
        }

        // Seleccionar un episodio al azar
        File randomEpisode = episodes[new Random().nextInt(episodes.length)];

        // Construir la ruta para el episodio seleccionado
        return "/Temporada" + randomFolder.getName() + "/" + randomEpisode.getName();
    }

    // Extraer número de un nombre de temporada
    private static int extractNumber(String name) {
        try {
            return Integer.parseInt(name.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Si no es un número, ponerlo al final
        }
    }

    private static String generateSeasonPage(String videoPath, String temporada) {
        temporada = temporada.replace("%20", " ");
        String path = (videoPath + "/" + temporada);
        File dir = new File(path);

        if (!dir.exists() || !dir.isDirectory()) {
            return "<html><body><h1>Error</h1><p>No se encontró la temporada especificada: "
                    + temporada + "</p><a href='/'>Volver al inicio</a></body></html>";
        }

        StringBuilder html = new StringBuilder("<!DOCTYPE html><html lang='es'><head>");
        html.append("<meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                .append("<style>")
                .append("body { background-color: black; color: white; font-family: 'Arial', sans-serif; margin: 0; padding: 0; }")
                .append("h1, h2 { color: yellow; text-align: center; margin: 10px; }")
                .append("a { color: yellow; text-decoration: none; transition: color 0.3s ease; }")
                .append("a:hover { color: orange; }")
                .append(".container { width: 90%; max-width: 1200px; margin: 0 auto; padding: 20px; }")
                .append(".episode-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-top: 20px; }")
                .append(".card { background-color: #444; border-radius: 10px; padding: 15px; text-align: center; color: white; box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2); transition: transform 0.3s ease; }")
                .append(".card:hover { transform: scale(1.05); }")
                .append(".card img { width: 100%; border-radius: 10px; }")
                .append(".back-button { display: block; width: 200px; margin: 20px auto; padding: 10px 20px; background-color: yellow; color: black; text-align: center; text-decoration: none; font-size: 18px; border-radius: 5px; transition: background-color 0.3s ease; }")
                .append(".back-button:hover { background-color: orange; }")
                .append(".footer { background-color: #333; text-align: center; padding: 10px; color: white; position: fixed; bottom: 0; width: 100%; }")
                .append("</style>")
                .append("<link rel='icon' type='image/png' href='https://pbs.twimg.com/profile_images/1660460780/Logo_200x200.gif'>")
                .append("<title>Temporada ").append(temporada).append(" - Comunidad de Montepinar</title>")
                .append("</head><body>");

        // Encabezado
        html.append("<div class='container'>")
                .append("<h1>Temporada ").append(temporada).append("</h1>");

        // Verificar si hay episodios disponibles
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".mp4"));
        if (files == null || files.length == 0) {
            html.append("<p>No se encontraron episodios en esta temporada.</p>");
        } else {
            // Mostrar episodios en una cuadrícula
            html.append("<div class='episode-grid'>");

            // Ordenar episodios por orden natural
            Arrays.sort(files, Comparator.comparing(File::getName, Comparator.comparingInt(Page::extractNumber)));

            for (File file : files) {
                String episodio = file.getName();
                String videoRoute = "/Temporada" + temporada + "/" + episodio;

                // Generar una imagen de vista previa utilizando ffmpeg (extraer primer fotograma)
                String imagePath = generateThumbnail(file);

                // Crear tarjeta visual para cada episodio
                html.append("<div class='card'>")
                        .append("<a href='").append(videoRoute).append("'>")
                        .append("<img src='/thumbnails/").append(imagePath).append("' alt='").append(episodio).append("'>")
                        .append("<h3>").append(episodio.replace(".mp4", "").replace("Episodio", "Episodio ")).append("</h3>")
                        .append("</a>")
                        .append("</div>");
            }
            html.append("</div>");
        }

        // Botón de regreso al inicio
        html.append("<a href='/' class='back-button'>Volver al inicio</a>")
                .append("</div>");

        // Pie de página
        html.append("<div class='footer'>")
                .append("&copy; 2024 Comunidad de Montepinar - Todos los derechos reservados.")
                .append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    // Método para generar una miniatura a partir de un video usando ffmpeg
    private static String generateThumbnail(File videoFile) {
        try {
            String videoPath = videoFile.getAbsolutePath().replace("\\", "/");
            System.out.println(videoPath);
            String thumbnailDir = "thumbnails"; // Carpeta donde se guardarán las miniaturas
            File dir = new File(thumbnailDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String thumbnailPath = videoPath.split("Episodio")[0] + thumbnailDir + "/" + videoFile.getName().replace(".mp4", ".jpg");
            System.out.println(thumbnailPath);

            // Comando para extraer la primera imagen del video
            String command = String.format("ffmpeg -i \"%s\" -ss 00:01:01.000 -vframes 1 \"%s\"",
                    videoPath, thumbnailPath);

            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            return thumbnailPath;
        } catch (Exception e) {
            e.printStackTrace();
            return "default-thumbnail.jpg"; // Devuelve una miniatura predeterminada en caso de error
        }
    }
}