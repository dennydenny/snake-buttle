package ru.codebattle.client;

import lombok.extern.slf4j.Slf4j;
import ru.codebattle.client.api.BoardElement;
import ru.codebattle.client.api.BoardPoint;
import ru.codebattle.client.api.Direction;
import ru.codebattle.client.api.GameBoard;

import java.util.*;
import java.util.stream.Collectors;

import static ru.codebattle.client.api.BoardElement.*;


@Slf4j
public class DecisionMaker {
    public static Direction lastTurn = null;
    public static Map<BoardPoint, Integer> history = new HashMap<>();
    private List<BoardElement> dangerousElements = Arrays.asList(START_FLOOR, WALL, STONE, ENEMY_HEAD_DOWN, ENEMY_HEAD_LEFT, ENEMY_HEAD_RIGHT,
            ENEMY_HEAD_UP, ENEMY_HEAD_EVIL, ENEMY_HEAD_FLY, ENEMY_TAIL_END_DOWN, ENEMY_TAIL_END_LEFT, ENEMY_TAIL_END_UP, ENEMY_TAIL_END_RIGHT,
            ENEMY_TAIL_INACTIVE, ENEMY_BODY_HORIZONTAL, ENEMY_BODY_VERTICAL, ENEMY_BODY_LEFT_DOWN, ENEMY_BODY_LEFT_UP, ENEMY_BODY_RIGHT_DOWN,
            ENEMY_BODY_RIGHT_UP, TAIL_END_DOWN, TAIL_END_LEFT, TAIL_END_UP, TAIL_END_RIGHT, TAIL_INACTIVE, BODY_HORIZONTAL, BODY_VERTICAL,
            BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP);
    private List<BoardElement> dangerousElementsInEvil = Arrays.asList(START_FLOOR, WALL, TAIL_END_DOWN, TAIL_END_LEFT, TAIL_END_UP,
            TAIL_END_RIGHT, TAIL_INACTIVE, BODY_HORIZONTAL, BODY_VERTICAL,
            BODY_LEFT_DOWN, BODY_LEFT_UP, BODY_RIGHT_DOWN, BODY_RIGHT_UP);
    private List<BoardElement> betterThanDeadElements = Arrays.asList(START_FLOOR, WALL, STONE);
    private List<BoardElement> goodElementsInEvil = Arrays.asList(APPLE, FLYING_PILL, FURY_PILL, GOLD, ENEMY_HEAD_DOWN, ENEMY_HEAD_LEFT, ENEMY_HEAD_RIGHT,
            ENEMY_HEAD_UP, ENEMY_HEAD_EVIL, ENEMY_HEAD_FLY, ENEMY_TAIL_END_DOWN, ENEMY_TAIL_END_LEFT, ENEMY_TAIL_END_UP, ENEMY_TAIL_END_RIGHT,
            ENEMY_TAIL_INACTIVE, ENEMY_BODY_HORIZONTAL, ENEMY_BODY_VERTICAL, ENEMY_BODY_LEFT_DOWN, ENEMY_BODY_LEFT_UP, ENEMY_BODY_RIGHT_DOWN,
            ENEMY_BODY_RIGHT_UP);
    private static DecisionMaker instance;

    private DecisionMaker() {
    }

    public static DecisionMaker getInstance() {
        if (instance == null) instance = new DecisionMaker();
        return instance;
    }

    public void checkWays(GameBoard gameBoard, BoardPoint current) {
        if (gameBoard.hasElementAt(current.shiftRight(), dangerousElements)) log.error("Shit RIGHT");
        if (gameBoard.hasElementAt(current.shiftLeft(), dangerousElements)) log.error("Shit LEFT");
        if (gameBoard.hasElementAt(current.shiftTop(), dangerousElements)) log.error("Shit UP");
        if (gameBoard.hasElementAt(current.shiftBottom(), dangerousElements)) log.error("Shit DOWN");
    }

