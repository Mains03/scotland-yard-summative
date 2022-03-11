package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;

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
			if (setup.moves.isEmpty()) { throw new IllegalArgumentException(); }
			inspectDetectives(detectives);
			if (setup.graph.nodes().size() == 0) { throw new IllegalArgumentException(); }
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			moves = generateMoves(setup.graph, remaining);
			winner = ImmutableSet.of();
		}

		private void inspectDetectives(final ImmutableList<Player> detectives) {
			if (detectives.stream() // check for double tickets
					.anyMatch(player -> player.has(ScotlandYard.Ticket.DOUBLE))) { throw new IllegalArgumentException(); }
			if (detectives.stream() // check for duplicate locations
					.map(player -> player.location())
					.distinct()
					.count() < detectives.size()) { throw new IllegalArgumentException(); }
			if (detectives.stream() // check for secret tickets
					.anyMatch(player -> player.has(ScotlandYard.Ticket.SECRET))) { throw new IllegalArgumentException(); }
		}

		private ImmutableSet<Move> generateMoves(
				final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
				final ImmutableSet<Piece> remaining
		) {
			Function<Piece, Player> getPlayerByPiece = piece -> {
				if (piece.isMrX()) return mrX;
				else {
					return detectives.stream().filter(detective -> {
						return detective.piece().webColour().equals(piece.webColour());
					}).collect(Collectors.toList()).get(0);
				}
			};

			ImmutableSet.Builder<Move> builder = new ImmutableSet.Builder<>();
			for (Piece piece : remaining) {
				builder.addAll(generatePossibleMoves(
						graph,
						getPlayerByPiece.apply(piece)
				));
			}
			return builder.build();
		}

		private ImmutableSet<Move> generatePossibleMoves(
				final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
				final Player player
		) {
			Predicate<Move> isMovePossible = move -> {
				List<ScotlandYard.Ticket> tickets = Lists.newArrayList(move.tickets());
				if (player.isDetective()) {
					if (tickets.size() > 1) return false;
					if (tickets.contains(ScotlandYard.Ticket.SECRET)) return false;
				}
				if (tickets.size() == 1) {
					if (!player.has(tickets.get(0))) return false;
				} else {
					if (!player.has(ScotlandYard.Ticket.DOUBLE)) return false;
					if (tickets.get(0) == tickets.get(1)) {
						if (!player.hasAtLeast(tickets.get(0), 2)) return false;
					} else {
						if (!player.has(tickets.get(0))) return false;
						if (!player.has(tickets.get(1))) return false;
					}
				}
				return true;
			};

			return ImmutableSet.copyOf(
					generateAllMoves(graph, player).stream()
							.filter(isMovePossible)
							.collect(Collectors.toList())
			);
		}

		// all moves available at the current location even if the player can't do them
		private ImmutableSet<Move> generateAllMoves(
				final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
				final Player player
		) {
			ImmutableSet.Builder<Move> builder = new ImmutableSet.Builder<>();
			for (Integer destination : graph.adjacentNodes(player.location())) {
				for (ScotlandYard.Transport transport : graph.edgeValue(player.location(), destination).get()) {
					builder.add(new Move.SingleMove(
							player.piece(), player.location(), transport.requiredTicket(), destination
					));
					for (Integer destination2 : graph.adjacentNodes(destination)) {
						for (ScotlandYard.Transport transport2 : graph.edgeValue(destination, destination2).get()) {
							builder.add(new Move.DoubleMove(
									player.piece(), player.location(), transport.requiredTicket(), destination, transport2.requiredTicket(), destination2
							));
							builder.add(new Move.DoubleMove(
									player.piece(), player.location(), ScotlandYard.Ticket.SECRET, destination, transport2.requiredTicket(), destination2
							));
						}
						builder.add(new Move.DoubleMove(
								player.piece(), player.location(), transport.requiredTicket(), destination, ScotlandYard.Ticket.SECRET, destination2
						));
						builder.add(new Move.DoubleMove(
								player.piece(), player.location(), ScotlandYard.Ticket.SECRET, destination, ScotlandYard.Ticket.SECRET, destination2
						));
					}
				}
				builder.add(new Move.SingleMove(
						player.piece(), player.location(), ScotlandYard.Ticket.SECRET, destination
				));
			}
			return builder.build();
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

			if (mrX.piece() == piece) { target.set(mrX); }
			else {
				detectives.stream()
						.filter(player -> player.piece() == piece)
						.findFirst()
						.ifPresent(player -> target.set(player));
			}

			if (target.get() == null) { return Optional.empty(); }
			return Optional.of(ticket -> {
				// not sure if this anonymous instance will always have access to target
				if (target.get().tickets().containsKey(ticket)) {
					return target.get().tickets().get(ticket);
				} else {
					return 0;
				}
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
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			/*
			GameState gs = move.accept(new Move.Visitor<>() {
				@Override public visit(Move.SingleMove singleMove){

				}
				@Override public visit(Move.DoubleMove doubleMove){


				}
			});
			*/


			return null;
		}
	}

	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(MrX.MRX), ImmutableList.of(), mrX, detectives);
	}

}
