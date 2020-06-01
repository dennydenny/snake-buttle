package ru.codebattle.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import ru.codebattle.client.api.BoardElement;
import ru.codebattle.client.api.BoardPoint;
import ru.codebattle.client.api.Direction;
import ru.codebattle.client.api.SnakeAction;

@Slf4j
public class Main {

    private static final String SERVER_ADDRESS = "http://codebattle-pro-2020s1.westeurope.cloudapp.azure.com/codenjoy-contest/board/player/wpd1fhaza5dpvps83wka?code=3800417225037778592&gameName=snakebattle";


    public static void main(String[] args) throws URISyntaxException, IOException {
        SnakeBattleClient client = new SnakeBattleClient(SERVER_ADDRESS);

        client.run(gameBoard -> {
            BoardPoint current = gameBoard.getMyHead();
            try {
                if (gameBoard.hasElementAt(current, BoardElement.HEAD_SLEEP) || gameBoard.hasElementAt(current, BoardElement.HEAD_DEAD)) {
                    StatHelper.saveStatus(0);
                    log.info("I'm dead...");
                    if (!DecisionMaker.history.isEmpty()) DecisionMaker.history.clear();
                } else {
                    DecisionMaker.history.put(current, 1);
                    log.info("Now: {}", current);
                    log.info("Last turn: {}", DecisionMaker.lastTurn);
                    StatHelper.saveStatus(1);
                }
                var random = new Random(System.currentTimeMillis());
                var direction = DecisionMaker.getInstance().saveStrategy(gameBoard, current);
                DecisionMaker.lastTurn = direction;
                var act = random.nextInt() % 2 == 0;
                log.info("Turn: {}", direction);
                return new SnakeAction(act, direction);
            }
            catch (NullPointerException npe) {
                log.error("NPE occurred.");
                return new SnakeAction(false, Direction.STOP);
            }
        });

        System.in.read();

        client.initiateExit();
    }
}
