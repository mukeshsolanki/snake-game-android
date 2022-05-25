package com.mukeshsolanki.snake.domain.game

import androidx.compose.runtime.mutableStateOf
import com.mukeshsolanki.snake.data.model.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class GameEngine(
    private val scope: CoroutineScope,
    private val onGameEnded: () -> Unit,
    private val onFoodEaten: () -> Unit
) {
    private val mutex = Mutex()
    private val mutableState =
        MutableStateFlow(
            State(
                food = Pair(5, 5),
                snake = listOf(Pair(7, 7)),
                currentDirection = SnakeDirection.Right
            )
        )
    val state: Flow<State> = mutableState
    private val currentDirection = mutableStateOf(SnakeDirection.Right)

    var move = Pair(1, 0)
        set(value) {
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    fun reset() {
        mutableState.update {
            it.copy(
                food = Pair(5, 5),
                snake = listOf(Pair(7, 7)),
                currentDirection = SnakeDirection.Right
            )
        }
        currentDirection.value = SnakeDirection.Right
        move = Pair(1, 0)
    }

    init {
        scope.launch {
            var snakeLength = 2
            while (true) {
                delay(150)
                mutableState.update {
                    val hasReachedLeftEnd =
                        it.snake.first().first == 0 && it.currentDirection == SnakeDirection.Left
                    val hasReachedTopEnd =
                        it.snake.first().second == 0 && it.currentDirection == SnakeDirection.Up
                    val hasReachedRightEnd =
                        it.snake.first().first == BOARD_SIZE - 1 && it.currentDirection == SnakeDirection.Right
                    val hasReachedBottomEnd =
                        it.snake.first().second == BOARD_SIZE - 1 && it.currentDirection == SnakeDirection.Down
                    if (hasReachedLeftEnd || hasReachedTopEnd || hasReachedRightEnd || hasReachedBottomEnd) {
                        snakeLength = 2
                        onGameEnded.invoke()
                    }
                    if (move.first == 0 && move.second == -1) {
                        currentDirection.value = SnakeDirection.Up
                    } else if (move.first == -1 && move.second == 0) {
                        currentDirection.value = SnakeDirection.Left
                    } else if (move.first == 1 && move.second == 0) {
                        currentDirection.value = SnakeDirection.Right
                    } else if (move.first == 0 && move.second == 1) {
                        currentDirection.value = SnakeDirection.Down
                    }
                    val newPosition = it.snake.first().let { poz ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first + BOARD_SIZE) % BOARD_SIZE,
                                (poz.second + move.second + BOARD_SIZE) % BOARD_SIZE
                            )
                        }
                    }
                    if (newPosition == it.food) {
                        onFoodEaten.invoke()
                        snakeLength++
                    }

                    if (it.snake.contains(newPosition)) {
                        snakeLength = 2
                        onGameEnded.invoke()
                    }

                    it.copy(
                        food = if (newPosition == it.food) Pair(
                            Random().nextInt(BOARD_SIZE),
                            Random().nextInt(BOARD_SIZE)
                        ) else it.food,
                        snake = listOf(newPosition) + it.snake.take(snakeLength - 1),
                        currentDirection = currentDirection.value,
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 32
    }
}