    private List<Direction> calculateSafeWays(GameBoard gameBoard, BoardPoint current, List<BoardElement> alarm, BoardPoint previous) {
        List<Direction> goodWays = new ArrayList<>();
        log.info("Calc step for {}", current);

        if (previous == null) {
            if (!gameBoard.hasElementAt(current.shiftRight(), alarm)) {
                log.info("{} is OK.", Direction.RIGHT);
                goodWays.add(Direction.RIGHT);
            }
            if (!gameBoard.hasElementAt(current.shiftLeft(), alarm)) {
                log.info("{} is OK.", Direction.LEFT);
                goodWays.add(Direction.LEFT);
            }

            if (!gameBoard.hasElementAt(current.shiftTop(), alarm)) {
                log.info("{} is OK.", Direction.UP);
                goodWays.add(Direction.UP);
            }
            if (!gameBoard.hasElementAt(current.shiftBottom(), alarm)) {
                log.info("{} is OK.", Direction.DOWN);
                goodWays.add(Direction.DOWN);
            }
        }
        else {
            log.info("Previous point was: {}", previous);
            if (!gameBoard.hasElementAt(current.shiftRight(), alarm) && !current.shiftRight().equals(previous)) {
                log.info("{} is OK.", Direction.RIGHT);
                goodWays.add(Direction.RIGHT);
            }
            if (!gameBoard.hasElementAt(current.shiftLeft(), alarm) && !current.shiftLeft().equals(previous)) {
                log.info("{} is OK.", Direction.LEFT);
                goodWays.add(Direction.LEFT);
            }

            if (!gameBoard.hasElementAt(current.shiftTop(), alarm) && !current.shiftTop().equals(previous)) {
                log.info("{} is OK.", Direction.UP);
                goodWays.add(Direction.UP);
            }
            if (!gameBoard.hasElementAt(current.shiftBottom(), alarm) && !current.shiftBottom().equals(previous)) {
                log.info("{} is OK.", Direction.DOWN);
                goodWays.add(Direction.DOWN);
            }
        }
        log.info("Intermediate safe ways: {}", goodWays);


        Collections.shuffle(goodWays);
        List<Direction> safeWays = goodWays.stream().filter(x -> {
            return checkIsOkWithDirection(x);
        }).collect(Collectors.toList());
        log.info("Intermediate safe ways after direction check: {}", safeWays);
        return safeWays;
    }

    private List<Direction> filterOutPreviousPoints(List<Direction> options, BoardPoint current) {
        return options.stream().filter(x -> {
            BoardPoint futurePoint = shiftAccordingDirection(current, x);
            return history.get(futurePoint) == null;
        }).collect(Collectors.toList());
    }

    public Direction saveStrategy(GameBoard gameBoard, BoardPoint current) {
        checkWays(gameBoard, current);

        // Basic checks
        List<Direction> safeWays;
        if (!gameBoard.amIEvil()) {
            safeWays = calculateSafeWays(gameBoard, current, dangerousElements, null);
        } else {
            log.info("I am evil now.");
            safeWays = calculateSafeWays(gameBoard, current, dangerousElementsInEvil, null);
        }

        if (safeWays.isEmpty()) {
            log.info("No safe ways. Choose better than dead.");
            safeWays = calculateSafeWays(gameBoard, current, betterThanDeadElements, null);
        }

        log.info("Safe ways: {}", safeWays);

        // History
        List<Direction> safeWaysWithoutHistory = filterOutPreviousPoints(safeWays, current);
        log.info("Ways after history filter: {}", safeWaysWithoutHistory);

        // Second step check
        List<Direction> safeWaysWithSecondStep;
        if (safeWaysWithoutHistory.isEmpty()) {
            safeWaysWithSecondStep = safeWays.stream().filter(x -> {
                return isSecondStepPossible(gameBoard, current, x);
            }).collect(Collectors.toList());
        } else {
            safeWaysWithSecondStep = safeWaysWithoutHistory.stream().filter(x -> {
                return isSecondStepPossible(gameBoard, current, x);
            }).collect(Collectors.toList());
        }

        log.info("Ways with second step: {}", safeWaysWithSecondStep);

        List<BoardPoint> allGoods = getAllGoods(gameBoard);

        if (allGoods.isEmpty()) {
            log.info("Goods are empty");
        }
        else {
            log.debug("All goods: {}", allGoods);
        }

        Direction shortesWinDirection = calcShortestWin(gameBoard, allGoods, current, safeWaysWithSecondStep);
        if (shortesWinDirection != null) log.info("Better direction for goods: {}", shortesWinDirection);

        if (shortesWinDirection != null) return shortesWinDirection;
        return !safeWays.isEmpty() ? safeWays.get(0) : Direction.STOP;
    }

    private void incrementCounter(Map<Direction, Integer> map, Direction value) {
        map.merge(value, 1, Integer::sum);
    }

    public <K, V extends Comparable<V>> K maxUsingCollectionsMaxAndLambda(Map<K, V> map) {
        Map.Entry<K, V> maxEntry = Collections.max(map.entrySet(), (Map.Entry<K, V> e1, Map.Entry<K, V> e2) -> e1.getValue()
                .compareTo(e2.getValue()));
        return maxEntry.getKey();
    }

    public <K, V extends Comparable<V>> Map.Entry<K, V> minUsingCollectionsMinAndLambda(Map<K, V> map) {
        Map.Entry<K, V> minEntry = Collections.min(map.entrySet(), (Map.Entry<K, V> e1, Map.Entry<K, V> e2) -> e1.getValue()
                .compareTo(e2.getValue()));
        return minEntry;
    }

    private boolean checkIsOkWithDirection(Direction newDirection) {
        if (lastTurn != null) {
            if ((lastTurn.equals(Direction.RIGHT) && newDirection.equals(Direction.LEFT)) ||
                    (lastTurn.equals(Direction.LEFT) && newDirection.equals(Direction.RIGHT)) ||
                    (lastTurn.equals(Direction.DOWN) && newDirection.equals(Direction.UP)) ||
                    (lastTurn.equals(Direction.UP) && newDirection.equals(Direction.DOWN))) {
                return false;
            }
        }
        return true;
    }

