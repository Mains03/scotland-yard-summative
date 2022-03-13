package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.util.*;
import java.util.function.Consumer;

public final class MoveGenerator {
    private abstract class UnclaimedMove {
        abstract boolean isSingleMove();
        boolean isDoubleMove() { return !isSingleMove(); }
    }

    private final class UnclaimedSingleMove extends UnclaimedMove {
        final Integer source;
        final ScotlandYard.Ticket ticket;
        final Integer destination;

        UnclaimedSingleMove(Integer source, ScotlandYard.Ticket ticket, Integer destination) {
            this.source = source;
            this.ticket = ticket;
            this.destination = destination;
        }

        @Override
        boolean isSingleMove() {
            return true;
        }
    }

    private final class UnclaimedDoubleMove extends UnclaimedMove {
        final Integer source;
        final ScotlandYard.Ticket ticket1;
        final Integer destination1;
        final ScotlandYard.Ticket ticket2;
        final Integer destination2;

        UnclaimedDoubleMove(
                Integer source,
                ScotlandYard.Ticket ticket1,
                Integer destination1,
                ScotlandYard.Ticket ticket2,
                Integer destination2) {
            this.source = source;
            this.ticket1 = ticket1;
            this.destination1 = destination1;
            this.ticket2 = ticket2;
            this.destination2 = destination2;
        }

        @Override
        boolean isSingleMove() {
            return false;
        }
    }

    private final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph;
    private final Collection<Player> players;


    public MoveGenerator(
            final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
            final Collection<Player> players
    ) {
        this.graph = graph;
        this.players = players;
    }

    public ImmutableSet<Move> generateMoves() {
        Collection<UnclaimedMove> unclaimedMoves = new ArrayList<>();
        Consumer<Player> consumeMoves = player -> {
            var moves = generateUnclaimedMoves(player.location());
            unclaimedMoves.addAll(moves);
        };
        players.stream().forEach(consumeMoves);
        return generateMoves(unclaimedMoves);
    }

    private Collection<UnclaimedMove> generateUnclaimedMoves(Integer location) {
        Collection<UnclaimedMove> unclaimedMoves = new ArrayList<>();
        for (Integer destination : graph.adjacentNodes(location)) {
            for (ScotlandYard.Transport transport : graph.edgeValue(location, destination).get()) {
                unclaimedMoves.add(new UnclaimedSingleMove(location, transport.requiredTicket(), destination));
                for (Integer destination2 : graph.adjacentNodes(destination)) {
                    for (ScotlandYard.Transport transport2 : graph.edgeValue(destination, destination2).get()) {
                        unclaimedMoves.add(new UnclaimedDoubleMove(
                                location, transport.requiredTicket(), destination, transport2.requiredTicket(), destination2
                        ));
                        unclaimedMoves.add(new UnclaimedDoubleMove(
                                location, ScotlandYard.Ticket.SECRET, destination, transport2.requiredTicket(), destination2
                        ));
                    }
                    unclaimedMoves.add(new UnclaimedDoubleMove(
                            location, transport.requiredTicket(), destination, ScotlandYard.Ticket.SECRET, destination2
                    ));
                    unclaimedMoves.add(new UnclaimedDoubleMove(
                            location, ScotlandYard.Ticket.SECRET, destination, ScotlandYard.Ticket.SECRET, destination2
                    ));
                }
            }
            unclaimedMoves.add(new UnclaimedSingleMove(
                    location, ScotlandYard.Ticket.SECRET, destination
            ));
        }
        return unclaimedMoves;
    }

    private ImmutableSet<Move> generateMoves(Collection<UnclaimedMove> unclaimedMoves) {
        ImmutableSet.Builder<Move> builder = new ImmutableSet.Builder<>();
        Consumer<Player> generatePlayerMoves = player -> {
            Consumer<UnclaimedMove> claimMove = unclaimedMove -> {
                Consumer<Move> moveConsumer = move -> builder.add(move);
                if (unclaimedMove.isDoubleMove()) {
                    claimDoubleMove(player, (UnclaimedDoubleMove) unclaimedMove)
                            .ifPresent(moveConsumer);
                } else {
                    claimSingleMove(player, (UnclaimedSingleMove) unclaimedMove)
                            .ifPresent(moveConsumer);
                }
            };
            unclaimedMoves.stream().forEach(claimMove);
        };
        players.stream().forEach(generatePlayerMoves);
        return builder.build();
    }

    private Optional<Move> claimDoubleMove(final Player player, final UnclaimedDoubleMove unclaimedMove) {
        if (player.location() != unclaimedMove.source) return Optional.empty();
        if (player.isDetective()) return Optional.empty();
        if (unclaimedMove.ticket1 == unclaimedMove.ticket2) {
            if (!player.hasAtLeast(unclaimedMove.ticket1, 2)) return Optional.empty();
        } else {
            if (!player.has(unclaimedMove.ticket1)) return Optional.empty();
            if (!player.has(unclaimedMove.ticket2)) return Optional.empty();
        }
        return Optional.of(new Move.DoubleMove(
                player.piece(),
                unclaimedMove.source,
                unclaimedMove.ticket1,
                unclaimedMove.destination1,
                unclaimedMove.ticket2,
                unclaimedMove.destination2
        ));
    }

    private Optional<Move> claimSingleMove(final Player player, final UnclaimedSingleMove unclaimedMove) {
        if (player.location() != unclaimedMove.source) return Optional.empty();
        if (!player.has(unclaimedMove.ticket)) return Optional.empty();
        return Optional.of(new Move.SingleMove(
                player.piece(),
                unclaimedMove.source,
                unclaimedMove.ticket,
                unclaimedMove.destination
        ));
    }
}
