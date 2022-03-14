package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {
	private final class MyModel implements Model {
		private Board.GameState board;

		private Set<Observer> observers;

		private MyModel(final GameSetup setup,
				final Player mrX,
				final ImmutableList<Player> detectives
		) {
			board = new MyGameStateFactory().build(setup, mrX, detectives);
			observers = new HashSet<>();
		}

		@Nonnull
		@Override
		public Board getCurrentBoard() {
			return board;
		}

		@Override
		public void registerObserver(@Nonnull Observer observer) {
			Objects.requireNonNull(observer);
			if (!observers.add(observer)) throw new IllegalArgumentException();
		}

		@Override
		public void unregisterObserver(@Nonnull Observer observer) {
			Objects.requireNonNull(observer);
			if (!observers.remove(observer)) throw new IllegalArgumentException();
		}

		@Nonnull
		@Override
		public ImmutableSet<Observer> getObservers() {
			return ImmutableSet.copyOf(observers);
		}

		@Override
		public void chooseMove(@Nonnull Move move) {
			board = board.advance(move);
			for (Observer observer : observers) {
				observer.onModelChanged(board, Observer.Event.MOVE_MADE);
			}
			if (board.getWinner().size() != 0) {
				for (Observer observer : observers) {
					observer.onModelChanged(board, Observer.Event.GAME_OVER);
				}
			}
		}
	}

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {
		return new MyModel(setup, mrX, detectives);
	}
}
