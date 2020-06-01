package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatHelper {
    private static int aliveCounter = 0;
    // 0 - dead, 1 - alive
    public static void saveStatus(int status) {
        if (status == 1) {
            ++aliveCounter;
            log.info("Alive: {}", aliveCounter);
        }
        else {
            aliveCounter = 0;
        }
    }
}
