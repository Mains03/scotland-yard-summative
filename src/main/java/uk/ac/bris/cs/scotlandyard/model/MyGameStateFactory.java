package uk.ac.bris.cs.scotlandyard.model;

// not sure if we are supposed to import this
// import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;
import uk.ac.bris.cs.scotlandyard.model.Piece.MrX;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
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
				final ImmutableList<Player> detectives) {
			// tests imply NullPointerException should be thrown
			// found this concise way of doing this
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
			return null;
		}

		@Nonnull
		@Override
		public ImmutableSet<Piece> getWinner() {
			return winner;
		}

		@Nonnull
		@Override
		public ImmutableSet<Move> getAvailableMoves() {
			var builder = ImmutableSet.builder();

			BiFunction<Player, ScotlandYard.Ticket, Boolean> hasTicket = (player, ticket) -> {
				player.tickets().containsKey(ticket);
			};

			Function<Player, List<ScotlandYard.Ticket>> getTickets = player -> {
				return player.tickets().keySet().stream()
						.filter(ticket -> hasTicket.apply(player, ticket))
						.collect(Collectors.toList());
			};

			Function<Integer, Set<ScotlandYard.Ticket>> useableTicketsAtLocation = location -> {
				Set<ScotlandYard.Ticket> tickets = new HashSet<ScotlandYard.Ticket>();
				setup.graph.adjacentNodes(location).stream()
						.map(dest -> setup.graph.edgeValue(location, dest).get())
						.forEach(transports -> {
							transports.stream()
									.forEach(transport -> tickets.add(transport.requiredTicket()));
						});
				return tickets;
			};

			Function<Player, List<ScotlandYard.Ticket>> getUseableTickets = player -> {
				Set<ScotlandYard.Ticket> targetTickets = useableTicketsAtLocation.apply(player.location());
				return getTickets.apply(player).stream()
						.filter(ticket -> targetTickets.contains(ticket))
						.collect(Collectors.toList());
			};

			// need to know the destination of each ticket also
			Function<Player, Move> getMoves = player -> {
				getUseableTickets.apply(player);
			};

			return builder.build();
		}

		@Nonnull
		@Override
		public GameState advance(Move move) {
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