    private BoardPoint shiftAccordingDirection(BoardPoint boardPoint, Direction direction) {
        switch (direction) {
            case UP:
                return boardPoint.shiftTop();
            case DOWN:
                return boardPoint.shiftBottom();
            case LEFT:
                return boardPoint.shiftLeft();
            default:
                return boardPoint.shiftRight();
        }
    }

    private boolean isSecondStepPossible(GameBoard gameBoard, BoardPoint current, Direction choose) {
        boolean result = false;

        BoardPoint secondCoordinate = shiftAccordingDirection(current, choose);
        log.info("SecondCoordinate is {}", secondCoordinate);

        if (!gameBoard.amIEvil()) {
            if (!calculateSafeWays(gameBoard, secondCoordinate, dangerousElements, current).isEmpty()) {
                result = true;
            }
        } else {
            if (!calculateSafeWays(gameBoard, secondCoordinate, dangerousElementsInEvil, current).isEmpty()) {
                result = true;
            }
        }
        log.info("Result is {}", result);
        return result;
    }

    private List<BoardPoint> getAllGoods(GameBoard gameBoard) {
        List<BoardPoint> result = gameBoard.getApples();
        result.addAll(gameBoard.getGold());
        result.addAll(gameBoard.getFuryPills());
        return result;
    }

    private Direction calcShortestWin(GameBoard gameBoard, List<BoardPoint> goods, BoardPoint current, List<Direction> safeWays) {
        Map<Direction, Integer> result = new HashMap<>();
        for (Direction direction : safeWays) {
            int lenght = getLengthForGoodsBasedOnDirection(gameBoard, direction, current, goods);
            if (lenght == 0) continue;
            log.info("Some goods on {} after {} steps.", direction, lenght);
            result.put(direction, lenght);
        }
        if (result.isEmpty()) return null;
        return minUsingCollectionsMinAndLambda(result).getKey();
    }

    private int getLengthForGoodsBasedOnDirection(GameBoard gameBoard, Direction direction, BoardPoint current, List<BoardPoint> allGoods) {
        List<BoardPoint> goodsOnMyWay = new ArrayList<>();
        int size = gameBoard.size();
        int beforeBorder = 0;

        switch (direction) {
            case DOWN:
                beforeBorder = size - current.getY();
                for (int y = current.getY(); beforeBorder != 0; y++) {
                    BoardPoint fantomPoint = new BoardPoint(current.getX(), y);
                    if (allGoods.contains(fantomPoint)) {
                        goodsOnMyWay.add(fantomPoint);
                    } else {
                        if (gameBoard.amIEvil()) {
                            if (goodElementsInEvil.contains(gameBoard.getElementAt(fantomPoint))) {
                                goodsOnMyWay.add(fantomPoint);
                            }
                        }
                    }
                    --beforeBorder;
                }
                break;
            case UP:
                beforeBorder = current.getY();
                for (int y = current.getY(); beforeBorder != 0; y--) {
                    BoardPoint fantomPoint = new BoardPoint(current.getX(), y);
                    if (allGoods.contains(fantomPoint)) {
                        goodsOnMyWay.add(fantomPoint);
                    } else {
                        if (gameBoard.amIEvil()) {
                            if (goodElementsInEvil.contains(gameBoard.getElementAt(fantomPoint))) {
                                goodsOnMyWay.add(fantomPoint);
                            }
                        }
                    }
                    --beforeBorder;
                }
                break;
            case LEFT:
                beforeBorder = current.getX();
                for (int x = current.getX(); beforeBorder != 0; x--) {
                    BoardPoint fantomPoint = new BoardPoint(x, current.getY());
                    if (allGoods.contains(fantomPoint)) {
                        goodsOnMyWay.add(fantomPoint);
                    } else {
                        if (gameBoard.amIEvil()) {
                            if (goodElementsInEvil.contains(gameBoard.getElementAt(fantomPoint))) {
                                goodsOnMyWay.add(fantomPoint);
                            }
                        }
                    }
                    --beforeBorder;
                }
                break;
            default:
                beforeBorder = size - current.getX();
                for (int x = current.getX(); beforeBorder != 0; x++) {
                    BoardPoint fantomPoint = new BoardPoint(x, current.getY());
                    if (allGoods.contains(fantomPoint)) {
                        goodsOnMyWay.add(fantomPoint);
                    } else {
                        if (gameBoard.amIEvil()) {
                            if (goodElementsInEvil.contains(gameBoard.getElementAt(fantomPoint))) {
                                goodsOnMyWay.add(fantomPoint);
                            }
                        }
                    }
                    --beforeBorder;
                }
                break;
        }
        Map<BoardPoint, Integer> calculated = new HashMap<>();
        goodsOnMyWay.forEach(x -> {
            Integer distance = getDistanceBetween(current, x);
            calculated.put(x, distance);
        });
        if (calculated.isEmpty()) return 0;

        return minUsingCollectionsMinAndLambda(calculated).getValue();
    }

    private int getDistanceBetween(BoardPoint one, BoardPoint two) {
        return (int) Math.hypot(one.getX() - two.getX(), one.getY() - two.getY());
    }

}
