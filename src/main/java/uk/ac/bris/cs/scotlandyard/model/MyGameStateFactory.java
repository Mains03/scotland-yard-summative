package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

    private final class MyGameState implements GameState {

		private final GameSetup setup;
		private final ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;
		private final Player mrX;
		private final List<Player> detectives;
		private final ImmutableSet<Move> moves;
		// I think we can determine the winner when a new game state is constructed
		private final ImmutableSet<Piece> winner;

        private MyGameState(
                final GameSetup setup,
                final ImmutableSet<Piece> remaining,
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final ImmutableList<Player> detectives
        ) {
            Objects.requireNonNull(setup);
            Objects.requireNonNull(remaining);
            Objects.requireNonNull(log);
            Objects.requireNonNull(mrX);
            Objects.requireNonNull(detectives);
            if (setup.moves.isEmpty()) {
                throw new IllegalArgumentException();
            }
            inspectDetectives(detectives);
            if (setup.graph.nodes().size() == 0) {
                throw new IllegalArgumentException();
            }
            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            moves = generateMoves(setup.graph, mrX, detectives, remaining);
            winner = ImmutableSet.of();
        }

        private void inspectDetectives(final ImmutableList<Player> detectives) {
            if (detectives.stream() // check for double tickets
                    .anyMatch(player -> player.has(ScotlandYard.Ticket.DOUBLE))) {
                throw new IllegalArgumentException();
            }
            if (detectives.stream() // check for duplicate locations
                    .map(Player::location)
                    .distinct()
                    .count() < detectives.size()) {
                throw new IllegalArgumentException();
            }
            if (detectives.stream() // check for secret tickets
                    .anyMatch(player -> player.has(ScotlandYard.Ticket.SECRET))) {
                throw new IllegalArgumentException();
            }
        }

        // Generates all possible moves for each player
        private ImmutableSet<Move> generateMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player mrX,
                final Collection<Player> detectives,
                final Collection<Piece> pieces
        ) {
            Collection<Player> players = new ArrayList<>();
            players.add(mrX);
            players.addAll(detectives);
            return new MoveGenerator(
                    graph,
                    players,
                    pieces
            ).generateMoves();
        }

        @Nonnull
        @Override
        public GameSetup getSetup() {
            return setup;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getPlayers() {
            ImmutableSet.Builder<Piece> builder = ImmutableSet.builder();
            builder.add(mrX.piece());
            builder.addAll(detectives.stream().map(player -> player.piece()).collect(Collectors.toList()));
            return builder.build();
        }

        @Nonnull
        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            Optional<Player> target = detectives.stream()
                    .filter(player -> player.piece() == detective)
                    .findFirst();
            return target.isPresent() ?
                    Optional.of(target.get().location())
                    : Optional.empty();
        }

        @Nonnull
        @Override
        public Optional<TicketBoard> getPlayerTickets(Piece piece) {
            AtomicReference<Player> target = new AtomicReference<Player>();

            if (mrX.piece() == piece) {
                target.set(mrX);
            } else {
                detectives.stream()
                        .filter(player -> player.piece() == piece)
                        .findFirst()
                        .ifPresent(player -> target.set(player));
            }

            if (target.get() == null) {
                return Optional.empty();
            }
            return Optional.of(ticket -> {
                // not sure if this anonymous instance will always have access to target
                return target.get().tickets().getOrDefault(ticket, 0);
            });
        }

        @Nonnull
        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return log;
        }

        @Nonnull
        @Override
        public ImmutableSet<Piece> getWinner() {
            return winner;
        }

        @Nonnull
        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return moves;
        }

        @Nonnull
        @Override
        public GameState advance(Move move) {
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);

			/*
			GameState gs = move.accept(new Move.Visitor<>() {
				@Override public visit(Move.SingleMove singleMove){
					Piece piece = singleMove.commencedBy();
					ScotlandYard.Ticket ticket =
				}
				@Override public visit(Move.DoubleMove doubleMove){


				}
			});
			*/


            return null;
        }
    }

    @Nonnull
    @Override
    public GameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {
        return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
    }

}
