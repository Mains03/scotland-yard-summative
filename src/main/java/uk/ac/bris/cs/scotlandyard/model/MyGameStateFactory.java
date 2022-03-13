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

		private final GameSetup setup;
		private final ImmutableSet<Piece> remaining;
		private final ImmutableList<LogEntry> log;
		private final Player mrX;
		private final ImmutableList<Player> detectives;
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
            moves = generateMoves(setup.graph, remaining);
            winner = ImmutableSet.of();
        }

        // Gets the reference of a player from the provided piece
        private Player getPlayer(Piece piece) {
            if (piece.isMrX()) return mrX;
            else {
                return detectives.stream()
                        .filter(p -> p.piece() == piece)
                        .findFirst()
                        .stream().toList()
                        .get(0);
            }
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
                final Set<Piece> pieces
        ) {
            Set<Player> players = pieces.stream().map(p -> getPlayer(p)).collect(Collectors.toSet());
            ImmutableSet.Builder<Move> builder = new ImmutableSet.Builder<>();
            for (Player player : players) {
                builder.addAll(generatePossibleMoves(graph, player));
            }
            return builder.build();
        }

        private ImmutableSet<Move> generatePossibleMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            // Used to distinguish between single and double moves from stream to check
            // destination, destination1, destination2
            Move.Visitor<Boolean> isMoveValidVisitor = new Move.Visitor<>() {
                @Override
                public Boolean visit(Move.SingleMove move) {
                    // check if destination is occupied
                    if (detectives.stream().anyMatch(detective -> detective.location() == move.destination))
                        return false;
                    if (!player.has(move.ticket)) return false;
                    return true;
                }

                @Override
                public Boolean visit(Move.DoubleMove move) {
                    // check if destination is occupied
                    if (detectives.stream().anyMatch(
                            detective -> (detective.location() == move.destination1)
                                    || (detective.location() == move.destination2)
                    ))
                        return false;
                    // COvering double tickets for MrX
                    // If MrX doesn't have a double ticket
                    if (!player.has(ScotlandYard.Ticket.DOUBLE)) return false;
                    // If it's MrX's last move they can't use two tickets
                    if (setup.moves.size() == 1) return false;
                    List<ScotlandYard.Ticket> tickets = Lists.newArrayList(move.tickets());
                    // If MrX is using two of the same ticket they don't have
                    if (tickets.get(0) == tickets.get(1)) {
                        if (!player.hasAtLeast(tickets.get(0), 2)) return false;
                    } else {
                        // If MrX is using two different tickets
                        if (!player.has(tickets.get(0))) return false;
                        if (!player.has(tickets.get(1))) return false;
                    }
                    return true;
                }
            };

            Predicate<Move> isMovePossible = move -> move.accept(isMoveValidVisitor);

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
            // iterate over neighbouring nodes at player's location
            for (Integer destination : graph.adjacentNodes(player.location())) {
                // iterate over transport available from player's location to destination
                for (ScotlandYard.Transport transport : graph.edgeValue(player.location(), destination).get()) {
                    builder.add(new Move.SingleMove(
                            player.piece(), player.location(), transport.requiredTicket(), destination
                    ));
                    // iterate over neighbouring nodes at destination
                    for (Integer destination2 : graph.adjacentNodes(destination)) {
                        // iterate over transport available from destination to destination2
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

            return new MyGameState(
                    setup,
                    newRemaining(move),
                    newLog(move),
                    newMrX(move),
                    newDetectives(move)
            );
        }

        private ImmutableSet<Piece> newRemaining(Move move) {
            if (move.commencedBy().isMrX()) {
                return ImmutableSet.copyOf(
                        detectives.stream()
                                .map(player -> player.piece())
                                .collect(Collectors.toList())
                );
            } else {
                Collection<Piece> currentRemaining = remaining.stream()
                        .filter(piece -> !move.commencedBy().webColour().equals(piece.webColour()))
                        .collect(Collectors.toList());
                if (currentRemaining.isEmpty()) return ImmutableSet.of(MrX.MRX);
                else return ImmutableSet.copyOf(currentRemaining);
            }
        }

        private Player newMrX(Move move) {
            return newPlayer(move, mrX);
        }

        private Player newPlayer(Move move, Player player) {
            Move.Visitor<Integer> destinationVisitor = new Move.Visitor<Integer>() {
                @Override
                public Integer visit(Move.SingleMove move) {
                    return move.destination;
                }

                @Override
                public Integer visit(Move.DoubleMove move) {
                    return move.destination2;
                }
            };

            if (!move.commencedBy().webColour().equals(player.piece().webColour())) return player;
            else {
                player = player.use(move.tickets());
                return player.at(move.accept(destinationVisitor));
            }
        }

        private ImmutableList<LogEntry> newLog(Move move) {
            if (!move.commencedBy().webColour().equals(mrX.piece().webColour())) return log;
            else {
                Move.Visitor<Collection<LogEntry>> logEntryVisitor = new Move.Visitor<Collection<LogEntry>>() {
                    @Override
                    public Collection<LogEntry> visit(Move.SingleMove move) {
                        // TODO: check if move should be revealed
                        return List.of(LogEntry.hidden(move.ticket));
                    }

                    @Override
                    public Collection<LogEntry> visit(Move.DoubleMove move) {
                        Collection<LogEntry> entries = new ArrayList<>();
                        // TODO: check if move should be revealed
                        entries.add(LogEntry.hidden(move.ticket1));
                        entries.add(LogEntry.hidden(move.ticket2));
                        return entries;
                    }
                };

                Collection<LogEntry> newLog = new ArrayList<>();
                newLog.addAll(log);
                newLog.addAll(move.accept(logEntryVisitor));
                return ImmutableList.copyOf(newLog);
            }
        }

        private ImmutableList<Player> newDetectives(Move move) {
            return ImmutableList.copyOf(
                    detectives.stream()
                            .map(player -> newPlayer(move, player))
                            .collect(Collectors.toList())
            );
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
