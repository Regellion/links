import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class WalkLink extends RecursiveAction {
    private String url;
    private static Set<String> uniqueURL = Collections.synchronizedSet(new HashSet<>());
    private String siteURL;
    private String tab;
    private static List<String> linksList = Collections.synchronizedList(new ArrayList<>());

    WalkLink(String url, String tab, String siteURL){
        this.url = url;
        this.tab = tab;
        this.siteURL = siteURL;
    }

    @Override
    protected void compute() {

        if(uniqueURL.contains(url)){
            return;
        }
        // Если ссылка уникальная пишем в файл
        linksList.add(tab + url);
        //System.out.println(tab + url);
        // Добавляем в сет
        uniqueURL.add(url);
        try{

            //Пробуем подключиться к ссылки
            Document document = Jsoup.connect(url)
                    .maxBodySize(0)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .ignoreHttpErrors(true)
                    .get();
            if (document == null) {
                return;
            }
            // Получаем все ссылки страницы
            Elements links = document.select("a");
            if (links.isEmpty()) {
                return;
            }

            List<WalkLink> subTasks = new LinkedList<>();

            links.stream().map(link -> link.attr("href"))
                    .filter(url-> url.startsWith(siteURL))
                    .filter(url-> !url.contains("#"))
                    .map(url-> {
                        if(url.contains("?utm_source")) {
                            url = url.split("\\?utm_source", 2)[0];
                        }
                        return url;
                    })
                    .filter(url-> !uniqueURL.contains(url))
                    .forEachOrdered(url-> {
                        //Все ссылки прощедшие фильтры делаем подзадачами
                        String newTab = tab + "\t";
                        WalkLink task = new WalkLink(url, newTab, siteURL);
                        task.fork();
                        subTasks.add(task);
                    });

            // Собираем результат всех подзадач
            subTasks.forEach(ForkJoinTask::join);

        }catch (Exception ex){
            System.out.println("Connection is failed. URL: " + url + " " + ex.getMessage());
        }
    }

    void writer(String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        StringBuffer sb = new StringBuffer();
        linksList.forEach(url-> sb.append(url).append("\n"));
        fileWriter.write(String.valueOf(sb));
        fileWriter.close();
    }
}