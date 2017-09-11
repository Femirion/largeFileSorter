package com.puresteam;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Рабочий, читающий порцию данных из большого файла и сортирующий ее
 */
public class Worker implements Callable<Boolean> {

    /** Путь к файлу, потом понять на параметр */
    private static final String FILE_PATH = "/media/steam/E4DE4FB4DE4F7DB4/tmp/";
    /** Количество считываемых в память строк */
    private static final int COUNT = 10000;
    /** Текущая позиция в файле */
    private static AtomicLong currentPosition = new AtomicLong(0);

    @Override
    public Boolean call() {
        long currentId = Thread.currentThread().getId();
        List<String> lines;
        try  {
            while (true) {
                try (Stream<String> currentLines = Files.lines(Paths.get(FILE_PATH + "large_file.txt"))) {
                    // пропустим указанное количество строк currentPosition
                    // и после этого атомарно увеличим currentPosition на COUNT
                    // теперь ограничим limit(COUNT),
                    // получается мы считали N-строк начиная с позиции M
                    // следующий поток будет считывать N-строк с позиции M+N и тд до конца файла
                    lines = currentLines
                            .skip(currentPosition.getAndAccumulate(COUNT, (prev, incrementValue) -> prev + incrementValue))
                            .limit(COUNT)
                            .collect(Collectors.toList());
                }

//                System.out.println("id=" + currentId + "  lines=" + lines);

                // если ни одной строки не считали, значит файл подошел к концу
                // выйдем из цикла
                if (lines.isEmpty()) {
                    System.out.println("id=" + currentId + "  finished");
                    break;
                }

                // сортируем файл
                Collections.sort(lines);

                // отсортированная последовательность будет сохранена во временный файл
                // с названием tmp_номер-потока_текущее-время.txt
                // так точно не будет пересечения по названию!
                Path file = Paths.get(FILE_PATH + "tmp_" + currentId + "_" + LocalDateTime.now() + ".txt");
                Files.write(file, lines, Charset.forName("UTF-8"));

//                System.out.println("id=" + currentId + "  writeTo=" + file.getFileName());
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
