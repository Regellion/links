import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Main {
    private static Set<String> uniqueURL = new HashSet<>();
   // private static final String siteURL = "https://skillbox.ru/";
    private static final String filePath = "src\\main\\resources\\map.txt";
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите адрес сайта: ");
        String siteURL = scanner.nextLine();
        //Однопоточный режим
        //getLinks(siteURL, "");
        //uniqueURL.forEach(System.out::println);
        //System.out.println(uniqueURL.size());


        // Многопоточный режим
        WalkLink walkLink = new WalkLink(siteURL, "", siteURL);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(walkLink);
        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Записываем в файл
        try {
            walkLink.writer(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("End");
    }

    private static void getLinks(String pageUrl, String tab) {

        // добавляем введеную ссылку
        uniqueURL.add(pageUrl);
        System.out.println(tab + pageUrl);
        // Если эта ссылка ведет на файл то прерываем метод
        if (!pageUrl.endsWith("/")) {
            return;
        }
        Document document = null;
        try {
            Thread.sleep(200);
            document = Jsoup.connect(pageUrl).maxBodySize(0).get();
        } catch (Exception ignored) {
            // потом логировать
        }
        if (document == null) {
            return;
        }
        Elements links = document.select("a");

        if (links.isEmpty()) {
            return;
        }


        links.stream().map(link -> link.attr("href")).forEachOrdered(url -> {
           /* if (!url.startsWith(siteURL)) {
                return;
            }*/
            if (url.contains("#")) {
                return;
            }
            if (uniqueURL.contains(url)) {
                return;
            }

            uniqueURL.add(url);
            String newTab = tab + "\t";
            getLinks(url, newTab);
        });
    }
}