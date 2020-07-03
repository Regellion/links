import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.concurrent.RecursiveAction;

public class WalkLink extends RecursiveAction {
    private String url;
    private static Set<String> uniqueURL = Collections.synchronizedSet(new HashSet<>());
    private static final String siteURL = "https://skillbox.ru/";
    private String tab;

    WalkLink(String url, String tab){
        this.url = url;
        this.tab = tab;
    }
    @Override
    protected void compute() {
        if(uniqueURL.contains(url)){
            return;
        }
        // Если ссылка уникальная, печатаем. потом можно будет записывать в файл
        System.out.println(tab + url);
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

            links.stream().map(link -> link.attr("abs:href"))
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
                        WalkLink task = new WalkLink(url, newTab);
                        task.fork();
                        subTasks.add(task);
                        //TODO Метод работает верно, собирает ровно столько же ссылок сколько и в однопоточном режиме,
                        // но происходит Конкуренция потоков. Из за этого лесенка не получается красивой. Если
                        // здесь применить метод join(), то тогда все отображается отлично, но по сути, приложение так же становиться однопоточным.
                        // Как всего этого избежать пока не пойму.
                    });

            subTasks.forEach(task-> {
                // Собираем результат всех подзадач
                task.join();
            });

        }catch (Exception ignored){}

    }
}